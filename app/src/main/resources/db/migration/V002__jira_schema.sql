-- =====================================================================
-- V002 — Jira projection cache + event dedup
-- PLAN-005 · REQ-007, DEC-001 (statusCategory + Discarded flag)
-- =====================================================================

CREATE SCHEMA IF NOT EXISTS jira;

-- ---------------------------------------------------------------------
-- Issue projection (Jira is the SoR; this is a cache)
-- ---------------------------------------------------------------------
CREATE TABLE jira.issue_projection (
    issue_key                 TEXT PRIMARY KEY,
    project_key               TEXT NOT NULL,
    assignee_account_id       TEXT NULL,
    status                    TEXT NOT NULL,
    status_category           TEXT NOT NULL CHECK (status_category IN ('new','indeterminate','done')),  -- DEC-001
    resolution                TEXT NULL,
    discarded                 BOOLEAN NOT NULL DEFAULT FALSE,  -- resolution ∈ {Won't Fix, Duplicate, Invalid}
    priority                  TEXT NULL,
    labels                    TEXT[] NOT NULL DEFAULT '{}',
    fix_version               TEXT NULL,
    fix_version_release_date  DATE NULL,                       -- DEC-010 criterion 1
    due_date                  DATE NULL,
    original_estimate_h       NUMERIC(7,2) NULL,
    remaining_estimate_h      NUMERIC(7,2) NULL,
    jira_updated_at           TIMESTAMPTZ NOT NULL,            -- UPSERT monotonic guard
    last_seen_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    allow_list_ok             BOOLEAN NOT NULL
);

CREATE INDEX ix_issue_assignee_status_cat ON jira.issue_projection(assignee_account_id, status_category);
CREATE INDEX ix_issue_project             ON jira.issue_projection(project_key);
CREATE INDEX ix_issue_due_active          ON jira.issue_projection(due_date)
  WHERE status_category <> 'done';
CREATE INDEX ix_issue_fix_version         ON jira.issue_projection(fix_version);

-- ---------------------------------------------------------------------
-- Worklog projection
-- ---------------------------------------------------------------------
CREATE TABLE jira.worklog_projection (
    id                BIGINT PRIMARY KEY,                      -- Jira worklog id
    issue_key         TEXT NOT NULL REFERENCES jira.issue_projection(issue_key) ON DELETE CASCADE,
    project_key       TEXT NOT NULL,
    jira_account_id   TEXT NOT NULL,
    in_roster         BOOLEAN NOT NULL,                        -- O-11 answer: non-roster worklog is projected but excluded from team-MD
    started_at        TIMESTAMPTZ NOT NULL,
    duration_seconds  INTEGER NOT NULL CHECK (duration_seconds > 0),
    jira_updated_at   TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_worklog_account_started  ON jira.worklog_projection(jira_account_id, started_at);
CREATE INDEX ix_worklog_project_started  ON jira.worklog_projection(project_key, started_at);

-- ---------------------------------------------------------------------
-- Event dedup (AC-007.1/.3)
-- ---------------------------------------------------------------------
CREATE TABLE jira.jira_event_dedup (
    jira_event_id  TEXT NOT NULL,
    event_type     TEXT NOT NULL,
    received_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (jira_event_id, event_type)
);
CREATE INDEX ix_dedup_received ON jira.jira_event_dedup(received_at);
-- TTL: a nightly job prunes rows older than 7 days.
