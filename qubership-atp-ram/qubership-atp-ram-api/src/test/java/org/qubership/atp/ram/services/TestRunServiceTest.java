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

package org.qubership.atp.ram.services;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.qubership.atp.ram.enums.ExecutionStatuses.FINISHED;
import static org.qubership.atp.ram.enums.ExecutionStatuses.IN_PROGRESS;
import static org.qubership.atp.ram.enums.ExecutionStatuses.TERMINATED;
import static org.qubership.atp.ram.enums.TestingStatuses.FAILED;
import static org.qubership.atp.ram.enums.TestingStatuses.PASSED;
import static org.qubership.atp.ram.enums.TestingStatuses.STOPPED;
import static org.qubership.atp.ram.enums.TestingStatuses.UNKNOWN;
import static org.qubership.atp.ram.utils.LabelReportNodeMock.toFailedLogRecordNodeResponses;
import static org.qubership.atp.ram.utils.StreamUtils.findFirstInList;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;
import org.qubership.atp.ram.ExecutionRequestsMock;
import org.qubership.atp.ram.LogRecordMock;
import org.qubership.atp.ram.RootCauseMock;
import org.qubership.atp.ram.TestRunsMock;
import org.qubership.atp.ram.client.DataSetListFeignClient;
import org.qubership.atp.ram.clients.api.dto.catalogue.FieldsDto;
import org.qubership.atp.ram.clients.api.dto.catalogue.JiraIssueDto;
import org.qubership.atp.ram.dto.request.StatusUpdateRequest;
import org.qubership.atp.ram.dto.request.TestRunDefectsPropagationRequest;
import org.qubership.atp.ram.dto.request.TestRunsByValidationLabelsRequest;
import org.qubership.atp.ram.dto.request.TestingStatusUpdateRequest;
import org.qubership.atp.ram.dto.request.ValidationLabelFilterRequest;
import org.qubership.atp.ram.dto.response.AnalyzedTestRunResponse;
import org.qubership.atp.ram.dto.response.LabelNodeReportResponse.TestRunNodeResponse;
import org.qubership.atp.ram.dto.response.PaginationResponse;
import org.qubership.atp.ram.dto.response.SimpleTestRunResponse;
import org.qubership.atp.ram.dto.response.StatusUpdateResponse;
import org.qubership.atp.ram.dto.response.StatusUpdateResponse.BaseStatusUpdateResponse;
import org.qubership.atp.ram.dto.response.StatusUpdateResponse.TestRunStatusUpdateResponse;
import org.qubership.atp.ram.dto.response.TestCaseLabelResponse;
import org.qubership.atp.ram.dto.response.TestRunDefectsPropagationResponse;
import org.qubership.atp.ram.dto.response.TestRunDefectsPropagationResponse.Item;
import org.qubership.atp.ram.dto.response.TestRunTreeResponse;
import org.qubership.atp.ram.dto.response.TestRunWithValidationLabelsResponse;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.model.datacontext.TestRunsDataContext;
import org.qubership.atp.ram.models.Comment;
import org.qubership.atp.ram.models.EnrichedTestRun;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.Issue;
import org.qubership.atp.ram.models.JiraTicket;
import org.qubership.atp.ram.models.Label;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.RamObject;
import org.qubership.atp.ram.models.RootCause;
import org.qubership.atp.ram.models.RootCauseType;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.models.TestRunSearchRequest;
import org.qubership.atp.ram.repositories.ExecutionRequestRepository;
import org.qubership.atp.ram.repositories.RootCauseRepository;
import org.qubership.atp.ram.repositories.TestRunRepository;
import org.qubership.atp.ram.utils.IssueMock;
import org.qubership.atp.ram.utils.LabelMock;
import org.qubership.atp.ram.utils.PatchHelper;
import org.qubership.atp.ram.utils.StreamUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;

@ExtendWith(SpringExtension.class)
public class TestRunServiceTest {

    private final ModelMapper modelMapper = new ModelMapper();

    @Mock
    private TestRunRepository testRunRepository;
    @Mock
    private RootCauseService rootCauseService;
    @Mock
    private CatalogueService catalogueService;
    @Mock
    private DataSetListFeignClient dataSetListFeignClient;
    @Mock
    private LogRecordService logRecordService;
    @Mock
    private RootCauseRepository rootCauseRepository;
    @Mock
    private TreeNodeService treeNodeService;
    @Mock
    private TestCaseService testCaseService;
    @Mock
    private IssueService issueService;
    @Mock
    private LabelsService labelsService;
    @Mock
    private ExecutionRequestRepository executionRequestRepository;
    @Mock
    private ProjectsService projectsService;
    @Mock
    private TestPlansService testPlansService;
    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private TestRunService testRunService;

    @Captor
    private ArgumentCaptor<TestRun> testRunSaveCaptor;

    @BeforeEach
    public void set() {
        testRunService = new TestRunService(mongoTemplate, logRecordService, testRunRepository, rootCauseService,
                projectsService, testPlansService, modelMapper, catalogueService, dataSetListFeignClient,
                executionRequestRepository, rootCauseRepository, treeNodeService, testCaseService, issueService,
                new PatchHelper(), labelsService);

        List<RootCause> rootCausesMock = RootCauseMock.getAllRootCauses();
        when(testRunRepository.findAllByExecutionRequestId(any())).thenReturn(TestRunsMock.findByExecutionRequestId());
        when(testRunRepository.findRootCauseIdTestingStatusByExecutionRequestIdAndNameNot(any(UUID.class),
                anyString())).thenReturn(TestRunsMock.findByExecutionRequestIdWithoutSystemTestRun(rootCausesMock));

        when(rootCauseService.getAllRootCauses()).thenReturn(rootCausesMock);
        when(testRunRepository.findAllByExecutionRequestIdAndTestingStatusIsNotNull(any()))
                .thenReturn(TestRunsMock.findByExecutionRequestId());
    }

    @Test
    public void getTestRunsGroupedByRootCauses_GroupsTestRunsByRootCauseNames_ReturnsMapContainingGroupedTestRuns() {
        RootCause notAnalyzedRootCause = new RootCause(randomUUID(), RootCauseType.CUSTOM, "Not Analyzed");
        RootCause defaultIssueRootCause = new RootCause(randomUUID(), RootCauseType.GLOBAL, "Default issue");
        RootCause atIssueRootCause = new RootCause(randomUUID(), RootCauseType.CUSTOM, "AT issue");

        List<RootCause> rootCauses = asList(notAnalyzedRootCause, defaultIssueRootCause, atIssueRootCause);

        UUID currentErId = UUID.randomUUID();
        List<TestRun> currentErTestRuns = TestRunsMock.generateTestRunsWithRootCause(notAnalyzedRootCause, currentErId,
                2);
        currentErTestRuns.addAll(TestRunsMock.generateTestRunsWithRootCause(defaultIssueRootCause, currentErId, 3));
        currentErTestRuns.addAll(TestRunsMock.generateTestRunsWithRootCause(atIssueRootCause, currentErId, 1));

        when(rootCauseService.getAllRootCauses()).thenReturn(rootCauses);
        when(testRunRepository.findAllTestRunRootCausesByExecutionRequestId(currentErId)).thenReturn(currentErTestRuns);

        Map<String, Integer> expectedMap = new HashMap<>();
        expectedMap.put("Not Analyzed", 2);
        expectedMap.put("Default issue", 3);
        expectedMap.put("AT issue", 1);

        Map<String, Integer> result = testRunService.getTestRunsGroupedByRootCauses(currentErId);
        Assertions.assertEquals(expectedMap, result);
    }

    @Test
    public void service_findTestRunsWithFillStatusByRequestId_ReturnsTestRunsWithNotEmptyTestingStatus() {
        List<TestRun> result = testRunService.findTestRunsWithFillStatusByRequestId(any());

        List<TestRun> testRunsWithEmptyStatus =
                result.stream().filter(testRun -> testRun.getTestingStatus() == null).collect(Collectors.toList());

        int countOfTestRunWithoutStatus = 0;
        Assertions.assertEquals(countOfTestRunWithoutStatus, testRunsWithEmptyStatus.size());
    }

    @Test
    public void filterLogRecordsByValidationLabels_ReturnsLogRecordsWithExistingValidationLabels() {
        LogRecord logRecord1 = LogRecordMock.generateLogRecord("test1", UUID.randomUUID());
        logRecord1.setValidationLabels(Collections.singleton("label"));

        LogRecord logRecord2 = LogRecordMock.generateLogRecord("test2", UUID.randomUUID());

        LogRecord logRecord3 = LogRecordMock.generateLogRecord("test3", UUID.randomUUID());
        logRecord3.setValidationLabels(Collections.singleton("label"));

        Stream<LogRecord> logRecords = Stream.of(logRecord1, logRecord2, logRecord3);
        Assertions.assertEquals(2, testRunService.filterLogRecordsWithValidationParams(logRecords).size());
    }

    @Test
    public void stopServiceTestRun_ShouldUpdateSystemTestRun() {
        TestRun testRun = new TestRun();
        testRun.setExecutionRequestId(UUID.randomUUID());
        testRun.setUuid(UUID.randomUUID());
        testRun.setName("Execution Request's Logs");

        when(testRunRepository.findByExecutionRequestIdAndName(any(), anyString())).thenReturn(testRun);

        testRunService.stopServiceTestRun(UUID.randomUUID());

        Assertions.assertEquals(FINISHED, testRun.getExecutionStatus(), "Status should be finished");
        assertTrue(Objects.nonNull(testRun.getFinishDate()), "Finish date should not be NULL");
    }

    @Test
    public void updateTestRunsStatusToTerminatedByErId_ShouldTerminateNotFinishedTestRuns() {
        TestRun testRun = new TestRun();
        testRun.setExecutionStatus(IN_PROGRESS);
        testRun.setUuid(UUID.randomUUID());
        when(testRunRepository.findAllByExecutionRequestIdAndExecutionStatusIn(any(), any()))
                .thenReturn(Collections.singletonList(testRun));

        testRunService.updateTestRunsStatusToTerminatedByErId(UUID.randomUUID());

        Assertions.assertEquals(TERMINATED,
                testRun.getExecutionStatus(), "Status should be terminated");
        assertTrue(Objects.nonNull(testRun.getFinishDate()), "Finish date should not be NULL");
    }

    @Test
    public void getTestRunByIdWithParent_SetIdOfGroupedTestRun_ShouldReturnOnlyGroupedTestRunWithChangedName() {
        TestRun groupedTestRun = TestRunsMock.generateGroupedTestRun(UUID.randomUUID(), "New", UUID.randomUUID());
        UUID testRunId = groupedTestRun.getUuid();
        when(testRunRepository.findById(testRunId)).thenReturn(Optional.of(groupedTestRun));

        TestRun groupedParentTestRun =
                TestRunsMock.generateGroupedTestRun(groupedTestRun.getParentTestRunId(), "Offline", null);
        when(testRunRepository.findNameParentIdByUuid(groupedTestRun.getParentTestRunId())).thenReturn(
                groupedParentTestRun);

        TestRunTreeResponse testRunTreeResponse = testRunService.getTestRunByIdWithParent(testRunId);

        assertNull(testRunTreeResponse.getSimpleTestRun(), "Simple TR should not be exists");
    }

    @Test
    public void getTestRun_setIdOfSimpleTestRun_shouldReturnSimpleTestRun() {
        TestRun simpleTestRun = TestRunsMock.generateSimpleTestRun(UUID.randomUUID());
        UUID testRunId = simpleTestRun.getUuid();
        when(testRunRepository.findById(testRunId)).thenReturn(Optional.of(simpleTestRun));

        String rootCauseName = "RC";
        when(rootCauseService.getRootCauseNameById(simpleTestRun.getRootCauseId())).thenReturn(rootCauseName);

        ExecutionRequest executionRequest = ExecutionRequestsMock.generateRequest();
        when(executionRequestRepository.findByUuid(simpleTestRun.getExecutionRequestId())).thenReturn(executionRequest);

        long allLogRecordsCount = 8L;
        when(logRecordService.countLrsByTestRunsId(any())).thenReturn(allLogRecordsCount);
        long passedLogRecordsCount = 5L;
        when(logRecordService.countAllPassedLrByTestRunIds(any())).thenReturn(passedLogRecordsCount);

        TestRunTreeResponse testRunTreeResponse = testRunService.getTestRunByIdWithParent(testRunId);
        SimpleTestRunResponse simpleTestRunExp = TestRunsMock.generateSimpleTrResponse(
                simpleTestRun, rootCauseName, allLogRecordsCount, passedLogRecordsCount);

        Assertions.assertEquals(simpleTestRunExp, testRunTreeResponse.getSimpleTestRun(), "Simple TR should be valid");
    }

    @Test
    public void getTestRunByIdWithParent_givenAllRequiredParamsForBrowserMonitoringLink_canGenerateBrowserLink() {
        UUID trId = UUID.randomUUID();
        TestRun tr = new TestRun();
        tr.setUuid(trId);
        tr.setBrowserNames(singletonList("browserName1"));
        tr.setStartDate(Timestamp.valueOf("2022-01-01 01:01:01.001"));
        tr.setFinishDate(Timestamp.valueOf("2022-01-01 01:02:00.003"));
        String template = "https://dashboard-service-address/d/kzqCPg_Wk/atp-cloud-pods?orgId=3&var-gr_prefix=teams"
                + ".oshobj&var-cluster=atp-cloud&var-ns=prod&var-app_type=atp-ram&var-pod=%{browser_pod}"
                + "&from=%{from_timestamp}&to=%{to_timestamp}";
        String expectedLink = "https://dashboard-service-address/d/kzqCPg_Wk/atp-cloud-pods?orgId=3&var-gr_prefix"
                + "=teams.oshobj&var-cluster=atp-cloud&var-ns=prod&var-app_type=atp-ram&var-pod=browserName1&"
                + "from=" + tr.getStartDate().getTime() + "&to=" + tr.getFinishDate().getTime();
        ReflectionTestUtils.setField(testRunService, "browserMonitoringLinkTemplate", template);
        when(testRunRepository.findById(trId)).thenReturn(Optional.of(tr));

        SimpleTestRunResponse simpleTestRunResponse = testRunService.getTestRunByIdWithParent(trId).getSimpleTestRun();

        Assertions.assertEquals(1, simpleTestRunResponse.getBrowserInfos().size(),
                "SimpleTestRunResponse must contain one browser name and link");
        Assertions.assertEquals(tr.getBrowserNames().get(0),
                simpleTestRunResponse.getBrowserInfos().get(0).getBrowserName());
        Assertions.assertEquals(expectedLink, simpleTestRunResponse.getBrowserInfos().get(0).getBrowserUrl());
    }

    @Test
    public void getTestRunByIdWithParent_givenEmptyBrowserMonitoringLinkTemplate_canSetBrowserNameWithoutUrl() {
        UUID trId = UUID.randomUUID();
        TestRun tr = new TestRun();
        tr.setUuid(trId);
        tr.setBrowserNames(singletonList("browserName1"));
        tr.setStartDate(Timestamp.valueOf("2022-01-01 01:01:01.001"));
        tr.setFinishDate(Timestamp.valueOf("2022-01-01 01:02:00.003"));
        when(testRunRepository.findById(trId)).thenReturn(Optional.of(tr));

        SimpleTestRunResponse simpleTestRunResponse = testRunService.getTestRunByIdWithParent(trId).getSimpleTestRun();

        Assertions.assertEquals(1, simpleTestRunResponse.getBrowserInfos().size(),
                "SimpleTestRunResponse must contain one browser name");
        Assertions.assertEquals(tr.getBrowserNames().get(0),
                simpleTestRunResponse.getBrowserInfos().get(0).getBrowserName());
        assertNull(simpleTestRunResponse.getBrowserInfos().get(0).getBrowserUrl(), "Browser URL must be null");
    }

    @Test
    public void getTestRunByIdWithParent_givenTestRunWithoutFinishDate_canGenerateBrowserLinkWithCurrentTimestamp() {
        UUID trId = UUID.randomUUID();
        TestRun tr = new TestRun();
        tr.setUuid(trId);
        tr.setBrowserNames(singletonList("browserName1"));
        tr.setStartDate(Timestamp.valueOf("2022-01-01 01:01:01.001"));
        String template = "https://dashboard-service-address/d/kzqCPg_Wk/atp-cloud-pods?orgId=3&var-gr_prefix=teams"
                + ".oshobj&var-cluster=atp-cloud&var-ns=prod&var-app_type=atp-ram&var-pod=%{browser_pod}"
                + "&from=%{from_timestamp}&to=%{to_timestamp}";
        ReflectionTestUtils.setField(testRunService, "browserMonitoringLinkTemplate", template);
        when(testRunRepository.findById(trId)).thenReturn(Optional.of(tr));

        SimpleTestRunResponse simpleTestRunResponse = testRunService.getTestRunByIdWithParent(trId).getSimpleTestRun();

        Assertions.assertEquals(1, simpleTestRunResponse.getBrowserInfos().size(),
                "SimpleTestRunResponse must contain one browser name and link");
        Assertions.assertEquals(tr.getBrowserNames().get(0),
                simpleTestRunResponse.getBrowserInfos().get(0).getBrowserName());
        String actualBrowserUrl = simpleTestRunResponse.getBrowserInfos().get(0).getBrowserUrl();
        Assertions.assertNotNull(Long.valueOf(actualBrowserUrl.substring(actualBrowserUrl.indexOf("&to=") + 4)),
                "No exception should be thrown when searching and parsing the 'to' value in the URL");
    }

    @Test
    public void getEnrichedTestRunsByExecutionRequestId_ShouldReturnEnrichedTestRuns() {
        EnrichedTestRun simpleTestRun = TestRunsMock.generateEnrichedTestRun(UUID.randomUUID());
        TestCaseLabelResponse testCaseLabelResponse = TestRunsMock.generateTestCaseLabelResponse(
                simpleTestRun.getTestCaseId());
        when(testRunRepository.findAllEnrichedTestRunsByExecutionRequestId(simpleTestRun.getExecutionRequestId()))
                .thenReturn(Collections.singletonList(simpleTestRun));
        when(catalogueService.getTestCaseLabelsByIds(Collections.singletonList(simpleTestRun)))
                .thenReturn(Collections.singletonList(testCaseLabelResponse));
        when(executionRequestRepository.findByUuid(any())).thenReturn(new ExecutionRequest());
        List<EnrichedTestRun> result =
                testRunService.getEnrichedTestRunsByExecutionRequestId(simpleTestRun.getExecutionRequestId());
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(2, result.get(0).getLabels().size());
        Assertions.assertEquals("label1", result.get(0).getLabels().get(0).getName());
        Assertions.assertEquals("label2", result.get(0).getLabels().get(1).getName());
    }

    @Test
    public void getEnrichedTestRunsByExecutionRequestId_ShouldReturnEnrichedTestRuns2() throws IOException {
        String testRunS =
                "{\"uuid\":\"accaf272-45a5-4234-995a-09587efe8cdf\",\"name\":\"6. [Navigation] Check navigation during clicking on URL logo\","
                        +
                        "\"parentTestRunId\":null,\"isGroupedTestRun\":false,\"executionRequestId\":\"b6f11a43-e714-4b27-8a56-a65a317e7619\",\"testCaseId\":null,"
                        +
                        "\"testCaseName\":null,\"executionStatus\":\"FINISHED\",\"testingStatus\":\"PASSED\",\"startDate\":null,\"finishDate\":null,\"duration\":0,"
                        +
                        "\"executor\":null,\"jiraTicket\":null,\"taHost\":[],\"qaHost\":[],\"solutionBuild\":[],\"rootCauseId\":\"9015ba95-3fa9-4036-ba69-da2f1ab743a2\","
                        +
                        "\"dataSetUrl\":null,\"flags\":null,\"dataSetListUrl\":null,\"logCollectorData\":null,\"fdrWasSent\":false,\"fdrLink\":null,\"numberOfScreens\":125,"
                        +
                        "\"urlToBrowserOrLogs\":null,\"urlToBrowserSession\":null,\"passedRate\":98,\"warningRate\":0,\"failedRate\":0,\"comment\":"
                        +
                        "{\"text\":\"success case https://some-pipeline\",\"html\":"
                        +
                        "\"success case <a href=\\\"https://some-pipeline\\\" target=\\\"_blank\\\">"
                        +
                        "https://some-pipeline</a>\"},\"metaInfo\":null,\"statistic\":null,"
                        +
                        "\"testScopeSection\":null,\"order\":0,\"labelIds\":[],\"browserNames\":null,\"isFinalTestRun\":false,\"initialTestRunId\":null,\"groupedTestRun\":false,"
                        + "\"finalTestRun\":false}";
        EnrichedTestRun simpleTestRun = new ObjectMapper().readValue(testRunS, EnrichedTestRun.class);

        when(testRunRepository.findAllEnrichedTestRunsByExecutionRequestId(simpleTestRun.getExecutionRequestId()))
                .thenReturn(Collections.singletonList(simpleTestRun));
        when(catalogueService.getTestCaseLabelsByIds(Collections.singletonList(simpleTestRun)))
                .thenCallRealMethod();
        when(executionRequestRepository.findByUuid(any())).thenReturn(new ExecutionRequest());

        List<EnrichedTestRun> result =
                testRunService.getEnrichedTestRunsByExecutionRequestId(simpleTestRun.getExecutionRequestId());

        Assertions.assertEquals(0, result.get(0).getLabels().size());
    }

    @Test
    public void getTestRunNodeWithFailedLogRecordsByTrIds_ShouldReturnListOfTestRunsWithFailedLogRecords() {
        RootCause rootCause1 = new RootCause(UUID.randomUUID(), RootCauseType.CUSTOM, "AT issue");
        RootCause rootCause2 = new RootCause(UUID.randomUUID(), RootCauseType.CUSTOM, "Design issue");

        TestRun testRun1 = TestRunsMock.generateTestRun("test run1");
        testRun1.setRootCauseId(rootCause1.getUuid());
        TestRun testRun2 = TestRunsMock.generateTestRun("test run2");
        testRun2.setRootCauseId(rootCause2.getUuid());
        TestRun testRun3 = TestRunsMock.generateTestRun("test run3");

        List<TestRun> testRuns = asList(testRun1, testRun2, testRun3);
        Set<UUID> testRunIds = StreamUtils.extractIds(testRuns, TestRun::getUuid);

        LogRecord logRecord1 = LogRecordMock.generateLogRecordWithTestRunId(testRun1.getUuid());
        LogRecord logRecord2 = LogRecordMock.generateLogRecordWithTestRunId(testRun1.getUuid());
        LogRecord logRecord3 = LogRecordMock.generateLogRecordWithTestRunId(testRun2.getUuid());

        when(testRunRepository.findTestRunForReportByUuidIn(testRunIds)).thenReturn(testRuns);

        List<LogRecord> testRun1LogRecords = Collections.singletonList(logRecord1);
        when(logRecordService.getAllByTestingStatusAndTestRunId(FAILED, testRun1.getUuid()))
                .thenReturn(testRun1LogRecords);

        List<LogRecord> testRun2LogRecords = asList(logRecord1, logRecord2);
        when(logRecordService.getAllByTestingStatusAndTestRunId(FAILED, testRun1.getUuid()))
                .thenReturn(testRun2LogRecords);

        List<LogRecord> testRun3LogRecords = Collections.singletonList(logRecord3);
        when(logRecordService.getAllByTestingStatusAndTestRunId(FAILED, testRun2.getUuid()))
                .thenReturn(testRun3LogRecords);

        TestRunNodeResponse testRunNode1 = modelMapper.map(testRun1, TestRunNodeResponse.class);
        testRunNode1.setFailureReason(rootCause1.getName());
        testRunNode1.setFailedStep(toFailedLogRecordNodeResponses(testRun1LogRecords));
        testRunNode1.setIssues(new HashSet<>());

        TestRunNodeResponse testRunNode2 = modelMapper.map(testRun2, TestRunNodeResponse.class);
        testRunNode2.setFailureReason(rootCause2.getName());
        testRunNode2.setFailedStep(toFailedLogRecordNodeResponses(testRun3LogRecords));
        testRunNode2.setIssues(new HashSet<>());

        TestRunNodeResponse testRunNode3 = modelMapper.map(testRun3, TestRunNodeResponse.class);
        testRunNode3.setIssues(new HashSet<>());

        List<TestRunNodeResponse> expRes = asList(testRunNode1, testRunNode2, testRunNode3);

        Map<UUID, List<LogRecord>> testRunLogRecordsMap = new HashMap<>();
        testRunLogRecordsMap.put(testRun1.getUuid(), testRun2LogRecords);
        testRunLogRecordsMap.put(testRun2.getUuid(), testRun3LogRecords);

        Map<UUID, String> rootCausesMap = new HashMap<>();
        rootCausesMap.put(rootCause1.getUuid(), rootCause1.getName());
        rootCausesMap.put(rootCause2.getUuid(), rootCause2.getName());

        TestRunsDataContext context = TestRunsDataContext.builder()
                .testRunFailedLogRecordsMap(testRunLogRecordsMap)
                .testRunTestCasesMap(new HashMap<>())
                .testRunDslNamesMap(new HashMap<>())
                .rootCausesMap(rootCausesMap)
                .build();

        List<TestRunNodeResponse> actRes =
                testRunService.getTestRunNodeWithFailedLogRecords(testRuns, null, context);

        Assertions.assertEquals(expRes, actRes, "List of test runs with failed steps are valid");
    }

    @Test
    public void getTestRunNodeWithFailedLogRecordsByErId_ShouldReturnListOfTestRunsWithFailedLogRecords() {
        RootCause rootCause1 = new RootCause(UUID.randomUUID(), RootCauseType.CUSTOM, "AT issue");
        RootCause rootCause2 = new RootCause(UUID.randomUUID(), RootCauseType.CUSTOM, "Design issue");

        TestRun testRun1 = TestRunsMock.generateTestRun("test run1");
        testRun1.setRootCauseId(rootCause1.getUuid());
        TestRun testRun2 = TestRunsMock.generateTestRun("test run2");
        testRun2.setRootCauseId(rootCause2.getUuid());
        TestRun testRun3 = TestRunsMock.generateTestRun("test run3");

        List<TestRun> testRuns = asList(testRun1, testRun2, testRun3);
        when(testRunRepository.findTestRunForReportByExecutionRequestId(any()))
                .thenReturn(testRuns);

        LogRecord logRecord1 = LogRecordMock.generateLogRecordWithTestRunId(testRun1.getUuid());
        List<LogRecord> testRun1LogRecords = Collections.singletonList(logRecord1);
        when(logRecordService.getAllByTestingStatusAndTestRunId(FAILED, testRun1.getUuid()))
                .thenReturn(testRun1LogRecords);

        LogRecord logRecord2 = LogRecordMock.generateLogRecordWithTestRunId(testRun1.getUuid());
        List<LogRecord> testRun2LogRecords = asList(logRecord1, logRecord2);
        when(logRecordService.getAllByTestingStatusAndTestRunId(FAILED, testRun1.getUuid()))
                .thenReturn(testRun2LogRecords);

        LogRecord logRecord3 = LogRecordMock.generateLogRecordWithTestRunId(testRun2.getUuid());
        List<LogRecord> testRun3LogRecords = Collections.singletonList(logRecord3);
        when(logRecordService.getAllByTestingStatusAndTestRunId(FAILED, testRun2.getUuid()))
                .thenReturn(testRun3LogRecords);

        TestRunNodeResponse testRunNode1 = modelMapper.map(testRun1, TestRunNodeResponse.class);
        testRunNode1.setFailureReason("AT issue");
        testRunNode1.setFailedStep(toFailedLogRecordNodeResponses(testRun1LogRecords));
        testRunNode1.setIssues(new HashSet<>());

        TestRunNodeResponse testRunNode2 = modelMapper.map(testRun2, TestRunNodeResponse.class);
        testRunNode2.setFailureReason("Design issue");
        testRunNode2.setFailedStep(toFailedLogRecordNodeResponses(testRun3LogRecords));
        testRunNode2.setIssues(new HashSet<>());

        TestRunNodeResponse testRunNode3 = modelMapper.map(testRun3, TestRunNodeResponse.class);
        testRunNode3.setIssues(new HashSet<>());
        List<TestRunNodeResponse> expRes = asList(testRunNode1, testRunNode2, testRunNode3);

        Map<UUID, List<LogRecord>> testRunLogRecordsMap = new HashMap<>();
        testRunLogRecordsMap.put(testRun1.getUuid(), testRun2LogRecords);
        testRunLogRecordsMap.put(testRun2.getUuid(), testRun3LogRecords);

        Map<UUID, String> rootCausesMap = new HashMap<>();
        rootCausesMap.put(rootCause1.getUuid(), rootCause1.getName());
        rootCausesMap.put(rootCause2.getUuid(), rootCause2.getName());

        TestRunsDataContext context = TestRunsDataContext.builder()
                .testRunFailedLogRecordsMap(testRunLogRecordsMap)
                .testRunTestCasesMap(new HashMap<>())
                .testRunDslNamesMap(new HashMap<>())
                .rootCausesMap(rootCausesMap)
                .build();

        List<TestRunNodeResponse> actRes =
                testRunService.getTestRunNodeWithFailedLogRecords(testRuns, null, context);

        Assertions.assertEquals(expRes, actRes, "List of test runs with failed steps are valid");
    }

    @Test
    public void getStatusUpdate_shouldSuccessfullyExecuted() throws ParseException {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.ENGLISH);

        TestRun testRun1 = TestRunsMock.generateTestRun("Test run 1");
        UUID testRun1Id = testRun1.getUuid();

        LogRecord logRecord1 = LogRecordMock.generateLogRecordWithTestRunId(
                testRun1Id, format.parse("2020-07-18T17:00:00.000"), PASSED, FINISHED);
        LogRecord logRecord2 = LogRecordMock.generateLogRecordWithTestRunId(
                testRun1Id, format.parse("2020-07-18T17:05:00.000"), FAILED, FINISHED);

        TestRun testRun2 = TestRunsMock.generateTestRun("Test run 2");
        UUID testRun2Id = testRun2.getUuid();

        LogRecord logRecord3 = LogRecordMock.generateLogRecordWithTestRunId(
                testRun2Id, format.parse("2020-07-18T17:02:00.000"), PASSED, FINISHED);
        LogRecord logRecord4 = LogRecordMock.generateLogRecordWithTestRunId(
                testRun2Id, format.parse("2020-07-18T17:15:00.000"), PASSED, FINISHED);
        LogRecord logRecord5 = LogRecordMock.generateLogRecordWithTestRunId(
                testRun2Id, format.parse("2020-07-18T17:20:00.000"), UNKNOWN, IN_PROGRESS);

        Date lastLoaded = format.parse("2020-07-18T17:03:00.000");

        List<TestRun> testRuns = asList(testRun1, testRun2);

        when(testRunRepository.findShortTestRunsByUuidIn(asList(testRun1Id, testRun2Id)))
                .thenReturn(testRuns);

        when(logRecordService.findAllByLastUpdatedAfterAndTestRunIdIn(lastLoaded, asList(testRun1Id, testRun2Id)))
                .thenReturn(asList(logRecord2, logRecord4, logRecord5));

        StatusUpdateRequest request = new StatusUpdateRequest();
        request.setLastLoaded(lastLoaded);
        request.setTestRunsIds(asList(testRun1Id, testRun2Id));

        StatusUpdateResponse result = testRunService.getStatusUpdate(request);

        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getLastLoaded());
        assertTrue(result.getLastLoaded().after(request.getLastLoaded()));

        final List<TestRunStatusUpdateResponse> resultTestRuns = result.getTestRuns();
        Assertions.assertNotNull(resultTestRuns);
        Assertions.assertFalse(resultTestRuns.isEmpty());
        Assertions.assertEquals(2, resultTestRuns.size());

        Assertions.assertArrayEquals(
                StreamUtils.extractIds(resultTestRuns, TestRunStatusUpdateResponse::getId).toArray(),
                StreamUtils.extractIds(testRuns).toArray());

        final TestRunStatusUpdateResponse resultTestRun1 = resultTestRuns.get(0);
        Assertions.assertNotNull(resultTestRun1);
        Assertions.assertEquals(testRun1Id, resultTestRun1.getId());

        final List<BaseStatusUpdateResponse> resultTestRun1LogRecords = resultTestRun1.getLogRecords();
        Assertions.assertNotNull(resultTestRun1LogRecords);
        Assertions.assertFalse(resultTestRun1LogRecords.isEmpty());
        final Set<UUID> resultTestRun1LogRecordIds =
                StreamUtils.extractIds(resultTestRun1LogRecords, BaseStatusUpdateResponse::getId);
        Assertions.assertEquals(1, resultTestRun1LogRecordIds.size());
        assertTrue(resultTestRun1LogRecordIds.contains(logRecord2.getUuid()));

        final TestRunStatusUpdateResponse resultTestRun2 = resultTestRuns.get(1);
        Assertions.assertNotNull(resultTestRun2);
        Assertions.assertEquals(testRun2Id, resultTestRun2.getId());

        final List<BaseStatusUpdateResponse> resultTestRun2LogRecords = resultTestRun2.getLogRecords();
        Assertions.assertNotNull(resultTestRun2LogRecords);
        Assertions.assertFalse(resultTestRun2LogRecords.isEmpty());
        final Set<UUID> resultTestRun2LogRecordIds =
                StreamUtils.extractIds(resultTestRun2LogRecords, BaseStatusUpdateResponse::getId);
        Assertions.assertEquals(2, resultTestRun2LogRecordIds.size());
        assertTrue(resultTestRun2LogRecordIds.contains(logRecord4.getUuid()));
        assertTrue(resultTestRun2LogRecordIds.contains(logRecord5.getUuid()));
    }

    @Test
    public void testUpdateTestRunsTestingStatus_shouldUpdateTestingStatus_whenSuccessful() {
        TestRun testRunFailed = TestRunsMock.generateSimpleTestRun(UUID.randomUUID());
        String newTestStatusFailed = "FAILED";
        TestRun testRunStopped = TestRunsMock.generateSimpleTestRun(UUID.randomUUID());
        String newTestStatusStopped = "STOPPED";
        TestingStatusUpdateRequest testingStatusUpdReqFailed = new TestingStatusUpdateRequest(testRunFailed.getUuid(),
                newTestStatusFailed);
        TestingStatusUpdateRequest testingStatusUpdReqStopped = new TestingStatusUpdateRequest(testRunStopped.getUuid(),
                newTestStatusStopped);

        List<TestRun> testRuns = asList(testRunFailed, testRunStopped);
        when(testRunRepository.findAllByUuidIn(anyList())).thenReturn(testRuns);

        List<TestingStatusUpdateRequest> testingStatusUpdateRequests =
                asList(testingStatusUpdReqFailed, testingStatusUpdReqStopped);
        testRunService.updateTestRunsTestingStatus(testingStatusUpdateRequests);
        Assertions.assertEquals(FAILED, testRunFailed.getTestingStatus(), "Test run does not have correct status");
        Assertions.assertEquals(STOPPED, testRunStopped.getTestingStatus(), "Test run does not have correct status");
        verify(testRunRepository, times(1)).saveAll(testRuns);
    }

    @Test
    public void testUpdTestingStatus_shouldSentRequestToCatalog_whenReplaceLastRun() {

        TestRun testRun = TestRunsMock.generateSimpleTestRun(UUID.randomUUID());

        when(testRunRepository.findById(eq(testRun.getUuid()))).thenReturn(Optional.of(testRun));
        when(testRunRepository.findFirstByTestCaseIdOrderByStartDateDesc(eq(testRun.getTestCaseId())))
                .thenReturn(testRun);

        testRunService.updTestingStatusHard(testRun.getUuid(), FAILED);

        verify(testCaseService, times(1)).updateCaseStatuses(Collections.singletonList(testRun));
        Assertions.assertEquals(FAILED, testRun.getTestingStatus());
    }

    @Test
    public void testUpdTestingStatus_shouldNotSentRequestToCatalog_whenReplaceNotLastRun() {

        TestRun testRun = TestRunsMock.generateSimpleTestRun(UUID.randomUUID());
        TestRun lastTestRun = TestRunsMock.generateSimpleTestRun(UUID.randomUUID());

        when(testRunRepository.findById(eq(testRun.getUuid()))).thenReturn(Optional.of(testRun));
        when(testRunRepository.findFirstByTestCaseIdOrderByStartDateDesc(eq(testRun.getTestCaseId())))
                .thenReturn(lastTestRun);

        testRunService.updTestingStatusHard(testRun.getUuid(), FAILED);

        verifyZeroInteractions(testCaseService);
        Assertions.assertEquals(FAILED, testRun.getTestingStatus());
    }

    @Test
    public void patch_shouldUpdatePropertyFromPatch() {
        TestRun storedTestRun = TestRunsMock.generateSimpleTestRun(UUID.randomUUID());
        TestRun testRunPatch = TestRunsMock.generateSimpleTestRun(storedTestRun.getUuid());
        when(testRunRepository.findByUuid(eq(testRunPatch.getUuid()))).thenReturn(storedTestRun);
        Timestamp finishDate = new Timestamp(System.currentTimeMillis());
        testRunPatch.setFinishDate(finishDate);
        testRunService.patch(testRunPatch);

        Mockito.verify(testRunRepository, times(1))
                .save(argThat(testRun -> testRun.getFinishDate().equals(finishDate)));
    }

    @Test
    public void patch_shouldIgnoreEmptyCollectionsInPatch() {
        TestRun storedTestRun = TestRunsMock.generateSimpleTestRun(UUID.randomUUID());
        TestRun testRunPatch = TestRunsMock.generateSimpleTestRun(storedTestRun.getUuid());
        when(testRunRepository.findByUuid(eq(testRunPatch.getUuid()))).thenReturn(storedTestRun);
        List<String> storedTaHosts = storedTestRun.getTaHost();
        testRunPatch.setTaHost(emptyList());
        testRunService.patch(testRunPatch);

        Mockito.verify(testRunRepository, times(1))
                .save(argThat(testRun -> testRun.getTaHost().equals(storedTaHosts)));
    }

    @Test
    public void patch_shouldIgnoreNullFieldsInPatch() {
        TestRun storedTestRun = TestRunsMock.generateSimpleTestRun(UUID.randomUUID());
        TestRun testRunPatch = TestRunsMock.generateSimpleTestRun(storedTestRun.getUuid());
        when(testRunRepository.findByUuid(eq(testRunPatch.getUuid()))).thenReturn(storedTestRun);
        TestingStatuses storedTestingStatus = PASSED;
        storedTestRun.setTestingStatus(storedTestingStatus);
        testRunPatch.setTestingStatus(null);
        testRunService.patch(testRunPatch);

        Mockito.verify(testRunRepository, times(1))
                .save(argThat(testRun -> testRun.getTestingStatus().equals(storedTestingStatus)));
    }

    @Test
    public void patch_durationShouldBeCalculatedProperly() {
        TestRun storedTestRun = TestRunsMock.generateSimpleTestRun(UUID.randomUUID());
        TestRun testRunPatch = TestRunsMock.generateSimpleTestRun(storedTestRun.getUuid());
        when(testRunRepository.findByUuid(eq(testRunPatch.getUuid()))).thenReturn(storedTestRun);
        Timestamp storedStartDate = storedTestRun.getStartDate();
        Timestamp finishDate = new Timestamp(System.currentTimeMillis());
        testRunPatch.setFinishDate(finishDate);
        long duration = TimeUnit.MILLISECONDS.toSeconds(finishDate.getTime() - storedStartDate.getTime());
        testRunService.patch(testRunPatch);

        Mockito.verify(testRunRepository, times(1))
                .save(argThat(testRun -> testRun.getDuration() == duration));
    }

    @Test
    public void finishTestRun_notFinalStatusIsUpdated() {
        TestRun testRun = TestRunsMock.generateSimpleTestRun(UUID.randomUUID());
        testRun.setExecutionStatus(IN_PROGRESS);
        testRunService.finishTestRun(testRun, true);
        Assertions.assertEquals(FINISHED, testRun.getExecutionStatus(), "ExecutionStatus should be set to FINISHED");
    }

    @Test
    public void finishTestRun_FinalStatusIsNotUpdated() {
        TestRun testRun = TestRunsMock.generateSimpleTestRun(UUID.randomUUID());
        testRun.setExecutionStatus(TERMINATED);
        testRunService.finishTestRun(testRun, true);
        Assertions.assertEquals(TERMINATED, testRun.getExecutionStatus(), "ExecutionStatus should not be updated");
    }

    @Test
    public void finishTestRun_DurationShouldBeUpdateInCaseOfDelayedIsTrue() {
        long expectedDurationInSeconds = 10;
        TestRun testRun = TestRunsMock.generateSimpleTestRun(UUID.randomUUID());
        Timestamp startDate = new Timestamp(testRun.getStartDate().getTime() - expectedDurationInSeconds * 1000);
        testRun.setStartDate(startDate);
        testRun.setDuration(0);
        testRunService.finishTestRun(testRun, false);
        assertTrue(testRun.getDuration() >= 10, "Duration should be updated");
    }

    @Test
    public void finishTestRuns_AllTestRunsStatusesShouldBeUpdated() {
        TestRun testRun1 = TestRunsMock.generateSimpleTestRun(UUID.randomUUID());
        TestRun testRun2 = TestRunsMock.generateSimpleTestRun(UUID.randomUUID());
        when(testRunRepository.findAllByUuidIn(argThat(ids ->
                ids.contains(testRun1.getUuid()) && ids.contains(testRun2.getUuid()) && ids.size() == 2)))
                .thenReturn(asList(testRun1, testRun2));
        testRunService.finishTestRuns(
                Stream.of(testRun1, testRun2).map(RamObject::getUuid).collect(Collectors.toList()), true);
        Mockito.verify(testRunRepository, times(1)).saveAll(argThat(
                testRuns -> StreamSupport
                        .stream(testRuns.spliterator(), false)
                        .allMatch(testRun -> testRun.getExecutionStatus().equals(FINISHED))
        ));
    }

    @Test
    public void finishTestRuns_DurationsShouldBeUpdatedInCaseOfDelayedIsTrue() {
        long expectedDurationInSeconds = 10;
        TestRun testRun1 = TestRunsMock.generateSimpleTestRun(UUID.randomUUID());
        TestRun testRun2 = TestRunsMock.generateSimpleTestRun(UUID.randomUUID());
        Timestamp startDate = new Timestamp(testRun1.getStartDate().getTime() - expectedDurationInSeconds * 1000);
        testRun1.setStartDate(startDate);
        testRun2.setStartDate(startDate);
        when(testRunRepository.findAllByUuidIn(argThat(ids ->
                ids.contains(testRun1.getUuid()) && ids.contains(testRun2.getUuid()) && ids.size() == 2)))
                .thenReturn(asList(testRun1, testRun2));
        testRunService.finishTestRuns(
                Stream.of(testRun1, testRun2).map(RamObject::getUuid).collect(Collectors.toList()), false);
        Mockito.verify(testRunRepository, times(1)).saveAll(argThat(
                testRuns -> StreamSupport
                        .stream(testRuns.spliterator(), false)
                        .allMatch(testRun -> testRun.getDuration() >= 10)
        ));
    }

    @Test
    public void findTestRunsByNamesLabelsValidationLabelsTest_shouldFilterTestRunsByValidationLabels() {
        // given
        UUID executionRequestId = UUID.randomUUID();
        UUID testRunId = UUID.randomUUID();
        String testRunName = "testRunName";
        // testing status from UI is in upper case
        String testRunTestingStatus = TestingStatuses.PASSED.getName().toUpperCase();
        String validationLabelName = "validationLabel";
        Set<String> validationLabels = Sets.newHashSet(validationLabelName);
        ValidationLabelFilterRequest validationLabelFilter = new ValidationLabelFilterRequest(validationLabelName,
                singletonList(testRunTestingStatus));
        TestRunsByValidationLabelsRequest filter = new TestRunsByValidationLabelsRequest(
                null, null, singletonList(validationLabelFilter));
        TestRunWithValidationLabelsResponse response = TestRunsMock.generateTestRunWithValidationLabelsResponse(
                testRunId, testRunName, testRunTestingStatus, validationLabels);
        List<TestRunWithValidationLabelsResponse> expectedTestRunsResponses = singletonList(response);
        TestRun testRun = TestRunsMock.generateTestRun(testRunName, TestingStatuses.PASSED);
        testRun.setUuid(testRunId);

        LogRecord logRecord = LogRecordMock.generateLogRecordWithValidationLabels(testRunId,
                new HashSet<>(validationLabels));
        // when
        when(testRunRepository.findTestRunsByExecutionRequestIdAndNamesAndLabelIds(any(), any(), any())).thenReturn(
                singletonList(testRun));
        when(logRecordService.getAllLogRecordsByTestRunIds(any())).thenReturn(singletonList(logRecord));
        List<TestRunWithValidationLabelsResponse> actualTestRunsResponses =
                testRunService.findTestRunsByNamesLabelsValidationLabels(executionRequestId, filter);

        // then
        Assertions.assertEquals(expectedTestRunsResponses, actualTestRunsResponses);
    }

    @Test
    public void findTestRunsByNamesLabelsValidationLabelsTest_testRunWithoutValidationLabel_shouldFilterTestRunsByValidationLabels() {
        // given
        UUID executionRequestId = UUID.randomUUID();
        UUID testRunId = UUID.randomUUID();
        String testRunName = "testRunName";
        String testRunTestingStatus = TestingStatuses.PASSED.getName().toUpperCase();
        String validationLabelName = "validationLabel";
        String validationLabelNA = "N/A";
        ValidationLabelFilterRequest validationLabelFilter = new ValidationLabelFilterRequest(validationLabelName,
                singletonList(validationLabelNA));
        TestRunsByValidationLabelsRequest filter = new TestRunsByValidationLabelsRequest(
                null, null, singletonList(validationLabelFilter));
        TestRunWithValidationLabelsResponse response = TestRunsMock.generateTestRunWithValidationLabelsResponse(
                testRunId, testRunName, testRunTestingStatus, new HashSet<>());
        List<TestRunWithValidationLabelsResponse> expectedTestRunsResponses = singletonList(response);

        TestRun testRun = TestRunsMock.generateTestRun(testRunName, PASSED);
        testRun.setUuid(testRunId);

        LogRecord logRecord = LogRecordMock.generateLogRecordWithValidationLabels(testRunId, new HashSet<>());
        // when
        when(testRunRepository.findTestRunsByExecutionRequestIdAndNamesAndLabelIds(any(), any(), any())).thenReturn(
                singletonList(testRun));
        when(logRecordService.getAllLogRecordsByTestRunIds(any())).thenReturn(singletonList(logRecord));
        List<TestRunWithValidationLabelsResponse> actualTestRunsResponses =
                testRunService.findTestRunsByNamesLabelsValidationLabels(executionRequestId, filter);

        // then
        Assertions.assertEquals(expectedTestRunsResponses, actualTestRunsResponses);
    }

    @Test
    public void findTestRunsByTwoValidationLabels_shouldFilterTestRunsByValidationLabels() {
        // given
        UUID executionRequestId = UUID.randomUUID();
        UUID testRunId = UUID.randomUUID();
        String testRunName = "testRunName";
        String validationLabelName1 = "validationLabel1";
        String validationLabelName2 = "validationLabel2";
        String validationLabel1NA = "N/A";
        String validationLabel2Passed = PASSED.getName().toUpperCase();

        ValidationLabelFilterRequest validationLabelFilter1 = new ValidationLabelFilterRequest(validationLabelName1,
                singletonList(validationLabel1NA));
        ValidationLabelFilterRequest validationLabelFilter2 = new ValidationLabelFilterRequest(validationLabelName2,
                singletonList(validationLabel2Passed));
        TestRunsByValidationLabelsRequest filter = new TestRunsByValidationLabelsRequest(
                null, null, Arrays.asList(validationLabelFilter1, validationLabelFilter2));
        TestRun testRun = TestRunsMock.generateTestRun(testRunName, PASSED);
        testRun.setUuid(testRunId);
        TestRunWithValidationLabelsResponse response = TestRunsMock.generateTestRunWithValidationLabelsResponse(
                testRunId, testRunName, testRun.getTestingStatus().getName().toUpperCase(),
                Sets.newHashSet(validationLabelName2));
        List<TestRunWithValidationLabelsResponse> expectedTestRunsResponses = singletonList(response);

        LogRecord logRecord = LogRecordMock.generateLogRecordWithValidationLabels(testRunId,
                Sets.newHashSet(validationLabelName2));
        // when
        when(testRunRepository.findTestRunsByExecutionRequestIdAndNamesAndLabelIds(any(), any(), any())).thenReturn(
                singletonList(testRun));
        when(logRecordService.getAllLogRecordsByTestRunIds(any())).thenReturn(singletonList(logRecord));
        List<TestRunWithValidationLabelsResponse> actualTestRunsResponses =
                testRunService.findTestRunsByNamesLabelsValidationLabels(executionRequestId, filter);

        // then
        Assertions.assertEquals(expectedTestRunsResponses, actualTestRunsResponses);
    }

    @Test
    public void getAnalyzedTestRunsTest_filterWithLabelNameAndLabelContainsConfigured_shouldSendPageableRequestWithLabelIds() {
        // given
        UUID testCaseId = UUID.randomUUID();
        TestRun testRun = TestRunsMock.generateTestRunWithId("testRun", testCaseId);
        Label label1 = LabelMock.generateLabelWithName("label1");
        Label label2 = LabelMock.generateLabelWithName("label2");
        TestCaseLabelResponse testCaseLabelResponse = TestRunsMock
                .generateTestCaseLabelResponseWithLabelIds(testCaseId, Arrays.asList(label1, label2));
        List<RootCause> rootCauses = RootCauseMock.getAllRootCauses();
        PaginationResponse<TestRun> paginationResponse = new PaginationResponse<>(Collections.singletonList(testRun),
                1);
        TestRunSearchRequest filter = new TestRunSearchRequest();
        filter.setLabelNames(Collections.singletonList("label1"));
        filter.setLabelNameContains(Collections.singletonList("label"));
        // when
        when(testRunRepository.findAllByFilter(anyInt(), anyInt(), any(), any(), any())).thenReturn(paginationResponse);
        when(catalogueService.getTestCaseLabelsByIds(any()))
                .thenReturn(Collections.singletonList(testCaseLabelResponse));
        when(rootCauseRepository.findAll()).thenReturn(rootCauses);

        AnalyzedTestRunResponse actualResponse =
                testRunService.getAnalyzedTestRuns(0, 0, null, null, filter);
        // then
        assertTrue(filter.getLabelIds().contains(label1.getUuid()));
        assertTrue(filter.getLabelIds().contains(label2.getUuid()));
        Assertions.assertEquals(Integer.valueOf(1), actualResponse.getTotalNumberOfEntities());
        Assertions.assertEquals(2, actualResponse.getTestRuns().get(0).getLabels().size());
        assertTrue(actualResponse.getTestRuns().get(0).getLabels().stream().anyMatch(label -> label.equals(label1)));
        assertTrue(actualResponse.getTestRuns().get(0).getLabels().stream().anyMatch(label -> label.equals(label2)));
    }

    @Test
    public void getAnalyzedTestRunsTest_filterWithLabelNameAndNotFilteredLabelsConfigured_shouldSendPageableRequestWithLabelIds() {
        // given
        UUID testCaseId = UUID.randomUUID();
        Label label1 = LabelMock.generateLabelWithName("label1");
        Label label2 = LabelMock.generateLabelWithName("label2");
        TestCaseLabelResponse testCaseLabelResponse = TestRunsMock
                .generateTestCaseLabelResponseWithLabelIds(testCaseId, Arrays.asList(label1, label2));
        List<RootCause> rootCauses = RootCauseMock.getAllRootCauses();
        PaginationResponse<TestRun> paginationResponse = new PaginationResponse<>(Collections.emptyList(), 0);
        TestRunSearchRequest filter = new TestRunSearchRequest();
        filter.setLabelNames(Collections.singletonList("another_lbl"));
        // when
        when(testRunRepository.findAllByFilter(anyInt(), anyInt(), any(), any(), any())).thenReturn(paginationResponse);
        when(catalogueService.getTestCaseLabelsByIds(any()))
                .thenReturn(Collections.singletonList(testCaseLabelResponse));
        when(rootCauseRepository.findAll()).thenReturn(rootCauses);

        AnalyzedTestRunResponse actualResponse =
                testRunService.getAnalyzedTestRuns(0, 0, null, null, filter);
        // then
        assertTrue(CollectionUtils.isEmpty(filter.getLabelIds()));
        Assertions.assertEquals(Integer.valueOf(0), actualResponse.getTotalNumberOfEntities());
        assertTrue(CollectionUtils.isEmpty(actualResponse.getTestRuns()));
    }

    @Test
    public void getAnalyzedTestRunsTest_filterWithLabelNameContainsConfigured_shouldSendPageableRequestWithLabelIds() {
        // given
        UUID testCaseId = UUID.randomUUID();
        Label label1 = LabelMock.generateLabelWithName("label1");
        Label label2 = LabelMock.generateLabelWithName("label2");
        TestCaseLabelResponse testCaseLabelResponse = TestRunsMock
                .generateTestCaseLabelResponseWithLabelIds(testCaseId, Arrays.asList(label1, label2));
        List<RootCause> rootCauses = RootCauseMock.getAllRootCauses();
        PaginationResponse<TestRun> paginationResponse = new PaginationResponse<>(Collections.emptyList(), 0);
        TestRunSearchRequest filter = new TestRunSearchRequest();
        filter.setLabelNameContains(Collections.singletonList("my"));
        // when
        when(testRunRepository.findAllByFilter(anyInt(), anyInt(), any(), any(), any())).thenReturn(paginationResponse);
        when(catalogueService.getTestCaseLabelsByIds(any()))
                .thenReturn(Collections.singletonList(testCaseLabelResponse));
        when(rootCauseRepository.findAll()).thenReturn(rootCauses);

        AnalyzedTestRunResponse actualResponse =
                testRunService.getAnalyzedTestRuns(0, 0, null, null, filter);
        // then
        assertTrue(CollectionUtils.isEmpty(filter.getLabelIds()));
        Assertions.assertEquals(Integer.valueOf(0), actualResponse.getTotalNumberOfEntities());
        assertTrue(CollectionUtils.isEmpty(actualResponse.getTestRuns()));
    }

    @Test
    public void propagateDefectsToComment_shouldBeSuccessfullyPropagated() {
        // given
        final TestRun testRun1 = TestRunsMock.generateTestRun("Test Run 1");
        final TestRun testRun2 = TestRunsMock.generateTestRun("Test Run 2");

        final UUID testRun1Id = testRun1.getUuid();
        final UUID testRun2Id = testRun2.getUuid();
        final Set<UUID> testRunIds = Sets.newHashSet(testRun1Id, testRun2Id);

        final Issue issue1 = IssueMock.generateIssue("Issue 1", testRun1Id,
                new JiraTicket("https://service-address/browse/SOMEPROJECT-98765"));
        final Issue issue2 = IssueMock.generateIssue("Issue 2", testRun1Id);
        final Issue issue3 = IssueMock.generateIssue("Issue 3", testRun2Id,
                new JiraTicket("https://service-address/browse/SOMEPROJECT-12345"));

        final ExecutionRequest executionRequest = ExecutionRequestsMock.generateRequest();
        final UUID executionRequestId = executionRequest.getUuid();
        final UUID testPlanId = randomUUID();
        executionRequest.setTestPlanId(testPlanId);

        final TestRunDefectsPropagationRequest request = new TestRunDefectsPropagationRequest(
                UUID.randomUUID(), executionRequestId, testRunIds);

        when(executionRequestRepository.findByUuid(executionRequestId)).thenReturn(executionRequest);
        when(issueService.getAllIssuesByTestRunIds(eq(executionRequestId), any())).thenReturn(
                asList(issue1, issue2, issue3));
        when(catalogueService.searchIssues(testPlanId,
                Sets.newHashSet("SOMEPROJECT-98765", "SOMEPROJECT-12345"),
                Sets.newHashSet("summary")))
                .thenReturn(asList(
                        getIssueDto("SOMEPROJECT-98765",
                                "https://service-address/rest/api/2/issue/12367089",
                                "Filtering by AT status when send report"),
                        getIssueDto("SOMEPROJECT-12345",
                                "https://service-address/rest/api/2/issue/11344515",
                                "SSE connections errors")
                ));
        when(testRunRepository.findAllByUuidIn(testRunIds)).thenReturn(asList(testRun1, testRun2));

        // when
        final TestRunDefectsPropagationResponse response = testRunService.propagateDefectsToComments(request);

        verify(testRunRepository, times(2)).save(testRunSaveCaptor.capture());

        // then
        Assertions.assertNotNull(response, "Response shouldn't be null");

        final List<Item> failedTestRuns = response.getFailedTestRuns();
        Assertions.assertNull(failedTestRuns, "Failed test runs should be null");

        final List<Item> successTestRuns = response.getSuccessTestRuns();
        Assertions.assertEquals(2, successTestRuns.size(), "Success test runs should contain 2 items");

        final Item firstTestRun = findFirstInList(successTestRuns, item -> item.getName().equals(testRun1.getName()));
        Assertions.assertNotNull(firstTestRun, "First test run should be in success test runs");

        final Item secondTestRun = findFirstInList(successTestRuns, item -> item.getName().equals(testRun2.getName()));
        Assertions.assertNotNull(secondTestRun, "Second test run should be in success test runs");

        final List<TestRun> savedTestRuns = testRunSaveCaptor.getAllValues();
        Assertions.assertEquals(2, savedTestRuns.size(), "Two test runs should be saved");

        final TestRun firstSavedTestRun =
                findFirstInList(savedTestRuns, item -> item.getUuid().equals(testRun1.getUuid()));
        Assertions.assertNotNull(firstSavedTestRun, "First test run should be saved");

        final Comment firstSavedTestRunComment = firstSavedTestRun.getComment();
        Assertions.assertNotNull(firstSavedTestRunComment, "First test run should be saved with comment");

        final String firstSavedTestRunCommentText = firstSavedTestRunComment.getText();
        Assertions.assertNotNull(firstSavedTestRunCommentText, "First saved test run comment should contain text");
        Assertions.assertTrue(firstSavedTestRunCommentText.contains("SOMEPROJECT-98765"),
                "First saved test run comment text should contain issue key");

        final String firstSavedTestRunCommentHtml = firstSavedTestRunComment.getHtml();
        Assertions.assertNotNull("First saved test run comment should contain html", firstSavedTestRunCommentHtml);
        Assertions.assertTrue(
                firstSavedTestRunCommentHtml.contains("https://service-address/browse/SOMEPROJECT-98765"),
                "First saved test run comment html should contain issue reference");
    }

    private JiraIssueDto getIssueDto(String key, String self, String summary) {
        final JiraIssueDto jiraIssueDto = new JiraIssueDto();
        jiraIssueDto.setKey(key);
        jiraIssueDto.setSelf(self);

        final FieldsDto fieldsDto = new FieldsDto();
        fieldsDto.setSummary(summary);
        jiraIssueDto.setFields(fieldsDto);

        return jiraIssueDto;
    }
}
