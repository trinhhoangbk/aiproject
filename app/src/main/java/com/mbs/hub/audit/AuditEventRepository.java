package com.mbs.hub.audit;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Append-only repository. The entity carries NO setter path for id/occurredAt and
 * no delete/update methods are exposed beyond the base JpaRepository — which the
 * {@code AuditService} is the sole caller of (for save), and the admin controller
 * is the sole caller of (for query). Nothing in production calls delete.
 */
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    Page<AuditEvent> findByActionOrderByOccurredAtDesc(String action, Pageable p);

    @Query("""
           SELECT a FROM AuditEvent a
            WHERE a.occurredAt >= :from AND a.occurredAt < :toExclusive
            ORDER BY a.occurredAt DESC
           """)
    List<AuditEvent> findWithin(@Param("from") OffsetDateTime from,
                                @Param("toExclusive") OffsetDateTime toExclusive);
}
