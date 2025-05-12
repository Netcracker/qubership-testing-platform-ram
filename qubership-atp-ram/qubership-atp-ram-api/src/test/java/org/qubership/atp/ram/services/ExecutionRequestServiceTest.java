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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;
import org.qubership.atp.common.lock.LockManager;
import org.qubership.atp.ram.ExecutionRequestsMock;
import org.qubership.atp.ram.LogRecordMock;
import org.qubership.atp.ram.TestRunsMock;
import org.qubership.atp.ram.dto.request.ExecutionRequestSearchRequest;
import org.qubership.atp.ram.dto.request.SortingParams;
import org.qubership.atp.ram.dto.response.ExecutionRequestMainInfoResponse;
import org.qubership.atp.ram.entities.ComparisonExecutionRequest;
import org.qubership.atp.ram.entities.ComparisonStep;
import org.qubership.atp.ram.entities.ComparisonTestRun;
import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.Label;
import org.qubership.atp.ram.models.Project;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.models.response.ExecutionRequestResponse;
import org.qubership.atp.ram.models.response.PaginatedResponse;
import org.qubership.atp.ram.repositories.CustomExecutionRequestRepository;
import org.qubership.atp.ram.repositories.ExecutionRequestConfigRepository;
import org.qubership.atp.ram.repositories.ExecutionRequestRepository;
import org.qubership.atp.ram.services.filtering.ExecutionRequestFilteringService;
import org.qubership.atp.ram.services.sorting.ExecutionRequestSortingService;
import org.qubership.atp.ram.utils.RateCalculator;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(SpringExtension.class)
@Isolated
public class ExecutionRequestServiceTest {

    private static ExecutionRequestService service;
    private static ExecutionRequestRepository repository;
    private static CustomExecutionRequestRepository customRepository;
    private static ExecutionRequestConfigRepository configRepository;
    private static ProjectsService projectsService;
    private static LabelsService labelService;

    @BeforeAll
    public static void setUp() throws Exception {
        repository = mock(ExecutionRequestRepository.class);
        customRepository = mock(CustomExecutionRequestRepository.class);
        configRepository = mock(ExecutionRequestConfigRepository.class);
        LogRecordService lrService = mock(LogRecordService.class);
        TestRunService testRunService = mock(TestRunService.class);
        RateCalculator rateCalculator = mock(RateCalculator.class);
        projectsService = mock(ProjectsService.class);
        ExecutionRequestFilteringService executionRequestFilteringService
                = mock(ExecutionRequestFilteringService.class);
        ExecutionRequestSortingService executionRequestSortingService
                = spy(ExecutionRequestSortingService.class);
        UserService userService = mock(UserService.class);
        //        when(lrService.findAllByTestRunIdOrderByStartDateAsc(any(UUID.class), any())).thenReturn(LogRecordMock.findLogRecordsByTestRunId());
        TestCaseService testCaseService = mock(TestCaseService.class);
        WidgetConfigTemplateService widgetConfigTemplateService = mock(WidgetConfigTemplateService.class);
        ModelMapper modelMapper = new ModelMapper();
        JiraIntegrationService jiraIntegrationServiceMock = mock(JiraIntegrationService.class);
        EnvironmentsService environmentsService = mock(EnvironmentsService.class);
        final EnvironmentsInfoService environmentsInfoService = mock(EnvironmentsInfoService.class);
        OrchestratorService orchestratorService = mock(OrchestratorService.class);
        labelService = mock(LabelsService.class);
        service = new ExecutionRequestService(repository, customRepository,
                lrService, testRunService, testCaseService, rateCalculator,
                projectsService, userService, modelMapper, configRepository,
                widgetConfigTemplateService, executionRequestFilteringService,
                executionRequestSortingService, jiraIntegrationServiceMock, environmentsInfoService,
                orchestratorService, environmentsService,
                labelService, mock(LockManager.class), mock(RootCauseService.class));
        ReflectionTestUtils.setField(service, "limit", 70);
    }

    @Test
    public void testRemoveLabels() {
        ExecutionRequest executionRequest = ExecutionRequestsMock.generateRequest();
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        List<UUID> labels = Arrays.asList(uuid1, uuid2);
        executionRequest.setLabels(labels);
        when(repository.findById(any())).thenReturn(Optional.of(executionRequest));

        service.removeLabel(UUID.randomUUID(), Collections.singletonList(uuid1));
        Assertions.assertEquals(1, executionRequest.getLabels().size());
    }

    @Test
    public void updateExecutionStatus_ExecutionStatusIsFinished_passedResult() {
        ExecutionRequest executionRequest = new ExecutionRequest();
        executionRequest.setName("ER");
        UUID uuid1 = UUID.randomUUID();
        executionRequest.setUuid(uuid1);
        executionRequest.setStartDate(new Timestamp(System.currentTimeMillis()));

        when(repository.findById(any())).thenReturn(Optional.of(executionRequest));
        Assertions.assertNull(executionRequest.getFinishDate());

        service.updateExecutionStatus(uuid1, ExecutionStatuses.FINISHED);

        Assertions.assertEquals(ExecutionStatuses.FINISHED, executionRequest.getExecutionStatus());
        Assertions.assertNotNull(executionRequest.getFinishDate());
    }

    @Test
    public void updateExecutionStatus_ExecutionStatusIsTerminated_passedResult() {
        ExecutionRequest executionRequest = new ExecutionRequest();
        executionRequest.setName("ER");
        UUID uuid1 = UUID.randomUUID();
        executionRequest.setUuid(uuid1);
        executionRequest.setStartDate(new Timestamp(System.currentTimeMillis()));

        when(repository.findById(any())).thenReturn(Optional.of(executionRequest));
        Assertions.assertNull(executionRequest.getFinishDate());

        service.updateExecutionStatus(uuid1, ExecutionStatuses.TERMINATED);

        Assertions.assertEquals(ExecutionStatuses.TERMINATED, executionRequest.getExecutionStatus());
        Assertions.assertNotNull(executionRequest.getFinishDate());
    }


    @Test
    public void updateExecutionStatus_ExecutionStatusIsInProgress_passedResult() {
        ExecutionRequest executionRequest = new ExecutionRequest();
        executionRequest.setName("ER");
        UUID uuid1 = UUID.randomUUID();
        executionRequest.setUuid(uuid1);
        executionRequest.setStartDate(new Timestamp(System.currentTimeMillis()));

        when(repository.findById(any())).thenReturn(Optional.of(executionRequest));
        Assertions.assertNull(executionRequest.getFinishDate());

        service.updateExecutionStatus(uuid1, ExecutionStatuses.IN_PROGRESS);

        Assertions.assertEquals(ExecutionStatuses.IN_PROGRESS, executionRequest.getExecutionStatus());
        Assertions.assertNull(executionRequest.getFinishDate());
    }

    @Test
    public void service_FindRequestsByTestScopeUuid_ReturnRequestsList() {
        List<ExecutionRequest> expResult = ExecutionRequestsMock.generateRequestsList();
        when(service.findByTestScopeUuid(any())).thenReturn(expResult);

        List<ExecutionRequest> result = service.findByTestScopeUuid(any());

        Assertions.assertEquals(expResult, result);
    }

    @Test
    public void buildExecutionRequestResponses_WithoutLabels_ReturnListOfShortErsAndNotUseLabelService() {
        List<ExecutionRequest> ers = ExecutionRequestsMock.generateRequestsList();

        List<ExecutionRequestResponse> result = service.buildExecutionRequestResponses(ers);

        Assertions.assertEquals(result.size(), 2);
        Mockito.verify(labelService, times(0)).getLabels(any());
    }

    @Test
    public void buildExecutionRequestResponses_WithLabels_ReturnListOfShortErsAndUseLabelService() {
        List<ExecutionRequest> ers = ExecutionRequestsMock.generateRequestsList();
        Label label1 = new Label();
        label1.setUuid(UUID.randomUUID());
        Set<UUID> labels1 = new HashSet<>();
        labels1.add(label1.getUuid());
        ers.get(0).setFilteredByLabels(labels1);

        Label label2 = new Label();
        label2.setUuid(UUID.randomUUID());
        Set<UUID> labels2 = new HashSet<>();
        labels2.add(label2.getUuid());
        ers.get(1).setFilteredByLabels(labels2);
        Mockito.when(labelService.getLabels(any())).thenReturn(Arrays.asList(label1, label2));

        List<ExecutionRequestResponse> result = service.buildExecutionRequestResponses(ers);

        Assertions.assertEquals(result.size(), 2);
        Mockito.verify(labelService, times(1)).getLabels(any());
        List<UUID> labelsId = result.stream()
                .map(response -> response.getFilteredByLabels().get(0).getUuid()).collect(Collectors.toList());
        Assertions.assertTrue(labelsId.contains(label1.getUuid()));
        Assertions.assertTrue(labelsId.contains(label2.getUuid()));
    }

    @Test
    public void buildExecutionRequestResponses_WithInitialIdInRequest_ExecutionResponseCollectSuccesfully() {
        List<ExecutionRequest> ers = ExecutionRequestsMock.generateRequestsList();
        ers.get(0).setInitialExecutionRequestId(UUID.randomUUID());

        List<ExecutionRequestResponse> result = service.buildExecutionRequestResponses(ers);

        Assertions.assertEquals(result.get(0).getUuid(), ers.get(0).getUuid());
    }

    // todo fix this
    @Disabled
    @Test
    public void getComparisonExecutionRequestWithSteps_GenerateComparisonStepsTestRunsAndErs_ReturnedComparisonErEqualsExpected() {
        List<UUID> uuids = new ArrayList<>();
        uuids.add(UUID.randomUUID());
        uuids.add(UUID.randomUUID());
        uuids.add(UUID.randomUUID());
        List<ExecutionRequest> executionRequests = ExecutionRequestsMock.generateFinishedRequestsList();
        when(repository.findByUuid(any()))
                .thenReturn(executionRequests.get(0))
                .thenReturn(executionRequests.get(1))
                .thenReturn(executionRequests.get(2));

        when(service.getAllTestRuns(any()))
                .thenReturn(TestRunsMock.findByExecutionRequestId())
                .thenReturn(TestRunsMock.findByExecutionRequestIdWithOtherIds());

        List<ComparisonExecutionRequest> expCompErs = generateExpectedComparisonErs(executionRequests);
        List<ComparisonExecutionRequest> compErs = service.getComparisonExecutionRequestWithSteps(uuids);
        Assertions.assertEquals(expCompErs.get(0).getTestRuns().size(),
                compErs.get(0).getTestRuns().size(), "Comparison Test Runs for first Er have different size.");
        Assertions.assertEquals(expCompErs.get(0).getNonComparisonTestRuns().size(),
                compErs.get(0).getNonComparisonTestRuns().size(),
                "Non comparison Test Runs for first Er have different size.");
        Assertions.assertEquals(expCompErs.get(1).getTestRuns().size(), compErs.get(1).getTestRuns().size(),
                "Comparison Test Runs for second Er have different size.");
        Assertions.assertEquals(expCompErs.get(1).getNonComparisonTestRuns().size(),
                compErs.get(1).getNonComparisonTestRuns().size(),
                "Non comparison Test Runs for second Er have different size.");
    }

    private List<ComparisonExecutionRequest> generateExpectedComparisonErs(List<ExecutionRequest> ers) {
        List<TestRun> testRuns1 = TestRunsMock.findByExecutionRequestId();
        List<TestRun> testRuns2And3 = TestRunsMock.findByExecutionRequestIdWithOtherIds();

        Set<ComparisonStep> expStep = LogRecordMock.findComparisonStepByTestRunIds();

        Set<ComparisonTestRun> generalTestRuns2And3 = new HashSet<>();
        for (int i = 0; i < 4; i++) {
            generalTestRuns2And3.add(getComparisonTestRun(i, testRuns1, expStep));
        }

        Set<ComparisonTestRun> generalTestRuns1 = new HashSet<>(generalTestRuns2And3);
        generalTestRuns2And3.add(getComparisonTestRun(4, testRuns2And3, expStep));

        Set<ComparisonTestRun> nonCompTestRuns1 = new HashSet<>();
        for (int i = 4; i < 6; i++) {
            nonCompTestRuns1.add(getComparisonTestRun(i, testRuns1, expStep));
        }

        List<ComparisonExecutionRequest> compers = new ArrayList<>();
        compers.add(getComparisonRequest(ers.get(0).getName(), generalTestRuns1, nonCompTestRuns1));

        compers.add(getComparisonRequest(ers.get(1).getName(), generalTestRuns2And3, Collections.EMPTY_SET));
        compers.add(getComparisonRequest(ers.get(2).getName(), generalTestRuns2And3, Collections.EMPTY_SET));
        return compers;
    }

    private ComparisonTestRun getComparisonTestRun(int index, List<TestRun> testRuns, Set<ComparisonStep> expStep) {
        ComparisonTestRun compTr = new ComparisonTestRun();
        compTr.setSteps(expStep);
        compTr.setId(testRuns.get(index).getUuid());
        compTr.setTestCaseId(testRuns.get(index).getTestCaseId());
        compTr.setDuration(testRuns.get(index).getDuration());
        compTr.setStatuses(testRuns.get(index).getTestingStatus());
        compTr.setTrName(testRuns.get(index).getName());
        return compTr;
    }

    private ComparisonExecutionRequest getComparisonRequest(String erName,
                                                            Set<ComparisonTestRun> testRuns,
                                                            Set<ComparisonTestRun> nonCompTestRuns) {
        ComparisonExecutionRequest compEr = new ComparisonExecutionRequest();
        compEr.setErName(erName);
        compEr.setTestRuns(testRuns);
        compEr.setNonComparisonTestRuns(nonCompTestRuns);
        return compEr;
    }

    @Test
    public void service_GetExecutionRequests_ReturnPaginatedResponse(){
        final int TEST_PLAN_REQUEST_COUNT = 25;
        final int REQUEST_PAGE = 0;
        final int REQUEST_SIZE = 10;
        UUID testPlanId = UUID.randomUUID();
        SortingParams sortingParams = new SortingParams();
        sortingParams.setSortType(SortingParams.Direction.DESC);
        sortingParams.setColumn(ExecutionRequest.START_DATE_FIELD);
        ExecutionRequestSearchRequest request = new ExecutionRequestSearchRequest();
        request.setPage(REQUEST_PAGE);
        request.setSize(REQUEST_SIZE);
        request.setSort(Collections.singletonList(sortingParams));

        when(customRepository.searchExecutionRequests(any(),any()))
                .thenReturn(ExecutionRequestsMock.generateRequestsList(request.getSize()));
        when(customRepository.getExecutionRequestsCountByCriteria(any())).thenReturn(TEST_PLAN_REQUEST_COUNT);

        PaginatedResponse<ExecutionRequest> executionRequests = service.getExecutionRequests(testPlanId, request);
        Assertions.assertEquals(TEST_PLAN_REQUEST_COUNT, executionRequests.getTotal());
        Assertions.assertEquals(request.getSize(), Integer.valueOf(executionRequests.getData().size()));
    }

    @Test
    public void findByIdMainInfoTest_virtualExecutionRequestConfigured_returnExecutionRequestMainInfoResponse() {
        // given
        ExecutionRequest executionRequest = ExecutionRequestsMock.generateRequest();
        executionRequest.setVirtual(true);
        Project project = new Project();
        // when
        when(repository.findById(any())).thenReturn(Optional.of(executionRequest));
        when(projectsService.getProjectById(any())).thenReturn(project);
        ExecutionRequestMainInfoResponse actualExecutionRequestMainInfoResponse =
                service.findByIdMainInfo(UUID.randomUUID());
        // then
        Assertions.assertNotNull(actualExecutionRequestMainInfoResponse);
        Assertions.assertTrue(actualExecutionRequestMainInfoResponse.isVirtual());
    }
}
