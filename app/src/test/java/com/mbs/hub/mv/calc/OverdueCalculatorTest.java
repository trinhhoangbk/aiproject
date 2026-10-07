package com.mbs.hub.mv.calc;

import static com.mbs.hub.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.mbs.hub.core.capacity.CapacityResolver;
import com.mbs.hub.core.member.Member;
import com.mbs.hub.core.member.Role;
import com.mbs.hub.mv.MemberOverdueRow;
import com.mbs.hub.sync.projection.IssueProjection;
import com.mbs.hub.sync.projection.IssueProjectionRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** TC-OD-01..09 — AC-003.1/.2/.4/.5, AC-001.4. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OverdueCalculatorTest {

    @Mock CapacityResolver capacity;
    @Mock WorkingDayCalculator workingDays;
    @Mock IssueProjectionRepository issues;

    Member m = member("acc-1", Role.MEMBER);

    @BeforeEach
    void setUp() {
        when(capacity.resolveOn(any(), any())).thenReturn(eightHourDay());
        when(workingDays.workingDays(any(), any())).thenAnswer(inv ->
                weekdays(inv.getArgument(0), inv.getArgument(1)));
    }

    private List<MemberOverdueRow> run(Clock clock, IssueProjection... list) {
        when(issues.findByAssigneeAccountIdAndStatusCategoryNotAndAllowListOkTrue(anyString(), anyString()))
                .thenReturn(List.of(list));
        return new OverdueCalculator(capacity, workingDays, issues, clock).compute(m);
    }

    private List<MemberOverdueRow> run(IssueProjection... list) { return run(clock(), list); }

    @Test
    void TC_OD_01_oneDayOverdue() {
        List<MemberOverdueRow> rows = run(issue("KAN-1", "8", LocalDate.of(2026, 10, 6)));
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getDaysOverdue()).isEqualTo(1);
        assertThat(rows.get(0).getOverdueBand()).isEqualTo("1_to_3_days");
        assertThat(rows.get(0).getTprBand()).isEqualTo("none");
    }

    @Test
    void TC_OD_02_tenDaysOverdueIsMoreThanAWeek() {
        assertThat(run(issue("KAN-1", "8", LocalDate.of(2026, 9, 27))).get(0).getOverdueBand())
                .isEqualTo("more_than_a_week");
    }

    @Test
    void TC_OD_03_sevenDaysOverdueIsFourToSeven() {
        assertThat(run(issue("KAN-1", "8", LocalDate.of(2026, 9, 30))).get(0).getOverdueBand())
                .isEqualTo("4_to_7_days");
    }

    @Test
    void TC_OD_04_rdcAboveSeventyPercentIsYellow() {
        // due Fri 10-09: Wed..Fri = 3 WD × 8 h = 24 h; 18/24 = 0.750
        List<MemberOverdueRow> rows = run(issue("KAN-1", "18", LocalDate.of(2026, 10, 9)));
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getDaysOverdue()).isZero();
        assertThat(rows.get(0).getTpr()).isEqualByComparingTo("0.750");
        assertThat(rows.get(0).getTprBand()).isEqualTo("yellow");
    }

    @Test
    void TC_OD_05_rdcAboveOneIsRed() {
        assertThat(run(issue("KAN-1", "30", LocalDate.of(2026, 10, 9))).get(0).getTprBand())
                .isEqualTo("red");
    }

    @Test
    void TC_OD_06_healthyIssueProducesNoRow() {
        assertThat(run(issue("KAN-1", "8", LocalDate.of(2026, 10, 9)))).isEmpty();
    }

    @Test
    void TC_OD_07_noWorkingDayLeftIsRed() {
        // Today Sat 2026-10-10, due today → 0 working days remaining.
        LocalDate sat = LocalDate.of(2026, 10, 10);
        List<MemberOverdueRow> rows = run(clockOn(sat), issue("KAN-1", "1", sat));
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getTprBand()).isEqualTo("red");
    }

    @Test
    void TC_OD_08_undatedIssueIsIgnored() {
        assertThat(run(issue("KAN-1", "80", null))).isEmpty();
    }

    @Test
    void TC_OD_09_unestimatedOverdueIssueUsesHalfManDayPlaceholder() {
        // SF-03: remaining 0 with no original estimate = unestimated (AC-001.4) → 4 h.
        List<MemberOverdueRow> rows = run(issue("KAN-1", "0", LocalDate.of(2026, 10, 6)));
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getRemainingH()).isEqualByComparingTo("4.00");
    }
}
