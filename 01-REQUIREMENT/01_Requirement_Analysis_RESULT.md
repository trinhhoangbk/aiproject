# 01 — REQUIREMENT ANALYSIS · Result

| Field | Value |
|---|---|
| Stage | 01-REQUIREMENT / 01 Requirement Analysis |
| Date | 2026-10-04 15:13 ICT |
| Role | Senior BA (AI) — proposes; does not approve |
| Project onboarding | `PROJECT AI-READY WITH OPEN ITEMS` |
| **Readiness** | **🟡 READY FOR REQUIREMENT CLARIFICATION — WITH OPEN ITEMS** |
| Business Request source | `2739e3fa-attachment.txt` — BRD v1.0, "Multi-Project Workload & Resource Balancing Hub" (user provided 2026-10-04 15:13) |

---

## A. Business Request Summary (neutral restatement)
A BRD v1.0 requests a management tool that aggregates — per member, across all Jira projects — active issues, daily done tasks and worklog, overdue items, remaining estimates and personal ETA, and that maintains a pipeline of queued projects to be balanced against each member's available man-day budget. The tool consumes Jira as the system of record for issues/worklog; it does not replace Jira or implement payroll.

## B. Business Objective
BO-01, BO-02, BO-03 taken verbatim from §2.3 of the BRD:
- **BO-01** Reduce by 80% the time to compile daily work and progress reports.
- **BO-02** Detect overload risk and overdue tasks in real time.
- **BO-03** Optimize resource utilization via an available-man-day table so new projects can be accepted and allocated proactively.

## C. Requirement Register

| REQ ID | Requirement | Actor / Trigger | Expected Outcome | Evidence | Status |
|---|---|---|---|---|---|
| REQ-001 | Aggregate every active (not-done) Jira task of a member across all projects into one view, with per-project distribution ratio (by task count or remaining hours) and automatic Overload detection against a configurable daily capacity (default 8h/day, 40h/week). Red flag when total remaining estimate ÷ working days to deadline > 100% capacity. | Manager viewing member workload | Single member-centric workload dashboard with overload flag | BRD §4 BR-01.1–BR-01.3 | PROPOSED |
| REQ-002 | Daily output report: for any chosen day and any member, list all tasks moved to Done/Closed within 24h, with project tag; and total worklog hours of that day broken down by project. | Manager / member | Daily done list + worklog totals per project | BRD §4 BR-02.1–BR-02.2, §6 UAC "Báo cáo Daily" | PROPOSED |
| REQ-003 | Overdue radar: list every unresolved task with Due Date < today; classify by severity (late 1–3 days, late >1 week) with assignee and owning project; and raise an Early-Warning for tasks not yet overdue whose remaining time exceeds the member's available capacity before deadline. | Manager / member | Overdue list + early warnings | BRD §4 BR-03.1–BR-03.3, §6 UAC "Cảnh báo Overdue" | PROPOSED |
| REQ-004 | Personal ETA: per member × project, compute Total Remaining Estimate and forecast an estimated completion date based on actual working capacity and the member's time-allocation ratio for that project. | Manager | Personal ETA per member × project | BRD §4 BR-04.1–BR-04.2 | PROPOSED |
| REQ-005 | Project Pipeline: manage queued projects, each recording name & objective, target deadline, total estimated man-days (or story points), and required skills/positions (Frontend, Backend, QC, Designer, …). | Manager | Queue of upcoming projects with required capacity | BRD §4 BR-05.1–BR-05.2 | PROPOSED |
| REQ-006 | Resource Balancing: compute Available Man-day per member over a chosen horizon (this week, next 2 weeks, next month) using `available = standard_MD − committed_MD (Jira Remaining)`; display an availability heatmap (red / yellow / green); let the manager compare pipeline-project man-day demand against members' available man-days and perform direct Assign / Re-assign. | Manager | Availability matrix + assignment action | BRD §4 BR-06.1–BR-06.3, §6 UAC "Cân đối Man-day" | PROPOSED |
| REQ-007 | Jira synchronization: event-driven webhook sync for status change, assignee change, and worklog updates; and a full polling sync every hour for consistency. | System | Near-real-time Jira state in the Hub | BRD §7 NFR | PROPOSED |
| REQ-008 | Role-based access: `Manager` sees all members, allocates pipeline projects, configures team capacity; `Member` sees only their own personal dashboard and progress. | Manager / Member | RBAC enforced on all screens and APIs | BRD §7 NFR | PROPOSED |
| REQ-009 | Performance: team dashboard load time ≤ 2.5 s for 10–50 members and thousands of issues. | System | Latency SLO met | BRD §7 NFR | PROPOSED |

### Business Rules (from §5 BRD — kept verbatim; not reinterpreted)
| ID | Rule |
|---|---|
| B-RULE-01 | 1 MD = 8 standard working hours. Exclude Saturday, Sunday and configured holidays. |
| B-RULE-02 | A task with no Original/Remaining Estimate is tagged "Chưa ước lượng" and temporarily counted as 4 h (0.5 MD) for load calculations, so load is not under-reported. |
| B-RULE-03 | A member is Overload when weekly committed hours > 40 h, or when any single day's due workload exceeds 8 h without reschedule. |
| B-RULE-04 | A member is Available when the free man-day budget in the requested window ≥ the estimated man-day load of the new task/project. |

## D. Scope
### In Scope (from BRD §3)
- Sync of issue, worklog, estimate from Jira.
- Member-centric aggregation of activity, progress and overdue status.
- Workload measurement and personal ETA.
- Project Pipeline & deadlines.
- Available-man-day computation and allocation support.

### Out of Scope (from BRD §3)
- Replacing Jira for issue creation, sprint planning or workflow management.
- Timekeeping / payroll features.

## E. Known Constraints
- Integration target: Atlassian Jira (Jira Software / Jira Cloud API).
- Primary users: Team Leader, PM, Engineering Manager, Resource Manager (BRD §1).
- NFR performance target: dashboard ≤ 2.5 s (REQ-009).
- NFR sync: webhook + hourly polling (REQ-007).
- B-RULE-01…04 as given.

## F. Assumptions — explicitly unapproved
None. Every ambiguity is routed to DEC-* in `02_Requirement_Clarification.md` instead of being assumed here.

## G. Unknowns / Contradictions
Material unknowns detected and forwarded to Clarification:
- Mapping of "Done / Closed" to concrete Jira statuses, per project.
- Timezone for "today" / "24h" window.
- Holiday calendar source and governance.
- Configurable-capacity governance (who can change the 8 h/40 h defaults).
- Scope of Jira projects observed (all tenant projects vs. an allow-list).
- Member identity source (Jira directory vs. HR vs. manual).
- Skills taxonomy (free text vs. controlled list).
- Whether Assign / Re-assign writes back to Jira.
- Early-warning concrete threshold (BR-03.3 lacks a numeric rule).
- "Yellow — busy" heatmap threshold (only red is quantified in B-RULE-03).
- Overload's "cannot reschedule" test in B-RULE-03.
- Pipeline-project ownership / edit rights (RBAC detail).
- Member visibility onto peers (confirm Member role is strictly self-only).
- Notification channel for alerts (UI only vs. email/Slack).
- Audit retention for worklog snapshots.

No internal contradictions found between BR-* and §5/§6/§7.

## H. Decisions Required (summary — detail in file 02)
Routed to `02_Requirement_Clarification.md` as DEC-001…DEC-016 with severity.

## I. Traceability (Business Request → REQ)

| BRD section | → REQ |
|---|---|
| §4 BR-01 | REQ-001 |
| §4 BR-02 + §6 UAC Daily | REQ-002 |
| §4 BR-03 + §6 UAC Overdue | REQ-003 |
| §4 BR-04 | REQ-004 |
| §4 BR-05 | REQ-005 |
| §4 BR-06 + §6 UAC Cân đối | REQ-006 |
| §7 NFR sync | REQ-007 |
| §7 NFR RBAC | REQ-008 |
| §7 NFR performance | REQ-009 |
| §5 B-RULE-01…04 | Cross-cuts all REQs |

## J. Readiness
```
READY FOR REQUIREMENT CLARIFICATION — WITH OPEN ITEMS
```

Baseline is coherent and sourced; ambiguities exist but each is non-destructive and routes to Clarification.

---
⛔ **STOP — stage boundary.** Next authorized file: `02_Requirement_Clarification.md`.
