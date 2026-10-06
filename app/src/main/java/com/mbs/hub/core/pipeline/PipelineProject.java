package com.mbs.hub.core.pipeline;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(schema = "core", name = "pipeline_project")
@Getter @Setter @NoArgsConstructor
public class PipelineProject {
    @Id @GeneratedValue
    private UUID id;

    @NotBlank @Size(min = 1, max = 200)
    @Column(nullable = false)
    private String name;

    @NotBlank
    @Column(nullable = false)
    private String objective;

    @Column(name = "target_deadline", nullable = false)
    private LocalDate targetDeadline;

    @Positive
    @Column(name = "total_estimated_md", nullable = false)
    private BigDecimal totalEstimatedMd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PipelineState state = PipelineState.DRAFT;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist void onInsert() {
        var now = OffsetDateTime.now();
        createdAt = now; updatedAt = now;
    }
    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now(); }
}
