package com.mbs.hub.sync.dedup;

import java.time.OffsetDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JiraEventDedupRepository extends JpaRepository<JiraEventDedup, JiraEventDedupKey> {

    @Modifying
    @Query("DELETE FROM JiraEventDedup d WHERE d.receivedAt < :cutoff")
    int pruneOlderThan(@Param("cutoff") OffsetDateTime cutoff);
}
