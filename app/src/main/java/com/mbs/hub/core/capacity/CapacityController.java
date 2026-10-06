package com.mbs.hub.core.capacity;

import com.mbs.hub.core.capacity.dto.CapacityUpdateRequest;
import com.mbs.hub.core.capacity.dto.EffectiveCapacityView;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * Admin / Manager-only in M8. For M1 PLAN-010 we expose the endpoints without
 * a security filter; the Spring Security chain and @PreAuthorize annotations
 * come in M8 (PLAN-028/029).
 */
@RestController
@RequestMapping("/api/capacity")
public class CapacityController {

    private final CapacityGlobalRepository globalRepo;
    private final CapacityTeamRepository teamRepo;
    private final CapacityMemberRepository memberRepo;
    private final CapacityResolver resolver;
    private final Clock clock;

    public CapacityController(CapacityGlobalRepository g, CapacityTeamRepository t,
            CapacityMemberRepository m, CapacityResolver r, Clock clock) {
        this.globalRepo = g; this.teamRepo = t; this.memberRepo = m;
        this.resolver = r; this.clock = clock;
    }

    // --- GLOBAL ---
    @GetMapping("/global")
    public CapacityGlobal getGlobal() { return globalRepo.getSingleton(); }

    @PutMapping("/global")
    @Transactional
    public CapacityGlobal putGlobal(@Valid @RequestBody CapacityUpdateRequest req) {
        CapacityGlobal g = globalRepo.getSingleton();
        if (req.dailyHours()    != null) g.setDailyHours(req.dailyHours());
        if (req.weeklyHours()   != null) g.setWeeklyHours(req.weeklyHours());
        if (req.effectiveFrom() != null) g.setEffectiveFrom(req.effectiveFrom());
        return g;
    }

    // --- TEAM ---
    @PostMapping("/team/{teamId}")
    @Transactional
    public CapacityTeam addTeamOverride(@PathVariable UUID teamId,
                                        @Valid @RequestBody CapacityUpdateRequest req) {
        CapacityTeam c = new CapacityTeam();
        c.setTeamId(teamId);
        c.setDailyHours(req.dailyHours());
        c.setWeeklyHours(req.weeklyHours());
        c.setEffectiveFrom(req.effectiveFrom() != null ? req.effectiveFrom() : LocalDate.now(clock));
        return teamRepo.save(c);
    }

    // --- MEMBER ---
    @PostMapping("/member/{memberId}")
    @Transactional
    public CapacityMember addMemberOverride(@PathVariable UUID memberId,
                                            @Valid @RequestBody CapacityUpdateRequest req) {
        CapacityMember c = new CapacityMember();
        c.setMemberId(memberId);
        c.setDailyHours(req.dailyHours());
        c.setWeeklyHours(req.weeklyHours());
        c.setEffectiveFrom(req.effectiveFrom() != null ? req.effectiveFrom() : LocalDate.now(clock));
        return memberRepo.save(c);
    }

    // --- RESOLVED (view) ---
    @GetMapping("/effective/{memberId}")
    public EffectiveCapacityView effective(@PathVariable UUID memberId,
                                           @RequestParam(required = false) LocalDate on) {
        EffectiveCapacity e = (on == null)
                ? resolver.resolveToday(memberId)
                : resolver.resolveOn(memberId, on);
        return EffectiveCapacityView.of(e);
    }
}
