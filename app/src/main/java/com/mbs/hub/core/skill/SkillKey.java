package com.mbs.hub.core.skill;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Composite PK for skill_taxonomy and FK target for member_skill / pipeline_required_skill. */
@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class SkillKey implements Serializable {
    @Column(nullable = false, length = 50)
    private String l1;

    /** Empty string when the row is an L1-only entry (schema uses default '' for simpler PK). */
    @Column(nullable = false, length = 50)
    private String l2 = "";

    @Override public boolean equals(Object o) {
        if (!(o instanceof SkillKey other)) return false;
        return Objects.equals(l1, other.l1) && Objects.equals(l2, other.l2);
    }
    @Override public int hashCode() { return Objects.hash(l1, l2); }
}
