package com.mbs.hub.mv;

import java.time.LocalDate;
import java.time.temporal.ChronoField;

/** Horizon values stored in mv.mv_allocation_rate.horizon. */
public enum Horizon {
    THIS_WEEK("this_week"),
    NEXT_2_WEEKS("next_2_weeks"),
    NEXT_MONTH("next_month");

    private final String wire;
    Horizon(String wire) { this.wire = wire; }
    public String wire() { return wire; }

    /** Window start for a horizon anchored on {@code today} (Asia/Saigon). */
    public LocalDate windowStart(LocalDate today) {
        return switch (this) {
            case THIS_WEEK, NEXT_2_WEEKS -> today.with(ChronoField.DAY_OF_WEEK, 1); // Monday
            case NEXT_MONTH               -> today.withDayOfMonth(1);
        };
    }

    /** Exclusive window end. */
    public LocalDate windowEndExclusive(LocalDate today) {
        LocalDate start = windowStart(today);
        return switch (this) {
            case THIS_WEEK     -> start.plusWeeks(1);
            case NEXT_2_WEEKS  -> start.plusWeeks(2);
            case NEXT_MONTH    -> start.plusMonths(1);
        };
    }
}
