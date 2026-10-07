package com.mbs.hub;

import com.mbs.hub.core.capacity.EffectiveCapacity;
import com.mbs.hub.core.member.Member;
import com.mbs.hub.core.member.Role;
import com.mbs.hub.sync.projection.IssueProjection;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

/** Shared test data for 10-TESTING Step-10 cases. "Today" = Wed 2026-10-07, Asia/Saigon. */
public final class TestFixtures {

    private TestFixtures() {}

    public static final ZoneId ICT = ZoneId.of("Asia/Saigon");
    public static final LocalDate TODAY = LocalDate.of(2026, 10, 7);

    /** 10:00 ICT on {@code day}. */
    public static Clock clockOn(LocalDate day) {
        return Clock.fixed(day.atTime(10, 0).atZone(ICT).toInstant(), ICT);
    }

    public static Clock clock() { return clockOn(TODAY); }

    public static EffectiveCapacity eightHourDay() {
        return new EffectiveCapacity(new BigDecimal("8.00"), new BigDecimal("40.00"),
                EffectiveCapacity.Source.GLOBAL);
    }

    public static Member member(String accountId, Role role) {
        Member m = new Member();
        m.setId(UUID.randomUUID());
        m.setJiraAccountId(accountId);
        m.setDisplayName("Member " + accountId);
        m.setEmail(accountId + "@example.com");
        m.setRole(role);
        m.setActive(true);
        m.setPasswordHash("{noop}x");
        return m;
    }

    public static IssueProjection issue(String key, String remainingH, LocalDate due) {
        IssueProjection i = new IssueProjection();
        i.setIssueKey(key);
        i.setProjectKey(key.substring(0, key.indexOf('-')));
        i.setStatus("In Progress");
        i.setStatusCategory("indeterminate");
        i.setRemainingEstimateH(remainingH == null ? null : new BigDecimal(remainingH));
        i.setDueDate(due);
        i.setAllowListOk(true);
        return i;
    }

    /** Mon–Fri working days in [from, toExclusive), no holidays. */
    public static int weekdays(LocalDate from, LocalDate toExclusive) {
        int n = 0;
        for (LocalDate d = from; d.isBefore(toExclusive); d = d.plusDays(1)) {
            if (isWeekday(d)) n++;
        }
        return n;
    }

    public static boolean isWeekday(LocalDate d) {
        return d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY;
    }
}
