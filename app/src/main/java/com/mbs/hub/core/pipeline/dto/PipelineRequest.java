package com.mbs.hub.core.pipeline.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Request body for POST /api/pipeline + PUT /api/pipeline/{id} — AC-005.1 / AC-005.2. */
public record PipelineRequest(
        @NotBlank @Size(min = 1, max = 200) String name,
        @NotBlank String objective,
        @NotNull LocalDate targetDeadline,
        @NotNull @DecimalMin(value = "0.01") BigDecimal totalEstimatedMd,
        @NotNull @Size(min = 1) @Valid List<SkillRef> requiredSkills
) {
    /** From DEC-007 L1/L2 taxonomy; the service validates against core.skill_taxonomy. */
    public record SkillRef(
            @NotBlank String l1,
            String l2
    ) {
        public String safeL2() { return l2 == null ? "" : l2; }
    }
}
