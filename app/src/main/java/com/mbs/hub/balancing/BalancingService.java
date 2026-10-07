package com.mbs.hub.balancing;

import com.mbs.hub.balancing.dto.BalancingView;
import com.mbs.hub.core.capacity.CapacityResolver;
import com.mbs.hub.core.capacity.EffectiveCapacity;
import com.mbs.hub.core.member.Member;
import com.mbs.hub.core.member.MemberRepository;
import com.mbs.hub.core.pipeline.PipelineProject;
import com.mbs.hub.core.pipeline.PipelineProjectRepository;
import com.mbs.hub.core.pipeline.PipelineRequiredSkill;
import com.mbs.hub.core.pipeline.PipelineRequiredSkillRepository;
import com.mbs.hub.core.skill.MemberSkill;
import com.mbs.hub.core.skill.MemberSkillRepository;
import com.mbs.hub.mv.calc.WorkingDayCalculator;
import com.mbs.hub.sync.projection.IssueProjection;
import com.mbs.hub.sync.projection.IssueProjectionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * PLAN-025 — Balancing suggestion (B-RULE-04 / AC-006.3).
 *
 * <p>Given a Pipeline project P with required MD R and target deadline D:
 * for every member M in the roster, compute Available_MD in [today, D] and
 * check whether M's skills cover P's requiredSkills. Return the members that
 * pass both filters, sorted DESC by availableMd.</p>
 *
 * <p>Available_MD is computed from the same primitives as the workload view:
 * standard_md = workingDays(window) × dailyHours / dailyHours = workingDays(window)
 * (expressed in member-standard-days). committed_md = Σ remaining_estimate_h over
 * active, allow-listed, assigned issues whose due_date falls in the window ÷
 * dailyHours.</p>
 */
@Service
public class BalancingService {

    private final PipelineProjectRepository pipelines;
    private final PipelineRequiredSkillRepository requiredSkills;
    private final MemberRepository members;
    private final MemberSkillRepository memberSkills;
    private final CapacityResolver capacity;
    private final IssueProjectionRepository issues;
    private final WorkingDayCalculator wdc;
    private final Clock clock;

    public BalancingService(PipelineProjectRepository pipelines,
                            PipelineRequiredSkillRepository requiredSkills,
                            MemberRepository members,
                            MemberSkillRepository memberSkills,
                            CapacityResolver capacity,
                            IssueProjectionRepository issues,
                            WorkingDayCalculator wdc,
                            Clock clock) {
        this.pipelines = pipelines;
        this.requiredSkills = requiredSkills;
        this.members = members;
        this.memberSkills = memberSkills;
        this.capacity = capacity;
        this.issues = issues;
        this.wdc = wdc;
        this.clock = clock;
    }

    public BalancingView suggestFor(UUID pipelineId) {
        PipelineProject p = pipelines.findById(pipelineId)
                .orElseThrow(() -> new IllegalArgumentException("pipeline not found: " + pipelineId));
        LocalDate today = LocalDate.now(clock);
        LocalDate deadline = p.getTargetDeadline();
        if (!deadline.isAfter(today)) {
            // Window is empty / past — surface a clear empty result rather than fail hard.
            return new BalancingView(pipelineId, today, deadline, p.getTotalEstimatedMd(),
                    requiredSkillTags(pipelineId), List.of(), 0, 0, 0);
        }

        // Pipeline requirements.
        List<PipelineRequiredSkill> reqSkills = requiredSkills.findByKey_PipelineId(pipelineId);
        Set<String> reqKeys = reqSkills.stream()
                .map(r -> skillKey(r.getKey().getL1(), r.getKey().getL2()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        BigDecimal R = p.getTotalEstimatedMd();

        List<Member> all = members.findAll();
        int skillRejects = 0;
        int capacityRejects = 0;
        List<BalancingView.Candidate> candidates = new ArrayList<>();

        for (Member m : all) {
            if (!m.isActive()) continue;

            // Skill filter.
            Set<String> memberKeys = memberSkills.findByKey_MemberId(m.getId()).stream()
                    .map(ms -> skillKey(ms.getKey().getL1(), ms.getKey().getL2()))
                    .collect(Collectors.toSet());
            Set<String> matched = new LinkedHashSet<>(reqKeys);
            matched.retainAll(memberKeys);
            if (matched.size() < reqKeys.size()) {   // must cover every required skill
                skillRejects++;
                continue;
            }

            // Available_MD in [today, deadline+1).
            EffectiveCapacity cap = capacity.resolveOn(m.getId(), today);
            BigDecimal dailyHours = cap.dailyHours();
            int wd = wdc.workingDays(today, deadline.plusDays(1));
            BigDecimal standardMd = BigDecimal.valueOf(wd);     // 1 MD = member standard day
            BigDecimal committedHours = issues
                    .findByAssigneeAccountIdAndStatusCategoryNotAndAllowListOkTrue(m.getJiraAccountId(), "done")
                    .stream()
                    .filter(i -> i.getDueDate() != null
                               && !i.getDueDate().isBefore(today)
                               && !i.getDueDate().isAfter(deadline))
                    .map(com.mbs.hub.mv.calc.AllocationRateCalculator::effectiveRemaining)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal committedMd = dailyHours.signum() > 0
                    ? committedHours.divide(dailyHours, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal availableMd = standardMd.subtract(committedMd).setScale(2, RoundingMode.HALF_UP);
            if (availableMd.compareTo(R) < 0) {
                capacityRejects++;
                continue;
            }

            List<BalancingView.SkillTag> matchedTags = matched.stream()
                    .map(k -> {
                        int sep = k.indexOf('/');
                        return new BalancingView.SkillTag(k.substring(0, sep), k.substring(sep + 1));
                    })
                    .toList();
            candidates.add(new BalancingView.Candidate(
                    m.getId(), m.getDisplayName(), availableMd, dailyHours, matchedTags));
        }

        candidates.sort(Comparator.comparing(BalancingView.Candidate::availableMd).reversed());

        return new BalancingView(
                pipelineId, today, deadline, R,
                requiredSkillTags(pipelineId),
                candidates,
                all.size(), skillRejects, capacityRejects);
    }

    private List<BalancingView.SkillTag> requiredSkillTags(UUID pipelineId) {
        return requiredSkills.findByKey_PipelineId(pipelineId).stream()
                .map(r -> new BalancingView.SkillTag(r.getKey().getL1(), r.getKey().getL2()))
                .toList();
    }


    private static String skillKey(String l1, String l2) {
        return l1 + "/" + (l2 == null ? "" : l2);
    }
}
