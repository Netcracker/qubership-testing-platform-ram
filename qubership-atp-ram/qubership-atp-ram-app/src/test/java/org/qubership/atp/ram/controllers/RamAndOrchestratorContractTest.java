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

import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.qubership.atp.ram.config.EmailConfigurationProvider;
import org.qubership.atp.ram.config.MvcConfig;
import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.enums.TypeAction;
import org.qubership.atp.ram.models.EnrichedTestRun;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.ExecutionRequestDetails;
import org.qubership.atp.ram.models.ExecutionRequestReporting;
import org.qubership.atp.ram.models.Label;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.MetaInfo;
import org.qubership.atp.ram.models.RootCause;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.models.logrecords.parts.Log;
import org.qubership.atp.ram.models.logrecords.parts.ValidationTable;
import org.qubership.atp.ram.service.mail.ExecutionRequestReport;
import org.qubership.atp.ram.service.mail.MailService;
import org.qubership.atp.ram.service.rest.server.mongo.EmailController;
import org.qubership.atp.ram.service.rest.server.mongo.ExecutionRequestController;
import org.qubership.atp.ram.service.rest.server.mongo.LogRecordController;
import org.qubership.atp.ram.service.rest.server.mongo.TestRunController;
import org.qubership.atp.ram.service.template.impl.ScreenshotsReportTemplateRenderService;
import org.qubership.atp.ram.services.*;
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
@PactUrl(urls = {"src/test/resources/pacts/atp-orchestrator-atp-ram.json"})
@AutoConfigureMockMvc(addFilters = false, webDriverEnabled = false)
@WebMvcTest(properties = {"spring.cloud.consul.config.enabled=false"},
        controllers = {
                TestRunController.class,
                EmailController.class,
                ExecutionRequestController.class,
                LogRecordController.class
        })
@ContextConfiguration(classes = {RamAndOrchestratorContractTest.TestApp.class})
@EnableAutoConfiguration
@Import({JacksonAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class,
        MvcConfig.class,
        TestRunController.class,
        EmailController.class,
        ExecutionRequestController.class,
        LogRecordController.class
})
@Slf4j
@Isolated
public class RamAndOrchestratorContractTest {

    @Configuration
    public static class TestApp {
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Mock
    private ExecutionRequestReport executionRequestReport;

    @MockBean
    private EmailConfigurationProvider configurationProvider;

    @MockBean
    private MailService mailService;

    @MockBean
    private IssueService issueService;

    @MockBean
    private OrchestratorService orchestratorService;

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

    @MockBean
    private JiraIntegrationService jiraIntegrationService;

    @MockBean
    private LogRecordService logRecordService;

    @MockBean
    private ExecutionRequestService executionRequestService;

    @MockBean
    private ExecutionRequestReportingService executionRequestReportingService;
    @MockBean
    private ExecutionRequestDetailsService executionRequestDetailsService;
    @MockBean
    private FileResponseEntityService fileResponseEntityService;

    @MockBean
    private JointExecutionRequestService jointExecutionRequestService;

    @MockBean
    private EnvironmentsService environmentsService;

    @MockBean
    private ScriptReportService scriptReportService;

    public void beforeAll() {
        Mockito.when(executionRequestService.updateExecutionStatus(Mockito.any(), Mockito.any()))
                .thenReturn(getExecutionRequest());
        Mockito.when(executionRequestService.getAllEnrichedTestRuns(Mockito.any()))
                .thenReturn(Collections.singletonList(getEnrichedTestRun()));
        Mockito.when(executionRequestReportingService.createReporting(Mockito.any(), Mockito.any()))
                .thenReturn(getExecutionRequestReporting());
        Mockito.when(executionRequestReportingService.updateReportingStatus(Mockito.any(), Mockito.any()))
                .thenReturn(getExecutionRequestReporting());
        Mockito.when(executionRequestDetailsService.createDetails(Mockito.any(), Mockito.any()))
                .thenReturn(getExecutionRequestDetails());
        Mockito.when(executionRequestService.save(Mockito.any()))
                .thenReturn(getExecutionRequest());
        Mockito.when(logRecordService.findById(Mockito.any()))
                .thenReturn(getLogRecord());
        Mockito.when(logRecordService.getLogRecordChildren(Mockito.any()))
                .thenReturn(Stream.of(getLogRecord()));
        Mockito.when(logRecordService.save(Mockito.any()))
                .thenReturn(getLogRecord());
        Mockito.when(logRecordService.revertTestingStatusForLogRecord(Mockito.any()))
                .thenReturn(getLogRecord());
        Mockito.when(testRunService.revertTestingStatusForLogRecord(Mockito.any()))
                .thenReturn(getLogRecord());
        Mockito.when(testRunService.getByUuid(Mockito.any()))
                .thenReturn(getTestRun());
        Mockito.when(testRunService.save(Mockito.any()))
                .thenReturn(getTestRun());
        Mockito.when(testRunService.getAllLogRecordsByTestRunId(Mockito.any()))
                .thenReturn(Collections.singletonList(getLogRecord()));
        Mockito.when(testRunService.updTestingStatusHard(Mockito.any(), Mockito.any()))
                .thenReturn(getTestRun());
        Mockito.when(testRunService.updTestingStatusHardAndBrowserNames(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(getTestRun());
        Mockito.when(testRunService.updateLogRecordsTestingStatus(Mockito.any(), Mockito.any()))
                .thenReturn(Collections.singletonList(getLogRecord()));
        Mockito.when(configurationProvider.provideEmailConfiguration())
                .thenReturn(executionRequestReport);
        Mockito.when(executionRequestReport.buildEmailBody(Mockito.any()))
                .thenReturn("str");
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

        MetaInfo metaInfo = new MetaInfo();
        metaInfo.setDefinitionId(UUID.randomUUID());
        metaInfo.setHidden(true);
        metaInfo.setLine(1);
        metaInfo.setScenarioHashSum("str");
        metaInfo.setScenarioId(UUID.randomUUID());
        testRun.setMetaInfo(metaInfo);

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

    public ExecutionRequestReporting getExecutionRequestReporting() {
        ExecutionRequestReporting executionRequestReporting = new ExecutionRequestReporting();
        executionRequestReporting.setExecutionRequestId(UUID.randomUUID());
        executionRequestReporting.setName("name");
        executionRequestReporting.setProjectId(UUID.randomUUID());
        executionRequestReporting.setRecipients(Collections.singletonList("string"));
        executionRequestReporting.setSubject("subj");
        executionRequestReporting.setUuid(UUID.randomUUID());
        executionRequestReporting.setStatus(TestingStatuses.PASSED);
        return executionRequestReporting;
    }

    public EnrichedTestRun getEnrichedTestRun() {
        EnrichedTestRun enrichedTestRun = new EnrichedTestRun();
        enrichedTestRun.setDataSetListUrl("dataset");
        enrichedTestRun.setDataSetUrl("url");
        enrichedTestRun.setDuration(1L);
        enrichedTestRun.setExecutionRequestId(UUID.randomUUID());
        enrichedTestRun.setFdrLink("fdr");
        enrichedTestRun.setFdrLink("fdr");
        enrichedTestRun.setGroupedTestRun(true);
        enrichedTestRun.setFdrWasSent(true);
        enrichedTestRun.setJiraTicket("jira");
        enrichedTestRun.setLogCollectorData("lc");
        enrichedTestRun.setExecutor("ex");
        enrichedTestRun.setLogCollectorData("lcData");
        enrichedTestRun.setName("name");
        enrichedTestRun.setParentTestRunId(UUID.randomUUID());
        enrichedTestRun.setRootCauseId(UUID.randomUUID());
        enrichedTestRun.setTestCaseId(UUID.randomUUID());
        enrichedTestRun.setTestCaseName("case name");
        enrichedTestRun.setUrlToBrowserOrLogs(Collections.EMPTY_SET);
        enrichedTestRun.setUrlToBrowserSession("br session");
        enrichedTestRun.setUuid(UUID.randomUUID());

        RootCause rootCause = new RootCause();
        rootCause.setName("name");
        rootCause.setProjectId(UUID.randomUUID());
        rootCause.setParentId(UUID.randomUUID());
        enrichedTestRun.setFailureReason(rootCause);

        Label label = new Label();
        label.setName("name");
        label.setUuid(UUID.randomUUID());
        enrichedTestRun.setLabels(Collections.singletonList(label));

        MetaInfo metaInfo = new MetaInfo();
        metaInfo.setDefinitionId(UUID.randomUUID());
        metaInfo.setHidden(true);
        metaInfo.setLine(1);
        metaInfo.setScenarioId(UUID.randomUUID());
        metaInfo.setScenarioHashSum("Hashsumm");
        enrichedTestRun.setMetaInfo(metaInfo);
        return enrichedTestRun;
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
        Set<UUID> randomId = new HashSet<>();
        randomId.add(UUID.randomUUID());
        executionRequest.setFilteredByLabels(randomId);
        executionRequest.setFlagIds(randomId);
        executionRequest.setLabels(Collections.singletonList(UUID.randomUUID()));
        executionRequest.setFinishDate(new Timestamp(1L));
        executionRequest.setStartDate(new Timestamp(1L));
        executionRequest.setJointExecutionKey("jointExecutionKey");
        executionRequest.setJointExecutionCount(1);
        executionRequest.setJointExecutionTimeout(1);
        executionRequest.setInitialExecutionRequestId(UUID.randomUUID());
        return executionRequest;
    }
}
