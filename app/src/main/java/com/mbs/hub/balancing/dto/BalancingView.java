package com.mbs.hub.balancing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Balancing suggestion payload — AC-006.3 / B-RULE-04 / 02 API §5.6.
 *
 * Hub returns the list of members whose Available MD in window [today, pipeline.targetDeadline]
 * ≥ pipeline.totalEstimatedMd AND whose skills cover the pipeline's requiredSkills,
 * sorted DESC by availableMd. The caller (Manager) decides who to assign.
 */
public record BalancingView(
        UUID pipelineId,
        LocalDate windowStart,
        LocalDate windowEnd,
        BigDecimal requiredMd,
        List<SkillTag> requiredSkills,
        List<Candidate> candidates,
        int totalCandidatesConsidered,       // size of the roster inspected
        int rejectedBySkill,
        int rejectedByCapacity
) {
    public record SkillTag(String l1, String l2) {}

    public record Candidate(
            UUID memberId,
            String displayName,
            BigDecimal availableMd,
            BigDecimal dailyHours,            // for reference / sorting ties
            List<SkillTag> matchedSkills
    ) {}
}
