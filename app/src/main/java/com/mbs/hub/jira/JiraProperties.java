package com.mbs.hub.jira;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Jira Cloud integration configuration — ADR-ARCH-001 (Jira Cloud, API v3) +
 * ADR-ARCH-010 (API Token service account). All values are env-driven; the
 * {@code writeDryRun} default is {@code true} per PLAN-COND-01.
 */
@ConfigurationProperties(prefix = "hub.jira")
public record JiraProperties(
        @NotBlank String baseUrl,
        @NotBlank String userEmail,
        @NotBlank String apiToken,
        @NotBlank String webhookSecret,
        boolean writeDryRun,
        String reconcilerCron,
        boolean reconcilerAdaptiveCadence
) {}
