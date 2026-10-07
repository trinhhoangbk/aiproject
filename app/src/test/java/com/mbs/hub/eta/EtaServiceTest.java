package com.mbs.hub.eta;

import static com.mbs.hub.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.mbs.hub.core.capacity.CapacityResolver;
import com.mbs.hub.core.member.Member;
import com.mbs.hub.core.member.MemberRepository;
import com.mbs.hub.core.member.Role;
import com.mbs.hub.eta.dto.EtaView;
import com.mbs.hub.mv.calc.WorkingDayCalculator;
import com.mbs.hub.sync.projection.IssueProjectionRepository;
import com.mbs.hub.sync.projection.WorklogProjectionRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** TC-ETA-01..03 — AC-004.1 / AC-004.2 (α over last 10 WD, F-03). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EtaServiceTest {

    @Mock MemberRepository members;
    @Mock IssueProjectionRepository issues;
    @Mock WorklogProjectionRepository worklogs;
    @Mock CapacityResolver capacity;
    @Mock WorkingDayCalculator wdc;

    Member m = member("acc-1", Role.MEMBER);
    EtaService service;

    @BeforeEach
    void setUp() {
        when(members.findById(m.getId())).thenReturn(Optional.of(m));
        when(capacity.resolveOn(eq(m.getId()), any())).thenReturn(eightHourDay());
        when(wdc.isWorkingDay(any())).thenAnswer(inv -> isWeekday(inv.getArgument(0)));
        service = new EtaService(members, issues, worklogs, capacity, wdc, clock());
    }

    private EtaView.ProjectEta project(EtaView v, String key) {
        return v.projects().stream().filter(p -> p.projectKey().equals(key)).findFirst().orElseThrow();
    }

    @Test
    void TC_ETA_01_alphaHalf_fortyHours_tenWorkingDays() {
        when(worklogs.sumByProjectBetween(eq("acc-1"), any(), any())).thenReturn(List.<Object[]>of(
                new Object[]{"KAN", 72_000L}, new Object[]{"OPS", 72_000L}));   // 20 h each
        when(issues.findByAssigneeAccountIdAndStatusCategoryNotAndAllowListOkTrue("acc-1", "done"))
                .thenReturn(List.of(issue("KAN-1", "40", null)));

        EtaView.ProjectEta kan = project(service.forecast(m.getId()), "KAN");

        assertThat(kan.allocationShare()).isEqualByComparingTo("0.500");
        assertThat(kan.rwd()).isEqualTo(10);
        assertThat(kan.etaDate()).isEqualTo(LocalDate.of(2026, 10, 21));
    }

    @Test
    void TC_ETA_02_remainingReportedInHoursAndManDays() {
        when(worklogs.sumByProjectBetween(eq("acc-1"), any(), any())).thenReturn(List.<Object[]>of(
                new Object[]{"KAN", 72_000L}));
        when(issues.findByAssigneeAccountIdAndStatusCategoryNotAndAllowListOkTrue("acc-1", "done"))
                .thenReturn(List.of(issue("KAN-1", "40", null)));

        EtaView.ProjectEta kan = project(service.forecast(m.getId()), "KAN");

        assertThat(kan.remainingHours()).isEqualByComparingTo("40.00");
        assertThat(kan.remainingMd()).isEqualByComparingTo("5.00");
    }

    @Test
    void TC_ETA_03_noObservedWorklogGivesNoDate() {
        when(worklogs.sumByProjectBetween(eq("acc-1"), any(), any())).thenReturn(List.of());
        when(issues.findByAssigneeAccountIdAndStatusCategoryNotAndAllowListOkTrue("acc-1", "done"))
                .thenReturn(List.of(issue("KAN-1", "16", null)));

        EtaView.ProjectEta kan = project(service.forecast(m.getId()), "KAN");

        assertThat(kan.etaDate()).isNull();
        assertThat(kan.note()).isEqualTo("no_worklog_on_project_last_10_wd");
    }
}
