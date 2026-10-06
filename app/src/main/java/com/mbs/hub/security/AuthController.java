package com.mbs.hub.security;

import com.mbs.hub.core.member.Member;
import com.mbs.hub.core.member.MemberRepository;
import com.mbs.hub.security.dto.MeView;
import java.util.NoSuchElementException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** `/api/auth/login` and `/logout` are wired by SecurityConfig's formLogin; only /me lives here. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final MemberRepository members;

    public AuthController(MemberRepository members) { this.members = members; }

    @GetMapping("/me")
    public ResponseEntity<MeView> me(@AuthenticationPrincipal HubUserDetails principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        Member m = members.findById(principal.memberId())
                .orElseThrow(() -> new NoSuchElementException("Member not found"));
        return ResponseEntity.ok(new MeView(m.getId(), m.getEmail(), m.getRole(), m.isActive()));
    }
}
