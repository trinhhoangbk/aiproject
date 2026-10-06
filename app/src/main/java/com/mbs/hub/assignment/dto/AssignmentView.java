package com.mbs.hub.assignment.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Result of an assignment call — includes dry-run flag so the UI can label it correctly. */
public record AssignmentView(
        String issueKey,
        UUID targetMemberId,
        String targetJiraAccountId,
        String previousJiraAccountId,
        UUID requestedBy,
        OffsetDateTime at,
        boolean dryRun,
        String note
) {}
