package com.mbs.hub.mv.calc;

import static com.mbs.hub.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.mbs.hub.core.capacity.CapacityResolver;
import com.mbs.hub.core.member.Member;
import com.mbs.hub.core.member.Role;
import com.mbs.hub.mv.AllocationRateRow;
import com.mbs.hub.mv.Horizon;
import com.mbs.hub.sync.projection.IssueProjection;
import com.mbs.hub.sync.projection.IssueProjectionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** TC-AR-01..05 — AC-006.1 / AC-006.2 / AC-001.4 (BD-04 interpretation of "committed in W"). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AllocationRateCalculatorTest {

    @Mock CapacityResolver capacity;
    @Mock WorkingDayCalculator workingDays;
    @Mock IssueProjectionRepository issues;

    AllocationRateCalculator calc;
    Member m = member("acc-1", Role.MEMBER);

    @BeforeEach
    void setUp() {
        when(capacity.resolveOn(any(), any())).thenReturn(eightHourDay());
        when(workingDays.workingDays(any(), any())).thenReturn(5);
        calc = new AllocationRateCalculator(capacity, workingDays, issues, clock());
    }

    private void active(IssueProjection... list) {
        when(issues.findByAssigneeAccountIdAndStatusCategoryNotAndAllowListOkTrue(anyString(), anyString()))
                .thenReturn(List.of(list));
    }

    @Test
    void TC_AR_01_onlyWorkDueInsideTheWindowIsCommitted() {
        active(issue("KAN-1", "16", LocalDate.of(2026, 10, 9)),
               issue("KAN-2", "40", LocalDate.of(2026, 10, 20)));

        AllocationRateRow row = calc.compute(m, Horizon.THIS_WEEK);

        assertThat(row.getStandardMd()).isEqualByComparingTo("5.00");
        assertThat(row.getCommittedMd()).isEqualByComparingTo("2.00");
        assertThat(row.getAllocationRate()).isEqualByComparingTo("40.00");
        assertThat(row.getBand()).isEqualTo("dark_green");
    }

    @Test
    void TC_AR_02_overdueAndUndatedWorkIsStillCommitted() {
        active(issue("KAN-1", "8", LocalDate.of(2026, 10, 1)),
               issue("KAN-2", "8", null));

        AllocationRateRow row = calc.compute(m, Horizon.THIS_WEEK);

        assertThat(row.getCommittedMd()).isEqualByComparingTo("2.00");
    }

    @Test
    void TC_AR_03_unestimatedIssueCountsAsHalfManDay() {
        IssueProjection noEstimate = issue("KAN-1", null, TODAY);
        IssueProjection zeroNoOriginal = issue("KAN-2", "0", TODAY);
        IssueProjection zeroWithOriginal = issue("KAN-3", "0", TODAY);
        zeroWithOriginal.setOriginalEstimateH(new BigDecimal("8"));

        assertThat(AllocationRateCalculator.effectiveRemaining(noEstimate)).isEqualByComparingTo("4");
        assertThat(AllocationRateCalculator.effectiveRemaining(zeroNoOriginal)).isEqualByComparingTo("4");
        assertThat(AllocationRateCalculator.effectiveRemaining(zeroWithOriginal)).isEqualByComparingTo("0");
    }

    @Test
    void TC_AR_04_overCapacityIsRed() {
        active(issue("KAN-1", "44", LocalDate.of(2026, 10, 9)));

        AllocationRateRow row = calc.compute(m, Horizon.THIS_WEEK);

        assertThat(row.getAllocationRate()).isEqualByComparingTo("110.00");
        assertThat(row.getBand()).isEqualTo("red");
    }

    @Test
    void TC_AR_05_zeroWorkingDaysDoesNotDivideByZero() {
        when(workingDays.workingDays(any(), any())).thenReturn(0);
        active(issue("KAN-1", "8", LocalDate.of(2026, 10, 9)));

        AllocationRateRow row = calc.compute(m, Horizon.THIS_WEEK);

        assertThat(row.getStandardMd()).isEqualByComparingTo("0");
        assertThat(row.getAllocationRate()).isEqualByComparingTo("100");
    }
}
