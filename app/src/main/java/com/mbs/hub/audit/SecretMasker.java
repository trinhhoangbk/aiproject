package com.mbs.hub.audit;

import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Masks values that MUST NEVER appear in audit payloads or log lines — 05 Security §SV-04.
 * Key list follows the keys actually used by the Hub + a few common hazards.
 *
 * <p>The masker runs on JSON payload STRINGS at the write point of AuditService and
 * at the logback layout for all app-level logs (see PLAN-034 glue).</p>
 */
@Component
public class SecretMasker {

    static final Set<String> MASKED_KEYS = Set.of(
            "password", "pwd",
            "api_token", "apiToken", "jira_api_token", "jiraApiToken",
            "webhook_secret", "webhookSecret", "jira_webhook_secret", "jiraWebhookSecret",
            "authorization",
            "cookie", "set-cookie",
            "x-atlassian-token"
    );

    /** Matches a JSON key:value pair for any of the masked keys. */
    private static final Pattern JSON_KV = Pattern.compile(
            "(?i)\"(" + String.join("|", MASKED_KEYS.stream().map(Pattern::quote).toList()) + ")\"\\s*:\\s*\"([^\"\\\\]|\\\\.)*\"");

    /** Matches `Authorization: Basic …` / `Bearer …` in header-shaped log strings. */
    private static final Pattern BEARER = Pattern.compile(
            "(?i)(Authorization\\s*:\\s*)(Basic|Bearer)\\s+\\S+");

    /** Mask a JSON or header-ish string. Returns the input unchanged when nothing matches. */
    public String mask(String in) {
        if (in == null || in.isEmpty()) return in;
        String out = JSON_KV.matcher(in).replaceAll(m -> "\"" + m.group(1) + "\":\"***\"");
        out = BEARER.matcher(out).replaceAll("$1$2 ***");
        return out;
    }
}
