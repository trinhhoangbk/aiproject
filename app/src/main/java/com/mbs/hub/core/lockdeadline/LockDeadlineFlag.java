package com.mbs.hub.core.lockdeadline;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** DEC-010 criterion 3 — Manager-set "Lock Deadline" flag per Jira issue. */
@Entity
@Table(schema = "core", name = "lock_deadline_flag")
@Getter @Setter @NoArgsConstructor
public class LockDeadlineFlag {
    @Id
    @Column(name = "issue_key", nullable = false)
    private String issueKey;

    @Column(name = "locked_by", nullable = false)
    private UUID lockedBy;

    private String reason;

    @Column(name = "locked_at", nullable = false)
    private OffsetDateTime lockedAt;

    @PrePersist void onInsert() { if (lockedAt == null) lockedAt = OffsetDateTime.now(); }
}
