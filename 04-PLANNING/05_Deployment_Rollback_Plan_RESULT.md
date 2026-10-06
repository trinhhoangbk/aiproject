# 05 — DEPLOYMENT & ROLLBACK PLAN · Result (re-run)

| Field | Value |
|---|---|
| Stage | 04-PLANNING v5.2 / 05 Deployment & Rollback Plan |
| Date | 2026-10-06 10:00 ICT (re-run) |
| Target env | **Developer machine** (macOS) |
| **Status** | **🟡 DEPLOYMENT ROLLBACK PLAN COMPLETE WITH CONDITIONS** |

---

## 1. Release Scope
Three containers via `docker compose up -d`: `postgres:16-alpine`, `confluentinc/cp-kafka:7.6.x` (KRaft), `hub`. Plus a host-side tunnel exposing only `/webhooks/jira`.

## 2. Deployable Units
Hub Docker image `com.mbs/hub:<tag>` (built from `app/` via `jib`), Postgres/Kafka pulled from Docker Hub, Flyway migrations packaged in the image, static SPA packaged in the image, `.env` authored by operator (0600, never committed). Tag: `v<semver>-<yyyymmdd>`.

## 3. Preconditions
| ID | Precondition |
|---|---|
| PRE-00 | **`pg_dump` of current `hub` DB taken** (PLAN-COND-03 habit). |
| PRE-01 | JDK 21 + Docker Desktop on dev host. |
| PRE-02 | `.env` prepared; perm 0600. |
| PRE-03 | Non-production Jira Cloud Free site; service account + API Token issued. |
| PRE-04 | Tunnel binary installed; stable URL (preferred: Cloudflare named tunnel). |
| PRE-05 | Tunnel exposes **only** `/webhooks/jira` (TD-COND-01). |
| PRE-06 | `./gradlew test` passes locally on the tagged commit. |
| PRE-07 | Git tag `v<semver>-<yyyymmdd>`. |
| PRE-08 | VN holiday list for current year verified in V005 seed. |
| PRE-09 | `JIRA_WRITE_DRY_RUN=true` in `.env` for first boot of a tag. |

## 4. Deployment Sequence
```
1.  git checkout v<semver>-<yyyymmdd>
2.  cd new project/app
3.  ./gradlew clean build -x test            # tests already green in PRE-06
4.  ./gradlew jibDockerBuild
5.  cd ../deploy
6.  edit .env  (ensure JIRA_WRITE_DRY_RUN=true for first boot)
7.  docker compose pull                       # Postgres, Kafka
8.  docker compose up -d postgres kafka       # wait for pg_isready & kafka-topics --list
9.  docker compose up -d hub                  # Flyway migrates on boot; wait /actuator/health UP
10. In another terminal: start tunnel  (cloudflared tunnel run <name>  OR  ngrok http 8080)
11. Note HUB_PUBLIC_URL; update Jira webhook URL (one-time Admin UI action in Jira)
12. Smoke-test (§8).
13. Flip JIRA_WRITE_DRY_RUN=false in .env; docker compose restart hub.
14. Announce v<semver>.
```

## 5. Migration / Config Sequence
Flyway runs inside Hub container boot; order V001 → V002 → V003 → V004 → V005 → V006. `V006__bootstrap_admin.sql` prints a one-time password to stdout once; operator captures from `docker compose logs hub`, logs in, changes it before anyone else is granted access, then clears scrollback (PLAN-COND-04).

## 6. Compatibility / Feature-Flag Strategy
Single flag `JIRA_WRITE_DRY_RUN` (default `true` — PLAN-COND-01). No API versioning in v1.0.

## 7. Pre-Deploy Checks
| ID | Check |
|---|---|
| PD-01 | Flyway migration count matches tag. |
| PD-02 | `.env` has every required variable (`scripts/check-env.sh`). |
| PD-03 | `JIRA_WRITE_DRY_RUN=true` on first boot. |
| PD-04 | `docker info` works. |
| PD-05 | Port 8080 not in use. |
| PD-06 | Disk ≥ 2 GB free. |
| PD-07 | `pg_dump` artifact from PRE-00 saved under `deploy/backups/`. |

## 8. Post-Deploy Verification
| ID | Check | Pass |
|---|---|---|
| PS-01 | `/actuator/health` | 200 UP + DB + Kafka components UP |
| PS-02 | Login as bootstrap Admin | 200 |
| PS-03 | Allow-list a Jira project | row in `core.allow_list` |
| PS-04 | Register webhook in Jira against tunnel URL | Jira UI OK |
| PS-05 | Reconciler smoke-ingest | `issue_projection` has rows after 1 h or manual trigger |
| PS-06 | Webhook smoke-ingest | trivial Jira change reflects within 60 s |
| PS-07 | Dashboard load (dry-run on) | p95 ≤ 2.5 s with seed |
| PS-08 | Audit mutation | capacity edit → `audit_log` row OK |
| PS-09 | Assign write (dry-run off) | Jira assignee + comment appear |
| PS-10 | `/actuator/prometheus` | shows TD §7 metrics |

If any fails → rollback (§10).

## 9. Rollback Triggers
PS-01…10 failures without quick fix · unintended Jira writes (dry-run off against prod site) · Flyway migration failure without hotfix available · dev-host OOM.

## 10. Rollback / Recovery

### 10.1 Immediate
`docker compose stop hub` → stop tunnel → disable (don't delete) Jira webhook temporarily.

### 10.2 Code rollback
`git checkout v<previous>` → rebuild → `docker compose up -d hub`.

### 10.3 Schema rollback (Flyway forward-only)
Author `V00N+1__revert_<change>.sql` + hotfix tag + deploy forward. If destructive: restore `pg_dump` from PRE-00.

### 10.4 Projection recovery
`TRUNCATE jira.issue_projection, jira.worklog_projection CASCADE` + `REFRESH MATERIALIZED VIEW` + wait for next reconcile (or trigger manually).

### 10.5 Audit recovery
No procedure — best-effort by business decision (F-ARCH-NEW-02 Reading A).

### 10.6 Jira state recovery
Operator manually reassigns in Jira; Hub cannot undo its write. Delete stray internal comment manually.

## 11. Irreversible / Forward-Fix
| Item | Why | Mitigation |
|---|---|---|
| Jira writes | SoR = Jira | Dry-run default + CP-1 non-production site |
| Destructive Flyway migration | Forward-only | `pg_dump` PRE-00 + forward-fix migrations |
| Audit append-only | Design | — |

## 12. Coordination
Operator (Master) + Jira site admin (same person).

## 13. Traceability
| PLAN step | Deployment artefact | Verification | Rollback |
|---|---|---|---|
| PLAN-002 | `docker-compose.yml` | PS-01 | `down -v` |
| PLAN-004…008 Flyway | image | PS-01 | §10.3 |
| PLAN-017 tunnel | §4 step 10 | PS-04 | stop + re-register |
| PLAN-026/027 Jira write | §4 steps 12–13 (dry-run toggle) | PS-09 | §10.6 manual |
| PLAN-030 bootstrap | V006 | PS-02 | wipe + re-run |
| PLAN-034 metrics | image | PS-10 | — |
| TD-COND-01 ingress | §4 step 10 | PRE-05 | adjust tunnel config |

## 14. Status
```
DEPLOYMENT ROLLBACK PLAN COMPLETE WITH CONDITIONS
```

Conditions: non-production Jira site PRE-03 · `JIRA_WRITE_DRY_RUN=true` first boot PRE-09 · `pg_dump` pre-migration PRE-00.

⛔ **STOP — stage boundary.** Next: `06_Plan_Review.md`.
