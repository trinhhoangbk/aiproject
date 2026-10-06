package com.mbs.hub.mv;

import java.math.BigDecimal;

/**
 * DEC-011 Allocation Rate bands.
 * AR &lt; 60%     → DARK_GREEN     (underloaded)
 * 60% ≤ AR ≤ 85% → LIGHT_GREEN    (optimal)
 * 85% &lt; AR ≤ 100% → YELLOW   (busy)
 * AR &gt; 100%    → RED            (overloaded)
 */
public enum AllocationBand {
    DARK_GREEN("dark_green"),
    LIGHT_GREEN("light_green"),
    YELLOW("yellow"),
    RED("red");

    public final String wire;
    AllocationBand(String wire) { this.wire = wire; }

    public static AllocationBand of(BigDecimal allocationRatePercent) {
        int cmp60  = allocationRatePercent.compareTo(new BigDecimal("60"));
        int cmp85  = allocationRatePercent.compareTo(new BigDecimal("85"));
        int cmp100 = allocationRatePercent.compareTo(new BigDecimal("100"));
        if (cmp60  <  0) return DARK_GREEN;
        if (cmp85  <= 0) return LIGHT_GREEN;
        if (cmp100 <= 0) return YELLOW;
        return RED;
    }
}
