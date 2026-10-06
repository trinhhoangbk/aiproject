package com.mbs.hub.eta;

import com.mbs.hub.core.capacity.CapacityResolver;
import com.mbs.hub.core.capacity.EffectiveCapacity;
import com.mbs.hub.core.member.Member;
import com.mbs.hub.core.member.MemberRepository;
import com.mbs.hub.eta.dto.EtaView;
import com.mbs.hub.mv.calc.WorkingDayCalculator;
import com.mbs.hub.sync.projection.IssueProjection;
import com.mbs.hub.sync.projection.IssueProjectionRepository;
import com.mbs.hub.sync.projection.WorklogProjectionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * ETA forecasting — F-03 / AC-004.1-2.
 *
 * <p>Per member × project P:
 * <pre>
 *   remainingHours_P = Σ remaining_estimate_h over active, allow-listed, assigned-to-member issues in P
 *   totalSeconds     = Σ duration_seconds of in-roster worklog over last 10 WD (any project)
 *   seconds_P        = Σ duration_seconds of in-roster worklog over last 10 WD on P
 *   α_P              = seconds_P / totalSeconds        (0..1, 3 d.p. HALF_UP)
 *   capacity_P       = α_P × dailyHours                (hours/WD actually spent on P)
 *   RWD_P            = ceil(remainingHours_P / capacity_P)
 *   ETA_date_P       = addWorkingDays(today, RWD_P)    via B-RULE-01
 * </pre>
 *
 * <p>When totalSeconds == 0 OR seconds_P == 0, α_P = 0 → ETA cannot be forecast
 * from observed data; the view returns {@code etaDate=null} with a {@code note}
 * instead of fabricating a number.</p>
 */
@Service
public class EtaService {

    /** F-03 default. Documented in TD §3. */
    static final int DEFAULT_WINDOW_WD = 10;

    /** How far back (calendar days) to scan to collect 10 WD of worklog.
     *  10 WD + weekends + a holiday or two → scan the last 21 calendar days.
     *  Overshooting is harmless; the WorkingDayCalculator trims. */
    private static final int CALENDAR_LOOKBACK_DAYS = 21;

    private final MemberRepository members;
    private final IssueProjectionRepository issues;
    private final WorklogProjectionRepository worklogs;
    private final CapacityResolver capacity;
    private final WorkingDayCalculator wdc;
    private final Clock clock;

    public EtaService(MemberRepository members,
                      IssueProjectionRepository issues,
                      WorklogProjectionRepository worklogs,
                      CapacityResolver capacity,
                      WorkingDayCalculator wdc,
                      Clock clock) {
        this.members = members;
        this.issues = issues;
        this.worklogs = worklogs;
        this.capacity = capacity;
        this.wdc = wdc;
        this.clock = clock;
    }

    public EtaView forecast(UUID memberId) {
        Member m = members.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("member not found: " + memberId));
        LocalDate today = LocalDate.now(clock);
        EffectiveCapacity cap = capacity.resolveOn(memberId, today);
        BigDecimal dailyHours = cap.dailyHours();

        // 1. Collect last-10-WD worklog totals per project.
        ZoneId zone = clock.getZone();
        OffsetDateTime toExclusive = today.plusDays(1).atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime scanFrom = today.minusDays(CALENDAR_LOOKBACK_DAYS)
                .atStartOfDay(zone).toOffsetDateTime();

        List<Object[]> sums = worklogs.sumByProjectBetween(m.getJiraAccountId(), scanFrom, toExclusive);
        Map<String, Long> byProject = sums.stream()
                .collect(Collectors.toMap(r -> (String) r[0], r -> ((Number) r[1]).longValue()));
        long totalSeconds = byProject.values().stream().mapToLong(Long::longValue).sum();

        // 2. Active issues of the member — group remainingEstimate by projectKey.
        Map<String, BigDecimal> remainingByProject = issues
                .findByAssigneeAccountIdAndStatusCategoryNotAndAllowListOkTrue(
                        m.getJiraAccountId(), "done")
                .stream()
                .filter(i -> i.getRemainingEstimateH() != null
                           && i.getRemainingEstimateH().signum() > 0)
                .collect(Collectors.groupingBy(
                        IssueProjection::getProjectKey,
                        Collectors.mapping(IssueProjection::getRemainingEstimateH,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));

        // 3. For every project with any remaining work OR any observed α, produce a row.
        Set<String> allProjects = new TreeSet<>();
        allProjects.addAll(remainingByProject.keySet());
        allProjects.addAll(byProject.keySet());

        List<EtaView.ProjectEta> rows = new ArrayList<>();
        for (String projectKey : allProjects) {
            BigDecimal remainingH = remainingByProject.getOrDefault(projectKey, BigDecimal.ZERO)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal remainingMd = (dailyHours.signum() > 0)
                    ? remainingH.divide(dailyHours, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            BigDecimal alpha = (totalSeconds > 0)
                    ? BigDecimal.valueOf(byProject.getOrDefault(projectKey, 0L))
                          .divide(BigDecimal.valueOf(totalSeconds), 3, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            Integer rwd = null;
            LocalDate etaDate = null;
            String note = null;

            if (alpha.signum() == 0 || remainingH.signum() == 0) {
                note = alpha.signum() == 0
                        ? "no_worklog_on_project_last_" + DEFAULT_WINDOW_WD + "_wd"
                        : "no_remaining_estimate";
            } else {
                BigDecimal capacityHoursPerWd = alpha.multiply(dailyHours);
                // ceil(remaining / capacity)
                BigDecimal rwdDec = remainingH.divide(capacityHoursPerWd, 10, RoundingMode.HALF_UP);
                rwd = rwdDec.setScale(0, RoundingMode.CEILING).intValueExact();
                etaDate = addWorkingDays(today, rwd);
            }

            rows.add(new EtaView.ProjectEta(projectKey, remainingH, remainingMd, alpha, rwd, etaDate, note));
        }

        return new EtaView(memberId, DEFAULT_WINDOW_WD, today, rows);
    }

    /** Walks {@code days} working days forward from {@code anchor}, inclusive of the next WD. */
    private LocalDate addWorkingDays(LocalDate anchor, int days) {
        LocalDate d = anchor;
        int remaining = days;
        // Hard cap at 2 years ahead to guard pathological inputs.
        LocalDate hardStop = anchor.plusYears(2);
        while (remaining > 0 && d.isBefore(hardStop)) {
            d = d.plusDays(1);
            if (wdc.isWorkingDay(d)) remaining--;
        }
        return d;
    }
}
