package com.mbs.hub.sync.projection;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(schema = "jira", name = "worklog_projection")
@Getter @Setter @NoArgsConstructor
public class WorklogProjection {
    @Id
    private Long id;

    @Column(name = "issue_key", nullable = false)
    private String issueKey;

    @Column(name = "project_key", nullable = false)
    private String projectKey;

    @Column(name = "jira_account_id", nullable = false)
    private String jiraAccountId;

    @Column(name = "in_roster", nullable = false)
    private boolean inRoster;      // O-11 decision — non-roster worklog projected but excluded from team-MD

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    @Column(name = "jira_updated_at", nullable = false)
    private OffsetDateTime jiraUpdatedAt;
}
