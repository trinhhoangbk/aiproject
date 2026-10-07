# Jira import — backlog Hub v1.0

`jira-import.csv` is generated mechanically from `04-PLANNING/07_Jira_Task_Breakdown_RESULT.md`
(§3 Epics, §4 backlog, §5 dependencies). It adds no new scope.

| Content | Count |
|---|---|
| Epic | 7 |
| Story | 9 |
| Task (incl. 3 Checkpoint + 3 Governance) | 33 |
| "Blocks" links | 82 |

## Columns

| CSV column | Map to Jira field | Note |
|---|---|---|
| Issue Id | Issue Id | Internal id used to link Parent and Blocks (Epic 1–7, PLAN-xxx = 100+xxx, CP = 201–203, GOV = 301–303) |
| Parent | Parent Id | Points to the Epic's Issue Id |
| Issue Type | Issue Type | Epic / Story / Task |
| Summary | Summary | Prefixed with `[PLAN-xxx]` |
| Epic Name | Epic Name | Company-managed projects only. For team-managed, choose "Don't map" |
| Description | Description | Local id, PLAN, REQ/AC, risk, blocked-by, DoD |
| Priority | Priority | Derived from Risk (H/M/L → High/Medium/Low) |
| Status | Status | **Optional** — actual delivery state on 2026-10-07. Choose "Don't map" to import everything as To Do |
| Labels ×4 | Labels | `v1.0`, `area-*`, `plan-xxx` / `checkpoint` / `governance`, `risk-*` |
| Blocks ×7 | Linked Issues → **Blocks** | This issue blocks the issue with that Issue Id |

## How to import (Jira Cloud)

1. Create the project first (e.g. key `RBH`), team-managed Scrum/Kanban.
2. Go to **Settings (⚙) → System → External System Import → CSV**.
   Or open the project → **⋯ → Import issues from CSV** (team-managed).
3. Upload `jira-import.csv`, encoding **UTF-8**, delimiter `,`.
4. Choose project `RBH`.
5. Map fields according to the table above. The two most important ones are **Issue Id → Issue Id** and **Parent → Parent Id**; get these wrong and the Epic hierarchy and links will fail.
6. If you map Status, the values `To Do` / `In Progress` / `Done` must exist in the workflow (Jira's default workflow has all three).
7. **Begin Import**, then check the result: 49 issues, 7 Epics, and an issue such as `[PLAN-026]` should show "blocks" links.

## Note

This is a different project from the one the Hub reads (the allow-list). If you import into a project that the Hub has allow-listed, the webhook will send all 49 issues to the Hub; that is harmless, but they will show up on the workload board.
