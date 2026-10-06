package com.mbs.hub.core.member.dto;

import com.mbs.hub.core.member.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record MemberCreateRequest(
        @NotBlank @Size(max = 200) String jiraAccountId,
        @NotBlank @Size(max = 200) String displayName,
        @NotBlank @Email            String email,
        @NotNull                    Role role,
                                    UUID teamId,
                                    Boolean operationsLead,
        @NotBlank @Size(min = 10)   String initialPassword
) {}
