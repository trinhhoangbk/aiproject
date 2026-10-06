package com.mbs.hub.mv;

/** F-01 (BUSINESS APPROVED) overdue severity bands. */
public enum OverdueBand {
    ONE_TO_3_DAYS("1_to_3_days"),
    FOUR_TO_7_DAYS("4_to_7_days"),
    MORE_THAN_A_WEEK("more_than_a_week");

    public final String wire;
    OverdueBand(String wire) { this.wire = wire; }

    public static OverdueBand of(int daysOverdue) {
        if (daysOverdue <= 3) return ONE_TO_3_DAYS;
        if (daysOverdue <= 7) return FOUR_TO_7_DAYS;
        return MORE_THAN_A_WEEK;
    }
}
