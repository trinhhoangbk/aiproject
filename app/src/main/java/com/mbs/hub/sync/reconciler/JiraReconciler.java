package com.mbs.hub.sync.reconciler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mbs.hub.config.KafkaTopics;
import com.mbs.hub.core.allowlist.AllowListEntry;
import com.mbs.hub.core.allowlist.AllowListRepository;
import com.mbs.hub.jira.client.AdaptiveCadence;
import com.mbs.hub.jira.client.JiraRateLimitException;
import com.mbs.hub.jira.client.JiraRestClient;
import com.mbs.hub.jira.dto.JiraIssue;
import com.mbs.hub.jira.dto.JiraSearchResponse;
import com.mbs.hub.sync.JiraInboundEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Hourly reconciler — AC-007.2 / AC-007.3.
 *
 * <p>For every allow-listed Jira project, run a JQL search for issues updated
 * since the last successful tick and publish each one to
 * {@code jira.inbound.events} as a {@code reconcile.issue} event. The consumer
 * dedups and UPSERTs with the monotonic guard, so this acts as a safety net
 * for lost webhooks without creating duplicate state.</p>
 *
 * <p>On 429 the {@link AdaptiveCadence} doubles the next tick's gap (TD-COND-04).</p>
 */
@Component
public class JiraReconciler {

    private static final Logger log = LoggerFactory.getLogger(JiraReconciler.class);
    private static final String FIELDS = String.join(",",
            "status", "resolution", "assignee", "priority", "labels",
            "fixVersions", "duedate", "timeoriginalestimate", "timeestimate",
            "updated", "worklog");
    private static final int PAGE_SIZE = 100;

    private final JiraRestClient jira;
    private final AllowListRepository allowList;
    private final KafkaTemplate<String, String> kafka;
    private final AdaptiveCadence cadence;
    private final ObjectMapper om;
    private final Clock clock;
    private final Counter ticks;
    private final Counter published;
    private final Counter rateLimited;
    private final AtomicReference<OffsetDateTime> lastSuccess = new AtomicReference<>();

    public JiraReconciler(JiraRestClient jira, AllowListRepository allowList,
                          KafkaTemplate<String, String> kafka, AdaptiveCadence cadence,
                          ObjectMapper om, Clock clock, MeterRegistry metrics) {
        this.jira = jira;
        this.allowList = allowList;
        this.kafka = kafka;
        this.cadence = cadence;
        this.om = om;
        this.clock = clock;
        this.ticks       = Counter.builder("jira_reconciler_ticks_total").register(metrics);
        this.published   = Counter.builder("jira_reconciler_published_total").register(metrics);
        this.rateLimited = Counter.builder("jira_reconciler_rate_limited_total").register(metrics);
    }

    /**
     * Hourly on the hour. The cron is configured in {@code application.yml}
     * ({@code hub.jira.reconciler-cron}); the fallback here keeps a sensible
     * default for pure JUnit boot tests.
     */
    @Scheduled(cron = "${hub.jira.reconciler-cron:0 0 * * * *}")
    public void tick() {
        ticks.increment();
        boolean anyRateLimited = false;

        for (AllowListEntry entry : allowList.findByEnabledTrue()) {
            try {
                reconcileOne(entry.getProjectKey());
            } catch (JiraRateLimitException rl) {
                anyRateLimited = true;
                rateLimited.increment();
                log.warn("Reconciler hit 429 on project {} — backing off", entry.getProjectKey());
                break;   // honour global rate limit; next tick resumes
            } catch (RuntimeException e) {
                log.warn("Reconciler failed on project {}: {}", entry.getProjectKey(), e.getMessage());
            }
        }

        if (anyRateLimited) cadence.on429();
        else                cadence.onSuccess();
        lastSuccess.set(OffsetDateTime.now(clock));
    }

    private void reconcileOne(String projectKey) {
        // AC-007.3: look back 24h as safety net. The webhook is the fast path.
        String jql = "project = \"" + projectKey + "\" AND updated > -24h ORDER BY updated DESC";
        int startAt = 0;
        while (true) {
            JiraSearchResponse page = jira.search(jql, FIELDS, startAt, PAGE_SIZE);
            if (page == null || page.issues() == null || page.issues().isEmpty()) return;
            for (JiraIssue issue : page.issues()) publishIssue(issue);
            int fetched = page.issues().size();
            if (fetched < PAGE_SIZE) return;
            startAt += fetched;
        }
    }

    private void publishIssue(JiraIssue issue) {
        try {
            String payload = om.writeValueAsString(issue);
            String idemp = idempotencyKey("reconcile", issue.key(), payload);
            JiraInboundEvent env = new JiraInboundEvent(
                    JiraInboundEvent.CURRENT_SCHEMA,
                    "reconcile.issue",
                    idemp,
                    OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.ofHours(7)),
                    payload
            );
            String envelope = om.writeValueAsString(env);
            kafka.send(KafkaTopics.JIRA_INBOUND_EVENTS, issue.key(), envelope);
            published.increment();
        } catch (Exception e) {
            log.warn("Reconciler failed to publish {}: {}", issue.key(), e.getMessage());
        }
    }

    private static String idempotencyKey(String kind, String issueKey, String payload) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(kind.getBytes());
            md.update((byte) 0);
            md.update(issueKey.getBytes());
            md.update((byte) 0);
            md.update(payload.getBytes());
            return HexFormat.of().formatHex(md.digest());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public OffsetDateTime lastSuccessAt() { return lastSuccess.get(); }
}
