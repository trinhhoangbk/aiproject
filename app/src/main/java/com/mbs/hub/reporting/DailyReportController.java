package com.mbs.hub.reporting;

import com.mbs.hub.reporting.dto.DailyReportView;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST surface for the daily report — AC-002.1..4.
 *
 * Access rules — AC-002.4 / DEC-014:
 * <ul>
 *   <li>MEMBER may request the report ONLY for their own memberId.</li>
 *   <li>ADMIN / MANAGER may request any memberId or leave it unset (team-wide roll-up).</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/reporting")
public class DailyReportController {

    private final DailyReportService service;

    public DailyReportController(DailyReportService service) {
        this.service = service;
    }

    @GetMapping("/daily")
    @PreAuthorize("""
            hasAnyRole('ADMIN','MANAGER')
            or (hasRole('MEMBER')
                and #memberId != null
                and principal.memberId.toString() == #memberId.toString())
            """)
    public DailyReportView daily(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) UUID memberId,
            @RequestParam(name = "excludeDiscarded", defaultValue = "true") boolean excludeDiscarded) {
        return service.compile(date, memberId, excludeDiscarded);
    }
}
