package com.mbs.hub.jira.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Jira REST v3 "fields" sub-object — only the fields the Hub reads
 * (05 Context → data-ownership table).
 */
public record JiraFields(
        String summary,
        JiraStatus status,
        JiraResolution resolution,
        JiraUser assignee,
        JiraPriority priority,
        List<String> labels,
        List<JiraFixVersion> fixVersions,
        @JsonProperty("duedate")              LocalDate dueDate,
        @JsonProperty("timeoriginalestimate") Integer originalEstimateSeconds,
        @JsonProperty("timeestimate")         Integer remainingEstimateSeconds,
        OffsetDateTime updated,
        JiraWorklogPage worklog
) {}
