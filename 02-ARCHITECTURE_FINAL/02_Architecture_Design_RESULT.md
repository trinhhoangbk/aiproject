# 02 — ARCHITECTURE DESIGN · Result

| Field | Value |
|---|---|
| Stage | 02-ARCHITECTURE_FINAL / 02 Architecture Design |
| Date | 2026-10-04 15:54 ICT |
| Role | Principal / Solution Architect (AI) — target design, no implementation |
| Preconditions | BUSINESS APPROVED (15:46) ✅ · CONTEXT APPROVED (15:54) ✅ |
| **Design status** | **🟡 ARCHITECTURE PROPOSED WITH OPEN ITEMS** |

---

## 1. Architecture Design Summary

A **modular monolith**, Spring Boot, with Kafka as the internal event bus and PostgreSQL as the single datastore (both Hub-owned data **and** a projection cache of Jira state). Jira Cloud is the authoritative Issue system; Hub ingests Jira events via webhooks (primary) and an hourly reconcile scheduler (backstop), serves a single-page UI, and performs synchronous assignee write-back to Jira with audit via Kafka.

The design is the smallest shape that satisfies the drivers for a 10–50-member system with p95 ≤ 2.5 s dashboard and p99 ≤ 5 min freshness. No microservices are introduced; no new storage technology beyond what CONTEXT APPROVED supplies.

## 2. Approved Baselines Used

- **REQ** REQ-001…REQ-009 (frozen at BUSINESS APPROVED).
- **AC** AC-001.1…AC-009.1 (03 Acceptance Criteria).
- **Business Rules** B-RULE-01…04 and BR-DEC-01…11 (02 Clarification).
- **Domain** 04 Domain Decomposition (glossary, invariants, SoR table).
- **Context** C-01…C-17 and 7 carried open items (`CONTEXT APPROVAL_RECORD.md`).

## 3. Current → Target Architecture

**Current:** no Hub exists. Jira (external) is the only running system.

**Target (text diagram):**
```
                 ┌────────────┐
                 │  Browser   │   Admin / Manager / Member
                 └─────▲──────┘
                       │ HTTPS (JWT session)
                       ▼
┌────────────────────────────────────────────────────┐
│                 Hub (Spring Boot)                   │
│                                                    │
│  ┌─────────────┐   ┌────────────────┐   ┌───────┐  │
│  │   Web UI    │   │   Hub REST     │   │ RBAC  │  │
│  │   module    │   │   (controllers)│   │Filter │  │
│  └──────┬──────┘   └──────┬─────────┘   └───┬───┘  │
│         │                 │                 │      │
│         ▼                 ▼                 │      │
│  ┌──────────────────────────────────────┐   │      │
│  │   Domain Services                    │◀──┘      │
│  │   workload / overload / overdue /    │          │
│  │   ETA / pipeline / balancing / audit │          │
│  └──────┬───────────────────────────────┘          │
│         │                                           │
│         ▼                                           │
│  ┌───────────────┐          ┌──────────────────┐    │
│  │ Hub Postgres  │          │  Kafka Producer   │   │
│  │ repositories  │          │ /Consumer wiring  │   │
│  └──────┬────────┘          └──────▲────────▲───┘   │
│         │                          │        │       │
│         ▼                          │        │       │
│    (PostgreSQL)                    │        │       │
│                                    │        │       │
│  ┌────────────────────┐            │        │       │
│  │ Jira Webhook       │────publish─┘        │       │
│  │ Receiver (HTTP)    │                     │       │
│  └────────────────────┘                     │       │
│                                             │       │
│  ┌────────────────────┐    ┌────────────────┴───┐   │
│  │ Jira Reconciler    │    │ Jira Event        │   │
│  │ (@Scheduled 1h)    │    │ Consumer          │   │
│  └─────────┬──────────┘    └─────────┬─────────┘   │
│            │                         │             │
│            └──── Jira Write Client ──┘             │
│                       │                            │
└───────────────────────┼────────────────────────────┘
                        ▼
                 ┌────────────┐
                 │ Jira Cloud │   REST API v3 + Webhooks
                 └────────────┘
```
Everything in the box is one deployable unit (modular monolith). Kafka and PostgreSQL are separate infrastructure components.

## 4. Target Components & Responsibilities

| Component | Responsibility | Justification |
|---|---|---|
| **Web UI** | Single-page web application serving Admin/Manager/Member dashboards; renders in Vietnamese; UI framework left to Technical Design (not an architecture driver here). | REQ-001…006, REQ-008, DEC-016. |
| **Hub REST API (Spring Boot controllers)** | Public API for the UI; enforces RBAC (DEC-004, REQ-008); orchestrates reads and writes. | REQ-008 scope separation. |
| **Domain Services** | Pure business logic — Workload, Overload, Overdue, Early-Warning, ETA, Pipeline, Balancing, Audit. Deterministic functions over cached state. | 04 Domain §B capabilities. |
| **Hub Repositories** | Persistence of Hub-owned data in PostgreSQL (see §7). | DEC-004, DEC-005, DEC-006, DEC-007, REQ-005, DEC-008, DEC-010. |
| **Jira Webhook Receiver** | HTTPS endpoint, HMAC/secret verifies the Jira Cloud webhook, publishes a canonical event to Kafka. Returns 2xx fast so Jira does not retry on backpressure. | REQ-007 freshness; AC-007.1. |
| **Jira Event Consumer** | Kafka consumer that applies canonical events to the issue projection in Postgres and recomputes affected materialized views. | REQ-007; AC-003.3 freshness SLO. |
| **Jira Reconciler** (`@Scheduled` hourly) | Polls Jira REST v3 for every allow-listed project and reconciles divergence; writes "reconcile events" into Kafka. | REQ-007 polling; AC-007.2/3. |
| **Jira Write Client** | Calls `PUT /rest/api/3/issue/{idOrKey}/assignee` + `POST .../comment` (ADF body); surfaces 403/workflow-block to the caller; publishes an `assignment.committed` or `assignment.failed` audit event to Kafka. | DEC-008; AC-006.4/5/6. |
| **Audit Service** | Writes audit entries to the `audit_log` table (append-only); consumes `hub.audit` Kafka topic for post-hoc events. | DEC-008, DEC-013 (retention OPEN). |
| **Scheduler** | Spring `@Scheduled`: hourly reconcile, nightly materialized-view full rebuild, 5-min heartbeat metric. | REQ-007, REQ-009. |
| **PostgreSQL** | Single datastore for all Hub-owned data **and** issue projection cache. One database, logical schemas per bounded concern. | CONTEXT (C-16). |
| **Apache Kafka** | Internal event bus. Topics: `jira.inbound.events`, `hub.domain.events`, `hub.audit`, `hub.sync.commands`. | CONTEXT (C-16); REQ-007. |

No component is introduced without a REQ or decision anchor.

## 5. Boundary Model

| Boundary | Type | Enforcement |
|---|---|---|
| Jira ↔ Hub | External trust boundary | HMAC-verified webhooks; outbound auth per `O-03` (OPEN); TLS everywhere. |
| Browser ↔ Hub API | External trust boundary | JWT session; RBAC (DEC-004); CORS allow-list. |
| UI ↔ Domain Services | Internal module boundary | DTOs; no direct repository access from controllers (`layered`). |
| Jira Reconciler ↔ Jira Write Client | Internal module boundary | Shared Jira HTTP client with rate-limit guard. |
| `audit_log` | Internal data boundary | Append-only; no `UPDATE`/`DELETE` paths; only Audit Service writes. |
| Allow-listed Jira Projects ↔ Everything else | Business boundary | Scope filter in Reconciler and Consumer (DEC-005). |
| Hub Roster ↔ Jira accounts | Business boundary | DEC-006; non-roster accounts skipped in team-MD aggregates. |

## 6. Interaction Model

### Synchronous
| Flow | Caller → Callee | Why sync |
|---|---|---|
| UI reads (dashboard, overdue, heatmap, pipeline) | Browser → Hub API → Postgres | p95 ≤ 2.5 s requires direct read from precomputed state. |
| Assign/Re-assign write-back | UI → Hub API → Jira Write Client → Jira | User must see success/failure immediately; rollback requires sync (DEC-008). |

### Asynchronous (via Kafka)
| Flow | Producer | Consumer | Reason |
|---|---|---|---|
| Jira webhook ingestion | Jira Webhook Receiver | Jira Event Consumer | Decouple Jira retry pressure from Hub DB. |
| Hourly reconcile | Scheduler | Jira Reconciler | Backstop against lost webhooks. |
| Audit emission | Jira Write Client, Domain Services | Audit Service | Durability without blocking the user. |
| Materialized-view refresh triggers | Jira Event Consumer | Internal refresh worker | Keep heatmap and overdue fresh with p99 ≤ 5 min. |

### Failure behaviour on each path
- Jira webhook → Kafka: on Kafka outage, Receiver returns 503; Jira Cloud retries the webhook. Hourly reconciler is still the safety net.
- Hub API write-back: on Jira 403 or workflow block, UI rolls back optimistic state, audit records the failure (AC-006.5).
- Reconciler → Jira: on Jira 5xx or rate-limit, exponential backoff; next scheduled tick re-attempts.
- Consumer poison messages: dead-letter topic `hub.dlq`; monitored.

## 7. Data Ownership Model

### PostgreSQL — logical schemas
| Schema | Owned by | Writers | Readers |
|---|---|---|---|
| `roster` | Hub (DEC-006) | Manager via Hub API | Domain Services |
| `capacity` (3 tiers Global/Team/Member) | Hub (DEC-004) | Admin / Manager | Domain Services |
| `holiday_calendar` | Hub (DEC-003) | Admin / Operations Lead | Domain Services |
| `allow_list` | Hub (DEC-005) | Manager | Reconciler, Consumer, Domain Services |
| `skill_taxonomy` + `member_skills` | Hub (DEC-007) | Manager | Domain Services |
| `pipeline_project` | Hub (REQ-005) | Manager | Domain Services |
| `lock_deadline_flag` | Hub (DEC-010.3) | Manager | Domain Services |
| `audit_log` | Hub (DEC-008); retention = **O-09 OPEN** | Audit Service only | Admin / Manager (read-only) |
| `issue_projection` | Jira is **SoR**; Hub keeps a projection cache | Jira Event Consumer, Reconciler | Domain Services, UI |
| `worklog_projection` | Jira is **SoR**; projection cache | Jira Event Consumer, Reconciler | Domain Services |
| `materialized.allocation_rate` | Hub (derived) | Refresh worker | UI |
| `materialized.member_overdue` | Hub (derived) | Refresh worker | UI |

Consistency:
- Hub-owned schemas: strong consistency via Postgres transactions.
- Projection caches: eventually consistent, bounded by freshness SLO (C-02).
- Audit: append-only, never retroactively modified.

## 8. Quality Attribute Design

| Attribute | Target | Approach |
|---|---|---|
| **Performance** (REQ-009, p95 ≤ 2.5 s) | Team dashboard ≤ 2.5 s | Pre-aggregate `allocation_rate` and `member_overdue` as materialized views refreshed on event; UI reads only from these. Expected row counts: 50 members × 4 windows = 200 rows per view. |
| **Freshness** (AC-003.3, AC-007.1 p99 ≤ 5 min) | Webhook → UI reflects change | Webhook receiver → Kafka → consumer path targets sub-10-second median; 5-min SLO leaves 30× margin. Reconcile backstop at 60-min cadence. |
| **Availability** | Business-hours critical; after-hours best-effort | Deployment-dependent (O-04 OPEN). Degraded-mode read from projection cache even if Jira is down. |
| **Reliability** | No double assignment, no lost events | Idempotency key on each Kafka event (`jira_event_id`); consumer uses `UPSERT`. Dead-letter topic for poison messages. |
| **Scalability** | 10–50 members, thousands of issues, modest manager traffic | Monolith on a single JVM sized to that scale is sufficient; Postgres single primary. |
| **Security** | Baseline + MBS financial-sector sensibility (O-10 OPEN) | Spring Security; HTTPS everywhere; HMAC on webhook; secrets via externalised backend (choice OPEN); audit on every mutation. |
| **Observability** | Enough to debug a lag or a failed assignment | Micrometer metrics: `jira_webhook_total`, `jira_webhook_signature_failed_total`, `kafka_consumer_lag_seconds`, `jira_api_errors_total`, `assignment_failed_total`. Structured JSON logs. |
| **Operability** | Manual releases; must stay simple | Single artifact; one DB migration tool (Flyway or Liquibase — Tech Design). Blue/green not required at v1.0 scale. |
| **Maintainability** | Modular monolith; swap Jira edition later if needed | Jira adapter is one package behind an interface; could be rewritten for Jira DC without touching Domain Services. |
| **Recoverability** | Rebuild projection from Jira if DB corrupt | Reconciler can perform full rebuild from Jira state (bounded by Jira history visibility). |

## 9. Architecture Decisions (ADR-ARCH-xxx)

### ADR-ARCH-001 — Integrate Jira Cloud via REST API v3
- **Context:** CONTEXT APPROVED set Jira Cloud + API v3; DEC-008 requires an Internal Comment, which on v3 is ADF JSON.
- **Decision:** Use Jira Cloud REST API v3 for all reads and writes. Use ADF for comment bodies.
- **Alternatives:** v2 (legacy); Jira DC.
- **Rationale:** Current recommended version; aligned with Cloud; ADF supported.
- **Trade-offs:** ADF JSON structure is more verbose than v2 plain-text comments.
- **Consequences:** Jira adapter package handles ADF; one place to change if Jira revises.
- **Traceability:** C-13, C-14; DEC-008.
- **Open:** None.

### ADR-ARCH-002 — Modular monolith (not microservices)
- **Context:** 10–50 members; manager traffic modest; small team; no CI/CD.
- **Decision:** One Spring Boot application; internal modules by capability (REQ-001…008).
- **Alternatives:** Microservices per capability; event-sourcing-heavy design.
- **Rationale:** Playbook rule — smallest architecture change; no driver demands distribution.
- **Trade-offs:** Horizontal scale is coarse (whole app replicates); acceptable at scale.
- **Consequences:** Single deployment unit, simpler release (fits C-17).
- **Traceability:** REQ-009 (perf) achievable without distribution; C-17.
- **Open:** None.

### ADR-ARCH-003 — PostgreSQL as single datastore, including Jira projection cache
- **Context:** CONTEXT fixes Postgres; separate cache store (e.g. Redis) was not approved.
- **Decision:** One Postgres database, logical schemas; derived views as PG materialized views.
- **Alternatives:** Add Redis for read cache; add OpenSearch for issue search.
- **Rationale:** 50 members × thousands of issues fits comfortably; adding stores adds operational cost without evidence of need.
- **Trade-offs:** Full-text search in Postgres (if needed) is weaker than OpenSearch; not currently required.
- **Consequences:** One backup, one monitor, one connection pool.
- **Traceability:** C-16; REQ-009.
- **Open:** Row-count budgeting is Technical Design.

### ADR-ARCH-004 — Kafka as internal event bus
- **Context:** CONTEXT fixes Kafka; need to decouple Jira webhook ingestion from DB write.
- **Decision:** Four topics — `jira.inbound.events`, `hub.domain.events`, `hub.audit`, `hub.sync.commands` — single-partition initially (10–50 users), expand only if lag observed.
- **Alternatives:** In-process queue (loses durability); Postgres LISTEN/NOTIFY (loses buffering).
- **Rationale:** Already in CONTEXT; gives durability + backpressure.
- **Trade-offs:** Operational cost of a cluster for one app; mitigated by shared infra at MBS (assumption).
- **Consequences:** Clear async paths; audit durable even on consumer restart.
- **Traceability:** C-16; AC-007.1, AC-007.3.
- **Open:** Partition count — defer to Technical Design.

### ADR-ARCH-005 — Jira sync strategy: webhook primary + hourly reconcile
- **Context:** AC-007.1 freshness SLO; AC-007.2/3 reconciliation; webhooks are at-most-once in practice.
- **Decision:** Webhook is the fast path; `@Scheduled` reconciler is the safety net; both feed Kafka.
- **Alternatives:** Polling-only (would miss the 5-min SLO at any realistic cadence); webhook-only (loses events on outage).
- **Rationale:** Playbook BRD §7 explicitly mandates both.
- **Trade-offs:** Occasional duplicate events — handled via idempotency key.
- **Consequences:** Robust against transient failures of either path.
- **Traceability:** REQ-007; AC-007.1/2/3.
- **Open:** None.

### ADR-ARCH-006 — Synchronous write-back for assignee; audit via Kafka
- **Context:** DEC-008 requires immediate UI feedback and rollback on failure; audit must be durable.
- **Decision:** `PUT /issue/{id}/assignee` and `POST .../comment` executed inline on the request thread; UI result driven by the Jira response; audit event published to `hub.audit` for durable append.
- **Alternatives:** Async queue the write (loses user-feedback requirement).
- **Rationale:** DEC-008 explicitly demands rollback on 403/workflow-block.
- **Trade-offs:** Jira latency becomes user-facing (typically < 500 ms on Cloud).
- **Consequences:** Audit guaranteed even if the UI session dies.
- **Traceability:** DEC-008; AC-006.4/5/6.
- **Open:** None.

### ADR-ARCH-007 — RBAC enforced centrally; Member sees self-only
- **Context:** DEC-004, REQ-008, DEC-014.
- **Decision:** Spring Security method annotations (`@PreAuthorize`) on every controller + service method; data-level filter wraps every query that could leak peers.
- **Alternatives:** UI-only RBAC (unsafe).
- **Rationale:** Defence in depth.
- **Trade-offs:** Verbosity on controllers.
- **Consequences:** Audit of denied requests is possible.
- **Traceability:** REQ-008; DEC-004; DEC-014.
- **Open:** DEC-012 edit granularity for pipeline accepted at BUSINESS APPROVED.

### ADR-ARCH-008 — No CI/CD for v1.0; explicit manual release gate
- **Context:** C-17 fixes no CI/CD.
- **Decision:** Release = tag + manual build + manual deploy + change-control record; migrations run via `flyway migrate` as a release-time step.
- **Alternatives:** CI/CD (not approved).
- **Rationale:** User decision.
- **Trade-offs:** Higher risk of human error; mitigated by small release cadence and migration checklists.
- **Consequences:** Must document a RELEASE.md for operators.
- **Traceability:** C-17.
- **Open:** None in architecture; operational.

### ADR-ARCH-009 — Precomputed materialized views for heatmap and overdue
- **Context:** REQ-009 p95 ≤ 2.5 s; event-driven refresh fits Kafka consumer pattern.
- **Decision:** Define `materialized.allocation_rate(member, window)` and `materialized.member_overdue(member)` refreshed by the consumer on relevant events + nightly full-rebuild.
- **Alternatives:** Compute on each request (likely misses SLO at 50 members); keep in Redis (not approved).
- **Rationale:** Fastest in-process path.
- **Trade-offs:** Writes to Postgres from the consumer path; mitigated by batching.
- **Consequences:** Technical Design must specify refresh triggers precisely.
- **Traceability:** REQ-009; AC-009.1; AC-006.2.
- **Open:** None.

### ADR-ARCH-010 — Jira authentication model — **PROPOSED, pending O-03**
- **Context:** O-03 remains OPEN at CONTEXT APPROVED.
- **Decision (proposed):** Service-account via Atlassian **API Token + Basic auth** for reads/reconcile and the write-back path. Audit attribution on the Jira side is a static "resource-balancing-hub-bot"; the Hub-side audit attributes the Manager by name (DEC-008 wording).
- **Alternatives:** OAuth 2.0 3LO (user-impersonated — richer attribution but needs user consent and refresh-token storage); OAuth 2.0 2LO via Connect/Forge (requires marketplace app).
- **Rationale:** API Token is simplest, matches the service-account model (DEC-006 excludes bot/service accounts from team-MD so no risk of self-counting), and keeps credentials under Hub control.
- **Trade-offs:** Jira audit shows "bot"; the Hub audit must carry the real actor.
- **Consequences:** Secret storage design still needed (O-10).
- **Traceability:** DEC-008; O-03.
- **Open:** **O-03** — human decision required to confirm or override at ARCHITECTURE APPROVED.

### ADR-ARCH-011 — Deployment target — **DEFERRED, O-04**
- **Context:** O-04 OPEN; MBS financial-securities context suggests on-prem/private cloud.
- **Decision (deferred):** Target is a container-based deployment (Docker image) runnable on either on-prem Kubernetes or VM; final hosting is a human decision.
- **Alternatives:** Public cloud SaaS (not viable without data-residency confirmation).
- **Traceability:** O-04; O-08 (HA/RTO/RPO depends).
- **Open:** **O-04**.

## 10. Alternatives & Trade-offs (summary)

| Choice | Picked | Rejected | Why rejected |
|---|---|---|---|
| Deployment shape | Monolith | Microservices | 10–50 users, playbook rule "smallest shape". |
| Datastore | Single Postgres | Postgres + Redis + OpenSearch | No evidence of need; cost. |
| Sync strategy | Webhook + hourly reconcile | Polling-only | Misses 5-min SLO. |
| Write-back | Synchronous | Async-queued | DEC-008 rollback. |
| Auth (Jira) | API-token service account (proposed) | OAuth 3LO; OAuth 2LO | Simpler; final human decision pending. |
| Caching | PG materialized views | In-memory LRU | Durable; survives restart; sized for 50 members. |

## 11. Failure Modes

| Scenario | Business impact | Architecture-level mitigation |
|---|---|---|
| Jira Cloud outage (read) | Dashboard shows last known state; staleness grows | Projection cache serves reads; freshness badge in UI shows last-successful sync time. |
| Jira Cloud outage (write) | Assign/Re-assign fails | Return specific error to user; audit the failed attempt; no Hub-side rollback needed because Jira state is unchanged. |
| Jira Cloud rate limit | Reconciler backs off | Exponential backoff; alert if rate-limited > N min. |
| Kafka outage | Webhook receiver 503 → Jira retries; audit backlog in-memory of writer | Short self-buffer in writer with max N entries then drop-oldest; alarms. |
| Postgres outage | Hub serves nothing | 503; UI shows maintenance banner. |
| Duplicate webhook delivery | None at business level | Idempotency key = `jira_event_id`; UPSERT. |
| Dropped webhook | Freshness lag until next reconcile | Hourly reconcile closes the gap. |
| Clock skew between TZs | Day-boundary mis-classification | Fixed TZ `Asia/Saigon` at the service layer; always derive calendar day from the server clock (DEC-002). |
| Poison event | Consumer stuck | Dead-letter topic `hub.dlq`; metric + alert. |
| Secret leak | Jira write under hub bot | Rotate immediately; audit shows all actions. |

## 12. Migration / Compatibility

- Greenfield; no backward-compatibility obligation.
- Database migrations managed with Flyway (details → Technical Design).
- If MBS later migrates to Jira Data Center, only the Jira adapter package changes; Domain Services and schemas are untouched (ADR-ARCH-001 modularity).

## 13. Risks / Open Items

| ID | Risk | Owner | Severity |
|---|---|---|---|
| R-ARCH-01 | **O-03 Jira auth** final choice may change `jira-write-client` and secret storage. | Approver at Architecture gate | High |
| R-ARCH-02 | **O-04 deployment** choice affects availability design, networking, secret backend. | Approver | High |
| R-ARCH-03 | **O-09 audit retention** affects storage sizing + archival. | Approver | Medium |
| R-ARCH-04 | **O-10 secrets backend** not chosen. ADR-ARCH-010 depends on it. | Approver | Medium |
| R-ARCH-05 | **O-07 concurrent viewers** could push p95 if far above 50. | Approver | Low |
| R-ARCH-06 | **O-08 HA/RTO/RPO** not specified. | Approver | Low (impacts Architecture only when it becomes high). |
| R-ARCH-07 | **O-11 non-roster worklog** — architecture excludes from team-MD aggregates but still projects for traceability. May need to re-visit if business decides otherwise. | Approver | Low |
| R-ARCH-08 | No CI/CD = human-release risk. | Operations | Accepted (C-17) |
| R-ARCH-09 | ADF JSON for comment bodies is verbose; adapter must be tested. | Technical Design | Low |

## 14. Traceability Matrix

```
REQ-001 ─► ADR-ARCH-002, ADR-ARCH-003, ADR-ARCH-009 ─► Hub REST + Domain Services + materialized.allocation_rate
REQ-002 ─► ADR-ARCH-001, ADR-ARCH-003, ADR-ARCH-005 ─► Jira Event Consumer + issue_projection + worklog_projection
REQ-003 ─► ADR-ARCH-005, ADR-ARCH-009 ─► materialized.member_overdue + Early-Warning service (TPR)
REQ-004 ─► ADR-ARCH-003 ─► Domain Services (ETA; α over last 10 WD)
REQ-005 ─► ADR-ARCH-002, ADR-ARCH-003 ─► Hub REST + pipeline_project schema + skill_taxonomy
REQ-006 ─► ADR-ARCH-004, ADR-ARCH-006, ADR-ARCH-009, ADR-ARCH-010 ─► Jira Write Client + audit_log + materialized.allocation_rate
REQ-007 ─► ADR-ARCH-001, ADR-ARCH-004, ADR-ARCH-005 ─► Webhook Receiver + Reconciler + Kafka
REQ-008 ─► ADR-ARCH-007 ─► Spring Security + data-level filter
REQ-009 ─► ADR-ARCH-002, ADR-ARCH-003, ADR-ARCH-009 ─► materialized views + PG indices
NFR SLO freshness (C-02) ─► ADR-ARCH-005
TZ (DEC-002) ─► Domain Services use fixed `Asia/Saigon` clock
```

## 15. Design Status

```
ARCHITECTURE PROPOSED WITH OPEN ITEMS
```

Open items are the 7 CONTEXT conditions (O-03, O-04, O-07, O-08, O-09, O-10, O-11) and ADR-ARCH-010/011 which depend on O-03/O-04.

---
⛔ **STOP — stage boundary.** Next authorized file: `03_Architecture_Review.md`.
*Hard rule: AI must not approve its own design.*
