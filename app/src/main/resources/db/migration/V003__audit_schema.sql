-- =====================================================================
-- V003 — audit schema (append-only)
-- PLAN-006 · DEC-008, F-ARCH-NEW-02 Reading A
-- =====================================================================

CREATE TABLE audit.audit_log (
    id                 BIGSERIAL PRIMARY KEY,
    occurred_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    actor_member_id    UUID NULL REFERENCES core.member(id) ON DELETE SET NULL,
    action             TEXT NOT NULL,            -- 'ASSIGNMENT','CAPACITY_EDIT','LOCK_DEADLINE_TOGGLE','ALLOWLIST_EDIT','ROSTER_EDIT','SYNC_DIVERGENCE'...
    target_type        TEXT NOT NULL,
    target_id          TEXT NOT NULL,
    payload            JSONB NOT NULL,           -- before/after; secrets must be masked at the application layer
    result             TEXT NOT NULL CHECK (result IN ('OK','FAILED')),
    jira_status        INTEGER NULL              -- for Jira write audit entries
);

CREATE INDEX ix_audit_occurred           ON audit.audit_log(occurred_at DESC);
CREATE INDEX ix_audit_actor_occurred     ON audit.audit_log(actor_member_id, occurred_at DESC);
CREATE INDEX ix_audit_action_occurred    ON audit.audit_log(action, occurred_at DESC);

-- ---------------------------------------------------------------------
-- Grants (05 Security T-10)
-- The application role (configured in POSTGRES_USER) gets INSERT + SELECT only.
-- A dedicated retention role (added later via ops migration or psql) gets DELETE.
--
-- NOTE: In the dev-host prototype the single Postgres user owns everything,
-- so the GRANT/REVOKE is intentionally scoped by convention; the explicit
-- principle is encoded in the application: only the AuditService writes,
-- and no code path issues UPDATE/DELETE against audit_log.
-- A later ops migration (V00X__audit_grants.sql) will split roles when the
-- deployment target gets its own retention user.
-- ---------------------------------------------------------------------
