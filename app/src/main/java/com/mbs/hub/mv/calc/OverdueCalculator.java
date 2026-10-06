package com.mbs.hub.mv.calc;

import com.mbs.hub.core.capacity.CapacityResolver;
import com.mbs.hub.core.capacity.EffectiveCapacity;
import com.mbs.hub.core.member.Member;
import com.mbs.hub.mv.MemberOverdueKey;
import com.mbs.hub.mv.MemberOverdueRow;
import com.mbs.hub.mv.OverdueBand;
import com.mbs.hub.mv.TprBand;
import com.mbs.hub.sync.projection.IssueProjection;
import com.mbs.hub.sync.projection.IssueProjectionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Computes {@link MemberOverdueRow}s for one member — one row per active issue:
 * overdue → severity band (F-01); not-yet-overdue → TPR / Early-Warning (DEC-009).
 *
 * <p>"Overdue" means {@code due_date < today} AND status is not done AND the
 * project is allow-list-OK. "Early-warning" means not overdue but RDC breaches
 * thresholds.</p>
 */
@Component
public class OverdueCalculator {

    private static final BigDecimal NO_ESTIMATE_PLACEHOLDER_HOURS = new BigDecimal("4");

    private final CapacityResolver capacity;
    private final WorkingDayCalculator workingDays;
    private final IssueProjectionRepository issues;
    private final Clock clock;

    public OverdueCalculator(CapacityResolver capacity,
                             WorkingDayCalculator workingDays,
                             IssueProjectionRepository issues,
                             Clock clock) {
        this.capacity = capacity;
        this.workingDays = workingDays;
        this.issues = issues;
        this.clock = clock;
    }

    public List<MemberOverdueRow> compute(Member member) {
        LocalDate today = LocalDate.now(clock);
        if (member.getJiraAccountId() == null) return List.of();
        EffectiveCapacity ec = capacity.resolveOn(member.getId(), today);
        BigDecimal dailyH = ec.dailyHours();

        List<IssueProjection> active = issues
                .findByAssigneeAccountIdAndStatusCategoryNotAndAllowListOkTrue(
                        member.getJiraAccountId(), "done");
        List<MemberOverdueRow> rows = new ArrayList<>();
        for (IssueProjection i : active) {
            LocalDate due = i.getDueDate();
            if (due == null) continue;
            MemberOverdueRow row = buildRow(member, i, due, today, dailyH);
            if (row != null) rows.add(row);
        }
        return rows;
    }

    private MemberOverdueRow buildRow(Member m, IssueProjection i, LocalDate due,
                                      LocalDate today, BigDecimal dailyH) {
        BigDecimal remainingH = i.getRemainingEstimateH();
        if (remainingH == null) remainingH = NO_ESTIMATE_PLACEHOLDER_HOURS;

        if (due.isBefore(today)) {
            // Overdue path — F-01 bands by calendar-day distance.
            int days = (int) (today.toEpochDay() - due.toEpochDay());
            OverdueBand band = OverdueBand.of(days);
            MemberOverdueRow row = new MemberOverdueRow();
            row.setKey(new MemberOverdueKey(i.getIssueKey(), m.getId()));
            row.setProjectKey(i.getProjectKey());
            row.setDueDate(due);
            row.setDaysOverdue(days);
            row.setOverdueBand(band.wire);
            row.setRemainingH(remainingH);
            row.setTpr(null);
            row.setTprBand(TprBand.NONE.wire);
            return row;
        }

        // Not overdue → compute TPR / Early-Warning (DEC-009)
        int rwd = workingDays.workingDays(today, due.plusDays(1));   // inclusive of due day
        BigDecimal rdc;
        if (rwd <= 0 || dailyH.signum() == 0) {
            // Deadline today or passed with 0 working days left → treat as Red (DEC-009 edge AC-003.5)
            rdc = new BigDecimal("9.999");
        } else {
            BigDecimal denom = dailyH.multiply(BigDecimal.valueOf(rwd));
            rdc = remainingH.divide(denom, 3, RoundingMode.HALF_UP);
        }
        TprBand band = TprBand.of(rdc);
        if (band == TprBand.NONE) return null;   // only warn when yellow/red

        MemberOverdueRow row = new MemberOverdueRow();
        row.setKey(new MemberOverdueKey(i.getIssueKey(), m.getId()));
        row.setProjectKey(i.getProjectKey());
        row.setDueDate(due);
        row.setDaysOverdue(0);
        row.setOverdueBand(OverdueBand.ONE_TO_3_DAYS.wire);   // placeholder; UI filters by days_overdue=0
        row.setRemainingH(remainingH);
        row.setTpr(rdc);
        row.setTprBand(band.wire);
        return row;
    }
}
