package com.mbs.hub.balancing;

import static com.mbs.hub.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.mbs.hub.balancing.dto.BalancingView;
import com.mbs.hub.core.capacity.CapacityResolver;
import com.mbs.hub.core.member.Member;
import com.mbs.hub.core.member.MemberRepository;
import com.mbs.hub.core.member.Role;
import com.mbs.hub.core.pipeline.PipelineProject;
import com.mbs.hub.core.pipeline.PipelineProjectRepository;
import com.mbs.hub.core.pipeline.PipelineRequiredSkill;
import com.mbs.hub.core.pipeline.PipelineRequiredSkillKey;
import com.mbs.hub.core.pipeline.PipelineRequiredSkillRepository;
import com.mbs.hub.core.skill.MemberSkill;
import com.mbs.hub.core.skill.MemberSkillKey;
import com.mbs.hub.core.skill.MemberSkillRepository;
import com.mbs.hub.mv.calc.WorkingDayCalculator;
import com.mbs.hub.sync.projection.IssueProjectionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** TC-BL-01 — AC-006.3 / B-RULE-04 / DEC-007. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BalancingServiceTest {

    @Mock PipelineProjectRepository pipelines;
    @Mock PipelineRequiredSkillRepository requiredSkills;
    @Mock MemberRepository members;
    @Mock MemberSkillRepository memberSkills;
    @Mock CapacityResolver capacity;
    @Mock IssueProjectionRepository issues;
    @Mock WorkingDayCalculator wdc;

    static MemberSkill skill(Member m, String l1) {
        MemberSkill s = new MemberSkill();
        s.setKey(new MemberSkillKey(m.getId(), l1, ""));
        return s;
    }

    @Test
    void TC_BL_01_skillAndCapacityFilteredCandidatesSortedByAvailability() {
        UUID pid = UUID.randomUUID();
        PipelineProject p = new PipelineProject();
        p.setId(pid);
        p.setTargetDeadline(LocalDate.of(2026, 10, 16));
        p.setTotalEstimatedMd(new BigDecimal("5"));
        when(pipelines.findById(pid)).thenReturn(Optional.of(p));
        PipelineRequiredSkill req = new PipelineRequiredSkill();
        req.setKey(new PipelineRequiredSkillKey(pid, "BACKEND", ""));
        when(requiredSkills.findByKey_PipelineId(pid)).thenReturn(List.of(req));

        Member a = member("acc-A", Role.MEMBER);
        Member b = member("acc-B", Role.MEMBER);
        Member c = member("acc-C", Role.MEMBER);
        Member d = member("acc-D", Role.MEMBER);
        when(members.findAll()).thenReturn(List.of(a, b, c, d));
        when(memberSkills.findByKey_MemberId(a.getId())).thenReturn(List.of(skill(a, "BACKEND")));
        when(memberSkills.findByKey_MemberId(b.getId())).thenReturn(List.of(skill(b, "FRONTEND")));
        when(memberSkills.findByKey_MemberId(c.getId())).thenReturn(List.of(skill(c, "BACKEND")));
        when(memberSkills.findByKey_MemberId(d.getId())).thenReturn(List.of(skill(d, "BACKEND")));

        when(capacity.resolveOn(any(), any())).thenReturn(eightHourDay());
        when(wdc.workingDays(any(), any())).thenReturn(10);
        when(issues.findByAssigneeAccountIdAndStatusCategoryNotAndAllowListOkTrue("acc-A", "done")).thenReturn(List.of());
        when(issues.findByAssigneeAccountIdAndStatusCategoryNotAndAllowListOkTrue("acc-C", "done"))
                .thenReturn(List.of(issue("KAN-3", "72", LocalDate.of(2026, 10, 14))));
        when(issues.findByAssigneeAccountIdAndStatusCategoryNotAndAllowListOkTrue("acc-D", "done"))
                .thenReturn(List.of(issue("KAN-4", "16", LocalDate.of(2026, 10, 15))));

        BalancingView v = new BalancingService(pipelines, requiredSkills, members, memberSkills,
                capacity, issues, wdc, clock()).suggestFor(pid);

        assertThat(v.candidates()).extracting(BalancingView.Candidate::memberId)
                .containsExactly(a.getId(), d.getId());
        assertThat(v.candidates().get(0).availableMd()).isEqualByComparingTo("10.00");
        assertThat(v.candidates().get(1).availableMd()).isEqualByComparingTo("8.00");
        assertThat(v.rejectedBySkill()).isEqualTo(1);
        assertThat(v.rejectedByCapacity()).isEqualTo(1);
    }
}
