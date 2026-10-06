package com.mbs.hub.assignment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request to assign a Jira issue to a Hub member — AC-006.4..6 / DEC-008 /
 * 02 API §5.7.
 *
 * <p>{@code reason} is required because it is printed into the Jira ADF
 * comment that accompanies the assignee change (DEC-008).</p>
 */
public record AssignmentRequest(
        @NotBlank String issueKey,
        @NotNull UUID targetMemberId,
        @NotBlank String reason,
        UUID pipelineId        // optional — links the assignment back to a pipeline project
) {}
