package com.mbs.hub.sync.dedup;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(schema = "jira", name = "jira_event_dedup")
@Getter @Setter @NoArgsConstructor
public class JiraEventDedup {
    @EmbeddedId
    private JiraEventDedupKey key;

    @Column(name = "received_at", nullable = false)
    private OffsetDateTime receivedAt;

    @PrePersist void stamp() { if (receivedAt == null) receivedAt = OffsetDateTime.now(); }
}
