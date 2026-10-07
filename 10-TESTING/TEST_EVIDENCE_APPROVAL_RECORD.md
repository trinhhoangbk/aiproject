# TEST EVIDENCE · Approval Record

| Field | Value |
|---|---|
| Decision date | 2026-10-07 15:53 ICT |
| Decisions | **`DIFF CONDITIONS ACCEPTED`** (05-DEVELOPMENT/03 run 2) · **`TEST EVIDENCE VERIFIED`** (10-TESTING Step 10) |
| Authorized by | Project owner / operator (user) — in-session statement, verbatim below |
| Candidate | `ca8db44` on `main` (`github.com/trinhhoangbk/aiproject`) |

```
DIFF CONDITIONS ACCEPTED — BD-01 accept, BD-02 accept, BD-03 accept, BD-04 accept
TEST EVIDENCE VERIFIED
```

## 1. Baseline deviations accepted (now part of the approved baseline)

| ID | Accepted deviation | Superseded baseline text |
|---|---|---|
| BD-01 | CSRF disabled on `/api/**` (same-origin SPA, session cookie); still enabled for the SPA shell; `/webhooks/jira` remains HMAC-only | 05 Security: CSRF on all endpoints except `/webhooks/jira` |
| BD-02 | No regex secret-masking in the log layout; secrets masked in audit payloads by `SecretMasker`; code must not log secrets | PLAN-034 / F-SEC-02 "logback JSON + secret masking" |
| BD-03 | `mv.mv_allocation_rate` / `mv.mv_member_overdue` are plain tables refreshed in Java (V006), not materialized views | 03 DB Design §6 `REFRESH MATERIALIZED VIEW CONCURRENTLY` |
| BD-04 | AC-006.1 "Committed in W" = remaining of issues due before the window end; overdue and undated issues count in every horizon. Daily "done" uses `jira_updated_at` as closed-at proxy | AC-006.1, AC-002.1 (interpretation) |

## 2. Approved test evidence

- `01_Test_Analysis_RESULT.md` — READY WITH GAPS
- `02_Test_Case_Generation_RESULT.md` — 52 cases (incl. TC-WL-06)
- `03_Automated_Test_Implementation_RESULT.md` — 12 new classes
- `04_Automated_Test_Execution_RESULT.md` — run 3: **78/78 passed**, exit 0
- `05_Test_Failure_Analysis_RESULT.md` — 1 dependency issue + 3 code bugs, all resolved
- `06_Test_Evidence_Review_RESULT.md` — READY FOR TEST EVIDENCE APPROVAL
- Raw evidence: `10-TESTING/evidence/test-run-{1,2,3}.txt` (operator host)

## 3. Carried into Step 11 (not closed by this approval)

C-2 SPA workload field mismatch (SF-04) · C-3 DB/Kafka paths without automated tests · C-4 TD-COND-01 tunnel narrowing · C-5 worklog page > 20 · C-6 AC-009.1 performance · C-7 commit evidence files.

## 4. Next authorized stage

`11-INTEGRATION-E2E`.
