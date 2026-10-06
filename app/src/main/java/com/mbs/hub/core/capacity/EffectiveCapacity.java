package com.mbs.hub.core.capacity;

import java.math.BigDecimal;

/**
 * Effective capacity for a member on a given date.
 * {@code source} says which tier applied: GLOBAL / TEAM / MEMBER.
 */
public record EffectiveCapacity(
        BigDecimal dailyHours,
        BigDecimal weeklyHours,
        Source source
) {
    public enum Source { GLOBAL, TEAM, MEMBER }
}
