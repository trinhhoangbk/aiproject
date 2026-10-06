# 03 — ACCEPTANCE CRITERIA · Result

| Field | Value |
|---|---|
| Stage | 01-REQUIREMENT / 03 Acceptance Criteria |
| Date | 2026-10-04 15:36 ICT |
| Role | Senior Requirements Verification Engineer (AI) — defines testable AC without designing implementation |
| Precondition | `BUSINESS CLARIFICATION CLEARED` (recorded in checkpoint result) |
| **Readiness** | **🟡 AC READY WITH OPEN ITEMS** |

---

## A. Acceptance Criteria Register

| AC ID | REQ | Given | When | Then | Evidence / Decision |
|---|---|---|---|---|---|
| AC-001.1 | REQ-001 | Member X has active issues (statusCategory ≠ 'done') in Jira projects on the Allow-list | Manager opens X's workload view | View shows every active issue grouped by project with (a) count ratio and (b) remaining-hours ratio per project | BR-01.1/.2; DEC-005 |
| AC-001.2 | REQ-001 | X's sum of committed hours in the current ISO week > 40 h (adjusted by Team/Member capacity overrides where applicable) | View is rendered | Overload flag is **Red** at week level | B-RULE-03; DEC-004 |
| AC-001.3 | REQ-001 | A specific working day D has total due workload for X > X's standard daily hours AND affected issues are classified "cannot reschedule" per DEC-010 | View is rendered | Overload flag is **Red** at day level with reasons listed | B-RULE-03; DEC-010 |
| AC-001.4 | REQ-001 | An issue has no Original/Remaining Estimate | Workload is computed | Issue is tagged **"Chưa ước lượng"** and counted as 0.5 MD (4 h) for load; counted-value is distinguishable from estimated values in the UI | B-RULE-02 |
| AC-001.5 | REQ-001 | A Member-level capacity override exists for X | Any workload / overload / ETA / AR computation runs for X | Member override wins over Team and Global defaults | DEC-004 |
| AC-002.1 | REQ-002 | Manager picks calendar date D in `Asia/Saigon` | X moves issues to a status with `statusCategory.key='done'` within `[D 00:00:00, D 23:59:59]` | All such issues are listed with project tag; resolution-based `Discarded` issues are shown but flagged | DEC-001; DEC-002; §6 UAC Daily |
| AC-002.2 | REQ-002 | Same window D | X records worklog entries in Jira with `started` within D | Total hours per project and overall are displayed, matching Jira's `timeSpentSeconds` | BR-02.2 |
| AC-002.3 | REQ-002 | Issue is closed with `resolution ∈ {Won't Fix, Duplicate, Invalid}` | KPI Output is computed | Issue appears in daily done list flagged **Discarded**; it is excluded from KPI Output when the "exclude Discarded" option is active (default on) | DEC-001 |
| AC-002.4 | REQ-002, REQ-008 | User role = Member | User opens Daily report | They see only their own data (REQ-008; conservative DEC-014 default) | REQ-008; DEC-014 (provisional) |
| AC-003.1 | REQ-003 | Issue has `Due Date < today (Asia/Saigon)` AND `statusCategory ≠ 'done'` AND is on allow-list | Overdue radar is opened | Issue appears in the Overdue list with assignee and owning project | BR-03.1; DEC-005 |
| AC-003.2 | REQ-003 | Overdue issue | List is rendered | Severity label = "Trễ 1–3 ngày" when 1 ≤ days_overdue ≤ 3; "Trễ >1 tuần" when days_overdue > 7; a third band "Trễ 4–7 ngày" is shown for 3 < days_overdue ≤ 7 | BR-03.2 (verbatim bands; middle band inferred as gap-filler and marked explicit in UI copy) |
| AC-003.3 | REQ-003, REQ-007 | A Jira status/assignee/worklog event occurs | Webhook is received by the Hub | Overdue list reflects the change within ≤ 5 minutes of Jira update time (p99) | §6 UAC Overdue; §7 NFR |
| AC-003.4 | REQ-003 | For an unresolved issue of X: `RDC = Remaining Estimate (h) / (RWD × X's standard daily hours)` with `RWD` excluding Sat/Sun/holidays | Early-warning is computed | **Yellow** flag when `0.7 < RDC ≤ 1.0`; **Red** flag when `RDC > 1.0` | DEC-009 |
| AC-003.5 | REQ-003 | `RWD = 0` (deadline today or already passed) | Early-warning is computed | Treat as Red (division-by-zero guard); if `Due Date < today` the Overdue path applies instead | DEC-009 edge case |
| AC-004.1 | REQ-004 | X has active issues in project P | Personal ETA panel is opened | Total Remaining Estimate for X × P is shown in hours and in MD (hours ÷ X's standard daily hours) | BR-04.1 |
| AC-004.2 | REQ-004 | X's observed per-project time-allocation ratio over the last N working days is `α_P` (N = 10 WD default) and X's standard daily hours = h | ETA for X × P is computed | `ETA_date = today + ceil(Remaining Estimate_P / (α_P × h))` working days, counted via B-RULE-01 | BR-04.2 (α and N measurable, stable, auditable) |
| AC-005.1 | REQ-005 | Manager creates a Queued Project | Save is submitted | Record requires: Name, Objective, Target Deadline, Total Estimated MD (or Story Points), Required Skills (from DEC-007 controlled list) | BR-05.2; DEC-007 |
| AC-005.2 | REQ-005 | A required field is missing | Save is submitted | Save is rejected with field-level error; no record is created | BR-05.2 |
| AC-006.1 | REQ-006 | Horizon `W` (this week / next 2 weeks / next month) chosen | AR / Available MD are computed for X | `Standard_MD = working_days(W) × h / 8`; `Committed_MD = Σ Remaining Estimate (h) in W / 8`; `Available_MD = Standard_MD − Committed_MD`; `AR = Committed_MD / Standard_MD × 100%` | BR-06.1; B-RULE-01; DEC-011 |
| AC-006.2 | REQ-006 | AR for X in W computed | Heatmap cell is rendered | `AR < 60%` → **Dark Green**; `60% ≤ AR ≤ 85%` → **Light Green**; `85% < AR ≤ 100%` → **Yellow**; `AR > 100%` → **Red** | DEC-011 |
| AC-006.3 | REQ-006 | Queued project `P` requires `R` MD before deadline `D` | Manager asks the Hub for suggestions | Hub returns the list of members whose Available_MD in window `[today, D]` ≥ R, matching required skills (DEC-007), sorted descending by Available_MD | BR-06.3; B-RULE-04; §6 UAC Cân đối |
| AC-006.4 | REQ-006 | Manager clicks Assign/Re-assign for issue `I` to member `Y` | Action is confirmed | Hub calls `PUT /rest/api/3/issue/{I}/assignee` with `Y.jira_account_id` **and** posts Internal Comment `"Task reassigned via Resource Balancing Hub by [Manager Name]"`; audit log entry is created | DEC-008 |
| AC-006.5 | REQ-006 | Jira returns `403` or workflow rejects the assignee change | Hub receives the response | Hub rolls back the optimistic UI update and shows a specific error message naming the cause; audit log records the failed attempt | DEC-008 |
| AC-006.6 | REQ-006 | Any Assign/Re-assign or capacity override occurs | Action completes | An audit entry with `{timestamp, actor, source, target, action, result}` is persisted | DEC-008 (retention duration deferred — DEC-013 OPEN) |
| AC-007.1 | REQ-007 | A Jira webhook arrives for `issue_updated` events touching status, assignee, or worklog | Event is accepted | The affected member-centric views are updated so that the change is observable within ≤ 5 min (p99) of the Jira event timestamp | §7 NFR; §6 UAC Overdue |
| AC-007.2 | REQ-007 | 60 minutes elapse since last full-sync | Scheduler fires | A full consistency poll runs over every allow-listed project; any divergence from webhook state is reconciled; divergence count is logged | §7 NFR |
| AC-007.3 | REQ-007 | A webhook is lost or delayed (simulated by replaying an older state) | Next full-sync runs | The authoritative Jira state replaces the stale Hub state; event is logged as "reconciled by polling" | §7 NFR (fallback behaviour) |
| AC-008.1 | REQ-008 | User role = Manager (or Admin) | Any member-centric view, pipeline screen, capacity editor is opened | View renders with full-team data; capacity editor is writeable at Global / Team / Member tiers per DEC-004 | DEC-004; §7 NFR |
| AC-008.2 | REQ-008 | User role = Member | Personal dashboard is opened | View is scoped to that user's own data only; peer rows are not displayed | REQ-008; DEC-014 provisional |
| AC-008.3 | REQ-008 | User role = Member | User attempts to open team heatmap, roster, pipeline, or capacity editor | Hub returns 403; action is logged | DEC-004; DEC-012 provisional |
| AC-009.1 | REQ-009 | Dataset size: 10–50 members, ≤ 50 000 open issues, 90-day worklog history | p95 team-dashboard first contentful render is measured over 100 loads | p95 ≤ 2.5 s on the reference hardware spec declared in Architecture stage | §7 NFR |

## B. Negative & Boundary Scenarios

| Scenario | Expected observable |
|---|---|
| Issue outside allow-list is linked to a member | Issue is bucketed as **Unknown** or excluded; it does not count toward Workload / Overdue / AR | DEC-005 |
| Partner / bot Jira account appears on a project | Not counted toward team MD; not shown as a Member | DEC-006 |
| `resolution = Won't Fix / Duplicate / Invalid` | Flagged **Discarded**; excluded from KPI Output by default; still visible in Daily list for traceability | DEC-001 |
| Timezone edge: issue closed at `23:59 ICT` of day D | Counted for day D's Done list; not for D+1 | DEC-002 |
| Issue closed at `00:01 ICT` of day D+1 | Counted for D+1 | DEC-002 |
| `Remaining Estimate` increases after sync | AR, Overload and ETA all recompute within next sync tick | AC-007.1 |
| Member capacity override removed | Falls back to Team default; if no Team default, falls back to Global | DEC-004 |
| Fix Version has no Release Date | Hard-Deadline criterion 1 does not fire; criteria 2 and 3 still apply | DEC-010 |
| Hub attempts assignee change but issue workflow forbids assignee change in current status | Rollback and user-visible error naming the status | DEC-008 |
| Capacity edited mid-week | New capacity applies forward from the moment of change; current-week AR recomputes using the mixed value with the change point in the audit entry | DEC-004 (behavioural interpretation; may be re-affirmed) |

## C. Business Rule Coverage

| Rule | ACs covering it |
|---|---|
| B-RULE-01 (1 MD = 8 h; exclude Sat/Sun/holidays) | AC-001.1, AC-003.4, AC-004.2, AC-006.1 |
| B-RULE-02 (no-estimate → 0.5 MD placeholder, flagged) | AC-001.4 |
| B-RULE-03 (Overload) | AC-001.2, AC-001.3 (with DEC-010 for "cannot reschedule") |
| B-RULE-04 (Available ≥ required MD) | AC-006.3 |
| DEC-009 formula | AC-003.4, AC-003.5 |
| DEC-011 bands | AC-006.2 |

## D. Untestable / Blocked Items

| Item | Why | Routing |
|---|---|---|
| AC-006.6 retention duration | DEC-013 OPEN; retention policy not supplied | Business decision at Validation |
| AC-008.1/.2/.3 fine-grained peer visibility | DEC-014 provisional; conservative "self-only" applied | Business decision at Validation |
| Alert channel (email/Slack) tests | DEC-015 OPEN; AC covers UI only | Business decision at Validation |
| Pipeline-project edit by non-Manager roles | DEC-012 provisional; "Admin + Manager" default applied | Business decision at Validation |
| UI language variants | DEC-016 informational | Business decision at Validation |

## E. Traceability Matrix (Business Request → REQ → DEC/BR → AC)

```
BRD §4 BR-01 ──► REQ-001 ──► B-RULE-02, B-RULE-03, DEC-004, DEC-010
                           ├─► AC-001.1 (workload view)
                           ├─► AC-001.2 (week overload)
                           ├─► AC-001.3 (day overload)
                           ├─► AC-001.4 (no-estimate)
                           └─► AC-001.5 (capacity override)
BRD §4 BR-02 + §6 UAC ──► REQ-002 ──► DEC-001, DEC-002
                                     ├─► AC-002.1 Done list
                                     ├─► AC-002.2 Worklog totals
                                     ├─► AC-002.3 Discarded
                                     └─► AC-002.4 Member scope
BRD §4 BR-03 + §6 UAC ──► REQ-003 ──► DEC-009, DEC-005
                                     ├─► AC-003.1..3 Overdue list + severity + SLA
                                     ├─► AC-003.4 Early-warning bands
                                     └─► AC-003.5 Edge case
BRD §4 BR-04 ──► REQ-004 ──► B-RULE-01
                           ├─► AC-004.1 Remaining
                           └─► AC-004.2 ETA
BRD §4 BR-05 ──► REQ-005 ──► DEC-007
                           ├─► AC-005.1 Create
                           └─► AC-005.2 Reject invalid
BRD §4 BR-06 + §6 UAC ──► REQ-006 ──► DEC-008, DEC-011, DEC-005, DEC-006, DEC-007
                                     ├─► AC-006.1 AR math
                                     ├─► AC-006.2 Heatmap bands
                                     ├─► AC-006.3 Suggestion
                                     ├─► AC-006.4 Write-back + comment
                                     ├─► AC-006.5 Rollback
                                     └─► AC-006.6 Audit
BRD §7 NFR Sync ──► REQ-007 ──► AC-007.1 Webhook ─ AC-007.2 Polling ─ AC-007.3 Reconcile
BRD §7 NFR RBAC ──► REQ-008 ──► DEC-004, DEC-006 ─► AC-008.1..3
BRD §7 NFR Perf ──► REQ-009 ──► AC-009.1
```

## F. Readiness
```
AC READY WITH OPEN ITEMS
```

Open items remaining: DEC-012, DEC-013, DEC-014, DEC-015, DEC-016 (non-blockers). None prevents a complete AC baseline because conservative provisional dispositions are declared in B/§D and will be re-affirmed at Business Validation.

---
⛔ **STOP — stage boundary.** Next authorized file: `04_Domain_Decomposition.md`.
