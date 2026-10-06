package com.mbs.hub.jira.dto;

import java.util.List;

public record JiraSearchResponse(Integer startAt, Integer maxResults, Integer total,
                                 List<JiraIssue> issues) {}
