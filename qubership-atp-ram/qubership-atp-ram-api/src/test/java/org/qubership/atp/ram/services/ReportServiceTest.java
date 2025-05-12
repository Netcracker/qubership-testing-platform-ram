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

import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;
import org.qubership.atp.dataset.clients.dto.DatasetResponseDto;
import org.qubership.atp.ram.EnvironmentsInfoMock;
import org.qubership.atp.ram.ExecutionRequestsMock;
import org.qubership.atp.ram.TestRunsMock;
import org.qubership.atp.ram.TreeAssertWalker;
import org.qubership.atp.ram.TreeConsoleDrawer;
import org.qubership.atp.ram.client.DataSetListFeignClient;
import org.qubership.atp.ram.dto.response.ExecutionRequestWidgetConfigTemplateResponse;
import org.qubership.atp.ram.dto.response.ExecutionSummaryResponse;
import org.qubership.atp.ram.dto.response.LabelNodeReportResponse;
import org.qubership.atp.ram.dto.response.LabelNodeReportResponse.TestRunNodeResponse;
import org.qubership.atp.ram.dto.response.RootCausesStatisticResponse;
import org.qubership.atp.ram.dto.response.ServerSummaryResponse;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.LabelTemplate;
import org.qubership.atp.ram.models.LabelTemplate.LabelTemplateNode;
import org.qubership.atp.ram.models.RerunDetails;
import org.qubership.atp.ram.models.RootCause;
import org.qubership.atp.ram.models.RootCauseType;
import org.qubership.atp.ram.models.TestCaseWidgetReportRequest;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.models.tree.TreeWalker;
import org.qubership.atp.ram.repositories.ExecutionRequestRepository;
import org.qubership.atp.ram.repositories.RootCauseRepository;
import org.qubership.atp.ram.repositories.TestRunRepository;
import org.qubership.atp.ram.utils.LabelReportNodeMock;
import org.qubership.atp.ram.utils.PatchHelper;
import org.qubership.atp.ram.utils.RateCalculator;
import org.qubership.atp.ram.utils.StreamUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

public class ReportServiceTest {
    private EnvironmentsInfoService environmentsInfoService;
    private EnvironmentsService environmentsService;
    private ExecutionRequestRepository executionRequestRepository;
    private TestRunService testRunService;
    private ReportService reportService;
    private LabelTemplateNodeService labelTemplateNodeService;
    private LabelsService labelsService;
    private ValidationLabelConfigTemplateService validationLabelConfigTemplateService;
    private WidgetConfigTemplateService widgetConfigService;
    private TreeNodeService treeNodeService;
    private CatalogueService catalogueService;
    private RateCalculator rateCalculator;
    private final ModelMapper modelMapper = new ModelMapper();
    private RootCauseService rootCauseService;
    private TestRunRepository testRunRepository;
    private TestCaseService testCaseService;
    private IssueService issueService;
    private final PatchHelper patchHelper = new PatchHelper();

    @BeforeEach
    public void setUp() {
        environmentsInfoService = mock(EnvironmentsInfoService.class);
        environmentsService = mock(EnvironmentsService.class);
        executionRequestRepository = mock(ExecutionRequestRepository.class);
        testRunRepository = mock(TestRunRepository.class);
        rootCauseService = mock(RootCauseService.class);
        testCaseService = mock(TestCaseService.class);
        issueService = mock(IssueService.class);
        rateCalculator = mock(RateCalculator.class);
        labelsService = mock(LabelsService.class);

        DatasetResponseDto datasetResponseDto = new DatasetResponseDto();
        datasetResponseDto.setDataSetId(randomUUID());
        datasetResponseDto.setDataSetName("name");

        DataSetListFeignClient dataSetListFeignClient = Mockito.mock(DataSetListFeignClient.class);
        when(dataSetListFeignClient.getDataSetsWithNameAndDataSetList(anyList()))
                .thenReturn(new ResponseEntity<>(Collections.singletonList(datasetResponseDto), HttpStatus.OK));

        testRunService = new TestRunService(
                mock(MongoTemplate.class),
                mock(LogRecordService.class),
                testRunRepository,
                rootCauseService,
                mock(ProjectsService.class),
                mock(TestPlansService.class),
                modelMapper,
                mock(CatalogueService.class),
                dataSetListFeignClient,
                mock(ExecutionRequestRepository.class),
                mock(RootCauseRepository.class),
                mock(TreeNodeService.class),
                testCaseService,
                issueService,
                patchHelper,
                labelsService
        );

        labelTemplateNodeService = mock(LabelTemplateNodeService.class);
        labelsService = mock(LabelsService.class);
        validationLabelConfigTemplateService = mock(ValidationLabelConfigTemplateService.class);
        widgetConfigService = mock(WidgetConfigTemplateService.class);
        treeNodeService = mock(TreeNodeService.class);
        catalogueService = mock(CatalogueService.class);
        reportService = spy(new ReportService(environmentsInfoService, environmentsService, executionRequestRepository, testRunService,
                labelTemplateNodeService, labelsService, modelMapper, validationLabelConfigTemplateService,
                widgetConfigService, treeNodeService, rateCalculator, catalogueService));
        ReflectionTestUtils.setField(reportService, "catalogueUrl",
                "https://catalogue-secure-uat.dev-atp-cloud.some-domain.com");
    }

    @Test
    public void getSummaryTestRunsAndSetRerunMapping_LabelTemplateIsNotExist_ShouldReturnTreeWithTestRuns() {
        UUID initialErId = fromString("4fc39cee-2c9f-484c-826f-41b00aaa0000");
        ExecutionRequest initialExecutionRequest = new ExecutionRequest();
        initialExecutionRequest.setUuid(initialErId);
        TestRun initialTestRun1 = TestRunsMock.generateTestRun("Initial TR 1", TestingStatuses.FAILED);
        TestRun initialTestRun2 = TestRunsMock.generateTestRun("Initial TR 2", TestingStatuses.FAILED);
        initialTestRun1.setExecutionRequestId(initialErId);
        initialTestRun2.setExecutionRequestId(initialErId);
        List<TestRun> initialTestRuns = new ArrayList<>();
        initialTestRuns.add(initialTestRun1);
        initialTestRuns.add(initialTestRun2);

        UUID finalErId1 = fromString("4fc39cee-2c9f-484c-826f-41b00bbb1111");
        ExecutionRequest finalExecutionRequest1 = new ExecutionRequest();
        finalExecutionRequest1.setUuid(finalErId1);
        UUID finalErId2 = fromString("4fc39cee-2c9f-484c-826f-41b00ccc2222");
        ExecutionRequest finalExecutionRequest2 = new ExecutionRequest();
        finalExecutionRequest2.setUuid(finalErId2);
        TestRun finalTestRun1 = TestRunsMock.generateTestRun("Final TR 1", TestingStatuses.PASSED);
        TestRun finalTestRun2 = TestRunsMock.generateTestRun("Final TR 2", TestingStatuses.PASSED);
        finalTestRun1.setInitialTestRunId(initialTestRun1.getUuid());
        finalTestRun2.setInitialTestRunId(initialTestRun2.getUuid());
        finalTestRun1.setFinalTestRun(true);
        finalTestRun2.setFinalTestRun(true);
        finalTestRun1.setExecutionRequestId(finalErId1);
        finalTestRun2.setExecutionRequestId(finalErId2);
        List<TestRun> finalTestRunsList = new ArrayList<>();
        finalTestRunsList.add(finalTestRun1);
        finalTestRunsList.add(finalTestRun2);
        Map<UUID, TestRun> finalTestRunsMap = new HashMap<>();
        finalTestRunsMap.put(finalTestRun1.getUuid(), finalTestRun1);
        finalTestRunsMap.put(finalTestRun2.getUuid(), finalTestRun2);

        List<TestRun> startReportTestRuns = new ArrayList<>();
        startReportTestRuns.add(finalTestRun1);

        Map<UUID, RerunDetails> rerunDetailsMapping = new HashMap<>();

        when(executionRequestRepository.findAllByInitialExecutionRequestId(any())).thenReturn(asList(finalExecutionRequest1, finalExecutionRequest2));
        when(executionRequestRepository.findByUuid(any())).thenReturn(initialExecutionRequest);
        when(testRunService.findAllByExecutionRequestId(initialErId)).thenReturn(initialTestRuns);
        when(testRunService.findAllByExecutionRequestId(finalErId1)).thenReturn(finalTestRunsList);

        reportService.getSummaryTestRunsAndSetRerunMapping(initialErId, startReportTestRuns, rerunDetailsMapping);
        Assertions.assertEquals(TestingStatuses.PASSED, rerunDetailsMapping.get(finalTestRun1.getUuid()).getFinalStatus());
        Assertions.assertEquals(TestingStatuses.PASSED, rerunDetailsMapping.get(finalTestRun2.getUuid()).getFinalStatus());
    }

    @Test
    public void getServerSummaryForExecutionRequest_FoundEnvInfoForEr_ShouldReturnValidServerSummary() {
        final UUID erId = randomUUID();
        when(environmentsInfoService.findQaTaSystemsByExecutionRequestId(erId))
                .thenReturn(EnvironmentsInfoMock.generateEnvInfo());

        ServerSummaryResponse serverSummaryResponse1 = new ServerSummaryResponse();
        serverSummaryResponse1.setServer("qa1.some-domain.com");
        serverSummaryResponse1.setBuild(singletonList("qa1Build"));

        ServerSummaryResponse serverSummaryResponse2 = new ServerSummaryResponse();
        serverSummaryResponse2.setServer("qa1.dev.some-domain.com");
        serverSummaryResponse2.setBuild(singletonList("qa1Build"));

        ServerSummaryResponse serverSummaryResponse3 = new ServerSummaryResponse();
        serverSummaryResponse3.setServer("qa2.some-domain.com");
        serverSummaryResponse3.setBuild(singletonList("qa2Build"));

        ServerSummaryResponse serverSummaryResponse4 = new ServerSummaryResponse();
        serverSummaryResponse4.setServer("qa2.dev.some-domain.com");
        serverSummaryResponse4.setBuild(singletonList("qa2Build"));

        ServerSummaryResponse serverSummaryResponse5 = new ServerSummaryResponse();
        serverSummaryResponse5.setServer("ta1.some-domain.com");
        serverSummaryResponse5.setBuild(singletonList("ta1Build"));

        ServerSummaryResponse serverSummaryResponse6 = new ServerSummaryResponse();
        serverSummaryResponse6.setServer("ta1.dev.some-domain.com");
        serverSummaryResponse6.setBuild(singletonList("ta1Build"));

        ServerSummaryResponse serverSummaryResponse7 = new ServerSummaryResponse();
        serverSummaryResponse7.setServer("ta2.some-domain.com");
        serverSummaryResponse7.setBuild(singletonList("ta2Build"));

        ServerSummaryResponse serverSummaryResponse8 = new ServerSummaryResponse();
        serverSummaryResponse8.setServer("ta2.dev.some-domain.com");
        serverSummaryResponse8.setBuild(singletonList("ta2Build"));

        List<ServerSummaryResponse> expServerSummary = asList(
                serverSummaryResponse1, serverSummaryResponse2,
                serverSummaryResponse3, serverSummaryResponse4,
                serverSummaryResponse5, serverSummaryResponse6,
                serverSummaryResponse7, serverSummaryResponse8);

        List<ServerSummaryResponse> actServerSummary = reportService.getServerSummaryForExecutionRequest(erId);

        Assertions.assertEquals(expServerSummary, actServerSummary, "Generated server summary should be valid");

    }

    @Test
    public void getRootCausesStatisticForExecutionRequestAndPrevious_FoundEr_ShouldReturnRootCauseStatistics() {
        UUID idEr1 = randomUUID();
        UUID idEr2 = randomUUID();

        RootCause notAnalyzedRootCause = new RootCause(randomUUID(), RootCauseType.CUSTOM, "Not Analyzed");
        RootCause defaultIssueRootCause = new RootCause(randomUUID(), RootCauseType.GLOBAL, "Default issue");
        RootCause atIssueRootCause = new RootCause(randomUUID(), RootCauseType.CUSTOM, "AT issue");

        List<RootCause> rootCauses = asList(notAnalyzedRootCause, defaultIssueRootCause, atIssueRootCause);

        when(rootCauseService.getAllRootCauses()).thenReturn(rootCauses);

        ExecutionRequest currentER = ExecutionRequestsMock.generateExecutionRequestWithPrevId(idEr1, idEr2);
        ExecutionRequest prevER = ExecutionRequestsMock.generateExecutionRequestWithPrevId(idEr2, null);

        UUID currentErId = currentER.getUuid();
        List<TestRun> currentErTestRuns =
                TestRunsMock.generateTestRunsWithRootCause(notAnalyzedRootCause, currentErId, 2);
        currentErTestRuns.addAll(TestRunsMock.generateTestRunsWithRootCause(defaultIssueRootCause, currentErId, 3));
        currentErTestRuns.addAll(TestRunsMock.generateTestRunsWithRootCause(atIssueRootCause, currentErId, 1));

        UUID prevErId = prevER.getUuid();
        List<TestRun> prevErTestRuns = TestRunsMock.generateTestRunsWithRootCause(notAnalyzedRootCause, prevErId, 1);

        when(testRunRepository.findAllTestRunRootCausesByExecutionRequestId(currentErId)).thenReturn(currentErTestRuns);
        when(testRunRepository.findAllTestRunRootCausesByExecutionRequestId(prevErId)).thenReturn(prevErTestRuns);

        when(executionRequestRepository.findNameStartDatePreviousErIdByUuid(idEr1)).thenReturn(currentER);
        when(executionRequestRepository.findNameStartDatePreviousErIdByUuid(idEr2)).thenReturn(prevER);

        when(testRunRepository.countAllByExecutionRequestId(idEr1)).thenReturn(Long.valueOf(currentErTestRuns.size()));
        when(testRunRepository.countAllByExecutionRequestId(idEr2)).thenReturn(Long.valueOf(prevErTestRuns.size()));

        RootCausesStatisticResponse rootCausesStatisticResponse1 = new RootCausesStatisticResponse();
        rootCausesStatisticResponse1.setExecutionRequestName(prevER.getName());
        rootCausesStatisticResponse1.setStartDate(prevER.getStartDate());
        rootCausesStatisticResponse1.setRootCausesGroups(singletonList(
                new RootCausesStatisticResponse.RootCausesGroup("Not Analyzed", 1, 100)));

        RootCausesStatisticResponse rootCausesStatisticResponse2 = new RootCausesStatisticResponse();
        rootCausesStatisticResponse2.setExecutionRequestName(currentER.getName());
        rootCausesStatisticResponse2.setStartDate(currentER.getStartDate());
        rootCausesStatisticResponse2.setRootCausesGroups(asList(
                new RootCausesStatisticResponse.RootCausesGroup("Not Analyzed", 2, 33),
                new RootCausesStatisticResponse.RootCausesGroup("Default issue", 3, 50),
                new RootCausesStatisticResponse.RootCausesGroup("AT issue", 1, 17)
        ));

        List<RootCausesStatisticResponse> expResult = new ArrayList<>();
        expResult.add(rootCausesStatisticResponse2);
        expResult.add(rootCausesStatisticResponse1);

        List<RootCausesStatisticResponse> result =
                reportService.getRootCausesStatisticForExecutionRequestAndPrevious(idEr1);

        Assertions.assertEquals(expResult, result, "Root cause statistic should be valid");
    }

    /*
     * LT
     * |___E2E
     * |    |___CMM
     * |    |    |---TR CMM 1
     * |    |    |---TR CMM 2
     * |    |
     * |    |---TR E2E 1
     * |    |---TR E2E 2
     * |
     * |___Integration
     * |
     * |___TR Unknown 1
     * |___TR Unknown 2
     */
    @Test
    @SuppressWarnings("unchecked")
    public void getTestCasesForExecutionRequest_LabelTemplateIsExist_ShouldReturnTreeNodes() {
        TestRun testUnknown1TR = TestRunsMock.generateTestRun("TR Unknown 1", TestingStatuses.FAILED);
        TestRun testUnknown2TR = TestRunsMock.generateTestRun("TR Unknown 2", TestingStatuses.FAILED);
        TestRun testCmm1TR = TestRunsMock.generateTestRun("TR CMM 1", TestingStatuses.FAILED);
        TestRun testCmm2TR = TestRunsMock.generateTestRun("TR CMM 2", TestingStatuses.WARNING);
        TestRun testE2E1TR = TestRunsMock.generateTestRun("TR E2E 1", TestingStatuses.PASSED);
        TestRun testE2E2TR = TestRunsMock.generateTestRun("TR E2E 2", TestingStatuses.WARNING);

        List<TestRun> testRuns = asList(testUnknown1TR, testUnknown2TR, testCmm1TR, testCmm2TR, testE2E1TR, testE2E2TR);

        LabelTemplate labelTemplate = new LabelTemplate();

        LabelTemplateNode unknownNode = LabelReportNodeMock
                .generateNode("Unknown", newHashSet(testUnknown1TR.getUuid(), testUnknown2TR.getUuid()), emptyList());
        LabelTemplateNode cmmNode = LabelReportNodeMock
                .generateNode("CMM", newHashSet(testCmm1TR.getUuid(), testCmm2TR.getUuid()), emptyList());
        LabelTemplateNode e2eNode = LabelReportNodeMock
                .generateNode("E2E", newHashSet(testE2E1TR.getUuid(), testE2E2TR.getUuid()), singletonList(cmmNode));
        LabelTemplateNode integrationNode =
                LabelReportNodeMock.generateNode("Integration", singleton(randomUUID()), emptyList());

        labelTemplate.setUnknownNode(unknownNode);
        labelTemplate.setLabelNodes(asList(e2eNode, integrationNode));

        ExecutionRequest executionRequest = ExecutionRequestsMock.generateRequest();
        executionRequest.setLabelTemplateId(randomUUID());

        when(executionRequestRepository.findByUuid(any())).thenReturn(executionRequest);
        when(labelTemplateNodeService.populateLabelTemplateWithTestRuns(any(), any(UUID.class)))
                .thenReturn(labelTemplate);
        when(labelTemplateNodeService.populateLabelTemplateWithTestRuns(any(), any(LabelTemplate.class)))
                .thenReturn(labelTemplate);
        when(testRunRepository.findAllByExecutionRequestId(any())).thenReturn(testRuns);
        when(labelTemplateNodeService.getLabelTemplate(any())).thenReturn(labelTemplate);
        when(widgetConfigService.defineLabelTemplateId(any(ExecutionRequest.class), any(UUID.class)))
                .thenReturn(UUID.randomUUID());
        when(widgetConfigService.getWidgetConfigTemplateForEr(any(ExecutionRequest.class))).thenReturn(new ExecutionRequestWidgetConfigTemplateResponse());

        LabelNodeReportResponse result = reportService.getTestCasesForExecutionRequest(executionRequest.getUuid(), null,
                        null, false, new TestCaseWidgetReportRequest());

        Assertions.assertNotNull(result);

        TreeAssertWalker<LabelNodeReportResponse> walker =
                new TreeAssertWalker<>(result, LabelNodeReportResponse::getChildren);

        walker.setAssert(node -> isEmpty(node.getLabelName()),
                getNodeAssert(singletonList("E2E"), asList(testUnknown1TR.getUuid(), testUnknown2TR.getUuid())));

        walker.setAssert(node -> !isEmpty(node.getLabelName()) && node.getLabelName().equals("E2E"),
                getNodeAssert(singletonList("CMM"), asList(testE2E1TR.getUuid(), testE2E2TR.getUuid())));

        walker.setAssert(node -> !isEmpty(node.getLabelName()) && node.getLabelName().equals("CMM"),
                getNodeAssert(emptyList(), asList(testCmm1TR.getUuid(), testCmm2TR.getUuid())));

        walker.setAssert(node -> !isEmpty(node.getLabelName()) && node.getLabelName().equals("Integration"),
                getNodeAssert(emptyList(), emptyList()));

        walker.assertAll();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getTestCasesForExecutionRequest_LabelTemplateIsExistAndTestRunsIsStopped_ShouldReturnTreeNodes() {
        TestRun testUnknown1TR = TestRunsMock.generateTestRun("TR Unknown 1", TestingStatuses.FAILED);
        TestRun testUnknown2TR = TestRunsMock.generateTestRun("TR Unknown 2", TestingStatuses.FAILED);
        TestRun testCmm1TR = TestRunsMock.generateTestRun("TR CMM 1", TestingStatuses.STOPPED);
        TestRun testCmm2TR = TestRunsMock.generateTestRun("TR CMM 2", TestingStatuses.STOPPED);
        TestRun testE2E1TR = TestRunsMock.generateTestRun("TR E2E 1", TestingStatuses.STOPPED);
        TestRun testE2E2TR = TestRunsMock.generateTestRun("TR E2E 2", TestingStatuses.STOPPED);

        List<TestRun> testRuns = asList(testUnknown1TR, testUnknown2TR, testCmm1TR, testCmm2TR, testE2E1TR, testE2E2TR);

        LabelTemplate labelTemplate = new LabelTemplate();

        LabelTemplateNode unknownNode = LabelReportNodeMock
                .generateNode("Unknown", newHashSet(testUnknown1TR.getUuid(), testUnknown2TR.getUuid()), emptyList());
        LabelTemplateNode cmmNode = LabelReportNodeMock
                .generateNode("CMM", newHashSet(testCmm1TR.getUuid(), testCmm2TR.getUuid()), emptyList());
        LabelTemplateNode e2eNode = LabelReportNodeMock
                .generateNode("E2E", newHashSet(testE2E1TR.getUuid(), testE2E2TR.getUuid()), singletonList(cmmNode));
        LabelTemplateNode integrationNode =
                LabelReportNodeMock.generateNode("Integration", singleton(randomUUID()), emptyList());

        labelTemplate.setUnknownNode(unknownNode);
        labelTemplate.setLabelNodes(asList(e2eNode, integrationNode));

        ExecutionRequest executionRequest = ExecutionRequestsMock.generateRequest();
        executionRequest.setLabelTemplateId(randomUUID());

        when(executionRequestRepository.findByUuid(any())).thenReturn(executionRequest);
        when(labelTemplateNodeService.populateLabelTemplateWithTestRuns(any(), any(UUID.class))).thenReturn(labelTemplate);
        when(labelTemplateNodeService.populateLabelTemplateWithTestRuns(any(), any(LabelTemplate.class))).thenReturn(labelTemplate);
        when(testRunRepository.findAllByExecutionRequestId(any())).thenReturn(testRuns);
        when(labelTemplateNodeService.getLabelTemplate(any())).thenReturn(labelTemplate);
        when(widgetConfigService.defineLabelTemplateId(any(ExecutionRequest.class), any(UUID.class))).thenReturn(UUID.randomUUID());
        when(widgetConfigService.getWidgetConfigTemplateForEr(any(ExecutionRequest.class))).thenReturn(new ExecutionRequestWidgetConfigTemplateResponse());

        LabelNodeReportResponse result = reportService.getTestCasesForExecutionRequest(executionRequest.getUuid(), null,
                        null, false, new TestCaseWidgetReportRequest());

        Assertions.assertNotNull(result);

        Assertions.assertEquals(result.getChildren().get(0).getStatus(), TestingStatuses.FAILED,
                "Testing status testruns with common label is failed");
        Assertions.assertEquals(result.getChildren().get(0).getTestRuns().get(0).getTestingStatus(), TestingStatuses.STOPPED,
                "First test run have status stopped");
        Assertions.assertEquals(result.getChildren().get(0).getTestRuns().get(1).getTestingStatus(), TestingStatuses.STOPPED,
                "Second test run have status stopped");
    }

    private BiConsumer<LabelNodeReportResponse, LabelNodeReportResponse> getNodeAssert(
            List<String> childrenNodesExpected,
            List<UUID> childrenTestRunsExpected) {
        return (rootNode, childNode) -> {
            assertNodeLists(childNode, "children nodes", childNode.getChildren(),
                    childrenNodesExpected, LabelNodeReportResponse::getLabelName);

            assertNodeLists(childNode, "test runs", childNode.getTestRuns(),
                    childrenTestRunsExpected, TestRunNodeResponse::getUuid);
        };
    }

    private <T, R> void assertNodeLists(LabelNodeReportResponse node,
                                        String expectedEntityName,
                                        List<T> actualEntitiesParams,
                                        List<R> expectedEntitiesParams,
                                        Function<T, R> actualEntitiesParamsExtractor) {
        final String nodeName = node.getLabelName();
        if (!isEmpty(expectedEntitiesParams)) {
            Assertions.assertFalse(isEmpty(actualEntitiesParams), format("%s of '%s' node are empty", expectedEntityName, nodeName));
            Assertions.assertEquals(expectedEntitiesParams.size(), actualEntitiesParams.size(),
                    format("Size of '%s' node %s is invalid", nodeName, expectedEntityName));

            Set<R> actualExtractedEntitiesParams =
                    StreamUtils.extractFields(actualEntitiesParams, actualEntitiesParamsExtractor);
            Assertions.assertEquals(new HashSet<>(expectedEntitiesParams), actualExtractedEntitiesParams,
                    format("%s of '%s' node are not equal", expectedEntityName, nodeName));
        } else {
            Assertions.assertTrue(isEmpty(actualEntitiesParams), format("%s of '%s' node should be absent", expectedEntityName, nodeName));
        }
    }

    @Test

    public void getTestCasesForExecutionRequest_LabelTemplateIsNotExist_ShouldReturnTreeWithTestRuns() {
        TestRun testRun1 = TestRunsMock.generateTestRun("TR 1", TestingStatuses.PASSED);
        TestRun testRun2 = TestRunsMock.generateTestRun("TR 2", TestingStatuses.FAILED);
        DataSetListFeignClient http = Mockito.mock(DataSetListFeignClient.class);

        List<TestRun> testRuns = asList(testRun1, testRun2);

        when(http.getDataSetsWithNameAndDataSetList(anyList()))
                .thenReturn(new ResponseEntity<>(Collections.singletonList(new DatasetResponseDto()), HttpStatus.OK));
        when(labelTemplateNodeService.populateLabelTemplateWithTestRuns(any(), any(UUID.class)))
                .thenThrow(new IllegalStateException());
        when(executionRequestRepository.findByUuid(any()))
                .thenReturn(ExecutionRequestsMock.generateRequest());
        when(testRunRepository.findAllByExecutionRequestId(any())).thenReturn(testRuns);
        when(http.getDataSetsWithNameAndDataSetList(anyList())).thenReturn(new ResponseEntity<>(Collections.singletonList(new DatasetResponseDto()), HttpStatus.OK));
        when(widgetConfigService.getWidgetConfigTemplateForEr(any(ExecutionRequest.class))).thenReturn(new ExecutionRequestWidgetConfigTemplateResponse());

        LabelNodeReportResponse result = reportService.getTestCasesForExecutionRequest(null, null,
                null, false, new TestCaseWidgetReportRequest());

        Assertions.assertNotNull(result);

        TreeAssertWalker<LabelNodeReportResponse> walker =
                new TreeAssertWalker<>(result, LabelNodeReportResponse::getChildren);

        walker.setAssert(node -> isEmpty(node.getLabelName()),
                getNodeAssert(emptyList(), asList(testRun1.getUuid(), testRun2.getUuid())));

        walker.assertAll();
    }

    @Test
    public void getExecutionSummary_ShouldReturnValidExecutionSummary() {
        UUID erId = randomUUID();
        String environmentName = "EnvironmentTestName";
        ExecutionRequest executionRequest = ExecutionRequestsMock.generateRequest();
        List<TestRun> testRuns = TestRunsMock.generateSetCountTestRuns(2,
                3, 13, 1, 4, 1, 1, 4);
        ExecutionSummaryResponse expRes = generateExpectedExecutionSummaryResponse(executionRequest, 24, 2, 3,
                13, 4, 1, 1, 8.3f, 12.5f, 54.2f,
                16.7f, 4.2f, 4, 23.5f, 1, environmentName);

        when(executionRequestRepository.findByUuid(erId)).thenReturn(executionRequest);
        when(environmentsService.getEnvironmentNameById(executionRequest.getEnvironmentId()))
                .thenReturn(environmentName);
        when(testRunService.findTestRunForExecutionSummaryByExecutionRequestId(any())).thenReturn(testRuns);

        ExecutionSummaryResponse actRes = reportService.getExecutionSummary(erId, false);

        Assertions.assertEquals(expRes, actRes, "Execution summary should be valid");
    }

    @Test
    public void getExecutionSummary_WithFlagIsExecutionRequestsSummary_ShouldReturnValidExecutionSummary() {
        UUID erId = randomUUID();
        String environmentName = "EnvironmentTestName";
        ExecutionRequest executionRequest = ExecutionRequestsMock.generateRequest();

        ExecutionSummaryResponse expRes = generateExpectedExecutionSummaryResponse(executionRequest, 5, 4, 0, 1,
                0, 0, 0, 80.0f, 0.0f, 20.0f, 0.0f,
                0.0f, 0, 0.0f, 0, environmentName);

        List<TestRun> testRuns = TestRunsMock.generateInitialTestRuns(
                3, 0, 0, 0, 0, 0, 0,0);
        List<TestRun> initialTestRuns = TestRunsMock.generateInitialTestRunsAndFinalFailedTestRun(
                4, 0, 1, 0, 0, 0, 0,0);

        when(executionRequestRepository.findByUuid(erId)).thenReturn(executionRequest);
        when(environmentsService.getEnvironmentNameById(executionRequest.getEnvironmentId()))
                .thenReturn(environmentName);
        when(testRunService.findTestRunForExecutionSummaryByExecutionRequestId(any())).thenReturn(testRuns);
        when(testRunService.findAllByExecutionRequestId(any())).thenReturn(initialTestRuns);

        ExecutionSummaryResponse actRes = reportService.getExecutionSummary(erId, true);
        Assertions.assertEquals(expRes, actRes, "Execution summary should be valid");
    }


    @Test
    public void getExecutionSummary_WithFlagIsExecutionRequestsSummaryAndFinalFailedTestRunInInitialTestRun_ShouldReturnValidExecutionSummary() {
        UUID erId = randomUUID();
        String environmentName = "EnvironmentTestName";
        ExecutionRequest executionRequest = ExecutionRequestsMock.generateRequest();

        ExecutionSummaryResponse expRes = generateExpectedExecutionSummaryResponse(executionRequest, 4, 4, 0, 0,
                0, 0, 0, 100.0f, 0.0f, 0.0f, 0.0f, 0.0f,
                0, 0.0f, 0, environmentName);
        List<TestRun> testRuns = TestRunsMock.generateInitialTestRuns(
                3, 0, 0, 0, 0, 0, 0,0);
        List<TestRun> initialTestRuns = TestRunsMock.generateInitialTestRuns(
                4, 0, 0, 0, 0, 0, 0,0);

        when(executionRequestRepository.findByUuid(erId)).thenReturn(executionRequest);
        when(environmentsService.getEnvironmentNameById(executionRequest.getEnvironmentId()))
                .thenReturn(environmentName);
        when(testRunService.findTestRunForExecutionSummaryByExecutionRequestId(any())).thenReturn(testRuns);
        when(testRunService.findAllByExecutionRequestId(any())).thenReturn(initialTestRuns);

        ExecutionSummaryResponse actRes = reportService.getExecutionSummary(erId, true);
        Assertions.assertEquals(expRes, actRes, "Execution summary should be valid");
    }

    @Test
    public void getExecutionSummary_WithFlagIsExecutionRequestsSummaryAndFinalFailedTestRunInMatchTestRuns_ShouldReturnValidExecutionSummary() {
        UUID erId = randomUUID();
        String environmentName = "EnvironmentTestName";
        ExecutionRequest executionRequest = ExecutionRequestsMock.generateRequest();

        ExecutionSummaryResponse expRes = generateExpectedExecutionSummaryResponse(executionRequest, 2, 1, 0, 1,
                0, 0, 0, 50.0f, 0.0f, 50.0f, 0.0f, 0.0f,
                0, 0.0f, 0, environmentName);
        List<UUID> uuidList = new ArrayList<>();
        uuidList.add(UUID.fromString("395c193a-0fe2-472f-a646-02c2f87dc001"));
        uuidList.add(UUID.fromString("395c193a-0fe2-472f-a646-02c2f87dc002"));

        List<TestRun> testRuns = new ArrayList<>();
        testRuns.addAll(TestRunsMock.getTestRunsByCountAndStatusAndInitialId(TestingStatuses.PASSED, 1, uuidList.get(0)));
        TestRun testRun = new TestRun();
        testRun.setInitialTestRunId(uuidList.get(1));
        testRun.setFinalTestRun(true);
        testRun.updateTestingStatus(TestingStatuses.FAILED);
        testRuns.add(testRun);

        List<TestRun> parentTestRuns = new ArrayList<>();
        TestRun firstParentTestRun = new TestRun();
        firstParentTestRun.setUuid(uuidList.get(0));
        firstParentTestRun.setTestingStatus(TestingStatuses.PASSED);
        parentTestRuns.add(firstParentTestRun);
        TestRun secondTestRun = new TestRun();
        secondTestRun.setUuid(uuidList.get(1));
        secondTestRun.updateTestingStatus(TestingStatuses.PASSED);
        parentTestRuns.add(secondTestRun);

        when(executionRequestRepository.findByUuid(erId)).thenReturn(executionRequest);
        when(environmentsService.getEnvironmentNameById(executionRequest.getEnvironmentId()))
                .thenReturn(environmentName);
        when(testRunService.findTestRunForExecutionSummaryByExecutionRequestId(any())).thenReturn(testRuns);
        when(testRunService.findAllByExecutionRequestId(any())).thenReturn(parentTestRuns);

        ExecutionSummaryResponse actRes = reportService.getExecutionSummary(erId, true);
        Assertions.assertEquals(expRes, actRes, "Execution summary should be valid");
    }


    @Test
    public void testRemoveEmptyNodes_shouldBeSuccessfullyRemoved() {
        List<TestRunNodeResponse> fakeTestRunNodes = asList(
                new TestRunNodeResponse("TR 1"),
                new TestRunNodeResponse("TR 2")
        );

        LabelNodeReportResponse erNode = new LabelNodeReportResponse("ER");
        LabelNodeReportResponse prerequisites = new LabelNodeReportResponse("Prerequisites");
        LabelNodeReportResponse execution = new LabelNodeReportResponse("Execution");
        LabelNodeReportResponse news = new LabelNodeReportResponse("News");
        LabelNodeReportResponse newsOffline = new LabelNodeReportResponse("Offline");
        LabelNodeReportResponse newsOnline = new LabelNodeReportResponse("Online");
        LabelNodeReportResponse newsOnlinePostpaid = new LabelNodeReportResponse("Postpaid");
        LabelNodeReportResponse modify = new LabelNodeReportResponse("Modify");
        LabelNodeReportResponse modifyOffline = new LabelNodeReportResponse("Offline");
        LabelNodeReportResponse modifyOnline = new LabelNodeReportResponse("Online");
        LabelNodeReportResponse modifyOnlinePostpaid = new LabelNodeReportResponse("Postpaid");
        LabelNodeReportResponse modifyOnlinePrepaid = new LabelNodeReportResponse("Prepaid");
        LabelNodeReportResponse features = new LabelNodeReportResponse("Features");
        LabelNodeReportResponse featuresOffline = new LabelNodeReportResponse("Offline");
        LabelNodeReportResponse featuresOfflinePartial = new LabelNodeReportResponse("Partial");
        LabelNodeReportResponse featuresOfflineRainy = new LabelNodeReportResponse("Rainy");
        LabelNodeReportResponse featuresOfflineRerate = new LabelNodeReportResponse("Rerate");
        LabelNodeReportResponse featuresOnline = new LabelNodeReportResponse("Online");
        LabelNodeReportResponse featuresOnlineAutoReplan = new LabelNodeReportResponse("AutoReplan");
        LabelNodeReportResponse featuresOnlineEap = new LabelNodeReportResponse("EAP");
        LabelNodeReportResponse featuresOnlineSy = new LabelNodeReportResponse("SY");
        LabelNodeReportResponse unknown = new LabelNodeReportResponse("Unknown");
        LabelNodeReportResponse validation = new LabelNodeReportResponse("Validation");

        erNode.setChildren(asList(prerequisites, execution, validation));
        execution.setChildren(asList(news, modify, features, unknown));
        news.setChildren(asList(newsOffline, newsOnline));
        newsOnline.setChildren(singletonList(newsOnlinePostpaid));
        modify.setChildren(asList(modifyOffline, modifyOnline));
        modifyOnline.setChildren(asList(modifyOnlinePostpaid, modifyOnlinePrepaid));
        features.setChildren(asList(featuresOffline, featuresOnline));
        featuresOffline.setChildren(asList(featuresOfflinePartial, featuresOfflineRainy, featuresOfflineRerate));
        featuresOnline.setChildren(asList(featuresOnlineAutoReplan, featuresOnlineEap, featuresOnlineSy));

        newsOffline.setTestRuns(fakeTestRunNodes);
        newsOnlinePostpaid.setTestRuns(fakeTestRunNodes);
        modifyOffline.setTestRuns(fakeTestRunNodes);
        modifyOnlinePostpaid.setTestRuns(fakeTestRunNodes);
        featuresOfflinePartial.setTestRuns(fakeTestRunNodes);
        featuresOfflineRainy.setTestRuns(fakeTestRunNodes);
        featuresOnlineEap.setTestRuns(fakeTestRunNodes);

        reportService.removeEmptyNodes(erNode);

        //      Simple drawer to view tree in the console
        new TreeConsoleDrawer<>(erNode, LabelNodeReportResponse::getChildren, LabelNodeReportResponse::getLabelName)
                .draw();

        List<String> expectedExcludedNodes = asList("Prepaid", "Rerate", "AutoReplan", "SY", "Unknown");

        TreeWalker<LabelNodeReportResponse> treeWalker = new TreeWalker<>();
        treeWalker.walkWithPreProcess(erNode, LabelNodeReportResponse::getChildren, (root, child) -> {
            String nodeName = child.getLabelName();
            Assertions.assertFalse(expectedExcludedNodes.contains(nodeName), nodeName + " node shouldn't exist in result tree");
        });
    }

    private ExecutionSummaryResponse generateExpectedExecutionSummaryResponse(
            ExecutionRequest executionRequest, int testCasesCount, int setPassedCount, int warningCount, int failedCount,
            int stoppedCount, int notStartedCount, int skippedCount, float passedRate, float warningRate,
            float failedRate, float stoppedRate, float notStartedRate, int blockedCount, float blockedRate,
            int inProgressCount, String environmentName) {

        ExecutionSummaryResponse expRes = modelMapper.map(executionRequest, ExecutionSummaryResponse.class);
        expRes.setTestCasesCount(testCasesCount);
        expRes.setPassedCount(setPassedCount);
        expRes.setWarningCount(warningCount);
        expRes.setFailedCount(failedCount);
        expRes.setStoppedCount(stoppedCount);
        expRes.setNotStartedCount(notStartedCount);
        expRes.setSkippedCount(skippedCount);
        expRes.setPassedRate(passedRate);
        expRes.setWarningRate(warningRate);
        expRes.setFailedRate(failedRate);
        expRes.setStoppedRate(stoppedRate);
        expRes.setNotStartedRate(notStartedRate);
        expRes.setBlockedCount(blockedCount);
        expRes.setBlockedRate(blockedRate);
        expRes.setInProgressCount(inProgressCount);
        expRes.setEnvironmentName(environmentName);
        expRes.setEnvironmentLink("https://catalogue-secure-uat.dev-atp-cloud.some-domain.com/project/"
                + executionRequest.getProjectId()
                + "/environments/environment/" + executionRequest.getEnvironmentId());
        return expRes;
    }
}
