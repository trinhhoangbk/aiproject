package com.mbs.hub.jira.client;

import java.time.Duration;

/** Thrown by the Jira client when Jira returns HTTP 429. */
public class JiraRateLimitException extends RuntimeException {
    private final Duration retryAfter;
    public JiraRateLimitException(Duration retryAfter) {
        super("Jira responded 429 (rate-limited); Retry-After=" + retryAfter);
        this.retryAfter = retryAfter;
    }
    public Duration retryAfter() { return retryAfter; }
}
