# 01 — IMPACT ANALYSIS · Result (re-run)

| Field | Value |
|---|---|
| Stage | 04-PLANNING v5.2 / 01 Impact Analysis |
| Date | 2026-10-06 10:00 ICT (re-run; original 2026-10-04 16:21) |
| Role | Senior Engineer / Change Analyst (AI) |
| Change nature | **Greenfield** — target application does not yet exist |
| **Status** | **🟡 IMPACT ANALYSIS COMPLETE WITH OPEN ITEMS** |

> Playbook v5.2 kept 01–06 content identical to v5.1; the real additions sit in 07–08. Impact analysis is therefore substantively unchanged from the previous run.

---

## 1. Scope Summary
A new Spring Boot application lives in `new project/app/`, Docker Compose in `new project/deploy/`, runbook/docs under `new project/docs/`. Nothing in the existing repository (playbooks + Context Layer) is deleted or edited; everything is additive. No incumbent production code → regression surface is **external only** (Jira Cloud).

## 2. Evidence Inspected
Full recursive listing of `/Users/hoang/new project` on 2026-10-06 10:00 ICT plus staged contents of every stage result produced through TECHNICAL DESIGN APPROVED (16:20 on 2026-10-04).

Existing content:
- 5 playbook folders (00–04) + Context Layer + CLAUDE.md (read-only process assets).
- No `app/`, `deploy/`, `docs/` directories; no VCS metadata.

## 3. Direct Change Surface

Every path below is **new**.

### 3.1 Build & scaffold
`new project/app/{build.gradle.kts, settings.gradle.kts, gradlew, gradle/wrapper/*}` · `app/src/main/resources/application.yml` · `app/src/main/java/com/mbs/hub/HubApplication.java` · `deploy/{docker-compose.yml, .env.example, RELEASE.md}` · `docs/{runbook.md, ADF-examples.md}`.

### 3.2 Flyway migrations
`V001__core_schema.sql` (incl. `password_hash` + `password_updated_at` per TD-COND-02) · `V002__jira_schema.sql` · `V003__audit_schema.sql` · `V004__materialized_views.sql` · `V005__seed_vn_holidays.sql` · `V006__bootstrap_admin.sql`.

### 3.3 Application packages (TD §4)
`com.mbs.hub.{config, security, jira, jira.webhook, jira.write, sync.reconciler, sync.consumer, materialized, roster, capacity, holiday, allowlist, skill, workload, overdue, reporting, pipeline, balancing, audit, ui}`.

### 3.4 Config
`application.yml` (Spring Security, Kafka, Flyway, Actuator) · `logback-spring.xml` (JSON logs + secret-masking).

## 4. Indirect / Dependency Impact
In-repo: none. External: Jira Cloud (webhook registration one-off), tunnel (ngrok/Cloudflare), developer host (Docker Desktop, JDK 21).

## 5. API / Data / Integration / Security Impact

| Area | Impact |
|---|---|
| API | 15 new controller families under `/api/*` + `/webhooks/jira` (02 API Contract §3). |
| Data | 4 new logical schemas (`core`, `jira`, `audit`, `mv`), 18 tables, 2 materialized views. |
| Integration | 4 new surfaces I-01…I-04 (Jira read/write/webhook + Kafka). |
| Security | New Spring Security filter chain, HMAC verifier, BCrypt store, append-only audit grants. |

## 6. Runtime / Deployment Impact
Dev machine, Docker Compose (Postgres 16, Kafka KRaft, Hub). `.env` perm 0600. Tunnel exposes **only** `/webhooks/jira` (TD-COND-01). No CI/CD; manual Gradle build + `docker compose up`. Observability via Actuator + Micrometer; logs to stdout.

## 7. Regression Surface
In-repo: none (greenfield). External: Jira Cloud projects on allow-list — risk of unintended writes mitigated by `JIRA_WRITE_DRY_RUN=true` default (PLAN-COND-01). Operator machine: new local services.

## 8. Blast Radius
In repo: new files only; no risk to playbooks/Context Layer. On dev host: 3 new local services + `.env`. On Jira Cloud: writes limited to allow-listed projects (DEC-005) under service-account token.

## 9. UNKNOWN / Blockers

| ID | Item |
|---|---|
| IMP-O-01 | Dev-host OS = macOS (confirmed from device metadata). |
| IMP-O-02 | Specific non-production Jira site URL to use — Deployment-stage decision. |
| IMP-O-03 | SPA framework (not architectural; PLAN-COND-02 locks it to pre-built static assets). |

## 10. Traceability Matrix

```
REQ-001 → TD §4 workload → 3.3 workload (new)
REQ-002 → TD §4 reporting + jira adapter → 3.3 reporting, 3.3 jira (new)
REQ-003 → TD §4 overdue → 3.3 overdue (new)
REQ-004 → TD §4 workload.EtaService → 3.3 workload (new)
REQ-005 → TD §4 pipeline → 3.3 pipeline + 3.2 V001 (new)
REQ-006 → TD §4 balancing + jira.write + audit → 3.3 balancing, jira.write, audit (new)
REQ-007 → TD §4 jira.webhook + sync → 3.3 jira.webhook, sync (new)
REQ-008 → TD §4 security → 3.3 security + COND-02 column (new)
REQ-009 → TD-008 materialized views → 3.2 V004 (new)
TD-COND-01 → tunnel ingress rule → docs/runbook.md
TD-COND-02 → password_hash in V001 → 3.2 V001
TD-COND-03 → rotation checklist → docs/runbook.md
TD-COND-04 → adaptive reconciler cadence → 3.3 sync.reconciler + application.yml
```

## 11. Status
```
IMPACT ANALYSIS COMPLETE WITH OPEN ITEMS
```

⛔ **STOP — stage boundary.** Next: `02_Dependency_Analysis.md`.
