package com.mbs.hub.core.capacity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.mbs.hub.core.member.Member;
import com.mbs.hub.core.member.MemberRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Verifies DEC-004 three-tier precedence: MEMBER override &gt; TEAM override &gt; GLOBAL default.
 */
@ExtendWith(MockitoExtension.class)
class CapacityResolverTest {

    @Mock CapacityGlobalRepository globalRepo;
    @Mock CapacityTeamRepository   teamRepo;
    @Mock CapacityMemberRepository memberRepo;
    @Mock MemberRepository         members;

    Clock clock = Clock.fixed(Instant.parse("2026-10-06T03:00:00Z"), ZoneId.of("Asia/Saigon"));
    CapacityResolver resolver;
    UUID memberId = UUID.randomUUID();
    UUID teamId   = UUID.randomUUID();
    LocalDate on  = LocalDate.of(2026, 10, 6);

    @BeforeEach
    void setup() {
        resolver = new CapacityResolver(globalRepo, teamRepo, memberRepo, members, clock);
    }

    @Test
    void memberOverrideBeatsTeamAndGlobal() {
        CapacityMember cm = new CapacityMember();
        cm.setDailyHours(new BigDecimal("4.00"));
        cm.setWeeklyHours(new BigDecimal("20.00"));
        when(memberRepo.findCurrent(memberId, on)).thenReturn(Optional.of(cm));

        EffectiveCapacity out = resolver.resolveOn(memberId, on);

        assertThat(out.source()).isEqualTo(EffectiveCapacity.Source.MEMBER);
        assertThat(out.dailyHours()).isEqualByComparingTo("4.00");
        assertThat(out.weeklyHours()).isEqualByComparingTo("20.00");
    }

    @Test
    void teamOverrideBeatsGlobal_whenNoMemberOverride() {
        when(memberRepo.findCurrent(memberId, on)).thenReturn(Optional.empty());
        Member m = new Member();
        m.setId(memberId); m.setTeamId(teamId);
        when(members.findById(memberId)).thenReturn(Optional.of(m));
        CapacityTeam ct = new CapacityTeam();
        ct.setDailyHours(new BigDecimal("7.00"));
        ct.setWeeklyHours(new BigDecimal("35.00"));
        when(teamRepo.findCurrent(teamId, on)).thenReturn(Optional.of(ct));

        EffectiveCapacity out = resolver.resolveOn(memberId, on);

        assertThat(out.source()).isEqualTo(EffectiveCapacity.Source.TEAM);
        assertThat(out.dailyHours()).isEqualByComparingTo("7.00");
    }

    @Test
    void globalDefault_whenNoOverrides() {
        when(memberRepo.findCurrent(memberId, on)).thenReturn(Optional.empty());
        Member m = new Member();
        m.setId(memberId); m.setTeamId(null);        // no team
        when(members.findById(memberId)).thenReturn(Optional.of(m));
        CapacityGlobal g = new CapacityGlobal();
        g.setDailyHours(new BigDecimal("8.00"));
        g.setWeeklyHours(new BigDecimal("40.00"));
        when(globalRepo.getSingleton()).thenReturn(g);

        EffectiveCapacity out = resolver.resolveOn(memberId, on);

        assertThat(out.source()).isEqualTo(EffectiveCapacity.Source.GLOBAL);
        assertThat(out.dailyHours()).isEqualByComparingTo("8.00");
    }

    @Test
    void globalDefault_whenTeamHasNoOverride() {
        when(memberRepo.findCurrent(memberId, on)).thenReturn(Optional.empty());
        Member m = new Member();
        m.setId(memberId); m.setTeamId(teamId);
        when(members.findById(memberId)).thenReturn(Optional.of(m));
        when(teamRepo.findCurrent(teamId, on)).thenReturn(Optional.empty());
        CapacityGlobal g = new CapacityGlobal();
        g.setDailyHours(new BigDecimal("8.00"));
        g.setWeeklyHours(new BigDecimal("40.00"));
        when(globalRepo.getSingleton()).thenReturn(g);

        EffectiveCapacity out = resolver.resolveOn(memberId, on);

        assertThat(out.source()).isEqualTo(EffectiveCapacity.Source.GLOBAL);
    }
}
