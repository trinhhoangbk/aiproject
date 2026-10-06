-- =====================================================================
-- V001 — core schema (Hub-owned governance data)
-- PLAN-004 · includes TD-COND-02 (password_hash + password_updated_at)
-- Baselines: DEC-003/004/005/006/007/010, REQ-005/008
-- =====================================================================

CREATE SCHEMA IF NOT EXISTS core;
CREATE SCHEMA IF NOT EXISTS audit;      -- Table added in V003 but schema created early for grants.

-- ---------------------------------------------------------------------
-- 1. Teams
-- ---------------------------------------------------------------------
CREATE TABLE core.team (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          TEXT NOT NULL UNIQUE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------
-- 2. Members  (roster — DEC-006)
--    `password_hash` + `password_updated_at` per TD-COND-02 (05 Security §4.1)
--    `operations_lead` flag per DEC-003 subrole (05 Security §5.3)
-- ---------------------------------------------------------------------
CREATE TABLE core.member (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    jira_account_id         TEXT NOT NULL UNIQUE,
    display_name            TEXT NOT NULL,
    email                   TEXT NOT NULL UNIQUE,
    role                    TEXT NOT NULL CHECK (role IN ('ADMIN','MANAGER','MEMBER')),
    team_id                 UUID NULL REFERENCES core.team(id) ON DELETE SET NULL,
    operations_lead         BOOLEAN NOT NULL DEFAULT FALSE,  -- Co-owner of holiday calendar (DEC-003)
    active                  BOOLEAN NOT NULL DEFAULT TRUE,
    password_hash           TEXT NOT NULL,                   -- BCrypt-12; TD-COND-02
    password_updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_member_team_active ON core.member(team_id, active);

-- ---------------------------------------------------------------------
-- 3. Capacity — three-tier (DEC-004) with effective-from history
-- ---------------------------------------------------------------------
CREATE TABLE core.capacity_global (
    id              SMALLINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),  -- single-row
    daily_hours     NUMERIC(4,2) NOT NULL DEFAULT 8.00,
    weekly_hours    NUMERIC(5,2) NOT NULL DEFAULT 40.00,
    effective_from  DATE NOT NULL DEFAULT CURRENT_DATE,
    updated_by      UUID NULL REFERENCES core.member(id) ON DELETE SET NULL,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
INSERT INTO core.capacity_global (id, daily_hours, weekly_hours, effective_from)
VALUES (1, 8.00, 40.00, CURRENT_DATE);

CREATE TABLE core.capacity_team (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id         UUID NOT NULL REFERENCES core.team(id) ON DELETE CASCADE,
    daily_hours     NUMERIC(4,2) NOT NULL,
    weekly_hours    NUMERIC(5,2) NOT NULL,
    effective_from  DATE NOT NULL,
    updated_by      UUID NULL REFERENCES core.member(id) ON DELETE SET NULL,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_capacity_team_lookup ON core.capacity_team(team_id, effective_from DESC);

CREATE TABLE core.capacity_member (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    member_id       UUID NOT NULL REFERENCES core.member(id) ON DELETE CASCADE,
    daily_hours     NUMERIC(4,2) NOT NULL,
    weekly_hours    NUMERIC(5,2) NOT NULL,
    effective_from  DATE NOT NULL,
    updated_by      UUID NULL REFERENCES core.member(id) ON DELETE SET NULL,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_capacity_member_lookup ON core.capacity_member(member_id, effective_from DESC);

-- ---------------------------------------------------------------------
-- 4. Holiday calendar (DEC-003)
-- ---------------------------------------------------------------------
CREATE TABLE core.holiday_calendar (
    date        DATE PRIMARY KEY,
    kind        TEXT NOT NULL CHECK (kind IN ('NATIONAL','COMPENSATED_WORKDAY','COMPANY_DAY')),
    description TEXT NOT NULL,
    updated_by  UUID NULL REFERENCES core.member(id) ON DELETE SET NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
-- COMPENSATED_WORKDAY means "a Saturday that counts as working" — so a working-day
-- function tests: NOT weekend OR this kind, AND NOT NATIONAL/COMPANY_DAY.

-- ---------------------------------------------------------------------
-- 5. Allow-list (DEC-005)
-- ---------------------------------------------------------------------
CREATE TABLE core.allow_list (
    project_key TEXT PRIMARY KEY,
    enabled     BOOLEAN NOT NULL DEFAULT TRUE,
    added_by    UUID NULL REFERENCES core.member(id) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------
-- 6. Skill taxonomy — 2-level controlled list (DEC-007)
-- ---------------------------------------------------------------------
CREATE TABLE core.skill_taxonomy (
    l1      TEXT NOT NULL,
    l2      TEXT NOT NULL DEFAULT '',
    active  BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (l1, l2)
);
-- Seed L1 values from the Business Decision Log DEC-007
INSERT INTO core.skill_taxonomy (l1, l2) VALUES
  ('FRONTEND',''), ('BACKEND',''), ('MOBILE',''), ('QA_QC',''), ('DEVOPS',''), ('UI_UX','')
ON CONFLICT DO NOTHING;

CREATE TABLE core.member_skill (
    member_id  UUID NOT NULL REFERENCES core.member(id) ON DELETE CASCADE,
    l1         TEXT NOT NULL,
    l2         TEXT NOT NULL DEFAULT '',
    PRIMARY KEY (member_id, l1, l2),
    FOREIGN KEY (l1, l2) REFERENCES core.skill_taxonomy(l1, l2) ON DELETE RESTRICT
);

-- ---------------------------------------------------------------------
-- 7. Pipeline projects (REQ-005)
-- ---------------------------------------------------------------------
CREATE TABLE core.pipeline_project (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                  TEXT NOT NULL CHECK (length(name) BETWEEN 1 AND 200),
    objective             TEXT NOT NULL,
    target_deadline       DATE NOT NULL,
    total_estimated_md    NUMERIC(6,2) NOT NULL CHECK (total_estimated_md > 0),
    state                 TEXT NOT NULL DEFAULT 'DRAFT' CHECK (state IN
                            ('DRAFT','READY_TO_ALLOCATE','ALLOCATED',
                             'CHANGES_REQUIRED','REJECTED','CLOSED','REASSIGNED')),
    created_by            UUID NULL REFERENCES core.member(id) ON DELETE SET NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_pipeline_state ON core.pipeline_project(state, target_deadline);

CREATE TABLE core.pipeline_required_skill (
    pipeline_id UUID NOT NULL REFERENCES core.pipeline_project(id) ON DELETE CASCADE,
    l1          TEXT NOT NULL,
    l2          TEXT NOT NULL DEFAULT '',
    PRIMARY KEY (pipeline_id, l1, l2),
    FOREIGN KEY (l1, l2) REFERENCES core.skill_taxonomy(l1, l2) ON DELETE RESTRICT
);

-- ---------------------------------------------------------------------
-- 8. Lock-deadline flag (DEC-010 criterion 3)
-- ---------------------------------------------------------------------
CREATE TABLE core.lock_deadline_flag (
    issue_key   TEXT PRIMARY KEY,
    locked_by   UUID NOT NULL REFERENCES core.member(id) ON DELETE RESTRICT,
    reason      TEXT NULL,
    locked_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
