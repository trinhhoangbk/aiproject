package com.mbs.hub.core.skill;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillTagRepository extends JpaRepository<SkillTag, SkillKey> {
    List<SkillTag> findByActiveTrue();
}
