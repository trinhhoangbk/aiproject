# GO LIVE — Operator Steps (CP-1 → CP-2 → PS-01..PS-08)

> This is the condensed walkthrough. Full reference: `docs/runbook.md`.
> Baseline posture: `JIRA_WRITE_DRY_RUN=true` through PS-08. Flip to `false`
> only after PS-01..PS-08 are all green, then re-run PS-04 (the assignment
> smoke) to confirm real writes land.

---

## 1 · Install `.env`

The repo includes `deploy/hub-env-filled.txt` with your credentials already in
place (JIRA_BASE_URL, JIRA_USER_EMAIL, JIRA_API_TOKEN, JIRA_WEBHOOK_SECRET,
HUB_BOOTSTRAP_ADMIN_EMAIL). One rename + chmod turns it into the real `.env`:

```bash
cd "/Users/hoang/new project/deploy"
mv hub-env-filled.txt .env
chmod 600 .env
```

Verify:
```bash
grep -E '^JIRA_|^HUB_BOOTSTRAP_|^JIRA_WRITE_DRY_RUN' .env
```
Expected: all four Jira lines filled, `JIRA_WRITE_DRY_RUN=true`,
`HUB_PUBLIC_URL=https://CHANGE-ME.ngrok.app` (still a placeholder — fixed in §2).

---

## 2 · Bring up a tunnel

Pick ONE (Cloudflare is preferred — ngrok free tier can't enforce TD-COND-01
without an Edge rule).

### 2a · Cloudflare named tunnel

```bash
cloudflared tunnel login
cloudflared tunnel create hub
cloudflared tunnel route dns hub hub.<your-domain>
cp deploy/tunnel-cloudflared.yml.example /etc/cloudflared/config.yml
# edit: replace <your-named-tunnel-id> + hostname + credentials-file
cloudflared tunnel --config /etc/cloudflared/config.yml run hub &
```

Set the resulting hostname into `.env`:
```bash
sed -i '' -e 's|^HUB_PUBLIC_URL=.*|HUB_PUBLIC_URL=https://hub.<your-domain>|' deploy/.env
```

### 2b · ngrok (fallback)

```bash
ngrok config add-authtoken <your-ngrok-token>
ngrok start --all --config deploy/tunnel-ngrok.yml
```
Then in the ngrok dashboard add an Edge rule for the reserved domain:
`if path matches ^/webhooks/jira then forward; else return 404`.

Set the reserved domain into `.env`:
```bash
sed -i '' -e 's|^HUB_PUBLIC_URL=.*|HUB_PUBLIC_URL=https://hub-webhook.<your-reserved>.ngrok.app|' deploy/.env
```

### Verify tunnel narrowing

```bash
curl -i https://<HUB_PUBLIC_URL>/api/auth/me       # expect 404
curl -i https://<HUB_PUBLIC_URL>/webhooks/jira     # expect 401 (missing signature)
```
If `/api/auth/me` returns anything other than 404, STOP and tighten the ingress
before continuing — TD-COND-01 forbids exposing anything but `/webhooks/jira`.

---

## 3 · Register the webhook in Jira

In the Jira Cloud UI:

1. Go to  <https://weeklywtf.atlassian.net/plugins/servlet/webhooks> (or
   `https://<your-site>.atlassian.net/plugins/servlet/webhooks` for yours).
2. Click **Create a webhook**.
3. Fill:
   - **Name**: `MBS Hub`
   - **Status**: Enabled
   - **URL**: `${HUB_PUBLIC_URL}/webhooks/jira`
   - **Secret**: paste the `JIRA_WEBHOOK_SECRET` value from your `.env`
   - **Events**: tick Issue (created, updated), Worklog (created, updated, deleted),
     and Comment (created, updated) at minimum.
   - **JQL filter**: `project in (<your allow-listed projects>) AND resolution = Unresolved`
     — the Hub's allow-list is the authoritative gate, but this narrows the
     webhook traffic before it hits your tunnel.
4. Save. Jira UI → "Send test" to confirm connectivity (the Hub returns 200 fast
   on a verified HMAC, or 401 on a wrong secret).

---

## 4 · Boot the Hub

```bash
cd "/Users/hoang/new project"
docker compose -f deploy/docker-compose.yml up -d
docker compose -f deploy/docker-compose.yml logs -f hub | grep -A3 "BOOTSTRAP ADMIN"
```

The log prints ONCE a banner like:
```
=== BOOTSTRAP ADMIN =============================================
 email    = txhoang.bk@gmail.com
 password = <24-char Base64URL>
 ROTATE   via POST /api/auth/change-password after first login
=================================================================
```
**Save that password now** — it is NOT re-printed on subsequent boots.

Export for the smoke script:
```bash
export HUB_ADMIN_EMAIL="txhoang.bk@gmail.com"
export HUB_ADMIN_PASS="<the 24-char password>"
export TUNNEL_URL="https://<HUB_PUBLIC_URL from step 2>"
```

---

## 5 · CP-2 — Metrics sanity probe

```bash
curl -fsS -u "$HUB_ADMIN_EMAIL:$HUB_ADMIN_PASS" \
     http://127.0.0.1:8080/actuator/prometheus | \
  grep -E '^(jira_events_processed_total|jira_webhook_signature_failed_total|mv_refresh_total|audit_writes_total)'
```
Expected: each metric is listed, even if its value is `0`.

(Note: HTTP Basic auth on `/actuator/prometheus` works because the SecurityConfig
form-login chain also accepts Basic for API callers. If it 401's, pre-login
first and use the cookie: `curl -c /tmp/c -X POST -d "email=…&password=…"
http://127.0.0.1:8080/api/auth/login`, then re-probe with `-b /tmp/c`.)

---

## 6 · Run PS-01..PS-08

```bash
cd "/Users/hoang/new project"
./scripts/smoke.sh
```

The script runs:
- **PS-01** `/actuator/health` → UP
- **PS-02** admin login → 200 + `/me` ROLE=ADMIN
- **PS-03** POST `/api/allowlist` → row in `core.allow_list`
- **PS-04** tunnel probes (`/api/auth/me` → 404, `/webhooks/jira` → 401)
- **PS-05** reconciler bean present
- **PS-06** prometheus counters registered
- **PS-07** 5-sample p95 timing on `/api/workload`
- **PS-08** capacity PUT → new `audit_log` row

Expected final line: `ALL GREEN — ready for CP-2 sign-off.`

### If something fails
- Red line names the failing check. Fix per `docs/runbook.md` §10 troubleshooting.
- `docker compose logs hub --tail=200` is usually the next step.

---

## 7 · Flip off DRY-RUN (ONLY after PS-01..PS-08 are all green)

```bash
sed -i '' -e 's|^JIRA_WRITE_DRY_RUN=.*|JIRA_WRITE_DRY_RUN=false|' deploy/.env
docker compose -f deploy/docker-compose.yml restart hub
```

Then re-run only the assignment smoke (PS-09 — not in `smoke.sh` because it
writes to Jira):

1. In the SPA at `http://127.0.0.1:8080`, navigate to a Pipeline with
   `requiredSkills` matching a roster member.
2. Click **Gợi ý phân phối →** to open balancing.
3. Pick a candidate → **Phân phối…** → fill `issueKey` with a REAL issue in your
   allow-listed project → submit.
4. The response should NOT say "DRY_RUN" anymore. In Jira:
   - Issue assignee updated to the chosen member's Jira account.
   - A Hub comment appears on the issue naming you + the reason.
5. In Postgres:
   ```
   docker exec -it hub-postgres psql -U hub -d hub \
     -c "SELECT occurred_at, action, result FROM audit.audit_log \
         WHERE action='ASSIGNMENT' ORDER BY occurred_at DESC LIMIT 3;"
   ```
   Expect `result=OK` on the newest row.

### Rollback

If the real write misbehaves:
```bash
sed -i '' -e 's|^JIRA_WRITE_DRY_RUN=.*|JIRA_WRITE_DRY_RUN=true|' deploy/.env
docker compose -f deploy/docker-compose.yml restart hub
```
The Hub returns to dry-run; no further Jira writes until the flag is flipped
again.

---

## Done

- **CP-1**: credentials in `.env` + tunnel narrowing verified + webhook registered
- **CP-2**: metrics-sanity green (step 5)
- **PS-01..PS-08**: all green via `./scripts/smoke.sh`
- **PS-09 (optional)**: real Jira writes verified after flipping dry-run

Report back with the smoke output and the SPA assignment outcome; we'll handle
anything that didn't come out green.
