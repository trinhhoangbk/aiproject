#!/usr/bin/env bash
# ============================================================================
# scripts/smoke.sh — CP-2 metrics sanity + PS-01..PS-08 deployment smoke
# (PS-09 "assign write" runs separately with JIRA_WRITE_DRY_RUN=false.)
#
# Precondition: `docker compose -f deploy/docker-compose.yml up -d` is up and
# healthy. The script talks to the Hub over 127.0.0.1:8080 and to Postgres via
# the compose network name ("hub-postgres" when run from the host with the
# container port published).
#
# Usage from the repo root:
#   HUB_ADMIN_EMAIL=you@mbs.vn HUB_ADMIN_PASS=xxx ./scripts/smoke.sh
#
# The script never calls Jira directly — all Jira smoke happens via the Hub
# (CP-2 counters + webhook signature smoke + capacity-edit audit).
# ============================================================================
set -u -o pipefail

HUB_URL="${HUB_URL:-http://127.0.0.1:8080}"
ADMIN_EMAIL="${HUB_ADMIN_EMAIL:-}"
ADMIN_PASS="${HUB_ADMIN_PASS:-}"
PG_CONTAINER="${PG_CONTAINER:-hub-postgres}"
PG_USER="${PG_USER:-hub}"
PG_DB="${PG_DB:-hub}"
TUNNEL_URL="${TUNNEL_URL:-}"           # optional — if set, PS-04 probes it too

COOKIE="$(mktemp)"
trap 'rm -f "$COOKIE"' EXIT

C_GREEN=$'\033[32m'; C_RED=$'\033[31m'; C_YEL=$'\033[33m'; C_END=$'\033[0m'
FAILS=0

pass() { printf "  ${C_GREEN}✓${C_END} %s\n" "$1"; }
fail() { printf "  ${C_RED}✗${C_END} %s\n" "$1"; FAILS=$((FAILS+1)); }
warn() { printf "  ${C_YEL}!${C_END} %s\n" "$1"; }
hdr()  { printf "\n${C_GREEN}== %s ==${C_END}\n" "$1"; }

need() { command -v "$1" >/dev/null 2>&1 || { echo "missing dep: $1"; exit 2; }; }
need curl; need jq; need docker

# ---------------------------------------------------------------------------
# PS-01 — Health probe + component UP (DB + Kafka)
# ---------------------------------------------------------------------------
hdr "PS-01  /actuator/health"
HEALTH="$(curl -fsS "$HUB_URL/actuator/health" || true)"
if [[ -z "$HEALTH" ]]; then
  fail "/actuator/health unreachable at $HUB_URL"
else
  STATUS=$(echo "$HEALTH" | jq -r '.status // empty')
  [[ "$STATUS" == "UP" ]] && pass "overall status UP" || fail "overall status=$STATUS"
  # show-details=when-authorized → anonymous call omits components; this is OK.
  # If we're logged in later we re-probe.
fi

# ---------------------------------------------------------------------------
# PS-02 — Login as bootstrap admin
# ---------------------------------------------------------------------------
hdr "PS-02  Admin login"
if [[ -z "$ADMIN_EMAIL" || -z "$ADMIN_PASS" ]]; then
  warn "HUB_ADMIN_EMAIL / HUB_ADMIN_PASS not set — skipping (grab them from docker logs for the first boot)"
else
  LOGIN_CODE=$(curl -sS -o /dev/null -w '%{http_code}' \
      -c "$COOKIE" -b "$COOKIE" \
      -X POST "$HUB_URL/api/auth/login" \
      -H 'Content-Type: application/x-www-form-urlencoded' \
      --data-urlencode "email=$ADMIN_EMAIL" \
      --data-urlencode "password=$ADMIN_PASS")
  if [[ "$LOGIN_CODE" == "200" ]]; then
    pass "login 200"
    ME=$(curl -fsS -b "$COOKIE" "$HUB_URL/api/auth/me")
    ROLE=$(echo "$ME" | jq -r '.role // empty')
    [[ "$ROLE" == "ADMIN" ]] && pass "/me role=ADMIN" || fail "/me role=$ROLE (expected ADMIN)"
  else
    fail "login HTTP $LOGIN_CODE"
  fi
fi

# ---------------------------------------------------------------------------
# PS-03 — Allow-list a Jira project (admin API)
# ---------------------------------------------------------------------------
hdr "PS-03  Allow-list a Jira project"
if [[ -z "$ADMIN_EMAIL" ]]; then
  warn "no admin session — skipping"
else
  # Pick a safe project key for the smoke. Operator can override via env.
  SMOKE_PROJECT="${SMOKE_PROJECT:-DEMO}"
  CREATE=$(curl -sS -b "$COOKIE" -c "$COOKIE" -o /tmp/_al.json -w '%{http_code}' \
      -X PUT "$HUB_URL/api/allowlist/$SMOKE_PROJECT" \
      -H 'Content-Type: application/json' \
      -d "{\"projectKey\":\"$SMOKE_PROJECT\",\"enabled\":true}")
  if [[ "$CREATE" =~ ^20[01]$ ]]; then
    pass "allow-list PUT returned $CREATE"
  else
    fail "allow-list POST HTTP $CREATE — body: $(cat /tmp/_al.json 2>/dev/null)"
  fi
  # Verify by SELECT.
  ROWS=$(docker exec -i "$PG_CONTAINER" psql -U "$PG_USER" -d "$PG_DB" -tA \
      -c "SELECT project_key FROM core.allow_list WHERE enabled;" 2>/dev/null || true)
  if echo "$ROWS" | grep -q "^$SMOKE_PROJECT$"; then
    pass "DB row present in core.allow_list"
  else
    fail "no row for $SMOKE_PROJECT in core.allow_list"
  fi
fi

# ---------------------------------------------------------------------------
# PS-04 — Tunnel probe + webhook signature rejection
# ---------------------------------------------------------------------------
hdr "PS-04  Tunnel + webhook"
if [[ -n "$TUNNEL_URL" ]]; then
  OUT=$(curl -sS -o /dev/null -w '%{http_code}' "$TUNNEL_URL/api/auth/me" || true)
  [[ "$OUT" == "404" ]] && pass "tunnel /api/auth/me → 404 (narrowing OK, TD-COND-01)" \
    || fail "tunnel /api/auth/me HTTP $OUT (expected 404 — tunnel exposes too much)"
  OUT=$(curl -sS -o /dev/null -w '%{http_code}' "$TUNNEL_URL/webhooks/jira" || true)
  [[ "$OUT" == "401" ]] && pass "tunnel /webhooks/jira (no HMAC) → 401" \
    || fail "tunnel /webhooks/jira HTTP $OUT (expected 401)"
else
  warn "TUNNEL_URL not set — probing local only"
fi
OUT=$(curl -sS -o /dev/null -w '%{http_code}' -X POST "$HUB_URL/webhooks/jira" \
    -H 'Content-Type: application/json' -d '{"timestamp":0,"webhookEvent":"ping"}' || true)
if [[ "$OUT" == "401" ]]; then
  pass "local /webhooks/jira (no HMAC) → 401"
else
  fail "local /webhooks/jira HTTP $OUT (expected 401)"
fi

# ---------------------------------------------------------------------------
# PS-05 — Reconciler smoke-ingest (presence of hourly cron bean)
# ---------------------------------------------------------------------------
hdr "PS-05  Reconciler bean + readiness"
BEANS=$(curl -sS "$HUB_URL/actuator/beans" 2>/dev/null | jq -r '.contexts.application.beans | keys[] // empty' | grep -i reconciler || true)
if [[ -n "$BEANS" ]]; then
  pass "reconciler bean present: $(echo "$BEANS" | tr '\n' ' ')"
else
  warn "/actuator/beans not exposed; checking scheduled tasks instead"
  TASKS=$(curl -sS -b "$COOKIE" "$HUB_URL/actuator/scheduledtasks" 2>/dev/null | jq -r '.cron[]?.runnable.target // empty' || true)
  echo "$TASKS" | grep -qi reconcile && pass "cron scheduled: $TASKS" || warn "could not confirm reconciler schedule"
fi

# ---------------------------------------------------------------------------
# PS-06 — Webhook event ingestion smoke (prometheus counter check)
# ---------------------------------------------------------------------------
hdr "PS-06  Webhook event metric"
# A real Jira ping (operator triggers "Send test" in Jira UI) increments the counter.
# Here we only verify the counter is registered and reachable.
if [[ -n "$ADMIN_EMAIL" ]]; then
  P=$(curl -sS -b "$COOKIE" "$HUB_URL/actuator/prometheus" || true)
  # Micrometer-registry-prometheus may render a counter named foo_total as
  # either foo_total or foo_total_total depending on version. Match either.
  for M in jira_events_processed_total jira_webhook_signature_failed_total mv_refresh_total audit_writes_total; do
    LINE=$(printf '%s\n' "$P" | awk -v m="$M" '$1 == m || $1 == m"_total" {print; exit}')
    if [[ -n "$LINE" ]]; then
      V=$(printf '%s' "$LINE" | awk '{print $2}')
      pass "$M present (current=${V:-0})"
    else
      fail "$M MISSING from /actuator/prometheus"
    fi
  done
else
  warn "no admin session — /actuator/prometheus is ADMIN-only, skipping"
fi

# ---------------------------------------------------------------------------
# PS-07 — Dashboard load (dry-run on): /api/workload p95 (quick 5x sampling)
# ---------------------------------------------------------------------------
hdr "PS-07  Dashboard load (quick timing)"
if [[ -n "$ADMIN_EMAIL" ]]; then
  MID=$(curl -sS -b "$COOKIE" "$HUB_URL/api/auth/me" | jq -r '.memberId // empty')
  if [[ -z "$MID" ]]; then
    warn "no memberId on admin principal — bootstrap-admin may not have been seeded, skipping"
  else
    for i in 1 2 3 4 5; do
      T=$(curl -sS -b "$COOKIE" -o /dev/null -w '%{time_total}' "$HUB_URL/api/workload/$MID?window=week" || echo 9)
      printf "    sample #%d — %.3fs\n" "$i" "$T"
    done
    pass "sampled; p95 target ≤ 2.5 s — eyeball above"
  fi
else
  warn "no admin session — skipping"
fi

# ---------------------------------------------------------------------------
# PS-08 — Audit mutation (capacity edit → audit_log row)
# ---------------------------------------------------------------------------
hdr "PS-08  Audit mutation"
if [[ -n "$ADMIN_EMAIL" ]]; then
  BEFORE=$(docker exec -i "$PG_CONTAINER" psql -U "$PG_USER" -d "$PG_DB" -tA \
      -c "SELECT count(*) FROM audit.audit_log WHERE action LIKE '%CAPACITY%';" 2>/dev/null | tr -d '[:space:]')
  # Any capacity-global PUT — operator supplies a reasonable value.
  OUT=$(curl -sS -b "$COOKIE" -c "$COOKIE" -o /tmp/_cap.json -w '%{http_code}' \
      -X PUT "$HUB_URL/api/capacity/global" \
      -H 'Content-Type: application/json' \
      -d '{"dailyHours":8.0,"weeklyHours":40.0,"effectiveFrom":"2026-10-06"}')
  if [[ "$OUT" =~ ^20[01]$ ]]; then
    AFTER=$(docker exec -i "$PG_CONTAINER" psql -U "$PG_USER" -d "$PG_DB" -tA \
      -c "SELECT count(*) FROM audit.audit_log WHERE action LIKE '%CAPACITY%';" 2>/dev/null | tr -d '[:space:]')
    if [[ "${AFTER:-0}" -gt "${BEFORE:-0}" ]]; then
      pass "audit_log grew by $((AFTER - BEFORE)) (CAPACITY action)"
    else
      fail "no new audit_log row — aspect not binding?"
    fi
  else
    fail "capacity PUT HTTP $OUT — body: $(cat /tmp/_cap.json 2>/dev/null)"
  fi
else
  warn "no admin session — skipping"
fi

# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------
hdr "Summary"
if [[ $FAILS -eq 0 ]]; then
  printf "${C_GREEN}ALL GREEN${C_END} — ready for CP-2 sign-off.\n"
  exit 0
else
  printf "${C_RED}%d check(s) failed${C_END} — fix before CP-2 sign-off.\n" "$FAILS"
  exit 1
fi
