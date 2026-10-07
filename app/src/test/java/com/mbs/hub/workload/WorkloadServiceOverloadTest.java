package com.mbs.hub.workload;

import static com.mbs.hub.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.mbs.hub.core.capacity.CapacityResolver;
import com.mbs.hub.core.lockdeadline.LockDeadlineFlagRepository;
import com.mbs.hub.core.member.MemberRepository;
import com.mbs.hub.mv.AllocationRateRepository;
import com.mbs.hub.sync.projection.IssueProjection;
import com.mbs.hub.sync.projection.IssueProjectionRepository;
import com.mbs.hub.workload.dto.WorkloadView.OverloadReason;
import com.mbs.hub.workload.dto.WorkloadView.OverloadView;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** TC-WL-01..06 — AC-001.2 (week), AC-001.3 + DEC-010 (day, cannot-reschedule), AC-001.4. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkloadServiceOverloadTest {

    @Mock MemberRepository members;
    @Mock CapacityResolver capacity;
    @Mock AllocationRateRepository mvRate;
    @Mock IssueProjectionRepository issues;
    @Mock LockDeadlineFlagRepository lockFlags;

    WorkloadService service;
    UUID memberId = UUID.randomUUID();
    LocalDate fri = LocalDate.of(2026, 10, 9);

    @BeforeEach
    void setUp() {
        service = new WorkloadService(members, capacity, mvRate, issues, lockFlags, clock());
    }

    private OverloadView overload(IssueProjection... active) {
        return service.overload(memberId, List.of(active), eightHourDay());
    }

    @Test
    void TC_WL_01_moreThanFortyHoursIsWeekOverload() {
        OverloadView v = overload(issue("KAN-1", "24", null), issue("KAN-2", "20", null));
        assertThat(v.flag()).isEqualTo("red");
        assertThat(v.reasons()).extracting(OverloadReason::level).containsExactly("week");
        assertThat(v.reasons().get(0).committedH()).isEqualByComparingTo("44");
        assertThat(v.reasons().get(0).capacityH()).isEqualByComparingTo("40");
    }

    @Test
    void TC_WL_02_exactlyFortyHoursIsNotOverload() {
        OverloadView v = overload(issue("KAN-1", "20", null), issue("KAN-2", "20", null));
        assertThat(v.flag()).isEqualTo("green");
        assertThat(v.reasons()).isEmpty();
    }

    @Test
    void TC_WL_03_dayOverCapWithBlockerIsDayOverload() {
        IssueProjection a = issue("KAN-1", "6", fri);
        IssueProjection b = issue("KAN-2", "6", fri);
        b.setPriority("Blocker");

        OverloadView v = overload(a, b);

        assertThat(v.flag()).isEqualTo("red");
        assertThat(v.reasons()).hasSize(1);
        OverloadReason r = v.reasons().get(0);
        assertThat(r.level()).isEqualTo("day");
        assertThat(r.date()).isEqualTo(fri);
        assertThat(r.hardDeadlineCriteria()).contains("priority_blocker");
    }

    @Test
    void TC_WL_04_dayOverCapWithoutHardDeadlineIsNotOverload() {
        OverloadView v = overload(issue("KAN-1", "6", fri), issue("KAN-2", "6", fri));
        assertThat(v.flag()).isEqualTo("green");
    }

    @Test
    void TC_WL_05_hubLockFlagMakesDayOverload() {
        when(lockFlags.existsById("KAN-2")).thenReturn(true);
        OverloadView v = overload(issue("KAN-1", "6", fri), issue("KAN-2", "6", fri));
        assertThat(v.reasons()).hasSize(1);
        assertThat(v.reasons().get(0).hardDeadlineCriteria()).contains("hub_lock_flag");
    }

    @Test
    void TC_WL_06_unestimatedMeansNoRemainingAndNoOriginal() {
        IssueProjection zeroWithOriginal = issue("KAN-3", "0", null);
        zeroWithOriginal.setOriginalEstimateH(new java.math.BigDecimal("8"));

        assertThat(WorkloadService.isUnestimated(issue("KAN-1", null, null))).isTrue();
        assertThat(WorkloadService.isUnestimated(issue("KAN-2", "0", null))).isTrue();
        assertThat(WorkloadService.isUnestimated(zeroWithOriginal)).isFalse();
        assertThat(WorkloadService.isUnestimated(issue("KAN-4", "8", null))).isFalse();
    }
}
