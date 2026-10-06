# 04 — DOMAIN DECOMPOSITION · Result

| Field | Value |
|---|---|
| Stage | 01-REQUIREMENT / 04 Domain Decomposition |
| Date | 2026-10-04 15:36 ICT |
| Role | Senior Domain Analyst (AI) — business/domain view only; no software architecture |
| **Readiness** | **🟡 DOMAIN BASELINE READY WITH OPEN ITEMS** |

---

## A. Ubiquitous Language / Glossary

| Term | Business meaning (not an implementation detail) |
|---|---|
| **Member** | A person recognised by the Hub roster, mapped to a `jira_account_id` (DEC-006). Partner, customer and bot accounts are not Members. |
| **Role** | One of: Admin, Manager, Member (DEC-004). Manager is a role umbrella for Team Leader, PM, Engineering Manager, Resource Manager (BRD §1). |
| **Standard Working Hours** | The agreed hours a Member is expected to work in a day. Default 8 h; may be overridden at Team or Member tier (DEC-004). |
| **Working Day** | A day that is not Sat/Sun and not in the configured Holiday Calendar (B-RULE-01, DEC-003). |
| **Man-day (MD)** | 1 MD = Standard Working Hours of the Member in question (default 8 h). An aggregated team MD uses 8 h unless a Team override exists. |
| **Remaining Estimate** | Jira's `remainingEstimateSeconds` for a non-done issue. Issues without an estimate are treated as 0.5 MD (B-RULE-02). |
| **Committed MD** | Sum of Remaining Estimate (hours) ÷ Standard Working Hours for a Member over a window. |
| **Standard MD** | Working Days in the window × Standard Working Hours ÷ 8. |
| **Available MD** | `Standard MD − Committed MD` (BR-06.1). |
| **Allocation Rate (AR)** | `Committed MD ÷ Standard MD × 100 %` (DEC-011). |
| **Required Daily Capacity (RDC)** | `Remaining Estimate (h) ÷ (RWD × Member Standard Working Hours)` (DEC-009). |
| **Remaining Working Days (RWD)** | Working days from today to Due Date inclusive-exclusive by business convention (DEC-009). |
| **Time Pressure Ratio (TPR)** | Alias of RDC used for Early-Warning (DEC-009). |
| **Overload** | A Member whose committed hours in the week > 40 h **or** whose day has due workload > Standard Working Hours and those issues qualify as "cannot reschedule" (B-RULE-03 + DEC-010). |
| **Available (status)** | A Member whose Available MD in the asked window ≥ requested MD (B-RULE-04). |
| **Done status** | A Jira status whose `statusCategory.key = 'done'` (DEC-001). |
| **Discarded** | A Done issue whose `resolution ∈ {Won't Fix, Duplicate, Invalid}`; excluded from KPI Output by default (DEC-001). |
| **Hard Deadline** | A due date that cannot be moved. Criteria: Jira Fix Version with a fixed Release Date; or Priority ∈ {Blocker, Critical}; or label `Hard-Deadline`; or Manager's "Lock Deadline" flag (DEC-010). |
| **Allow-list** | The set of Jira Project Keys the Hub observes (DEC-005). |
| **Pipeline / Queued Project** | A project not yet in execution, recorded in the Hub with name, objective, deadline, estimated MD and required skills (BR-05.2). |
| **Skill (L1 / L2)** | Controlled vocabulary. L1 = Role/Specialty; L2 = Primary Tech (DEC-007). |
| **Member Profile** | Hub-side record holding roster link to Jira, Role, L1/L2 skills, capacity override, calendar overrides (DEC-004, DEC-006, DEC-007). |
| **Audit Entry** | A record of a Hub-driven state change (assignment, capacity override) with `{timestamp, actor, source, target, action, result}` (DEC-008). |

## B. Business Capability Map

| Capability | Business purpose | REQ coverage |
|---|---|---|
| Workload Visibility | See every active Jira issue of each Member across all allow-listed projects. | REQ-001 |
| Overload Detection | Surface members whose load breaches daily/weekly capacity considering Hard Deadlines. | REQ-001 |
| Daily Reporting | Describe what each Member finished and logged on any calendar day. | REQ-002 |
| Overdue & Early-Warning | Catch missed and about-to-be-missed deadlines ahead of a Daily Standup. | REQ-003 |
| ETA Forecasting | Predict personal completion dates per project. | REQ-004 |
| Pipeline Management | Keep an authoritative list of queued projects with required MD and skills. | REQ-005 |
| Resource Balancing | Match pipeline demand to member supply and perform the assignment. | REQ-006 |
| Jira Integration | Keep the Hub's picture of Jira near-real-time and consistent. | REQ-007 |
| Access & Governance | Enforce Admin/Manager/Member roles and roster governance. | REQ-008 |
| Performance SLO | Keep the operator experience responsive at team scale. | REQ-009 |

## C. Actor & Responsibility Map

| Actor | Responsibilities |
|---|---|
| **Admin** | Edits capacity at any tier (DEC-004); owns the Holiday Calendar (DEC-003) alongside Engineering Manager / Operations Lead. |
| **Manager** (Team Lead / PM / Eng Mgr / Resource Mgr) | Maintains the Hub roster (DEC-006); configures Allow-list (DEC-005); edits capacity at Global/Team/Member tier (DEC-004); creates and edits Pipeline Projects; performs Assign/Re-assign; toggles "Lock Deadline". |
| **Member** | View-only on their own dashboard; cannot change capacity or roster. |
| **System — Scheduler** | Runs the hourly full-sync (REQ-007). |
| **System — Webhook Receiver** | Consumes Jira events (status, assignee, worklog). |
| **System — Jira (external)** | Owns issues, worklogs, statuses, resolutions, assignees, estimates, due dates, fix versions, priorities, labels. |

## D. Domain Concept Register

| Concept | Business attributes | Not a… |
|---|---|---|
| Member | Hub ID, `jira_account_id`, Role, L1/L2 skills, capacity overrides, active flag. | database table decision. |
| Capacity | Scope = {Global, Team, Member}, daily hours, weekly hours, effective-from, actor. | schema. |
| Holiday Calendar | Country-default holidays (VN), compensated workdays (e.g. working Saturday), company days (team-building). | ICS feed decision. |
| Allow-list | Set of Jira Project Keys. | API design. |
| Issue (observed) | `issue key`, project, assignee (Member), status, statusCategory, resolution, priority, labels, fix version, due date, original estimate, remaining estimate, worklogs. | ORM entity. |
| Worklog | Member, issue, started (date-time), duration (seconds), project. | model class. |
| Pipeline Project | Name, Objective, Target Deadline, Estimated MD (or Story Points), Required Skills (L1 + optional L2). | ticket. |
| Assignment Action | Actor, Member source, Member target, Issue, Timestamp, Jira write result (OK / 403 / workflow-blocked), Internal Comment id. | REST endpoint. |
| Audit Entry | Action, actor, target, timestamp, result. | log level. |
| Heatmap Cell | Member × Window → AR → Band. | pixel. |
| ETA | Member × Project → (Remaining Estimate, Allocation Ratio α, ETA date). | Gantt bar. |
| Early-Warning | Issue → RDC → Band (None / Yellow / Red). | notification. |
| Overload Flag | Member × Day or × Week → Flag Red / None, with contributing issues. | alert mechanism. |

## E. Business Rule Register

| ID | Rule (business meaning) | Source |
|---|---|---|
| B-RULE-01 | 1 MD = 8 standard working hours; Sat/Sun/holidays excluded from working-day counts. | BRD §5 |
| B-RULE-02 | Issue without estimate → tagged "Chưa ước lượng"; treated as 0.5 MD for load calculations. | BRD §5 |
| B-RULE-03 | Overload: weekly committed > 40 h, or any day has due workload > daily capacity on non-reschedulable tasks. | BRD §5 + DEC-010 |
| B-RULE-04 | Member Available when free MD in the window ≥ required MD. | BRD §5 |
| BR-DEC-01 | Done is defined by `statusCategory.key='done'`, not by status name. Resolutions Won't Fix / Duplicate / Invalid flag Discarded. | DEC-001 |
| BR-DEC-02 | System timezone `Asia/Saigon`; days are calendar days. | DEC-002 |
| BR-DEC-03 | Default Holiday Calendar = VN national; Admin / Operations Lead can edit compensated and company days. | DEC-003 |
| BR-DEC-04 | Capacity tiers Global → Team → Member (Member wins). | DEC-004 |
| BR-DEC-05 | Jira projects are observed only when their Key is on the Allow-list. | DEC-005 |
| BR-DEC-06 | Members must be on the Hub roster (mapped to `jira_account_id`). | DEC-006 |
| BR-DEC-07 | Skills follow the 2-level controlled list. | DEC-007 |
| BR-DEC-08 | Assign/Re-assign writes back to Jira with an internal comment; 403/workflow-block triggers rollback. | DEC-008 |
| BR-DEC-09 | TPR formula; Yellow 0.7 < RDC ≤ 1.0; Red RDC > 1.0. | DEC-009 |
| BR-DEC-10 | "Cannot reschedule" by Jira Fix-Version Release Date OR Priority Blocker/Critical OR label `Hard-Deadline` OR Hub "Lock Deadline" flag. | DEC-010 |
| BR-DEC-11 | AR bands: `<60` Dark Green; `60–85` Light Green; `85–100` Yellow; `>100` Red. | DEC-011 |

## F. State / Lifecycle Model

### Issue (Hub-observed view; authoritative state is Jira)
```
    [to-do category]         [in-progress category]       [done category]
         ●  ──────────────────────►  ●  ──────────────────────►  ●
         │                            │                           │
         │  — Overdue path if         │  — Overload counted while │  — Daily Done path
         │     Due Date < today        │     Remaining Estimate     │     + Discarded flag
         │     & statusCategory!=done  │     > 0                    │     if resolution
         │                            │                           │     ∈ {WF, Dup, Inv}
```
Transition legality is Jira's; the Hub does not re-define it.

### Pipeline Project (Hub-native)
```
   Draft ──► Ready-to-Allocate ──► Allocated ──► Closed
                 ▲    │                   │
                 │    └──► Rejected       └──► Reassigned (back to Allocated)
                 └── Changes-required
```
Only states `Draft`, `Ready-to-Allocate`, `Allocated`, `Closed` are evidenced by BRD §4 BR-05. Other states are proposed and marked UNKNOWN below.

### Assignment Action
```
   Proposed ──► Written to Jira ──► Confirmed  (audit = OK)
            │
            └──► Rejected by Jira (403 / workflow) ──► Rolled back  (audit = failed)
```

## G. Domain Invariants

| Inv | Statement |
|---|---|
| INV-01 | A Member identified only by `jira_account_id` not on the Hub roster is never counted toward team MD. (DEC-006) |
| INV-02 | Capacity resolution for a Member on a date: Member override > Team override > Global default. (DEC-004) |
| INV-03 | An Issue outside the Allow-list is never counted toward Workload, Overdue, AR or ETA. (DEC-005) |
| INV-04 | A Member can only see their own data; Admin/Manager can see all. (DEC-004 + REQ-008; DEC-014 provisional) |
| INV-05 | Every Hub-initiated Jira write has a corresponding Audit Entry, OK or failed. (DEC-008) |
| INV-06 | Discarded issues count as closed, but by default do not count toward KPI Output. (DEC-001) |
| INV-07 | A Day's capacity is a function of the Member's effective daily hours on that date (B-RULE-01, DEC-004). |

## H. Ownership / System of Record

| Data domain | SoR | Evidence |
|---|---|---|
| Issues, Statuses, StatusCategory, Resolution, Priority, Labels, FixVersion, DueDate, OriginalEstimate, RemainingEstimate, Worklog, Assignee | **Jira** | BRD §1 "Hệ thống liên kết: Atlassian Jira"; §7 NFR sync |
| Member Roster + `jira_account_id` mapping | **Hub** | DEC-006 |
| Capacity (Global, Team, Member) | **Hub** | DEC-004 |
| Holiday Calendar | **Hub** (defaults imported from VN national) | DEC-003 |
| Skills Taxonomy & Member Skills | **Hub** | DEC-007 |
| Allow-list of Jira Project Keys | **Hub** | DEC-005 |
| Pipeline Projects | **Hub** | BRD §4 BR-05 |
| "Lock Deadline" flag per issue | **Hub** | DEC-010 criterion 3 |
| Audit Log | **Hub** | DEC-008 |

## I. External Business Interactions

| Interaction | Direction | Trigger | Business meaning |
|---|---|---|---|
| Jira issue updated (status/assignee/worklog) | Inbound (webhook) | Any user action in Jira on an allow-listed issue | Keep workload / overdue / AR / ETA current. |
| Full consistency sync | Outbound (Hub → Jira read API) | Hourly scheduler | Catch missed events; reconcile. |
| Assign / Re-assign | Outbound (Hub → Jira write API) | Manager action in Hub | Change the Jira Assignee + record internal comment. |

## J. Traceability Matrix (business layer)

| REQ | Capabilities | Core Concepts | Rules |
|---|---|---|---|
| REQ-001 | Workload Visibility, Overload Detection | Member, Issue, Capacity, Overload Flag | B-RULE-02, B-RULE-03, BR-DEC-04, BR-DEC-10 |
| REQ-002 | Daily Reporting | Issue (Done), Worklog, Discarded flag | BR-DEC-01, BR-DEC-02 |
| REQ-003 | Overdue & Early-Warning | Issue, Early-Warning, Overload Flag | BR-DEC-05, BR-DEC-09 |
| REQ-004 | ETA Forecasting | Issue, ETA, Allocation Ratio α | B-RULE-01 |
| REQ-005 | Pipeline Management | Pipeline Project, Skill (L1/L2) | BR-DEC-07 |
| REQ-006 | Resource Balancing | Heatmap Cell, Assignment Action, Audit | B-RULE-04, BR-DEC-08, BR-DEC-11 |
| REQ-007 | Jira Integration | Issue, Worklog | §7 NFR |
| REQ-008 | Access & Governance | Role, Member Profile, Roster | BR-DEC-04, BR-DEC-06 |
| REQ-009 | Performance SLO | — | §7 NFR |

## K. Unknowns / Risks (open; not resolved here)

| ID | Item | Impact |
|---|---|---|
| U-01 | Pipeline Project full state model (states beyond Draft/Allocated/Closed are proposed, not evidenced). | Workflow modelling for pipeline. |
| U-02 | Audit retention duration (DEC-013). | Storage sizing + policy. |
| U-03 | Alert channels beyond UI (DEC-015). | Integration surface. |
| U-04 | Member peer visibility specifics (DEC-014). | RBAC fine-grain. |
| U-05 | Pipeline-project edit ACL granularity (DEC-012). | Governance. |
| U-06 | UI language variants (DEC-016). | i18n. |
| U-07 | Window edge semantics: whether `Due Date = today` is "overdue" or "due" (not stated in BRD). | AC-003.1 boundary case. |

## L. Readiness
```
DOMAIN BASELINE READY WITH OPEN ITEMS
```

Baseline is coherent and sourced. Open items are non-destructive and listed in §K; they do not force Architecture to invent business behaviour.

---
⛔ **STOP — stage boundary.** Next authorized file: `05_Business_Validation.md`.
