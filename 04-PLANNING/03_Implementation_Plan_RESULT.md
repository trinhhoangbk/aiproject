# 03 — IMPLEMENTATION PLAN · Result (re-run)

| Field | Value |
|---|---|
| Stage | 04-PLANNING v5.2 / 03 Implementation Plan |
| Date | 2026-10-06 10:00 ICT (re-run; original 2026-10-04) |
| **Status** | **🟡 IMPLEMENTATION PLAN DRAFTED WITH OPEN ITEMS** |

> 12 milestones, 36 ordered steps (`PLAN-001` … `PLAN-036`), 3 human checkpoints. Fully reversible via `git revert` + `DROP SCHEMA` + Flyway re-run (greenfield). Each step is specific, small, independently verifiable and traceable.

---

## 1. Plan Summary
See status.

## 2. Preconditions
HB-01…HB-07 from `02_Dependency_Analysis_RESULT.md` + confirmed macOS dev host + non-production Jira site.

## 3. Ordered Implementation Steps

### M0 — Scaffold
- **PLAN-001** Scaffold Gradle Spring Boot app. Target: `app/{build.gradle.kts,settings.gradle.kts,gradlew,gradle/wrapper/*,src/main/java/com/mbs/hub/HubApplication.java,src/main/resources/application.yml}`. Verify: `./gradlew bootRun` + `/actuator/health` UP. TD-001.
- **PLAN-002** Docker Compose for Postgres + Kafka + Hub. Target: `deploy/{docker-compose.yml,.env.example}`. Verify: `docker compose up -d` + `pg_isready` + `kafka-topics --list`. TD-005.
- **PLAN-003** `Clock` bean fixed to `Asia/Saigon`. Target: `com.mbs.hub.config.ClockConfig`. Verify: unit test proves TZ regardless of host. DEC-002, F-TD-01.

### M1 — Core schema + roster bootstrap
- **PLAN-004** `V001__core_schema.sql` with `password_hash` + `password_updated_at` (COND-02). All `core.*` tables.
- **PLAN-005** `V002__jira_schema.sql` (`issue_projection`, `worklog_projection`, `jira_event_dedup`).
- **PLAN-006** `V003__audit_schema.sql` with role grants (append-only).
- **PLAN-007** `V004__materialized_views.sql` with unique indexes (F-DB-03).
- **PLAN-008** `V005__seed_vn_holidays.sql` for current year.
- **PLAN-009** Repositories + domain record classes for roster/capacity/holiday/allowlist/skill; `CapacityResolver` 3-tier (DEC-004).
- **PLAN-010** REST controllers for `/api/{roster,capacity,holidays,allowlist,skills}/*` (no security filter yet).

### M2 — Jira adapter (read) + webhook + tunnel
- **PLAN-011** Jira REST client (`search`, `issue`, `worklog`), Basic auth from `JIRA_API_TOKEN` + `JIRA_USER_EMAIL`. TD-003, ADR-ARCH-001/010.
- **PLAN-012** Jira webhook controller + HMAC verifier. 05 Security §4.3; SV-01.
- **PLAN-013** Kafka wiring (4 topics + DLQ). TD-004.
- **PLAN-014** Webhook → Kafka wiring.
- **PLAN-015** Jira event consumer with dedup + UPSERT guard.
- **PLAN-016** Jira reconciler `@Scheduled` hourly + adaptive cadence on 429 (COND-04).
- **PLAN-017** Tunnel configuration in `docs/runbook.md`; exposes **only** `/webhooks/jira` (COND-01).

### 🧑‍⚖️ CP-1 (before any Jira write)
Operator confirms service account + API Token + webhook URL registered on allow-listed projects only + non-production Jira site.

### M3 — ingestion complete
Rolled into PLAN-015/016.

### M4 — Materialized views refresher
- **PLAN-018** MV refresh service + debouncer (2 s).
- **PLAN-019** Nightly full-rebuild (`@Scheduled` cron 02:30).

### M5 — Workload / Overload / Overdue / Early-Warning
- **PLAN-020** `WorkloadService` + `/api/workload/{memberId}` + `/overload`. AC-001.*.
- **PLAN-021** `OverdueService` + `EarlyWarningService` with TPR formula. AC-003.*; DEC-009.

### M6 — Daily report + ETA
- **PLAN-022** `DailyReportService` + `/api/reporting/daily`. AC-002.*; DEC-001/002.
- **PLAN-023** `EtaService` with α over last 10 WD (F-03). AC-004.*.

### M7 — Pipeline + Balancing + Assignment write-back
- **PLAN-024** `PipelineService` + `/api/pipeline` CRUD. AC-005.*.
- **PLAN-025** `BalancingService` + `/api/heatmap` + `/api/balancing/suggest`. AC-006.1/2/3.
- **PLAN-026** `JiraWriteClient` (assignee + ADF comment) with sync + 403 rollback. DEC-008; TD-009.
- **PLAN-027** `AssignmentService` + `POST /api/assignments` + audit. AC-006.4/5/6.

### M8 — Spring Security + login
- **PLAN-028** `SecurityConfig` form login, BCrypt-12, session cookie, CSRF enabled (webhook excluded).
- **PLAN-029** `@PreAuthorize` on every controller + MEMBER data-level filter.
- **PLAN-030** `V006__bootstrap_admin.sql` + `BootstrapAdminRunner`; one-time password to stdout; runbook: clear scrollback (PLAN-COND-04). TD-012.

### M9 — Audit
- **PLAN-031** `AuditService` + instrumentation on every mutation.

### M10 — SPA
- **PLAN-032** SPA scaffold under `src/main/resources/static/`; pre-built static assets; Dockerfile has no Node (PLAN-COND-02).
- **PLAN-033** All screens: workload, daily, overdue, heatmap, pipeline, assign, admin, login. Vietnamese UI (DEC-016).

### M11 — Observability + runbook
- **PLAN-034** Micrometer metrics + logback JSON + secret masking.
- **PLAN-035** `docs/runbook.md`: start/stop, tunnel, Jira API-Token 90-day rotation (COND-03), rebuild projection, `pg_dump` pre-migration habit, `scripts/check-no-playbook-drift.sh` authoring (PLAN-COND-03).

### 🧑‍⚖️ CP-2 (metrics sanity)

### M12 — Smoke tests
- **PLAN-036** E2E on dev host with Testcontainers + WireMock Jira; `./gradlew test` green.

### 🧑‍⚖️ CP-3 (= Development exit for v1.0)

## 4. Migration / Config Steps
V001 (PLAN-004) → V002 (005) → V003 (006) → V004 (007) → V005 (008) → V006 (030).

## 5. Test / Verification Hooks per Step
Each PLAN step above lists its Verification. Full catalogue in `04_Test_Strategy_RESULT.md`.

## 6. Human / External Checkpoints
CP-1 (post PLAN-017), CP-2 (post PLAN-034), CP-3 (post PLAN-036).

## 7. Risky / Irreversible Steps
| Step | Why | Mitigation |
|---|---|---|
| PLAN-026/027 | Jira writes visible to Jira users immediately | `JIRA_WRITE_DRY_RUN=true` default (PLAN-COND-01); non-production Jira site via CP-1 |
| PLAN-004 (V001) | Flyway forward-only | `pg_dump` pre-migration habit; runbook |
| PLAN-030 | Bootstrap-admin stdout password | Operator captures + clears scrollback (PLAN-COND-04) |

## 8. Traceability (REQ → AC → TD → PLAN)
```
REQ-001 → AC-001.* → ADR-ARCH-002/009, TD-001/008 → PLAN-004,005,007,009,015,018,020
REQ-002 → AC-002.* → ADR-ARCH-001/003/005, TD-001/003/011 → PLAN-011..016,022
REQ-003 → AC-003.* → DEC-009 → PLAN-005,007,015,018,021
REQ-004 → AC-004.* → ADR-ARCH-003 → PLAN-023
REQ-005 → AC-005.* → ADR-ARCH-002 → PLAN-004,009,010,024
REQ-006 → AC-006.* → ADR-ARCH-004/006/010 → PLAN-025,026,027,031
REQ-007 → AC-007.* → ADR-ARCH-001/004/005 → PLAN-011..017
REQ-008 → AC-008.* → ADR-ARCH-007, COND-02 → PLAN-004 (col),028,029,030
REQ-009 → AC-009.1 → ADR-ARCH-009 → PLAN-007,018,019,034
TD-COND-01 → PLAN-017 + runbook
TD-COND-02 → PLAN-004
TD-COND-03 → PLAN-035
TD-COND-04 → PLAN-016
F-TD-01 → PLAN-003
```

## 9. Open Items
| ID | Item |
|---|---|
| PLAN-O-01 | SPA framework at PLAN-032 (IMP-O-03). Not architectural. |
| PLAN-O-02 | Tunnel vendor at PLAN-017 (INT-O-01). |
| PLAN-O-03 | Full skill-taxonomy L1/L2 values (DB-O-02). |

## 10. Status
```
IMPLEMENTATION PLAN DRAFTED WITH OPEN ITEMS
```

⛔ **STOP — stage boundary.** Next: `04_Test_Strategy.md`.
