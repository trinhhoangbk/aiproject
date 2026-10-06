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

        BigDecimal committedH = sumRemainingHours(member.getJiraAccountId());
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

    private BigDecimal sumRemainingHours(String jiraAccountId) {
        if (jiraAccountId == null) return BigDecimal.ZERO;
        List<IssueProjection> active = issues
                .findByAssigneeAccountIdAndStatusCategoryNotAndAllowListOkTrue(jiraAccountId, "done");
        BigDecimal sum = BigDecimal.ZERO;
        for (IssueProjection i : active) {
            BigDecimal h = i.getRemainingEstimateH();
            sum = sum.add(h == null ? NO_ESTIMATE_PLACEHOLDER_HOURS : h);   // B-RULE-02
        }
        return sum;
    }

    /** 1 MD = member's daily hours (not globally 8h) per DEC-004 — matches 04 Domain glossary. */
    private BigDecimal toMd(BigDecimal hours, BigDecimal dailyHours) {
        if (dailyHours.signum() == 0) return BigDecimal.ZERO;
        return hours.divide(dailyHours, 2, RoundingMode.HALF_UP);
    }
}
