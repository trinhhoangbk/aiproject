package com.mbs.hub.audit;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Append-only audit record — DEC-008, F-ARCH-NEW-02 Reading A.
 * See V003__audit_schema.sql. No UPDATE/DELETE path exists in the codebase.
 */
@Entity
@Table(schema = "audit", name = "audit_log")
@Getter @Setter @NoArgsConstructor
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "occurred_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime occurredAt;

    @Column(name = "actor_member_id")
    private UUID actorMemberId;      // nullable: webhook/system-driven events have no human actor

    @Column(nullable = false)
    private String action;           // AUTH_LOGIN_OK / AUTH_LOGIN_FAIL / AUTH_LOGOUT / CAPACITY_EDIT / ...

    @Column(name = "target_type", nullable = false)
    private String targetType;       // member / capacity / holiday / allowlist / skill / assignment / webhook

    @Column(name = "target_id", nullable = false)
    private String targetId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;          // JSON string; secret masking is applied at the service

    @Column(nullable = false)
    private String result;           // 'OK' | 'FAILED'

    @Column(name = "jira_status")
    private Integer jiraStatus;      // only for Jira write audits (null everywhere else)
}
