package com.mbs.hub.core.pipeline.dto;

import com.mbs.hub.core.pipeline.PipelineState;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Pipeline response payload — 02 API §5.5 / AC-005.1. */
public record PipelineView(
        UUID id,
        String name,
        String objective,
        LocalDate targetDeadline,
        BigDecimal totalEstimatedMd,
        PipelineState state,
        List<SkillRef> requiredSkills,
        UUID createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public record SkillRef(String name, String l1, String l2) {}
}
