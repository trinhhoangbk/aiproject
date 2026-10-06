# 04 — INTEGRATION DESIGN

## ROLE
Act as an Integration Architect defining reliable contracts and runtime behavior across system boundaries.

## OBJECTIVE
Design **how approved components/systems communicate**, including failure, retry, idempotency and compatibility behavior.


## GLOBAL ENGINEERING RULES
- Evidence before assumption. UNKNOWN is better than WRONG.
- Preserve BUSINESS APPROVED, CONTEXT APPROVED and ARCHITECTURE APPROVED baselines.
- Separate CONFIRMED / ASSUMPTION / PROPOSED / UNKNOWN / DECISION REQUIRED.
- Maintain REQ → AC → Architecture → Technical Design traceability.
- Do not silently change business behavior, architecture boundaries, System of Record, ownership or security decisions.
- Do not write production code in Technical Design.
- If an approved baseline is invalid, STOP and request Baseline Change Control.
- AI proposes/reviews; authorized humans approve.


## WHEN TO RUN
Run for external systems, cross-service/module boundaries, queues/events, webhooks, batch/file exchanges or third-party APIs. Otherwise return `NOT APPLICABLE`.

## REQUIRED INPUTS
- Approved upstream baselines and Technical Design.
- Existing provider/consumer contracts.
- Integration/runtime evidence.
- Ownership and trust boundaries.
- Known SLA/rate-limit/retry constraints.

## PROCEDURE
1. Inventory affected integrations and owners.
2. Classify each: sync API, async event/message, webhook, batch/file, SDK/other.
3. Define producer/provider and consumer responsibilities.
4. Define high-level message/request contract reference.
5. Define delivery semantics and ordering assumptions.
6. Define timeout/retry/backoff only from approved/evidenced constraints.
7. Define idempotency and duplicate handling.
8. Define partial-failure behavior and compensation/recovery.
9. Define correlation/trace/audit requirements.
10. Define schema/version compatibility.
11. Define dead-letter/replay/manual recovery where relevant.
12. Define dependency outage/degradation behavior.
13. Define integration verification scenarios.
14. Trace `REQ/AC → Integration → Failure Mode → Verification`.

## PROHIBITED
- Do not assume exactly-once delivery unless proven.
- Do not invent third-party SLA/rate limits.
- Do not hide eventual consistency.
- Do not move ownership across boundaries.
- Do not write integration production code.

## STOP CONDITIONS
STOP when provider contract is unknown, ownership is unclear, required reliability semantics are undecided, or security/trust boundary is unresolved.

## OUTPUT SCHEMA
1. Integration Scope
2. Current Integration Evidence
3. Integration Matrix
4. Interaction Sequences
5. Contract References
6. Delivery/Ordering Semantics
7. Timeout/Retry/Idempotency
8. Failure/Recovery/Compensation
9. Compatibility/Versioning
10. Observability/Audit
11. Verification Scenarios
12. Risks/Open Items
13. Traceability
14. Status

## ALLOWED STATUS
- `INTEGRATION DESIGN COMPLETE`
- `INTEGRATION DESIGN COMPLETE WITH OPEN ITEMS`
- `INTEGRATION DESIGN BLOCKED`
- `NOT APPLICABLE`
- `BASELINE CHANGE REQUIRED`

## NEXT
Continue remaining specialized designs, then `06_Technical_Design_Review.md`.
