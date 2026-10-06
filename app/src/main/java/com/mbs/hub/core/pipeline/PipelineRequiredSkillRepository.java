package com.mbs.hub.core.pipeline;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PipelineRequiredSkillRepository
        extends JpaRepository<PipelineRequiredSkill, PipelineRequiredSkillKey> {
    List<PipelineRequiredSkill> findByKey_PipelineId(UUID pipelineId);
    void deleteByKey_PipelineId(UUID pipelineId);
}
