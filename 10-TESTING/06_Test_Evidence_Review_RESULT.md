# 06 — TEST EVIDENCE REVIEW · Result

| Field | Value |
|---|---|
| Date | 2026-10-07 16:10 ICT |
| Role | QA Lead / Test Evidence Reviewer (AI — recommends; QA authority decides) |
| Frozen candidate | **`ca8db44`** (`main`) |
| Evidence | `10-TESTING/evidence/test-run-1.txt`, `-2.txt`, `-3.txt` on the operator host · `04`/`05` results |
| **Status** | **🟡 `READY FOR TEST EVIDENCE APPROVAL`** → **✅ `TEST EVIDENCE VERIFIED`** by the operator 2026-10-07 15:53 (see `TEST_EVIDENCE_APPROVAL_RECORD.md`) |

## 1. Evidence belongs to the frozen candidate

Run 3 recompiled `compileJava` and `compileTestJava` after `git pull` to `ca8db44` and executed 78 tests including the three that failed on `3956cac` and the new TC-WL-06 → the passing evidence is for the fixed code, not a stale build.

Delta `3956cac → ca8db44` (fix diff, reviewed per 05 §4): `AssignmentService`, `OverdueCalculator`, `WorkloadService`, `GlobalExceptionHandler`, one test file, docs. Each maps to AC-006.4 / AC-001.4 / AC-006.5; no API path, DB schema, security rule or unrelated behaviour changed → **fix diff verified**.

## 2. Traceability — REQ → AC → PLAN/Jira → Test → Run 3

| REQ | AC | PLAN | Test IDs | Run 3 |
|---|---|---|---|---|
| REQ-001 | AC-001.2 / .3 | 020 | TC-WL-01…05 | 5/5 ✅ |
| REQ-001 | AC-001.4 | 018, 020, 021 | TC-AR-03, TC-OD-09, TC-WL-06 | 3/3 ✅ |
| REQ-001 | AC-001.5 | 009 | CapacityResolverTest (4) | 4/4 ✅ |
| REQ-002 | AC-002.1 / .2 / .3 | 022 | TC-DR-01…05, TC-WP-01/02 | 7/7 ✅ |
| REQ-002 | AC-002.4 | 022, 029 | TC-SEC-06 | ✅ |
| REQ-003 | AC-003.1 / .2 / .4 / .5 | 021 | TC-OD-01…08, OverdueBandTest, TprBandTest | 15/15 ✅ |
| REQ-004 | AC-004.1 / .2 | 023 | TC-ETA-01…03 | 3/3 ✅ |
| REQ-005 | AC-005.1 / .2 | 024 | TC-PL-01…03 | 3/3 ✅ |
| REQ-006 | AC-006.1 / .2 | 018 | TC-AR-01/02/04/05, AllocationBandTest | 8/8 ✅ |
| REQ-006 | AC-006.3 | 025 | TC-BL-01 | ✅ |
| REQ-006 | AC-006.4 / .5 / .6 | 026, 027 | TC-JW-01…04, TC-AS-01…06 | 10/10 ✅ |
| REQ-007 | AC-007.1 (signature, worklog mapping) | 012, 022 | HmacVerifierTest, TC-WP-01/02 | ✅ |
| REQ-007 | TD-COND-04 cadence | 016 | AdaptiveCadenceTest | 3/3 ✅ |
| REQ-008 | AC-008.1 / .2 / .3 | 029 | TC-SEC-01…05 | 5/5 ✅ |
| — | DEC-002 clock | 003 | ClockConfigTest | ✅ |
| — | SV-04 secret masking (audit) | 031 | TC-SM-01/02 | 2/2 ✅ |
| — | B-RULE-01 working days | 018 | WorkingDayCalculatorTest | 4/4 ✅ |

**Execution summary (run 3): 78 executed · 78 passed · 0 failed · 0 skipped · exit 0.**

## 3. Required levels from `01` — executed or dispositioned

| Level | Disposition |
|---|---|
| Unit | executed |
| HTTP-mocked client | executed (`MockRestServiceServer`) |
| Web / security slice | executed (`@WebMvcTest` + real `SecurityConfig`) |
| Repository / DB, Kafka, Jira round-trip | **deferred to Step 11** (explicit, `01` §8) |
| UI | **deferred to Step 11** |
| Performance (AC-009.1) | **gap** — benchmark not defined |

## 4. Failures history

| Run | Outcome | Classification | Resolution |
|---|---|---|---|
| 1 | build blocked | DEPENDENCY (WireMock coordinate) | fixed `3956cac` |
| 2 | 3 failed | CODE BUG ×3 (F-1 comment wording, F-2 B-RULE-02 on 0 estimate, F-3 500 instead of 422) | fixed `ca8db44` |
| 3 | 0 failed | — | — |

No test was weakened, skipped or removed between runs.

## 5. Open conditions / residual risk (must be visible to the approver)

| ID | Item | Severity | Owner |
|---|---|---|---|
| C-1 | **Diff-verification conditions BD-01…BD-04 not yet human-accepted** (CSRF off on `/api/**`, log-layer masking removed, MV→tables, AC-006.1 interpretation) | HIGH — governance | Product owner / Security |
| C-2 | **SF-04** SPA workload screen reads fields the API does not return (`horizon`, `windowStart`, `overload.isOverloaded`, `projectBreakdown`) → that screen renders blanks | MEDIUM — UI defect, not covered at Step 10 | Development → verify in Step 11 |
| C-3 | DB/Kafka paths (UPSERT guard, dedup, allow-list re-flag, MV refresh transaction, worklog persistence) have **live smoke evidence only**, no automated test | MEDIUM | Step 11 (Testcontainers on Colima) |
| C-4 | TD-COND-01 not met on ngrok free tier | HIGH — security | Operator |
| C-5 | Worklog pages > 20 entries truncated | LOW | backlog |
| C-6 | AC-009.1 performance unmeasured | MEDIUM | Step 11 / benchmark |
| C-7 | Evidence files and the 10-TESTING playbook live on the operator host, not yet committed | LOW — traceability | Operator: `git add 10-TESTING && git commit` |

## 6. E2E handoff package (Step 11)

Candidate `ca8db44` · stack via `deploy/docker-compose.yml` · smoke driver `scripts/smoke.sh` (PS-01…08) · scope: C-2, C-3, C-6, AC-003.3 / AC-007.1 freshness ≤ 5 min, AC-007.2/.3 reconciler, PS-09 real Jira write with `JIRA_WRITE_DRY_RUN=false` on a non-production project, TD-COND-01 tunnel narrowing.

## 7. Recommendation

The Step-10 scope defined in `01` has complete, reproducible, passing execution evidence on the frozen candidate, with every failure classified and resolved without weakening tests. **Recommend `TEST EVIDENCE VERIFIED`** for Step 10, **conditional on C-1** (human disposition of BD-01…BD-04), and carrying C-2…C-6 into Step 11.

```
READY FOR TEST EVIDENCE APPROVAL
```

### Human gate
Authorised QA/QC sets:
```
TEST EVIDENCE VERIFIED
```
(after recording C-1). Next authorised stage: `11-INTEGRATION-E2E`.
