package com.mbs.hub.core.capacity;

import com.mbs.hub.core.member.Member;
import com.mbs.hub.core.member.MemberRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Three-tier capacity resolver per DEC-004.
 * Precedence (highest wins): MEMBER override → TEAM override → GLOBAL default.
 * History-aware: picks the latest row with {@code effective_from <= on}.
 */
@Service
public class CapacityResolver {

    private final CapacityGlobalRepository globalRepo;
    private final CapacityTeamRepository teamRepo;
    private final CapacityMemberRepository memberRepo;
    private final MemberRepository members;
    private final Clock clock;

    public CapacityResolver(
            CapacityGlobalRepository globalRepo,
            CapacityTeamRepository teamRepo,
            CapacityMemberRepository memberRepo,
            MemberRepository members,
            Clock clock) {
        this.globalRepo = globalRepo;
        this.teamRepo = teamRepo;
        this.memberRepo = memberRepo;
        this.members = members;
        this.clock = clock;
    }

    /** Resolve for today in the configured timezone (Asia/Saigon, DEC-002). */
    public EffectiveCapacity resolveToday(UUID memberId) {
        return resolveOn(memberId, LocalDate.now(clock));
    }

    public EffectiveCapacity resolveOn(UUID memberId, LocalDate on) {
        // 1. Member tier wins if a row exists.
        Optional<CapacityMember> mOverride = memberRepo.findCurrent(memberId, on);
        if (mOverride.isPresent()) {
            CapacityMember c = mOverride.get();
            return new EffectiveCapacity(c.getDailyHours(), c.getWeeklyHours(),
                EffectiveCapacity.Source.MEMBER);
        }
        // 2. Team tier if the member belongs to a team with an override.
        Member member = members.findById(memberId).orElseThrow(() ->
            new IllegalArgumentException("Unknown member id: " + memberId));
        if (member.getTeamId() != null) {
            Optional<CapacityTeam> tOverride = teamRepo.findCurrent(member.getTeamId(), on);
            if (tOverride.isPresent()) {
                CapacityTeam c = tOverride.get();
                return new EffectiveCapacity(c.getDailyHours(), c.getWeeklyHours(),
                    EffectiveCapacity.Source.TEAM);
            }
        }
        // 3. Global default (always present per V001 seed).
        CapacityGlobal g = globalRepo.getSingleton();
        return new EffectiveCapacity(g.getDailyHours(), g.getWeeklyHours(),
            EffectiveCapacity.Source.GLOBAL);
    }
}
