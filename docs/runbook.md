# Runbook — Hub (Workload & Resource Balancing Hub)

> PLAN-035 — operator runbook for the dev-host prototype.
> Baselines in effect: BUSINESS / CONTEXT / ARCHITECTURE / TECHNICAL DESIGN / PLAN / JIRA READY, all APPROVED.
> Scope: single dev host (ADR-ARCH-011 LOCKED prototype). HA/multi-host is out of scope for v1.

---

## 1. Prerequisites on the dev host

- Docker Engine 24+ and Docker Compose plugin.
- 8 GB RAM, 20 GB disk free.
- Host timezone can be ANY zone — the Hub uses an injected `Clock` bean fixed at `Asia/Saigon` (F-TD-01 / DEC-002). The host zone does NOT shift any Hub date window.
- Outbound HTTPS from the host must be able to reach `*.atlassian.net` (Jira REST).
- A public tunnel endpoint (Cloudflare named tunnel OR ngrok) terminating at `127.0.0.1:8080` and forwarding ONLY `^/webhooks/jira` (TD-COND-01). See `deploy/tunnel-cloudflared.yml.example` and `deploy/tunnel-ngrok.yml.example`.

## 2. One-time setup

1. Clone the repo and `cd` into the project directory.
2. Copy the env template:
   ```
   cp deploy/.env.example deploy/.env
   chmod 600 deploy/.env
   ```
3. Fill `deploy/.env`:
   - `DB_URL`, `DB_USER`, `DB_PASS`
   - `JIRA_BASE_URL`, `JIRA_EMAIL`, `JIRA_API_TOKEN`     (CP-1 — see §6)
   - `JIRA_WEBHOOK_SECRET`                               (32-byte hex)
   - `JIRA_WRITE_DRY_RUN=true` (default — PLAN-COND-01)
   - `HUB_PUBLIC_URL=https://<your-tunnel-hostname>`
   - `HUB_BOOTSTRAP_ADMIN_EMAIL=<your-email>`
4. Start the stack:
   ```
   docker compose -f deploy/docker-compose.yml up -d
   ```
5. Capture the bootstrap-admin password from the Hub logs ONCE:
   ```
   docker compose logs hub | grep -A3 "BOOTSTRAP ADMIN"
   ```
   The banner prints a 24-char password that you MUST change at first login.

## 3. Starting / stopping

```
docker compose -f deploy/docker-compose.yml up -d          # start all
docker compose -f deploy/docker-compose.yml stop hub       # stop app (keeps DB/Kafka)
docker compose -f deploy/docker-compose.yml down           # stop all
docker compose -f deploy/docker-compose.yml logs -f hub    # tail app logs
```

Health probe: `curl -fsS http://127.0.0.1:8080/actuator/health` returns `{"status":"UP"}` when the Hub is ready.

## 4. Tunnel setup (TD-COND-01)

Cloudflare named tunnel is preferred — the config forwards only `^/webhooks/jira(/.*)?` inline. ngrok is a fallback and REQUIRES an Edge rule in the dashboard, otherwise it exposes the UI publicly, violating TD-COND-01.

Verification probes after bringing the tunnel up:
```
curl -i https://<HUB_PUBLIC_URL>/api/auth/me     # expect 404
curl -i https://<HUB_PUBLIC_URL>/webhooks/jira   # expect 401 (missing signature)
```

If either returns anything else, STOP and fix the tunnel ingress before signing CP-1.

## 5. Jira API Token rotation — every 90 days (COND-03)

1. In Jira Cloud, revoke the current token for the service account.
2. Create a new token.
3. Update `deploy/.env` → `JIRA_API_TOKEN=<new>`.
4. Restart the Hub:
   ```
   docker compose -f deploy/docker-compose.yml restart hub
   ```
5. Verify a webhook ping replays cleanly (Jira UI → send test webhook → `jira_webhook_received_total` increments in `/actuator/prometheus`).

Set a calendar reminder for +90 days when you rotate; the Hub does not auto-rotate.

## 6. Checkpoints

- **CP-1 — Jira is live.** The gate that moves the Hub from "code lands, nothing calls Jira" to real integration. Operator signs CP-1 by (a) filling `deploy/.env` with real Jira creds (§2 step 3), (b) bringing up a working tunnel per §4, and (c) registering the Jira webhook pointing at `${HUB_PUBLIC_URL}/webhooks/jira` with the shared secret. CP-1 unblocks M7 (Jira write-back).
- **CP-2 — Metrics sanity.** After PLAN-034 glue lands, verify `/actuator/prometheus` shows:
  - `jira_events_processed_total` increments on each inbound webhook
  - `mv_refresh_total` increments shortly after
  - `audit_writes_total` increments on any admin mutation or login
  - `jira_webhook_signature_failed_total` is at 0
- **CP-3 — Smoke.** After PLAN-036 E2E (M12), the operator runs the dev-host smoke script end-to-end before declaring v1.

## 7. Projection rebuild (when webhook events were missed or corrupted)

The reconciler normally closes any gap in one hourly tick (PLAN-016, 24-hour JQL window). To force a full rebuild:

```
# Pause the webhook controller (reject new events while rebuilding).
curl -X POST http://127.0.0.1:8080/actuator/loggers/com.mbs.hub.jira.webhook \
  -H 'Content-Type: application/json' -d '{"configuredLevel":"WARN"}'   # cosmetic; webhook still runs

# Trigger a full reconciler pass (admin only).
curl -X POST http://127.0.0.1:8080/api/admin/reconciler/run \
  -u admin@example:$ADMIN_PASS

# After the pass completes, the mv.* tables rebuild on the next debounced tick.
```

(If the admin reconciler endpoint is not wired yet — PLAN-034 adds it under `/actuator/scheduledtasks` — fall back to `docker compose restart hub`; the reconciler re-fires on boot.)

## 8. `pg_dump` before every migration

Before applying a new Flyway migration against a non-empty database:

```
docker compose exec postgres pg_dump -U $DB_USER $DB_NAME \
  | gzip > backups/hub-$(date +%Y%m%d-%H%M%S).sql.gz
```

Flyway migrations in this project are forward-only (TD-010); a rollback is a restore of the dump taken just before the migration.

## 9. `scripts/check-no-playbook-drift.sh` (PLAN-COND-03)

A convenience helper that fails CI (or a local pre-commit) when the Development artifacts diverge from the frozen playbook stages:

```
#!/usr/bin/env bash
set -euo pipefail
if git diff --name-only HEAD~1 | grep -E '^(00-PROJECT-ONBOARDING|01-REQUIREMENT|02-ARCHITECTURE_FINAL|03-TECHNICAL-DESIGN|04-PLANNING)/'; then
  echo "ERROR: playbook stages touched; those are frozen baselines."
  exit 1
fi
echo "OK: no playbook drift."
```

Save as `scripts/check-no-playbook-drift.sh` (`chmod +x`). Run manually before each dev-cycle commit.

## 10. Troubleshooting

| Symptom | Likely cause | Action |
|---|---|---|
| `/actuator/prometheus` returns 401 | SecurityConfig restricts it to ADMIN | Log in as the bootstrap admin, then re-probe with that session cookie |
| `jira_webhook_signature_failed_total` keeps incrementing | Secret in Jira UI ≠ `JIRA_WEBHOOK_SECRET` in `.env` | Rotate secret in Jira UI AND `.env` together, then restart Hub |
| MV heatmap empty with issues in Jira | No webhook events received yet | Trigger a reconciler run (§7); check tunnel ingress is actually reaching the Hub |
| `audit_write_failures_total > 0` | DB write path failed; non-blocking by design | Inspect the exception in logs; typical cause is temporary DB connectivity |
| Overload calc looks off | DEC-010 "cannot_reschedule" source not set | Review `hub_lock_deadline_flag` on the issue (admin UI → lock deadline) |
| Hub boots but shows no admin | `HUB_BOOTSTRAP_ADMIN_EMAIL` not set OR a member row already existed | Add one via SQL or restart with empty core.member + env set |

## 11. Known deferrals (v1.0)

- SV-06 login lockout (needs Redis) — tracked in `SecurityConfig` javadoc.
- Per-issue targeted MV refresh (currently every member on every event; cheap at 10–50 members).
- Multi-host HA / RTO-RPO contracts (ADR-ARCH-011 LOCKED prototype).

---
This runbook is maintained alongside the code; the authoritative version is the `main` branch of the repo.
