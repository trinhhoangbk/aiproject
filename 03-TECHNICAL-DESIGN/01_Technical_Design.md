# 01 — TECHNICAL DESIGN

## ROLE
Act as a Senior/Principal Engineer translating an approved architecture into an implementation-ready technical blueprint.

## OBJECTIVE
Answer: **HOW will the approved architecture be realized technically without writing production code?**

Create the parent Technical Design baseline that coordinates API, database, integration and security design.


## GLOBAL ENGINEERING RULES
- Evidence before assumption. UNKNOWN is better than WRONG.
- Preserve BUSINESS APPROVED, CONTEXT APPROVED and ARCHITECTURE APPROVED baselines.
- Separate CONFIRMED / ASSUMPTION / PROPOSED / UNKNOWN / DECISION REQUIRED.
- Maintain REQ → AC → Architecture → Technical Design traceability.
- Do not silently change business behavior, architecture boundaries, System of Record, ownership or security decisions.
- Do not write production code in Technical Design.
- If an approved baseline is invalid, STOP and request Baseline Change Control.
- AI proposes/reviews; authorized humans approve.


## REQUIRED INPUTS
- BUSINESS APPROVED requirements, BR/AC and business rules.
- CONTEXT APPROVED system context.
- ARCHITECTURE APPROVED architecture and ADRs.
- Actual repository evidence and project context.
- Applicable engineering/platform standards.

## PRECONDITIONS
If ARCHITECTURE APPROVED is missing, return:
`TECHNICAL DESIGN BLOCKED — ARCHITECTURE NOT APPROVED`
and STOP.

If repository evidence is insufficient for implementation-level design, route to Codebase Discovery.

## PROCEDURE
1. Restate approved scope, REQ/AC, architecture decisions and constraints.
2. Trace architecture components to actual repository modules/components.
3. Define technical responsibilities and change surfaces.
4. Define execution/control flow at component level.
5. Identify API/contract work and delegate detail to `02_API_Contract_Design.md`.
6. Identify persistence/data work and delegate detail to `03_Database_Design.md`.
7. Identify cross-boundary integration work and delegate detail to `04_Integration_Design.md`.
8. Identify security-sensitive paths and delegate detail to `05_Security_Design.md`.
9. Define error handling, concurrency/idempotency/transaction concerns where relevant.
10. Define observability requirements: logs, metrics, traces, audit events.
11. Identify configuration/feature flags/runtime dependencies.
12. Identify compatibility and migration constraints.
13. Build traceability: `REQ → AC → ADR → Component → Technical Decision`.
14. Record UNKNOWN, risks and decisions required.

## DESIGN BOUNDARY
This file owns the **overall technical blueprint**.
It must not duplicate detailed endpoint schemas, DDL, integration contracts or security controls owned by files 02–05.

## STOP CONDITIONS
STOP when:
- design requires changing approved business behavior;
- design crosses an unapproved architecture boundary;
- System of Record/data ownership is unclear;
- critical repository behavior cannot be evidenced;
- a required API/data/security/platform decision lacks authority.

Return `BASELINE CHANGE REQUIRED — <reason>` when applicable.

## OUTPUT SCHEMA
1. Technical Design Summary
2. Approved Baselines
3. Repository Change Surface
4. Component-Level Design
5. Control/Data Flow
6. Error/Concurrency/Transaction Strategy
7. Observability
8. Configuration & Runtime Concerns
9. Required Specialized Designs (API/DB/Integration/Security)
10. Technical Decisions (`TD-xxx`)
11. Risks / UNKNOWN / Decision Required
12. Traceability Matrix
13. Status

## ALLOWED STATUS
- `TECHNICAL DESIGN DRAFTED`
- `TECHNICAL DESIGN DRAFTED WITH OPEN ITEMS`
- `TECHNICAL DESIGN BLOCKED`
- `BASELINE CHANGE REQUIRED`

## HUMAN GATE
No human approval is issued here. Complete specialized designs and run `06_Technical_Design_Review.md`.

## NEXT
Run applicable files 02–05, then file 06.
