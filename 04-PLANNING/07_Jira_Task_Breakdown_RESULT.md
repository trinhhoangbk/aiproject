# 07 — JIRA TASK BREAKDOWN · Result

| Field | Value |
|---|---|
| Stage | 04-PLANNING v5.2 / 07 Jira Task Breakdown |
| Date | 2026-10-06 10:00 ICT |
| Role | Enterprise Delivery Architect (AI) — proposes backlog; does not create Jira issues |
| Precondition | `PLAN APPROVED` (carried from 2026-10-04 16:30; see `PLAN_APPROVAL_RECORD.md`) |
| Jira project conventions supplied? | ❌ No — Jira Cloud Free (ADR-ARCH-010), project key / workflow / required fields / DoD unknown |
| **Status** | **🟡 JIRA BREAKDOWN DRAFTED WITH OPEN ITEMS** |

> Jira is a delivery view of the approved Engineering Plan — it adds no scope. All temporary ids below are local planning identifiers (`JIRA-TMP-xxx`, `EPIC-TMP-xxx`), never real Jira keys. Nothing is created in Jira in this file.

---

## 1. Jira Breakdown Summary

Approved plan carries **36 PLAN steps** across **12 milestones** and **3 Human Checkpoints**. Grouped into **7 Epics** and **42 delivery issues** (36 implementation + 3 Checkpoint tasks + 3 approval-condition remediation issues). No sub-tasks were introduced; each milestone step is already small enough to own as a single Story/Task. Jira project configuration is unknown — all issue-type mapping and workflow mapping are marked `TO CONFIGURE`.

## 2. Approved Scope & Conditions

- **Approved baselines** carried through: BUSINESS APPROVED 2026-10-04 15:46 · CONTEXT APPROVED 15:54 (+ clarifications 16:08) · ARCHITECTURE APPROVED 16:04 (+ clarifications 16:08) · TECHNICAL DESIGN APPROVED 16:20 · PLAN APPROVED 16:30 (carried).
- **Conditions** (become authoritative action items via dedicated issues below): PLAN-COND-01…04 and TD-COND-01…04.
- **Exclusions** (per BRD §3): no replacement of Jira for issue creation / sprint planning / workflow; no payroll/timekeeping.
- **Human Gates still live within execution:** CP-1 (before Jira write), CP-2 (metrics sanity), CP-3 (= Development exit).

## 3. Epic Map

Each Epic groups a coherent approved outcome. No Epic introduces new REQ/AC.

| local id | Summary | Outcome | REQ IDs | PLAN steps in scope |
|---|---|---|---|---|
| **EPIC-TMP-01** | Foundation — scaffold + core governance schema | A buildable Spring Boot app on dev host with a working core database (`core.*`) governing roster, capacity (3-tier), holidays, allow-list, skills and pipeline. | REQ-008 (part) | PLAN-001 … PLAN-010 |
| **EPIC-TMP-02** | Jira Sync Integration | Hub ingests Jira state via webhook (fast path) + hourly reconcile (safety net); projection + materialized views keep p99 ≤ 5 min and dashboard ≤ 2.5 s. | REQ-007, REQ-009 | PLAN-011 … PLAN-019 (+ CP-1) |
| **EPIC-TMP-03** | Workload & Overdue Views | Member-centric workload / overload / overdue / early-warning / ETA / daily report are observable through the Hub API. | REQ-001, REQ-002, REQ-003, REQ-004 | PLAN-020 … PLAN-023 |
| **EPIC-TMP-04** | Pipeline & Resource Balancing | Managers maintain queued projects, see a heatmap, get member suggestions, perform safe Assign/Re-assign with audit. | REQ-005, REQ-006 | PLAN-024 … PLAN-027 |
| **EPIC-TMP-05** | Security & Audit | Spring Security filter chain, BCrypt-12 login, RBAC in depth, append-only audit, bootstrap admin. | REQ-008 (completion) | PLAN-028 … PLAN-031 |
| **EPIC-TMP-06** | User Interface (SPA) | Pre-built static SPA in Vietnamese for Admin/Manager/Member screens; no Node in Hub Docker image. | REQ-001…006, REQ-008 (UI side) | PLAN-032, PLAN-033 |
| **EPIC-TMP-07** | Observability & Release Readiness | Metrics, structured logs, runbook, dev-host smoke tests; dev-host exit for v1.0. | REQ-009 (observability), + CPs | PLAN-034 … PLAN-036 (+ CP-2, CP-3) |

Each Epic also carries the relevant subset of conditions (listed per-issue below).

## 4. Story/Task/Sub-task Backlog (human-readable table)

Legend — Type: **S** Story (user-observable behaviour) · **T** Task (technical/operational) · **CP** Checkpoint-task · **GOV** Governance/condition task. Risk: L/M/H.

| local id | Parent | Type | Summary | PLAN step(s) | REQ/AC | Risk | Depends on (BLOCKED BY) |
|---|---|---|---|---|---|---|---|
| JIRA-TMP-001 | EPIC-TMP-01 | T | Scaffold Gradle Spring Boot app | PLAN-001 | — | L | — |
| JIRA-TMP-002 | EPIC-TMP-01 | T | Docker Compose for Postgres+Kafka+Hub | PLAN-002 | — | L | 001 |
| JIRA-TMP-003 | EPIC-TMP-01 | T | Clock bean fixed to Asia/Saigon | PLAN-003 | DEC-002 | L | 001 |
| JIRA-TMP-004 | EPIC-TMP-01 | T | Flyway V001 `core_schema` (incl. `password_hash` for TD-COND-02) | PLAN-004 | REQ-008 | M | 002 |
| JIRA-TMP-005 | EPIC-TMP-01 | T | Flyway V002 `jira_schema` | PLAN-005 | REQ-007 | L | 004 |
| JIRA-TMP-006 | EPIC-TMP-01 | T | Flyway V003 `audit_schema` with append-only grants | PLAN-006 | DEC-008 | M | 004 |
| JIRA-TMP-007 | EPIC-TMP-01 | T | Flyway V004 materialized views + unique indexes | PLAN-007 | REQ-009, DEC-011 | M | 004, 005 |
| JIRA-TMP-008 | EPIC-TMP-01 | T | Flyway V005 seed VN holidays | PLAN-008 | DEC-003 | L | 004 |
| JIRA-TMP-009 | EPIC-TMP-01 | T | Repositories + `CapacityResolver` (3-tier) | PLAN-009 | DEC-004 | L | 004 |
| JIRA-TMP-010 | EPIC-TMP-01 | S | Admin CRUD screens for roster/capacity/holiday/allow-list/skills via `/api/*` | PLAN-010 | DEC-003/004/005/006/007 | M | 009 |
| JIRA-TMP-011 | EPIC-TMP-02 | T | Jira REST client (search/issue/worklog) with API Token auth | PLAN-011 | REQ-007, ADR-ARCH-010 | M | 001 |
| JIRA-TMP-012 | EPIC-TMP-02 | T | Jira webhook controller + HMAC verifier | PLAN-012 | AC-007.1, T-01 | M | 001 |
| JIRA-TMP-013 | EPIC-TMP-02 | T | Kafka wiring (4 topics + DLQ) | PLAN-013 | ADR-ARCH-004 | L | 002 |
| JIRA-TMP-014 | EPIC-TMP-02 | T | Webhook→Kafka wiring | PLAN-014 | AC-007.1 | L | 012, 013 |
| JIRA-TMP-015 | EPIC-TMP-02 | T | Jira event consumer: dedup + UPSERT monotonic guard | PLAN-015 | AC-007.1/3, F-DB-01 | M | 005, 013, 014 |
| JIRA-TMP-016 | EPIC-TMP-02 | T | Jira reconciler `@Scheduled` hourly + adaptive cadence on 429 (COND-04) | PLAN-016 | AC-007.2, F-INT-02 | M | 011, 013 |
| JIRA-TMP-017 | EPIC-TMP-02 | T | Tunnel configuration documented; exposes **only** `/webhooks/jira` (TD-COND-01) | PLAN-017 | COND-01, SEC-R-01 | H | 012 |
| JIRA-TMP-CP1 | EPIC-TMP-02 | CP | **Checkpoint CP-1** — operator confirms Jira service account + API Token + webhook on allow-list only + non-production site | — | precondition for 018+ | H | 017 |
| JIRA-TMP-018 | EPIC-TMP-02 | T | Materialized-view refresher + 2 s debounce | PLAN-018 | REQ-009, AC-003.3 | M | 007, 015 |
| JIRA-TMP-019 | EPIC-TMP-02 | T | Nightly MV full-rebuild job | PLAN-019 | REQ-009 | L | 018 |
| JIRA-TMP-020 | EPIC-TMP-03 | S | `WorkloadService` + `/api/workload/{memberId}` + `/overload` | PLAN-020 | AC-001.1…1.5 | M | 009, 018 |
| JIRA-TMP-021 | EPIC-TMP-03 | S | `OverdueService` + `EarlyWarningService` (TPR DEC-009) + `/api/overdue*` | PLAN-021 | AC-003.1…3.5 | M | 015, 018 |
| JIRA-TMP-022 | EPIC-TMP-03 | S | `DailyReportService` + `/api/reporting/daily` | PLAN-022 | AC-002.1…2.4 | M | 005, 015 |
| JIRA-TMP-023 | EPIC-TMP-03 | S | `EtaService` with α over last 10 WD + `/api/eta/*` | PLAN-023 | AC-004.1/2 | M | 005 |
| JIRA-TMP-024 | EPIC-TMP-04 | S | `PipelineService` + `/api/pipeline` CRUD (skills from DEC-007 taxonomy) | PLAN-024 | AC-005.1/2 | M | 010 |
| JIRA-TMP-025 | EPIC-TMP-04 | S | `BalancingService` + `/api/heatmap` + `/api/balancing/suggest` | PLAN-025 | AC-006.1/2/3 | M | 020, 024 |
| JIRA-TMP-026 | EPIC-TMP-04 | T | `JiraWriteClient` (assignee + ADF comment) + 403 rollback + `JIRA_WRITE_DRY_RUN` flag honoured (PLAN-COND-01) | PLAN-026 | AC-006.4/5, DEC-008 | H | 011, CP-1 |
| JIRA-TMP-027 | EPIC-TMP-04 | S | `AssignmentService` + `POST /api/assignments` + audit emission | PLAN-027 | AC-006.4/5/6 | H | 006, 025, 026 |
| JIRA-TMP-028 | EPIC-TMP-05 | T | Spring Security config (form login, BCrypt-12, session, CSRF, CORS) | PLAN-028 | REQ-008, SV-02…09 | M | 004 |
| JIRA-TMP-029 | EPIC-TMP-05 | T | `@PreAuthorize` on every controller + MEMBER data-level filter | PLAN-029 | AC-008.1/2/3, T-03/04/08 | M | 020, 021, 022, 024, 025, 027, 028 |
| JIRA-TMP-030 | EPIC-TMP-05 | T | Bootstrap-admin runner + V006 + runbook clears scrollback (PLAN-COND-04) | PLAN-030 | REQ-008, TD-012, F-PLN-03 | M | 004, 028 |
| JIRA-TMP-031 | EPIC-TMP-05 | T | `AuditService` + instrumentation on every mutation | PLAN-031 | DEC-008, AC-006.6 | M | 006, 027, 029 |
| JIRA-TMP-032 | EPIC-TMP-06 | T | SPA scaffold under `static/`; pre-built, no Node in Dockerfile (PLAN-COND-02) | PLAN-032 | REQ-001…006, 008, F-PLN-01 | M | 010, 020, 021, 022, 024, 025, 027, 029 |
| JIRA-TMP-033 | EPIC-TMP-06 | S | SPA screens (workload, daily, overdue, heatmap, pipeline, assign, admin, login) — Vietnamese UI | PLAN-033 | REQ-001…008 UI side; DEC-016 | M | 032 |
| JIRA-TMP-034 | EPIC-TMP-07 | T | Micrometer metrics + logback JSON + secret masking | PLAN-034 | TD §7, F-SEC-02 | L | 011, 015, 016, 026 |
| JIRA-TMP-CP2 | EPIC-TMP-07 | CP | **Checkpoint CP-2** — metrics & logs sanity review | — | post-PLAN-034 | M | 034 |
| JIRA-TMP-035 | EPIC-TMP-07 | T | `docs/runbook.md`: start/stop, tunnel, Jira API-Token 90-day rotation (COND-03), `pg_dump` PRE-00, `scripts/check-no-playbook-drift.sh` authoring (PLAN-COND-03) | PLAN-035 | COND-03, F-PLN-SEC-01, F-PLN-DEP-02 | M | 017, 030 |
| JIRA-TMP-036 | EPIC-TMP-07 | T | E2E smoke tests on dev host (Testcontainers + WireMock) | PLAN-036 | AC-001.1, AC-002.1, AC-003.4, AC-006.4/5, AC-007.1/2 | M | 020, 021, 022, 024, 025, 027, 029, 031 |
| JIRA-TMP-CP3 | EPIC-TMP-07 | CP | **Checkpoint CP-3** — final sign-off (= v1.0 Development exit) | — | post-PLAN-036 | H | 036, CP-2 |
| JIRA-TMP-G01 | EPIC-TMP-02 | GOV | TD-COND-01 governance: operator-signed statement that tunnel exposes only `/webhooks/jira` with log evidence from first run | — | TD-COND-01 | H | 017 |
| JIRA-TMP-G02 | EPIC-TMP-02 | GOV | TD-COND-04 governance: documented adaptive-cadence rule + metric alarm | — | TD-COND-04 | M | 016 |
| JIRA-TMP-G03 | EPIC-TMP-05 | GOV | TD-COND-03 governance: 90-day Jira API Token rotation calendar reminder | — | TD-COND-03 | M | 035 |

**Totals:** 36 implementation (JIRA-TMP-001…036) + 3 CP (CP1/2/3) + 3 GOV (G01…G03) = **42 issues**. Zero sub-tasks (none needed at this granularity).

## 5. Dependency Map (text form of the dependency graph)

### Hard orderings (BLOCKS)
- **001 BLOCKS** 002, 003, 011, 012, 013 (scaffold must exist).
- **004 BLOCKS** 005, 006, 007, 008, 009, 010, 015, 028, 030 (core schema is the gate).
- **005 BLOCKS** 007, 015, 022, 023 (jira schema must exist before projections and reports).
- **006 BLOCKS** 027, 031 (audit schema before any audit emission).
- **007 BLOCKS** 018, 020, 021 (materialized views before workload/overdue views).
- **009 BLOCKS** 010, 020, 024 (repos before controllers).
- **010 BLOCKS** 024, 025, 027, 032 (admin screens require roster controllers; UI depends on them).
- **011 BLOCKS** 016, 026, 034 (Jira adapter must exist).
- **012 BLOCKS** 014, 017, CP-1 (webhook controller before wiring + tunnel + checkpoint).
- **013 BLOCKS** 014, 015, 016 (Kafka wiring prereq).
- **014 BLOCKS** 015 (webhook must publish before consumer).
- **015 BLOCKS** 018, 020, 021, 022, 027, 031, 034 (projection must exist to feed everything).
- **016 BLOCKS** 034 (reconciler metric sources).
- **017 BLOCKS** CP-1 (tunnel must be live).
- **CP-1 BLOCKS** 018, 026, 027 (no Jira write before human checkpoint).
- **018 BLOCKS** 020, 021, 025 (MVs feed views).
- **020 BLOCKS** 025 (workload used by balancing).
- **024 BLOCKS** 025 (pipeline required to balance).
- **025 BLOCKS** 027 (balancing recommendation precedes assign).
- **026 BLOCKS** 027.
- **027 BLOCKS** 031 (audit emission around assignment).
- **028 BLOCKS** 029, 030 (filter chain before method-level RBAC + bootstrap).
- **029 BLOCKS** 032 (RBAC before UI consumption).
- **030 BLOCKS** 035 (bootstrap runbook refers to the runner).
- **031 BLOCKS** 036 (audit must be in place for E2E).
- **032 BLOCKS** 033.
- **034 BLOCKS** CP-2.
- **CP-2 BLOCKS** 035 (runbook finalisation after metrics sanity, optional ordering — can parallelise).
- **036 BLOCKS** CP-3.
- GOV issues G01…G03 relate to the operator and may close in parallel with their anchor PLAN issues.

### Non-blocking relates
- 020 ↔ 021 (siblings; share projections).
- 022 ↔ 023 (sibling reports).
- 033 ↔ 034 (UI vs ops — different owners).

### Graph is acyclic (verified by topological order: 001 → 002/003 → 004 → 005/006/008/009 → 007/010 → 011 → 012/013 → 014/016 → 015 → 017 → CP-1 → 018 → 019 → 020/022/023 → 021 → 024 → 025 → 026 → 027 → 028 → 029/030 → 031 → 032 → 033 → 034 → CP-2 → 035 → 036 → CP-3).

## 6. Definition of Ready Assessment

Every implementation issue is Ready: scope bounded, REQ/AC trace exists, PLAN mapping exists, dependencies known, target change surface known (greenfield → new file paths listed in `01_Impact_Analysis_RESULT.md §3`), acceptance/verification testable, no unresolved blocking decision (CP-1 is the only remaining human gate before the first Jira write and is modelled as a Checkpoint task).

Three Checkpoint tasks are Ready for a human (not for a developer).

Three GOV issues are Ready for the operator.

## 7. Traceability Matrix (PLAN → JIRA-TMP → Epic → REQ/AC)

| PLAN | JIRA-TMP | Epic | REQ/AC/Decision |
|---|---|---|---|
| PLAN-001 | 001 | 01 | — (scaffold) |
| PLAN-002 | 002 | 01 | — (infra) |
| PLAN-003 | 003 | 01 | DEC-002 |
| PLAN-004 | 004 | 01 | REQ-008 + TD-COND-02 |
| PLAN-005 | 005 | 01 | REQ-007 |
| PLAN-006 | 006 | 01 | DEC-008 |
| PLAN-007 | 007 | 01 | REQ-009, DEC-011 |
| PLAN-008 | 008 | 01 | DEC-003 |
| PLAN-009 | 009 | 01 | DEC-004 |
| PLAN-010 | 010 | 01 | DEC-003/004/005/006/007 |
| PLAN-011 | 011 | 02 | REQ-007, ADR-ARCH-010 |
| PLAN-012 | 012 | 02 | AC-007.1 |
| PLAN-013 | 013 | 02 | ADR-ARCH-004 |
| PLAN-014 | 014 | 02 | AC-007.1 |
| PLAN-015 | 015 | 02 | AC-007.1/3 |
| PLAN-016 | 016 | 02 | AC-007.2 + TD-COND-04 |
| PLAN-017 | 017 | 02 | TD-COND-01 |
| PLAN-018 | 018 | 02 | REQ-009, AC-003.3 |
| PLAN-019 | 019 | 02 | REQ-009 |
| PLAN-020 | 020 | 03 | AC-001.1…5 |
| PLAN-021 | 021 | 03 | AC-003.1…5 + DEC-009 |
| PLAN-022 | 022 | 03 | AC-002.1…4 + DEC-001/002 |
| PLAN-023 | 023 | 03 | AC-004.1/2 |
| PLAN-024 | 024 | 04 | AC-005.1/2 + DEC-007 |
| PLAN-025 | 025 | 04 | AC-006.1/2/3 + B-RULE-04 + DEC-011 |
| PLAN-026 | 026 | 04 | AC-006.4/5 + DEC-008 + PLAN-COND-01 |
| PLAN-027 | 027 | 04 | AC-006.4/5/6 |
| PLAN-028 | 028 | 05 | REQ-008 |
| PLAN-029 | 029 | 05 | AC-008.1/2/3 |
| PLAN-030 | 030 | 05 | REQ-008 + TD-012 + PLAN-COND-04 |
| PLAN-031 | 031 | 05 | DEC-008 + AC-006.6 |
| PLAN-032 | 032 | 06 | REQ-001…008 UI + PLAN-COND-02 |
| PLAN-033 | 033 | 06 | DEC-016 |
| PLAN-034 | 034 | 07 | TD §7 observability |
| PLAN-035 | 035 | 07 | TD-COND-03 + PLAN-COND-03 |
| PLAN-036 | 036 | 07 | AC smoke subset |
| CP-1 | CP1 | 02 | precondition for 018+ |
| CP-2 | CP2 | 07 | post-034 |
| CP-3 | CP3 | 07 | post-036 |
| GOV conditions | G01…G03 | 02/05 | TD-COND-01/04/03 |

## 8. Unmapped PLAN Steps
**None.** All 36 PLAN steps are covered. All 3 CPs are represented. All 4 TD-COND and 4 PLAN-COND are embedded in PLAN steps or GOV tasks.

## 9. Estimation / Assignment / Scheduling Placeholders

Every delivery issue carries:
- `estimate: TO ESTIMATE` (project did not supply an estimation method).
- `assignee: UNASSIGNED`.
- `priority: TO PRIORITIZE`.
- `sprint: TO SCHEDULE`.

AI does not propose values. The project owner may fill these at the Jira creation step.

## 10. Structured Jira Payload Specification

Below is the full YAML spec for the first few issues (one per Epic) and all 3 Checkpoints and 3 GOVs. Issues 005–036 follow the same schema with the fields mapped above (summary = table "Summary" column; `plan_steps`, `requirements`, `acceptance_criteria`, `technical_design`, `change_surface`, `dependencies.blocked_by` from §4 and §5). A fully-expanded YAML for every issue is available on request; this file deliberately keeps it to a representative sample to stay reviewable in one sitting.

```yaml
- local_id: EPIC-TMP-01
  issue_type: Epic
  summary: "Foundation — scaffold + core governance schema"
  outcome: "A buildable Spring Boot app on dev host with a working core database."
  traceability:
    requirements: [REQ-008]
    plan_steps: [PLAN-001, PLAN-002, PLAN-003, PLAN-004, PLAN-005, PLAN-006, PLAN-007, PLAN-008, PLAN-009, PLAN-010]
  exclusions:
    - no application code; migrations only
  estimate: TO ESTIMATE
  assignee: UNASSIGNED
  priority: TO PRIORITIZE
  sprint: TO SCHEDULE

- local_id: JIRA-TMP-001
  issue_type: Task
  parent: EPIC-TMP-01
  summary: "Scaffold Gradle Spring Boot application"
  objective: "Produce a minimal buildable Spring Boot app that boots and returns `/actuator/health` UP."
  scope:
    - app/build.gradle.kts, settings.gradle.kts
    - app/src/main/java/com/mbs/hub/HubApplication.java
    - app/src/main/resources/application.yml
    - gradle/wrapper + gradlew, gradlew.bat
  out_of_scope:
    - no domain code
    - no DB connection
  traceability:
    requirements: []
    acceptance_criteria: []
    technical_design: [TD-001]
    plan_steps: [PLAN-001]
  change_surface:
    components: ["app/"]
    files_or_paths:
      - "app/build.gradle.kts (NEW)"
      - "app/settings.gradle.kts (NEW)"
      - "app/src/main/java/com/mbs/hub/HubApplication.java (NEW)"
      - "app/src/main/resources/application.yml (NEW)"
  dependencies:
    blocks: [JIRA-TMP-002, JIRA-TMP-003, JIRA-TMP-011, JIRA-TMP-012, JIRA-TMP-013]
    blocked_by: []
  acceptance_and_verification:
    - "`./gradlew bootRun` starts without error"
    - "`curl localhost:8080/actuator/health` returns 200 {status:UP}"
  definition_of_done:
    - approved scope implemented
    - actual diff limited to listed paths
    - review completed
    - documentation updated where required
  risk: LOW
  release_notes: "First commit on new branch; builds a scaffold only."
  estimate: TO ESTIMATE
  assignee: UNASSIGNED
  priority: TO PRIORITIZE
  sprint: TO SCHEDULE

- local_id: EPIC-TMP-02
  issue_type: Epic
  summary: "Jira Sync Integration"
  outcome: "Hub ingests Jira state via webhook (fast path) + hourly reconcile (safety net); projection and materialized views keep p99 ≤ 5 min and dashboard ≤ 2.5 s."
  traceability:
    requirements: [REQ-007, REQ-009]
    plan_steps: [PLAN-011, PLAN-012, PLAN-013, PLAN-014, PLAN-015, PLAN-016, PLAN-017, PLAN-018, PLAN-019]
  exclusions:
    - Jira write-back lives in EPIC-TMP-04
  estimate: TO ESTIMATE
  assignee: UNASSIGNED
  priority: TO PRIORITIZE
  sprint: TO SCHEDULE

- local_id: JIRA-TMP-017
  issue_type: Task
  parent: EPIC-TMP-02
  summary: "Tunnel configuration exposes ONLY /webhooks/jira (TD-COND-01)"
  objective: "A tunnel binary (ngrok / cloudflared) is configured so the public URL serves only /webhooks/jira; the UI is NOT publicly reachable."
  scope:
    - docs/runbook.md section "Tunnel setup"
    - deploy/.env.example HUB_PUBLIC_URL placeholder
    - example cloudflared ingress config
  out_of_scope:
    - choice of vendor (operator decides at deploy time)
  traceability:
    requirements: [REQ-007]
    acceptance_criteria: [AC-007.1]
    technical_design: [TD-006]
    plan_steps: [PLAN-017]
  change_surface:
    components: ["deploy/", "docs/"]
    files_or_paths:
      - "docs/runbook.md (NEW section)"
      - "deploy/.env.example (NEW keys)"
  dependencies:
    blocks: [JIRA-TMP-CP1, JIRA-TMP-G01]
    blocked_by: [JIRA-TMP-012]
  acceptance_and_verification:
    - "GET ${HUB_PUBLIC_URL}/webhooks/jira without signature returns 401 within 500 ms"
    - "GET ${HUB_PUBLIC_URL}/api/auth/me is NOT reachable from the public URL (connection refused or 404)"
    - "Operator checklist signed; log evidence attached (JIRA-TMP-G01)"
  definition_of_done:
    - approved scope documented
    - verification evidence captured in evidence folder
    - review completed
  risk: HIGH
  release_notes: "First time the dev host is exposed publicly; narrow surface."
  estimate: TO ESTIMATE
  assignee: UNASSIGNED
  priority: TO PRIORITIZE
  sprint: TO SCHEDULE

- local_id: JIRA-TMP-CP1
  issue_type: Task
  parent: EPIC-TMP-02
  summary: "Checkpoint CP-1 — before any Jira write"
  objective: "Operator confirms: (a) dedicated Atlassian service account exists; (b) API Token stored in .env with 0600; (c) webhook URL registered in Jira against allow-listed projects only; (d) Jira site is non-production."
  scope:
    - operator checklist in docs/runbook.md
    - evidence captured as a text file in deploy/evidence/CP-1-<date>.txt
  out_of_scope:
    - no code change
  traceability:
    plan_steps: [post PLAN-017]
  dependencies:
    blocks: [JIRA-TMP-018, JIRA-TMP-026, JIRA-TMP-027]
    blocked_by: [JIRA-TMP-017]
  acceptance_and_verification:
    - all four checklist items ticked
    - evidence file committed
  definition_of_done:
    - approved human sign-off recorded
  risk: HIGH
  estimate: TO ESTIMATE
  assignee: UNASSIGNED
  priority: TO PRIORITIZE
  sprint: TO SCHEDULE

- local_id: JIRA-TMP-026
  issue_type: Task
  parent: EPIC-TMP-04
  summary: "JiraWriteClient — assignee + ADF comment with 403 rollback and dry-run flag"
  objective: "Implement the two-way write path per DEC-008 and ADR-ARCH-010; honour JIRA_WRITE_DRY_RUN flag (PLAN-COND-01)."
  scope:
    - com.mbs.hub.jira.write.JiraWriteClient
    - com.mbs.hub.jira.write.AdfBuilder
  out_of_scope:
    - the AssignmentService orchestration lives in JIRA-TMP-027
  traceability:
    requirements: [REQ-006]
    acceptance_criteria: [AC-006.4, AC-006.5]
    technical_design: [TD-003, TD-009]
    plan_steps: [PLAN-026]
  change_surface:
    components: ["com.mbs.hub.jira.write"]
    files_or_paths:
      - "com/mbs/hub/jira/write/JiraWriteClient.java (NEW)"
      - "com/mbs/hub/jira/write/AdfBuilder.java (NEW)"
  dependencies:
    blocks: [JIRA-TMP-027]
    blocked_by: [JIRA-TMP-011, JIRA-TMP-CP1]
  acceptance_and_verification:
    - "WireMock Jira: 204 happy path writes assignee + ADF comment"
    - "WireMock Jira: 403 on assignee → raise JiraWriteBlockedException with Jira message; UI test asserts rollback"
    - "WireMock Jira: 429 Retry-After honoured once; subsequent 429 surfaced"
    - "JIRA_WRITE_DRY_RUN=true → no HTTP call made; synthetic 204 returned; log line emitted"
  definition_of_done:
    - approved scope implemented
    - unit + integration tests green
    - review completed
  risk: HIGH
  estimate: TO ESTIMATE
  assignee: UNASSIGNED
  priority: TO PRIORITIZE
  sprint: TO SCHEDULE

- local_id: JIRA-TMP-G01
  issue_type: Task
  parent: EPIC-TMP-02
  summary: "GOV TD-COND-01: operator-signed evidence that tunnel exposes only /webhooks/jira"
  objective: "A human-signed evidence file + log excerpt saved under deploy/evidence/."
  scope:
    - deploy/evidence/TD-COND-01-<date>.txt
  traceability:
    plan_steps: [PLAN-017]
  dependencies:
    blocked_by: [JIRA-TMP-017]
  acceptance_and_verification:
    - operator signature in file
    - log excerpt showing probe of /api/* via tunnel is refused
  definition_of_done:
    - evidence filed; linked from runbook
  risk: HIGH
  estimate: TO ESTIMATE
  assignee: UNASSIGNED
  priority: TO PRIORITIZE
  sprint: TO SCHEDULE
```

The remaining 32 implementation issues, 2 Checkpoints and 2 GOV items follow this schema; their field values are deterministically taken from §4 (Summary + REQ/AC + risk), §5 (dependencies), §7 (traceability) and `03_Implementation_Plan_RESULT.md` (change surface + acceptance). On Jira creation, this file is the canonical source — a CSV / Jira import can be generated mechanically from §4+§5+§7.

## 11. Open Items / Blockers

| ID | Severity | Item |
|---|---|---|
| JIRA-O-01 | HIGH | **Jira project configuration unknown** — issue types, workflow, required custom fields, components/labels, DoD overrides, estimation method. Mapping below is marked `TO CONFIGURE` and must be resolved before `JIRA READY`. |
| JIRA-O-02 | MEDIUM | **Target Jira project key** not supplied. Create a Jira project (any name, e.g. "RBH" for Resource Balancing Hub) in the chosen non-production Jira Cloud Free site before import. |
| JIRA-O-03 | LOW | SPA framework still open (IMP-O-03, PLAN-COND-02 constrains outcome). Does not block Jira creation. |
| JIRA-O-04 | LOW | Full skill-taxonomy L1/L2 values (DB-O-02). JIRA-TMP-024 can be split later if a seed list arrives. |

### "TO CONFIGURE" field map (filled at Jira creation)
- Project key: TO CONFIGURE.
- Issue types permitted: TO CONFIGURE (Epic / Story / Task / Sub-task assumed neutral).
- Workflow (Open → In Progress → In Review → Done): TO CONFIGURE.
- Required custom fields: TO CONFIGURE.
- Components / labels: TO CONFIGURE (recommend: `area/foundation`, `area/sync`, `area/views`, `area/pipeline`, `area/security`, `area/ui`, `area/ops`).
- Definition of Done: TO CONFIGURE (default DoD proposed in §4 unless project overrides).
- Estimation method: TO CONFIGURE.

## 12. Status
```
JIRA BREAKDOWN DRAFTED WITH OPEN ITEMS
```

Open items are configuration items (JIRA-O-01…04); none alters the backlog shape. The backlog is ready for independent review in `08_Jira_Readiness_Review.md`.

---
⛔ **STOP — stage boundary.** Next: `08_Jira_Readiness_Review.md`.
*No Jira issues are created in this file. Human Gate `JIRA READY` is required before any import.*
