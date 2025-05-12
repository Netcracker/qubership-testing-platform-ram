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

package org.qubership.atp.ram.service;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.qubership.atp.ram.enums.SystemStatus.PASS;
import static org.qubership.atp.ram.enums.SystemStatus.WARN;
import static org.qubership.atp.ram.enums.TestingStatuses.PASSED;
import static org.qubership.atp.ram.enums.TestingStatuses.WARNING;
import static org.qubership.atp.ram.models.DefectPriority.CRITICAL;
import static org.qubership.atp.ram.testdata.EntitiesGenerator.newJiraComponent;
import static org.qubership.atp.ram.testdata.EntitiesGenerator.newLabel;
import static org.qubership.atp.ram.testdata.EnvironmentsInfoMock.newEnvironmentsInfo;
import static org.qubership.atp.ram.testdata.EnvironmentsInfoMock.newSystemInfo;
import static org.qubership.atp.ram.testdata.ExecutionRequestServiceMock.newExecutionRequest;
import static org.qubership.atp.ram.testdata.FailPatternMock.newFailPattern;
import static org.qubership.atp.ram.testdata.FailPatternMock.newRootCause;
import static org.qubership.atp.ram.testdata.LogRecordServiceMock.newLogRecord;
import static org.qubership.atp.ram.testdata.PotsStatisticsMock.newPotsStatistic;
import static org.qubership.atp.ram.testdata.TestRunServiceMock.newTestRun;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.qubership.atp.ram.dto.response.DefectPredefineResponse;
import org.qubership.atp.ram.dto.response.TestCaseLabelResponse;
import org.qubership.atp.ram.model.DefectDescriptionRenderModel;
import org.qubership.atp.ram.model.DefectDescriptionRenderModel.Link;
import org.qubership.atp.ram.models.EnvironmentsInfo;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.FailPattern;
import org.qubership.atp.ram.models.Issue;
import org.qubership.atp.ram.models.JiraComponent;
import org.qubership.atp.ram.models.Label;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.PotsStatisticsPerAction;
import org.qubership.atp.ram.models.RootCause;
import org.qubership.atp.ram.models.SystemInfo;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.service.template.TemplateRenderService;
import org.qubership.atp.ram.services.CatalogueService;
import org.qubership.atp.ram.services.EnvironmentsInfoService;
import org.qubership.atp.ram.services.EnvironmentsService;
import org.qubership.atp.ram.services.ExecutionRequestService;
import org.qubership.atp.ram.services.FailPatternService;
import org.qubership.atp.ram.services.IssueService;
import org.qubership.atp.ram.services.LogRecordService;
import org.qubership.atp.ram.services.PotService;
import org.qubership.atp.ram.services.RootCauseService;
import org.qubership.atp.ram.services.TestCaseService;
import org.qubership.atp.ram.services.TestRunService;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class DefectPredefineServiceTest {

    @InjectMocks
    private DefectPredefineService service;

    @Mock
    private IssueService issueService;
    @Mock
    private TemplateRenderService templateRenderService;
    @Mock
    private FailPatternService failPatternService;
    @Mock
    private RootCauseService rootCauseService;
    @Mock
    private ExecutionRequestService executionRequestService;
    @Mock
    private TestRunService testRunService;
    @Mock
    private EnvironmentsInfoService environmentsInfoService;
    @Mock
    private EnvironmentsService environmentsService;
    @Mock
    private PotService potService;
    @Mock
    private LogRecordService logRecordService;
    @Mock
    private TestCaseService testCaseService;
    @Mock
    private Resource descriptionTemplate;
    @Mock
    private CatalogueService catalogueService;
    @Mock
    private Resource environmentTemplate;

    @Captor
    private ArgumentCaptor<DefectDescriptionRenderModel> captor;

    private UUID issueId;
    private Issue issue;
    private UUID failPatternId;
    private UUID failReasonId;
    private List<UUID> failedTestRunIds;
    private List<UUID> logRecordIds;
    private String message;
    private UUID executionRequestId;
    private UUID environmentId;
    private EnvironmentsInfo environmentsInfo;
    private FailPattern failPattern;
    private RootCause failureReason;
    private String catalogueUrl;
    private List<SystemInfo> qaSystemInfoList;
    private String linkToSvp;
    private LogRecord logRecord;
    private UUID projectId;
    private UUID testPlanId;
    private UUID failedTestRunId;
    private List<PotsStatisticsPerAction> potsStatistic;
    private TestRun failedTestRun;
    private List<JiraComponent> allComponents;

    @BeforeEach
    public void setUp() throws Exception {
        catalogueUrl = "https://atp-public-gateway-service-address";
        linkToSvp = "http://svp-service-address";

        issueId = randomUUID();
        failPatternId = randomUUID();
        failReasonId = randomUUID();
        message = "Failed login";
        executionRequestId = randomUUID();

        failedTestRun = newTestRun();
        failedTestRun.setLogCollectorData("http://dev222-logcollector:8080");
        failedTestRunId = failedTestRun.getUuid();
        failedTestRunIds = singletonList(failedTestRunId);

        LogRecord logRecord1 = newLogRecord("Log Record #1");
        logRecord1.setTestRunId(failedTestRunId);
        logRecord1.setLinkToSvp(linkToSvp);
        logRecord = logRecord1;
        LogRecord logRecord2 = newLogRecord("Log Record #2");
        logRecordIds = asList(logRecord1.getUuid(), logRecord2.getUuid());

        issue = new Issue(CRITICAL, null, null, failPatternId, failReasonId, message,
                failedTestRunIds, logRecordIds, executionRequestId, 0);

        List<TestRun> failedTestRuns = singletonList(failedTestRun);

        Label prodLabel = newLabel("Prod");
        Label q1Scope = newLabel("Q1_scope");
        Label uatTested = newLabel("uat_tested");
        JiraComponent ramComponent = newJiraComponent("RAM");
        JiraComponent catalogComponent = newJiraComponent("Catalog");
        JiraComponent streamingComponent = newJiraComponent("Streaming");
        JiraComponent datasetsComponent = newJiraComponent("Datasets");
        JiraComponent commonComponent = newJiraComponent("Common");
        allComponents = asList(ramComponent, catalogComponent, streamingComponent, datasetsComponent, commonComponent);

        List<TestCaseLabelResponse> labelResponse = asList(
                new TestCaseLabelResponse(randomUUID(), "Test case #1", asList(prodLabel, q1Scope), asList(ramComponent, catalogComponent)),
                new TestCaseLabelResponse(randomUUID(), "Test case #2", asList(prodLabel, uatTested), asList(ramComponent, streamingComponent))
        );

        ExecutionRequest executionRequest = newExecutionRequest(executionRequestId);
        projectId = executionRequest.getProjectId();
        testPlanId = executionRequest.getTestPlanId();

        environmentId = UUID.randomUUID();
        qaSystemInfoList = asList(
                newSystemInfo("T_O_M_S", PASS, "1.0.2", "https://127.0.0.1:9876"),
                newSystemInfo("Exo", WARN, "21.3.12", "https://127.0.0.1:9876")
        );
        environmentsInfo = newEnvironmentsInfo("", environmentId, executionRequestId, qaSystemInfoList, null);

        failPattern = newFailPattern(failPatternId, "Login failed");
        failureReason = newRootCause(failReasonId, "Design gap");

        PotsStatisticsPerAction file1PotStatistic = newPotsStatistic("file1", PASSED, "file1.docx");
        PotsStatisticsPerAction file2PotStatistic = newPotsStatistic("file2", WARNING, "file2.docx");
        potsStatistic = asList(file1PotStatistic, file2PotStatistic);

        when(issueService.get(issueId)).thenReturn(issue);
        when(testRunService.getByIds(issue.getFailedTestRunIds())).thenReturn(failedTestRuns);
        when(testCaseService.getTestCaseLabelsByIds(failedTestRuns)).thenReturn(labelResponse);
        when(executionRequestService.get(executionRequestId)).thenReturn(executionRequest);
        when(logRecordService.get(logRecord1.getUuid())).thenReturn(logRecord1);
        when(environmentsInfoService.findByExecutionRequestId(executionRequestId)).thenReturn(environmentsInfo);
        when(environmentsService.getEnvironmentNameById(environmentId)).thenReturn(environmentsInfo.getName());
        when(failPatternService.get(failPatternId)).thenReturn(failPattern);
        when(rootCauseService.get(failReasonId)).thenReturn(failureReason);
        when(potService.collectStatisticForTestRun(logRecord1.getTestRunId())).thenReturn(potsStatistic);
        when(testRunService.getByIds(singletonList(failedTestRunId))).thenReturn(singletonList(failedTestRun));
        when(testRunService.get(failedTestRunId)).thenReturn(failedTestRun);
        when(descriptionTemplate.getInputStream()).thenReturn(new ByteArrayInputStream("content".getBytes()));
        when(environmentTemplate.getInputStream()).thenReturn(new ByteArrayInputStream("content".getBytes()));
        when(catalogueService.getTestPlanJiraComponents(testPlanId)).thenReturn(allComponents);

        when(logRecordService.getAllLogRecordsByTestRunIds(singletonList(failedTestRunId)))
                .thenReturn(singletonList(logRecord1));
        service.descriptionTemplate = descriptionTemplate;
        service.environmentTemplate = environmentTemplate;
        service.catalogueUrl = catalogueUrl;
    }

    @Test
    public void predefine() throws Exception {
        final DefectPredefineResponse response = service.predefine(issueId);

        verify(templateRenderService, times(2)).render(any(), captor.capture());

        assertNotNull(response);
        final List<JiraComponent> components = response.getComponents();
        assertNotNull(components);
        assertEquals(allComponents.size(), components.size());

        final DefectDescriptionRenderModel model = captor.getValue();

        assertNotNull(model);
        assertEquals(message, model.getMessage());
        assertEquals(failPattern.getName(), model.getFailPattern());
        assertEquals(failureReason.getName(), model.getFailReason());

        final Link logRecordLink = model.getLogRecordLink();
        assertEquals(logRecord.getName(), logRecordLink.getName());

        final String logRecordUrl = logRecordLink.getUrl();
        assertNotNull(logRecordUrl);
        assertTrue(logRecordUrl.startsWith(catalogueUrl));
        assertTrue(logRecordUrl.contains(logRecord.getUuid().toString()));
        assertTrue(logRecordUrl.contains(projectId.toString()));
        assertTrue(logRecordUrl.contains(failedTestRunId.toString()));

        final Link environmentLink = model.getEnvironmentLink();
        assertEquals(environmentsInfo.getName(), environmentLink.getName());

        final String environmentUrl = environmentLink.getUrl();
        assertNotNull(environmentUrl);
        assertTrue(environmentUrl.startsWith(catalogueUrl));
        assertTrue(environmentUrl.contains(projectId.toString()));
        assertTrue(environmentUrl.contains(environmentId.toString()));

        final List<SystemInfo> qaSystems = model.getQaSystems();
        assertNotNull(qaSystems);
        assertEquals(qaSystemInfoList.size(), qaSystems.size());

        final String linkToEr = model.getLinkToEr();
        assertNotNull(linkToEr);
        assertTrue(linkToEr.startsWith(catalogueUrl));
        assertTrue(linkToEr.contains(projectId.toString()));
        assertTrue(linkToEr.contains(executionRequestId.toString()));

        final List<Link> linkToSvp = model.getSvpLinks();
        assertFalse(linkToSvp.isEmpty());
        assertEquals(logRecord.getLinkToSvp(), linkToSvp.get(0).getUrl());

        final List<Link> potLinks = model.getPotLinks();
        assertNotNull(potLinks);
        assertEquals(potsStatistic.size(), potLinks.size());

        final String linkToLc = model.getLinkToLc();
        assertNotNull(linkToLc);
        assertEquals(failedTestRun.getLogCollectorData(), linkToLc);
    }
}
