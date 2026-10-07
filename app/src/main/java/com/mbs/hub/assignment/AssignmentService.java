package com.mbs.hub.assignment;

import com.mbs.hub.assignment.dto.AssignmentRequest;
import com.mbs.hub.assignment.dto.AssignmentView;
import com.mbs.hub.audit.AuditService;
import com.mbs.hub.core.member.Member;
import com.mbs.hub.core.member.MemberRepository;
import com.mbs.hub.jira.client.JiraClientException;
import com.mbs.hub.jira.write.JiraWriteClient;
import com.mbs.hub.jira.write.WriteOutcome;
import com.mbs.hub.sync.projection.IssueProjection;
import com.mbs.hub.sync.projection.IssueProjectionRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PLAN-027 — Assignment service.
 *
 * <p>Workflow (AC-006.4..6):
 * <ol>
 *   <li>Resolve the target Member → its {@code jiraAccountId}.</li>
 *   <li>Look up the current assignee from {@code jira.issue_projection} (fallback: unknown).</li>
 *   <li>Call {@link JiraWriteClient#assignIssue} — short-circuits as DRY_RUN when
 *       {@code JIRA_WRITE_DRY_RUN=true}.</li>
 *   <li>If the assignee PUT succeeds, call {@link JiraWriteClient#commentIssue} with an
 *       ADF comment naming the actor and the reason (DEC-008).</li>
 *   <li>Audit both calls through {@link AuditService}, including the dry-run flag so the
 *       audit trail still records intent even when nothing left the JVM.</li>
 *   <li>On non-429 Jira failure (CV-04) the service does NOT update the projection — the
 *       reconciler eventually re-reads the real Jira state anyway. On 429 the caller
 *       sees the exception and should retry later.</li>
 * </ol>
 */
@Service
public class AssignmentService {

    private static final Logger log = LoggerFactory.getLogger(AssignmentService.class);

    private final MemberRepository members;
    private final IssueProjectionRepository issues;
    private final JiraWriteClient writeClient;
    private final AuditService audit;
    private final Clock clock;

    public AssignmentService(MemberRepository members,
                             IssueProjectionRepository issues,
                             JiraWriteClient writeClient,
                             AuditService audit,
                             Clock clock) {
        this.members = members;
        this.issues = issues;
        this.writeClient = writeClient;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public AssignmentView assign(AssignmentRequest req, UUID requestedBy) {
        Member target = members.findById(req.targetMemberId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "target member not found: " + req.targetMemberId()));
        if (!target.isActive()) {
            throw new IllegalArgumentException("target member is inactive: " + req.targetMemberId());
        }

        String targetAccount = target.getJiraAccountId();
        String previousAccount = issues.findById(req.issueKey())
                .map(IssueProjection::getAssigneeAccountId)
                .orElse(null);

        Map<String, Object> auditPayload = new HashMap<>();
        auditPayload.put("issueKey", req.issueKey());
        auditPayload.put("targetMemberId", req.targetMemberId());
        auditPayload.put("targetJiraAccountId", targetAccount);
        auditPayload.put("previousJiraAccountId", previousAccount);
        auditPayload.put("pipelineId", req.pipelineId());
        auditPayload.put("reason", req.reason());

        try {
            WriteOutcome assignResult = writeClient.assignIssue(req.issueKey(), targetAccount);
            auditPayload.put("assigneeCall", Map.of(
                    "method", assignResult.method(),
                    "path", assignResult.path(),
                    "statusCode", assignResult.statusCode(),
                    "dryRun", assignResult.dryRun()));
            if (!assignResult.ok()) {
                audit.failed(requestedBy, "ASSIGNMENT", "issue", req.issueKey(), auditPayload);
                throw new JiraClientException(assignResult.statusCode(),
                        "assignee call returned " + assignResult.statusCode());
            }

            Map<String, Object> adf = adfComment(actorName(requestedBy), req.reason(), req.pipelineId());
            WriteOutcome commentResult = writeClient.commentIssue(req.issueKey(), adf);
            auditPayload.put("commentCall", Map.of(
                    "method", commentResult.method(),
                    "path", commentResult.path(),
                    "statusCode", commentResult.statusCode(),
                    "dryRun", commentResult.dryRun()));
            // A failing comment is logged but does NOT roll back the assignee change —
            // Jira is the SoR and the assignee is already updated. The audit record carries both.
            if (!commentResult.ok()) {
                log.warn("Assignment comment failed status={} on {} — assignee transfer kept",
                        commentResult.statusCode(), req.issueKey());
            }

            audit.ok(requestedBy, "ASSIGNMENT", "issue", req.issueKey(), auditPayload);
            return new AssignmentView(
                    req.issueKey(),
                    target.getId(),
                    targetAccount,
                    previousAccount,
                    requestedBy,
                    OffsetDateTime.now(clock),
                    assignResult.dryRun(),
                    assignResult.dryRun() ? "DRY_RUN — no HTTP call issued (PLAN-COND-01)" : null);

        } catch (RuntimeException e) {
            auditPayload.put("exception", e.getClass().getSimpleName());
            auditPayload.put("message", e.getMessage());
            audit.failed(requestedBy, "ASSIGNMENT", "issue", req.issueKey(), auditPayload);
            throw e;
        }
    }

    /** Display name of the requesting manager for the Jira comment (AC-006.4). */
    private String actorName(UUID actor) {
        if (actor == null) return "system";
        return members.findById(actor).map(Member::getDisplayName).orElse(actor.toString());
    }

    /**
     * ADF comment. The first sentence is the AC-006.4 wording verbatim
     * ("Task reassigned via Resource Balancing Hub by [Manager Name]"); the reason
     * and pipeline follow so the Jira history explains the move.
     */
    Map<String, Object> adfComment(String managerName, String reason, UUID pipelineId) {
        String text = "Task reassigned via Resource Balancing Hub by " + managerName + "."
                + (reason == null || reason.isBlank() ? "" : " Reason: " + reason + ".")
                + (pipelineId != null ? " Pipeline: " + pipelineId + "." : "");
        return Map.of(
                "type", "doc",
                "version", 1,
                "content", List.of(Map.of(
                        "type", "paragraph",
                        "content", List.of(Map.of(
                                "type", "text",
                                "text", text)))));
    }
}
