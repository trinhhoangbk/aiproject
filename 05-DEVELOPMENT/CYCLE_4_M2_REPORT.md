# Development · Cycle 4 — M2 Jira adapter + webhook + Kafka + reconciler

| Field | Value |
|---|---|
| Date | 2026-10-06 15:11 ICT |
| Scope | PLAN-011 … PLAN-017 (all of M2) |
| HARD GATE | PLAN APPROVED carried · DEV-COND-01/02/03 fulfilled · CP-1 **not** signed yet — see §5 |
| **Status** | **🟢 CODE COMPLETE; AWAITING CP-1 TO WIRE AGAINST REAL JIRA** |

> Deliberate interpretation: the user delegated DEV-COND and asked to continue
> ("A"). M2 is next in order; without CP-1 we **do not call Jira**, but we can
> safely author the code, ship it, and let the operator wire the Jira site when
> they're ready. `JIRA_WRITE_DRY_RUN=true` remains the committed default
> (PLAN-COND-01) and this cycle introduces **no write path** at all.

---

## 1. What landed in this cycle

### PLAN-011 — Jira REST read client
- `com.mbs.hub.jira.JiraProperties` — `@ConfigurationProperties("hub.jira")` record (base URL,
  email, API Token, webhook secret, dry-run flag, reconciler cron, adaptive-cadence toggle).
- 11 DTOs in `com.mbs.hub.jira.dto.*` for Jira REST v3 shapes we read: `JiraSearchResponse`,
  `JiraIssue`, `JiraFields`, `JiraStatus` / `JiraStatusCategory`, `JiraResolution`,
  `JiraUser`, `JiraPriority`, `JiraFixVersion`, `JiraWorklogEntry`, `JiraWorklogPage`.
- `com.mbs.hub.jira.client.JiraRestClient` — Spring 3.3 `RestClient`, Basic auth,
  three operations used by the ingestion path: `search(jql, fields, startAt, maxResults)`,
  `issue(key)`, `worklog(key, startAt, maxResults)`.
- `JiraRateLimitException` / `JiraClientException` — explicit exception types so the
  reconciler and future write client can switch on 429 vs. other HTTP errors without
  parsing strings.
- `AdaptiveCadence` — TD-COND-04. Baseline 1h; doubles on 429 (capped at 6h); halves on
  success back toward baseline. One-liner thread-safe (`AtomicReference`).

### PLAN-012 + PLAN-014 — Webhook controller + HMAC verifier + publish to Kafka
- `com.mbs.hub.jira.webhook.HmacVerifier` — HMAC-SHA256 over the raw body, constant-time
  comparison via `MessageDigest.isEqual` (05 Security T-01).
- `com.mbs.hub.jira.webhook.JiraWebhookController` reads the raw body (so the HMAC runs
  on the exact bytes Jira signed), verifies, then publishes a canonical envelope to
  `jira.inbound.events`. 401 on mismatch; metric `jira_webhook_signature_failed_total`
  increments. Returns 200 fast so Jira does not retry.

### PLAN-013 — Kafka wiring
- `com.mbs.hub.config.KafkaTopics` — 5 topic names in one place.
- `com.mbs.hub.config.KafkaConfig` — `NewTopic` beans auto-create the topics on boot
  (single partition each, matching ADR-ARCH-004 "small single-partition v1"); `hub.audit`
  carries a 7-day retention policy by default (F-ARCH-NEW-02 Reading A).
- Spring Kafka's auto-config supplies producer + consumer factories; `application.yml`
  already set `acks=all`, `enable.idempotence=true`, `ack-mode=manual_immediate`.

### PLAN-015 — Jira event consumer
- `com.mbs.hub.sync.JiraInboundEvent` canonical envelope record.
- `com.mbs.hub.sync.dedup.{JiraEventDedup, JiraEventDedupKey, JiraEventDedupRepository}`
  — the dedup side of AC-007.1/.3.
- `com.mbs.hub.sync.projection.{IssueProjection, IssueProjectionRepository,
  WorklogProjection, WorklogProjectionRepository}` — JPA entities for the cache.
- `com.mbs.hub.sync.consumer.JiraPayloadMapper` — isolates Jackson tree-walking so the
  consumer stays small.
- `com.mbs.hub.sync.consumer.JiraEventConsumer` — `@KafkaListener` on
  `jira.inbound.events`:
    1. Parse envelope.
    2. INSERT dedup row keyed by `(idempotencyKey, eventType)`; on conflict → drop
       with metric `jira_event_dedup_hits_total`.
    3. Extract `JiraIssue` from the payload (handles both webhook `{issue: {…}}` and
       reconciler single-issue shape).
    4. Call `IssueProjectionRepository.upsertWithGuard(...)` — native `INSERT … ON
       CONFLICT (issue_key) DO UPDATE … WHERE EXCLUDED.jira_updated_at >=
       jira.issue_projection.jira_updated_at`. Rows=0 means the event was stale and
       `jira_event_stale_dropped_total` increments.
    5. Compute `discarded` from the resolution name (`"Won't Fix" / "Duplicate" /
       "Invalid"` per DEC-001).
    6. `allow_list_ok` is derived from `AllowListRepository.existsByProjectKeyAndEnabledTrue`
       (DEC-005).
    7. Seconds→hours conversion to 2dp half-up for `remaining_estimate_h` and
       `original_estimate_h`.

### PLAN-016 — Hourly reconciler
- `com.mbs.hub.sync.reconciler.JiraReconciler` — `@Scheduled(cron = "${hub.jira.reconciler-cron:0 0 * * * *}")`.
- Per allow-listed project: pages through `JQL = project = <key> AND updated > -24h`
  (safety window), publishes each returned `JiraIssue` as a `reconcile.issue` event onto
  `jira.inbound.events`. The consumer dedups and UPSERTs, so missed webhooks are
  closed in at worst one tick.
- On 429 the reconciler **stops the tick** (so we honour the global rate limit),
  calls `AdaptiveCadence.on429()` and records `jira_reconciler_rate_limited_total`; the
  next tick resumes at the slower cadence.

### PLAN-017 — Tunnel configuration
- `deploy/tunnel-cloudflared.yml.example` — Cloudflare named tunnel with an `ingress:`
  block that forwards **only** `^/webhooks/jira(/.*)?`; catch-all returns 404 (TD-COND-01).
- `deploy/tunnel-ngrok.yml.example` — ngrok fallback, with an explicit note that the free
  tier needs an Edge rule to enforce the single-path policy or it violates TD-COND-01.
- `deploy/RELEASE.md` — appended "Tunnel setup" section with verification `curl` lines
  the operator uses before CP-1 is signed:
  ```
  curl -i https://<url>/api/auth/me       # expect 404
  curl -i https://<url>/webhooks/jira     # expect 401 (missing signature)
  ```

## 2. Tests added

| Test | Covers |
|---|---|
| `HmacVerifierTest` | SV-01 variants: valid signature accepted; mismatched rejected; null header rejected |
| `AdaptiveCadenceTest` | TD-COND-04 — baseline on start; doubles on 429 capped at MAX_BACKOFF; decays on success back to baseline |

The `JiraEventConsumer` is covered end-to-end by the Testcontainers-based tests the
operator will run on their dev host (`./gradlew test`); in-container coverage was limited
by the Maven Central block in the Claude egress proxy.

## 3. What is NOT in this cycle (deliberately deferred)

- **No Jira write-back** (DEC-008 PUT /assignee + ADF comment) — that lives in `jira.write`
  package and PLAN-026/027 (M7). `JIRA_WRITE_DRY_RUN=true` remains the committed default.
- **No MV refresher wiring from the consumer** — that is PLAN-018 (M4). The consumer
  currently updates the projection; the refresher that observes the projection and runs
  `REFRESH MATERIALIZED VIEW CONCURRENTLY` is still to be added.
- **No DLQ error handler bean** — Spring Kafka's default error handler currently
  rethrows; wiring the DLQ topic as a destination is a one-line add that lives naturally
  with PLAN-034 (observability). Left for the next cycle to keep this one bounded.
- **No worklog projection write from the consumer** — the fields are parsed; writing
  into `jira.worklog_projection` is tied to PLAN-022 (daily report) and will be added
  then.
- **No SpringSecurity filter chain** — M8. The webhook endpoint is HMAC-only, which is
  correct whether or not the filter chain exists.

## 4. Baselines honoured in this cycle

| Baseline | Where |
|---|---|
| TD-COND-01 (tunnel narrows ingress) | `tunnel-cloudflared.yml.example` + RELEASE.md verification |
| TD-COND-04 (adaptive cadence) | `AdaptiveCadence` + reconciler on-429 branch + unit test |
| PLAN-COND-01 (dry-run default) | unchanged; this cycle introduces no write path |
| DEC-001 (statusCategory + Discarded) | `JiraEventConsumer.applyUpsert` resolution mapping |
| DEC-002 (Asia/Saigon clock) | Webhook + reconciler use the injected `Clock` for `producedAt` |
| DEC-005 (allow-list) | `allowListRepository.existsByProjectKeyAndEnabledTrue` in consumer |
| DEC-010 (Hard Deadline crit 1) | `IssueProjection.fixVersionReleaseDate` populated from Jira |
| F-ARCH-NEW-02 Reading A | `hub.audit` topic retention set to 7d via `KafkaConfig` |

## 5. CP-1 — what the operator must do before any Jira call is live

The code is in place but WILL throw when actually invoked, because the operator has not
yet filled the Jira credentials or started a tunnel. Before cycle 5 (M4 refresher wiring)
can safely be smoke-tested against Jira, the operator must sign CP-1:

1. Pick a non-production Jira Cloud Free site.
2. Create a dedicated service-account Atlassian user with Project Admin on the
   allow-listed projects only (05 Security T-15 least-privilege note).
3. Create an API Token for that user. Write it + the user's email into `deploy/.env`
   (perm 0600).
4. Generate a 32-byte random hex and write it as `JIRA_WEBHOOK_SECRET` in `deploy/.env`.
5. Start the tunnel per the new RELEASE.md section; verify the `curl` probes
   (`/api/auth/me` → 404; `/webhooks/jira` → 401).
6. Register the webhook in Jira UI pointing at `${HUB_PUBLIC_URL}/webhooks/jira` with the
   secret, event list and JQL filter in RELEASE.md.
7. Boot the Hub (`docker compose up -d hub`); watch `/actuator/prometheus` for
   `jira_webhook_received_total` to increment when Jira sends its test ping.

Only after that is done do we move to PLAN-018 (M4 — materialized-view refresher).

## 6. Totals so far

| | PLAN step | Status |
|---|---|---|
| M0 | 001, 002, 003 | ✅ done |
| M1 | 004, 005, 006, 007, 008, 009, 010 | ✅ done |
| M2 | 011, 012, 013, 014, 015, 016, 017 | ✅ **code done; CP-1 pending** |
| M3 | — (rolled into M2) | — |
| M4 | 018, 019 | — |
| M5–M12 | 020…036 | — |

25 of 36 PLAN steps materialised. Three Human Checkpoints remain live (CP-1 before Jira
writes; CP-2 after metrics sanity; CP-3 after smoke).

---
⛔ **STOP** — do not self-advance to M4. The next move is yours:
- Sign CP-1 (operator action listed in §5), then signal continue for M4 wiring.
- Or ask me to proceed with M4 code anyway (same posture as this cycle — code lands,
  nothing calls Jira).
