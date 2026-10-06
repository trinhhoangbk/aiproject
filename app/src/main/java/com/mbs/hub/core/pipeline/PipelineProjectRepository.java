package com.mbs.hub.core.pipeline;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PipelineProjectRepository extends JpaRepository<PipelineProject, UUID> {
    List<PipelineProject> findByStateOrderByTargetDeadlineAsc(PipelineState state);
}
