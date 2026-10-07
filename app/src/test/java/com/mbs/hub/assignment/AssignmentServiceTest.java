package com.mbs.hub.assignment;

import static com.mbs.hub.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mbs.hub.assignment.dto.AssignmentRequest;
import com.mbs.hub.assignment.dto.AssignmentView;
import com.mbs.hub.audit.AuditService;
import com.mbs.hub.core.member.Member;
import com.mbs.hub.core.member.MemberRepository;
import com.mbs.hub.core.member.Role;
import com.mbs.hub.jira.client.JiraClientException;
import com.mbs.hub.jira.write.JiraWriteClient;
import com.mbs.hub.jira.write.WriteOutcome;
import com.mbs.hub.sync.projection.IssueProjection;
import com.mbs.hub.sync.projection.IssueProjectionRepository;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** TC-AS-01..05 — AC-006.4 / .5 / .6, DEC-008. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AssignmentServiceTest {

    @Mock MemberRepository members;
    @Mock IssueProjectionRepository issues;
    @Mock JiraWriteClient writeClient;
    @Mock AuditService audit;

    Member manager = member("acc-M", Role.MANAGER);
    Member target = member("acc-T", Role.MEMBER);
    AssignmentService service;

    @BeforeEach
    void setUp() {
        manager.setDisplayName("Hoang Manager");
        when(members.findById(manager.getId())).thenReturn(Optional.of(manager));
        when(members.findById(target.getId())).thenReturn(Optional.of(target));
        IssueProjection current = issue("KAN-1", "8", null);
        current.setAssigneeAccountId("acc-old");
        when(issues.findById("KAN-1")).thenReturn(Optional.of(current));
        when(writeClient.assignIssue(anyString(), anyString()))
                .thenReturn(WriteOutcome.dryRun("PUT", "/rest/api/3/issue/KAN-1/assignee"));
        when(writeClient.commentIssue(anyString(), anyMap()))
                .thenReturn(WriteOutcome.dryRun("POST", "/rest/api/3/issue/KAN-1/comment"));
        service = new AssignmentService(members, issues, writeClient, audit, clock());
    }

    AssignmentRequest req() {
        return new AssignmentRequest("KAN-1", target.getId(), "Cân đối tải", null);
    }

    @Test
    void TC_AS_01_inactiveTargetRejectedBeforeJira() {
        target.setActive(false);
        assertThatThrownBy(() -> service.assign(req(), manager.getId()))
                .isInstanceOf(IllegalArgumentException.class);
        verify(writeClient, never()).assignIssue(anyString(), anyString());
    }

    @Test
    void TC_AS_02_dryRunAssignmentIsAuditedOk() {
        AssignmentView v = service.assign(req(), manager.getId());

        assertThat(v.dryRun()).isTrue();
        assertThat(v.targetJiraAccountId()).isEqualTo("acc-T");
        assertThat(v.previousJiraAccountId()).isEqualTo("acc-old");
        verify(writeClient).assignIssue("KAN-1", "acc-T");
        verify(audit).ok(eq(manager.getId()), eq("ASSIGNMENT"), eq("issue"), eq("KAN-1"), anyMap());
    }

    @Test
    void TC_AS_03_jira403PropagatesIsAuditedFailedAndNoComment() {
        when(writeClient.assignIssue(anyString(), anyString()))
                .thenThrow(new JiraClientException(403, "Forbidden"));

        assertThatThrownBy(() -> service.assign(req(), manager.getId()))
                .isInstanceOf(JiraClientException.class);
        verify(audit).failed(eq(manager.getId()), eq("ASSIGNMENT"), eq("issue"), eq("KAN-1"), anyMap());
        verify(writeClient, never()).commentIssue(anyString(), anyMap());
        verify(audit, never()).ok(any(), anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    void TC_AS_04_commentFailureDoesNotUndoAssignment() {
        when(writeClient.commentIssue(anyString(), anyMap()))
                .thenReturn(new WriteOutcome("POST", "/rest/api/3/issue/KAN-1/comment", 500, false, null));

        AssignmentView v = service.assign(req(), manager.getId());

        assertThat(v.issueKey()).isEqualTo("KAN-1");
        verify(audit).ok(eq(manager.getId()), eq("ASSIGNMENT"), eq("issue"), eq("KAN-1"), anyMap());
    }

    @SuppressWarnings("unchecked")
    @Test
    void TC_AS_05_commentUsesTheApprovedWording() throws Exception {
        ArgumentCaptor<Map<String, Object>> adf = ArgumentCaptor.forClass(Map.class);

        service.assign(req(), manager.getId());

        verify(writeClient).commentIssue(eq("KAN-1"), adf.capture());
        String rendered = new ObjectMapper().writeValueAsString(adf.getValue());
        assertThat(rendered).contains("Task reassigned via Resource Balancing Hub by Hoang Manager");
    }
}
