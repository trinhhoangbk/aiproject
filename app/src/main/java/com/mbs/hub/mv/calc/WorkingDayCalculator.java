package com.mbs.hub.mv.calc;

import com.mbs.hub.core.holiday.Holiday;
import com.mbs.hub.core.holiday.HolidayKind;
import com.mbs.hub.core.holiday.HolidayRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * B-RULE-01 working-day counter. A date is a working day when it is NOT
 * Saturday/Sunday, UNLESS it is a {@code COMPENSATED_WORKDAY}, AND NOT a
 * {@code NATIONAL} / {@code COMPANY_DAY}.
 */
@Component
public class WorkingDayCalculator {

    private final HolidayRepository holidays;

    public WorkingDayCalculator(HolidayRepository holidays) { this.holidays = holidays; }

    /** Count working days in {@code [from, toExclusive)}. */
    public int workingDays(LocalDate from, LocalDate toExclusive) {
        if (!from.isBefore(toExclusive)) return 0;
        Map<LocalDate, HolidayKind> cal = calendarIndex(from, toExclusive.minusDays(1));
        int count = 0;
        for (LocalDate d = from; d.isBefore(toExclusive); d = d.plusDays(1)) {
            if (isWorkingDay(d, cal)) count++;
        }
        return count;
    }

    public boolean isWorkingDay(LocalDate d) {
        return isWorkingDay(d, calendarIndex(d, d));
    }

    private boolean isWorkingDay(LocalDate d, Map<LocalDate, HolidayKind> cal) {
        HolidayKind kind = cal.get(d);
        boolean weekend = (d.getDayOfWeek() == DayOfWeek.SATURDAY
                        || d.getDayOfWeek() == DayOfWeek.SUNDAY);
        if (kind == HolidayKind.COMPENSATED_WORKDAY) return true;
        if (kind == HolidayKind.NATIONAL || kind == HolidayKind.COMPANY_DAY) return false;
        return !weekend;
    }

    private Map<LocalDate, HolidayKind> calendarIndex(LocalDate from, LocalDate to) {
        List<Holiday> rows = holidays.findByDateBetween(from, to);
        Map<LocalDate, HolidayKind> idx = new HashMap<>();
        for (Holiday h : rows) idx.put(h.getDate(), h.getKind());
        return idx;
    }
}
