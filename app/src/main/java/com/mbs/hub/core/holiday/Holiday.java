package com.mbs.hub.core.holiday;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(schema = "core", name = "holiday_calendar")
@Getter @Setter @NoArgsConstructor
public class Holiday {
    @Id
    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HolidayKind kind;

    @Column(nullable = false)
    private String description;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist @PreUpdate void stamp() { updatedAt = OffsetDateTime.now(); }
}
