# 02 — TEST CASE GENERATION · Result

| Field | Value |
|---|---|
| Date | 2026-10-07 16:05 ICT |
| Role | Senior QA Engineer (AI) |
| Candidate | `6af57eb` |
| Input | `01_Test_Analysis_RESULT.md` (READY WITH GAPS) |
| **Status** | **🟡 TEST CASES READY WITH GAPS** |

Conventions: `Today` = Wed **2026-10-07** (Asia/Saigon) unless stated; standard day 8 h, week 40 h (GLOBAL tier). "Expected" is taken from the approved AC / API contract, **not** from the current code. Cases whose expected result contradicts a static finding (SF-xx) are marked ⚠ — they are expected to fail until Development fixes the code.

Level: **U** unit (Mockito) · **H** HTTP-mocked client · **W** Spring-MVC security slice · **E** Step-11 E2E candidate.

## 1. Test-case catalog

### Workload / overload — REQ-001
| ID | AC | Lvl | Given | When | Then |
|---|---|---|---|---|---|
| TC-WL-01 | AC-001.2 | U | active remaining 24 h + 20 h (44 h), weekly cap 40 h | overload computed | flag `red`, one reason `level=week`, committed 44 h, cap 40 h |
| TC-WL-02 | AC-001.2 | U | active 40 h exactly | overload computed | flag `green` (boundary: not > 40) |
| TC-WL-03 | AC-001.3 + DEC-010 | U | two issues due Fri 10-09, 6 h + 6 h, one priority `Blocker`, daily cap 8 h | overload | reason `level=day`, date 10-09, criteria contains `priority_blocker` |
| TC-WL-04 | AC-001.3 | U | same 12 h on 10-09, no hard-deadline criterion | overload | **no** day reason (can be rescheduled) |
| TC-WL-05 | AC-001.3 + DEC-010 crit 3 | U | 12 h on 10-09, issue key present in lock-deadline table | overload | day reason with `hub_lock_flag` |
| TC-WL-06 | AC-001.4 (SF-05) — *added after run 2* | U | remaining null / 0 without original / 0 with original 8 h / 8 h | isUnestimated | true / true / false / false |

### Allocation rate / heatmap — REQ-006, AC-001.4
| ID | AC | Lvl | Given | When | Then |
|---|---|---|---|---|---|
| TC-AR-01 | AC-006.1 (BD-04 interpretation) | U | this week (Mon 10-05..Sun 10-11), 5 WD; issues due 10-09 (16 h) and due 10-20 (40 h) | compute THIS_WEEK | committed 2.00 MD, standard 5.00, AR 40.00 %, band `dark_green` |
| TC-AR-02 | AC-006.1 | U | issue overdue (due 10-01, 8 h) + undated (8 h) | THIS_WEEK | committed 2.00 MD (both counted) |
| TC-AR-03 | AC-001.4 | U | remaining `null`; remaining 0 with no original; remaining 0 with original 8 h | effectiveRemaining | 4 h, 4 h, 0 h |
| TC-AR-04 | AC-006.2 | U | 44 h due this week | THIS_WEEK | AR 110.00 %, band `red` |
| TC-AR-05 | AC-006.1 edge | U | 0 working days in window | compute | AR 100 % (no divide-by-zero) |

### Overdue / early-warning — REQ-003
| ID | AC | Lvl | Given | When | Then |
|---|---|---|---|---|---|
| TC-OD-01 | AC-003.1/.2 | U | due 10-06 (1 day ago) | compute | row, daysOverdue 1, band `1_to_3_days`, tpr band `none` |
| TC-OD-02 | AC-003.2 | U | due 09-27 (10 days ago) | compute | band `more_than_a_week` |
| TC-OD-03 | AC-003.2 boundary | U | due 09-30 (7 days) | compute | band `4_to_7_days` |
| TC-OD-04 | AC-003.4 | U | due 10-09, 3 WD left, remaining 18 h → RDC 0.75 | compute | warning row, band `yellow` |
| TC-OD-05 | AC-003.4 | U | remaining 30 h → RDC 1.25 | compute | band `red` |
| TC-OD-06 | AC-003.4 | U | remaining 8 h → RDC 0.333 | compute | **no** row |
| TC-OD-07 | AC-003.5 | U | 0 working days left (due on a weekend day ≥ today) | compute | band `red` |
| TC-OD-08 | AC-003.1 | U | undated issue | compute | no row |
| ⚠ TC-OD-09 | AC-001.4 (SF-03) | U | overdue issue (due 10-06), remaining 0, no original estimate | compute | row stores the 0.5 MD placeholder: `remaining_h = 4.00` |

### Daily report — REQ-002
| ID | AC | Lvl | Given | When | Then |
|---|---|---|---|---|---|
| TC-DR-01 | AC-002.1 / DEC-002 | U | date 2026-10-06, member M | compile | repository queried with window `[2026-10-06T00:00+07:00, 2026-10-07T00:00+07:00)` |
| TC-DR-02 | AC-002.3 | U | done issues: one `Done`, one `Won't Fix` (discarded) | compile, excludeDiscarded=true | 1 row |
| TC-DR-03 | AC-002.3 | U | same | excludeDiscarded=false | 2 rows, the second flagged `discarded=true` |
| TC-DR-04 | AC-002.2 | U | worklogs 1 h + 2 h on KAN, 30 min on OPS for M | compile | totals KAN 3.00 h, OPS 0.50 h, grand 3.50 h |
| TC-DR-05 | AC-002.2 + O-11 | U | team-wide; worklog by non-roster account 5 h | compile (memberId=null) | non-roster hours excluded |

### ETA — REQ-004
| ID | AC | Lvl | Given | When | Then |
|---|---|---|---|---|---|
| TC-ETA-01 | AC-004.2 | U | last-10-WD worklog KAN 20 h, OPS 20 h → α_KAN 0.5; KAN remaining 40 h; 8 h/day | forecast | α 0.500, RWD 10, ETA **2026-10-21** (weekends skipped) |
| TC-ETA-02 | AC-004.1 | U | same | forecast | remaining 40.00 h, 5.00 MD |
| TC-ETA-03 | AC-004.2 edge | U | KAN remaining 16 h, no worklog at all | forecast | etaDate null, note `no_worklog_on_project_last_10_wd` |

### Pipeline — REQ-005
| ID | AC | Lvl | Given | When | Then |
|---|---|---|---|---|---|
| TC-PL-01 | AC-005.1 | U | valid request, skill BACKEND exists | create | saved with state `DRAFT`, createdBy = actor, one required-skill row |
| TC-PL-02 | AC-005.1 / DEC-007 | U | skill `COBOL` not in taxonomy | create | `IllegalArgumentException`; nothing saved |
| TC-PL-03 | AC-005.2 | W | MANAGER posts body without `name` | POST `/api/pipeline` | 400 ProblemDetail; service not called |

### Balancing — REQ-006
| ID | AC | Lvl | Given | When | Then |
|---|---|---|---|---|---|
| TC-BL-01 | AC-006.3 / B-RULE-04 | U | pipeline needs 5 MD BACKEND by 10-16 (window 10 WD); A: BACKEND, no load; B: no BACKEND; C: BACKEND, 72 h due in window; D: BACKEND, 16 h | suggest | candidates [A (10.00), D (8.00)] in that order; rejectedBySkill 1, rejectedByCapacity 1 |

### Jira write + assignment — REQ-006
| ID | AC | Lvl | Given | When | Then |
|---|---|---|---|---|---|
| TC-JW-01 | PLAN-COND-01 | H | dry-run = true | assignIssue | `dryRun=true`, **no** HTTP request sent |
| TC-JW-02 | AC-006.4 | H | dry-run = false, Jira 204 | assignIssue | PUT `/rest/api/3/issue/KAN-1/assignee` body `{"accountId":"acc-T"}`, outcome ok |
| TC-JW-03 | AC-006.5 | H | Jira 403 | assignIssue | `JiraClientException` with status 403 |
| TC-JW-04 | INT-R-02 | H | Jira 429 `Retry-After: 30` | assignIssue | `JiraRateLimitException`, retryAfter 30 s |
| TC-AS-01 | AC-006.4 | U | inactive target member | assign | `IllegalArgumentException`; Jira never called |
| TC-AS-02 | AC-006.4/.6 | U | dry-run outcomes | assign | view `dryRun=true`, previous assignee reported, audit `ASSIGNMENT` result OK |
| TC-AS-03 | AC-006.5/.6 | U | assignee call throws 403 | assign | exception propagates, audit FAILED recorded, comment **not** posted |
| TC-AS-04 | AC-006.4 | U | assignee OK, comment returns 500 | assign | assignment succeeds (Jira is SoR), audit OK |
| ⚠ TC-AS-05 | AC-006.4 (SF-02) | U | manager "Hoang Manager" assigns | assign | comment text contains `Task reassigned via Resource Balancing Hub by Hoang Manager` |
| ⚠ TC-AS-06 | AC-006.5 / CV-04 (SF-01) | W | service throws `JiraClientException(403)` | POST `/api/assignments` | **422** `application/problem+json` |

### Security / RBAC — REQ-008, AC-002.4
| ID | AC | Lvl | Given | When | Then |
|---|---|---|---|---|---|
| TC-SEC-01 | AC-008.2 | W | MEMBER M | GET `/api/workload/{M}` | 200 |
| TC-SEC-02 | AC-008.2 / CV-07 | W | MEMBER M | GET `/api/workload/{other}` | 403 |
| TC-SEC-03 | AC-008.1 | W | MANAGER | GET `/api/workload/{other}` | 200 |
| TC-SEC-04 | AC-008.3 | W | MEMBER | GET `/api/roster` | 403 |
| TC-SEC-05 | REQ-008 | W | anonymous | GET `/api/workload/{M}` | not 2xx (302 to login or 401) |
| TC-SEC-06 | AC-002.4 | W | MEMBER, no memberId | GET `/api/reporting/daily` | 403 |

### Data protection / ingestion
| ID | AC | Lvl | Given | When | Then |
|---|---|---|---|---|---|
| TC-SM-01 | 05 Security SV-04 | U | JSON with `password`, `apiToken`, `jira_webhook_secret` | mask | values replaced by `***`, other keys untouched |
| TC-SM-02 | SV-04 | U | `Authorization: Basic abc==` | mask | `Authorization: Basic ***` |
| TC-WP-01 | AC-002.2 / AC-007.1 | U | worklog page with valid, no-author, zero-duration, non-numeric-id entries | toRows | only the valid entry mapped (id, account, started, seconds, project) |
| TC-WP-02 | AC-007.1 | U | null page | toRows | empty |

## 2. Coverage check

| AC | Cases | Status |
|---|---|---|
| AC-001.1 | — | **uncovered at Step 10** (project-breakdown ratios) → E2E/UI (SF-04) |
| AC-001.2 / .3 / .4 / .5 | WL-01..06, AR-03, OD-09, existing CapacityResolverTest | covered |
| AC-002.1 / .2 / .3 / .4 | DR-01..05, WP-01, SEC-06 | covered |
| AC-003.1 / .2 / .4 / .5 | OD-01..08, existing band tests | covered |
| AC-003.3 | — | E2E |
| AC-004.1 / .2 | ETA-01..03 | covered |
| AC-005.1 / .2 | PL-01..03 | covered |
| AC-006.1 / .2 / .3 | AR-01..05, BL-01, existing AllocationBandTest | covered |
| AC-006.4 / .5 / .6 | JW-01..04, AS-01..06 | covered (2 ⚠) |
| AC-007.1 | existing HmacVerifierTest, WP-01/02 | partial — delivery latency E2E |
| AC-007.2 / .3 | existing AdaptiveCadenceTest | partial — E2E |
| AC-008.1 / .2 / .3 | SEC-01..05 | covered |
| AC-009.1 | — | benchmark gap |

## 3. Manual-only / E2E candidates
SPA rendering of every screen (SF-04), PS-09 real Jira write, tunnel narrowing, latency ≤ 5 min, reconciler, performance.

## 4. Status
```
TEST CASES READY WITH GAPS
```
Gaps: AC-001.1 (UI/E2E), AC-003.3, AC-007.2/.3, AC-009.1 deferred to Step 11; four ⚠ cases encode approved behaviour the code is not expected to meet yet.
Next: `03_Automated_Test_Implementation.md`.
