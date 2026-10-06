package com.mbs.hub.sync.dedup;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class JiraEventDedupKey implements Serializable {
    @Column(name = "jira_event_id", nullable = false)
    private String jiraEventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Override public boolean equals(Object o) {
        if (!(o instanceof JiraEventDedupKey k)) return false;
        return Objects.equals(jiraEventId, k.jiraEventId) && Objects.equals(eventType, k.eventType);
    }
    @Override public int hashCode() { return Objects.hash(jiraEventId, eventType); }
}
