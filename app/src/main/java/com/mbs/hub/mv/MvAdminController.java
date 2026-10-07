package com.mbs.hub.mv;

import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Operator escape hatch — recompute the heatmap / overdue tables now instead of
 * waiting for the next Jira event or the 02:30 nightly rebuild (PLAN-019).
 */
@RestController
@RequestMapping("/api/admin/mv")
@PreAuthorize("hasRole('ADMIN')")
public class MvAdminController {

    private final MaterializedViewRefresher refresher;

    public MvAdminController(MaterializedViewRefresher refresher) { this.refresher = refresher; }

    @PostMapping("/refresh")
    public Map<String, String> refresh() {
        refresher.refreshAll();
        return Map.of("status", "refreshed");
    }
}
