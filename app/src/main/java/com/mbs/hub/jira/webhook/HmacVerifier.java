package com.mbs.hub.jira.webhook;

import com.mbs.hub.jira.JiraProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * Verifies the {@code X-Hub-Signature: sha256=HEX} header against the raw body
 * using the Jira webhook secret. Constant-time comparison (05 Security T-01).
 */
@Component
public class HmacVerifier {

    private final JiraProperties props;
    public HmacVerifier(JiraProperties props) { this.props = props; }

    public boolean verify(String header, byte[] rawBody) {
        if (header == null) return false;
        String expected = "sha256=" + hex(hmacSha256(props.webhookSecret(), rawBody));
        // Constant-time compare — do NOT short-circuit on first mismatch.
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                header.getBytes(StandardCharsets.UTF_8)
        );
    }

    private static byte[] hmacSha256(String secret, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC init failed", e);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
