package com.mbs.hub.jira.dto;

import java.util.List;

public record JiraWorklogPage(Integer startAt, Integer maxResults, Integer total,
                              List<JiraWorklogEntry> worklogs) {}
