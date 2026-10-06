# 03 — DATABASE DESIGN · Result

| Field | Value |
|---|---|
| Stage | 03-TECHNICAL-DESIGN / 03 Database Design |
| Date | 2026-10-04 16:08 ICT |
| Role | Data/Database Architect (AI) |
| Target | PostgreSQL 16 (ADR-ARCH-003, C-16); Flyway managed (TD-002) |
| **Status** | **🟡 DATABASE DESIGN COMPLETE WITH OPEN ITEMS** |

---

## 1. Data Design Scope

Define the schema that satisfies REQ-001…009 given the ownership table (04 Domain §H) and the two materialized views required by REQ-009. One Postgres database, logical schemas per bounded concern. Flyway migrations are **forward-only** (TD-010).

## 2. Current-State Evidence

Greenfield. No incumbent schema. No ORM entity to reconcile.

## 3. Logical Model

### 3.1 Entities and relationships (business level)

- `Member (1) ── (1) MemberProfile ── (0..1) CapacityOverride(member-tier)`
- `Team (1) ── (0..*) Member`, `Team (1) ── (0..1) CapacityOverride(team-tier)`
- `Global CapacityDefault (0..1)`
- `HolidayCalendar (1) ── (0..*) HolidayEntry`
- `AllowList (1) ── (0..*) AllowListProjectKey`
- `SkillTaxonomy (1) ── (0..*) SkillTag (L1, L2)`
- `Member (1) ── (0..*) MemberSkill (SkillTag)`
- `PipelineProject (1) ── (0..*) PipelineRequiredSkill`
- `IssueProjection (1) ── (0..*) WorklogProjection`
- `IssueProjection (1) ── (0..1) LockDeadlineFlag`
- `AssignmentAudit (append-only)`
- `JiraEventDedup (short-lived)`

### 3.2 Invariants (DB-enforceable)

- A `member` row has exactly one `jira_account_id` and it is unique.
- A `capacity_override` row at `member` tier takes precedence over `team` tier takes precedence over `global` default (enforced in app logic, not DB).
- `allow_list.project_key` is unique.
- `pipeline_required_skill.l1` must exist in `skill_taxonomy.l1`; same for `l2` if provided.
- `audit_log` is **append-only** (`REVOKE UPDATE, DELETE` on the role used by the app; only a retention job truncates by date).

## 4. Physical Schema Proposal

One database (`hub`) with logical schemas. SQL style only; no production migration code.

### 4.1 Schema layout

```
hub
├── core/
│   ├── member
│   ├── team
│   ├── member_profile
│   ├── capacity_global
│   ├── capacity_team
│   ├── capacity_member
│   ├── holiday_calendar
│   ├── allow_list
│   ├── skill_taxonomy
│   ├── member_skill
│   ├── pipeline_project
│   ├── pipeline_required_skill
│   └── lock_deadline_flag
├── jira/
│   ├── issue_projection
│   ├── worklog_projection
│   └── jira_event_dedup
├── audit/
│   └── audit_log
└── mv/
    ├── mv_allocation_rate
    └── mv_member_overdue
```

### 4.2 Table definitions (design-level, not executable DDL)

**core.member**
- `id UUID PK`
- `jira_account_id TEXT UNIQUE NOT NULL`
- `display_name TEXT NOT NULL`
- `email TEXT NOT NULL`
- `role TEXT NOT NULL CHECK (role IN ('ADMIN','MANAGER','MEMBER'))`
- `team_id UUID NULL REFERENCES core.team(id)`
- `active BOOLEAN NOT NULL DEFAULT true`
- `created_at, updated_at TIMESTAMPTZ`

**core.team**
- `id UUID PK`, `name TEXT UNIQUE NOT NULL`, timestamps.

**core.member_profile**
- `member_id UUID PK REFERENCES core.member(id)`
- `notes TEXT NULL`
- update timestamp.

**core.capacity_global**
- `id SMALLINT PK CHECK (id = 1)` — single-row.
- `daily_hours NUMERIC(4,2) NOT NULL DEFAULT 8.00`
- `weekly_hours NUMERIC(5,2) NOT NULL DEFAULT 40.00`
- `effective_from DATE NOT NULL`
- audit timestamp + `updated_by UUID`.

**core.capacity_team** / **core.capacity_member**
- Same shape; FK to team / member; `effective_from DATE NOT NULL`.
- A member or team may have **a history** of capacity rows; "current" is the row with the latest `effective_from ≤ today`.

**core.holiday_calendar**
- `date DATE PK`
- `kind TEXT CHECK (kind IN ('NATIONAL','COMPENSATED_WORKDAY','COMPANY_DAY'))`
- `description TEXT`
- `updated_by UUID`, timestamp.

**core.allow_list**
- `project_key TEXT PK` (Jira project key as per DEC-005)
- `enabled BOOLEAN NOT NULL DEFAULT true`
- `added_by UUID`, timestamps.

**core.skill_taxonomy**
- `l1 TEXT NOT NULL`, `l2 TEXT NULL`
- `PK (l1, COALESCE(l2,''))`
- `active BOOLEAN NOT NULL DEFAULT true`.

**core.member_skill**
- `member_id UUID`, `l1 TEXT`, `l2 TEXT NULL`
- `PK (member_id, l1, COALESCE(l2,''))`
- `FK (l1, l2) → skill_taxonomy`.

**core.pipeline_project**
- `id UUID PK`
- `name TEXT NOT NULL CHECK (length(name) BETWEEN 1 AND 200)`
- `objective TEXT NOT NULL`
- `target_deadline DATE NOT NULL`
- `total_estimated_md NUMERIC(6,2) NOT NULL CHECK (total_estimated_md > 0)`
- `state TEXT NOT NULL CHECK (state IN ('DRAFT','READY_TO_ALLOCATE','ALLOCATED','CHANGES_REQUIRED','REJECTED','CLOSED','REASSIGNED'))` (04 Domain §F; F-05 accepted)
- `created_by UUID`, timestamps.

**core.pipeline_required_skill**
- `pipeline_id UUID`, `l1 TEXT`, `l2 TEXT NULL`
- `PK (pipeline_id, l1, COALESCE(l2,''))`.

**core.lock_deadline_flag**
- `issue_key TEXT PK` (DEC-010 criterion 3)
- `locked_by UUID NOT NULL`, `reason TEXT`, `locked_at TIMESTAMPTZ`.

**jira.issue_projection**
- `issue_key TEXT PK`
- `project_key TEXT NOT NULL`
- `assignee_account_id TEXT NULL`
- `status TEXT NOT NULL`
- `status_category TEXT NOT NULL CHECK (status_category IN ('new','indeterminate','done'))` (DEC-001)
- `resolution TEXT NULL`
- `discarded BOOLEAN NOT NULL DEFAULT false` (true when resolution ∈ Won't Fix/Duplicate/Invalid)
- `priority TEXT NULL`
- `labels TEXT[] NOT NULL DEFAULT '{}'`
- `fix_version TEXT NULL`
- `fix_version_release_date DATE NULL` (DEC-010 criterion 1 trigger)
- `due_date DATE NULL`
- `original_estimate_h NUMERIC(7,2) NULL`
- `remaining_estimate_h NUMERIC(7,2) NULL`
- `jira_updated_at TIMESTAMPTZ NOT NULL` (monotonic UPSERT guard)
- `last_seen_at TIMESTAMPTZ NOT NULL`
- `allow_list_ok BOOLEAN NOT NULL` (derived; false → excluded from aggregates per DEC-005).

**jira.worklog_projection**
- `id BIGINT PK` (Jira worklog id)
- `issue_key TEXT NOT NULL REFERENCES jira.issue_projection(issue_key) ON DELETE CASCADE`
- `project_key TEXT NOT NULL`
- `jira_account_id TEXT NOT NULL`
- `in_roster BOOLEAN NOT NULL` (derived from `core.member`; see O-11 handling below)
- `started_at TIMESTAMPTZ NOT NULL`
- `duration_seconds INTEGER NOT NULL CHECK (duration_seconds > 0)`
- `jira_updated_at TIMESTAMPTZ NOT NULL`.

**jira.jira_event_dedup**
- `jira_event_id TEXT`, `event_type TEXT`, `received_at TIMESTAMPTZ`
- `PK (jira_event_id, event_type)`.
- TTL: rows older than 7 days removed by a nightly job.

**audit.audit_log** (append-only)
- `id BIGSERIAL PK`
- `occurred_at TIMESTAMPTZ NOT NULL DEFAULT now()`
- `actor_member_id UUID NULL` (null for system)
- `action TEXT NOT NULL` (e.g. `ASSIGNMENT`, `CAPACITY_EDIT`, `LOCK_DEADLINE_TOGGLE`, `ALLOWLIST_EDIT`)
- `target_type TEXT NOT NULL`, `target_id TEXT NOT NULL`
- `payload JSONB NOT NULL` (before/after values; redacted secrets)
- `result TEXT NOT NULL CHECK (result IN ('OK','FAILED'))`
- `jira_status INTEGER NULL` (for Jira calls).
- Grants: app role has `INSERT, SELECT` only; retention job role has `DELETE WHERE occurred_at < now() - interval 'N days'`.

**mv.mv_allocation_rate** (materialized view, refreshed CONCURRENTLY on event)
- Columns: `member_id, window, window_start, window_end, standard_md, committed_md, available_md, allocation_rate, band`.
- **Required unique index** for CONCURRENTLY: `UNIQUE (member_id, window, window_start)`.
- Underlying SQL joins `core.member`, effective capacity across the 3 tiers, holiday calendar, `jira.issue_projection` (filtered by `allow_list_ok AND status_category != 'done' AND assignee_account_id = member.jira_account_id`), applies B-RULE-01/02, derives band per DEC-011.

**mv.mv_member_overdue**
- Columns: `member_id, issue_key, project_key, due_date, days_overdue, overdue_band, remaining_h, tpr, tpr_band`.
- `UNIQUE (issue_key, member_id)` for CONCURRENTLY.
- Underlying SQL: all issues where `due_date < current_date AT TIME ZONE 'Asia/Saigon' AND status_category <> 'done' AND allow_list_ok`.
- `overdue_band` per F-01 bands (1_to_3, 4_to_7, more_than_a_week).
- `tpr_band` per DEC-009.

## 5. Keys / Constraints / Indexes

| Table | Index | Purpose |
|---|---|---|
| `core.member` | `(jira_account_id)` unique (already) | O(1) member lookup from Jira events |
| `core.member` | `(team_id, active)` | team aggregates |
| `core.capacity_member` | `(member_id, effective_from DESC)` | fetch current override |
| `core.capacity_team` | `(team_id, effective_from DESC)` | fetch current override |
| `core.allow_list` | `(project_key)` unique (PK) | O(1) scope check |
| `core.pipeline_project` | `(state, target_deadline)` | pipeline listing |
| `core.lock_deadline_flag` | `(issue_key)` unique (PK) | overload calc join |
| `jira.issue_projection` | `(assignee_account_id, status_category)` | workload view |
| `jira.issue_projection` | `(project_key)` | by-project aggregates |
| `jira.issue_projection` | `(due_date) WHERE status_category <> 'done'` partial | overdue filter |
| `jira.issue_projection` | `(fix_version)` | Hard-Deadline criterion 1 |
| `jira.worklog_projection` | `(jira_account_id, started_at)` | daily report |
| `jira.worklog_projection` | `(issue_key)` | FK + join |
| `jira.worklog_projection` | `(project_key, started_at)` | daily per-project totals |
| `audit.audit_log` | `(occurred_at DESC)` | time-ordered reads |
| `audit.audit_log` | `(actor_member_id, occurred_at DESC)` | per-actor audit |
| `mv.mv_allocation_rate` | `(member_id, window, window_start)` unique | REFRESH CONCURRENTLY |
| `mv.mv_member_overdue` | `(issue_key, member_id)` unique | REFRESH CONCURRENTLY |

## 6. Transaction & Consistency Model

| Operation | Transaction | Isolation |
|---|---|---|
| Inbound Jira event → projection UPSERT + dedup insert | 1 TX | Default (`READ COMMITTED`) |
| Materialized-view refresh | separate from the ingest TX, debounced | `REFRESH MATERIALIZED VIEW CONCURRENTLY` |
| Assign/Re-assign (DEC-008): Jira writes **outside** DB TX; audit write **after** Jira response | separate TX | `READ COMMITTED` |
| Roster/capacity/pipeline edits | 1 TX | `READ COMMITTED` with optional `SELECT … FOR UPDATE` on PATCH (optimistic by ETag preferred) |

Business consistency:
- Jira projection is eventually consistent with Jira (bounded by freshness SLO 5 min).
- Hub-owned schemas are strongly consistent per Postgres transactions.
- Audit is best-effort durable; retention policy allows pruning.

## 7. Concurrency Strategy

- **UPSERT guard** on `jira.issue_projection` uses `jira_updated_at` monotonicity: `INSERT … ON CONFLICT (issue_key) DO UPDATE SET … WHERE EXCLUDED.jira_updated_at >= jira.issue_projection.jira_updated_at`.
- **Dedup** key `(jira_event_id, event_type)` guarantees idempotency on inbound events (AC-007.1/.3).
- **Hub edits**: optimistic concurrency via `updated_at` ETag on PATCH; 409 on conflict (API §8).
- **Audit** inserts are append-only; no concurrency control needed.
- **Materialized view** refresh is single-flight per view (app-level lock) to avoid refresh thrash.

## 8. Migration / Backfill Plan

Flyway files (initial set):
- `V001__core_schema.sql` — all `core.*` tables + skill taxonomy + seeded VN holidays for current year.
- `V002__jira_schema.sql` — `jira.*` tables.
- `V003__audit_schema.sql` — `audit.audit_log` with grants.
- `V004__materialized_views.sql` — the two MVs + unique indexes + initial `REFRESH`.
- `V005__bootstrap_admin.sql` — single-row insert using `HUB_BOOTSTRAP_ADMIN_EMAIL` env variable (via a one-shot script invoked by Flyway `callback` or by the app on first start).

Backfill:
- Jira projection is populated by the reconciler on first start: iterate every allow-listed project with `JQL = updated > -90d` (configurable), paginate.
- No business data to migrate (greenfield).

## 9. Rollback / Reversibility

- **Flyway is forward-only** (TD-010). To rollback a schema change, author `V00N+1__revert_...sql`.
- Projection data: fully rebuildable from Jira (ADR-ARCH-003 recoverability); drop and re-sync is acceptable.
- Audit data: deliberately low-durability by business decision (F-ARCH-NEW-02 Reading A); loss is tolerated.
- Materialized views: rebuilt by `REFRESH MATERIALIZED VIEW`.

## 10. Data Verification / Reconciliation

- Hourly reconciler compares `jira.issue_projection.jira_updated_at` with Jira's own `updated` field per issue; divergence > 0 is logged to `audit_log` with `action='SYNC_DIVERGENCE'`.
- Nightly job rebuilds both materialized views in full to catch any drift from incremental refreshes.
- Smoke-test queries documented in `docs/runbook.md`:
  - `SELECT count(*) FROM jira.issue_projection WHERE allow_list_ok`;
  - `SELECT max(occurred_at) FROM audit.audit_log`;
  - `SELECT state, count(*) FROM core.pipeline_project GROUP BY state`.

## 11. Risks

| ID | Severity | Risk |
|---|---|---|
| DB-R-01 | MEDIUM | REFRESH CONCURRENTLY requires exclusive lock briefly; with 10–50 members and small MV sizes this is sub-second. Monitor `materialized_view_refresh_duration_seconds`. |
| DB-R-02 | LOW | `TEXT[]` for labels is Postgres-specific; swap to `JSONB` later if migration needed. |
| DB-R-03 | LOW | Capacity history grows over years but is small; prune policy OPEN. |
| DB-R-04 | LOW | ADR-ARCH-003 cache strategy sized for 50 members; at 150 concurrent viewers (O-07) connection pool is the limit, not row count. |

## 12. Traceability

| AC | Table / View |
|---|---|
| AC-001.* | `jira.issue_projection`, `core.capacity_*`, `core.lock_deadline_flag`, `mv.mv_allocation_rate` |
| AC-002.* | `jira.worklog_projection`, `jira.issue_projection.status_category/discarded` |
| AC-003.* | `mv.mv_member_overdue` |
| AC-004.* | `jira.issue_projection.remaining_estimate_h`, `jira.worklog_projection` (α calculation) |
| AC-005.* | `core.pipeline_project`, `core.pipeline_required_skill` |
| AC-006.* | `mv.mv_allocation_rate`, `audit.audit_log`, `core.member.jira_account_id` |
| AC-007.* | `jira.jira_event_dedup`, `jira.issue_projection.jira_updated_at` |
| AC-008.* | Row-level filters in app layer referencing `core.member.role` |

## 13. Open Items

| ID | Item |
|---|---|
| DB-O-01 | Capacity-history pruning policy (keep forever vs. keep 5 y). |
| DB-O-02 | Full-list of controlled skill-taxonomy L1/L2 values beyond the seeded set (DEC-007 open vocab). |
| DB-O-03 | Secondary indexes may be adjusted after live profiling; initial set above is a safe default. |

## 14. Status

```
DATABASE DESIGN COMPLETE WITH OPEN ITEMS
```

---
⛔ **STOP — stage boundary.** Next: `04_Integration_Design.md`.
