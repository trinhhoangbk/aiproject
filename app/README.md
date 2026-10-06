# Hub — application module

Spring Boot 3.3 (Java 21) · modular monolith.
Baselines in effect: BUSINESS / CONTEXT / ARCHITECTURE / TECHNICAL DESIGN / PLAN APPROVED · JIRA READY.

## Pre-reqs
- JDK 21 (Temurin recommended).
- Docker Desktop (for Postgres + Kafka + Hub).
- A chosen **non-production** Jira Cloud Free site + service-account API Token (per CP-1).
- A tunnel binary (ngrok or cloudflared).

## First-run
```bash
# 0. Install Gradle wrapper (one-off; needs a system gradle). After that use ./gradlew only.
gradle wrapper --gradle-version 8.10

# 1. Build
./gradlew clean build

# 2. Bring up Postgres + Kafka (not Hub yet)
cd ../deploy
cp .env.example .env    # fill real values; chmod 600 .env
docker compose up -d postgres kafka

# 3. Start the Hub
docker compose up -d hub
```
Health probe: `curl -s localhost:8080/actuator/health | jq`.

## Build tasks
- `./gradlew bootRun` — run app locally (uses JVM on host, hits containerised PG/Kafka).
- `./gradlew test` — unit + Testcontainers integration tests.
- `./gradlew bootBuildImage` — produce `com.mbs/hub:<ver>` Docker image.

## Documentation
See the stage result files at the repository root:
- `04-PLANNING/03_Implementation_Plan_RESULT.md` — the 36 PLAN steps.
- `04-PLANNING/07_Jira_Task_Breakdown_RESULT.md` — 42-item Jira backlog.
- `02-ARCHITECTURE_FINAL/architecture-diagram.html` — pictures.

## Deliberate v1.0 limits
- No CI/CD (ADR-ARCH-008); release is manual with `pg_dump` taken first (PRE-00 in deploy/RELEASE.md).
- Jira writes gated by `JIRA_WRITE_DRY_RUN=true` default (PLAN-COND-01); flip only after CP-1.
- Tunnel exposes **only** `/webhooks/jira` (TD-COND-01).
