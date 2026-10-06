-- =====================================================================
-- V006 — replace the empty materialized-view shells with plain tables
-- PLAN-018 · the Hub refresher populates these via Spring JPA / native UPSERT.
--
-- Why plain tables instead of materialized views: the DEC-004 three-tier
-- capacity resolution and the DEC-003 holiday-aware working-day count are
-- cheaper and clearer to compute in Java (CapacityResolver + WorkingDayCalculator)
-- than to re-encode in a SQL view definition. The schema name `mv.*` and the
-- band semantics (DEC-009 / DEC-011) stay the same.
-- =====================================================================

DROP MATERIALIZED VIEW IF EXISTS mv.mv_allocation_rate;
DROP MATERIALIZED VIEW IF EXISTS mv.mv_member_overdue;

CREATE TABLE mv.mv_allocation_rate (
    member_id        UUID         NOT NULL,
    horizon          TEXT         NOT NULL,            -- 'this_week' | 'next_2_weeks' | 'next_month'
    window_start     DATE         NOT NULL,
    window_end       DATE         NOT NULL,
    standard_md      NUMERIC(7,2) NOT NULL,
    committed_md     NUMERIC(7,2) NOT NULL,
    available_md     NUMERIC(7,2) NOT NULL,
    allocation_rate  NUMERIC(7,2) NOT NULL,            -- percent, two decimal places
    band             TEXT         NOT NULL,            -- dark_green|light_green|yellow|red (DEC-011)
    refreshed_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (member_id, horizon, window_start)
);
CREATE INDEX ix_mv_alloc_rate_member ON mv.mv_allocation_rate(member_id);

CREATE TABLE mv.mv_member_overdue (
    issue_key        TEXT         NOT NULL,
    member_id        UUID         NOT NULL,
    project_key      TEXT         NOT NULL,
    due_date         DATE         NOT NULL,
    days_overdue     INTEGER      NOT NULL,
    overdue_band     TEXT         NOT NULL,            -- 1_to_3_days | 4_to_7_days | more_than_a_week (F-01)
    remaining_h      NUMERIC(7,2) NULL,
    tpr              NUMERIC(6,3) NULL,                -- DEC-009 RDC
    tpr_band         TEXT         NOT NULL,            -- none | yellow | red
    refreshed_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (issue_key, member_id)
);
CREATE INDEX ix_mv_overdue_member ON mv.mv_member_overdue(member_id);
