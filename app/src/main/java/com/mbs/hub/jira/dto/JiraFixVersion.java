package com.mbs.hub.jira.dto;

import java.time.LocalDate;

/**
 * DEC-010 criterion 1: when {@code releaseDate} is set, the issue is Hard-Deadlined.
 */
public record JiraFixVersion(String id, String name, Boolean released, LocalDate releaseDate) {}
