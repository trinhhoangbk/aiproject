package com.mbs.hub.audit;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin read view of the audit log — 02 API /api/audit.
 * ADMIN-only. Read-only. The service never exposes a delete endpoint.
 */
@RestController
@RequestMapping("/api/audit")
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    private final AuditEventRepository repo;

    public AuditController(AuditEventRepository repo) { this.repo = repo; }

    @GetMapping
    public List<AuditEvent> list(
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "100") int limit) {
        int bounded = Math.min(Math.max(limit, 1), 1000);
        if (action != null && !action.isBlank()) {
            return repo.findByActionOrderByOccurredAtDesc(action, PageRequest.of(0, bounded)).getContent();
        }
        return repo.findAll(PageRequest.of(0, bounded,
                org.springframework.data.domain.Sort.by("occurredAt").descending())).getContent();
    }

    @GetMapping("/by-date")
    public List<AuditEvent> byDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toExclusive) {
        return repo.findWithin(
                from.atStartOfDay().atOffset(java.time.ZoneOffset.ofHours(7)),
                toExclusive.atStartOfDay().atOffset(java.time.ZoneOffset.ofHours(7)));
    }
}
