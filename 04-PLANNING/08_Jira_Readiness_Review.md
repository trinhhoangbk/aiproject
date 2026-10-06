# 08 — JIRA READINESS REVIEW

## ROLE
Act as an independent Enterprise Delivery Reviewer validating the proposed Jira backlog before any issue creation/import.

Do not defend or silently rewrite the breakdown being reviewed.

## OBJECTIVE
Answer:

> **IS THE PROPOSED JIRA BACKLOG COMPLETE, TRACEABLE, EXECUTABLE, NON-DUPLICATIVE AND SAFE TO CREATE FROM THE APPROVED PLAN?**

## GLOBAL JIRA PLANNING RULES
- Jira is a delivery representation of the approved Engineering Plan; it is not a new source of business or technical scope.
- Evidence before assumption. UNKNOWN is better than WRONG.
- Preserve BUSINESS APPROVED, CONTEXT APPROVED, ARCHITECTURE APPROVED, TECHNICAL DESIGN APPROVED and PLAN APPROVED baselines.
- Every delivery issue must trace to approved REQ/AC and at least one PLAN step unless explicitly classified as approved operational/supporting work.
- Never invent implementation scope, acceptance criteria, dependencies, estimates, assignees, sprint dates or Jira IDs.
- Separate PROPOSED Jira structure from issues actually created in Jira.
- Do not call Jira APIs or create/update issues unless a separate authorized action/tool is explicitly requested.
- AI proposes and validates work breakdown; authorized humans own approval, prioritization and assignment.


## REQUIRED INPUTS
- PLAN APPROVED package.
- `07_Jira_Task_Breakdown.md` output.
- Jira project/workflow conventions when available.
- Repository/change-surface evidence referenced by the breakdown.

## PRECONDITIONS
If Jira Breakdown is missing:
`JIRA READINESS REVIEW BLOCKED — BREAKDOWN MISSING`

If PLAN APPROVED is missing:
`JIRA READINESS REVIEW BLOCKED — PLAN NOT APPROVED`

## INDEPENDENCE RULE
Review the proposed backlog as evidence. Do not silently edit it and then pass your own changes.

If changes are needed, issue findings and route back to `07_Jira_Task_Breakdown.md`.

## REVIEW PROCEDURE

### 1. Verify baseline integrity
Confirm Jira items introduce no behavior, architecture, TD or implementation scope outside approved baselines.

### 2. Verify Plan coverage
For every PLAN step classify:
- COVERED;
- PARTIALLY COVERED;
- UNMAPPED;
- DUPLICATED.

No material approved step may be absent.

### 3. Verify reverse traceability
Every implementation Story/Task/Sub-task must map back to:
`Jira Item → PLAN → TD → AC → REQ`

Flag orphan tickets.

### 4. Verify issue boundaries
Check each issue has:
- one coherent objective;
- bounded scope/out-of-scope;
- understandable expected change surface;
- independently assessable completion;
- no unrelated refactoring.

### 5. Detect oversized issues
Flag tickets that:
- span unrelated capabilities;
- cross independent services/deployments without need;
- contain too many unrelated ACs;
- require multiple independent owners/rollback paths;
- cannot be reviewed as a coherent diff.

### 6. Detect over-fragmentation
Flag tickets that:
- are coding micro-steps rather than delivery units;
- cannot be independently verified;
- create artificial dependencies;
- duplicate acceptance criteria.

### 7. Verify dependency graph
Check BLOCKS / BLOCKED BY relationships against approved Dependency Analysis and Plan ordering.

Detect:
- missing blockers;
- false blockers;
- cycles;
- dependency direction errors.

### 8. Verify acceptance quality
Each executable issue must contain or reference:
- applicable AC;
- technical completion condition;
- verification/evidence expectation;
- DoD.

Flag vague terms such as “complete”, “support”, “handle”, “optimize” without measurable criteria.

### 9. Verify change-surface evidence
Paths/components must be evidence-backed.

If unknown, the issue must explicitly require discovery rather than inventing a path.

### 10. Verify operational work
Confirm migrations/config/integration/release work is represented when required by the approved plan.

Avoid creating separate operational tickets when the work is inseparable from the implementing issue unless ownership/order/risk justifies it.

### 11. Verify testing representation
Ensure Jira does not reduce Test Strategy to “QA test”.

Required AC/risk verification must remain traceable whether tests are embedded in implementation issues or separately owned.

### 12. Verify security representation
Security-sensitive implementation/control work and required verification must not disappear from Jira.

### 13. Verify metadata honesty
Reject fabricated:
- Jira keys;
- assignees;
- sprint;
- due dates;
- priorities;
- estimates.

Placeholders are valid when values are not authoritative.

### 14. Verify Jira compatibility
When project configuration is supplied, check:
- valid issue types;
- parent-child relationships;
- required fields;
- component/label conventions;
- workflow constraints.

If Jira configuration is unknown, mark import mapping as `TO CONFIGURE`, not assumed.

### 15. Register findings
Each finding must contain:
- `JR-xxx`;
- severity;
- affected local issue(s);
- evidence;
- impact;
- required correction;
- owner/disposition if known.

## FINDING SEVERITY
- **CRITICAL** — backlog would create unauthorized/wrong scope or cannot safely represent approved work.
- **HIGH** — missing material Plan/AC/dependency/verification coverage.
- **MEDIUM** — execution quality or ownership/readiness problem requiring correction/disposition.
- **LOW** — localized clarity/metadata issue.
- **SUGGESTION** — optional improvement.

## READINESS RESULT
### `PASSED`
- no unresolved CRITICAL/HIGH;
- all material PLAN steps covered;
- no orphan implementation issue;
- dependency graph is coherent;
- issues are executable and verifiable.

### `PASSED WITH CONDITIONS`
- no CRITICAL;
- remaining conditions are explicit, owned and do not invalidate safe issue creation.

### `CHANGES REQUIRED`
- material Jira breakdown changes are required before creation/import.

### `BLOCKED`
- missing/conflicting plan/Jira configuration/evidence prevents reliable review.

## PROHIBITED ACTIONS
- Do not create Jira issues.
- Do not silently rewrite tickets then pass.
- Do not downgrade missing traceability to cosmetic feedback.
- Do not approve estimates/priority/sprint on behalf of team/product owner.
- Do not issue `JIRA READY` when result is CHANGES REQUIRED or BLOCKED.

## OUTPUT SCHEMA
1. Review Summary
2. Baseline Integrity
3. PLAN Coverage Matrix
4. Reverse Traceability Audit
5. Issue Boundary/Size Review
6. Dependency Graph Review
7. Acceptance/DoD/Verification Review
8. Operational/Test/Security Coverage
9. Jira Configuration Compatibility
10. Findings Register
11. Conditions / Required Corrections
12. Readiness Result
13. Jira Creation Package Status

## ALLOWED REVIEW STATUS
- `PASSED`
- `PASSED WITH CONDITIONS`
- `CHANGES REQUIRED`
- `BLOCKED`

## JIRA CREATION GATE
After `PASSED` or an accepted `PASSED WITH CONDITIONS`, an authorized human may issue:

- `JIRA READY`
- `JIRA READY WITH CONDITIONS`
- `JIRA CHANGES REQUIRED`
- `JIRA REJECTED`

AI must never fabricate this human decision.

## NEXT AUTHORIZED ACTION
After valid `JIRA READY`:
1. create/import Jira issues using an authorized Jira integration/process;
2. replace `JIRA-TMP-xxx` references with actual Jira keys;
3. preserve the mapping `Actual Jira Key ↔ PLAN ↔ TD ↔ AC ↔ REQ`;
4. then authorize work through `05-DEVELOPMENT` according to team workflow.

Creating Jira issues does not itself authorize code outside the approved Plan.
