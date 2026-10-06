package com.mbs.hub.core.allowlist;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** DEC-005: a Jira project key the Hub is authorised to observe. */
@Entity
@Table(schema = "core", name = "allow_list")
@Getter @Setter @NoArgsConstructor
public class AllowListEntry {
    @Id
    @Column(name = "project_key", nullable = false)
    private String projectKey;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "added_by")
    private UUID addedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist void onInsert() {
        var now = OffsetDateTime.now();
        createdAt = now; updatedAt = now;
    }
    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now(); }
}
