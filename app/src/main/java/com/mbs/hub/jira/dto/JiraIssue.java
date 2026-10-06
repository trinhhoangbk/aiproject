package com.mbs.hub.jira.dto;

public record JiraIssue(String id, String key, JiraFields fields) {

    public String projectKey() {
        int dash = key.indexOf('-');
        return dash > 0 ? key.substring(0, dash) : key;
    }
}
