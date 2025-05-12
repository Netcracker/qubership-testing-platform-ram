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

package org.qubership.atp.ram.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.Mock;
import org.qubership.atp.ram.client.OrchestratorFeignClient;
import org.qubership.atp.ram.config.EmailConfigurationProvider;
import org.qubership.atp.ram.config.MvcConfig;
import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.enums.Flags;
import org.qubership.atp.ram.enums.TestScopeSections;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.enums.TypeAction;
import org.qubership.atp.ram.models.Comment;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.ExecutionRequestDetails;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.MetaInfo;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.models.logrecords.parts.Log;
import org.qubership.atp.ram.models.logrecords.parts.ValidationTable;
import org.qubership.atp.ram.service.mail.ExecutionRequestReport;
import org.qubership.atp.ram.service.mail.MailService;
import org.qubership.atp.ram.service.rest.server.executor.TestRunController2;
import org.qubership.atp.ram.service.rest.server.mongo.ExecutionRequestController;
import org.qubership.atp.ram.service.rest.server.mongo.TestRunController;
import org.qubership.atp.ram.service.template.impl.ScreenshotsReportTemplateRenderService;
import org.qubership.atp.ram.services.ExecutionRequestCompareService;
import org.qubership.atp.ram.services.ExecutionRequestDetailsService;
import org.qubership.atp.ram.services.ExecutionRequestReportingService;
import org.qubership.atp.ram.services.ExecutionRequestService;
import org.qubership.atp.ram.services.FileResponseEntityService;
import org.qubership.atp.ram.services.GridFsService;
import org.qubership.atp.ram.services.IssueService;
import org.qubership.atp.ram.services.JiraIntegrationService;
import org.qubership.atp.ram.services.JointExecutionRequestService;
import org.qubership.atp.ram.services.LogRecordService;
import org.qubership.atp.ram.services.OrchestratorService;
import org.qubership.atp.ram.services.TestRunService;
import org.qubership.atp.ram.services.WidgetConfigTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactUrl;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Provider("atp-ram")
@PactUrl(urls = {"src/test/resources/pacts/atp-ram-results-importer-atp-ram.json"})
@AutoConfigureMockMvc(addFilters = false, webDriverEnabled = false)
@WebMvcTest(properties = {"spring.cloud.consul.config.enabled=false"},
        controllers = {
                TestRunController.class,
                TestRunController2.class,
                ExecutionRequestController.class,
        })
@ContextConfiguration(classes = {RamAndRamResultsImporterContractTest.TestApp.class})
@EnableAutoConfiguration
@Import({JacksonAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class,
        MvcConfig.class,
        TestRunController.class,
        TestRunController2.class,
        ExecutionRequestController.class
})
@Slf4j
@Isolated
public class RamAndRamResultsImporterContractTest {
        @Configuration
        public static class TestApp {
        }

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private EmailConfigurationProvider configurationProvider;
        @MockBean
        private MailService mailService;
        @MockBean
        private OrchestratorService orchestratorService;
        @MockBean
        private ExecutionRequestReportingService executionRequestReportingService;
        @MockBean
        private ExecutionRequestDetailsService executionRequestDetailsService;
        @MockBean
        private JointExecutionRequestService jointExecutionRequestService;

        @MockBean
        private IssueService issueService;
        @MockBean
        private OrchestratorFeignClient orchestratorFeignClient;
        @MockBean
        private WidgetConfigTemplateService widgetConfigTemplateService;
        @MockBean
        private TestRunService testRunService;
        @MockBean
        private ExecutionRequestCompareService executionRequestCompareService;
        @MockBean
        private ScreenshotsReportTemplateRenderService screenshotsReportTemplateRenderService;

        @MockBean
        private GridFsService gridFsService;
        @Autowired
        private ObjectMapper objectMapper;
        @MockBean
        private JiraIntegrationService jiraIntegrationService;

        @MockBean
        private LogRecordService logRecordService;
        @MockBean
        private ExecutionRequestService executionRequestService;
        @MockBean
        private FileResponseEntityService fileResponseEntityService;
        @Mock
        private ExecutionRequestReport executionRequestReport;

        public void beforeAll() {
                ExecutionRequest executionRequest = new ExecutionRequest();

                when(executionRequestService.save(any()))
                        .thenReturn(getExecutionRequest());
                when(jointExecutionRequestService.isJointExecutionRequest(any(UUID.class)))
                        .thenReturn(false);
                when(executionRequestService.updateExecutionStatus(any(), any()))
                        .thenReturn(getExecutionRequest());
                when(executionRequestDetailsService.createDetails(any(), any()))
                        .thenReturn(getExecutionRequestDetails());
                doNothing().when(jointExecutionRequestService).updateJointExecutionRequestRunStatus(any());


                when(testRunService.getByUuid(any())).thenReturn(getTestRun());
                doNothing().when(testRunService).finishTestRun(any(), anyBoolean());
                when(testRunService.save(any())).thenReturn(getTestRun());
                when(testRunService.updateLogRecordsTestingStatus(any(), any()))
                        .thenReturn(Collections.singletonList(getLogRecord()));

                doNothing().when(issueService).mapTestRunsAndRecalculateIssues(any());
                when(testRunService.finishTestRuns(any(), anyBoolean()))
                        .thenReturn(Collections.singletonList(UUID.fromString("bf1bbd5a-f1a7-47cc-86c0-5a2e50957f20")));

                when(testRunService.create(any(), any(), any(), any()))
                        .thenReturn(getTestRun());
                when(testRunService.patch(any())).thenReturn(getTestRun());

        }

        @TestTemplate
        @ExtendWith(PactVerificationInvocationContextProvider.class)
        void pactVerificationTestTemplate(PactVerificationContext context) {
                context.verifyInteraction();
        }

        @BeforeEach
        void before(PactVerificationContext context) throws Exception {
                beforeAll();
                context.setTarget(new MockMvcTestTarget(mockMvc));
        }

        @State("all ok")
        public void allPass() {
        }

        public ExecutionRequestDetails getExecutionRequestDetails() {
                ExecutionRequestDetails executionRequestDetails = new ExecutionRequestDetails();
                executionRequestDetails.setName("name");
                executionRequestDetails.setProjectId(UUID.randomUUID());
                executionRequestDetails.setExecutionRequestId(UUID.randomUUID());
                executionRequestDetails.setMessage("msg");
                executionRequestDetails.setUuid(UUID.randomUUID());
                executionRequestDetails.setStatus(TestingStatuses.PASSED);
                executionRequestDetails.setDate(new Date());
                return executionRequestDetails;
        }

        public ExecutionRequest getExecutionRequest() {
                ExecutionRequest executionRequest = new ExecutionRequest();
                executionRequest.setEmailSubject("email");
                executionRequest.setEmailTemplateId(UUID.randomUUID());
                executionRequest.setExecutorId(UUID.randomUUID());
                executionRequest.setLabelTemplateId(UUID.randomUUID());
                executionRequest.setLogCollectorConditionId(UUID.randomUUID());
                executionRequest.setName("name");
                executionRequest.setProjectId(UUID.randomUUID());
                executionRequest.setSolutionBuild("build");
                executionRequest.setTaToolsGroupId(UUID.randomUUID());
                executionRequest.setTestPlanId(UUID.randomUUID());
                executionRequest.setTestScopeId(UUID.randomUUID());
                executionRequest.setUuid(UUID.randomUUID());
                executionRequest.setWidgetConfigTemplateId(UUID.randomUUID());
                executionRequest.setCiJobUrl("ci");
                executionRequest.setEnvironmentId(UUID.randomUUID());
                executionRequest.setLegacyMailRecipients("legacy");
                executionRequest.setPreviousExecutionRequestId(UUID.randomUUID());
                executionRequest.setExecutionStatus(ExecutionStatuses.FINISHED);
                executionRequest.setCountLogRecords(1L);
                executionRequest.setExecutorName("name");
                executionRequest.setFilteredByLabels(Collections.singleton(UUID.randomUUID()));
                executionRequest.setFinishDate(new Timestamp(1L));
                executionRequest.setStartDate(new Timestamp(1L));
                executionRequest.setFlagIds(Collections.singleton(UUID.randomUUID()));
                executionRequest.setJointExecutionCount(1);
                executionRequest.setJointExecutionKey("joint");
                executionRequest.setJointExecutionTimeout(1);
                executionRequest.setLabels(Collections.singletonList(UUID.randomUUID()));
                executionRequest.setInitialExecutionRequestId(UUID.randomUUID());
                return executionRequest;
        }

        public TestRun getTestRun() {
                TestRun testRun = new TestRun();
                testRun.setDataSetUrl("url");
                testRun.setDataSetListUrl("list url");
                testRun.setExecutionRequestId(UUID.randomUUID());
                testRun.setExecutor("ex");
                testRun.setLabelIds(Collections.singleton(UUID.randomUUID()));
                testRun.setFdrLink("str");
                testRun.setJiraTicket("str");
                testRun.setLogCollectorData("str");
                testRun.setName("name");
                testRun.setParentTestRunId(UUID.randomUUID());
                testRun.setRootCauseId(UUID.randomUUID());
                testRun.setTestCaseId(UUID.randomUUID());
                testRun.setTestCaseName("str");
                testRun.setUrlToBrowserOrLogs(Collections.singleton("str"));
                testRun.setUrlToBrowserSession("str");
                testRun.setUuid(UUID.randomUUID());
                testRun.setQaHost(Collections.singletonList("a"));
                testRun.setSolutionBuild(Collections.singletonList("a"));
                testRun.setTaHost(Collections.singletonList("a"));
                testRun.setExecutionStatus(ExecutionStatuses.FINISHED);

                MetaInfo metaInfo = new MetaInfo();
                metaInfo.setDefinitionId(UUID.randomUUID());
                metaInfo.setHidden(true);
                metaInfo.setLine(1);
                metaInfo.setScenarioHashSum("str");
                metaInfo.setScenarioId(UUID.randomUUID());
                testRun.setMetaInfo(metaInfo);

                testRun.setBrowserNames(Collections.singletonList("name"));

                Comment comment = new Comment();
                comment.setHtml("html");
                comment.setText("txt");
                testRun.setComment(comment);

                testRun.setFinishDate(new Timestamp(1L));
                testRun.setFlags(Collections.singletonList(Flags.SKIP_IF_DEPENDENCY_FAILED));
                testRun.setGroupedTestRun(true);
                testRun.setStartDate(new Timestamp(1L));
                testRun.setTestScopeSection(TestScopeSections.EXECUTION);
                testRun.setFinalTestRun(true);
                testRun.setInitialTestRunId(UUID.randomUUID());

                return testRun;
        }

        public LogRecord getLogRecord() {
                LogRecord logRecord = new LogRecord();
                logRecord.setName("name");
                logRecord.setValidationTable(new ValidationTable());
                logRecord.setCompaund(true);
                logRecord.setMessageParametersPresent(true);
                logRecord.setServer("server");
                logRecord.setSection(true);
                logRecord.setCreatedDateStamp(1L);
                logRecord.setUuid(UUID.randomUUID());
                logRecord.setUrlToLogCollectorData("url");
                logRecord.setDuration(1L);
                logRecord.setStackTrace("stack");
                logRecord.setParentRecordId(UUID.randomUUID());
                logRecord.setSnapshotId("id");
                logRecord.setContextVariablesPresent(true);
                logRecord.setTestRunId(UUID.randomUUID());
                logRecord.setMessage("msg");
                logRecord.setPreview("str");
                logRecord.setValidationLabels(Collections.singleton("str"));
                logRecord.setConfigInfoId(Collections.singleton("str"));
                logRecord.setCreatedDate(new Timestamp(System.currentTimeMillis()));
                logRecord.setStartDate(new Timestamp(System.currentTimeMillis()));
                logRecord.setEndDate(new Timestamp(System.currentTimeMillis()));
                logRecord.setLastUpdated(new Timestamp(System.currentTimeMillis()));
                logRecord.setDuplicateId(Collections.singletonList(UUID.fromString("1ed7db2b-2d91-4c3d-9552-f9cdd5a5f07")));

                LogRecord logRecord1 = new LogRecord();
                logRecord1.setType(TypeAction.BV);
                logRecord1.setTestingStatus(TestingStatuses.UNKNOWN);
                logRecord1.setExecutionStatus(ExecutionStatuses.NOT_STARTED);
                LogRecord.Child child = new LogRecord.Child(logRecord1);
                child.setName("name");
                child.setUuid(UUID.randomUUID());
                logRecord.setChildren(Collections.singletonList(child));

                MetaInfo metaInfo = new MetaInfo();
                metaInfo.setHidden(true);
                metaInfo.setLine(1);
                metaInfo.setScenarioHashSum("asd");
                metaInfo.setScenarioId(UUID.randomUUID());
                metaInfo.setDefinitionId(UUID.randomUUID());
                logRecord.setMetaInfo(metaInfo);

                Log.TaToolLog taToolLog = new Log.TaToolLog();
                taToolLog.setMessage("msg");
                taToolLog.setScreenShotId("screen");
                logRecord.setTaToolsLogs(Collections.singletonList(taToolLog));

                return logRecord;
        }
}
