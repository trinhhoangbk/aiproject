package com.mbs.hub.sync.projection;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorklogProjectionRepository extends JpaRepository<WorklogProjection, Long> {

    void deleteByIssueKey(String issueKey);

    /** Daily report: worklog entries by Jira account within [from, to]. */
    List<WorklogProjection> findByJiraAccountIdAndStartedAtBetween(
            String jiraAccountId, OffsetDateTime from, OffsetDateTime to);

    /** All worklog within window (ADMIN/MANAGER daily report with memberId omitted). */
    List<WorklogProjection> findByStartedAtBetween(OffsetDateTime from, OffsetDateTime to);

    /** ETA α: last-10-WD worklog of this Jira account, grouped by project. */
    @Query(value = """
        SELECT w.project_key AS projectKey,
               CAST(SUM(w.duration_seconds) AS bigint) AS seconds
        FROM jira.worklog_projection w
        WHERE w.jira_account_id = :accountId
          AND w.started_at >= :from
          AND w.started_at <  :toExclusive
        GROUP BY w.project_key
        """, nativeQuery = true)
    List<Object[]> sumByProjectBetween(
            @Param("accountId") String accountId,
            @Param("from") OffsetDateTime from,
            @Param("toExclusive") OffsetDateTime toExclusive);
}
