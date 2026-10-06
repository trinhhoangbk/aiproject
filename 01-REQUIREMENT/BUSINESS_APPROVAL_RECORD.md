# BUSINESS APPROVAL · Record

| Field | Value |
|---|---|
| Date | 2026-10-04 15:46 ICT |
| Decision | **`BUSINESS APPROVED`** |
| Authorized by | Project owner (user) — in-session statement at 15:46 ICT |
| Scope frozen | REQ-001 … REQ-009 as in `01_Requirement_Analysis_RESULT.md` |
| Business rules frozen | B-RULE-01…04 (BRD §5) + BR-DEC-01…11 (Business Decision Log DEC-001…011) |

---

## 1. Items the approval authoritatively settles

### 1.1 Non-blocker DECs — now AUTHORITATIVE

| DEC | Decision (locked) |
|---|---|
| DEC-012 | Pipeline-project edit rights: **Manager = create/edit; Admin = all**. |
| DEC-013 | Audit mechanism required; **retention duration still OPEN** — carried into Architecture as an input (storage sizing). |
| DEC-014 | Member peer visibility: **self-only** (REQ-008 wording). |
| DEC-015 | Alert channel: **UI only** for v1.0. |
| DEC-016 | UI language: **Vietnamese**. |

### 1.2 Findings — accepted

| F | Resolution |
|---|---|
| F-01 | Overdue severity bands: **1–3 days / 4–7 days / >1 week** (3 bands, as per AC-003.2). |
| F-02 | Freshness SLA: **p99 ≤ 5 min** reading of BRD "max 5 phút". |
| F-03 | ETA observation window: **N = 10 working days** (AC-004.2). |
| F-05 | Pipeline Project lifecycle: **Draft → Ready-to-Allocate → Allocated → Closed** + lateral `Changes-required` / `Rejected` / `Reassigned` (04 §F proposal accepted). |
| F-07 | Architecture may proceed; OPEN item DEC-013 (audit retention) must be flagged as Architecture input, not silently resolved. |

### 1.3 REQ baseline — FROZEN

REQ-001 Workload Allocation · REQ-002 Daily Output · REQ-003 Overdue + Early-Warning · REQ-004 Personal ETA · REQ-005 Pipeline · REQ-006 Resource Balancing · REQ-007 Jira Sync · REQ-008 RBAC · REQ-009 Performance ≤ 2.5 s.

## 2. Next authorized stage
`02-ARCHITECTURE_FINAL/01_System_Context_Discovery.md`.
