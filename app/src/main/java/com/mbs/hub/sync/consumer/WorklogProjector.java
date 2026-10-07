package com.mbs.hub.sync.consumer;

import com.mbs.hub.core.member.MemberRepository;
import com.mbs.hub.jira.dto.JiraIssue;
import com.mbs.hub.jira.dto.JiraWorklogEntry;
import com.mbs.hub.jira.dto.JiraWorklogPage;
import com.mbs.hub.sync.projection.WorklogProjection;
import com.mbs.hub.sync.projection.WorklogProjectionRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * PLAN-022 / PLAN-023 — projects the worklog page embedded in a Jira issue payload
 * into {@code jira.worklog_projection} (AC-002.2 daily worklog totals, AC-004.2 ETA α).
 *
 * <p>Jira fires {@code issue_updated} when work is logged (time spent / remaining
 * change), and the issue payload carries {@code fields.worklog} with up to 20 entries.
 * The projector replaces the issue's worklog rows with that page. Pages truncated by
 * Jira ({@code total > worklogs.size()}) are counted in a warning; the hourly
 * reconciler re-reads the issue, but entries beyond the first page remain a known gap.</p>
 *
 * <p>Standalone {@code worklog_*} webhook events carry only {@code issueId} (no key)
 * and are ignored here; the accompanying {@code issue_updated} event covers them.</p>
 */
@Component
public class WorklogProjector {

    private static final Logger log = LoggerFactory.getLogger(WorklogProjector.class);

    private final WorklogProjectionRepository worklogs;
    private final MemberRepository members;

    public WorklogProjector(WorklogProjectionRepository worklogs, MemberRepository members) {
        this.worklogs = worklogs;
        this.members = members;
    }

    /** Replace the stored worklog rows of {@code issue}; no-op when the payload has no worklog page. */
    public int project(JiraIssue issue) {
        if (issue == null || issue.fields() == null || issue.fields().worklog() == null) return -1;
        JiraWorklogPage page = issue.fields().worklog();
        List<WorklogProjection> rows = toRows(issue.key(), issue.projectKey(), page,
                issue.fields().updated());
        for (WorklogProjection r : rows) r.setInRoster(members.existsByJiraAccountId(r.getJiraAccountId()));
        worklogs.deleteByIssueKey(issue.key());
        worklogs.saveAll(rows);
        if (page.total() != null && page.worklogs() != null && page.total() > page.worklogs().size()) {
            log.warn("Worklog page truncated for {}: {} of {} entries projected",
                    issue.key(), page.worklogs().size(), page.total());
        }
        return rows.size();
    }

    /**
     * Pure mapping (no I/O) — unit-testable. Skips entries Jira cannot attribute
     * (no author / no start) or that the schema rejects (duration ≤ 0, non-numeric id).
     * {@code inRoster} is left false; {@link #project} fills it.
     */
    static List<WorklogProjection> toRows(String issueKey, String projectKey,
                                          JiraWorklogPage page, OffsetDateTime issueUpdated) {
        List<WorklogProjection> out = new ArrayList<>();
        if (page == null || page.worklogs() == null) return out;
        for (JiraWorklogEntry e : page.worklogs()) {
            if (e == null || e.author() == null || e.author().accountId() == null) continue;
            if (e.started() == null || e.timeSpentSeconds() == null || e.timeSpentSeconds() <= 0) continue;
            long id;
            try { id = Long.parseLong(e.id()); } catch (NumberFormatException | NullPointerException ex) { continue; }
            WorklogProjection w = new WorklogProjection();
            w.setId(id);
            w.setIssueKey(issueKey);
            w.setProjectKey(projectKey);
            w.setJiraAccountId(e.author().accountId());
            w.setInRoster(false);
            w.setStartedAt(e.started());
            w.setDurationSeconds(e.timeSpentSeconds());
            OffsetDateTime upd = e.updated() != null ? e.updated() : issueUpdated;
            w.setJiraUpdatedAt(upd != null ? upd : e.started());
            out.add(w);
        }
        return out;
    }
}
