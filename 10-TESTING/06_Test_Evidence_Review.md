# 06 — TEST EVIDENCE REVIEW
## ROLE
QA Lead / Test Evidence Reviewer.
## OBJECTIVE
Determine whether the current candidate has sufficient test evidence to pass Step 10 and proceed to Integration/E2E.
## PRECONDITIONS
Actual execution evidence exists for the required Step-10 scope.
## EXECUTION PROCEDURE
1. Build `REQ → AC → Jira → Actual Diff → Test ID → Execution → Result → Evidence` traceability.
2. Verify required test levels from Test Analysis were actually executed or explicitly dispositioned.
3. Check failures, skipped/not-run tests and accepted gaps.
4. Verify regression scope and affected permissions/security where applicable.
5. Confirm evidence belongs to the frozen candidate.
6. Separate Step-10 verification from Step-11 E2E verification.
7. Produce residual risk and E2E handoff package.
## GATE RULE
Only evidence can support verification. AI may recommend readiness; organizational QA authority owns the gate.
## OUTPUT SCHEMA
Traceability matrix; execution summary; gaps; unresolved failures; residual risk; E2E handoff; recommendation; status.
## ALLOWED FINAL STATUS
`READY FOR TEST EVIDENCE APPROVAL` / `TEST EVIDENCE INCOMPLETE` / `TESTING BLOCKED`
## HUMAN GATE
Authorized QA/QC may set `TEST EVIDENCE VERIFIED`.
## NEXT AUTHORIZED STAGE
`11-INTEGRATION-E2E` after gate approval.

## GOVERNANCE PRINCIPLES
- Work only from authoritative project/repository evidence.
- Never invent execution results, UI behavior, requirements, permissions, test coverage, screenshots or PASS status.
- `UNKNOWN` is preferable to an unsupported assumption.
- Preserve traceability to approved baselines and actual implementation.
- AI may prepare evidence and recommendations; required human gates remain human decisions.
