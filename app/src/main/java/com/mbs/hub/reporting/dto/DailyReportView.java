package com.mbs.hub.reporting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Daily report payload — 02 API §5.3 / AC-002.1..4.
 *
 * The service compiles two slices for the date D in Asia/Saigon:
 *  <ul>
 *    <li>{@code done} — issues whose statusCategory became {@code done} within [D 00:00, D 23:59].
 *        Each row carries {@code discarded} per DEC-001.</li>
 *    <li>{@code worklogTotals} — sum of {@code durationSeconds} per project for the same window,
 *        plus the grand total in hours (2 d.p. half-up).</li>
 *  </ul>
 */
public record DailyReportView(
        LocalDate date,
        UUID memberId,              // null when the caller is ADMIN/MANAGER and asked for all members
        List<DoneRow> done,
        List<WorklogTotal> worklogTotals,
        BigDecimal totalWorklogHours,
        boolean excludeDiscarded      // AC-002.3 flag — default true on the controller
) {
    public record DoneRow(
            String issueKey,
            String projectKey,
            String resolution,       // null when not resolved
            boolean discarded        // DEC-001: Won't Fix / Duplicate / Invalid
    ) {}

    public record WorklogTotal(
            String projectKey,
            BigDecimal hours
    ) {}
}
