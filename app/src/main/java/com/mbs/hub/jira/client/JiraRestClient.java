package com.mbs.hub.jira.client;

import com.mbs.hub.jira.JiraProperties;
import com.mbs.hub.jira.dto.JiraIssue;
import com.mbs.hub.jira.dto.JiraSearchResponse;
import com.mbs.hub.jira.dto.JiraWorklogPage;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Jira Cloud REST API v3 client — ADR-ARCH-001 + ADR-ARCH-010.
 * Reads only; writes live in {@code jira.write} package (M7).
 *
 * <p>Basic auth header: Base64({@code email:api_token}). Secret never logged.</p>
 */
@Component
public class JiraRestClient {

    private static final Logger log = LoggerFactory.getLogger(JiraRestClient.class);

    private final RestClient http;
    private final JiraProperties props;

    public JiraRestClient(JiraProperties props,
                          org.springframework.web.client.RestClient.Builder builder) {
        this.props = props;
        String auth = props.userEmail() + ":" + props.apiToken();
        String basic = "Basic " + Base64.getEncoder()
                .encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        this.http = builder
                .baseUrl(props.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, basic)
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .build();
    }

    /** JQL search for an allow-listed project; see 04 Integration §4.1. */
    public JiraSearchResponse search(String jql, String fields, int startAt, int maxResults) {
        try {
            return http.get()
                    .uri(uri -> uri.path("/rest/api/3/search")
                            .queryParam("jql", jql)
                            .queryParam("fields", fields)
                            .queryParam("startAt", startAt)
                            .queryParam("maxResults", maxResults)
                            .build())
                    .retrieve()
                    .body(JiraSearchResponse.class);
        } catch (RestClientResponseException ex) {
            throw translate(ex, "GET /rest/api/3/search");
        }
    }

    public JiraIssue issue(String issueKey) {
        try {
            return http.get()
                    .uri("/rest/api/3/issue/{key}", issueKey)
                    .retrieve()
                    .body(JiraIssue.class);
        } catch (RestClientResponseException ex) {
            throw translate(ex, "GET /rest/api/3/issue/" + issueKey);
        }
    }

    public JiraWorklogPage worklog(String issueKey, int startAt, int maxResults) {
        try {
            return http.get()
                    .uri(uri -> uri.path("/rest/api/3/issue/{key}/worklog")
                            .queryParam("startAt", startAt)
                            .queryParam("maxResults", maxResults)
                            .build(issueKey))
                    .retrieve()
                    .body(JiraWorklogPage.class);
        } catch (RestClientResponseException ex) {
            throw translate(ex, "GET /rest/api/3/issue/" + issueKey + "/worklog");
        }
    }

    private RuntimeException translate(RestClientResponseException ex, String op) {
        int status = ex.getStatusCode().value();
        if (status == HttpStatus.TOO_MANY_REQUESTS.value()) {
            Duration retry = parseRetryAfter(ex);
            log.warn("Jira 429 on {} — Retry-After={}", op, retry);
            return new JiraRateLimitException(retry);
        }
        log.warn("Jira error {} on {}", status, op);
        return new JiraClientException(status, "Jira " + status + " on " + op);
    }

    private Duration parseRetryAfter(RestClientResponseException ex) {
        String h = ex.getResponseHeaders() == null ? null
                : ex.getResponseHeaders().getFirst(HttpHeaders.RETRY_AFTER);
        if (h == null) return Duration.ofSeconds(60);
        try { return Duration.ofSeconds(Long.parseLong(h.trim())); }
        catch (NumberFormatException ignored) { return Duration.ofSeconds(60); }
    }
}
