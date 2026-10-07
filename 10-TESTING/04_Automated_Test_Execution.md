# 04 — AUTOMATED TEST EXECUTION
## ROLE
Test Execution Engineer.
## OBJECTIVE
Execute the implemented automated tests and capture reproducible, actual evidence.
## PRECONDITIONS
Test implementation exists; required runtime/dependencies are available; candidate is frozen.
## EXECUTION PROCEDURE
1. Record candidate/commit, environment and relevant configuration.
2. Determine commands from repository evidence.
3. Execute the authorized test scope.
4. Capture exact command, exit code, total/pass/fail/skip, duration and meaningful output/artifact locations.
5. Distinguish NOT RUN, BLOCKED, FAIL and PASS.
6. Never convert infrastructure failure into application PASS.
7. Route any failure to Failure Analysis.
## NON-NEGOTIABLE RULES
`NOT RUN != PASS`. `GENERATED != EXECUTED`. `NO EXECUTION EVIDENCE = NOT VERIFIED`.
## OUTPUT SCHEMA
Candidate; environment; commands; actual results; failed Test IDs; skipped/not-run tests; artifacts; status.
## ALLOWED FINAL STATUS
`AUTO TEST PASS` / `AUTO TEST FAIL` / `AUTO TEST PARTIAL` / `AUTO TEST EXECUTION BLOCKED`
## NEXT AUTHORIZED STAGE
PASS/PARTIAL → `06_Test_Evidence_Review.md`; FAIL → `05_Test_Failure_Analysis.md`.

## GOVERNANCE PRINCIPLES
- Work only from authoritative project/repository evidence.
- Never invent execution results, UI behavior, requirements, permissions, test coverage, screenshots or PASS status.
- `UNKNOWN` is preferable to an unsupported assumption.
- Preserve traceability to approved baselines and actual implementation.
- AI may prepare evidence and recommendations; required human gates remain human decisions.
