package com.mbs.hub.overdue.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record OverdueView(
        String issueKey,
        String projectKey,
        UUID memberId,
        LocalDate dueDate,
        int daysOverdue,
        String overdueBand,       // 1_to_3_days | 4_to_7_days | more_than_a_week
        BigDecimal remainingH
) {}
