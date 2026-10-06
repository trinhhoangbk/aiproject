package com.mbs.hub.sync;

import java.time.OffsetDateTime;

/**
 * Canonical envelope the Webhook controller and the Reconciler produce onto
 * {@code jira.inbound.events}. The consumer dedups on {@code idempotencyKey}
 * and applies the raw JSON payload (opaque to Kafka).
 */
public record JiraInboundEvent(
        int schemaVersion,
        String eventType,            // e.g. jira:issue_updated, jira:worklog_updated, reconcile.issue
        String idempotencyKey,       // (jira_event_id, event_type) hash
        OffsetDateTime producedAt,
        String payloadJson           // raw JSON of the Jira issue or worklog
) {
    public static final int CURRENT_SCHEMA = 1;
}
