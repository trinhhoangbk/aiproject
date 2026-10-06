package com.mbs.hub.core.skill;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberSkillRepository extends JpaRepository<MemberSkill, MemberSkillKey> {
    List<MemberSkill> findByKey_MemberId(UUID memberId);
    void deleteByKey_MemberId(UUID memberId);
}
