# 01 — TECHNICAL DESIGN · Result

| Field | Value |
|---|---|
| Stage | 03-TECHNICAL-DESIGN / 01 Technical Design |
| Date | 2026-10-04 16:08 ICT |
| Role | Senior/Principal Engineer (AI) — blueprint author, no production code |
| Preconditions | BUSINESS/CONTEXT/ARCHITECTURE APPROVED (incl. Clarifications 2026-10-04 16:08) ✅ |
| **Status** | **🟡 TECHNICAL DESIGN DRAFTED WITH OPEN ITEMS** |

---

## 1. Technical Design Summary

Single Spring Boot application (Java 21, Gradle). One Postgres database with logical schemas and two materialized views for the heatmap and overdue list. One Kafka broker with four topics. Jira Cloud Free (v3 REST + webhooks) is the only external integrand. The whole stack runs locally on a developer machine via Docker Compose; a tunneling tool (ngrok / Cloudflare Tunnel) exposes the webhook endpoint publicly to Jira Cloud. Spring Security protects the Hub API with session + method-level RBAC. Flyway owns schema migrations. No CI/CD; releases are manual tags of the Gradle build. Audit is best-effort (short retention).

## 2. Approved Baselines (versions used)

| Baseline | Version used |
|---|---|
| REQ-001 … REQ-009 | BUSINESS APPROVED 2026-10-04 15:46 |
| B-RULE-01…04 + BR-DEC-01…11 | 02 Clarification 2026-10-04 15:36 |
| ADR-ARCH-001…011 | ARCHITECTURE APPROVED 2026-10-04 16:04 + Clarifications 16:08 |
| Context constraints C-01…C-17 | CONTEXT APPROVED 2026-10-04 15:54 |

## 3. Repository Change Surface

Greenfield. Target repository layout to be created inside `new project/` (keeping existing playbook / Context Layer intact):

```
new project/
├── CLAUDE.md, .claude/            (existing, untouched)
├── 00-PROJECT-ONBOARDING/, 01-REQUIREMENT/, 02-ARCHITECTURE_FINAL/, 03-TECHNICAL-DESIGN/
├── app/                           ← Spring Boot application (new)
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── java/com/mbs/hub/
│       │   │   ├── HubApplication.java
│       │   │   ├── config/
│       │   │   ├── security/
│       │   │   ├── jira/            ← adapter: client, webhook controller, DTOs, ADF
│       │   │   ├── roster/
│       │   │   ├── capacity/
│       │   │   ├── holiday/
│       │   │   ├── allowlist/
│       │   │   ├── skill/
│       │   │   ├── pipeline/
│       │   │   ├── workload/        ← Overload + heatmap + ETA
│       │   │   ├── overdue/         ← Overdue + Early-Warning
│       │   │   ├── reporting/      ← Daily done + worklog
│       │   │   ├── audit/
│       │   │   ├── sync/            ← Reconciler + consumer wiring
│       │   │   └── materialized/
│       │   └── resources/
│       │       ├── application.yml
│       │       └── db/migration/    ← Flyway SQL migrations
│       └── test/
├── deploy/
│   ├── docker-compose.yml           ← Postgres, Kafka, Hub app
│   ├── .env.example
│   └── RELEASE.md
└── docs/
    ├── runbook.md
    └── ADF-examples.md
```

Technical Design does **not** create these files; it defines them. Writing them is Development stage.

## 4. Component-Level Design

| Component | Package | Responsibility | Depends on | Produces |
|---|---|---|---|---|
| `HubApplication` | `com.mbs.hub` | Boot entrypoint, scheduler, Kafka config | Spring Boot auto-config | — |
| **Jira Adapter** | `com.mbs.hub.jira` | HTTP client (Spring `RestClient` + API-Token basic auth), DTOs, ADF JSON builder, rate-limit guard | ADR-ARCH-001/010 | `JiraIssue`, `JiraWorklog`, `AdfDoc` |
| **Jira Webhook Controller** | `jira.webhook` | Verifies HMAC, publishes canonical event to Kafka | Jira Adapter DTOs | `JiraEventPublished` |
| **Jira Reconciler** | `sync.reconciler` | `@Scheduled("0 0 * * * *")` hourly full-sync of allow-listed projects; emits `sync.command` and `jira.inbound.events` | Jira Adapter, Allow-list | reconcile events |
| **Jira Event Consumer** | `sync.consumer` | `@KafkaListener` on `jira.inbound.events`; UPSERTs `issue_projection` / `worklog_projection`; refreshes dependent materialized views | Jira domain repos | DB writes |
| **Jira Write Client** | `jira.write` | Executes assignee + comment calls synchronously; surfaces 403/workflow errors | Jira Adapter | `assignment.committed` / `assignment.failed` events |
| **Domain Services** | `workload`, `overdue`, `reporting`, `pipeline` | Pure business logic; no I/O other than injected repositories | Hub repos, materialized views | DTOs for UI |
| **Audit Service** | `audit` | Writes `audit_log` rows; also consumes `hub.audit` for post-hoc entries | audit repo | DB writes |
| **Materialized-View Refresher** | `materialized` | On relevant Kafka events, `REFRESH MATERIALIZED VIEW CONCURRENTLY` with debounce | PG | — |
| **Security Filter Chain** | `security` | Spring Security: session, CSRF, CORS, RBAC `@PreAuthorize` on every controller | — | — |
| **REST Controllers** | per capability package | Serve the single-page UI; apply RBAC; delegate to Domain Services | Domain Services | HTTP responses |
| **Single-Page UI** | `app/src/main/resources/static/` | Vanilla web app (framework choice deferred to Development stage; not an architecture driver) | REST controllers | — |

## 5. Control / Data Flow

### F-01 Member workload view (REQ-001)
```
Browser GET /api/workload/{memberId}?window=week
  → Security filter (RBAC: Manager / Admin, or Member = self)
  → WorkloadController
  → WorkloadService.loadWorkload(memberId, window)
      ├ reads capacity (3-tier) from CapacityRepo
      ├ reads active issues from issue_projection
      ├ applies B-RULE-02 to issues without estimate
      ├ applies B-RULE-03 + DEC-010 (Hard-Deadline criteria) → overload flag
      └ reads precomputed AR from materialized.allocation_rate
  → DTO → HTTP 200 JSON
```

### F-07 Assign / Re-assign (REQ-006)
```
Browser POST /api/assignments
  → Security filter (RBAC: Manager / Admin)
  → AssignmentController
  → PipelineService.assign(issueKey, newAssigneeId)
      ├ reads roster → resolves to jira_account_id
      ├ JiraWriteClient.setAssignee(issueKey, accountId)   (sync HTTP PUT)
      ├ JiraWriteClient.addInternalComment(issueKey, adfBody)  (sync HTTP POST)
      ├ on 403/workflow error → rollback optimistic UI state, raise error
      ├ writes AuditEntry (synchronous DB insert, flushed before response)
      └ publishes assignment.committed event to hub.audit (durable record)
  → DTO → HTTP 200 / 4xx
```

### F-08 Jira webhook ingestion (REQ-007)
```
Jira Cloud → POST https://<tunnel>/webhooks/jira
  → JiraWebhookController
      ├ verify HMAC-SHA256 (header `X-Hub-Signature`)
      ├ translate payload → canonical `JiraInboundEvent`
      └ publish to kafka topic `jira.inbound.events` (key = issueKey)
  → HTTP 2xx (fast, Jira does not retry)
     — then async —
  → JiraEventConsumer
      ├ UPSERT issue_projection / worklog_projection
      ├ compute affected materialized views → debounced refresh
      └ ack offset
```

### F-09 Hourly reconciler (REQ-007)
```
Scheduler (@Scheduled every hour) → ReconcilerCommand on `hub.sync.commands`
  → ReconcilerConsumer
      ├ for each allow-listed Project Key:
      │     JiraAdapter.search(JQL = "project = <key> AND updated > now()-24h")
      │     for each returned issue → publish to `jira.inbound.events`
      └ Record freshness_watermark per project
```

## 6. Error / Concurrency / Transaction Strategy

| Aspect | Strategy |
|---|---|
| Idempotency on inbound events | Idempotency key `(jira_event_id, event_type)`; `INSERT … ON CONFLICT DO NOTHING` on the dedup table before processing. |
| Issue projection UPSERT | `INSERT … ON CONFLICT (issue_key) DO UPDATE` with a monotonic `updated_at` guard: only overwrite when inbound event's `updated` field ≥ stored value. |
| Transactional boundaries | One DB transaction per inbound event; a batch of worklog deletes/creates for one webhook is a single transaction. |
| Materialized-view refresh | Done **outside** the main consumer transaction, debounced (max 1 refresh / 2 s per view). |
| Jira write-back | Not transactional end-to-end with DB; audit is written in a separate, best-effort transaction after the Jira response. On DB failure after a successful Jira write, the audit is still queued to `hub.audit`. |
| Concurrency between managers | Last-writer-wins on roster/capacity edits with an `ETag`/`If-Match` header (optional); conflict returns 409. For v1.0 prototype, lock-free is acceptable. |
| Error propagation | `GlobalExceptionHandler` → structured `ProblemDetails` RFC 7807 body. |
| Retries on Jira 5xx / 429 | Exponential backoff 3 attempts in reconciler; UI write-back does **not** retry (DEC-008 rollback semantics). |

## 7. Observability

| Signal | Where | Purpose |
|---|---|---|
| Micrometer metric `jira_webhook_received_total{type}` | webhook controller | Volume |
| `jira_webhook_signature_failed_total` | webhook controller | Security alarm |
| `jira_api_errors_total{status}` | Jira Adapter | Rate-limit / outage |
| `kafka_consumer_lag_seconds{topic}` | Micrometer-Kafka | Freshness SLO monitoring |
| `materialized_view_refresh_duration_seconds{view}` | refresher | Perf drift |
| `assignment_failed_total{reason}` | Jira Write Client | DEC-008 failure visibility |
| `scheduler_last_tick_timestamp` | reconciler | Scheduler health |
| Structured JSON logs | logback-spring.xml | Debug / audit trail |
| Audit events on `hub.audit` | Audit Service | Durable business trail |
| Health endpoint `/actuator/health` | Spring Boot Actuator | Dev-ops ping |

Tracing (Zipkin/Jaeger) deferred to post-prototype — not critical for dev-host deployment.

## 8. Configuration & Runtime Concerns

- **Spring Boot profiles**: `dev` (default on local), `prod` placeholder.
- **Environment variables** (`.env` file, perm 0600):
  - `JIRA_BASE_URL`, `JIRA_API_TOKEN`, `JIRA_USER_EMAIL`
  - `JIRA_WEBHOOK_SECRET`
  - `DB_URL`, `DB_USER`, `DB_PASS`
  - `KAFKA_BOOTSTRAP_SERVERS`
  - `HUB_PUBLIC_URL` (ngrok / Cloudflare Tunnel URL for webhooks)
  - `AUDIT_RETENTION_DAYS` (default 30)
  - `KAFKA_AUDIT_RETENTION_HOURS` (default 168)
- **Feature flags** (none needed for v1.0).
- **Clock**: `java.time.Clock` bean fixed to `ZoneId.of("Asia/Saigon")` globally — all date arithmetic goes through this.
- **Flyway**: `V001__initial.sql`, V002, … in `src/main/resources/db/migration/`.

## 9. Required Specialized Designs

| File | Applicability | Reason |
|---|---|---|
| `02_API_Contract_Design.md` | **Required** | REST surface for the UI + Jira webhook endpoint. |
| `03_Database_Design.md` | **Required** | Postgres schemas + materialized views + Flyway. |
| `04_Integration_Design.md` | **Required** | Jira REST (read + write) + Jira webhooks + Kafka topics. |
| `05_Security_Design.md` | **Required** | Spring Security + webhook HMAC + secret handling + financial-sector context. |

## 10. Technical Decisions (`TD-xxx`)

| ID | Decision | Rationale |
|---|---|---|
| TD-001 | **Java 21 + Spring Boot 3.3 LTS**, Gradle Kotlin DSL build. | Current LTS; aligns with user's existing Spring Boot stack. |
| TD-002 | **Flyway** for schema migrations. | Deterministic, SQL-first; works without CI/CD. |
| TD-003 | **Spring `RestClient`** (not `RestTemplate` legacy nor `WebClient` reactive) for Jira calls. | Project is non-reactive; RestClient is current recommendation. |
| TD-004 | **Spring Kafka** (`@KafkaListener`) for consumers; `KafkaTemplate` for producers. | Idiomatic. |
| TD-005 | **Docker Compose** for the dev environment: Postgres 16, Kafka (KRaft mode, no ZooKeeper), Hub app. | Simplest multi-container local setup. |
| TD-006 | **ngrok** (or Cloudflare Tunnel) for Jira webhook ingress to the dev machine. | Dev host has no public IP; Jira Cloud needs HTTPS public URL. |
| TD-007 | **Spring Security** form-login + session; CSRF enabled; CORS limited to the Hub's own origin. | Minimal viable auth for prototype. |
| TD-008 | **PostgreSQL materialized views** with `REFRESH CONCURRENTLY` for heatmap and overdue. | Fits REQ-009 perf; needs unique index for CONCURRENTLY. |
| TD-009 | **ADF JSON** emitted by a tiny in-house builder, not an SDK dependency. | Keeps dependencies lean; only one comment shape needed per DEC-008. |
| TD-010 | Flyway migrations are **forward-only**; rollback via a new migration, never by hand-editing. | Standard. |
| TD-011 | **Jackson** with `jsr310` module for ISO-8601 and `Instant` ↔ `OffsetDateTime`. | Standard. |
| TD-012 | **Local-first users**: the first local user bootstraps as Admin via a one-shot migration + env var `HUB_BOOTSTRAP_ADMIN_EMAIL`. | Dev-host prototype has no SSO. |

## 11. Risks / UNKNOWN / Decisions Required

| ID | Severity | Item | Owner / Resolution |
|---|---|---|---|
| TD-R-01 | MEDIUM | Jira Cloud rate limits on Free tier are not published precisely; reconciler may need throttling. | Flagged in Integration Design (04). |
| TD-R-02 | MEDIUM | Webhook ingress on a dev host via tunnel is fragile (tunnel URL changes if ngrok free tier). | Flagged in Integration Design + ops runbook. |
| TD-R-03 | LOW | Materialized-view `REFRESH CONCURRENTLY` requires a unique index on every view. | Addressed in DB Design. |
| TD-R-04 | LOW | Spring Boot form-login is weak for internet-facing deployments. On dev host behind tunnel, exposed. Mitigation: HTTP basic auth over tunnel + IP allow-list from Jira only on `/webhooks/jira`. | Security Design. |
| TD-R-05 | LOW | Vanilla / minimal frontend framework choice deferred to Development. | Development stage. |
| TD-R-06 | OPEN | Timezone of dev machine must be `Asia/Saigon` to match clock bean, or clock must override OS. The clock bean handles it; call out in runbook. | Ops runbook. |

## 12. Traceability Matrix

```
REQ-001 → AC-001.* → ADR-ARCH-002/009 → TD-001/008 → workload package + materialized.allocation_rate
REQ-002 → AC-002.* → ADR-ARCH-001/003/005 → TD-001/003/011 → reporting + jira adapter + consumer
REQ-003 → AC-003.* → ADR-ARCH-005/009 → TD-008 → overdue + materialized.member_overdue
REQ-004 → AC-004.* → ADR-ARCH-003 → TD-001 → workload.EtaService
REQ-005 → AC-005.* → ADR-ARCH-002 → TD-002 → pipeline package + Flyway schema
REQ-006 → AC-006.* → ADR-ARCH-004/006/010 → TD-003/009 → jira.write + audit + Kafka hub.audit
REQ-007 → AC-007.* → ADR-ARCH-001/004/005 → TD-004/005/006 → webhook controller + reconciler + Kafka topics + tunnel
REQ-008 → AC-008.* → ADR-ARCH-007 → TD-007/012 → security filter chain + roster bootstrap
REQ-009 → AC-009.1 → ADR-ARCH-002/009 → TD-008 → materialized views + Postgres index strategy
```

## 13. Status

```
TECHNICAL DESIGN DRAFTED WITH OPEN ITEMS
```

---
⛔ **STOP — stage boundary.** Next: `02_API_Contract_Design.md`.
*No human approval here; approval is at `06_Technical_Design_Review.md`.*
