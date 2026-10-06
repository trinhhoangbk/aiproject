# TECHNICAL DESIGN APPROVAL · Record

| Field | Value |
|---|---|
| Date | 2026-10-04 16:20 ICT |
| Decision | **`TECHNICAL DESIGN APPROVED`** |
| Authorized by | Project owner (user) — in-session statement |

## 1. Design package approved (as-is)
- `01_Technical_Design_RESULT.md` — parent blueprint, 12 TD decisions.
- `02_API_Contract_Design_RESULT.md` — Hub REST + Jira webhook.
- `03_Database_Design_RESULT.md` — Postgres schema `core` / `jira` / `audit` / `mv`.
- `04_Integration_Design_RESULT.md` — Jira read/write + webhook + Kafka topics.
- `05_Security_Design_RESULT.md` — RBAC + HMAC + secrets + 16 threats.
- `06_Technical_Design_Review_RESULT.md` — PASSED WITH CONDITIONS.

## 2. Conditions elevated to Development action items

| ID | Action | Owner | Done when |
|---|---|---|---|
| COND-01 | Tunnel exposes **only** `/webhooks/jira`; UI stays on `localhost`. Operator reaches UI via SSH tunnel or similar. | Operator + Development | ingress rule verified; UI unreachable from the public tunnel URL |
| COND-02 | Add `password_hash TEXT NOT NULL` and `password_updated_at TIMESTAMPTZ` columns to `core.member` in `V001__core_schema.sql`. | Development | Flyway migration contains both columns |
| COND-03 | Document a **90-day Jira API Token rotation checklist** in `docs/runbook.md`. | Operations | Runbook section present |
| COND-04 | Adaptive reconciler cadence: on observed Jira `429`, double the next tick's cadence (bounded). | Development | Config + metric gate implemented |

## 3. Baseline frozen for Planning
- REQ-001…009 (business), B-RULE-01…04 + BR-DEC-01…11.
- 11 ADR-ARCH-* (all LOCKED).
- 12 TD-*; 1 HIGH + 3 MEDIUM findings disposed as COND-01…COND-04.

## 4. Next authorized stage
`04-PLANNING/…` — **not yet present** in the repository.
Pipeline stops here until Planning playbooks are supplied.
