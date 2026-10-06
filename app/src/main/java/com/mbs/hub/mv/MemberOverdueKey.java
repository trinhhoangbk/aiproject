package com.mbs.hub.mv;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class MemberOverdueKey implements Serializable {
    @Column(name = "issue_key", nullable = false) private String issueKey;
    @Column(name = "member_id", nullable = false) private UUID memberId;

    @Override public boolean equals(Object o) {
        if (!(o instanceof MemberOverdueKey k)) return false;
        return Objects.equals(issueKey, k.issueKey) && Objects.equals(memberId, k.memberId);
    }
    @Override public int hashCode() { return Objects.hash(issueKey, memberId); }
}
