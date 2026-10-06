package com.mbs.hub.balancing;

import com.mbs.hub.balancing.dto.BalancingView;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Balancing suggestions — AC-006.3 / B-RULE-04 / 02 API §5.6.
 * MANAGER / ADMIN only. Read-only: the caller decides who to assign and sends
 * that choice to {@code /api/assignments} (PLAN-027).
 */
@RestController
@RequestMapping("/api/balancing")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class BalancingController {

    private final BalancingService service;

    public BalancingController(BalancingService service) { this.service = service; }

    @GetMapping("/{pipelineId}")
    public BalancingView forPipeline(@PathVariable UUID pipelineId) {
        return service.suggestFor(pipelineId);
    }
}
