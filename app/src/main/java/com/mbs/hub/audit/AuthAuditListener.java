package com.mbs.hub.audit;

import com.mbs.hub.security.HubUserDetails;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * PLAN-031 — subscribes to Spring Security auth events. Writes one audit row per
 * success/failure. Logout isn't event-based in Spring Security 6; it can be
 * audited later via a LogoutHandler bean (nice-to-have).
 */
@Component
public class AuthAuditListener {

    private final AuditService audit;

    public AuthAuditListener(AuditService audit) { this.audit = audit; }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent e) {
        UUID actor = memberIdOf(e.getAuthentication().getPrincipal());
        Map<String, Object> payload = new HashMap<>();
        payload.put("email", nameOf(e.getAuthentication().getPrincipal()));
        payload.put("remoteAuthorities", e.getAuthentication().getAuthorities().toString());
        audit.ok(actor, "AUTH_LOGIN_OK", "member", String.valueOf(actor), payload);
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent e) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("email", String.valueOf(e.getAuthentication().getPrincipal()));
        payload.put("exception", e.getException().getClass().getSimpleName());
        audit.failed(null, "AUTH_LOGIN_FAIL", "credential",
                String.valueOf(e.getAuthentication().getPrincipal()), payload);
    }

    private static UUID memberIdOf(Object principal) {
        if (principal instanceof HubUserDetails u) return u.getMemberId();
        return null;
    }

    private static String nameOf(Object principal) {
        if (principal instanceof UserDetails u) return u.getUsername();
        return String.valueOf(principal);
    }
}
