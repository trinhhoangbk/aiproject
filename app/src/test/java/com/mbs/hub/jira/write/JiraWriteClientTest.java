package com.mbs.hub.jira.write;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mbs.hub.jira.JiraProperties;
import com.mbs.hub.jira.client.JiraClientException;
import com.mbs.hub.jira.client.JiraRateLimitException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** TC-JW-01..04 — PLAN-COND-01 dry-run gate, AC-006.4 / AC-006.5, INT-R-02 (429). */
class JiraWriteClientTest {

    static final String BASE = "https://weeklywtf.atlassian.net";
    static final String ASSIGNEE_URL = BASE + "/rest/api/3/issue/KAN-1/assignee";

    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

    JiraWriteClient client(boolean dryRun) {
        JiraProperties props = new JiraProperties(BASE, "bot@example.com", "token", "secret",
                dryRun, "0 0 * * * *", true);
        return new JiraWriteClient(props, builder, new ObjectMapper(), new SimpleMeterRegistry());
    }

    @Test
    void TC_JW_01_dryRunSendsNoHttpRequest() {
        WriteOutcome out = client(true).assignIssue("KAN-1", "acc-T");

        assertThat(out.dryRun()).isTrue();
        assertThat(out.ok()).isTrue();
        server.verify();   // no expectation registered → any request would have failed
    }

    @Test
    void TC_JW_02_assigneePutWithAccountId() {
        server.expect(requestTo(ASSIGNEE_URL))
              .andExpect(method(HttpMethod.PUT))
              .andExpect(content().json("{\"accountId\":\"acc-T\"}"))
              .andRespond(withNoContent());

        WriteOutcome out = client(false).assignIssue("KAN-1", "acc-T");

        assertThat(out.dryRun()).isFalse();
        assertThat(out.statusCode()).isEqualTo(204);
        assertThat(out.ok()).isTrue();
        server.verify();
    }

    @Test
    void TC_JW_03_forbiddenSurfacesAsClientExceptionWith403() {
        server.expect(requestTo(ASSIGNEE_URL)).andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> client(false).assignIssue("KAN-1", "acc-T"))
                .isInstanceOf(JiraClientException.class)
                .satisfies(e -> assertThat(((JiraClientException) e).status()).isEqualTo(403));
    }

    @Test
    void TC_JW_04_rateLimitHonoursRetryAfter() {
        HttpHeaders h = new HttpHeaders();
        h.add("Retry-After", "30");
        server.expect(requestTo(ASSIGNEE_URL))
              .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).headers(h));

        assertThatThrownBy(() -> client(false).assignIssue("KAN-1", "acc-T"))
                .isInstanceOf(JiraRateLimitException.class)
                .satisfies(e -> assertThat(((JiraRateLimitException) e).retryAfter())
                        .isEqualTo(Duration.ofSeconds(30)));
    }
}
