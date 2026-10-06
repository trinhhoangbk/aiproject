package com.mbs.hub.jira.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import com.mbs.hub.jira.JiraProperties;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

/** Covers SV-01 — HMAC signature verification. */
class HmacVerifierTest {

    private HmacVerifier verifier(String secret) {
        JiraProperties p = new JiraProperties(
                "https://x.atlassian.net", "bot@example.com", "token", secret,
                true, "0 0 * * * *", true);
        return new HmacVerifier(p);
    }

    @Test
    void validSignatureAccepted() throws Exception {
        String secret = "s3cr3t";
        byte[] body = "{\"webhookEvent\":\"jira:issue_updated\"}".getBytes(StandardCharsets.UTF_8);

        String signed = "sha256=" + hex(hmac(secret, body));

        assertThat(verifier(secret).verify(signed, body)).isTrue();
    }

    @Test
    void mismatchedSignatureRejected() {
        byte[] body = "{\"x\":1}".getBytes(StandardCharsets.UTF_8);
        assertThat(verifier("real-secret").verify("sha256=deadbeef", body)).isFalse();
    }

    @Test
    void nullHeaderRejected() {
        assertThat(verifier("s").verify(null, new byte[]{1,2,3})).isFalse();
    }

    private static byte[] hmac(String secret, byte[] data) throws Exception {
        Mac m = Mac.getInstance("HmacSHA256");
        m.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return m.doFinal(data);
    }
    private static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }
}
