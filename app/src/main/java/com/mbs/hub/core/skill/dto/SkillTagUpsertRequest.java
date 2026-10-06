package com.mbs.hub.core.skill.dto;

import jakarta.validation.constraints.NotBlank;

public record SkillTagUpsertRequest(
        @NotBlank String l1,
        String l2,
        Boolean active
) {}
