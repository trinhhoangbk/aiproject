package com.mbs.hub.core.pipeline;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(schema = "core", name = "pipeline_required_skill")
@Getter @Setter @NoArgsConstructor
public class PipelineRequiredSkill {
    @EmbeddedId
    private PipelineRequiredSkillKey key;
}
