# 05 — TEST FAILURE ANALYSIS · Result

| Field | Value |
|---|---|
| Date | 2026-10-07 16:00 ICT |
| Role | Senior QA + Engineering Failure Analyst (AI) |
| Failures analysed | Run 1 (build) + Run 2 (3 tests) on candidate `3956cac` |
| **Status** | **🔴 `CODE BUG CONFIRMED`** (3) · `DEPENDENCY` issue (1, already fixed) |

## 1. Run 1 — `:compileTestJava` dependency resolution

| Field | Value |
|---|---|
| Output | `Could not find com.github.tomakehurst:wiremock-standalone:3.9.1. Required by: root project :` |
| Reproduction | Deterministic — the coordinate does not exist on Maven Central (WireMock 3.x is published under `org.wiremock`) |
| Trace | `app/build.gradle` test dependency since M0 (`3fadbfa`); latent because tests had never been executed |
| Classification | **DEPENDENCY ISSUE** (confidence: high) |
| Owner / action | Development — correct the coordinate. **Done** in `3956cac` (`org.wiremock:wiremock-standalone:3.9.1`); run 2 resolved it |

## 2. Run 2 — three failed tests

### F-1 · TC-AS-05 — Jira comment wording

| Field | Value |
|---|---|
| Expected (AC-006.4, DEC-008) | Internal comment `"Task reassigned via Resource Balancing Hub by [Manager Name]"` |
| Actual | `"Hub assignment — requested by <uuid>. Reason: …"` |
| Trace | `AssignmentService.adfComment` composed its own sentence and used the actor UUID, not the manager's name. Test data and mocks are faithful (manager display name supplied via `MemberRepository.findById`) |
| Classification | **CODE BUG** (confidence: high) — the test asserts the approved AC verbatim |
| Fix | `AssignmentService.actorName()` resolves the display name; comment starts with the AC sentence, then appends reason / pipeline |

### F-2 · TC-OD-09 — unestimated overdue issue

| Field | Value |
|---|---|
| Expected (AC-001.4, B-RULE-02) | Issue with no estimate counts as 0.5 MD = 4 h |
| Actual | `remaining_h = 0` |
| Trace | Jira sends `timeestimate = 0` (not `null`) for unestimated issues — observed live on KAN-26/27 (2026-10-07 15:00). `OverdueCalculator` only treated `null` as unestimated; `AllocationRateCalculator` was already fixed in `b7de657`, leaving the two calculators inconsistent |
| Classification | **CODE BUG** (confidence: high) |
| Fix | `OverdueCalculator` uses the shared `AllocationRateCalculator.effectiveRemaining`. Same root cause found statically in `WorkloadService` (SF-05: breakdown, overload and "unestimated" count) — fixed with the same helper; new test **TC-WL-06** added to cover it |

### F-3 · TC-AS-06 — Jira 403 surfaced as 500

| Field | Value |
|---|---|
| Expected (AC-006.5, 02 API CV-04) | HTTP **422** `application/problem+json` carrying Jira's message so the SPA can roll back |
| Actual | `JiraClientException` unhandled → `ServletException` (HTTP 500 in production) |
| Trace | `GlobalExceptionHandler` had no mapping for `JiraClientException`; the client itself maps 403 correctly (TC-JW-03 passed) |
| Classification | **CODE BUG** (confidence: high) |
| Fix | `GlobalExceptionHandler`: `JiraClientException` → 422 ProblemDetail (`jiraStatus` property); also `JiraRateLimitException` → 503 + `Retry-After` (previously also a 500) |

## 3. What was **not** changed

No test assertion was weakened, skipped or deleted. No acceptance criterion was edited. Fixes are confined to the three production classes above plus `WorkloadService` (SF-05) and one added test.

## 4. Route and retest scope

Per the playbook: CODE BUG → Development → `ACTUAL DIFF VERIFIED` for the fix → retest.

| Step | State |
|---|---|
| Development fix | committed (see git log after `3956cac`) |
| Diff verification of the fix | delta reviewed: 4 production files + 1 test file + docs, all mapped to AC-001.4 / AC-006.4 / AC-006.5; no other behaviour change → recorded in `06` |
| Retest scope | **full** `./gradlew test` (77 + 1 new = 78 cases) — run 3 by the operator |

## 5. Status

```
CODE BUG CONFIRMED  (F-1, F-2, F-3 — fixed, awaiting retest)
```
