# aiproject — Multi-Project Workload & Resource Balancing Hub

Stage-gated AI engineering pipeline for a Jira-integrated resource-balancing Hub.
This repository contains:

- the stage playbooks (`00-PROJECT-ONBOARDING/` … `05-DEVELOPMENT/`),
- their result files (`*_RESULT.md`) and human-approval records (`*_APPROVAL_RECORD.md`),
- the Context Engineering Layer (`CLAUDE.md`, `.claude/`),
- the first-cycle Development scaffold (`app/` + `deploy/`).

## Baselines currently in effect

| Stage | Decision | Date |
|---|---|---|
| Onboarding | `PROJECT AI-READY WITH OPEN ITEMS` | 2026-10-04 |
| Requirement | `BUSINESS APPROVED` | 2026-10-04 15:46 ICT |
| Architecture — System Context | `CONTEXT APPROVED` | 2026-10-04 15:54 ICT |
| Architecture — Design | `ARCHITECTURE APPROVED WITH CONDITIONS` (+ clarifications 16:08) | 2026-10-04 16:04 ICT |
| Technical Design | `TECHNICAL DESIGN APPROVED` | 2026-10-04 16:20 ICT |
| Planning | `PLAN APPROVED` (carried v5.1→v5.2) | 2026-10-04 16:30 ICT |
| Jira Delivery | `JIRA READY` | 2026-10-06 13:02 ICT |
| Development · cycle 1 (M0 + M1 migrations) | `ACTUAL DIFF VERIFIED WITH CONDITIONS` | 2026-10-06 13:06 ICT |
| Development · cycle 2 (DEV-COND fulfilment) | fulfilled offline — see `05-DEVELOPMENT/CYCLE_2_DEV_COND_REPORT.md` | 2026-10-06 ~13:50 ICT |

## Quick start (operator / dev host)

```bash
# 1. Clone
git clone https://github.com/trinhhoangbk/aiproject.git
cd aiproject

# 2. Operator preconditions
#    - Pick a NON-PRODUCTION Jira Cloud Free site; create a dedicated service account.
#    - Issue an API Token and save it in deploy/.env (chmod 600).
#    - Reserve a stable public tunnel URL (Cloudflare named tunnel recommended).

# 3. Build the application
cd app
./gradlew clean build           # requires network access to Maven Central
./gradlew bootBuildImage        # produces com.mbs/hub:0.1.0-SNAPSHOT

# 4. Infrastructure
cd ../deploy
cp .env.example .env
chmod 600 .env                  # edit values first
docker compose up -d postgres kafka
docker compose up -d hub        # Flyway migrates V001…V005 on boot

# 5. Capture bootstrap-admin one-time password; change it; clear scrollback (PLAN-COND-04).
docker compose logs hub | grep "BOOTSTRAP ADMIN"
history -c && clear

# 6. Start the tunnel — expose ONLY /webhooks/jira (TD-COND-01)
cloudflared tunnel --config deploy/tunnel-cloudflared.yml run hub

# 7. Register the webhook URL in Jira UI against allow-listed projects only (CP-1)

# 8. Smoke tests PS-01..PS-10 per deploy/RELEASE.md

# 9. After smoke green, flip JIRA_WRITE_DRY_RUN=false in deploy/.env and restart hub.
```

## What already works (dev-container verification 2026-10-06)

- All 5 Flyway migrations apply cleanly to a fresh PostgreSQL 16 database (no cache,
  single run from `psql -v ON_ERROR_STOP=1 -f`). Verification log attached in
  `05-DEVELOPMENT/CYCLE_2_DEV_COND_REPORT.md`.
- The `ClockConfig` bean reports `Asia/Saigon` even when the host JVM is forced
  to `UTC` (F-TD-01). Verified by a standalone Java harness in the dev container
  because the egress proxy does not permit Maven Central from the Claude session.

## Open items for the operator

See `05-DEVELOPMENT/CYCLE_2_DEV_COND_REPORT.md` and the stage result files for
exhaustive lists. Headlines:

- Create the Jira project (any key — e.g. `RBH`) on the chosen non-production
  Jira Cloud Free site before Jira import.
- `./gradlew test` and `./gradlew bootBuildImage` run on the operator machine
  (network access required); the test suite's F-TD-01 is already proven by a
  standalone harness.
- CP-1 operator checklist must be signed before any Jira write path runs.

## Repository shape

```
./
├── CLAUDE.md                               Context Engineering Layer entry point
├── README.md                               (this file)
├── .gitignore                              repo-level ignores
├── .claude/                                rules / commands / agents (Hub-specific)
├── 00-PROJECT-ONBOARDING/                  playbooks + result files + approval record
├── 01-REQUIREMENT/                         idem
├── 02-ARCHITECTURE_FINAL/                  idem (+ architecture-diagram.html)
├── 03-TECHNICAL-DESIGN/                    idem
├── 04-PLANNING/                            idem (+ 07/08 Jira delivery files)
├── 05-DEVELOPMENT/                         cycle result files + CYCLE_2 report
├── app/                                    Spring Boot (Java 21) application
│   ├── build.gradle.kts, settings.gradle.kts, gradle.properties
│   ├── gradlew / gradlew.bat / gradle/wrapper/*   (DEV-COND-02 fulfilled)
│   ├── src/main/java/com/mbs/hub/
│   │   ├── HubApplication.java
│   │   └── config/ClockConfig.java
│   ├── src/test/java/com/mbs/hub/config/ClockConfigTest.java
│   ├── src/main/resources/
│   │   ├── application.yml, logback-spring.xml
│   │   └── db/migration/V001__ … V005__.sql   (DEV-COND-03 verified)
│   └── README.md
└── deploy/
    ├── docker-compose.yml
    ├── .env.example                        (JIRA_WRITE_DRY_RUN=true default — PLAN-COND-01)
    └── RELEASE.md                          (90-day Jira token rotation — TD-COND-03)
```
