package com.mbs.hub.core.member.dto;

import com.mbs.hub.core.member.Role;
import jakarta.validation.constraints.Email;
import java.util.UUID;

/** Partial update — null fields are left unchanged. Role change requires Admin (M8). */
public record MemberUpdateRequest(
        String displayName,
        @Email String email,
        Role role,
        UUID teamId,
        Boolean operationsLead,
        Boolean active
) {}
