# 03 — ARCHITECTURE REVIEW · Result

| Field | Value |
|---|---|
| Stage | 02-ARCHITECTURE_FINAL / 03 Architecture Review |
| Date | 2026-10-04 15:54 ICT |
| Role | Independent Architecture Reviewer / ARB (AI) |
| Preconditions | BUSINESS APPROVED ✅ · CONTEXT APPROVED ✅ · ARCHITECTURE PROPOSED (from 02 file) ✅ |
| **Review result** | **🟡 `PASSED WITH CONDITIONS`** |

> Independence rule honoured: I did not edit the design during review. Every finding below cites an evidence line in a previous artifact.

---

## 1. Review Summary

The proposed architecture is coherent, bounded appropriately and traceable to the approved REQ/AC/Rule baseline. No `CRITICAL` or `HIGH` finding. Three `MEDIUM` findings relate to open conditions carried from CONTEXT; four `LOW` and three `SUGGESTION` findings refine readiness for Technical Design. No `BLOCKER`.

The architecture is **ready for human approval with conditions**: the conditions are the human decisions needed on O-03, O-04 and O-09 (which anchor ADR-ARCH-010 and ADR-ARCH-011 and the audit retention sizing).

## 2. Baseline Integrity

| Check | Result | Evidence |
|---|---|---|
| REQ list cited in Architecture Design matches BUSINESS APPROVED | ✅ Match | 02 Design §2 ↔ `BUSINESS_APPROVAL_RECORD.md` §1.3 |
| Rules cited match 02 Clarification + Business Decision Log | ✅ Match | 02 Design §2 ↔ 02 Clarification §A |
| Context constraints C-01…C-17 and open items O-03…O-11 all enumerated | ✅ Match | 02 Design §13 ↔ CONTEXT APPROVAL_RECORD §Carried |
| No stale references to pre-approval text | ✅ Clean | — |

## 3. Requirement Coverage

| REQ | AC coverage in Design | Verdict |
|---|---|---|
| REQ-001 | ADR-ARCH-002/003/009 + Domain Services + materialized.allocation_rate | COVERED |
| REQ-002 | ADR-ARCH-001/003/005 + issue_projection + worklog_projection | COVERED |
| REQ-003 | ADR-ARCH-005/009 + materialized.member_overdue + Early-Warning | COVERED (see F-02 for a nuance) |
| REQ-004 | ADR-ARCH-003 + Domain Services ETA with α over 10 WD (F-03 of BUSINESS APPROVED) | COVERED |
| REQ-005 | pipeline_project schema + skill_taxonomy | COVERED |
| REQ-006 | ADR-ARCH-004/006/009/010 + Jira Write Client + audit_log | COVERED |
| REQ-007 | ADR-ARCH-001/004/005 | COVERED |
| REQ-008 | ADR-ARCH-007 + Spring Security + data-level filter | COVERED |
| REQ-009 | ADR-ARCH-002/003/009 | COVERED (see F-04 — needs capacity analysis in Tech Design) |

No `REQ NOT COVERED`. No `UNAUTHORIZED BEHAVIOR` detected.

## 4. Review Dimensions

| Dimension | Verdict | Notes |
|---|---|---|
| Component boundaries | PASS | Modular monolith with clear capability packages; adapter isolates Jira. |
| Data ownership | PASS | Jira SoR respected; Hub-owned schemas match 04 Domain §H. |
| Integration coupling | PASS | Sync write-back only where DEC-008 mandates it; everything else async via Kafka. |
| Failure behaviour | PASS | Dual-path sync (webhook + reconcile); DLQ for poison messages; UI rollback on write-back failure. |
| Scalability | PASS | Sized for 10–50 members; monolith adequate. |
| Performance | PASS WITH NOTE | Materialized views are the right lever; refresh trigger specifics belong to Tech Design (F-04). |
| Security / trust | PASS WITH CONDITIONS | ADR-ARCH-010 depends on O-03 answer; secret backend O-10 open. |
| Observability | PASS | Named metrics; monitor Kafka lag and Jira error rate. |
| Operability | ACCEPTED | No CI/CD per C-17; manual-release risk acknowledged in R-ARCH-08. |
| Maintainability | PASS | Adapter package = single swap point for Jira edition. |
| Recoverability | PASS | Reconciler can rebuild projection from Jira. |

## 5. Findings Register

| ID | Severity | Artifact | Issue | Impact | Required action | Owner/Stage |
|---|---|---|---|---|---|---|
| F-01 | MEDIUM | ADR-ARCH-010 | Jira auth model is proposed (API-Token service account) but O-03 is open. If approver prefers OAuth 3LO, the Jira Write Client and secret storage change. | Design shape locally; no re-architecture. | Human decision at Architecture gate; then revise ADR-ARCH-010. | Approver |
| F-02 | MEDIUM | ADR-ARCH-011 | Deployment target is deferred (O-04). HA/availability design (R-ARCH-06) cannot be finalised. | Non-functional readiness. | Human decision at Architecture gate. | Approver |
| F-03 | MEDIUM | §7 `audit_log` | Retention duration (O-09 / DEC-013) still unset. Storage sizing is unknown. | Operational / cost. | Decide retention; architecture already supports configurable TTL. | Approver |
| F-04 | LOW | ADR-ARCH-009 | Which Jira events trigger which view refresh is left to Tech Design. Risk: event storm triggers thrash. | Perf risk under spike. | Tech Design: specify debounce window per materialized view. | Technical Design |
| F-05 | LOW | §6 Interaction Model | `hub.sync.commands` topic is listed but its command set (what the Scheduler publishes vs. what the Reconciler reads) is not enumerated. | Clarity. | Tech Design: enumerate commands. | Technical Design |
| F-06 | LOW | §4 Audit Service | Audit records "capacity edits" in general, but the exact event shape (before/after values) is not described. | Audit completeness. | Tech Design: define capacity-edit audit payload. | Technical Design |
| F-07 | LOW | §8 Security | HMAC-webhook secret rotation procedure is implied, not specified. | Operational. | Documented procedure in ops runbook. | Operations |
| F-08 | SUGGESTION | §4 Reconciler | Consider adding a `freshness_watermark` column to track last successful full-sync per project; surfaced in UI "freshness badge". | UX. | Tech Design option. | Technical Design |
| F-09 | SUGGESTION | ADR-ARCH-004 | Single-partition topics are fine at 50 members; add a rule-of-thumb for when to repartition (e.g. consumer lag > 60 s sustained). | Future-proofing. | Document in ops runbook. | Operations |
| F-10 | SUGGESTION | §5 Boundary Model | Non-roster Jira worklog (O-11) is excluded from team-MD but it is unclear whether such worklog is visible at all in `worklog_projection`. The design treats it as "projected, not counted". Reviewer agrees this is the safe default; confirm at gate. | Business-view correctness. | Confirm at Architecture gate. | Approver |

No CRITICAL or HIGH findings.

## 6. Architecture Decision Review

| ADR | Verdict | Comment |
|---|---|---|
| ADR-ARCH-001 (Jira Cloud / v3) | ✅ Sound | ADF handling isolated in adapter. |
| ADR-ARCH-002 (Modular monolith) | ✅ Sound | Right size; playbook-aligned. |
| ADR-ARCH-003 (Single Postgres) | ✅ Sound | No evidence to justify extra stores. |
| ADR-ARCH-004 (Kafka bus) | ✅ Sound | Even for scale this low, Kafka buys durability + decoupling. |
| ADR-ARCH-005 (Webhook + reconcile) | ✅ Sound | Required by REQ-007 wording. |
| ADR-ARCH-006 (Sync write-back) | ✅ Sound | Required by DEC-008. |
| ADR-ARCH-007 (RBAC in depth) | ✅ Sound | Method annotations + data filter is standard best practice. |
| ADR-ARCH-008 (No CI/CD v1) | ✅ Accepted | User-set; operational risk acknowledged. |
| ADR-ARCH-009 (Materialized views) | ✅ Sound | Fits perf SLO; trigger spec deferred (F-04). |
| ADR-ARCH-010 (API-Token auth) | ⚠️ Proposed | Depends on O-03 (F-01). |
| ADR-ARCH-011 (Deployment deferred) | ⚠️ Proposed | Depends on O-04 (F-02). |

## 7. Traceability Review

Spot-checks:
- REQ-002 → `statusCategory.key='done'` (DEC-001) → Issue Event Consumer updates `issue_projection.status_category` → UI query → **PASS**.
- REQ-006 heatmap → DEC-011 bands → `materialized.allocation_rate.band` enum derived at refresh time → UI reads band directly → **PASS**.
- DEC-008 → Jira Write Client flow: `PUT assignee` + `POST comment (ADF)` + audit event → **PASS**.
- DEC-010 "Lock Deadline" (criterion 3) → `lock_deadline_flag` table → considered in Overload calculation → **PASS**.
- DEC-003 Holiday Calendar ownership → `holiday_calendar` writers = Admin/Ops Lead → **PASS**.
- DEC-009 TPR math → Domain Service formula with Member standard daily hours per DEC-004 tier → **PASS**.

No traceability gaps.

## 8. Conditions / Open Decisions (routed to approver)

| ID | Decision needed | Default if not supplied |
|---|---|---|
| C-APP-01 | **O-03 Jira auth**: API Token (ADR-ARCH-010 default) / OAuth 3LO / OAuth 2LO | API Token service-account |
| C-APP-02 | **O-04 deployment**: on-prem K8s / on-prem VM / private cloud | Container-based, host TBD |
| C-APP-03 | **O-09 audit retention** duration (e.g. 1 y / 3 y / 7 y given MBS financial-sector) | **Must be set** before go-live |
| C-APP-04 | **O-07 concurrent viewers** peak (if > 50) | ≤ 50 assumed |
| C-APP-05 | **O-08 HA / RTO / RPO** targets | Business-hours critical only; after-hours best-effort |
| C-APP-06 | **O-10 secrets backend** (Vault / AWS SM / env var) | Externalised backend required; concrete choice at Tech Design |
| C-APP-07 | **O-11 non-roster worklog** visibility (project view only vs. exclude entirely) | Project-view-only; not in team-MD |

Each condition can be accepted-as-default at approval time, overridden with a concrete value, or routed back to Design.

## 9. Review Result

```
PASSED WITH CONDITIONS
```

No CRITICAL. No HIGH. Three MEDIUM findings are conditions, not defects. Traceability complete. Baseline integrity intact. Architecture is safe to become the baseline for Technical Design **once an authorised human issues the approval**.

## 10. Human Approval Package

The approver should respond with **one** of:

| Option | Meaning |
|---|---|
| `ARCHITECTURE APPROVED` | Accept the design as-is; the proposed defaults in §8 (C-APP-01 … C-APP-07) become authoritative. |
| `ARCHITECTURE APPROVED WITH CONDITIONS` | Supply concrete values or dispositions for any of §8 (particularly C-APP-01 O-03 and C-APP-03 O-09); AI will update ADR-ARCH-010 and the audit-sizing section accordingly. |
| `ARCHITECTURE CHANGES REQUIRED` | Name the finding(s) to re-work; routes back to `02_Architecture_Design.md`. |
| `ARCHITECTURE REJECTED` | Routes back further (REQ or Context). |

---
## ⛔ HUMAN GATE
AI stops here. The approver's statement is required.
*Hard rule: "Never infer or fabricate the human decision."*
Next authorized stage (only after `ARCHITECTURE APPROVED` or `ARCHITECTURE APPROVED WITH CONDITIONS`): `03-TECHNICAL-DESIGN/…`.
