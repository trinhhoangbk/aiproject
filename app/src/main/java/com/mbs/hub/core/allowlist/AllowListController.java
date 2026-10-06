package com.mbs.hub.core.allowlist;

import com.mbs.hub.core.allowlist.dto.AllowListUpsertRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/allowlist")
public class AllowListController {

    private final AllowListRepository repo;
    public AllowListController(AllowListRepository repo) { this.repo = repo; }

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
        return repo.save(e);
    }

    @DeleteMapping("/{projectKey}")
    public void delete(@PathVariable String projectKey) { repo.deleteById(projectKey); }
}
