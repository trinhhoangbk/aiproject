package com.mbs.hub.jira.webhook;

import com.mbs.hub.config.KafkaTopics;
import com.mbs.hub.sync.JiraInboundEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Jira inbound webhook — AC-007.1.
 *
 * <p>Flow (04 Integration §4.2):</p>
 * <ol>
 *   <li>Verify {@code X-Hub-Signature} HMAC. Mismatch → 401, metric increments.</li>
 *   <li>Translate the delivery into a {@link JiraInboundEvent} with an idempotency key.</li>
 *   <li>Publish to {@code jira.inbound.events} (key = Jira delivery id).</li>
 *   <li>Return 200 fast so Jira does not retry.</li>
 * </ol>
 *
 * <p>IMPORTANT: this controller reads the raw request body (not a JSON model)
 * so HMAC verification runs on the exact bytes Jira signed.</p>
 */
@RestController
@RequestMapping("/webhooks/jira")
public class JiraWebhookController {

    private static final Logger log = LoggerFactory.getLogger(JiraWebhookController.class);

    private final HmacVerifier hmac;
    private final KafkaTemplate<String, String> kafka;
    private final Clock clock;
    private final Counter received;
    private final Counter signatureFailed;

    public JiraWebhookController(HmacVerifier hmac,
                                 KafkaTemplate<String, String> kafka,
                                 Clock clock,
                                 MeterRegistry metrics) {
        this.hmac = hmac;
        this.kafka = kafka;
        this.clock = clock;
        this.received        = Counter.builder("jira_webhook_received_total").register(metrics);
        this.signatureFailed = Counter.builder("jira_webhook_signature_failed_total").register(metrics);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> accept(
            @RequestHeader(name = "X-Hub-Signature", required = false) String signature,
            @RequestHeader(name = "X-Atlassian-Webhook-Identifier", required = false) String deliveryId,
            @RequestHeader(name = "X-Jira-Webhook-Event", required = false) String eventType,
            HttpServletRequest request,
            @RequestBody byte[] rawBody) {

        if (!hmac.verify(signature, rawBody)) {
            signatureFailed.increment();
            log.warn("Jira webhook: HMAC mismatch (delivery={}, event={})", deliveryId, eventType);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        received.increment();

        String type = eventType == null ? "jira:unknown" : eventType;
        // Idempotency key: SHA-256 of (deliveryId || eventType || body hash) — stable, 64 hex chars.
        String idempotencyKey = deliveryIdempotencyKey(deliveryId, type, rawBody);
        String payload = new String(rawBody, java.nio.charset.StandardCharsets.UTF_8);

        JiraInboundEvent event = new JiraInboundEvent(
                JiraInboundEvent.CURRENT_SCHEMA,
                type,
                idempotencyKey,
                OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.ofHours(7)),
                payload
        );
        // Simple serialization (payload is already JSON); envelope → JSON object by hand to avoid
        // circular dependency on an ObjectMapper here.
        kafka.send(KafkaTopics.JIRA_INBOUND_EVENTS,
                /* key = */ deliveryId != null ? deliveryId : idempotencyKey,
                /* value = */ serialize(event));

        return ResponseEntity.ok().build();
    }

    private String serialize(JiraInboundEvent e) {
        // Minimal, allocation-light envelope. The payload is already JSON — embed as-is.
        return "{\"schemaVersion\":" + e.schemaVersion()
                + ",\"eventType\":" + quote(e.eventType())
                + ",\"idempotencyKey\":" + quote(e.idempotencyKey())
                + ",\"producedAt\":" + quote(e.producedAt().toString())
                + ",\"payload\":" + e.payloadJson()
                + "}";
    }

    private static String quote(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String deliveryIdempotencyKey(String deliveryId, String eventType, byte[] body) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            if (deliveryId != null) md.update(deliveryId.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            md.update((byte) 0);
            md.update(eventType.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            md.update((byte) 0);
            md.update(body);
            return HexFormat.of().formatHex(md.digest());
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
