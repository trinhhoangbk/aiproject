package com.mbs.hub.core.allowlist.dto;

import jakarta.validation.constraints.NotBlank;

public record AllowListUpsertRequest(
        @NotBlank String projectKey,
        Boolean enabled
) {}
