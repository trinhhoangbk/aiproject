package com.mbs.hub.sync.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import com.mbs.hub.jira.dto.JiraUser;
import com.mbs.hub.jira.dto.JiraWorklogEntry;
import com.mbs.hub.jira.dto.JiraWorklogPage;
import com.mbs.hub.sync.projection.WorklogProjection;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/** TC-WP-01..02 — AC-002.2 / AC-004.2 input: Jira worklog page → projection rows. */
class WorklogProjectorTest {

    static final OffsetDateTime STARTED = OffsetDateTime.parse("2026-10-06T09:00+07:00");
    static final JiraUser TAM = new JiraUser("acc-tam", "Tam", null);

    @Test
    void TC_WP_01_onlyAttributableValidEntriesAreMapped() {
        JiraWorklogPage page = new JiraWorklogPage(0, 20, 4, List.of(
                new JiraWorklogEntry("10001", TAM, STARTED, 5400, null),       // valid, 1.5 h
                new JiraWorklogEntry("10002", null, STARTED, 3600, null),      // no author
                new JiraWorklogEntry("10003", TAM, STARTED, 0, null),          // zero duration
                new JiraWorklogEntry("abc", TAM, STARTED, 3600, null)));       // non-numeric id

        List<WorklogProjection> rows = WorklogProjector.toRows("KAN-27", "KAN", page, STARTED);

        assertThat(rows).hasSize(1);
        WorklogProjection w = rows.get(0);
        assertThat(w.getId()).isEqualTo(10001L);
        assertThat(w.getIssueKey()).isEqualTo("KAN-27");
        assertThat(w.getProjectKey()).isEqualTo("KAN");
        assertThat(w.getJiraAccountId()).isEqualTo("acc-tam");
        assertThat(w.getStartedAt()).isEqualTo(STARTED);
        assertThat(w.getDurationSeconds()).isEqualTo(5400);
        assertThat(w.getJiraUpdatedAt()).isEqualTo(STARTED);
    }

    @Test
    void TC_WP_02_missingPageYieldsNothing() {
        assertThat(WorklogProjector.toRows("KAN-27", "KAN", null, STARTED)).isEmpty();
    }
}
