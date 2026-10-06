# 02 — REQUIREMENT CLARIFICATION · Result (updated with authorized answers)

| Field | Value |
|---|---|
| Stage | 01-REQUIREMENT / 02 Requirement Clarification |
| Date | 2026-10-04 15:36 ICT (update) |
| Role | Senior Requirements Facilitator (AI) — poses questions; records authorized answers verbatim |
| Authorized Business/Product source | *Phụ lục BRD — Bảng Quyết định Nghiệp vụ (Business Decision Log)*, supplied by project owner on 2026-10-04 |
| **Readiness** | **🟢 READY FOR BUSINESS CLARIFICATION CHECKPOINT** |

---

## A. Clarification Matrix (with authorized answers)

| DEC ID | REQ | Ambiguity | Severity | Authorized Answer (verbatim summary of the Business Decision Log) | Status |
|---|---|---|---|---|---|
| DEC-001 | REQ-002, REQ-003, REQ-006 | "Done / Closed" mapping | BLOCKER | Not a hard-coded name list. Use Jira REST `statusCategory.key == 'done'`. Additionally check `resolution`: issues closed with Won't Fix / Duplicate / Invalid are "ended" but flagged **Discarded** (optionally excluded from KPI Output). | RESOLVED |
| DEC-002 | REQ-002, REQ-003, REQ-006 | Timezone & day boundary | BLOCKER | **Fixed system timezone `Asia/Saigon` (UTC+7)**. **Calendar Day boundary `00:00:00–23:59:59`** (not rolling 24h). | RESOLVED |
| DEC-003 | REQ-001, REQ-006 (B-RULE-01) | Holiday calendar source and governance | BLOCKER | Default = official VN national holidays (annual notice of the Ministry of Labour, Invalids and Social Affairs). Owner = Engineering Manager / Operations Lead / Admin — may add compensated working Saturdays and internal days (company trip, team-building). Project data remains Jira-sourced only. | RESOLVED |
| DEC-004 | REQ-001, REQ-006 | Capacity governance (8 h/40 h) | BLOCKER | **Edit = Admin + Manager; Member = view-only.** Three-tier scope (highest wins): **Global** 8 h/day / 40 h/week (default) → **Team** override (e.g. support/on-call 7 h or 35 h) → **Member** override (interns/part-time 4 h/day; Tech Lead with 25 % mgmt time = 6 h/day / 30 h/week). | RESOLVED |
| DEC-005 | REQ-001, REQ-007 | Scope of Jira projects observed | BLOCKER | **Allow-list** mechanism (Project Scope Filter). Manager configures the set of Jira Project Keys (e.g. `PRJ-A`, `PRJ-B`, `CORE-SYS`). Issues outside the list are skipped or bucketed as "Unknown". | RESOLVED |
| DEC-006 | REQ-001, REQ-006, REQ-008 | Member roster source | BLOCKER | Hub-managed roster set by Manager, mapped to `jira_account_id`. Prevents partner / customer / bot accounts in the Jira project from being counted toward team man-day budget. | RESOLVED |
| DEC-007 | REQ-005, REQ-006 | Skills taxonomy | BLOCKER | **Controlled list, two levels.** L1 Role/Specialty: Frontend, Backend, Mobile, QA/QC, DevOps, UI/UX Designer. L2 Primary Tech: React, Node.js, Python, Flutter, Manual QA, Automation QA, Figma, … Entered by Manager in Member Profile on the Hub. | RESOLVED |
| DEC-008 | REQ-006 | Assign/Re-assign write-back | BLOCKER | **Two-way sync with audit log.** Hub calls `PUT /rest/api/3/issue/{issueIdOrKey}/assignee`; adds Internal Comment `"Task reassigned via Resource Balancing Hub by [Manager Name]"`. On 403 or workflow block, roll back the UI change and show a specific error. | RESOLVED |
| DEC-009 | REQ-003 | Early-Warning threshold | BLOCKER | **Time Pressure Ratio (TPR) formula.** `RWD` = remaining working days (excl. Sat/Sun/holidays). `RDC = Remaining Estimate (h) ÷ (RWD × member standard daily hours)`. **Yellow** when `RDC > 0.7`. **Red** when `RDC > 1.0`. | RESOLVED |
| DEC-010 | REQ-001 (B-RULE-03) | "Cannot reschedule" detection | BLOCKER | Combined criteria: **(1)** issue has a Jira **Fix Version with a fixed Release Date** → Hard Deadline. **(2)** `Priority ∈ {Blocker, Critical}` OR label `Hard-Deadline`. **(3)** Manager toggles **"Lock Deadline"** flag directly in the Hub UI. | RESOLVED |
| DEC-011 | REQ-006 | Heatmap thresholds | BLOCKER | **Allocation Rate `AR = Total Remaining Estimate ÷ Total Standard Capacity × 100 %`.** Bands: **Dark Green** `AR < 60 %` (Underloaded — top candidate for new pipeline work); **Light Green** `60 % ≤ AR ≤ 85 %` (Optimal — keeps 15–20 % buffer); **Yellow** `85 % < AR ≤ 100 %` (Busy); **Red** `AR > 100 %` (Overloaded — intervene). | RESOLVED |
| DEC-012 | REQ-005, REQ-008 | Pipeline-project edit rights (RBAC) | NON-BLOCKER | Not addressed in the Business Decision Log. **OPEN**; AC can proceed assuming "Manager = create/edit; Admin = all" pending disposition. | OPEN |
| DEC-013 | REQ-002, REQ-007 | Audit retention | NON-BLOCKER | Not addressed. **OPEN**; AC asserts "audit exists" with retention duration deferred. | OPEN |
| DEC-014 | REQ-008 | Member peer visibility | NON-BLOCKER | Not addressed. **OPEN**; conservative default: Member sees own data only (consistent with REQ-008 as written). | OPEN |
| DEC-015 | REQ-001, REQ-003 | Alert channel | NON-BLOCKER | Not addressed. **OPEN**; AC covers UI channel only. | OPEN |
| DEC-016 | — | UI language | INFORMATIONAL | Not addressed. **OPEN**; Vietnamese UI is the working assumption. | OPEN |

## B. Contradictions
None detected after answers applied. DEC-009 refines BR-01.3's 100 % figure into a precise formula at RDC > 1.0 (Red) and RDC > 0.7 (Yellow). DEC-011 adds Dark Green / Light Green / Yellow / Red bands with concrete AR thresholds.

## C. Updated Requirement Notes
Behavioural details added to REQs **by evidence from the Business Decision Log, not by assumption**:
- REQ-001 — overload detection follows B-RULE-03 enriched with DEC-010 (3-criteria "cannot reschedule") and DEC-004 (3-tier capacity).
- REQ-002 — Done is defined by Jira `statusCategory.key='done'` + resolution handling with Discarded flag (DEC-001); day boundaries per DEC-002.
- REQ-003 — early-warning uses TPR formula and bands from DEC-009.
- REQ-005 / REQ-006 — skills follow controlled 2-level taxonomy (DEC-007); pipeline projects balanced against members with free MD ≥ required (B-RULE-04).
- REQ-006 — write-back pattern (DEC-008); heatmap bands (DEC-011); allow-list scope (DEC-005); Hub roster (DEC-006).
- REQ-007 — unchanged; still Jira sync webhook + hourly polling.
- REQ-008 — Admin tier recognised (DEC-004); roster governed by Manager (DEC-006).

## D. Remaining Assumptions
Conservative defaults for the 5 OPEN non-blockers are listed in the table above; each is clearly flagged OPEN and will be re-visited at Business Validation.

## E. Blockers
**None.** All 11 BLOCKERs are RESOLVED by the Business Decision Log.

## F. Traceability
```
Business Request (BRD §1–§7)
 └── REQ-001 … REQ-009
       └── DEC-001 … DEC-011  (RESOLVED, Business Decision Log)
       └── DEC-012 … DEC-016  (OPEN, non-blocker)
             └── Authorized Answer
```

## G. Readiness
```
READY FOR BUSINESS CLARIFICATION CHECKPOINT
```

---
⛔ **STOP — stage boundary.** Next authorized file: `BUSINESS_CLARIFICATION_CHECKPOINT.md`.
*Hard rule: AI cannot approve this checkpoint.*
