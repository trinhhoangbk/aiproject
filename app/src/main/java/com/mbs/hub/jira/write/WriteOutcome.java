package com.mbs.hub.jira.write;

/**
 * Result of a Jira write call. {@code dryRun=true} means the request was
 * SHORT-CIRCUITED and NO HTTP call left the JVM (PLAN-COND-01). The caller
 * should audit a dry-run exactly like a real call so the record of intent survives.
 */
public record WriteOutcome(String method, String path, int statusCode, boolean dryRun, String message) {
    public static WriteOutcome dryRun(String method, String path) {
        return new WriteOutcome(method, path, 0, true, "DRY_RUN_SHORT_CIRCUIT");
    }
    public boolean ok() { return dryRun || (statusCode >= 200 && statusCode < 300); }
}
