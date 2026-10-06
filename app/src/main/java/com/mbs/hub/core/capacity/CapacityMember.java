package com.mbs.hub.core.capacity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Member-tier capacity override (DEC-004 highest-wins). History keyed by effective_from. */
@Entity
@Table(schema = "core", name = "capacity_member")
@Getter @Setter @NoArgsConstructor
public class CapacityMember {
    @Id @GeneratedValue
    private UUID id;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

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
