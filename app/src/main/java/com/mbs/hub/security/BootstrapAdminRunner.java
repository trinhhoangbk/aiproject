package com.mbs.hub.security;

import com.mbs.hub.core.member.Member;
import com.mbs.hub.core.member.MemberRepository;
import com.mbs.hub.core.member.Role;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * PLAN-030 / TD-012 — on first boot of a fresh database, insert one Admin row
 * with a one-time password printed to stdout. Operator captures the password,
 * logs in, changes it, and clears their terminal scrollback (PLAN-COND-04;
 * documented in deploy/RELEASE.md).
 *
 * <p>Idempotent: skips entirely if any member row already exists.</p>
 */
@Component
public class BootstrapAdminRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminRunner.class);

    private final MemberRepository members;
    private final PasswordEncoder encoder;
    private final String bootstrapEmail;

    public BootstrapAdminRunner(MemberRepository members, PasswordEncoder encoder,
                                @Value("${hub.bootstrap.admin-email:}") String bootstrapEmail) {
        this.members = members;
        this.encoder = encoder;
        this.bootstrapEmail = bootstrapEmail;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (bootstrapEmail == null || bootstrapEmail.isBlank()) {
            log.info("Bootstrap admin disabled (HUB_BOOTSTRAP_ADMIN_EMAIL is not set).");
            return;
        }
        if (members.count() > 0) {
            log.info("Bootstrap admin skipped — {} member rows already present.", members.count());
            return;
        }
        String password = generateOneTimePassword();
        Member m = new Member();
        m.setEmail(bootstrapEmail);
        m.setDisplayName("Bootstrap Admin");
        m.setJiraAccountId("bootstrap-admin");   // placeholder; operator changes via /api/roster
        m.setRole(Role.ADMIN);
        m.setActive(true);
        m.setOperationsLead(false);
        m.setPasswordHash(encoder.encode(password));
        m.setPasswordUpdatedAt(OffsetDateTime.now());
        members.save(m);

        // Deliberately shouty — operator must capture + clear scrollback (PLAN-COND-04)
        log.warn("================================================================================");
        log.warn("BOOTSTRAP ADMIN ONE-TIME PASSWORD — CAPTURE THIS NOW, CHANGE IT, CLEAR SCROLLBACK");
        log.warn("  email:    {}", bootstrapEmail);
        log.warn("  password: {}", password);
        log.warn("================================================================================");
    }

    private static String generateOneTimePassword() {
        byte[] b = new byte[18];
        new SecureRandom().nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }
}
