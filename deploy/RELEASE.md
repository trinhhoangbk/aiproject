# Release & manual deployment procedure

ADR-ARCH-008 locked **no CI/CD** for v1.0. This file is the manual release
contract. Follow it in order; do not skip steps.

---

## Preconditions (PRE-*)

Mirror `04-PLANNING/05_Deployment_Rollback_Plan_RESULT.md §3`:

| ID | Precondition | Verify |
|---|---|---|
| PRE-00 | `pg_dump` of the current `hub` database saved to `deploy/backups/<yyyymmdd-HHMM>.sql` (PLAN-COND-03) | `ls -la deploy/backups/` |
| PRE-01 | JDK 21 + Docker Desktop installed | `java -version && docker info` |
| PRE-02 | `.env` prepared with every variable in `.env.example`; perm `0600` | `stat -f %Lp deploy/.env` → `600` |
| PRE-03 | Non-production Jira Cloud Free site; service-account API Token issued | CP-1 checklist signed |
| PRE-04 | Tunnel binary installed; stable URL (named Cloudflare tunnel preferred) | `cloudflared tunnel list` |
| PRE-05 | Tunnel exposes ONLY `/webhooks/jira` (TD-COND-01) | `curl -s $HUB_PUBLIC_URL/api/auth/me` returns connection-refused / 404 |
| PRE-06 | `./gradlew test` passes on the tagged commit | green |
| PRE-07 | Git tag `v<semver>-<yyyymmdd>` created | `git tag --list` |
| PRE-08 | VN holiday list for the current year verified in `V005__seed_vn_holidays.sql` | eyeball |
| PRE-09 | `JIRA_WRITE_DRY_RUN=true` in `.env` for first boot | `grep JIRA_WRITE_DRY_RUN deploy/.env` |

---

## Deployment sequence

```bash
# 1. Check out the tag
git checkout v<semver>-<yyyymmdd>

# 2. Build
cd app
./gradlew clean build -x test   # tests already green in PRE-06
./gradlew bootBuildImage

# 3. Infrastructure
cd ../deploy
docker compose pull             # PG + Kafka
docker compose up -d postgres kafka

# wait for health
docker compose ps

# 4. Hub (Flyway migrates on boot)
docker compose up -d hub

# wait for /actuator/health UP
curl -s http://localhost:8080/actuator/health | jq

# 5. Capture one-time Admin password (TD-012)
docker compose logs hub | grep "BOOTSTRAP ADMIN ONE-TIME PASSWORD"
# Capture, log in, change the password immediately.
# Then: clear-scrollback.sh    # PLAN-COND-04
history -c && clear

# 6. Start tunnel (narrow ingress — TD-COND-01)
cloudflared tunnel --config deploy/tunnel-cloudflared.yml run hub

# 7. Register webhook URL in Jira UI → ${HUB_PUBLIC_URL}/webhooks/jira
#    against allow-listed projects only. CP-1 operator step.

# 8. Smoke-test (see PS-01..PS-10 in deployment plan)

# 9. Flip dry-run OFF after smoke green
sed -i.bak 's/^JIRA_WRITE_DRY_RUN=true/JIRA_WRITE_DRY_RUN=false/' deploy/.env
docker compose restart hub

# 10. Announce v<semver>
```

---

## Post-deploy verification (PS-*)

See `04-PLANNING/05_Deployment_Rollback_Plan_RESULT.md §8` for the full list
(PS-01 health, PS-02 login, PS-03 allow-list, PS-04 webhook, PS-05 reconciler,
PS-06 webhook ingest, PS-07 dashboard latency, PS-08 audit, PS-09 assign write,
PS-10 Prometheus).

---

## Rollback

Code: `git checkout v<previous>` → rebuild → `docker compose up -d hub`.
Schema: Flyway is forward-only — author `V00N+1__revert_*.sql` or restore from
PRE-00 `pg_dump`.
Jira state: Hub cannot undo its writes; manual repair in Jira UI.
Audit: no recovery (best-effort by business decision, F-ARCH-NEW-02 Reading A).

---

## 90-day Jira API Token rotation checklist (TD-COND-03)

1. In Atlassian → service-account user → "Create and manage API tokens" → create new.
2. Update `deploy/.env` → `JIRA_API_TOKEN=<new>`.
3. `docker compose restart hub` → verify `/actuator/health` UP.
4. Revoke the old token in Atlassian.
5. Record the rotation date in `deploy/token-rotation-log.md` and schedule the next (90 days ahead).
