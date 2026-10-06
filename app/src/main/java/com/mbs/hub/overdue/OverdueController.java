package com.mbs.hub.overdue;

import com.mbs.hub.overdue.dto.OverdueView;
import com.mbs.hub.overdue.dto.WarningView;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REQ-003 — Overdue radar + Early-Warning view.
 * MEMBER role sees own; Manager/Admin see any (or all when memberId omitted).
 */
@RestController
@RequestMapping("/api/overdue")
public class OverdueController {

    private final OverdueService service;

    public OverdueController(OverdueService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') "
            + "or (hasRole('MEMBER') and #memberId != null and principal.memberId.toString() == #memberId.toString())")
    public List<OverdueView> overdue(@RequestParam(required = false) UUID memberId) {
        return service.overdue(memberId);
    }

    @GetMapping("/warnings")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') "
            + "or (hasRole('MEMBER') and #memberId != null and principal.memberId.toString() == #memberId.toString())")
    public List<WarningView> warnings(@RequestParam(required = false) UUID memberId) {
        return service.warnings(memberId);
    }
}
