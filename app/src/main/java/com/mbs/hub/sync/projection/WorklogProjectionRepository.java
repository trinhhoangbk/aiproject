package com.mbs.hub.sync.projection;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WorklogProjectionRepository extends JpaRepository<WorklogProjection, Long> {
    void deleteByIssueKey(String issueKey);
}
