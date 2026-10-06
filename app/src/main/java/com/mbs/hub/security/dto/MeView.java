package com.mbs.hub.security.dto;

import com.mbs.hub.core.member.Role;
import java.util.UUID;

public record MeView(UUID memberId, String email, Role role, boolean active) {}
