# 05 — BUSINESS VALIDATION · Result

| Field | Value |
|---|---|
| Stage | 01-REQUIREMENT / 05 Business Validation |
| Date | 2026-10-04 15:36 ICT |
| Role | Independent Senior Business Requirements Reviewer (AI) |
| Independence rule | Previous-stage claims re-checked against their artifacts and the BRD |
| **Final status** | **🟡 `READY FOR BUSINESS APPROVAL WITH CONDITIONS`** |

---

## A. Validation Summary

- Business objective coverage: **PASS** (3/3 objectives covered by REQs).
- REQ ↔ BR traceability: **PASS**.
- Clarification closure: **PASS** on 11/11 BLOCKER DECs; 5 non-blockers remain OPEN with provisional dispositions, carried as conditions.
- AC coverage: **PASS WITH CONDITIONS** (every in-scope REQ has at least one testable AC; some ACs reference provisional dispositions).
- Business-rule coverage: **PASS** (B-RULE-01…04 and BR-DEC-01…11 each mapped to at least one AC or invariant).
- Domain consistency: **PASS** (glossary, actors, concepts and rules coherent; no cycle or contradiction detected).
- Architecture readiness: **CONDITIONAL** — Architecture may begin as long as it does not fix behavioural choices still OPEN (DEC-012..016); flagging them as Architecture inputs is OK; silently resolving them is not.

## B. Validation Matrix

| Check | Evidence consulted | Result | Finding(s) |
|---|---|---|---|
| 1. Objective coverage: every BO maps to ≥ 1 REQ | 01 §B, 01 §C | PASS | — |
| 2. Scope consistency (In/Out) has no contradiction | 01 §D vs. BRD §3 | PASS | — |
| 3. Decision closure: no BLOCKER hidden as assumption | 02 §A, Checkpoint result | PASS | — |
| 4. REQ quality: clear, atomic, source-traceable | 01 §C | PASS | F-02 (minor) |
| 5. AC coverage: every in-scope REQ has sufficient testable AC | 03 §A | PASS WITH CONDITIONS | F-03, F-04 |
| 6. Behaviour consistency: ACs don't contradict REQs / Rules / DECs | 03 §A, 02 §A | PASS | — |
| 7. Domain consistency: terminology / actors / concepts / rules / states coherent | 04 §A–§G | PASS | F-05 |
| 8. End-to-end traceability: BR → REQ → DEC/BR → AC → Domain | 01 §I, 02 §F, 03 §E, 04 §J | PASS | — |
| 9. Remaining assumptions classified with risk | 02 §A, 03 §D, 04 §K | PASS WITH CONDITIONS | F-06 |
| 10. Architecture readiness: no business invention required | All artifacts | CONDITIONAL | F-07 |

## C. Findings Register

| ID | Severity | Artifact | Issue | Required Owner / Action |
|---|---|---|---|---|
| F-01 | NOTE | 01 §C REQ-003 | BRD §4 BR-03.2 defines two overdue-severity bands ("1–3 days", ">1 week"); 03 §A AC-003.2 added a middle band "4–7 days" to avoid a gap. | **Business/Product** confirm the middle band at approval. |
| F-02 | LOW | 01 §C REQ-007 | The 5-minute freshness SLA on §6 UAC-Overdue is implemented via AC-007.1 (p99 ≤ 5 min). BRD text is "trong vòng tối đa 5 phút"; "p99" tightens that to a measurable statistic. | **Business/Product** confirm "max 5 min" ⇔ "p99 ≤ 5 min". |
| F-03 | MEDIUM | 03 §A AC-004.2 | ETA formula uses observed allocation ratio α over last N=10 WD. BRD BR-04.2 did not fix N or α's observation window; the AC made both measurable and auditable. | **Business/Product** confirm or override N = 10 WD. |
| F-04 | MEDIUM | 03 §A AC-008.2 | Member peer visibility set to "self-only" (REQ-008 wording). DEC-014 non-blocker not addressed. | **Business/Product** confirm "Member sees own data only" or specify exceptions. |
| F-05 | LOW | 04 §F | Pipeline Project lifecycle beyond Draft/Allocated/Closed is proposed, not evidenced. | **Business/Product** approve the states or trim. |
| F-06 | MEDIUM | Multiple | DEC-012 (edit ACL for pipeline), DEC-013 (audit retention), DEC-015 (alert channels), DEC-016 (UI language) have provisional dispositions, not authoritative answers. | **Business/Product** disposition at approval (RESOLVED, DEFERRED-WITH-DISPOSITION, or REJECTED). |
| F-07 | NOTE | — | Architecture may proceed with the 11 RESOLVED BLOCKERs. The 5 OPEN items must be flagged as Architecture inputs rather than silently resolved. | **Human approver** acknowledge this constraint. |

Severity key: BLOCKER > HIGH > MEDIUM > LOW > NOTE. **No BLOCKER or HIGH findings.**

## D. End-to-End Traceability Coverage

Spot-checks performed on the independence rule (reviewer traced from BRD line to AC):

| From BRD line | Traced to | Verified |
|---|---|---|
| BRD §4 BR-01.3 "Red when total Remaining ÷ working days > 100% capacity" | REQ-001 → B-RULE-03 → AC-001.3; DEC-010 for non-reschedulable qualifier | ✓ |
| BRD §4 BR-02.1 "within 24h window Done list" | REQ-002 → DEC-002 → AC-002.1 | ✓ |
| BRD §4 BR-03.3 "Early-Warning" | REQ-003 → DEC-009 → AC-003.4/.5 | ✓ |
| BRD §4 BR-06.2 "Heatmap" | REQ-006 → DEC-011 → AC-006.2 | ✓ |
| BRD §6 UAC "Overload example 3 tasks 16h due tomorrow" | AC-001.3 (day-level) satisfies the example | ✓ |
| BRD §6 UAC "5 MD before day 25" | AC-006.3 explicitly computes against Available_MD[today..D] | ✓ |
| BRD §7 NFR Performance "≤ 2.5 s for 10–50 members" | REQ-009 → AC-009.1 | ✓ |

No orphan BRs; no AC without a REQ anchor.

## E. Remaining Assumptions / Risks

| ID | Risk | Mitigation |
|---|---|---|
| R-01 | Provisional dispositions for DEC-012..016 may be flipped later, invalidating AC-006.6, AC-008.1..3, F-03. | Confirm at approval. |
| R-02 | Allocation ratio α (AC-004.2) requires at least N WD of historical data to be meaningful; cold-start members have no history. | Define a cold-start fallback (business decision). |
| R-03 | "Lock Deadline" flag (DEC-010 criterion 3) is Hub-side only; losing the Hub DB loses the flag. | Backup/retention — Architecture concern. |
| R-04 | VN national holiday import depends on a yearly, text-form government notice. | Operations Lead loads it; versioned. |
| R-05 | `AR = 100%` lands on the Yellow / Red boundary. Both sides are explicit in DEC-011 (`≤ 100%` is Yellow, `> 100%` is Red); AC-006.2 encodes it. | Confirmed; no action. |

## F. Architecture Readiness Assessment

**Conditional PASS.** Architecture (stage `02-ARCHITECTURE_FINAL/01_System_Context_Discovery.md`) may begin once Human Business Approval is granted, subject to:

1. The 5 OPEN non-blockers are explicitly dispositioned by the approver (F-06).
2. Architecture must **flag** them as Architecture inputs if un-dispositioned, not silently decide.
3. The 7 findings in §C are either accepted at approval, or routed back to the appropriate earlier file for update.

## G. Human Review Package

The authorised approver (project owner / Business/Product authority) must decide, in one pass:

### G.1. Non-blocker DEC decisions
- **DEC-012** Pipeline-project edit rights: *provisional "Admin + Manager"*. Accept / modify / reject.
- **DEC-013** Audit retention duration (days or months). Set a value.
- **DEC-014** Member peer visibility: *provisional "self-only"*. Accept / modify.
- **DEC-015** Alert channels: *provisional "UI only"*. Accept / extend to email / Slack / Jira comment.
- **DEC-016** UI language: *provisional "Vietnamese only"*. Accept or add English.

### G.2. Findings to confirm
- **F-01** Overdue severity middle band ("4–7 days"). Accept, remove or re-band.
- **F-02** Freshness SLA: confirm `p99 ≤ 5 min` interpretation of "max 5 min".
- **F-03** ETA observation window `N = 10 WD`. Accept or set.
- **F-05** Pipeline lifecycle states beyond Draft/Allocated/Closed. Accept or trim.

### G.3. REQ baseline freeze
- Confirm REQ-001..REQ-009 wording.
- Confirm scope In/Out (BRD §3) unchanged.

### G.4. Final decision
Issue **one** of:

| Option | Meaning |
|---|---|
| `BUSINESS APPROVED` | All of §G.1–§G.3 accepted as proposed; the 5 provisional dispositions become authoritative. |
| `BUSINESS APPROVED WITH CONDITIONS` | §G.1–§G.3 accepted with named edits; AI applies the edits and reports changed artifacts. |
| `CHANGES REQUIRED` | Specific items must be re-worked before another validation pass. |
| `REJECTED` | REQ baseline is unacceptable; return to `01_Requirement_Analysis.md`. |

## H. Allowed Final Status (AI)

```
READY FOR BUSINESS APPROVAL WITH CONDITIONS
```

Reason: the artifacts collectively satisfy every validation check; the only reason this is "with conditions" rather than plain `READY FOR BUSINESS APPROVAL` is the 5 OPEN non-blockers in §G.1 and the 4 findings in §G.2 that require the human approver to pronounce.

---
## ⛔ HUMAN GATE
AI stops here. The approver must respond with one of the four options in §G.4.
*Hard rule: `AI MUST NOT select the human decision.`*
Next authorized stage (only after `BUSINESS APPROVED` or `BUSINESS APPROVED WITH CONDITIONS`): `02-ARCHITECTURE_FINAL/01_System_Context_Discovery.md`.
