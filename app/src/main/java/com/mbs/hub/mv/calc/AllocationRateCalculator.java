package com.mbs.hub.mv.calc;

import com.mbs.hub.core.capacity.CapacityResolver;
import com.mbs.hub.core.capacity.EffectiveCapacity;
import com.mbs.hub.core.member.Member;
import com.mbs.hub.mv.AllocationBand;
import com.mbs.hub.mv.AllocationRateKey;
import com.mbs.hub.mv.AllocationRateRow;
import com.mbs.hub.mv.Horizon;
import com.mbs.hub.sync.projection.IssueProjection;
import com.mbs.hub.sync.projection.IssueProjectionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Computes one {@link AllocationRateRow} per (member, horizon).
 *
 * <p>Standard MD / Committed MD / Available MD / AR semantics per 04 Domain §A
 * and DEC-011 band thresholds.</p>
 *
 * <p>No-estimate issues are treated as 0.5 MD = 4 h per B-RULE-02.</p>
 */
@Component
public class AllocationRateCalculator {

    private static final BigDecimal NO_ESTIMATE_PLACEHOLDER_HOURS = new BigDecimal("4");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final CapacityResolver capacity;
    private final WorkingDayCalculator workingDays;
    private final IssueProjectionRepository issues;
    private final Clock clock;

    public AllocationRateCalculator(CapacityResolver capacity,
                                    WorkingDayCalculator workingDays,
                                    IssueProjectionRepository issues,
                                    Clock clock) {
        this.capacity = capacity;
        this.workingDays = workingDays;
        this.issues = issues;
        this.clock = clock;
    }

    public AllocationRateRow compute(Member member, Horizon horizon) {
        LocalDate today      = LocalDate.now(clock);
        LocalDate start      = horizon.windowStart(today);
        LocalDate endExcl    = horizon.windowEndExclusive(today);
        int wd               = workingDays.workingDays(start, endExcl);
        EffectiveCapacity ec = capacity.resolveOn(member.getId(), today);

        BigDecimal dailyH   = ec.dailyHours();
        BigDecimal standardH = dailyH.multiply(BigDecimal.valueOf(wd));
        BigDecimal standardMd = toMd(standardH, dailyH);

        BigDecimal committedH = sumRemainingHours(member.getJiraAccountId(), endExcl);
        BigDecimal committedMd = toMd(committedH, dailyH);

        BigDecimal availableMd = standardMd.subtract(committedMd).setScale(2, RoundingMode.HALF_UP);
        BigDecimal arPercent   = standardMd.signum() == 0
                ? ONE_HUNDRED
                : committedMd.multiply(ONE_HUNDRED).divide(standardMd, 2, RoundingMode.HALF_UP);
        AllocationBand band    = AllocationBand.of(arPercent);

        AllocationRateRow row = new AllocationRateRow();
        row.setKey(new AllocationRateKey(member.getId(), horizon.wire(), start));
        row.setWindowEnd(endExcl);
        row.setStandardMd(standardMd);
        row.setCommittedMd(committedMd);
        row.setAvailableMd(availableMd);
        row.setAllocationRate(arPercent);
        row.setBand(band.wire);
        return row;
    }

    /**
     * AC-006.1: Committed = remaining work that falls in the horizon. An issue counts
     * when it is due before the window ends — this includes overdue work (still owed)
     * and undated work (conservatively counted in every horizon).
     */
    private BigDecimal sumRemainingHours(String jiraAccountId, LocalDate windowEndExclusive) {
        if (jiraAccountId == null) return BigDecimal.ZERO;
        List<IssueProjection> active = issues
                .findByAssigneeAccountIdAndStatusCategoryNotAndAllowListOkTrue(jiraAccountId, "done");
        BigDecimal sum = BigDecimal.ZERO;
        for (IssueProjection i : active) {
            if (i.getDueDate() != null && !i.getDueDate().isBefore(windowEndExclusive)) continue;
            sum = sum.add(effectiveRemaining(i));
        }
        return sum;
    }

    /** B-RULE-02: no estimate (null, or 0 remaining with no original) → 0.5 MD placeholder. */
    public static BigDecimal effectiveRemaining(IssueProjection i) {
        BigDecimal rem  = i.getRemainingEstimateH();
        BigDecimal orig = i.getOriginalEstimateH();
        boolean unestimated = rem == null
                || (rem.signum() == 0 && (orig == null || orig.signum() == 0));
        return unestimated ? NO_ESTIMATE_PLACEHOLDER_HOURS : rem;
    }

    /** 1 MD = member's daily hours (not globally 8h) per DEC-004 — matches 04 Domain glossary. */
    private BigDecimal toMd(BigDecimal hours, BigDecimal dailyHours) {
        if (dailyHours.signum() == 0) return BigDecimal.ZERO;
        return hours.divide(dailyHours, 2, RoundingMode.HALF_UP);
    }
}
