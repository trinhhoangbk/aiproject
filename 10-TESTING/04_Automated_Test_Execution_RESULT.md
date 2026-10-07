# 04 — AUTOMATED TEST EXECUTION · Result

| Field | Value |
|---|---|
| Date | 2026-10-07 15:46 ICT (run 2) |
| Role | Test Execution Engineer (AI analysis of operator-executed runs) |
| Candidate | `3956cac` = `6af57eb` + test sources (`ce275dd`, `807a163`) + build-file fix (`3956cac`). No production-code change between `6af57eb` and `3956cac` |
| Environment | Operator dev host: macOS 13 (Apple Silicon), JDK 21.0.12 via Gradle foojay toolchain, Gradle 8.10 wrapper, no Docker needed for this scope |
| Executed by | Operator (`hoang@Hoangs-MacBook-Pro`); AI container cannot reach Maven Central |
| **Status** | Run 2: **🔴 `AUTO TEST FAIL`** → 05 · Run 3 (candidate `ca8db44`): **🟢 `AUTO TEST PASS`** for the Step-10 scope → 06 |

## 1. Command (from repository evidence)

```
cd app && ./gradlew test --continue > ../10-TESTING/evidence/test-run-N.txt 2>&1
```
`build.gradle` → `useJUnitPlatform()`, `testLogging events passed/skipped/failed`.

## 2. Runs

| Run | Evidence file (operator host) | Exit | Result |
|---|---|---|---|
| 1 | `10-TESTING/evidence/test-run-1.txt` | 1 | **BLOCKED** at `:compileTestJava` — `Could not find com.github.tomakehurst:wiremock-standalone:3.9.1`. **No test executed** (NOT RUN ≠ PASS). Routed to 05 → DEPENDENCY ISSUE → fixed in `3956cac` |
| 2 | `10-TESTING/evidence/test-run-2.txt` (20 434 bytes) | 1 | **77 executed · 74 passed · 3 failed · 0 skipped**, 20 s |
| 3 (retest after `ca8db44`) | `10-TESTING/evidence/test-run-3.txt` (5 729 bytes) | **0** | **78 executed · 78 passed · 0 failed · 0 skipped** — `BUILD SUCCESSFUL in 8s`; `compileJava` + `compileTestJava` re-ran (fresh build of the fixed candidate) |

## 3. Run-2 results

| Suite | Executed | Passed | Failed |
|---|---|---|---|
| Existing 8 classes (Clock, CapacityResolver, AdaptiveCadence, HmacVerifier, 3 band tests, WorkingDayCalculator) | 26 | 26 | 0 |
| New Step-10 classes (12) | 51 | 48 | 3 |
| **Total** | **77** | **74** | **3** |

Failed Test IDs (exact output in the evidence file):

| Test ID | Assertion | Line |
|---|---|---|
| TC-AS-05 | expected text to contain `Task reassigned via Resource Balancing Hub by Hoang Manager`; actual `Hub assignment — requested by 406176b5-…. Reason: Cân đối tải` | `AssignmentServiceTest.java:121` |
| TC-OD-09 | expected `4.00`, was `0` | `OverdueCalculatorTest.java:113` |
| TC-AS-06 | `jakarta.servlet.ServletException: Request processing failed: JiraClientException: Assignee not permitted by workflow` (unhandled → would be HTTP 500, not 422) | `ApiSecurityWebTest` |

All three are the ⚠ cases predicted in `03` §3. Every other case — including RBAC (TC-SEC-01…06), dry-run gate (TC-JW-01), 403/429 mapping in the client (TC-JW-03/04), worklog projection (TC-WP-01/02) — passed.

## 4. Not run / skipped

None skipped. Out-of-scope items (Step 11) listed in `01` §8 were not executed and are **not** counted as passed.

## 5. Status

```
Run 2: AUTO TEST FAIL          → 05_Test_Failure_Analysis.md
Run 3: AUTO TEST PASS (78/78)  → 06_Test_Evidence_Review.md
```
