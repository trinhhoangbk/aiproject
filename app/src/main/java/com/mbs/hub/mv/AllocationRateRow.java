package com.mbs.hub.mv;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(schema = "mv", name = "mv_allocation_rate")
@Getter @Setter @NoArgsConstructor
public class AllocationRateRow {
    @EmbeddedId
    private AllocationRateKey key;

    @Column(name = "window_end", nullable = false)
    private LocalDate windowEnd;

    @Column(name = "standard_md",     nullable = false) private BigDecimal standardMd;
    @Column(name = "committed_md",    nullable = false) private BigDecimal committedMd;
    @Column(name = "available_md",    nullable = false) private BigDecimal availableMd;
    @Column(name = "allocation_rate", nullable = false) private BigDecimal allocationRate;

    @Column(nullable = false) private String band;

    @Column(name = "refreshed_at", nullable = false)
    private OffsetDateTime refreshedAt;

    @PrePersist @PreUpdate void stamp() { refreshedAt = OffsetDateTime.now(); }
}
