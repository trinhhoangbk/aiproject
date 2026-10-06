package com.mbs.hub.core.pipeline;

import com.mbs.hub.core.pipeline.dto.PipelineRequest;
import com.mbs.hub.core.pipeline.dto.PipelineView;
import com.mbs.hub.security.HubUserDetails;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Pipeline CRUD — AC-005.* / 02 API §5.5.
 * MANAGER / ADMIN only — DEC-005 (allow-list extends to pipeline management).
 */
@RestController
@RequestMapping("/api/pipeline")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class PipelineController {

    private final PipelineService service;

    public PipelineController(PipelineService service) { this.service = service; }

    @GetMapping
    public List<PipelineView> list(@RequestParam(required = false) PipelineState state) {
        return service.list(state);
    }

    @GetMapping("/{id}")
    public PipelineView get(@PathVariable UUID id) { return service.get(id); }

    @PostMapping
    public ResponseEntity<PipelineView> create(@Valid @RequestBody PipelineRequest req,
                                               @AuthenticationPrincipal HubUserDetails actor) {
        PipelineView v = service.create(req, actor != null ? actor.getMemberId() : null);
        return ResponseEntity.status(201).body(v);
    }

    @PutMapping("/{id}")
    public PipelineView update(@PathVariable UUID id, @Valid @RequestBody PipelineRequest req) {
        return service.update(id, req);
    }

    @PatchMapping("/{id}/state")
    public PipelineView transition(@PathVariable UUID id, @RequestParam PipelineState next) {
        return service.transition(id, next);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
