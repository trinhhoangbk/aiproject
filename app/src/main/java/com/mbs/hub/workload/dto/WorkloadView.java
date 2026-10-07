package com.mbs.hub.workload.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record WorkloadView(
        UUID memberId,
        String memberName,
        String window,
        LocalDate anchor,
        LocalDate windowStart,         // first day of the horizon (inclusive)
        LocalDate windowEnd,           // last day of the horizon (inclusive)
        BigDecimal standardMd,
        BigDecimal committedMd,
        BigDecimal availableMd,
        BigDecimal allocationRate,
        String band,
        OverloadView overload,
        List<ProjectBreakdown> projects,
        UnestimatedSummary unestimated
) {
    public record OverloadView(String flag, List<OverloadReason> reasons) {}

    public record OverloadReason(
            String level,            // "week" | "day"
            LocalDate date,           // null for week-level
            BigDecimal committedH,
            BigDecimal capacityH,
            List<String> hardDeadlineCriteria   // day-level only; null for week
    ) {}

    public record ProjectBreakdown(
            String projectKey,
            int issueCount,
            BigDecimal remainingH,
            BigDecimal ratioByHours,
            BigDecimal ratioByCount
    ) {}

    public record UnestimatedSummary(int count, BigDecimal defaultEachMd) {}
}
