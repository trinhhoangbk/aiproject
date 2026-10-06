# ARCHITECTURE APPROVAL · Record

| Field | Value |
|---|---|
| Date | 2026-10-04 16:04 ICT |
| Decision | **`ARCHITECTURE APPROVED WITH CONDITIONS`** |
| Authorized by | Project owner (user) — in-session statement at 16:04 ICT |

---

## 1. Decisions recorded (verbatim, with disposition)

| Condition | User's answer (verbatim) | AI disposition |
|---|---|---|
| C-APP-01 (O-03 Jira auth) | "API Token" | ADR-ARCH-010 locked to **API Token + service-account**. |
| C-APP-02 (O-04 deployment) | "Deployment bản free của jira" | **See §2 — this is a Jira plan choice, not a Hub deployment target.** |
| C-APP-03 (O-09 audit retention) | "không cần lưu trữ" | **See §3 — read as "no long-term retention mandate"; audit log still produced per DEC-008.** |
| C-APP-04 (O-07 concurrency) | "150" | Peak concurrent viewers = **150**. Capacity-sizing input (see §4). |
| C-APP-05 (O-08 HA/RTO/RPO) | "critical business hours" | Business-hours-critical only; after-hours best-effort. |
| C-APP-06 (O-10 secrets) | "tự quyết định" | AI → externalised config via env vars + `.env` file (perm 0600). Manual rotation. See §5. |
| C-APP-07 (O-11 non-roster worklog) | "tự quyết định" | AI → stored in `worklog_projection` for traceability; **excluded** from team-MD aggregates. See §5. |

## 2. ⚠️ Material note — "bản free của jira"

The user's answer refers to the **Jira plan**, not the Hub's deployment target. These are different things, so this condition decides one axis and leaves the other open:

### 2.1 Jira plan — **CONFIRMED Jira Cloud Free**
- Previously CONTEXT APPROVED locked Jira edition to Jira Cloud (C-13). The user now narrows it to the **Free** tier of Jira Cloud.
- Jira Cloud Free is capped at **10 users** per site.
- Jira Cloud Free **does** support webhooks and REST API v3; the architecture integration surface works at this tier.

### 2.2 Scope conflict with REQ-009 (10–50 members)
- REQ-009 target: *"team dashboard ≤ 2.5 s with 10–50 members"*.
- Jira Cloud Free seat cap: 10 users.
- If the actual team is > 10 members, **the project must move to Jira Standard (or higher)** before go-live, or REQ-009 scope must be revised to ≤ 10 members.
- This is a **procurement / scope decision**, not an architecture decision. The architecture itself does not change.

Flagged as **F-ARCH-NEW-01** (status: HIGH, owner: Project owner / Business). Suggested resolutions:
- **Option A** — confirm team size ≤ 10 members. REQ-009 wording stays; Jira Free is sufficient.
- **Option B** — stay on Jira Free for the prototype only, upgrade to Standard before onboarding the 11th member. Mark as deferred procurement.
- **Option C** — move to Jira Standard now. Costs money.

### 2.3 Hub deployment target — **STILL OPEN**
"Bản free của jira" does not answer "where does the Hub run?". Carrying as follow-up:
- Working default (same spirit as "free"): **self-hosted container** on an existing MBS internal host or developer workstation; no cloud spend.
- ADR-ARCH-011 stays PROPOSED until deployment host is picked.

## 3. ⚠️ Material note — "không cần lưu trữ"

DEC-008 (BUSINESS APPROVED) states: *"Kích hoạt đồng bộ hai chiều (Two-way sync) có ghi nhận nhật ký (Audit log)."*

The user's answer "không cần lưu trữ" (do not need to retain) is incompatible with "no audit at all". Two readings:

- **Reading A — "no retention duration mandate"**: audit log still written per DEC-008, but no retention policy enforced; operator may truncate at any time. **This is the reading applied by this record** because it does not contradict DEC-008.
- **Reading B — "no audit at all"**: directly contradicts DEC-008 and would require reopening Requirement.

If the user intended Reading B, this record must be re-issued and `BASELINE CHANGE REQUIRED` routed to `01-REQUIREMENT/02_Requirement_Clarification.md` to re-open DEC-008.

Flagged as **F-ARCH-NEW-02** (status: MEDIUM; please confirm which reading applies).

Operational default under Reading A: Kafka topic `hub.audit` keeps 7 days (Kafka default); `audit_log` DB table rolling 30 days. Both tunable to 0 by the operator at any time.

## 4. Capacity-sizing input (O-07 = 150)

Previously assumed ≤ 50 concurrent viewers. New target: **150 concurrent viewers at peak**.

Impact:
- Spring Boot connection pool: raise to ~80–100 DB connections (from default 10).
- JVM heap: ~1 GB headroom per 50 viewers; recommend **≥ 3 GB** heap on the Hub JVM.
- Postgres: ensure `max_connections ≥ 200` with pgBouncer in front if the deployment is on-prem.
- Materialized views remain the right lever; no architecture change required.

Note that O-07 = 150 **concurrent viewers** is independent of REQ-009's **10–50 members**: a Member is a workload-subject (gets rows in the heatmap), a viewer is a browser session (looks at the heatmap). This is self-consistent **provided §2.2 is resolved**.

Flagged as **F-ARCH-NEW-03** (status: LOW; design absorbs with sizing changes above).

## 5. AI-made decisions on delegated items

### O-10 — Secrets backend
- **Decision**: externalised config via environment variables; file-system `.env` file with permission `0600` and ownership by the Hub process user.
- **Reason**: no CI/CD, no cloud secrets service in scope; low-cost sensibility (consistent with the "free" posture in §2).
- **Trade-off**: rotation is manual; not a secrets vault. Documented in ops runbook.
- **Open**: upgrade to HashiCorp Vault or similar if the deployment host (§2.3) later admits it.

### O-11 — Non-roster worklog treatment
- **Decision**: worklog from Jira users not on the Hub roster is **projected** into `worklog_projection` for traceability (project-view-only); **excluded** from team-MD aggregates (consistent with DEC-006).
- **Reason**: preserves auditability of project activity without inflating team-MD budget; aligns with the "project-view-only" default suggested at Review §8 (C-APP-07).
- **Open**: none.

## 6. Updated ADR status

| ADR | Status after approval |
|---|---|
| ADR-ARCH-001 Jira Cloud + API v3 | **LOCKED**, narrowed to Jira Cloud **Free** (see §2.1). |
| ADR-ARCH-002 Modular monolith | **LOCKED**. |
| ADR-ARCH-003 Single Postgres | **LOCKED**. |
| ADR-ARCH-004 Kafka bus | **LOCKED**. |
| ADR-ARCH-005 Webhook + reconcile | **LOCKED**. |
| ADR-ARCH-006 Sync write-back | **LOCKED**. |
| ADR-ARCH-007 RBAC in depth | **LOCKED**. |
| ADR-ARCH-008 No CI/CD v1 | **LOCKED**. |
| ADR-ARCH-009 Materialized views | **LOCKED**; sizing per §4. |
| ADR-ARCH-010 Jira auth = **API Token service-account** | **LOCKED** per C-APP-01. |
| ADR-ARCH-011 Deployment target | **STILL OPEN** (§2.3). Working default = self-hosted container. |

## 7. New findings carried forward

| ID | Severity | Blocker? | Required action |
|---|---|---|---|
| F-ARCH-NEW-01 | HIGH | No, but **MUST confirm** team size vs. Jira Free seat cap before onboarding | Procurement decision — §2.2 options A/B/C |
| F-ARCH-NEW-02 | MEDIUM | No, but needs confirmation that Reading A is intended | Confirm; otherwise reopen DEC-008 |
| F-ARCH-NEW-03 | LOW | No | Absorbed in capacity-sizing §4 |

## 8. Next authorized stage

`03-TECHNICAL-DESIGN/*` — **not yet present** in the repository `new project/`.
Technical Design playbooks have not been supplied. When they are added, Technical Design is the next step.

Until then, the AI stops here. Open items `F-ARCH-NEW-01` (Jira seat cap) and `F-ARCH-NEW-02` (audit intent) should be resolved before Technical Design proceeds.

---
⛔ **STOP — pipeline boundary reached.**
