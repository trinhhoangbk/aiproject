package com.mbs.hub.jira.dto;

import java.time.OffsetDateTime;

public record JiraWorklogEntry(
        String id,
        JiraUser author,
        OffsetDateTime started,
        Integer timeSpentSeconds,
        OffsetDateTime updated
) {}
