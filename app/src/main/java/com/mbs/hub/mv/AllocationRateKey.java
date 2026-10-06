package com.mbs.hub.mv;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class AllocationRateKey implements Serializable {
    @Column(name = "member_id", nullable = false) private UUID memberId;
    @Column(nullable = false)                     private String horizon;
    @Column(name = "window_start", nullable = false) private LocalDate windowStart;

    @Override public boolean equals(Object o) {
        if (!(o instanceof AllocationRateKey k)) return false;
        return Objects.equals(memberId, k.memberId)
            && Objects.equals(horizon, k.horizon)
            && Objects.equals(windowStart, k.windowStart);
    }
    @Override public int hashCode() { return Objects.hash(memberId, horizon, windowStart); }
}
