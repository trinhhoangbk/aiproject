# 01 — TEST ANALYSIS · Result

| Field | Value |
|---|---|
| Date | 2026-10-07 15:55 ICT |
| Role | Senior QA / Test Architect (AI) |
| Candidate (frozen) | `6af57eb` on `main` (`github.com/trinhhoangbk/aiproject`) |
| Work items | Jira KAN backlog imported from `05-DEVELOPMENT/jira-import/jira-import.csv` (PLAN-001…036 ↔ KAN issues); traced here by PLAN id |
| Precondition | `ACTUAL DIFF VERIFIED WITH CONDITIONS` (`05-DEVELOPMENT/03_Actual_Diff_Verification_RESULT_run2.md`). **Human acceptance of the conditions is pending** — this analysis is prepared provisionally; it does not substitute for that acceptance |
| **Status** | **🟡 TEST ANALYSIS READY WITH GAPS** |

---

## 1. Scope

In scope for Step 10 (component-level, runnable on the operator's dev host with `./gradlew test`, no Docker):
business rules and calculators, service orchestration with mocked repositories, Jira write client against a mocked HTTP server, and Spring-MVC security slices for RBAC and error contracts.

Out of Step-10 scope → Step 11 E2E candidates: Kafka/Postgres round-trips, webhook-to-view latency, reconciler against Jira, SPA rendering, performance benchmark.

## 2. Test framework evidence (CONFIRMED from repository)

| Item | Evidence |
|---|---|
| Runner | JUnit 5 via `spring-boot-starter-test` (`app/build.gradle`), `useJUnitPlatform()` |
| Assertions / mocks | AssertJ, Mockito (`@ExtendWith(MockitoExtension.class)`) — see `CapacityResolverTest`, `WorkingDayCalculatorTest` |
| Time control | `Clock.fixed(..., ZoneId.of("Asia/Saigon"))` pattern in `CapacityResolverTest`; tests run with `user.timezone=UTC` (F-TD-01) |
| Web/security slices | `spring-security-test` on test classpath; `@WebMvcTest` available from starter |
| HTTP mock | `MockRestServiceServer` (spring-test) binds to `RestClient.Builder`; WireMock also on classpath |
| Integration infra | Testcontainers PG/Kafka on classpath — **not used in Step 10** (Colima socket config on the dev host unverified) |
| Command | `cd app && ./gradlew test` (no CI — ADR-ARCH-008) |
| Existing tests | 8 classes: Clock, CapacityResolver, AdaptiveCadence, HmacVerifier, AllocationBand, OverdueBand, TprBand, WorkingDayCalculator. **Never executed** for any recorded candidate |

## 3. Traceability: REQ → AC → PLAN → changed surface → test level

| REQ | AC | PLAN | Surface (candidate) | Level |
|---|---|---|---|---|
| REQ-001 | AC-001.2 week overload | 020 | `WorkloadService.overload` | Unit |
| REQ-001 | AC-001.3 day overload + DEC-010 | 020 | `WorkloadService.overload` + `LockDeadlineFlagRepository` | Unit |
| REQ-001 | AC-001.4 no-estimate 0.5 MD | 018, 020 | `AllocationRateCalculator.effectiveRemaining`, `OverdueCalculator` | Unit |
| REQ-001 | AC-001.5 capacity override | 009 | `CapacityResolver` | Unit (existing) |
| REQ-002 | AC-002.1 done list in D (ICT) | 022 | `DailyReportService` | Unit |
| REQ-002 | AC-002.2 worklog totals | 022 | `DailyReportService`, `WorklogProjector` | Unit |
| REQ-002 | AC-002.3 Discarded | 022 | `DailyReportService` | Unit |
| REQ-002 | AC-002.4 member scope | 022, 029 | `DailyReportController` `@PreAuthorize` | Web slice |
| REQ-003 | AC-003.1/.2 overdue + bands | 021 | `OverdueCalculator`, `OverdueBand` | Unit |
| REQ-003 | AC-003.4/.5 TPR + RWD=0 | 021 | `OverdueCalculator`, `TprBand` | Unit |
| REQ-004 | AC-004.1/.2 ETA | 023 | `EtaService` | Unit |
| REQ-005 | AC-005.1/.2 pipeline create/reject | 024 | `PipelineService`, `PipelineController` | Unit + Web slice |
| REQ-006 | AC-006.1/.2 AR + bands | 018 | `AllocationRateCalculator`, `AllocationBand` | Unit |
| REQ-006 | AC-006.3 suggestions | 025 | `BalancingService` | Unit |
| REQ-006 | AC-006.4/.5 assign + rollback | 026, 027 | `JiraWriteClient`, `AssignmentService`, `AssignmentController` | Unit + HTTP mock + Web slice |
| REQ-006 | AC-006.6 audit entry | 027, 031 | `AssignmentService` → `AuditService`; `SecretMasker` | Unit |
| REQ-007 | AC-007.1 webhook HMAC | 012 | `HmacVerifier` | Unit (existing) |
| REQ-007 | AC-007.1 worklog part | 022 | `WorklogProjector.toRows` | Unit |
| REQ-008 | AC-008.2/.3 RBAC | 029 | `WorkloadController`, `MemberController` | Web slice |

## 4. Scenario inventory (summary — expanded in 02)

Positive · negative · boundary · failure · authorization, per AC above. Highlights:
- Boundaries: AR 60/85/100 %, TPR 0.7/1.0, overdue 3/7 days, weekly cap 40 h, daily cap 8 h, window end exclusive.
- Failure: Jira 403 / 429 on write, comment failure after assignee success, unknown skill on pipeline, inactive assignee.
- Authorization: MEMBER → other member's workload (403), MEMBER → roster (403), anonymous → API (redirect / 401).
- Data quality: worklog entries without author / zero duration / non-numeric id; remaining=0 with no original.

## 5. Regression scope

All 8 existing test classes are re-run in the same `./gradlew test` invocation. Recently changed behaviour with regression risk: `AllocationRateCalculator` window filter (`b7de657`), `MaterializedViewRefresher` transaction scope (`f620aec`), `JiraEventConsumer` worklog wiring (`6af57eb`), `AllowListController` re-flag (`b7de657`).

## 6. Static findings during analysis (evidence-based, not yet executed)

These are flagged now so that test cases assert the **approved** behaviour; execution will confirm or refute them.

| ID | Finding | Evidence | Expected per baseline |
|---|---|---|---|
| SF-01 | Assign rollback returns HTTP 500, not 422 | `GlobalExceptionHandler` has no handler for `JiraClientException` | AC-006.5 + 02 API CV-04: 422 ProblemDetails |
| SF-02 | Jira comment text differs from AC | `AssignmentService.adfComment`: "Hub assignment — requested by <uuid>. Reason: …" | AC-006.4: "Task reassigned via Resource Balancing Hub by [Manager Name]" |
| SF-03 | B-RULE-02 inconsistent in overdue path | `OverdueCalculator` treats `remaining=0` as 0 h; `AllocationRateCalculator` treats 0-with-no-original as 4 h | AC-001.4: unestimated → 4 h everywhere |
| SF-04 | SPA workload screen reads fields the API does not return | `workload.js` reads `horizon/windowStart/overload.isOverloaded/projectBreakdown`; `WorkloadView` exposes `window/anchor/overload.flag/projects` | 02 API §5.2 — **Step 11 / UI** (not unit-testable here) |
| SF-05 | Unestimated "flag" for remaining=0 not set | `WorkloadService.unestimated` counts only `null` | AC-001.4 |

## 7. Data / environment needs

- Step 10: none beyond JDK 21 (Gradle foojay toolchain already resolves it on the dev host) and Maven Central access. No DB, no Kafka, no Docker.
- The AI container **cannot execute** (Maven Central / Gradle plugin portal blocked by egress proxy) → execution (stage 04) is performed by the operator.

## 8. E2E candidates for Step 11

AC-003.3 / AC-007.1 freshness ≤ 5 min · AC-007.2/.3 reconciler · Kafka dedup + monotonic UPSERT on Postgres · AllowList re-flag + MV refresh on a real DB · SPA screens (SF-04) · AC-009.1 performance · PS-09 real Jira write (dry-run off) · TD-COND-01 tunnel narrowing.

## 9. Gaps / blockers

| ID | Gap | Impact |
|---|---|---|
| TA-G1 | Diff-verification conditions not yet human-accepted | Analysis is provisional |
| TA-G2 | No in-container execution | Stage 04 depends on the operator |
| TA-G3 | Repository/Kafka/Postgres behaviour not covered at Step 10 | Deferred to Step 11 |
| TA-G4 | BD-04 (AC-006.1 interpretation) unconfirmed by BA | AR tests assert the implemented interpretation and are labelled so |

## 10. Status

```
TEST ANALYSIS READY WITH GAPS
```
Next: `02_Test_Case_Generation.md`.
