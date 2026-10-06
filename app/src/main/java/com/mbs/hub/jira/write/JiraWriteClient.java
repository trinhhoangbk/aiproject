package com.mbs.hub.jira.write;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mbs.hub.jira.JiraProperties;
import com.mbs.hub.jira.client.JiraClientException;
import com.mbs.hub.jira.client.JiraRateLimitException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Jira write client — PLAN-026 / DEC-008.
 *
 * <p>Two operations:
 * <ol>
 *   <li>{@link #assignIssue(String, String)} — PUT /rest/api/3/issue/{key}/assignee
 *       with body {@code {"accountId":"..."}} to transfer the assignee.</li>
 *   <li>{@link #commentIssue(String, String)} — POST /rest/api/3/issue/{key}/comment
 *       with an ADF comment body. Used to record who requested the assignment and why.</li>
 * </ol>
 *
 * <p><strong>PLAN-COND-01 dry-run gate:</strong> when
 * {@code hub.jira.write-dry-run=true}, every call short-circuits BEFORE any HTTP
 * request leaves the JVM. The method logs the intended payload and returns a
 * fabricated {@link WriteOutcome} with {@code dryRun=true}. Flipping the flag to
 * {@code false} is an operator action (see §CP-1 in the runbook).</p>
 */
@Component
public class JiraWriteClient {

    private static final Logger log = LoggerFactory.getLogger(JiraWriteClient.class);

    private final RestClient http;
    private final JiraProperties props;
    private final ObjectMapper json;
    private final Counter writesAttempted;
    private final Counter writesDryRun;
    private final Counter writesOk;
    private final Counter writesFailed;
    private final Counter writesRateLimited;

    public JiraWriteClient(JiraProperties props,
                           RestClient.Builder builder,
                           ObjectMapper json,
                           MeterRegistry metrics) {
        this.props = props;
        this.json = json;
        String auth = props.userEmail() + ":" + props.apiToken();
        String basic = "Basic " + Base64.getEncoder()
                .encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        this.http = builder
                .baseUrl(props.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, basic)
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .defaultHeader("X-Atlassian-Token", "no-check")
                .build();

        this.writesAttempted   = Counter.builder("jira_writes_attempted_total").register(metrics);
        this.writesDryRun      = Counter.builder("jira_writes_dry_run_total").register(metrics);
        this.writesOk          = Counter.builder("jira_writes_ok_total").register(metrics);
        this.writesFailed      = Counter.builder("jira_writes_failed_total").register(metrics);
        this.writesRateLimited = Counter.builder("jira_writes_rate_limited_total").register(metrics);
    }

    public WriteOutcome assignIssue(String issueKey, String assigneeAccountId) {
        writesAttempted.increment();
        Map<String, Object> body = Map.of("accountId", assigneeAccountId);
        if (props.writeDryRun()) {
            writesDryRun.increment();
            log.info("[DRY-RUN] PUT /rest/api/3/issue/{}/assignee body={} — JIRA_WRITE_DRY_RUN=true, no HTTP call issued",
                    issueKey, body);
            return WriteOutcome.dryRun("PUT", "/rest/api/3/issue/" + issueKey + "/assignee");
        }
        return doWrite("PUT", "/rest/api/3/issue/" + issueKey + "/assignee", body);
    }

    /**
     * Post an ADF comment. Pass the ADF document itself as {@code adfDoc} — the
     * client wraps it as {@code {"body": <adfDoc>}} per Jira Cloud REST v3.
     */
    public WriteOutcome commentIssue(String issueKey, Map<String, Object> adfDoc) {
        writesAttempted.increment();
        Map<String, Object> body = Map.of("body", adfDoc);
        if (props.writeDryRun()) {
            writesDryRun.increment();
            log.info("[DRY-RUN] POST /rest/api/3/issue/{}/comment — JIRA_WRITE_DRY_RUN=true, no HTTP call issued",
                    issueKey);
            return WriteOutcome.dryRun("POST", "/rest/api/3/issue/" + issueKey + "/comment");
        }
        return doWrite("POST", "/rest/api/3/issue/" + issueKey + "/comment", body);
    }

    // -------------------------------------------------------------------

    private WriteOutcome doWrite(String method, String path, Map<String, Object> body) {
        try {
            var spec = switch (method) {
                case "PUT"  -> http.put().uri(path);
                case "POST" -> http.post().uri(path);
                default -> throw new IllegalArgumentException("unsupported method " + method);
            };
            var response = spec.body(body).retrieve().toBodilessEntity();
            int status = response.getStatusCode().value();
            writesOk.increment();
            return new WriteOutcome(method, path, status, false, null);
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            if (status == 429) {
                writesRateLimited.increment();
                Duration retryAfter = parseRetryAfter(e.getResponseHeaders() != null
                        ? e.getResponseHeaders().getFirst("Retry-After") : null);
                log.warn("Jira {} {} rate-limited (429); retry-after={}", method, path, retryAfter);
                throw new JiraRateLimitException(retryAfter);
            }
            writesFailed.increment();
            log.warn("Jira {} {} failed status={}", method, path, status);
            throw new JiraClientException(status, "Jira write failed: " + method + " " + path + " status=" + status);
        }
    }

    private static Duration parseRetryAfter(String header) {
        if (header == null || header.isBlank()) return Duration.ofSeconds(60);
        try {
            long seconds = Long.parseLong(header.trim());
            return Duration.ofSeconds(Math.max(1, Math.min(seconds, 600)));
        } catch (NumberFormatException _nfe) {
            return Duration.ofSeconds(60);
        }
    }
}
