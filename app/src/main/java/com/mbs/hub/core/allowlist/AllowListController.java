package com.mbs.hub.core.allowlist;

import com.mbs.hub.core.allowlist.dto.AllowListUpsertRequest;
import com.mbs.hub.sync.ProjectionUpdatedEvent;
import com.mbs.hub.sync.projection.IssueProjectionRepository;
import org.springframework.context.ApplicationEventPublisher;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
@RestController
@RequestMapping("/api/allowlist")
public class AllowListController {

    private final AllowListRepository repo;
    private final IssueProjectionRepository issues;
    private final ApplicationEventPublisher events;

    public AllowListController(AllowListRepository repo,
                               IssueProjectionRepository issues,
                               ApplicationEventPublisher events) {
        this.repo = repo;
        this.issues = issues;
        this.events = events;
    }

    /** Projections cached before the allow-list change keep a stale flag otherwise. */
    private void reflag(String projectKey, boolean ok) {
        issues.updateAllowListOk(projectKey, ok);
        events.publishEvent(new ProjectionUpdatedEvent(null, projectKey));
    }

    @GetMapping
    public List<AllowListEntry> list() { return repo.findAll(); }

    @PutMapping("/{projectKey}")
    @Transactional
    public AllowListEntry upsert(@PathVariable String projectKey,
                                 @Valid @RequestBody AllowListUpsertRequest req) {
        if (!projectKey.equals(req.projectKey()))
            throw new IllegalArgumentException("Path projectKey must match body projectKey");
        AllowListEntry e = repo.findById(projectKey).orElseGet(() -> {
            AllowListEntry fresh = new AllowListEntry();
            fresh.setProjectKey(projectKey);
            return fresh;
        });
        if (req.enabled() != null) e.setEnabled(req.enabled());
        AllowListEntry saved = repo.save(e);
        reflag(projectKey, saved.isEnabled());
        return saved;
    }

    @DeleteMapping("/{projectKey}")
    @Transactional
    public void delete(@PathVariable String projectKey) {
        repo.deleteById(projectKey);
        reflag(projectKey, false);
    }
}
