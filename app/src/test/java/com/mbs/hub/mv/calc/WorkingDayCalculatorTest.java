package com.mbs.hub.mv.calc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.mbs.hub.core.holiday.Holiday;
import com.mbs.hub.core.holiday.HolidayKind;
import com.mbs.hub.core.holiday.HolidayRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkingDayCalculatorTest {

    @Mock HolidayRepository holidays;

    @Test
    void weekOfSevenDaysWithTwoWeekendDays_counts5() {
        when(holidays.findByDateBetween(any(), any())).thenReturn(List.of());
        WorkingDayCalculator c = new WorkingDayCalculator(holidays);
        // Mon 2026-10-05 .. Mon 2026-10-12 (exclusive)
        assertThat(c.workingDays(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 12))).isEqualTo(5);
    }

    @Test
    void nationalHolidayExcluded() {
        when(holidays.findByDateBetween(any(), any())).thenReturn(List.of(
                holiday(LocalDate.of(2026, 10, 7), HolidayKind.NATIONAL)));
        WorkingDayCalculator c = new WorkingDayCalculator(holidays);
        // Mon..Fri with one NATIONAL mid-week → 4 WD
        assertThat(c.workingDays(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 10))).isEqualTo(4);
    }

    @Test
    void compensatedWorkdayCountsOnSaturday() {
        when(holidays.findByDateBetween(any(), any())).thenReturn(List.of(
                holiday(LocalDate.of(2026, 10, 10), HolidayKind.COMPENSATED_WORKDAY)));
        WorkingDayCalculator c = new WorkingDayCalculator(holidays);
        // Mon..Sat with Sat marked compensated → 6 WD
        assertThat(c.workingDays(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 11))).isEqualTo(6);
    }

    @Test
    void companyDayExcluded() {
        when(holidays.findByDateBetween(any(), any())).thenReturn(List.of(
                holiday(LocalDate.of(2026, 10, 7), HolidayKind.COMPANY_DAY)));
        WorkingDayCalculator c = new WorkingDayCalculator(holidays);
        assertThat(c.workingDays(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 10))).isEqualTo(4);
    }

    private static Holiday holiday(LocalDate d, HolidayKind k) {
        Holiday h = new Holiday();
        h.setDate(d); h.setKind(k); h.setDescription("test");
        h.setUpdatedAt(OffsetDateTime.now());
        return h;
    }
}
