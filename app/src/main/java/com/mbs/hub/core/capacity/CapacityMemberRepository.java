package com.mbs.hub.core.capacity;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CapacityMemberRepository extends JpaRepository<CapacityMember, UUID> {

    /** Current member-tier override: latest row with effective_from <= :on. */
    @Query("""
        SELECT c FROM CapacityMember c
        WHERE c.memberId = :memberId
          AND c.effectiveFrom <= :on
        ORDER BY c.effectiveFrom DESC
        LIMIT 1
    """)
    Optional<CapacityMember> findCurrent(@Param("memberId") UUID memberId, @Param("on") LocalDate on);
}
