package com.mbs.hub.core.member;

import com.mbs.hub.core.member.dto.MemberCreateRequest;
import com.mbs.hub.core.member.dto.MemberUpdateRequest;
import com.mbs.hub.core.member.dto.MemberView;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Admin roster controller — REQ-008 wiring surface, no security filter yet.
 * Security annotations are added in M8 (PLAN-029).
 */
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
@RestController
@RequestMapping("/api/roster")
public class MemberController {

    private final MemberRepository members;
    private final PasswordEncoder encoder;

    public MemberController(MemberRepository members, PasswordEncoder encoder) {
        this.members = members;
        this.encoder = encoder;
    }

    @GetMapping
    public List<MemberView> list() {
        return members.findAll().stream().map(MemberView::of).toList();
    }

    @GetMapping("/{id}")
    public MemberView get(@PathVariable UUID id) {
        return MemberView.of(members.findById(id).orElseThrow(
            () -> new NoSuchElementException("Member " + id + " not found")));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<MemberView> create(@Valid @RequestBody MemberCreateRequest req) {
        if (members.existsByEmail(req.email()))
            throw new IllegalStateException("A member with email " + req.email() + " already exists");
        if (members.existsByJiraAccountId(req.jiraAccountId()))
            throw new IllegalStateException("A member with jiraAccountId "
                + req.jiraAccountId() + " already exists");

        Member m = new Member();
        m.setJiraAccountId(req.jiraAccountId());
        m.setDisplayName(req.displayName());
        m.setEmail(req.email());
        m.setRole(req.role());
        m.setTeamId(req.teamId());
        m.setOperationsLead(Boolean.TRUE.equals(req.operationsLead()));
        m.setPasswordHash(encoder.encode(req.initialPassword()));
        m.setPasswordUpdatedAt(OffsetDateTime.now());
        m = members.save(m);
        return ResponseEntity.created(URI.create("/api/roster/" + m.getId()))
            .body(MemberView.of(m));
    }

    @PatchMapping("/{id}")
    @Transactional
    public MemberView update(@PathVariable UUID id, @Valid @RequestBody MemberUpdateRequest req) {
        Member m = members.findById(id).orElseThrow(
            () -> new NoSuchElementException("Member " + id + " not found"));
        if (req.displayName()    != null) m.setDisplayName(req.displayName());
        if (req.email()          != null) m.setEmail(req.email());
        if (req.role()           != null) m.setRole(req.role());
        if (req.teamId()         != null) m.setTeamId(req.teamId());
        if (req.operationsLead() != null) m.setOperationsLead(req.operationsLead());
        if (req.active()         != null) m.setActive(req.active());
        return MemberView.of(m);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        Member m = members.findById(id).orElseThrow(
            () -> new NoSuchElementException("Member " + id + " not found"));
        m.setActive(false);
        members.save(m);
        return ResponseEntity.noContent().build();
    }
}
