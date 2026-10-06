package com.mbs.hub.sync.projection;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Thin query-only repo for the daily report. Not a JpaRepository so it stays read-only.
 *
 * <p>Done rows for the window: statusCategory='done' AND jira_updated_at falls in [from, toExclusive).
 * See 05-DEVELOPMENT/CYCLE_7 §1 — in the absence of a dedicated resolved_at column, the
 * monotonic jira_updated_at is used as the "closed-at" proxy. An issue reopened and
 * re-closed on the same day lands correctly; an issue closed earlier and only metadata-edited
 * today appears today — accepted known limitation (v1).</p>
 */
public interface IssueProjectionDailyRepository extends Repository<IssueProjection, String> {

    @Query(value = """
        SELECT i FROM IssueProjection i
         WHERE i.statusCategory = 'done'
           AND i.jiraUpdatedAt >= :from
           AND i.jiraUpdatedAt <  :toExclusive
           AND i.allowListOk  = true
           AND (:accountId IS NULL OR i.assigneeAccountId = :accountId)
         ORDER BY i.jiraUpdatedAt
        """)
    List<IssueProjection> findDoneWithinWindow(
            @Param("from") OffsetDateTime from,
            @Param("toExclusive") OffsetDateTime toExclusive,
            @Param("accountId") String accountId);
}
