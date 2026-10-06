package com.mbs.hub.overdue.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record WarningView(
        String issueKey,
        String projectKey,
        UUID memberId,
        LocalDate dueDate,
        BigDecimal remainingH,
        BigDecimal tpr,           // DEC-009 RDC
        String tprBand            // yellow | red
) {}
