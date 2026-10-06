package com.mbs.hub.core.capacity.dto;

import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CapacityUpdateRequest(
        @Positive BigDecimal dailyHours,
        @Positive BigDecimal weeklyHours,
        LocalDate effectiveFrom
) {}
