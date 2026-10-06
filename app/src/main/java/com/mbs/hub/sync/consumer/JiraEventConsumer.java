package com.mbs.hub.sync.consumer;

import com.mbs.hub.config.KafkaTopics;
import com.mbs.hub.core.allowlist.AllowListRepository;
import com.mbs.hub.jira.dto.JiraFields;
import com.mbs.hub.jira.dto.JiraFixVersion;
import com.mbs.hub.jira.dto.JiraIssue;
import com.mbs.hub.sync.JiraInboundEvent;
import com.mbs.hub.sync.dedup.JiraEventDedup;
import com.mbs.hub.sync.dedup.JiraEventDedupKey;
import com.mbs.hub.sync.dedup.JiraEventDedupRepository;
import com.mbs.hub.sync.projection.IssueProjectionRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Jira inbound-event consumer — AC-007.1 / AC-007.3.
 *
 * <ol>
 *   <li>Dedup via {@code (jira_event_id, event_type)}: INSERT, ignore duplicate.</li>
 *   <li>Parse the issue payload.</li>
 *   <li>UPSERT the issue projection with the monotonic {@code jira_updated_at} guard.</li>
 * </ol>
 *
 * <p>Worklogs are embedded in the fields payload; this cycle only indexes the issue
 * itself; worklog projection ingestion (full-detail path) is added in a later cycle
 * when the daily-report service (PLAN-022) consumes it.</p>
 */
@Component
public class JiraEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(JiraEventConsumer.class);

    private static final List<String> DISCARDED_RESOLUTIONS = List.of("Won't Fix", "Duplicate", "Invalid");

    private final JiraPayloadMapper mapper;
    private final JiraEventDedupRepository dedup;
    private final IssueProjectionRepository issues;
    private final AllowListRepository allowList;
    private final Counter processed;
    private final Counter dedupHits;
    private final Counter stale;

    public JiraEventConsumer(JiraPayloadMapper mapper,
                             JiraEventDedupRepository dedup,
                             IssueProjectionRepository issues,
                             AllowListRepository allowList,
                             MeterRegistry metrics) {
        this.mapper = mapper;
        this.dedup = dedup;
        this.issues = issues;
        this.allowList = allowList;
        this.processed  = Counter.builder("jira_events_processed_total").register(metrics);
        this.dedupHits  = Counter.builder("jira_event_dedup_hits_total").register(metrics);
        this.stale      = Counter.builder("jira_event_stale_dropped_total").register(metrics);
    }

    @KafkaListener(topics = KafkaTopics.JIRA_INBOUND_EVENTS, groupId = "hub")
    @Transactional
    public void onEvent(String envelopeJson, Acknowledgment ack) {
        try {
            JiraInboundEvent env = mapper.parseEnvelope(envelopeJson);
            if (!dedupInsert(env)) {
                dedupHits.increment();
                ack.acknowledge();
                return;
            }
            JiraIssue issue = mapper.extractIssue(env.payloadJson());
            applyUpsert(issue);
            processed.increment();
            ack.acknowledge();
        } catch (Exception e) {
            // Routed to hub.dlq by Spring Kafka's default error handler at runtime (configured in
            // KafkaConfig if/when a DefaultErrorHandler bean is wired — not in this cycle for brevity).
            log.error("Failed processing Jira inbound event", e);
            throw new RuntimeException(e);
        }
    }

    /** Returns true if this is the first time we see (jira_event_id, event_type). */
    private boolean dedupInsert(JiraInboundEvent env) {
        JiraEventDedupKey k = new JiraEventDedupKey(env.idempotencyKey(), env.eventType());
        if (dedup.existsById(k)) return false;
        try {
            JiraEventDedup row = new JiraEventDedup();
            row.setKey(k);
            dedup.save(row);
            return true;
        } catch (DataIntegrityViolationException race) {
            // Lost a race with another consumer — treat as duplicate.
            return false;
        }
    }

    private void applyUpsert(JiraIssue issue) {
        JiraFields f = issue.fields();
        if (f == null) return;

        String assignee     = f.assignee() == null ? null : f.assignee().accountId();
        String status       = f.status() == null ? "" : f.status().name();
        String statusCat    = f.status() == null || f.status().statusCategory() == null
                ? "indeterminate" : f.status().statusCategory().key();
        String resolution   = f.resolution() == null ? null : f.resolution().name();
        boolean discarded   = resolution != null && DISCARDED_RESOLUTIONS.contains(resolution);
        String priority     = f.priority() == null ? null : f.priority().name();
        String[] labels     = f.labels() == null ? new String[0] : f.labels().toArray(new String[0]);
        JiraFixVersion fv   = (f.fixVersions() == null || f.fixVersions().isEmpty())
                ? null : f.fixVersions().get(0);
        String fixVersion   = fv == null ? null : fv.name();
        LocalDate fvRelease = fv == null ? null : fv.releaseDate();
        LocalDate due       = f.dueDate();
        BigDecimal origH    = secondsToHours(f.originalEstimateSeconds());
        BigDecimal remH     = secondsToHours(f.remainingEstimateSeconds());
        OffsetDateTime upd  = f.updated();
        boolean allowOk     = allowList.existsByProjectKeyAndEnabledTrue(issue.projectKey());

        String labelsPgArray = pgTextArray(labels);

        int rows = issues.upsertWithGuard(
                issue.key(), issue.projectKey(), assignee, status, statusCat,
                resolution, discarded, priority, labelsPgArray,
                fixVersion, fvRelease, due,
                origH, remH, upd, allowOk);
        if (rows == 0) stale.increment();
    }

    private static BigDecimal secondsToHours(Integer seconds) {
        if (seconds == null) return null;
        return BigDecimal.valueOf(seconds)
                .divide(BigDecimal.valueOf(3600), 2, RoundingMode.HALF_UP);
    }

    /** Turns a String[] into a PostgreSQL array literal, e.g. {"bug","P0"}. */
    private static String pgTextArray(String[] arr) {
        if (arr == null || arr.length == 0) return "{}";
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(',');
            sb.append('"').append(arr[i].replace("\\", "\\\\").replace("\"", "\\\"")).append('"');
        }
        sb.append('}');
        return sb.toString();
    }
}
