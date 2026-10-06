package com.mbs.hub.security;

import com.mbs.hub.core.member.Member;
import com.mbs.hub.core.member.MemberRepository;
import com.mbs.hub.security.dto.MeView;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * `/api/auth/login` and `/logout` are wired by SecurityConfig's formLogin;
 * only `/me` lives here. The SPA calls `/me` on load to decide whether to
 * show the login screen; unauthenticated callers get a 200 with
 * {@code {authenticated:false}} rather than a 401 so a cold browser load
 * never flashes an error.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final MemberRepository members;

    public AuthController(MemberRepository members) { this.members = members; }

    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal HubUserDetails principal) {
        if (principal == null) {
            return ResponseEntity.ok(Map.of("authenticated", false));
        }
        return members.findById(principal.memberId())
                .<ResponseEntity<?>>map(m -> ResponseEntity.ok(
                        new MeView(m.getId(), m.getEmail(), m.getRole(), m.isActive())))
                .orElseGet(() -> ResponseEntity.ok(Map.of("authenticated", false)));
    }
}
