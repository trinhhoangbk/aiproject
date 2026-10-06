-- =====================================================================
-- V004 — materialized views for heatmap + overdue
-- PLAN-007 · REQ-009 (p95 ≤ 2.5 s), DEC-011 (AR bands), DEC-009 (TPR), F-DB-03 (unique indexes)
-- Thin v1 views; full horizon/capacity computation lives in the application
-- which can REFRESH with its own derivation. These shells guarantee the
-- UNIQUE indexes required for `REFRESH MATERIALIZED VIEW CONCURRENTLY`.
-- =====================================================================

CREATE SCHEMA IF NOT EXISTS mv;

-- ---------------------------------------------------------------------
-- Allocation Rate band (per-member × per-horizon)
-- Window values: 'this_week', 'next_2_weeks', 'next_month'.
-- Full computation (standard_md / committed_md / available_md) is populated
-- by the Hub refresher using the three-tier capacity + holiday calendar.
-- ---------------------------------------------------------------------
CREATE MATERIALIZED VIEW mv.mv_allocation_rate AS
SELECT
    m.id                        AS member_id,
    'this_week'::text           AS horizon,
    date_trunc('week', (now() AT TIME ZONE 'Asia/Saigon'))::date      AS window_start,
    (date_trunc('week', (now() AT TIME ZONE 'Asia/Saigon')) + interval '7 days')::date AS window_end,
    0::numeric(7,2)             AS standard_md,
    0::numeric(7,2)             AS committed_md,
    0::numeric(7,2)             AS available_md,
    0::numeric(7,2)             AS allocation_rate,
    'dark_green'::text          AS band
FROM core.member m
WHERE FALSE;   -- empty shell; real rows produced by the Hub refresher.

CREATE UNIQUE INDEX ux_mv_alloc_rate ON mv.mv_allocation_rate(member_id, horizon, window_start);

-- ---------------------------------------------------------------------
-- Overdue + Early-Warning (TPR per DEC-009)
-- ---------------------------------------------------------------------
CREATE MATERIALIZED VIEW mv.mv_member_overdue AS
SELECT
    m.id                       AS member_id,
    i.issue_key                AS issue_key,
    i.project_key              AS project_key,
    i.due_date                 AS due_date,
    0::integer                 AS days_overdue,
    'none'::text               AS overdue_band,     -- '1_to_3_days' | '4_to_7_days' | 'more_than_a_week'
    i.remaining_estimate_h     AS remaining_h,
    0::numeric(6,3)            AS tpr,              -- DEC-009 RDC = remaining ÷ (RWD × member daily hours)
    'none'::text               AS tpr_band          -- 'none' | 'yellow' | 'red'
FROM core.member m
JOIN jira.issue_projection i
  ON i.assignee_account_id = m.jira_account_id
WHERE FALSE;   -- shell populated by the refresher per event.

CREATE UNIQUE INDEX ux_mv_member_overdue ON mv.mv_member_overdue(issue_key, member_id);

-- Initial empty refresh (safe since both shells WHERE FALSE)
REFRESH MATERIALIZED VIEW mv.mv_allocation_rate;
REFRESH MATERIALIZED VIEW mv.mv_member_overdue;
