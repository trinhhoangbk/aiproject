package com.mbs.hub.core.allowlist;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AllowListRepository extends JpaRepository<AllowListEntry, String> {
    List<AllowListEntry> findByEnabledTrue();
    boolean existsByProjectKeyAndEnabledTrue(String projectKey);
}
