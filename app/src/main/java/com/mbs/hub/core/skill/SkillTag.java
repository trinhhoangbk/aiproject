package com.mbs.hub.core.skill;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** DEC-007 controlled 2-level taxonomy row. */
@Entity
@Table(schema = "core", name = "skill_taxonomy")
@Getter @Setter @NoArgsConstructor
public class SkillTag {
    @EmbeddedId
    private SkillKey key;

    @Column(nullable = false)
    private boolean active = true;
}
