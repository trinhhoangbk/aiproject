package com.mbs.hub.sync.projection;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IssueProjectionRepository extends JpaRepository<IssueProjection, String> {

    /** Workload view join: active issues assigned to a given Jira account. */
    List<IssueProjection> findByAssigneeAccountIdAndStatusCategoryNotAndAllowListOkTrue(
            String assigneeAccountId, String statusCategoryNot);

    /**
     * Native UPSERT with monotonic guard — overwrite only when incoming event is newer.
     * Returns number of rows affected (0 means the event was stale).
     */
    @Modifying
    @Query(value = """
        INSERT INTO jira.issue_projection
            (issue_key, project_key, assignee_account_id, status, status_category,
             resolution, discarded, priority, labels, fix_version, fix_version_release_date,
             due_date, original_estimate_h, remaining_estimate_h, jira_updated_at,
             last_seen_at, allow_list_ok)
        VALUES
            (:issueKey, :projectKey, :assigneeAccountId, :status, :statusCategory,
             :resolution, :discarded, :priority, CAST(:labels AS text[]), :fixVersion, :fixVersionReleaseDate,
             :dueDate, :originalEstimateH, :remainingEstimateH, :jiraUpdatedAt,
             now(), :allowListOk)
        ON CONFLICT (issue_key) DO UPDATE SET
            project_key              = EXCLUDED.project_key,
            assignee_account_id      = EXCLUDED.assignee_account_id,
            status                   = EXCLUDED.status,
            status_category          = EXCLUDED.status_category,
            resolution               = EXCLUDED.resolution,
            discarded                = EXCLUDED.discarded,
            priority                 = EXCLUDED.priority,
            labels                   = EXCLUDED.labels,
            fix_version              = EXCLUDED.fix_version,
            fix_version_release_date = EXCLUDED.fix_version_release_date,
            due_date                 = EXCLUDED.due_date,
            original_estimate_h      = EXCLUDED.original_estimate_h,
            remaining_estimate_h     = EXCLUDED.remaining_estimate_h,
            jira_updated_at          = EXCLUDED.jira_updated_at,
            last_seen_at             = now()
        WHERE EXCLUDED.jira_updated_at >= jira.issue_projection.jira_updated_at
        """, nativeQuery = true)
    int upsertWithGuard(
            @Param("issueKey") String issueKey,
            @Param("projectKey") String projectKey,
            @Param("assigneeAccountId") String assigneeAccountId,
            @Param("status") String status,
            @Param("statusCategory") String statusCategory,
            @Param("resolution") String resolution,
            @Param("discarded") boolean discarded,
            @Param("priority") String priority,
            @Param("labels") String labelsCsv,     // caller passes "{a,b,c}" or "{}"
            @Param("fixVersion") String fixVersion,
            @Param("fixVersionReleaseDate") java.time.LocalDate fixVersionReleaseDate,
            @Param("dueDate") java.time.LocalDate dueDate,
            @Param("originalEstimateH") java.math.BigDecimal originalEstimateH,
            @Param("remainingEstimateH") java.math.BigDecimal remainingEstimateH,
            @Param("jiraUpdatedAt") OffsetDateTime jiraUpdatedAt,
            @Param("allowListOk") boolean allowListOk);
}
