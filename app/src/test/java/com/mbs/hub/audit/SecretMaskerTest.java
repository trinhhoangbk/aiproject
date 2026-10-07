package com.mbs.hub.audit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** TC-SM-01..02 — 05 Security SV-04: secrets never reach audit payloads. */
class SecretMaskerTest {

    SecretMasker masker = new SecretMasker();

    @Test
    void TC_SM_01_secretJsonValuesMaskedOthersKept() {
        String out = masker.mask("{\"password\":\"p@ss\",\"apiToken\":\"ATATTabc\","
                + "\"jira_webhook_secret\":\"deadbeef\",\"email\":\"a@b.vn\"}");

        assertThat(out).contains("\"password\":\"***\"")
                       .contains("\"apiToken\":\"***\"")
                       .contains("\"jira_webhook_secret\":\"***\"")
                       .contains("\"email\":\"a@b.vn\"")
                       .doesNotContain("p@ss", "ATATTabc", "deadbeef");
    }

    @Test
    void TC_SM_02_authorizationHeaderMasked() {
        assertThat(masker.mask("Authorization: Basic Ym90OnRva2Vu"))
                .isEqualTo("Authorization: Basic ***");
    }
}
