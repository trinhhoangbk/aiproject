package com.mbs.hub.eta.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * ETA payload — AC-004.1 / AC-004.2 / F-03.
 *
 * α (allocationShare) is the share of the member's last-N-WD worklog that landed on project P
 * (0..1). ETA date is counted via B-RULE-01 working-day arithmetic.
 */
public record EtaView(
        UUID memberId,
        int observationWindowWorkingDays,     // N — default 10
        LocalDate anchor,                     // "today" in Asia/Saigon
        List<ProjectEta> projects
) {
    public record ProjectEta(
            String projectKey,
            BigDecimal remainingHours,
            BigDecimal remainingMd,          // remainingHours / member standard daily hours
            BigDecimal allocationShare,      // α_P in [0,1], 3 d.p. half-up
            Integer rwd,                     // required working days (null when α_P == 0)
            LocalDate etaDate,               // null when α_P == 0 — "no observed capacity on P"
            String note                      // when etaDate is null — e.g. "no_worklog_on_project_last_10_wd"
    ) {}
}
