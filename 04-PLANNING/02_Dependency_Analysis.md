# 02 — DEPENDENCY ANALYSIS

## ROLE
Act as a Senior Engineer analyzing execution, build, runtime, data and organizational dependencies for the approved change surface.

## OBJECTIVE
Answer: **WHAT must exist, change, happen, or remain compatible for this implementation to succeed?**

## GLOBAL ENGINEERING RULES
- Evidence before assumption. UNKNOWN is better than WRONG.
- Preserve BUSINESS APPROVED, CONTEXT APPROVED, ARCHITECTURE APPROVED and TECHNICAL DESIGN APPROVED baselines.
- Use actual repository evidence; do not plan from filenames or assumptions alone.
- Maintain traceability: REQ → AC → Architecture → Technical Design → Plan Step → Verification.
- Do not write production code in Planning.
- Do not silently change an approved baseline to make the plan easier.
- If an approved baseline is invalid, STOP and request Baseline Change Control.
- AI proposes/reviews; authorized humans approve.


## REQUIRED INPUTS
- Completed Impact Analysis.
- Approved Technical Design.
- Actual dependency manifests/build files/config/contracts.
- Relevant service/module ownership evidence.

## PROCEDURE
1. Start from every DIRECT impact item.
2. Identify code/module/library dependencies.
3. Identify upstream and downstream runtime dependencies.
4. Identify API/contract/provider/consumer dependencies.
5. Identify data/schema/migration ordering dependencies.
6. Identify event/message/topic dependencies.
7. Identify infrastructure/config/secret/feature-flag dependencies.
8. Identify CI/CD/toolchain dependencies.
9. Identify external/vendor dependencies.
10. Identify team/approval/release coordination dependencies when evidenced.
11. Classify each dependency:
   - HARD — must be satisfied first;
   - SOFT — useful but not blocking;
   - RUNTIME — required during operation;
   - ORDERING — controls implementation/deployment sequence;
   - COMPATIBILITY — old/new versions must coexist;
   - UNKNOWN.
12. Detect cycles and incompatible ordering.
13. Define dependency verification evidence.
14. Build dependency graph and traceability.

## PROHIBITED
- Do not upgrade dependencies just because newer versions exist.
- Do not introduce new libraries/services.
- Do not create implementation plan yet.
- Do not assume external systems are available or compatible.

## STOP CONDITIONS
STOP for unresolved HARD dependency, unknown critical consumer/provider, incompatible migration order, or baseline conflict.

## OUTPUT SCHEMA
1. Dependency Summary
2. Dependency Matrix
3. Dependency Graph / Ordered Relationships
4. Hard Blockers
5. Compatibility Dependencies
6. Deployment/Migration Ordering Constraints
7. External/Organizational Dependencies
8. UNKNOWN / Risks
9. Traceability
10. Status

## ALLOWED STATUS
- `DEPENDENCY ANALYSIS COMPLETE`
- `DEPENDENCY ANALYSIS COMPLETE WITH OPEN ITEMS`
- `DEPENDENCY ANALYSIS BLOCKED`
- `BASELINE CHANGE REQUIRED`

## NEXT
`03_Implementation_Plan.md`
