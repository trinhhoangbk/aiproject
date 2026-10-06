# 04 — INTEGRATION DESIGN · Result

| Field | Value |
|---|---|
| Stage | 03-TECHNICAL-DESIGN / 04 Integration Design |
| Date | 2026-10-04 16:08 ICT |
| Role | Integration Architect (AI) |
| **Status** | **🟡 INTEGRATION DESIGN COMPLETE WITH OPEN ITEMS** |

---

## 1. Integration Scope

Four integration surfaces:
- **I-01 Jira Cloud REST (read)** — Hub → Jira, pull.
- **I-02 Jira Cloud REST (write)** — Hub → Jira, push (assignee + comment).
- **I-03 Jira Cloud Webhook** — Jira → Hub, push.
- **I-04 Kafka internal event bus** — Hub ↔ Hub (producer ↔ consumer).

## 2. Current Integration Evidence
None in repo (greenfield). Authoritative references:
- Jira Cloud REST API v3 (per CONTEXT APPROVED, C-14).
- Atlassian Jira Cloud Webhooks (per REQ-007).
- Kafka single-broker on the dev host (TD-005).

## 3. Integration Matrix

| ID | Classification | Direction | Producer / Caller | Consumer / Callee | Protocol | Notes |
|---|---|---|---|---|---|---|
| I-01 | Sync REST | out | Hub Reconciler + Jira Adapter | Jira Cloud | HTTPS, JSON | Pagination; rate-limited by Jira Cloud. |
| I-02 | Sync REST | out | Hub Jira Write Client | Jira Cloud | HTTPS, JSON | Must be synchronous (DEC-008). |
| I-03 | Webhook | in | Jira Cloud | Hub Webhook Controller | HTTPS, JSON | HMAC-SHA256 verified. |
| I-04 | Async event | internal | Various producers | `@KafkaListener`s | Kafka protocol | 4 topics (listed below). |

Kafka topics:
- `jira.inbound.events` — key=`issueKey`; produced by webhook controller + reconciler; consumed by `JiraEventConsumer`.
- `hub.domain.events` — internal domain events (e.g. `AssignmentCommitted`); produced by services, consumed by refresher.
- `hub.audit` — audit events; produced by write path, consumed by audit service.
- `hub.sync.commands` — scheduler commands (`ReconcileRequested`); produced by scheduler, consumed by reconciler.

## 4. Interaction Sequences

### 4.1 Jira read (reconciler)
```
Scheduler ─(every 1h)─► Kafka hub.sync.commands { ReconcileRequested }
JiraReconcilerConsumer:
   for each allow_list.project_key:
      page := 0
      while next_page:
         GET https://<site>.atlassian.net/rest/api/3/search
           ?jql=project = <key> AND updated > -1h
           &fields=status,resolution,assignee,priority,labels,fixVersions,duedate,timeoriginalestimate,timeremainingestimate,worklog
           &startAt=<page>&maxResults=100
           Authorization: Basic base64(email:api_token)
         for each issue in response:
             publish to Kafka jira.inbound.events (key=issueKey)
         handle 429/5xx with exponential backoff (3 attempts; next hour re-tries)
```

### 4.2 Jira webhook (ingest)
```
Jira Cloud → POST https://<ngrok>/webhooks/jira
    Headers: X-Hub-Signature: sha256=<hmac>, Content-Type: application/json
    Body: Jira webhook payload

Hub Webhook Controller:
  verify HMAC-SHA256 using JIRA_WEBHOOK_SECRET → 401 if mismatch
  parse → JiraInboundEvent canonical form
  dedup key = (webhookDeliveryId, eventType) — Jira includes a delivery id
  Kafka produce(jira.inbound.events, key=issueKey, value=canonicalEvent)
  return 200 OK

JiraEventConsumer:
  BEGIN tx
    INSERT INTO jira.jira_event_dedup (jira_event_id, event_type) ON CONFLICT DO NOTHING
    IF insert affected 0 rows → COMMIT and ack (duplicate)
    UPSERT jira.issue_projection with monotonic guard
    UPSERT/DELETE jira.worklog_projection as applicable
  COMMIT
  Trigger debounced materialized-view refresh
  ack Kafka offset
```

### 4.3 Jira write (assignee)
```
Hub Jira Write Client:
  PUT https://<site>.atlassian.net/rest/api/3/issue/<key>/assignee
    Authorization: Basic base64(email:api_token)
    Body: { "accountId": "<jira_account_id>" }
  cases:
    204 No Content → success
    403 → map to ProblemDetails jira-write-blocked (DEC-008), do NOT retry
    400 (bad accountId) → ProblemDetails invalid-assignee
    404 → ProblemDetails issue-not-found
    429 → single short retry after Retry-After header; else surface
    5xx → ProblemDetails jira-unavailable, no auto-retry

  on 204:
    POST https://<site>.atlassian.net/rest/api/3/issue/<key>/comment
      Body: ADF JSON (TD-009) with:
        "Task reassigned via Resource Balancing Hub by [Manager Name]"
    outcome recorded in audit regardless of comment success
  always: AssignmentAttempted event → Kafka hub.audit
```

### 4.4 Hourly reconcile vs. webhook interleave
```
If webhook arrives for an issue that the reconciler is about to update:
  Both end up as events on jira.inbound.events, with monotonic jira_updated_at.
  UPSERT guard ignores the older of the two.
```

## 5. Contract References

- Jira Cloud REST v3 operations used: `GET /rest/api/3/search`, `GET /rest/api/3/issue/{idOrKey}`, `PUT /rest/api/3/issue/{idOrKey}/assignee`, `POST /rest/api/3/issue/{idOrKey}/comment` (ADF body).
- Jira Webhook event names subscribed: `jira:issue_updated`, `jira:worklog_created`, `jira:worklog_updated`, `jira:worklog_deleted`.
- Internal Kafka envelope per event (JSON): `{ "schemaVersion": 1, "eventType": "...", "producedAt": "<rfc3339>", "idempotencyKey": "...", "payload": { ... } }`.
- `payload` for inbound events = canonical trimmed Jira fields (listed in §4.1 request).

## 6. Delivery / Ordering Semantics

| Flow | Semantics | Ordering |
|---|---|---|
| I-01 Reconciler → Kafka | At-least-once production (Kafka default acks=all for v1 is overkill with single broker; acks=1 acceptable). | Per issue, publication order follows Jira pagination. |
| I-03 Webhook → Kafka | At-least-once (Jira may re-deliver on timeouts; we respond 2xx fast). | Not guaranteed; UPSERT guard tolerates out-of-order via `jira_updated_at`. |
| I-04 Kafka → Consumer | At-least-once (Kafka default). | Per topic+partition ordered. Single partition for v1 (TD-004). |
| I-02 Jira write | Synchronous RPC. | — |

**No exactly-once assumption anywhere.** Idempotency is engineered.

## 7. Timeout / Retry / Idempotency

| Call | Timeout | Retry policy | Idempotency key |
|---|---|---|---|
| Jira `GET /search` | 20 s connect+read | Exp. backoff: 2 s, 8 s, 32 s; max 3 attempts. | None needed (read). |
| Jira `GET /issue/{k}` | 10 s | 3 attempts, same backoff. | — |
| Jira `PUT /assignee` | 10 s | **No automatic retry** (DEC-008 rollback semantics). | n/a |
| Jira `POST /comment` | 10 s | 1 retry on 5xx (comment is best-effort, log on fail). | n/a |
| Kafka produce | 5 s | `retries=3`, `delivery.timeout.ms=10000`. | Producer-side `enable.idempotence=true` for exactly-once-into-broker semantics. |
| Kafka consume | — | `max.poll.interval.ms=300000`; failed message → `hub.dlq.<topic>`. | `(jira_event_id, event_type)` dedup table (see 03 DB). |
| Webhook accept | — | Jira retries if we don't 2xx within 30 s; we respond fast (< 1 s). | Jira's webhook delivery id. |

## 8. Failure / Recovery / Compensation

| Failure | Visible impact | Recovery |
|---|---|---|
| ngrok / tunnel down | Jira cannot reach the webhook | Hourly reconciler closes the gap when tunnel is restored. UI shows "freshness: last sync HH:MM". |
| Jira read 429 (rate limit) | Reconciler slows | Honour `Retry-After`; next hour's tick resumes. |
| Jira write 403 (workflow) | Assign fails, user sees specific error | UI rolls back optimistic change; audit records failed attempt. **No compensation action**; user retries after moving the Jira status. |
| Jira write success + comment failure | Jira assignee is updated but comment missing | Audit flags `comment_failed=true`; retry on next event or during a rare manual repair. |
| Kafka broker down | Webhook controller returns 503; Jira retries; reconciler cannot publish → next hour | Alert operator; restart broker. |
| Postgres down | Everything 503 | Restart; projection rebuilds via next reconcile. |
| Consumer poison message | Message routed to `hub.dlq.<topic>` after 3 retries | Operator inspects DLQ; corrects data; replays via admin console. |
| Reconciler stuck (long GC, OOM) | Lag on `hub.sync.commands` | Scheduler emits every hour anyway; consumer picks up when back; `scheduler_last_tick_timestamp` alarms if idle > 90 min. |

## 9. Compatibility / Versioning

- Hub tolerates **unknown fields** on Jira responses (`FAIL_ON_UNKNOWN_PROPERTIES=false`).
- When Atlassian adds a required new field, Hub keeps working with cached data until mapped.
- Jira API version pinned to v3; if Atlassian deprecates v3, re-run Technical Design.
- Kafka internal envelope has `schemaVersion`; bumping is a code change plus a one-off reader-upgrade step.
- Webhook payload format changes by Atlassian are handled by schema-tolerant parsing + reconciler fallback.

## 10. Observability / Audit

See 01 Technical Design §7 for metric list. Additions here:

- Every Jira call logs: `jira_api_call{op, status, duration_ms}`.
- Every assignment attempt emits an audit event with the full Jira response code and the Hub's rollback decision.
- Correlation id = `X-Request-Id` header; propagated into Jira call logs and audit payload for traceability.
- DLQ depth metric: `kafka_dlq_depth{topic}`.
- A per-project `freshness_watermark` written on each successful reconcile tick; surfaced in UI header.

## 11. Verification Scenarios

| ID | Scenario | Expected |
|---|---|---|
| IV-01 | Webhook with wrong HMAC | 401; no Kafka publish; `jira_webhook_signature_failed_total` increments. |
| IV-02 | Same webhook delivered twice | Second processed → dedup drops it; `jira_event_dedup_hits_total` increments. |
| IV-03 | Jira `PUT /assignee` returns 403 | HTTP 422 to UI; audit entry `result=FAILED`, `jira_status=403`; no Hub-side state change. |
| IV-04 | Jira `GET /search` returns 429 with `Retry-After: 60` | Reconciler sleeps 60 s; completes; metric records single retry. |
| IV-05 | Tunnel down for 30 min | Webhook events miss; next hourly reconcile picks up the missed changes; `freshness_watermark` jumps forward. |
| IV-06 | Issue has `fixVersion.releaseDate = 2026-10-10` | After ingest, overload calc flags this issue as `cannot_reschedule=true` (DEC-010 criterion 1). |
| IV-07 | Issue priority = Blocker, no fix version | Still flagged `cannot_reschedule=true` (DEC-010 criterion 2). |
| IV-08 | Manager toggles Lock Deadline in Hub for an issue | `core.lock_deadline_flag` row created; overload calc picks it up on next request (DEC-010 criterion 3). |
| IV-09 | Worklog from a non-roster Jira user | Persisted into `jira.worklog_projection` with `in_roster=false`; excluded from team-MD per O-11 decision. |

## 12. Risks / Open Items

| ID | Severity | Item |
|---|---|---|
| INT-R-01 | HIGH | **Tunnel reliability** (TD-R-02). ngrok free-tier URLs change on reconnect; Jira webhook URL would need re-registration. Mitigations: paid tunnel with stable domain, or Cloudflare Tunnel; or move Hub to a host with a public IP later. |
| INT-R-02 | MEDIUM | Jira Cloud Free rate limits are unofficial; must observe at runtime. If `jira_api_errors_total{status=429}` becomes material, tune reconciler cadence or scope. |
| INT-R-03 | MEDIUM | Jira "worklog_deleted" webhook does not always include issue key context on all editions; verify payload shape in dev. |
| INT-R-04 | LOW | Comment ADF JSON on v3 is more verbose than v2 plain-text; adapter is isolated. |
| INT-R-05 | LOW | Jira API returns remaining estimate in seconds; conversion done at adapter boundary. |
| INT-O-01 | OPEN | Choice of tunnel vendor (ngrok vs Cloudflare Tunnel) — operator preference. |
| INT-O-02 | OPEN | Final list of subscribed webhook event names — confirm during Development after creating the webhook in Jira. |

## 13. Traceability

| REQ / AC | Integration |
|---|---|
| REQ-007 / AC-007.1 | I-03 Webhook |
| REQ-007 / AC-007.2 | I-01 Reconciler |
| REQ-007 / AC-007.3 | I-01 + I-03 interplay |
| REQ-006 / AC-006.4/5 | I-02 Write + rollback semantics |
| REQ-006 / AC-006.6 | I-04 `hub.audit` topic |
| Overload DEC-010 criteria | I-01 fetches `fixVersions`, `priority`, `labels` into projection |

## 14. Status

```
INTEGRATION DESIGN COMPLETE WITH OPEN ITEMS
```

---
⛔ **STOP — stage boundary.** Next: `05_Security_Design.md`.
