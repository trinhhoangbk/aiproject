package com.mbs.hub.security;

import com.mbs.hub.core.member.Member;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * UserDetails carrying the Hub member id so {@code @PreAuthorize("principal.memberId")}
 * SpEL expressions can enforce MEMBER self-only access.
 */
public final class HubUserDetails implements UserDetails {

    private final UUID memberId;
    private final String email;
    private final String passwordHash;
    private final boolean active;
    private final List<GrantedAuthority> authorities;

    public HubUserDetails(Member m) {
        this.memberId     = m.getId();
        this.email        = m.getEmail();
        this.passwordHash = m.getPasswordHash();
        this.active       = m.isActive();
        this.authorities  = List.of(new SimpleGrantedAuthority("ROLE_" + m.getRole().name()));
    }

    public UUID memberId() { return memberId; }
    public UUID getMemberId() { return memberId; }   // Spring SpEL property

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return active; }
}
