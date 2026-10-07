package com.mbs.hub.sync.consumer;

import com.mbs.hub.config.KafkaTopics;
import com.mbs.hub.core.allowlist.AllowListRepository;
import com.mbs.hub.jira.dto.JiraFields;
import com.mbs.hub.jira.dto.JiraFixVersion;
import com.mbs.hub.jira.dto.JiraIssue;
import com.mbs.hub.sync.JiraInboundEvent;
import com.mbs.hub.sync.ProjectionUpdatedEvent;
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
import org.springframework.context.ApplicationEventPublisher;
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
 *   <li>On successful write, publish a {@link ProjectionUpdatedEvent} so the
 *       {@code MaterializedViewRefresher} can recompute (debounced).</li>
 * </ol>
 */
@Component
public class JiraEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(JiraEventConsumer.class);

    private static final List<String> DISCARDED_RESOLUTIONS = List.of("Won't Fix", "Duplicate", "Invalid");

    private final JiraPayloadMapper mapper;
    private final JiraEventDedupRepository dedup;
    private final IssueProjectionRepository issues;
    private final AllowListRepository allowList;
    private final ApplicationEventPublisher events;
    private final WorklogProjector worklogProjector;
    private final Counter processed;
    private final Counter dedupHits;
    private final Counter stale;

    public JiraEventConsumer(JiraPayloadMapper mapper,
                             JiraEventDedupRepository dedup,
                             IssueProjectionRepository issues,
                             AllowListRepository allowList,
                             ApplicationEventPublisher events,
                             WorklogProjector worklogProjector,
                             MeterRegistry metrics) {
        this.worklogProjector = worklogProjector;
        this.mapper = mapper;
        this.dedup = dedup;
        this.issues = issues;
        this.allowList = allowList;
        this.events = events;
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
            int rows = applyUpsert(issue);
            if (rows > 0) {
                worklogProjector.project(issue);   // AC-002.2 / AC-004.2
                events.publishEvent(new ProjectionUpdatedEvent(issue.key(), issue.projectKey()));
            } else {
                stale.increment();
            }
            processed.increment();
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed processing Jira inbound event", e);
            throw new RuntimeException(e);
        }
    }

    private boolean dedupInsert(JiraInboundEvent env) {
        JiraEventDedupKey k = new JiraEventDedupKey(env.idempotencyKey(), env.eventType());
        if (dedup.existsById(k)) return false;
        try {
            JiraEventDedup row = new JiraEventDedup();
            row.setKey(k);
            dedup.save(row);
            return true;
        } catch (DataIntegrityViolationException race) {
            return false;
        }
    }

    private int applyUpsert(JiraIssue issue) {
        JiraFields f = issue.fields();
        if (f == null) return 0;

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

        return issues.upsertWithGuard(
                issue.key(), issue.projectKey(), assignee, status, statusCat,
                resolution, discarded, priority, labelsPgArray,
                fixVersion, fvRelease, due,
                origH, remH, upd, allowOk);
    }

    private static BigDecimal secondsToHours(Integer seconds) {
        if (seconds == null) return null;
        return BigDecimal.valueOf(seconds)
                .divide(BigDecimal.valueOf(3600), 2, RoundingMode.HALF_UP);
    }

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
