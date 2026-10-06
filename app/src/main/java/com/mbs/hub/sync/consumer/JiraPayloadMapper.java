package com.mbs.hub.sync.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mbs.hub.jira.dto.JiraIssue;
import com.mbs.hub.sync.JiraInboundEvent;
import java.io.IOException;
import org.springframework.stereotype.Component;

/**
 * Pulls a {@link JiraIssue} out of the envelope. The envelope embeds either
 * the full webhook JSON (which has an {@code issue} field) or a reconciler-emitted
 * single issue JSON. Both shapes are handled.
 */
@Component
public class JiraPayloadMapper {

    private final ObjectMapper om;

    public JiraPayloadMapper(ObjectMapper om) { this.om = om; }

    public JiraInboundEvent parseEnvelope(String json) throws IOException {
        JsonNode n = om.readTree(json);
        return new JiraInboundEvent(
                n.path("schemaVersion").asInt(JiraInboundEvent.CURRENT_SCHEMA),
                n.path("eventType").asText(""),
                n.path("idempotencyKey").asText(""),
                java.time.OffsetDateTime.parse(n.path("producedAt").asText()),
                n.path("payload").toString()
        );
    }

    /** Extracts the {@link JiraIssue} payload, whether the webhook put it inline or in {@code issue}. */
    public JiraIssue extractIssue(String payloadJson) throws IOException {
        JsonNode root = om.readTree(payloadJson);
        JsonNode issueNode = root.has("issue") ? root.get("issue") : root;
        return om.treeToValue(issueNode, JiraIssue.class);
    }
}
