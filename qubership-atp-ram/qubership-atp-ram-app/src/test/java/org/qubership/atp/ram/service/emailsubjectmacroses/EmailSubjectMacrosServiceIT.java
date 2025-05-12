/*
 * # Copyright 2024-2025 NetCracker Technology Corporation
 * #
 * # Licensed under the Apache License, Version 2.0 (the "License");
 * # you may not use this file except in compliance with the License.
 * # You may obtain a copy of the License at
 * #
 * #      http://www.apache.org/licenses/LICENSE-2.0
 * #
 * # Unless required by applicable law or agreed to in writing, software
 * # distributed under the License is distributed on an "AS IS" BASIS,
 * # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * # See the License for the specific language governing permissions and
 * # limitations under the License.
 */

package org.qubership.atp.ram.service.emailsubjectmacroses;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.qubership.atp.ram.service.emailsubjectmacros.EmailSubjectMacrosEnum.CURRENT_USER;
import static org.qubership.atp.ram.service.emailsubjectmacros.EmailSubjectMacrosEnum.EXECUTION_REQUEST_NAME;
import static org.qubership.atp.ram.service.emailsubjectmacros.EmailSubjectMacrosEnum.EXECUTION_REQUEST_STATUS;
import static org.qubership.atp.ram.service.emailsubjectmacros.EmailSubjectMacrosEnum.FAIL_RATE;
import static org.qubership.atp.ram.service.emailsubjectmacros.EmailSubjectMacrosEnum.PASSED_PLUS_WARNING_RATE;
import static org.qubership.atp.ram.service.emailsubjectmacros.EmailSubjectMacrosEnum.PASS_RATE;
import static org.qubership.atp.ram.service.emailsubjectmacros.EmailSubjectMacrosEnum.PROJECT_NAME;
import static org.qubership.atp.ram.service.emailsubjectmacros.EmailSubjectMacrosEnum.TEST_PLAN_NAME;
import static org.qubership.atp.ram.service.emailsubjectmacros.EmailSubjectMacrosEnum.WARNING_RATE;
import static org.qubership.atp.ram.utils.StreamUtils.extractFields;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.qubership.atp.ram.config.EmailSubjectMacrosTestConfig;
import org.qubership.atp.ram.dto.response.ExecutionSummaryResponse;
import org.qubership.atp.ram.dto.response.ProjectDataResponse;
import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.TestPlan;
import org.qubership.atp.ram.models.UserInfo;
import org.qubership.atp.ram.service.emailsubjectmacros.EmailSubjectMacrosEnum;
import org.qubership.atp.ram.service.emailsubjectmacros.EmailSubjectMacrosService;
import org.qubership.atp.ram.service.rest.dto.EmailSubjectMacrosResponse;
import org.qubership.atp.ram.services.CatalogueService;
import org.qubership.atp.ram.services.EnvironmentsService;
import org.qubership.atp.ram.services.ExecutionRequestService;
import org.qubership.atp.ram.services.JointExecutionRequestService;
import org.qubership.atp.ram.services.ReportService;
import org.qubership.atp.ram.services.TestRunService;
import org.qubership.atp.ram.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = EmailSubjectMacrosTestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"spring.cloud.consul.config.enabled=false"})
public class EmailSubjectMacrosServiceIT {

    @Autowired
    private EmailSubjectMacrosService service;

    @MockBean
    private CatalogueService catalogueService;

    @MockBean
    private ExecutionRequestService executionRequestService;

    @MockBean
    private TestRunService testRunService;

    @MockBean
    private ReportService reportService;

    @MockBean
    private UserService userService;

    @MockBean
    private JointExecutionRequestService jointExecutionRequestService;

    @MockBean
    private EnvironmentsService environmentsService;

    @Test
    public void resolveSubjectMacros_testAllMacrosResolve_shouldBeSuccessfullyResolved() {
        // given
        final UUID projectId = UUID.randomUUID();
        final UUID executionRequestId = UUID.randomUUID();
        final String projectName = "CPQ";

        final UserInfo userInfo = new UserInfo();
        userInfo.setUsername("username");

        final TestPlan testPlan = new TestPlan();
        testPlan.setUuid(UUID.randomUUID());
        testPlan.setName("Smoke Test Plan");

        final ExecutionRequest executionRequest = new ExecutionRequest();
        executionRequest.setUuid(executionRequestId);
        executionRequest.setName("Smoke");
        executionRequest.setExecutionStatus(ExecutionStatuses.FINISHED);
        executionRequest.setProjectId(projectId);
        executionRequest.setTestPlanId(testPlan.getUuid());

        final ExecutionSummaryResponse executionSummaryResponse = new ExecutionSummaryResponse();
        executionSummaryResponse.setPassedRate(35);
        executionSummaryResponse.setWarningRate(25);
        executionSummaryResponse.setFailedRate(40);

        final ProjectDataResponse projectData = new ProjectDataResponse();
        projectData.setName(projectName);

        final String subject = "[${PROJECT_NAME}] ${EXECUTION_REQUEST_NAME} - ${EXECUTION_REQUEST_STATUS}" +
                "(P = ${PASS_RATE}%, F - ${FAIL_RATE}%, W - ${WARNING_RATE}%, P+W = ${PASSED_PLUS_WARNING_RATE}%). " +
                "TP: ${TEST_PLAN_NAME}. CU: ${CURRENT_USER}";

        final String expectedSubject = subject
                .replace(PROJECT_NAME.getName(), projectName)
                .replace(EXECUTION_REQUEST_NAME.getName(), executionRequest.getName())
                .replace(EXECUTION_REQUEST_STATUS.getName(), executionRequest.getExecutionStatus().getName())
                .replace(PASS_RATE.getName(), String.valueOf(executionSummaryResponse.getPassedRate()))
                .replace(FAIL_RATE.getName(), String.valueOf(executionSummaryResponse.getFailedRate()))
                .replace(WARNING_RATE.getName(), String.valueOf(executionSummaryResponse.getWarningRate()))
                .replace(PASSED_PLUS_WARNING_RATE.getName(), String.valueOf(executionSummaryResponse.getPassedRate() + executionSummaryResponse.getWarningRate()))
                .replace(TEST_PLAN_NAME.getName(), testPlan.getName())
                .replace(CURRENT_USER.getName(), userInfo.getUsername());

        when(executionRequestService.get(executionRequestId)).thenReturn(executionRequest);
        when(catalogueService.getTestPlan(testPlan.getUuid())).thenReturn(testPlan);
        when(catalogueService.getProjectData(projectId)).thenReturn(projectData);
        when(reportService.getExecutionSummary(executionRequestId, false)).thenReturn(executionSummaryResponse);

        // when
        final String resolvedSubject = service.resolveSubjectMacros(executionRequestId, subject, userInfo);

        // then
        assertThat(resolvedSubject).isEqualTo(expectedSubject);
    }

    @Test
    public void getAll_testSuccessfulRetrieving_allElementsShouldMatchTheEnums() {
        // given
        final List<EmailSubjectMacrosEnum> subjectMacrosEnums = asList(EmailSubjectMacrosEnum.values());

        // when
        List<EmailSubjectMacrosResponse> macrosResponses = service.getAll();

        // then
        assertThat(macrosResponses)
                .isNotNull()
                .isNotEmpty()
                .size().isEqualTo(subjectMacrosEnums.size());

        final Set<String> macrosResponseNames = extractFields(macrosResponses, EmailSubjectMacrosResponse::getName);
        final Set<String> subjectMacrosEnumNames = extractFields(subjectMacrosEnums, EmailSubjectMacrosEnum::getName);

        assertThat(macrosResponseNames)
                .isNotNull()
                .isNotEmpty();

        assertThat(subjectMacrosEnumNames)
                .isNotNull()
                .isNotEmpty();

        assertThat(macrosResponseNames).isEqualTo(subjectMacrosEnumNames);
    }
}
