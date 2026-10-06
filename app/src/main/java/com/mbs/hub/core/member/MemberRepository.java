package com.mbs.hub.core.member;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, UUID> {
    Optional<Member> findByEmail(String email);
    Optional<Member> findByJiraAccountId(String jiraAccountId);
    List<Member> findByTeamIdAndActiveTrue(UUID teamId);
    boolean existsByEmail(String email);
    boolean existsByJiraAccountId(String jiraAccountId);
}
