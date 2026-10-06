package com.mbs.hub.core.member.dto;

import com.mbs.hub.core.member.Member;
import com.mbs.hub.core.member.Role;
import java.util.UUID;

/** Member projection — never exposes the password hash. */
public record MemberView(
        UUID id, String jiraAccountId, String displayName, String email,
        Role role, UUID teamId, boolean operationsLead, boolean active
) {
    public static MemberView of(Member m) {
        return new MemberView(m.getId(), m.getJiraAccountId(), m.getDisplayName(),
            m.getEmail(), m.getRole(), m.getTeamId(), m.isOperationsLead(), m.isActive());
    }
}
