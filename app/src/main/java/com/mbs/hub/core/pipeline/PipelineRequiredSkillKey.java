package com.mbs.hub.core.pipeline;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class PipelineRequiredSkillKey implements Serializable {
    @Column(name = "pipeline_id", nullable = false)
    private UUID pipelineId;

    @Column(nullable = false, length = 50)
    private String l1;

    @Column(nullable = false, length = 50)
    private String l2 = "";

    @Override public boolean equals(Object o) {
        if (!(o instanceof PipelineRequiredSkillKey other)) return false;
        return Objects.equals(pipelineId, other.pipelineId)
            && Objects.equals(l1, other.l1) && Objects.equals(l2, other.l2);
    }
    @Override public int hashCode() { return Objects.hash(pipelineId, l1, l2); }
}
