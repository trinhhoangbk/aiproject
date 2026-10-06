package com.mbs.hub.reporting;

import com.mbs.hub.core.member.Member;
import com.mbs.hub.core.member.MemberRepository;
import com.mbs.hub.reporting.dto.DailyReportView;
import com.mbs.hub.sync.projection.IssueProjection;
import com.mbs.hub.sync.projection.IssueProjectionDailyRepository;
import com.mbs.hub.sync.projection.WorklogProjection;
import com.mbs.hub.sync.projection.WorklogProjectionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Daily report composer — AC-002.1..4 / DEC-001 / DEC-002.
 *
 * The window boundaries are always resolved in Asia/Saigon via the injected Clock
 * so that an operator running the Hub elsewhere does not shift the window.
 */
@Service
public class DailyReportService {

    private final MemberRepository members;
    private final IssueProjectionDailyRepository issues;
    private final WorklogProjectionRepository worklogs;
    private final Clock clock;

    public DailyReportService(MemberRepository members,
                              IssueProjectionDailyRepository issues,
                              WorklogProjectionRepository worklogs,
                              Clock clock) {
        this.members = members;
        this.issues = issues;
        this.worklogs = worklogs;
        this.clock = clock;
    }

    public DailyReportView compile(LocalDate date, UUID memberId, boolean excludeDiscarded) {
        LocalDate day = date != null ? date : LocalDate.now(clock);
        ZoneId zone = clock.getZone();   // Asia/Saigon
        OffsetDateTime from = day.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime toExclusive = day.plusDays(1).atStartOfDay(zone).toOffsetDateTime();

        String accountId;
        if (memberId != null) {
            Member m = members.findById(memberId)
                    .orElseThrow(() -> new IllegalArgumentException("member not found: " + memberId));
            accountId = m.getJiraAccountId();
        } else {
            accountId = null;      // ADMIN/MANAGER view: all members in roster
        }

        // Done list.
        List<IssueProjection> doneIssues = issues.findDoneWithinWindow(from, toExclusive, accountId);
        List<DailyReportView.DoneRow> done = doneIssues.stream()
                .filter(i -> !excludeDiscarded || !i.isDiscarded())
                .map(i -> new DailyReportView.DoneRow(
                        i.getIssueKey(),
                        i.getProjectKey(),
                        i.getResolution(),
                        i.isDiscarded()))
                .toList();

        // Worklog totals.
        List<WorklogProjection> ws = accountId != null
                ? worklogs.findByJiraAccountIdAndStartedAtBetween(accountId, from, toExclusive)
                : worklogs.findByStartedAtBetween(from, toExclusive);

        Map<String, Long> byProject = ws.stream()
                .filter(WorklogProjection::isInRoster)
                .collect(Collectors.groupingBy(
                        WorklogProjection::getProjectKey,
                        Collectors.summingLong(WorklogProjection::getDurationSeconds)));

        List<DailyReportView.WorklogTotal> totals = byProject.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new DailyReportView.WorklogTotal(e.getKey(), secondsToHours(e.getValue())))
                .toList();

        BigDecimal grand = totals.stream()
                .map(DailyReportView.WorklogTotal::hours)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return new DailyReportView(day, memberId, done, totals, grand, excludeDiscarded);
    }

    private static BigDecimal secondsToHours(long seconds) {
        return BigDecimal.valueOf(seconds)
                .divide(BigDecimal.valueOf(3600), 2, RoundingMode.HALF_UP);
    }
}
