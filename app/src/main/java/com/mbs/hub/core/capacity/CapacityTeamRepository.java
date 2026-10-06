package com.mbs.hub.core.capacity;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CapacityTeamRepository extends JpaRepository<CapacityTeam, UUID> {

    /** Current team-tier override: latest row with effective_from <= :on. */
    @Query("""
        SELECT c FROM CapacityTeam c
        WHERE c.teamId = :teamId
          AND c.effectiveFrom <= :on
        ORDER BY c.effectiveFrom DESC
        LIMIT 1
    """)
    Optional<CapacityTeam> findCurrent(@Param("teamId") UUID teamId, @Param("on") LocalDate on);
}
