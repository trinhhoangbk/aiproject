package com.mbs.hub.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mbs.hub.config.KafkaTopics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * PLAN-031 — the single write path for the append-only audit log.
 *
 * <p>Every event is: (a) persisted into {@code audit.audit_log}, and
 * (b) published onto the {@code hub.audit} Kafka topic (7-day retention,
 * F-ARCH-NEW-02 Reading A). The DB write and the Kafka publish happen in
 * {@code REQUIRES_NEW} so an audit of a FAILED business operation still
 * commits.</p>
 *
 * <p>The payload is a JSON Map rendered through Jackson, then run through
 * {@link SecretMasker} before storage and before publish.</p>
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditEventRepository repo;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper json;
    private final SecretMasker masker;
    private final Clock clock;
    private final Counter writes;
    private final Counter writeFailures;

    public AuditService(AuditEventRepository repo,
                        KafkaTemplate<String, String> kafka,
                        ObjectMapper json,
                        SecretMasker masker,
                        Clock clock,
                        MeterRegistry metrics) {
        this.repo = repo;
        this.kafka = kafka;
        this.json = json;
        this.masker = masker;
        this.clock = clock;
        this.writes = Counter.builder("audit_writes_total").register(metrics);
        this.writeFailures = Counter.builder("audit_write_failures_total").register(metrics);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID actor, String action, String targetType, String targetId,
                       Map<String, Object> payload, String result, Integer jiraStatus) {
        try {
            String masked = masker.mask(json.writeValueAsString(payload == null ? Map.of() : payload));
            AuditEvent e = new AuditEvent();
            e.setActorMemberId(actor);
            e.setAction(action);
            e.setTargetType(targetType);
            e.setTargetId(targetId);
            e.setPayload(masked);
            e.setResult(result);
            e.setJiraStatus(jiraStatus);
            repo.save(e);
            writes.increment();

            // Publish mirror event — fire-and-forget; Kafka failure is logged but does
            // not roll back the DB write (the DB IS the source of truth; Kafka is the
            // 7-day mirror for downstream consumers).
            try {
                String envelope = json.writeValueAsString(Map.of(
                        "occurredAt", clock.instant().toString(),
                        "actor",      actor,
                        "action",     action,
                        "targetType", targetType,
                        "targetId",   targetId,
                        "result",     result,
                        "jiraStatus", jiraStatus,
                        "payload",    masked
                ));
                kafka.send(KafkaTopics.HUB_AUDIT, targetId, envelope);
            } catch (Exception ke) {
                log.warn("audit kafka publish failed (DB write persisted) action={}", action, ke);
            }
        } catch (JsonProcessingException | RuntimeException ex) {
            writeFailures.increment();
            // Never let an audit failure break the primary operation.
            log.error("audit write failed action={} target={}:{}", action, targetType, targetId, ex);
        }
    }

    public void ok(UUID actor, String action, String targetType, String targetId, Map<String, Object> payload) {
        record(actor, action, targetType, targetId, payload, "OK", null);
    }

    public void failed(UUID actor, String action, String targetType, String targetId, Map<String, Object> payload) {
        record(actor, action, targetType, targetId, payload, "FAILED", null);
    }
}
