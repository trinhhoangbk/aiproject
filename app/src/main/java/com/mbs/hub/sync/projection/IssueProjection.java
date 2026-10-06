package com.mbs.hub.sync.projection;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * jira.issue_projection — the cache of Jira issue state. Jira is the SoR.
 * UPSERTs are guarded by {@code jira_updated_at} monotonicity in the consumer.
 */
@Entity
@Table(schema = "jira", name = "issue_projection")
@Getter @Setter @NoArgsConstructor
public class IssueProjection {
    @Id
    @Column(name = "issue_key", nullable = false)
    private String issueKey;

    @Column(name = "project_key", nullable = false)
    private String projectKey;

    @Column(name = "assignee_account_id")
    private String assigneeAccountId;

    @Column(nullable = false)
    private String status;

    @Column(name = "status_category", nullable = false)
    private String statusCategory;   // 'new' | 'indeterminate' | 'done'   DEC-001

    private String resolution;

    @Column(nullable = false)
    private boolean discarded;        // DEC-001: Won't Fix / Duplicate / Invalid

    private String priority;

    @Column(name = "labels", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] labels = new String[0];

    @Column(name = "fix_version")
    private String fixVersion;

    @Column(name = "fix_version_release_date")
    private LocalDate fixVersionReleaseDate;    // DEC-010 crit 1

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "original_estimate_h")
    private BigDecimal originalEstimateH;

    @Column(name = "remaining_estimate_h")
    private BigDecimal remainingEstimateH;

    @Column(name = "jira_updated_at", nullable = false)
    private OffsetDateTime jiraUpdatedAt;   // monotonic UPSERT guard

    @Column(name = "last_seen_at", nullable = false)
    private OffsetDateTime lastSeenAt;

    @Column(name = "allow_list_ok", nullable = false)
    private boolean allowListOk;
}
