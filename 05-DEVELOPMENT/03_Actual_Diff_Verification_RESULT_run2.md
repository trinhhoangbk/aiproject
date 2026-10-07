# 03 — ACTUAL DIFF VERIFICATION · Result (run 2)

| Field | Value |
|---|---|
| Date | 2026-10-07 15:40 ICT |
| Role | Independent Development Verification Engineer (AI) |
| Repository | `github.com/trinhhoangbk/aiproject` · branch `main` |
| Candidate (frozen) | **`6af57eb`** |
| Diff range inspected | `3fadbfa` (run-1 baseline, M0/M1) → `6af57eb` · 179 files, +9 338 / −92 |
| Supersedes | `03_Actual_Diff_Verification_RESULT.md` (run 1 covered only the 18-file M0/M1 baseline; its condition DEV-O-01 `git init` is now **closed**) |
| **Verdict** | **🟡 `ACTUAL DIFF VERIFIED WITH CONDITIONS`** |

---

## 1. Commands inspected (CONFIRMED)

```
git status --short                      → clean (no uncommitted change at candidate)
git log --oneline 3fadbfa..6af57eb      → 25 commits (cycles 3–9 + smoke fixes)
git diff --name-status 3fadbfa..6af57eb → 179 paths
git grep -nE "ATATT|<bootstrap pw>|<webhook secret>" HEAD → no match
git ls-files | grep .env                → none (deploy/.env is git-ignored)
```

Runtime evidence available (operator dev host, 2026-10-07): image built from candidate−3, `scripts/smoke.sh` PS-01/02/03/05/06/07/08 green, PS-04 red on ngrok-free narrowing only; real Jira webhook → projection observed for KAN-26/27/28. **No `./gradlew test` run exists** for this candidate (container cannot reach Maven Central; operator has not run it) → see Condition DV-C1.

## 2. Diff inspection log — an implementation gap found and closed in this run

During inspection `jira.worklog_projection` was found to have **no writer**: `DailyReportService` (AC-002.2) and `EtaService` (AC-004.2) read a table nothing populated. Per the HARD STOP rule this was `IMPLEMENTATION INCOMPLETE` for PLAN-022/023. Returned to Development (delegated DEV authority, 2026-10-05) → commit **`6af57eb`** adds `WorklogProjector` and evaluates roster membership at query time. Re-inspected: writer now exists and is wired into `JiraEventConsumer` after a successful issue UPSERT. Verdict below is for `6af57eb`.

## 3. Changed-file classification

| Class | Paths (grouped) | Count |
|---|---|---|
| **APPROVED** | `app/src/main/java/com/mbs/hub/**` (assignment, audit, balancing, config, core/*, eta, jira/*, mv/*, overdue, reporting, security, sync/*, web, workload) · `db/migration/V006__mv_plain_tables.sql` · `static/**` (SPA) · `logback-spring.xml` | 152 |
| **SUPPORTING** | `app/build.gradle`, `app/settings.gradle`, `app/Dockerfile` (build) · `deploy/RELEASE.md`, `deploy/tunnel-*.example` · `docs/runbook.md`, `docs/GO_LIVE.md` · `scripts/smoke.sh`, `scripts/check-no-playbook-drift.sh` · `05-DEVELOPMENT/CYCLE_*`, `jira-import/*` | 19 |
| **TEST** | `app/src/test/**` (7 new unit-test classes) | 7 |
| **GENERATED** | — | 0 |
| **UNRELATED** | `aiproject-cycle8-m7.tar.gz` (82 KB binary committed in `49e882d` "?") — delivery bundle, no behaviour; scanned: no secrets | 1 |
| **UNKNOWN** | — | 0 |

`app/build.gradle.kts` / `settings.gradle.kts` deleted → replaced by Groovy DSL per **human decision 2026-10-06 21:31** ("dùng `.gradle`").

## 4. Traceability (path → PLAN → REQ/AC)

| Package / path | PLAN | REQ / AC | Verification available |
|---|---|---|---|
| `core/{member,capacity,holiday,allowlist,skill}` | 009, 010 | REQ-008, DEC-003/004/005/007 | `CapacityResolverTest`; PS-03 (allow-list PUT → DB row) |
| `jira/client`, `jira/dto` | 011 | REQ-007 | `AdaptiveCadenceTest` |
| `jira/webhook` | 012, 014 | AC-007.1 | `HmacVerifierTest`; PS-04 local 401; live Jira → 200 |
| `config/Kafka*`, `sync/consumer`, `sync/dedup`, `sync/projection` | 013, 015 | AC-007.1/3 | live: KAN-26/27/28 projected |
| `sync/consumer/WorklogProjector` (**new**) | 022, 023 | AC-002.2, AC-004.2 | none yet |
| `sync/reconciler` | 016 | AC-007.2, TD-COND-04 | `AdaptiveCadenceTest` |
| `deploy/tunnel-*`, runbook §4 | 017 | TD-COND-01 | PS-04 tunnel **red** (ngrok free) |
| `mv/**`, `V006` | 018, 019 | REQ-009, DEC-009/011, F-01 | band tests ×3, `WorkingDayCalculatorTest`; live heatmap |
| `workload/**` | 020 | AC-001.1…5 | PS-07 timing; live |
| `overdue/**` | 021 | AC-003.1…5 | none |
| `reporting/**` | 022 | AC-002.1…4 | none |
| `eta/**` | 023 | AC-004.1/2 | none |
| `core/pipeline/{Service,Controller,dto}` | 024 | AC-005.1/2 | none |
| `balancing/**` | 025 | AC-006.3 | none |
| `jira/write/**` | 026 | AC-006.4/5, PLAN-COND-01 | none (dry-run default confirmed in `.env`) |
| `assignment/**` | 027 | AC-006.4/5/6 | none |
| `security/**` | 028, 029, 030 | AC-008.*, TD-012 | PS-02 login + `/me` ADMIN |
| `audit/**` | 031 | DEC-008, AC-006.6 | PS-08 capacity PUT → `audit_log` +1 |
| `static/**` | 032, 033 | DEC-016 | manual (heatmap screenshot 2026-10-07) |
| `config/RequestIdFilter`, logback, runbook | 034, 035 | TD §7 | PS-06 counters present |

No path is outside the approved PLAN set except the UNRELATED tarball.

## 5. Baseline deviations — none silent, but four need human disposition

Each was committed with an explanatory message; they are listed here because Stage 03 must not let a baseline change pass unrecorded.

| ID | Deviation | Baseline affected | Disposition |
|---|---|---|---|
| **BD-01** | CSRF disabled on `/api/**` (`806efd8`) | 05 Security — CSRF on all except `/webhooks/jira` | **DECISION REQUIRED** (security) |
| **BD-02** | Log-layer secret-mask regex removed from `logback-spring.xml` (`54d13fa`); masking remains in `SecretMasker` for audit payloads only | PLAN-034 / F-SEC-02 "logback JSON + secret masking" | **DECISION REQUIRED** (security) |
| **BD-03** | Materialized views replaced by plain tables refreshed in Java (V006) | 03 DB Design §6 (`REFRESH MATERIALIZED VIEW CONCURRENTLY`) | Recorded in CYCLE_5 §1; **needs human acknowledgement** |
| **BD-04** | AC-006.1 interpretation: committed = remaining of issues **due before window end**, undated issues counted in every horizon (`b7de657`); daily "done" uses `jira_updated_at` as closed-at proxy | AC-006.1, AC-002.1 | ASSUMPTION — **needs BA confirmation** |
| BD-05 | Logout not audited (Spring Security 6 has no event) | PLAN-031 | minor — accept |
| BD-06 | Buildpacks → plain `Dockerfile` | PLAN-001 build path; PLAN-COND-02 still met (no Node) | minor — accept |
| BD-07 | SV-06 login lockout deferred | 05 Security | already deferred in CYCLE_6 — accept |

## 6. Evidence & gaps

| ID | Gap | Severity |
|---|---|---|
| **DV-C1** | No automated test execution for the candidate; no compile evidence inside the container (Maven Central blocked). Operator built `bootJar` successfully for `f620aec` (candidate−1). | HIGH — owned by 10-TESTING 04 |
| DV-C2 | `aiproject-cycle8-m7.tar.gz` committed (UNRELATED binary) | LOW — remove from repo |
| DV-C3 | TD-COND-01 not met on ngrok free (whole app reachable via tunnel) | HIGH (security) — operator: cloudflared or ngrok edge rule; stop tunnel when idle |
| DV-C4 | Worklog pages > 20 entries truncated | LOW — documented in `WorklogProjector` |
| DV-C5 | BD-01…BD-04 human disposition outstanding | see §5 |

## 7. Status

```
ACTUAL DIFF VERIFIED WITH CONDITIONS
```

Real implementation exists for all 36 PLAN steps; every changed file maps to an approved step except one UNRELATED binary; no secrets committed. Conditions: DV-C1 (execution evidence — produced by 10-TESTING), DV-C3 (tunnel narrowing), BD-01…BD-04 (human disposition).

Per the gate rule, Testing may start only after a human accepts these conditions. Requested acknowledgement text:

```
DIFF CONDITIONS ACCEPTED — BD-01 <accept|revert>, BD-02 <accept|revert>, BD-03 accept, BD-04 <accept|clarify>
```

---
## 8. Human disposition (2026-10-07 15:53 ICT)

```
DIFF CONDITIONS ACCEPTED — BD-01 accept, BD-02 accept, BD-03 accept, BD-04 accept
```
Recorded in `10-TESTING/TEST_EVIDENCE_APPROVAL_RECORD.md`. DV-C1 closed by 10-TESTING run 3 (78/78). DV-C2 (tarball) and DV-C3 (tunnel) remain open.
