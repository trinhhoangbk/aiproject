# 01 — SYSTEM CONTEXT DISCOVERY · Result (updated 2026-10-04 15:52 ICT)

| Field | Value |
|---|---|
| Stage | 02-ARCHITECTURE_FINAL / 01 System Context Discovery |
| Date | 2026-10-04 15:52 ICT (update) |
| Role | Senior Software Architect (AI) — evidence-driven investigator |
| **Readiness** | **🟡 SYSTEM CONTEXT DISCOVERED WITH OPEN ITEMS** (ready to be approved with carried conditions) |

> Greenfield project. The Hub does not exist yet. "What exists today" = Jira (external) + the project repository (playbooks + Context Layer, no application code).

---

## 1. Discovery Summary (unchanged)
No incumbent Hub. Only external system is Atlassian Jira. Repo contains playbooks + Context Layer; no code.

## 2. System Inventory (unchanged, see previous version)

## 3. Actors & External Systems (unchanged)

## 4. System Boundaries (unchanged)

## 5. Business Flow Traces (unchanged — F-01…F-10)

## 6. Data Ownership (unchanged)

## 7. Integration Map (UPDATED)

| Call | Direction | Purpose | Decision this run |
|---|---|---|---|
| `GET /rest/api/3/search` (JQL) | Hub → Jira | Hourly full-sync of allow-list | API **v3** |
| `GET /rest/api/3/issue/{idOrKey}` | Hub → Jira | Issue detail | v3 |
| `GET /rest/api/3/issue/{idOrKey}/worklog` | Hub → Jira | Worklog fetch | v3 |
| `PUT /rest/api/3/issue/{idOrKey}/assignee` | Hub → Jira | Assign/Re-assign (DEC-008) | v3 (body: `{ "accountId": "<jira_account_id>" }`) |
| `POST /rest/api/3/issue/{idOrKey}/comment` | Hub → Jira | Internal reassignment comment (DEC-008) | v3, body in **ADF JSON** |
| Webhook callback | Jira → Hub | `jira:issue_updated`, `worklog_created`, `worklog_updated`, `worklog_deleted` | Jira Cloud webhook format |

## 8. Runtime / Deployment Context (UPDATED from user decisions 15:52 ICT)

| Attribute | Status | Decision |
|---|---|---|
| Primary language | **RESOLVED** | **Java** (user, 15:52 ICT) |
| Application framework | **RESOLVED** | **Spring Boot** (user) |
| Primary database | **RESOLVED** | **PostgreSQL** (user) |
| Event bus / queue | **RESOLVED** | **Apache Kafka** (user) |
| CI/CD | **RESOLVED** | **None mandated for v1.0** — release process manual (user) |
| Deployment target (cloud / on-prem) | **OPEN** (O-04) | — |
| Observability stack | OPEN | — |
| Backup / DR | OPEN | — |

## 9. Constraints (UPDATED — added C-13…C-17)

| ID | Constraint | Source |
|---|---|---|
| C-01 … C-12 | (unchanged, see previous version) | — |
| C-13 | Hub is a **Jira Cloud** client. | BRD §1 verbatim ("Jira Software / Jira Cloud API") |
| C-14 | Hub uses **Jira REST API v3** (incl. ADF JSON for comment bodies). | Decided by AI 15:52 ICT with BRD evidence + current-recommended-API reasoning |
| C-15 | Hub implementation stack: **Java + Spring Boot**. | User 15:52 ICT |
| C-16 | Primary datastore: **PostgreSQL**. Event bus: **Apache Kafka**. | User 15:52 ICT |
| C-17 | No CI/CD pipeline for v1.0. Build / release is manual, with explicit human gate before each release. | User 15:52 ICT |

## 10. UNKNOWN / Contradictions / Blockers (UPDATED)

### RESOLVED this run
| ID | Resolution | Source |
|---|---|---|
| O-01 | **Jira Cloud** | AI inference from BRD §1 "Jira Cloud API" |
| O-02 | **Jira REST API v3** | AI inference: current-recommended version for Cloud; ADF-ready for DEC-008 Internal Comment |
| O-05 | **Java + Spring Boot** | User |
| O-06 | **PostgreSQL + Kafka + no CI/CD** | User |

### STILL OPEN (will be carried as Architecture conditions unless answered)
| ID | Open item | Impact |
|---|---|---|
| O-03 | **Jira authentication model**: Jira Cloud supports (a) email + API token (Basic), (b) OAuth 2.0 (3LO) user-impersonated, (c) OAuth 2.0 (2LO) service-account via Atlassian Connect/Forge. The choice decides credential storage, who the API acts as, and audit attribution on the Jira side. | Architecture decision input |
| O-04 | **Hub deployment target** (on-prem / MBS internal infra / cloud). Given MBS is a financial-securities company, on-prem or private cloud is likely but must be confirmed. | Architecture decision input |
| O-07 | Expected concurrency: 10–50 members (BRD) are the subjects, but how many Admin/Manager concurrent dashboard viewers? | Caching sizing |
| O-08 | HA / RTO / RPO targets. | Resiliency design |
| O-09 | **DEC-013 audit retention** duration (not addressed by BUSINESS APPROVED either). | Storage sizing + archival |
| O-10 | Security posture: data classification of workload data; secrets management; audit log protection. | Security design |
| O-11 | How the Hub treats worklog from Jira users NOT on the Hub roster: ignored outright, or counted as "project activity" without touching team-MD? | DEC-006 interaction |

### Contradictions
None.

### Blockers
None absolute. All resolved items are evidence-backed; remaining opens can be carried as Architecture conditions per the playbook (`ARCHITECTURE PROPOSED WITH OPEN ITEMS`).

## 11. Traceability Matrix (unchanged — F-01…F-10 ↔ REQ-001…REQ-009)

## 12. Readiness Result

```
SYSTEM CONTEXT DISCOVERED WITH OPEN ITEMS
```

7 UNKNOWNs are now resolved (O-01, O-02, O-05, O-06 explicitly; and the "platform standards" bucket). 7 open items remain (O-03, O-04, O-07, O-08, O-09, O-10, O-11); none is a blocker — each can be an Architecture condition.

---

## ⛔ HUMAN GATE
AI cannot issue `CONTEXT APPROVED` (hard rule in the playbook). The authorised approver must respond with one of:

- `CONTEXT APPROVED` — proceed; remaining O-03/04/07/08/09/10/11 are carried as Architecture conditions.
- `CONTEXT APPROVED WITH CONDITIONS` — supply answers or disposition for the open items.
- `CHANGES REQUIRED`.
- `CONTEXT REJECTED`.

Only after an authorised approval does `02_Architecture_Design.md` start.
