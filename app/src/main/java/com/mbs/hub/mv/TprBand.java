package com.mbs.hub.mv;

import java.math.BigDecimal;

/**
 * DEC-009 Time Pressure Ratio (Early-Warning) bands.
 * RDC &gt; 1.0        → RED
 * 0.7 &lt; RDC ≤ 1.0  → YELLOW
 * else                → NONE
 */
public enum TprBand {
    NONE("none"), YELLOW("yellow"), RED("red");

    public final String wire;
    TprBand(String wire) { this.wire = wire; }

    public static TprBand of(BigDecimal rdc) {
        if (rdc == null) return NONE;
        int cmp07 = rdc.compareTo(new BigDecimal("0.700"));
        int cmp10 = rdc.compareTo(new BigDecimal("1.000"));
        if (cmp10 >  0) return RED;
        if (cmp07 >  0) return YELLOW;
        return NONE;
    }
}
