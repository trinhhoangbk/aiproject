# JIRA READINESS APPROVAL · Record

| Field | Value |
|---|---|
| Date | 2026-10-06 13:02 ICT |
| Decision | **`JIRA READY`** |
| Authorized by | Project owner (user) — in-session statement |

## 1. Jira delivery package approved (as-is)
- `07_Jira_Task_Breakdown_RESULT.md` — 7 Epics + 42 delivery issues (36 implementation + 3 Checkpoints + 3 Governance), zero sub-tasks.
- `08_Jira_Readiness_Review_RESULT.md` — PASSED WITH CONDITIONS, 0 CRITICAL, 1 HIGH (config-level), 3 MEDIUM.

## 2. Conditions elevated to Jira-creation action items

| ID | Action | Owner | Done when |
|---|---|---|---|
| JIRA-COND-01 | Create a Jira project (e.g. key `RBH`) on the chosen non-production Jira Cloud Free site; declare issue types / workflow / required fields / DoD / estimation; confirm the §9 Jira-neutral mapping or supply overrides. | Project owner | Jira project exists; config captured as `deploy/jira-config.md` |
| JIRA-COND-02 | Decide whether `JIRA-TMP-033` is split by SPA screen, and whether to keep GOV tickets G01…G03 in Jira or move them to runbook attachments. | Project owner | Decision captured in `deploy/jira-config.md` |
| JIRA-COND-03 | Checkpoint tickets (CP1/CP2/CP3) are labelled `type/checkpoint` with a non-developer assignee. | Operator | Confirmed at import |

## 3. Backlog shape frozen
- 7 Epics: EPIC-TMP-01 Foundation · EPIC-TMP-02 Jira Sync · EPIC-TMP-03 Workload & Overdue Views · EPIC-TMP-04 Pipeline & Balancing · EPIC-TMP-05 Security & Audit · EPIC-TMP-06 UI · EPIC-TMP-07 Observability & Release.
- 36 implementation issues (`JIRA-TMP-001…036`) + 3 Checkpoints + 3 Governance tasks = 42 items.
- Dependency graph (`07 §5`) is acyclic; topological order documented.
- Every issue traces REQ → AC → TD → PLAN; every COND embedded.
- Every `estimate / assignee / priority / sprint` = `TO ESTIMATE / UNASSIGNED / TO PRIORITIZE / TO SCHEDULE`.

## 4. Jira creation — NOT performed in this file
This session has no authorised Jira create/import tool, and this stage result file **does not** create Jira issues.

To materialise the backlog, do one of:
- **A. Use the Atlassian REST API from the operator's machine** — a short script can read `07 §4` (table) + `§5` (dependencies) + `§7` (traceability) and POST issues. Example mapping — Epic → `issuetype: Epic`; Story → `issuetype: Story`; Task → `issuetype: Task`; parent via `customfield_1xxxx: EPIC_KEY`; links via `/issue/{key}/remotelink` or `/issueLink`. Dedup by `summary` on retry.
- **B. Use Jira's CSV importer** — convert `07 §4` to CSV (one row per issue; columns: Summary, Issue Type, Parent, Labels, Description, Blocked By, Blocks). The YAML in `§10` is the canonical source of description content; `description` can contain the full acceptance/DoD block.
- **C. Add a Jira connector to this session** — then this session can create issues under your approval; results would replace `JIRA-TMP-xxx` with actual keys here and in all preceding files.

After creation, preserve the mapping:

```
Actual Jira Key ↔ JIRA-TMP-xxx ↔ PLAN-xxx ↔ TD-xxx ↔ AC-xxx ↔ REQ-xxx
```

Save it as `deploy/jira-key-map.md` so later Development evidence can cite real keys.

## 5. Next authorised stage
`05-DEVELOPMENT/*` — **not yet present** in the repository.
Creating Jira issues does **not** itself authorise code outside the approved Plan. Development begins only when (a) issues are materialised AND (b) Development playbooks are supplied.

Carried human checkpoints that remain live inside Development:
- **CP-1** — before PLAN-018 and any Jira write.
- **CP-2** — after PLAN-034 (metrics sanity).
- **CP-3** — after PLAN-036 (= Development exit for v1.0).
