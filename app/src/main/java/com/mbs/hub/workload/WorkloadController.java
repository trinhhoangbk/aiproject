package com.mbs.hub.workload;

import com.mbs.hub.workload.dto.WorkloadView;
import com.mbs.hub.workload.dto.WorkloadView.OverloadView;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REQ-001 workload / overload view. MEMBER sees own data only; Manager/Admin see any.
 */
@RestController
@RequestMapping("/api/workload")
public class WorkloadController {

    private final WorkloadService service;

    public WorkloadController(WorkloadService service) { this.service = service; }

    @GetMapping("/{memberId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') "
            + "or (hasRole('MEMBER') and principal.memberId.toString() == #memberId.toString())")
    public WorkloadView workload(
            @PathVariable UUID memberId,
            @RequestParam(name = "window", defaultValue = "week") String window,
            @RequestParam(name = "anchor", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate anchor) {
        return service.loadWorkload(memberId, window, anchor);
    }

    @GetMapping("/{memberId}/overload")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') "
            + "or (hasRole('MEMBER') and principal.memberId.toString() == #memberId.toString())")
    public OverloadView overload(@PathVariable UUID memberId) {
        return service.loadOverload(memberId);
    }
}
