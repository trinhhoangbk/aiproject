package com.mbs.hub.reporting;

import static com.mbs.hub.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mbs.hub.core.member.Member;
import com.mbs.hub.core.member.MemberRepository;
import com.mbs.hub.core.member.Role;
import com.mbs.hub.reporting.dto.DailyReportView;
import com.mbs.hub.sync.projection.IssueProjection;
import com.mbs.hub.sync.projection.IssueProjectionDailyRepository;
import com.mbs.hub.sync.projection.WorklogProjection;
import com.mbs.hub.sync.projection.WorklogProjectionRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** TC-DR-01..05 — AC-002.1/.2/.3, DEC-001, DEC-002, O-11. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DailyReportServiceTest {

    @Mock MemberRepository members;
    @Mock IssueProjectionDailyRepository issues;
    @Mock WorklogProjectionRepository worklogs;

    static final LocalDate D = LocalDate.of(2026, 10, 6);
    static final OffsetDateTime FROM = OffsetDateTime.parse("2026-10-06T00:00+07:00");
    static final OffsetDateTime TO   = OffsetDateTime.parse("2026-10-07T00:00+07:00");

    Member m = member("acc-1", Role.MEMBER);
    DailyReportService service;

    @BeforeEach
    void setUp() {
        when(members.findById(m.getId())).thenReturn(Optional.of(m));
        when(members.findAll()).thenReturn(List.of(m));
        IssueProjection done = issue("KAN-1", "0", null);
        done.setStatusCategory("done");
        done.setResolution("Done");
        IssueProjection wontFix = issue("KAN-2", "0", null);
        wontFix.setStatusCategory("done");
        wontFix.setResolution("Won't Fix");
        wontFix.setDiscarded(true);
        when(issues.findDoneWithinWindow(any(), any(), any())).thenReturn(List.of(done, wontFix));
        when(worklogs.findByJiraAccountIdAndStartedAtBetween(eq("acc-1"), any(), any())).thenReturn(List.of(
                wl(1, "acc-1", "KAN", 3600), wl(2, "acc-1", "KAN", 7200), wl(3, "acc-1", "OPS", 1800)));
        service = new DailyReportService(members, issues, worklogs, clock());
    }

    static WorklogProjection wl(long id, String account, String project, int seconds) {
        WorklogProjection w = new WorklogProjection();
        w.setId(id);
        w.setIssueKey(project + "-" + id);
        w.setProjectKey(project);
        w.setJiraAccountId(account);
        w.setStartedAt(FROM.plusHours(9));
        w.setDurationSeconds(seconds);
        w.setJiraUpdatedAt(FROM.plusHours(9));
        return w;
    }

    @Test
    void TC_DR_01_windowIsTheCalendarDayInAsiaSaigon() {
        service.compile(D, m.getId(), true);
        verify(issues).findDoneWithinWindow(eq(FROM), eq(TO), eq("acc-1"));
        verify(worklogs).findByJiraAccountIdAndStartedAtBetween(eq("acc-1"), eq(FROM), eq(TO));
    }

    @Test
    void TC_DR_02_discardedExcludedByDefault() {
        DailyReportView v = service.compile(D, m.getId(), true);
        assertThat(v.done()).extracting(DailyReportView.DoneRow::issueKey).containsExactly("KAN-1");
    }

    @Test
    void TC_DR_03_discardedShownAndFlaggedWhenNotExcluded() {
        DailyReportView v = service.compile(D, m.getId(), false);
        assertThat(v.done()).hasSize(2);
        assertThat(v.done()).filteredOn(DailyReportView.DoneRow::discarded)
                .extracting(DailyReportView.DoneRow::issueKey).containsExactly("KAN-2");
    }

    @Test
    void TC_DR_04_worklogTotalsPerProjectInHours() {
        DailyReportView v = service.compile(D, m.getId(), true);
        assertThat(v.worklogTotals()).extracting(DailyReportView.WorklogTotal::projectKey)
                .containsExactly("KAN", "OPS");
        assertThat(v.worklogTotals().get(0).hours()).isEqualByComparingTo("3.00");
        assertThat(v.worklogTotals().get(1).hours()).isEqualByComparingTo("0.50");
        assertThat(v.totalWorklogHours()).isEqualByComparingTo("3.50");
    }

    @Test
    void TC_DR_05_teamWideExcludesNonRosterWorklog() {
        when(worklogs.findByStartedAtBetween(any(), any())).thenReturn(List.of(
                wl(1, "acc-1", "KAN", 3600), wl(9, "acc-stranger", "KAN", 18_000)));

        DailyReportView v = service.compile(D, null, true);

        assertThat(v.totalWorklogHours()).isEqualByComparingTo("1.00");
    }
}
