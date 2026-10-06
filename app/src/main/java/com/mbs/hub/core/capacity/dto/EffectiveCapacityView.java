package com.mbs.hub.core.capacity.dto;

import com.mbs.hub.core.capacity.EffectiveCapacity;
import java.math.BigDecimal;

public record EffectiveCapacityView(BigDecimal dailyHours, BigDecimal weeklyHours, String source) {
    public static EffectiveCapacityView of(EffectiveCapacity c) {
        return new EffectiveCapacityView(c.dailyHours(), c.weeklyHours(), c.source().name());
    }
}
