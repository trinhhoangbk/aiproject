package com.mbs.hub.assignment;

import com.mbs.hub.assignment.dto.AssignmentRequest;
import com.mbs.hub.assignment.dto.AssignmentView;
import com.mbs.hub.security.HubUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Assignment write — AC-006.4..6 / DEC-008 / 02 API §5.7.
 * MANAGER / ADMIN. The service enforces dry-run gating; the controller shape stays
 * identical regardless, so flipping JIRA_WRITE_DRY_RUN=false is a config change
 * only — no code change.
 */
@RestController
@RequestMapping("/api/assignments")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class AssignmentController {

    private final AssignmentService service;

    public AssignmentController(AssignmentService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<AssignmentView> assign(
            @Valid @RequestBody AssignmentRequest req,
            @AuthenticationPrincipal HubUserDetails actor) {
        AssignmentView v = service.assign(req, actor != null ? actor.getMemberId() : null);
        return ResponseEntity.status(201).body(v);
    }
}
