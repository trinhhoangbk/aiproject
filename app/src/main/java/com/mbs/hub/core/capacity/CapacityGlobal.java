package com.mbs.hub.core.capacity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Global default capacity — single-row singleton per DEC-004.
 * Row id is always 1 (CHECK constraint in V001).
 */
@Entity
@Table(schema = "core", name = "capacity_global")
@Getter @Setter @NoArgsConstructor
public class CapacityGlobal {
    @Id
    private Short id = (short) 1;

    @Column(name = "daily_hours", nullable = false)
    private BigDecimal dailyHours;

    @Column(name = "weekly_hours", nullable = false)
    private BigDecimal weeklyHours;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist @PreUpdate void stamp() { updatedAt = OffsetDateTime.now(); }
}
