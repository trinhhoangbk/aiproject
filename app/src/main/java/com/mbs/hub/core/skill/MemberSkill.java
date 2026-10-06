package com.mbs.hub.core.skill;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(schema = "core", name = "member_skill")
@Getter @Setter @NoArgsConstructor
public class MemberSkill {
    @EmbeddedId
    private MemberSkillKey key;
}
