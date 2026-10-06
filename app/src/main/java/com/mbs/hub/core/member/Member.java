package com.mbs.hub.core.member;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Hub-managed roster entry; mapped to a Jira user via jiraAccountId (DEC-006).
 */
@Entity
@Table(schema = "core", name = "member")
@Getter @Setter @NoArgsConstructor
public class Member {
    @Id @GeneratedValue
    private UUID id;

    @NotBlank
    @Column(name = "jira_account_id", nullable = false, unique = true)
    private String jiraAccountId;

    @NotBlank
    @Column(name = "display_name", nullable = false)
    private String displayName;

    @NotBlank @Email
    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "team_id")
    private UUID teamId;      // nullable; FK to core.team(id)

    @Column(name = "operations_lead", nullable = false)
    private boolean operationsLead = false;  // DEC-003 subrole

    @Column(nullable = false)
    private boolean active = true;

    /** BCrypt-12 hash (TD-COND-02). Not exposed by any DTO. */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "password_updated_at", nullable = false)
    private OffsetDateTime passwordUpdatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist void onInsert() {
        var now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        if (passwordUpdatedAt == null) passwordUpdatedAt = now;
        updatedAt = now;
    }
    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now(); }
}
