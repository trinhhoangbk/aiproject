package com.mbs.hub.jira.dto;

/**
 * Jira REST v3 {@code statusCategory} partial DTO. DEC-001 uses {@code key}:
 * {@code "done"} marks the issue as finished regardless of workflow name.
 */
public record JiraStatusCategory(String key, String name) {}
