# 04-PLANNING v5.2 — JIRA READY

v5.2 preserves the corrected v5.1 Engineering Planning prompts and adds an enterprise Jira delivery layer.

## Engineering Planning Flow

TECHNICAL DESIGN APPROVED
→ 01 Impact Analysis
→ 02 Dependency Analysis
→ 03 Implementation Plan
→ 04 Test Strategy
→ 05 Deployment & Rollback Plan
→ 06 Plan Review
→ HUMAN: PLAN APPROVED

## Jira Delivery Flow

PLAN APPROVED
→ 07 Jira Task Breakdown
→ 08 Jira Readiness Review
→ HUMAN: JIRA READY
→ Authorized Jira Create/Import
→ Actual Jira Keys mapped back to PLAN / TD / AC / REQ
→ 05-DEVELOPMENT

## Separation of responsibility
- 01 Impact = WHAT CAN CHANGE / BLAST RADIUS
- 02 Dependency = WHAT MUST EXIST / HAPPEN FIRST
- 03 Implementation Plan = WHAT TO CHANGE + ORDER
- 04 Test Strategy = HOW WE WILL PROVE IT WORKS
- 05 Deployment/Rollback = HOW TO RELEASE / RECOVER
- 06 Plan Review = SHOULD THE ENGINEERING PLAN BE APPROVED
- 07 Jira Task Breakdown = HOW TO REPRESENT THE APPROVED PLAN AS EXECUTABLE DELIVERY ISSUES
- 08 Jira Readiness Review = IS THAT BACKLOG SAFE, COMPLETE AND TRACEABLE ENOUGH TO CREATE

## Core traceability

REQ
→ AC
→ Architecture / Technical Design
→ PLAN-xxx
→ JIRA-TMP-xxx
→ Actual Jira Key
→ Actual Diff
→ Test Evidence
→ Review
→ Release

## Governance
Jira is not a new source of scope. It mirrors the approved Engineering Plan.

AI does not fabricate Jira IDs, assignments, estimates, sprint dates, priorities, PLAN APPROVED or JIRA READY.
