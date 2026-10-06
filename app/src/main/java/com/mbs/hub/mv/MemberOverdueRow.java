package com.mbs.hub.mv;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(schema = "mv", name = "mv_member_overdue")
@Getter @Setter @NoArgsConstructor
public class MemberOverdueRow {
    @EmbeddedId
    private MemberOverdueKey key;

    @Column(name = "project_key",  nullable = false) private String projectKey;
    @Column(name = "due_date",     nullable = false) private LocalDate dueDate;
    @Column(name = "days_overdue", nullable = false) private int daysOverdue;
    @Column(name = "overdue_band", nullable = false) private String overdueBand;

    @Column(name = "remaining_h") private BigDecimal remainingH;

    private BigDecimal tpr;

    @Column(name = "tpr_band", nullable = false) private String tprBand;

    @Column(name = "refreshed_at", nullable = false)
    private OffsetDateTime refreshedAt;

    @PrePersist @PreUpdate void stamp() { refreshedAt = OffsetDateTime.now(); }
}
