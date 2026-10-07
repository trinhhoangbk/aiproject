package com.mbs.hub.web;

import static com.mbs.hub.TestFixtures.member;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mbs.hub.assignment.AssignmentController;
import com.mbs.hub.assignment.AssignmentService;
import com.mbs.hub.config.PasswordConfig;
import com.mbs.hub.core.member.MemberController;
import com.mbs.hub.core.member.MemberRepository;
import com.mbs.hub.core.member.Role;
import com.mbs.hub.core.pipeline.PipelineController;
import com.mbs.hub.core.pipeline.PipelineService;
import com.mbs.hub.jira.client.JiraClientException;
import com.mbs.hub.reporting.DailyReportController;
import com.mbs.hub.reporting.DailyReportService;
import com.mbs.hub.security.HubUserDetails;
import com.mbs.hub.security.HubUserDetailsService;
import com.mbs.hub.security.SecurityConfig;
import com.mbs.hub.workload.WorkloadController;
import com.mbs.hub.workload.WorkloadService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Spring-MVC security slice — TC-SEC-01..06 (AC-008.1/.2/.3, AC-002.4, CV-07),
 * TC-PL-03 (AC-005.2) and TC-AS-06 (AC-006.5 / CV-04).
 * Services are mocked; the real {@link SecurityConfig} and method security apply.
 */
@WebMvcTest(controllers = {WorkloadController.class, MemberController.class,
        DailyReportController.class, AssignmentController.class, PipelineController.class})
@Import({SecurityConfig.class, PasswordConfig.class})
class ApiSecurityWebTest {

    @Autowired MockMvc mvc;

    @MockBean WorkloadService workloadService;
    @MockBean MemberRepository memberRepository;
    @MockBean DailyReportService dailyReportService;
    @MockBean AssignmentService assignmentService;
    @MockBean PipelineService pipelineService;
    @MockBean HubUserDetailsService userDetailsService;

    static HubUserDetails principal(Role role) {
        return new HubUserDetails(member("acc-" + role.name().toLowerCase(), role));
    }

    @Test
    void TC_SEC_01_memberSeesOwnWorkload() throws Exception {
        HubUserDetails me = principal(Role.MEMBER);
        mvc.perform(get("/api/workload/" + me.getMemberId()).with(user(me)))
           .andExpect(status().isOk());
    }

    @Test
    void TC_SEC_02_memberCannotSeePeerWorkload() throws Exception {
        mvc.perform(get("/api/workload/" + UUID.randomUUID()).with(user(principal(Role.MEMBER))))
           .andExpect(status().isForbidden());
    }

    @Test
    void TC_SEC_03_managerSeesAnyWorkload() throws Exception {
        mvc.perform(get("/api/workload/" + UUID.randomUUID()).with(user(principal(Role.MANAGER))))
           .andExpect(status().isOk());
    }

    @Test
    void TC_SEC_04_memberCannotOpenRoster() throws Exception {
        mvc.perform(get("/api/roster").with(user(principal(Role.MEMBER))))
           .andExpect(status().isForbidden());
    }

    @Test
    void TC_SEC_05_anonymousIsNotServed() throws Exception {
        int code = mvc.perform(get("/api/workload/" + UUID.randomUUID()))
                .andReturn().getResponse().getStatus();
        assertThat(code).isIn(302, 401);
    }

    @Test
    void TC_SEC_06_memberDailyReportWithoutOwnIdIsForbidden() throws Exception {
        mvc.perform(get("/api/reporting/daily").with(user(principal(Role.MEMBER))))
           .andExpect(status().isForbidden());
    }

    @Test
    void TC_PL_03_pipelineWithoutNameIsRejected() throws Exception {
        String body = """
                {"objective":"x","targetDeadline":"2026-11-30","totalEstimatedMd":5,
                 "requiredSkills":[{"l1":"BACKEND"}]}
                """;
        mvc.perform(post("/api/pipeline").with(user(principal(Role.MANAGER)))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
           .andExpect(status().isBadRequest());
        verify(pipelineService, never()).create(any(), any());
    }

    @Test
    void TC_AS_06_jiraRejectionIsReported422ProblemDetails() throws Exception {
        when(assignmentService.assign(any(), any()))
                .thenThrow(new JiraClientException(403, "Assignee not permitted by workflow"));
        String body = """
                {"issueKey":"KAN-1","targetMemberId":"%s","reason":"rebalance"}
                """.formatted(UUID.randomUUID());

        mvc.perform(post("/api/assignments").with(user(principal(Role.MANAGER)))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
           .andExpect(status().isUnprocessableEntity())
           .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

}
