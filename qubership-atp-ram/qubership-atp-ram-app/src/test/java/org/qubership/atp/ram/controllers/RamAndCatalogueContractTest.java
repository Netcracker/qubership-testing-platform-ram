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

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;
import org.qubership.atp.ram.config.MvcConfig;
import org.qubership.atp.ram.controllers.api.dto.jira.TestRunForRefreshFromJiraDto;
import org.qubership.atp.ram.controllers.api.dto.jira.TestRunToJiraInfoDto;
import org.qubership.atp.ram.converters.ModelConverter;
import org.qubership.atp.ram.dto.response.PaginationResponse;
import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.enums.Flags;
import org.qubership.atp.ram.enums.TestScopeSections;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.enums.TypeAction;
import org.qubership.atp.ram.model.TestRunForRefreshFromJira;
import org.qubership.atp.ram.model.TestRunToJiraInfo;
import org.qubership.atp.ram.models.Comment;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.ExecutionRequestDetails;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.MetaInfo;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.models.TestRunStatistic;
import org.qubership.atp.ram.models.logrecords.parts.Log;
import org.qubership.atp.ram.models.logrecords.parts.ValidationTable;
import org.qubership.atp.ram.service.rest.server.mongo.ExecutionRequestController;
import org.qubership.atp.ram.service.rest.server.mongo.JiraIntegrationController;
import org.qubership.atp.ram.service.rest.server.mongo.TestRunController;
import org.qubership.atp.ram.service.template.impl.ScreenshotsReportTemplateRenderService;
import org.qubership.atp.ram.services.ExecutionRequestCompareService;
import org.qubership.atp.ram.services.ExecutionRequestDetailsService;
import org.qubership.atp.ram.services.ExecutionRequestReportingService;
import org.qubership.atp.ram.services.ExecutionRequestService;
import org.qubership.atp.ram.services.GridFsService;
import org.qubership.atp.ram.services.IssueService;
import org.qubership.atp.ram.services.JiraIntegrationService;
import org.qubership.atp.ram.services.JointExecutionRequestService;
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
@PactUrl(urls = {"src/test/resources/pacts/atp-catalogue-atp-ram.json"})
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(properties = {"spring.cloud.consul.config.enabled=false"},
        controllers = {
                TestRunController.class,
                JiraIntegrationController.class,
                ExecutionRequestController.class
        })
@ContextConfiguration(classes = {RamAndCatalogueContractTest.TestApp.class})
@EnableAutoConfiguration
@Import({JacksonAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class,
        MvcConfig.class,
        TestRunController.class,
        JiraIntegrationController.class,
        ExecutionRequestController.class
})
@Slf4j
@Isolated
public class RamAndCatalogueContractTest {
    @Configuration
    public static class TestApp {
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private ModelConverter modelConverter;

    @MockBean
    private IssueService issueService;
    @MockBean
    private WidgetConfigTemplateService widgetConfigTemplateService;
    @MockBean
    private TestRunService testRunService;
    @MockBean
    private OrchestratorService orchestratorService;
    @MockBean
    private ExecutionRequestCompareService executionRequestCompareService;
    @MockBean
    private ScreenshotsReportTemplateRenderService screenshotsReportTemplateRenderService;
    @MockBean
    private GridFsService gridFsService;
    @MockBean
    private JiraIntegrationService jiraIntegrationService;
    @MockBean
    private ExecutionRequestService executionRequestService;
    @MockBean
    private JointExecutionRequestService jointExecutionRequestService;
    @MockBean
    private ExecutionRequestDetailsService executionRequestDetailsService;
    @MockBean
    private ExecutionRequestReportingService executionRequestReportingService;


    public void beforeAll() {
        when(jiraIntegrationService.getTestRunsForJiraInfoByExecutionId(any())).thenReturn(asList(getTestRunToJiraInfo()));
        when(executionRequestService.findById(any())).thenReturn(getExecutionRequest());
        when(executionRequestService.save(any())).thenReturn(getExecutionRequest());
        when(executionRequestDetailsService.createDetails(any(), any())).thenReturn(getExecutionRequestDetails());

        when(jiraIntegrationService.getTestRunsForJiraInfoByIds(any())).thenReturn(asList(getTestRunToJiraInfo()));
        when(modelConverter.convertJiraInfoModelToDto(anyList()))
                .thenReturn(getTestRunToJiraInfoDto());

        when(jiraIntegrationService.getTestRunsForRefreshFromJira(any()))
                .thenReturn(asList(getTestRunForRefreshFromJira()));
        when(modelConverter.convertRefreshFromJiraModelToDto(anyList()))
                .thenReturn(getTestRunForRefreshFromJiraDto());

        when(testRunService.search(any(), anyInt(), anyInt())).thenReturn(getPaginatedTestRunForRefreshFromJira());
        when(executionRequestService.updateExecutionStatus(any(), any())).thenReturn(getExecutionRequest());
        //when(testRunService.updateTestRunsWithJiraTickets(any())).thenReturn();
        when(testRunService.getTestStatuses()).thenReturn(getTestStatuses());

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

    public TestRunToJiraInfo getTestRunToJiraInfo() {
        TestRunToJiraInfo testRunToJiraInfo = new TestRunToJiraInfo();
        testRunToJiraInfo.setUuid(UUID.randomUUID());
        testRunToJiraInfo.setName("name");
        testRunToJiraInfo.setTestCaseId(UUID.randomUUID());
        testRunToJiraInfo.setExecutionRequestId(UUID.randomUUID());
        testRunToJiraInfo.setLastRun(true);
        testRunToJiraInfo.setJiraTicket("jiraTicket");
        testRunToJiraInfo.setTestingStatus("PASSED");
        testRunToJiraInfo.setEnvironmentInfo("environmentInfo");
        testRunToJiraInfo.setTestRunAtpLink("testRunAtpLink");

        return testRunToJiraInfo;
    }
    public List<TestRunToJiraInfoDto> getTestRunToJiraInfoDto() {
        TestRunToJiraInfoDto testRunToJiraInfo = new TestRunToJiraInfoDto();
        testRunToJiraInfo.setUuid(UUID.randomUUID());
        testRunToJiraInfo.setName("name");
        testRunToJiraInfo.setTestCaseId(UUID.randomUUID());
        testRunToJiraInfo.setExecutionRequestId(UUID.randomUUID());
        testRunToJiraInfo.setLastRun(true);
        testRunToJiraInfo.setJiraTicket("jiraTicket");
        testRunToJiraInfo.setTestingStatus("PASSED");
        testRunToJiraInfo.setEnvironmentInfo("environmentInfo");
        testRunToJiraInfo.setTestRunAtpLink("testRunAtpLink");

        return Collections.singletonList(testRunToJiraInfo);
    }

    public TestRunForRefreshFromJira getTestRunForRefreshFromJira() {
        TestRunForRefreshFromJira testRunForRefreshFromJira = new TestRunForRefreshFromJira();
        testRunForRefreshFromJira.setUuid(UUID.randomUUID());
        testRunForRefreshFromJira.setName("name");
        testRunForRefreshFromJira.setTestCaseId(UUID.randomUUID());
        testRunForRefreshFromJira.setLastRun(true);
        testRunForRefreshFromJira.setJiraTicket("jiraTicket");

        return testRunForRefreshFromJira;
    }
    public List<TestRunForRefreshFromJiraDto> getTestRunForRefreshFromJiraDto() {
        TestRunForRefreshFromJiraDto testRunForRefreshFromJiraDto = new TestRunForRefreshFromJiraDto();
        testRunForRefreshFromJiraDto.setUuid(UUID.randomUUID());
        testRunForRefreshFromJiraDto.setName("name");
        testRunForRefreshFromJiraDto.setTestCaseId(UUID.randomUUID());
        testRunForRefreshFromJiraDto.setLastRun(true);
        testRunForRefreshFromJiraDto.setJiraTicket("jiraTicket");

        return Collections.singletonList(testRunForRefreshFromJiraDto);
    }

    public PaginationResponse<TestRun> getPaginatedTestRunForRefreshFromJira() {

        TestRun testRun = new TestRun();

        testRun.setUuid(UUID.randomUUID());
        testRun.setName("name");
        testRun.setParentTestRunId(UUID.randomUUID());
        testRun.setGroupedTestRun(true);
        testRun.setExecutionRequestId(UUID.randomUUID());
        testRun.setTestCaseId(UUID.randomUUID());
        testRun.setTestCaseName("testCaseName");
        testRun.setExecutionStatus(ExecutionStatuses.IN_PROGRESS);
        testRun.setTestingStatus(TestingStatuses.PASSED);
        testRun.setStartDate(new Timestamp(1L));
        testRun.setFinishDate(new Timestamp(1L));
        testRun.setDuration(1L);
        testRun.setExecutor("executor");
        testRun.setJiraTicket("jiraTicket");
        testRun.setTaHost(asList("taHost"));
        testRun.setQaHost(asList("qaHost"));
        testRun.setSolutionBuild(asList("solutionBuild"));
        testRun.setRootCauseId(UUID.randomUUID());
        testRun.setDataSetUrl("dataSetUrl");
        testRun.setFlags(asList(Flags.TERMINATE_IF_FAIL));
        testRun.setDataSetListUrl("dataSetListUrl");
        testRun.setLogCollectorData("logCollectorData");
        testRun.setFdrWasSent(true);
        testRun.setFdrLink("fdrLink");
        testRun.setNumberOfScreens(1);

        Set<String> set = new HashSet<>();
        set.add("urlToBrowserOrLogs");
        testRun.setUrlToBrowserOrLogs(set);
        testRun.setUrlToBrowserSession("urlToBrowserSession");
        testRun.setPassedRate(1);
        testRun.setWarningRate(1);
        testRun.setFailedRate(1);

        Comment comment = new Comment();
        comment.setHtml("html");
        comment.setText("text");
        testRun.setComment(comment);

        MetaInfo metaInfo = new MetaInfo();
        metaInfo.setScenarioId(UUID.randomUUID());
        metaInfo.setLine(1);
        metaInfo.setScenarioHashSum("scenarioHashSum");
        metaInfo.setDefinitionId(UUID.randomUUID());
        metaInfo.setHidden(true);
        testRun.setMetaInfo(metaInfo);

        TestRunStatistic testRunStatistic = new TestRunStatistic();
        Map<String, TestRunStatistic.ReportLabelParameterData> reportLabelParams = new HashMap<>();
        TestRunStatistic.ReportLabelParameterData reportLabelParameterData = new TestRunStatistic.ReportLabelParameterData();
        reportLabelParameterData.setPassedCount(1);
        reportLabelParameterData.setFailedCount(1);
        reportLabelParams.put("key", reportLabelParameterData);
        testRunStatistic.setReportLabelParams(reportLabelParams);
        testRun.setStatistic(testRunStatistic);


        testRun.setTestScopeSection(TestScopeSections.EXECUTION);
        testRun.setOrder(1);

        Set<UUID> set2 = new HashSet<>();
        set2.add(UUID.randomUUID());
        testRun.setLabelIds(set2);

        testRun.setBrowserNames(asList("browserNames"));

        List<TestRun> listTestRuns = asList(testRun);
        PaginationResponse<TestRun> paginationResponse = new PaginationResponse<>();
        paginationResponse.setEntities(listTestRuns);
        paginationResponse.setTotalCount(listTestRuns.size());

        return  paginationResponse;
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

    public Map<TestingStatuses, String> getTestStatuses() {
        Map<TestingStatuses, String> testingStatusesStringMap = new HashMap<>();
        testingStatusesStringMap.put(TestingStatuses.PASSED, "string");

        return testingStatusesStringMap;
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
        executionRequest.setJointExecutionCount(1);
        executionRequest.setJointExecutionKey("key");
        executionRequest.setJointExecutionTimeout(1);
        executionRequest.setInitialExecutionRequestId(UUID.randomUUID());

        return executionRequest;
    }
}
