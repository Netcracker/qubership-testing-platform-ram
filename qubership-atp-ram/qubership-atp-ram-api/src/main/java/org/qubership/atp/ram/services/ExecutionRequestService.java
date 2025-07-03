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

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.qubership.atp.auth.springbootstarter.exceptions.AtpEntityNotFoundException;
import org.qubership.atp.common.lock.LockManager;
import org.qubership.atp.ram.constants.CacheConstants;
import org.qubership.atp.ram.dto.request.ExecutionRequestConfigUpdateRequest;
import org.qubership.atp.ram.dto.request.ExecutionRequestSearchRequest;
import org.qubership.atp.ram.dto.request.LogRecordRegexSearchRequest;
import org.qubership.atp.ram.dto.request.SortingParams;
import org.qubership.atp.ram.dto.request.TestRunsByValidationLabelsRequest;
import org.qubership.atp.ram.dto.response.Environment;
import org.qubership.atp.ram.dto.response.ExecutionRequestMainInfoResponse;
import org.qubership.atp.ram.dto.response.ExecutionRequestMainInfoResponse.Executor;
import org.qubership.atp.ram.dto.response.LogRecordRegexSearchResponse;
import org.qubership.atp.ram.dto.response.TestCaseLabelResponse;
import org.qubership.atp.ram.dto.response.TestRunWithValidationLabelsResponse;
import org.qubership.atp.ram.dto.response.TestRunsByValidationLabelsResponse;
import org.qubership.atp.ram.entities.ComparisonExecutionRequest;
import org.qubership.atp.ram.entities.ComparisonStep;
import org.qubership.atp.ram.entities.ComparisonTestRun;
import org.qubership.atp.ram.enums.DefaultSuiteNames;
import org.qubership.atp.ram.enums.ExecutionRequestWidgets;
import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.exceptions.executionrequests.RamExecutionRequestIdNotFoundException;
import org.qubership.atp.ram.model.BaseSearchRequest;
import org.qubership.atp.ram.model.ExecutionRequestTestResult;
import org.qubership.atp.ram.model.TestResult;
import org.qubership.atp.ram.models.EnrichedTestRun;
import org.qubership.atp.ram.models.EnvironmentsInfo;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.ExecutionRequestConfig;
import org.qubership.atp.ram.models.ExecutionRequestRatesResponse;
import org.qubership.atp.ram.models.InitialExecutionRequest;
import org.qubership.atp.ram.models.Label;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.Project;
import org.qubership.atp.ram.models.RootCause;
import org.qubership.atp.ram.models.SystemInfo;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.models.UserInfo;
import org.qubership.atp.ram.models.WidgetConfigTemplate;
import org.qubership.atp.ram.models.response.ExecutionRequestResponse;
import org.qubership.atp.ram.models.response.PaginatedResponse;
import org.qubership.atp.ram.repositories.CustomExecutionRequestRepository;
import org.qubership.atp.ram.repositories.ExecutionRequestConfigRepository;
import org.qubership.atp.ram.repositories.ExecutionRequestRepository;
import org.qubership.atp.ram.services.filtering.ExecutionRequestFilteringService;
import org.qubership.atp.ram.services.sorting.ExecutionRequestSortingService;
import org.qubership.atp.ram.utils.ListUtils;
import org.qubership.atp.ram.utils.PathsGenerator;
import org.qubership.atp.ram.utils.RateCalculator;
import org.qubership.atp.ram.utils.RateCalculator.TestingStatusesStat;
import org.qubership.atp.ram.utils.StepPath;
import org.qubership.atp.ram.utils.StreamUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.CriteriaDefinition;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.google.common.base.Splitter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExecutionRequestService extends CrudService<ExecutionRequest> {

    private static final String DEFAULT_DATE_FILED_NAME = "date";
    private static final String START_DATE_FILED_NAME = "startDate";
    private static final String FINISH_DATE_FILED_NAME = "finishDate";
    private static final int DEFAULT_NUMBER_PAGE = 0;
    private static final String UNKNOWN = "Unknown";
    private static final String PROJECT = "project";
    private static final String RAM = "ram";
    private static final String EXECUTION_REQUEST = "execution-request";
    private static final String A_HREF_OPEN = "<a href=\"%s\">";
    private static final String A_HREF_CLOSE = "</a>";

    private final ExecutionRequestRepository repository;
    private final CustomExecutionRequestRepository customRepository;
    private final LogRecordService logRecordService;
    private final TestRunService testRunService;
    private final TestCaseService testCaseService;
    private final RateCalculator rateCalculator;
    private final ProjectsService projectsService;
    private final UserService userService;
    private final ModelMapper modelMapper;
    private final ExecutionRequestConfigRepository configRepository;
    private final WidgetConfigTemplateService widgetConfigTemplateService;
    private final ExecutionRequestFilteringService filteringService;
    private final ExecutionRequestSortingService sortingService;
    private final JiraIntegrationService jiraIntegrationService;
    private final EnvironmentsInfoService environmentsInfoService;
    private final OrchestratorService orchestratorService;
    private final EnvironmentsService environmentsService;
    private final LabelsService labelsService;
    private final LockManager lockManager;
    private final RootCauseService rootCauseService;

    @Value("${limit.testresults.catalog.dashboard}")
    private int limit;

    @Value("${atp.ram.services.executionrequestconfig.creating.lock.duration.sec:300}")
    private Integer lockDurationForCreatingConfigSec;

    @Value("${catalogue.url}")
    private String catalogueUrl;

    @Override
    protected MongoRepository<ExecutionRequest, UUID> repository() {
        return repository;
    }

    public List<ExecutionRequest> getAllSortedExecutionRequests() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, START_DATE_FILED_NAME));
    }

    /**
     * Creates property maps for proper execution request mapping.
     */
    @PostConstruct
    public void init() {
        PropertyMap<ExecutionRequest, ExecutionRequestResponse> executionRequestResponsePropertyMap;
        executionRequestResponsePropertyMap = new PropertyMap<ExecutionRequest, ExecutionRequestResponse>() {
            protected void configure() {
                map(source.getUuid()).setUuid(null);
            }
        };
        modelMapper.addMappings(executionRequestResponsePropertyMap);
    }

    /**
     * Returns list of ERs by list uuid for compare.
     *
     * @param uuids List uuid Execution Requests.
     * @return Set comparison Execution Request.
     */
    public List<ComparisonExecutionRequest> getComparisonExecutionRequest(List<UUID> uuids) {
        List<ExecutionRequest> executionRequests = new ArrayList<>();
        for (UUID uuid : uuids) {
            ExecutionRequest er = findById(uuid);
            executionRequests.add(er);
        }
        return getComparisonEr(executionRequests);
    }

    /**
     * Returns list of ERs with steps by list uuid for compare.
     *
     * @param uuids List uuid Execution Requests.
     * @return Set comparison Execution Request.
     */
    public List<ComparisonExecutionRequest> getComparisonExecutionRequestWithSteps(List<UUID> uuids) {
        List<ExecutionRequest> executionRequests = new ArrayList<>();
        for (UUID uuid : uuids) {
            ExecutionRequest er = findById(uuid);
            executionRequests.add(er);
        }
        return getComparisonErWithSteps(executionRequests);
    }

    public List<TestRun> getAllTestRuns(UUID executionRequestId) {
        return testRunService.findAllByExecutionRequestId(executionRequestId);
    }

    /**
     * Get all Test Run ids by Execution Request id.
     *
     * @param executionRequestId Execution Request id.
     * @return List of Test Run id's.
     */
    public List<UUID> getAllTestRunUuidsByExecutionRequestId(UUID executionRequestId) {
        return testRunService.findTestRunsUuidByExecutionRequestId(executionRequestId);
    }

    public List<EnrichedTestRun> getAllEnrichedTestRuns(UUID executionRequestId) {
        return testRunService.getEnrichedTestRunsByExecutionRequestId(executionRequestId);
    }

    public Map<UUID, TestRun> getDataForTestResultByExecutionRequests(List<ExecutionRequest> ers) {
        List<UUID> erIds = ers.stream().map(ExecutionRequest::getUuid).collect(Collectors.toList());
        return testRunService.getDataForTestResultByExecutionRequestId(erIds);
    }

    public List<TestRun> getAllFailedTestRuns(UUID executionRequestId) {
        return testRunService.findNotPassedTestRunByErId(executionRequestId);
    }

    public void delete(UUID uuid) {
        repository.deleteByUuid(uuid);
    }

    /**
     * Find execution request by id.
     *
     * @param executionRequestId execution request identifier
     * @return execution request
     */
    public ExecutionRequest findById(UUID executionRequestId) {
        return get(executionRequestId);
    }

    /**
     * Find {@link ExecutionRequest} main info by specified identifier.
     *
     * @param id identifier
     * @return info response
     */
    public ExecutionRequestMainInfoResponse findByIdMainInfo(UUID id) {
        ExecutionRequest executionRequest = get(id);
        UUID projectId = executionRequest.getProjectId();
        Project executionRequestProject = projectsService.getProjectById(projectId);
        UUID executorId = executionRequest.getExecutorId();
        Executor executor = new Executor(executorId, executionRequest.getExecutorName());
        List<TestRun> testRuns = testRunService.findTestRunForExecutionSummaryByExecutionRequestId(id);
        rateCalculator.calculateErRates(executionRequest, testRuns);
        float passedRate = executionRequest.getPassedRate();
        int countOfTestRuns = executionRequest.getCountOfTestRuns();
        int passCount = Math.round((float) (passedRate * countOfTestRuns) / 100);
        UUID taToolId = executionRequest.getTaToolsGroupId();
        if (taToolId == null) { // to support old orders
            taToolId = getTaToolIdFromEnvironmentsInfo(id);
        }
        boolean isProcessAlive = isProcessForExecutionRequestAlive(id);
        return new ExecutionRequestMainInfoResponse(
                id,
                executionRequest.getName(),
                executor,
                executionRequest.getExecutionStatus(),
                Math.round(passedRate),
                passCount,
                executionRequest.getStartDate(),
                executionRequest.getFinishDate(),
                executionRequest.getDuration(),
                countOfTestRuns,
                executionRequestProject.getTroubleShooterUrl(),
                executionRequestProject.getMonitoringToolUrl(),
                executionRequestProject.getMissionControlToolUrl(),
                !isProcessAlive,
                executionRequest.getThreads(),
                executionRequest.getTestScopeId(),
                executionRequest.getEnvironmentId(),
                taToolId,
                executionRequest.getInitialExecutionRequestId(),
                executionRequest.getJointExecutionKey(),
                executionRequest.isVirtual()
        );
    }

    private UUID getTaToolIdFromEnvironmentsInfo(UUID id) {
        try {
            EnvironmentsInfo environmentsInfo = environmentsInfoService.findByExecutionRequestId(id);
            if (environmentsInfo != null) {
                return environmentsInfo.getTaToolsGroupId();
            }
        } catch (AtpEntityNotFoundException e) {
            log.error("Cannot find environment info for er with id {}", id, e);
        }
        return null;
    }

    private boolean isProcessForExecutionRequestAlive(UUID id) {
        UUID processId = null;
        try {
            processId = orchestratorService.getProcessIdByExecutionRequestId(id);
        } catch (Exception e) {
            log.error("Cannot get process id from orchestrator by Execution Request id", e);
        }
        return processId != null;
    }

    /**
     * Get ER executor info by identifier.
     *
     * @param id executor identifier
     * @return executor info
     */
    public Executor getExecutionRequestExecutor(UUID id) {
        if (isNull(id)) {
            return new Executor(null, UNKNOWN);
        }
        UserInfo executorUserInfo = userService.getUserInfoById(id);
        return mapUserInfoToExecutor(executorUserInfo);
    }

    private Executor mapUserInfoToExecutor(UserInfo userInfo) {
        if (isNull(userInfo)) {
            return new Executor(null, UNKNOWN);
        }
        final UUID id = userInfo.getId();
        final String username = userInfo.getUsername();
        if (username.contains("-plugin")) {
            return new Executor(id, "Jenkins");
        } else {
            String fullUsername = userInfo.getFirstName() + " " + userInfo.getLastName();
            return new Executor(id, fullUsername);
        }
    }

    /**
     * Find ExecutionRequests with In Progress status.
     *
     * @param executionRequestsIds set of ER ids.
     * @return ids of ERs with In Progress status.
     */
    public List<UUID> getRequestsForStoppingOrTerminating(List<UUID> executionRequestsIds) {
        return executionRequestsIds.stream().filter(executionRequestsId ->
                ExecutionStatuses.IN_PROGRESS.equals(findById(executionRequestsId).getExecutionStatus())
        ).collect(Collectors.toList());
    }

    /**
     * Find ExecutionRequests with Suspended status.
     *
     * @param executionRequestsIds set of ER ids.
     * @return ids of ERs with Suspended status.
     */
    public List<UUID> getRequestsForResuming(List<UUID> executionRequestsIds) {
        return executionRequestsIds.stream().filter(executionRequestsId ->
                ExecutionStatuses.SUSPENDED.equals(findById(executionRequestsId).getExecutionStatus())
        ).collect(Collectors.toList());
    }

    /**
     * Find ExecutionRequests with Suspended status.
     *
     * @param executionRequestsIds set of ER ids.
     * @return ids of ERs with Suspended status.
     */
    public List<ExecutionRequest> getExecutionRequestsByIds(Collection<UUID> executionRequestsIds) {
        return repository.findAllByUuidIn(executionRequestsIds);
    }

    /**
     * Find ordered ExecutionRequests by ids.
     *
     * @param executionRequestsIds set of ER ids.
     * @return ERs.
     */
    public List<ExecutionRequest> getOrderedExecutionRequestsByIds(Collection<UUID> executionRequestsIds) {
        return repository.findAllByUuidInOrderByStartDate(executionRequestsIds);
    }

    /**
     * Set label to execution request and return updated object.
     *
     * @param uuid       of execution request for update
     * @param uuidLabels uuid of labels for remove
     * @return updated object {@link ExecutionRequest}
     */
    public ExecutionRequest setLabel(UUID uuid, List<UUID> uuidLabels) {
        ExecutionRequest executionRequest = findById(uuid);
        executionRequest.setLabels(uuidLabels);
        save(executionRequest);
        return executionRequest;
    }

    /**
     * Find and remove label from execution request and return updated execution request.
     *
     * @param uuidRequest uuid of execution request for update
     * @param uuidLabels  uuid of labels for remove
     * @return updated object {@link ExecutionRequest}
     */
    public ExecutionRequest removeLabel(UUID uuidRequest, List<UUID> uuidLabels) {
        ExecutionRequest executionRequest = findById(uuidRequest);
        List<UUID> labels = executionRequest.getLabels()
                .stream()
                .filter(label1 -> !uuidLabels.contains(label1))
                .collect(Collectors.toList());
        executionRequest.setLabels(labels);
        save(executionRequest);
        return executionRequest;
    }

    /**
     * Find test runs and log records with names, which contain searchValue.
     *
     * @return list of found objects
     */
    public List<StepPath> findStepBySearchValue(UUID uuidRequest, String searchValue) {
        List<StepPath> stepPaths = new LinkedList<>();
        PathsGenerator generator = new PathsGenerator(logRecordService);
        List<TestRun> testRuns = getAllMatchesTestRuns(uuidRequest, searchValue);
        StepPath foundTestRuns = generator.generatePathToFoundTestRuns(testRuns);
        if (!foundTestRuns.getUuidSteps().isEmpty()) {
            stepPaths.add(foundTestRuns);
        }
        getAllTestRuns(uuidRequest).forEach(testRun -> {
            UUID uuid = testRun.getUuid();
            List<LogRecord> logRecords = testRunService.getAllMatchesLogRecords(uuid, searchValue);
            stepPaths.addAll(generator.generatePathToFoundLogRecords(logRecords));
        });
        return stepPaths;
    }

    /**
     * Returns list of ERs with qaHosts and taHosts by ProjectUuid.
     *
     * @param projectUuid Project Id
     * @return List of ExecutionRequestTestResult
     */
    public List<ExecutionRequestTestResult> getLastErsByProjectUuidWithTestRuns(UUID projectUuid) {
        List<ExecutionRequest> executionRequests = findFinishedErByProjectAndSortByFinishDate(projectUuid);
        return createErTestResultsByErs(executionRequests);
    }

    /**
     * Return list of ERs with qaHosts and taHosts by TestPlanUuid.
     *
     * @param testPlanUuid for found ERs
     * @return list of {@link TestResult}
     */
    public List<TestResult> getLastErsByTestplanUuidWithQaHostsAndTaHosts(UUID testPlanUuid) {
        List<ExecutionRequest> executionRequests = getSortedExecutionRequestsForTestPlan(testPlanUuid);
        return createTestResultsByErs(executionRequests);
    }

    private List<ExecutionRequestTestResult> createErTestResultsByErs(List<ExecutionRequest> executionRequests) {
        Set<UUID> erIds = StreamUtils.extractIds(executionRequests);
        Map<UUID, EnvironmentsInfo> executionRequestMap =
                environmentsInfoService.getDataForErTestResultByExecutionRequestIds(erIds);

        List<Environment> environments = getExecutionRequestEnvironments(executionRequests);
        Map<UUID, Environment> environmentMap = StreamUtils.toIdEntityMap(environments, Environment::getId);
        return executionRequests.stream()
                .map(executionRequest -> {
                    EnvironmentsInfo environmentsInfo = executionRequestMap.getOrDefault(executionRequest.getUuid(),
                            new EnvironmentsInfo());
                    List<SystemInfo> systemInfos = getSystemInfosByEnvironmentsInfo(environmentsInfo);

                    List<String> hosts = systemInfos.stream()
                            .map(SystemInfo::getUrls).flatMap(Collection::stream)
                            .collect(Collectors.toList());
                    List<String> solutionBuilds = systemInfos.stream()
                            .map(SystemInfo::getVersion)
                            .filter(version -> !version.isEmpty())
                            .distinct()
                            .collect(Collectors.toList());

                    Environment environment = environmentMap.get(executionRequest.getEnvironmentId());

                    return new ExecutionRequestTestResult(executionRequest, environment, hosts, solutionBuilds);
                })
                .collect(Collectors.toList());
    }

    private List<SystemInfo> getSystemInfosByEnvironmentsInfo(EnvironmentsInfo environmentsInfo) {
        Stream<SystemInfo> qaSystemInfo =
                Optional.ofNullable(environmentsInfo.getQaSystemInfoList())
                        .map(Collection::stream)
                        .orElseGet(Stream::empty);
        Stream<SystemInfo> taSystemInfo =
                Optional.ofNullable(environmentsInfo.getTaSystemInfoList())
                        .map(Collection::stream)
                        .orElseGet(Stream::empty);
        return Stream.concat(qaSystemInfo, taSystemInfo).collect(Collectors.toList());
    }

    /**
     * Returns list of ERs with TestRuns by TestScopeUuid.
     *
     * @param testScopeUuid for found ERs
     * @return list of {@link TestResult}
     */
    public List<TestResult> getTestResultsByRestScopeUuid(UUID testScopeUuid) {
        List<ExecutionRequest> executionRequests = findByTestScopeUuid(testScopeUuid);
        return createTestResultsByErs(executionRequests);
    }

    private List<TestResult> createTestResultsByErs(List<ExecutionRequest> executionRequests) {
        Map<UUID, TestRun> testRuns = getDataForTestResultByExecutionRequests(executionRequests);
        return executionRequests.stream()
                .map(executionRequest -> {
                    TestResult result = new TestResult(executionRequest);
                    TestRun testRun = testRuns.getOrDefault(executionRequest.getUuid(), new TestRun());
                    result.setQaHosts(testRun.getQaHost());
                    result.setTaHosts(testRun.getTaHost());
                    return result;
                })
                .collect(Collectors.toList());
    }

    private List<TestRun> getAllMatchesTestRuns(UUID uuidRequest, String searchValue) {
        return testRunService.getAllMatchedTestRunsByRequestId(uuidRequest, searchValue);
    }

    /**
     * Return List of ER-s in standard format.
     *
     * @param testPlanId for found ER-s
     * @return list of ER-s
     */
    public List<ExecutionRequest> findByTestPlanId(UUID testPlanId) {
        return repository.findAllByTestPlanId(testPlanId);
    }

    /**
     * Returns list of ERs by TestPlanUuid.
     */
    public List<ExecutionRequestResponse> findByTestPlanUuid(UUID testPlanUuid) {
        List<ExecutionRequest> executionRequests = repository.findAllByTestPlanId(testPlanUuid);
        return buildExecutionRequestResponses(executionRequests);
    }

    /**
     * Returns list of ERs by ProjectUuid.
     */
    public List<ExecutionRequest> findFinishedErByProjectAndSortByFinishDate(UUID projectUuid) {
        PageRequest request = PageRequest.of(DEFAULT_NUMBER_PAGE, limit, Sort.by(Sort.Direction.DESC,
                FINISH_DATE_FILED_NAME));
        return repository.findLimitRequestsByProjectIdAndExecutionStatusNotIn(projectUuid,
                Arrays.asList(ExecutionStatuses.IN_PROGRESS, ExecutionStatuses.NOT_STARTED), request);
    }

    /**
     * Returns list of ERs by ProjectUuid.
     */
    public List<ExecutionRequest> findFinishedErByProjectAndSortByFinishDate(UUID projectUuid,
                                                                             Timestamp dateFilter) {
        PageRequest request = PageRequest.of(DEFAULT_NUMBER_PAGE, limit,
                Sort.by(Sort.Direction.DESC, FINISH_DATE_FILED_NAME));
        return repository.findAllByProjectIdAndExecutionStatusNotInAndFinishDateAfter(projectUuid,
                Arrays.asList(ExecutionStatuses.IN_PROGRESS, ExecutionStatuses.NOT_STARTED), dateFilter, request);
    }

    /**
     * Find ER-s by test plan, sort and limit results.
     *
     * @param testPlanUuid for find ER-s
     * @param startIndex   number of start element
     * @param endIndex     number of finish element
     * @param columnType   field for sorting
     * @param sortType     direction of sorting
     * @return sorted ER-s by test plan
     */
    public List<ExecutionRequest> findPageByTestPlanUuidAndSort(
            UUID testPlanUuid, int startIndex, int endIndex, String columnType, String sortType) {
        Sort.Direction type =
                !Sort.Direction.ASC.name().equals(sortType) && !Sort.Direction.DESC.name().equals(sortType)
                        ? Sort.Direction.DESC
                        : Sort.Direction.fromString(sortType);
        int countOnPage = endIndex - startIndex;
        int numOfPage = startIndex / countOnPage;
        PageRequest request = PageRequest.of(numOfPage, countOnPage, Sort.by(type, prepareColumnType(columnType)));
        return repository.findAllByTestPlanId(testPlanUuid, request);
    }

    /**
     * Returns list of ERs by TestPlanUuid and sort, shortened into DTOs.
     */
    public List<ExecutionRequestResponse> findResponsePageByTestPlanUuidAndSort(
            UUID testPlanUuid, int startIndex, int endIndex, String columnType, String sortType) {
        List<ExecutionRequest> executionRequests =
                findPageByTestPlanUuidAndSort(testPlanUuid, startIndex, endIndex, columnType, sortType);
        return buildExecutionRequestResponses(executionRequests);
    }

    /**
     * Returns list of short ERs.
     */
    public List<ExecutionRequestResponse> buildExecutionRequestResponses(List<ExecutionRequest> executionRequests) {
        List<Environment> environments = getExecutionRequestEnvironments(executionRequests);
        Map<UUID, Environment> environmentMap = StreamUtils.toKeyEntityMap(environments, Environment::getId);
        List<Label> labels = getExecutionRequestsLabels(executionRequests);
        Map<UUID, List<Label>> filteredByLabelsMap = getExecutionRequestsAndFilteredByLabelsMap(executionRequests,
                labels);
        List<ExecutionRequest> initialExecutionRequests = getInitialExecutionRequests(executionRequests);
        Map<UUID, ExecutionRequest> initialExecutionRequestMap = StreamUtils.toKeyEntityMap(
                initialExecutionRequests, ExecutionRequest::getUuid);
        return executionRequests.stream()
                .map(request -> {
                    ExecutionRequestResponse response = modelMapper.map(request, ExecutionRequestResponse.class);
                    response.setUuid(request.getUuid());
                    Environment environment = environmentMap.get(request.getEnvironmentId());
                    response.setEnvironment(environment);
                    response.setExecutor(new Executor(request.getExecutorId(), request.getExecutorName()));
                    response.setFilteredByLabels(filteredByLabelsMap.get(request.getUuid()));
                    UUID initialErId = request.getInitialExecutionRequestId();
                    if (nonNull(initialErId) && initialExecutionRequestMap.containsKey(initialErId)) {
                        String initialErName = initialExecutionRequestMap.get(initialErId).getName();
                        InitialExecutionRequest initialEr = new InitialExecutionRequest(initialErId, initialErName);
                        response.setInitialExecutionRequest(initialEr);
                    }
                    return response;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get execution request environments.
     *
     * @param testPlanId test plan id search param
     * @return found environments
     */
    public List<Environment> getExecutionRequestEnvironments(UUID testPlanId) {
        List<ExecutionRequest> executionRequests = findByTestPlanId(testPlanId);
        return getExecutionRequestEnvironments(executionRequests);
    }

    /**
     * Get execution request environments.
     *
     * @return found environments
     */
    public List<Environment> getExecutionRequestEnvironments(List<ExecutionRequest> executionRequests) {
        Set<UUID> envIds = StreamUtils.extractIds(executionRequests, ExecutionRequest::getEnvironmentId);
        BaseSearchRequest searchRequest = BaseSearchRequest.builder().ids(envIds).build();
        return environmentsService.searchEnvironments(searchRequest);
    }

    /**
     * Get a list with initial Execution requests for reruned Execution requests.
     */
    private List<ExecutionRequest> getInitialExecutionRequests(List<ExecutionRequest> executionRequests) {
        Set<UUID> initialExecutionRequestIds = StreamUtils.extractIds(executionRequests,
                ExecutionRequest::getInitialExecutionRequestId);
        return repository.findAllByUuidIn(initialExecutionRequestIds);
    }

    /**
     * Get labels by list of execution requests.
     *
     * @param executionRequests list of execution requests
     * @return list of found labels
     */
    public List<Label> getExecutionRequestsLabels(List<ExecutionRequest> executionRequests) {
        Set<UUID> labelIds = StreamUtils.extractFlatIds(executionRequests, ExecutionRequest::getFilteredByLabels);
        return labelIds.isEmpty() ? Collections.emptyList() : labelsService.getLabels(labelIds);
    }

    /**
     * Get map of execution request ids and execution request's labels.
     *
     * @param executionRequests list of execution requests
     * @param labels            list of labels
     * @return map of execution request ids and execution request's labels
     */
    public Map<UUID, List<Label>> getExecutionRequestsAndFilteredByLabelsMap(List<ExecutionRequest> executionRequests,
                                                                             List<Label> labels) {
        Map<UUID, List<Label>> executionRequestAndLabelsMap = new HashMap<>();
        executionRequests.forEach(executionRequest -> {
            if (!CollectionUtils.isEmpty(executionRequest.getFilteredByLabels())) {
                executionRequestAndLabelsMap.put(executionRequest.getUuid(),
                        labels.stream()
                                .filter(label -> executionRequest.getFilteredByLabels().contains(label.getUuid()))
                                .collect(Collectors.toList()));
            }
        });
        return executionRequestAndLabelsMap;
    }

    /**
     * Get execution request executor users.
     *
     * @param testPlanId test plan id search param
     * @return found users
     */
    public List<Executor> getExecutionRequestExecutors(UUID testPlanId) {
        List<ExecutionRequest> executionRequests = findByTestPlanId(testPlanId);
        return executionRequests.stream().map(executionRequest ->
                new Executor(executionRequest.getExecutorId(), executionRequest.getExecutorName()))
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Get executors by user identifiers.
     *
     * @param userIds user identifiers
     * @return list of executors
     */
    public List<Executor> getExecutionRequestExecutors(Set<UUID> userIds) {
        return userService.getUserByIds(userIds)
                .stream()
                .map(this::mapUserInfoToExecutor)
                .collect(Collectors.toList());
    }

    private String prepareColumnType(String column) {
        return DEFAULT_DATE_FILED_NAME.equalsIgnoreCase(column) ? START_DATE_FILED_NAME : column;
    }

    /**
     * Returns list of ERs by TestPlanUuid and sort.
     */
    public List<ExecutionRequest> findPageByTestPlanUuidBetweenPeriodAndSort(
            UUID testPlanUuid, Timestamp startDate, Timestamp endDate, int startIndex, int endIndex,
            String columnType, String sortType) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortType), prepareColumnType(columnType));
        PageRequest request = PageRequest.of(startIndex, endIndex - startIndex, sort);
        if (START_DATE_FILED_NAME.equalsIgnoreCase(columnType)) {
            return repository.findAllByTestPlanIdAndStartDateBetween(testPlanUuid, startDate, endDate, request);
        } else {
            return repository.findAllByTestPlanIdAndFinishDateBetween(testPlanUuid, startDate, endDate, request);
        }
    }

    /**
     * Remove execution request with all test runs and log records.
     *
     * @param requestsUuidList list of uuid of execution request
     */
    public void recursiveDeleteListRequests(List<UUID> requestsUuidList) {
        List<UUID> testRunsUuidList = new LinkedList<>();
        requestsUuidList.forEach(requestsUuid ->
                testRunsUuidList.addAll(getAllTestRunUuidsByExecutionRequestId(requestsUuid))
        );
        testRunService.deleteListTestRuns(testRunsUuidList);
        repository.deleteAllByUuidIn(requestsUuidList);
    }

    private List<ComparisonExecutionRequest> getComparisonEr(List<ExecutionRequest> ers) {
        List<ComparisonExecutionRequest> compErs = getComparisonExecutionRequestWithTestRuns(ers);
        for (ComparisonExecutionRequest request : compErs) {
            Set<ComparisonTestRun> compTestRuns = setNonCompTestRunAndUpdateCompTestRun(compErs, request);
            request.setTestRuns(compTestRuns);
        }
        return compErs;
    }

    private List<ComparisonExecutionRequest> getComparisonErWithSteps(List<ExecutionRequest> ers) {
        List<ComparisonExecutionRequest> compErs = getComparisonExecutionRequestWithTestRuns(ers);
        for (ComparisonExecutionRequest request : compErs) {
            Set<ComparisonTestRun> compTestRuns = setNonCompTestRunAndUpdateCompTestRun(compErs, request);
            for (ComparisonTestRun testRun : compTestRuns) {
                List<LogRecord> logRecords = logRecordService
                        .findLogRecordsWithSpecificFieldsByTestRunIdOrderByStartDateAsc(testRun.getId());
                if (!logRecords.isEmpty()) {
                    testRun.setSteps(
                            logRecords.stream()
                                    .filter(logRecord -> logRecord.isSection() && !logRecord.isCompaund())
                                    .map(logRecord -> {
                                        ComparisonStep step = new ComparisonStep();
                                        step.setStepName(logRecord.getName());
                                        step.setStatuses(logRecord.getTestingStatus());
                                        step.setDuration(logRecord.getDuration());
                                        return step;
                                    })
                                    .collect(Collectors.toSet())
                    );
                }
            }
            request.setTestRuns(compTestRuns);
        }
        return compErs;
    }

    private List<ComparisonExecutionRequest> getComparisonExecutionRequestWithTestRuns(List<ExecutionRequest> ers) {
        List<ComparisonExecutionRequest> compErs = new ArrayList<>();
        for (ExecutionRequest executionRequest : ers) {
            ComparisonExecutionRequest request = new ComparisonExecutionRequest();
            request.setErName(executionRequest.getName());
            request.setTestRuns(getAllTestRuns(executionRequest.getUuid())
                    .stream()
                    .filter(testRun -> !testRun.getName().equals(DefaultSuiteNames.EXECUTION_REQUESTS_LOGS.getName()))
                    .map(testRun -> {
                        ComparisonTestRun compTr = new ComparisonTestRun();
                        compTr.setTrName(testRun.getName());
                        compTr.setTestCaseId(testRun.getTestCaseId());
                        compTr.setStatuses(testRun.getTestingStatus());
                        compTr.setDuration(testRun.getDuration());
                        compTr.setId(testRun.getUuid());
                        return compTr;
                    })
                    .collect(Collectors.toSet()));
            compErs.add(request);
        }
        return compErs;
    }

    private Set<ComparisonTestRun> setNonCompTestRunAndUpdateCompTestRun(List<ComparisonExecutionRequest> compErs,
                                                                         ComparisonExecutionRequest currentRequest) {
        List<ComparisonExecutionRequest> compErsWithoutCurrent = new ArrayList<>(compErs);
        compErsWithoutCurrent.remove(currentRequest);
        Set<UUID> testCaseIds = new HashSet<>();
        compErsWithoutCurrent.forEach(er ->
                testCaseIds.addAll(er.getTestRuns().stream()
                        .map(ComparisonTestRun::getTestCaseId).collect(Collectors.toList()))
        );
        Set<ComparisonTestRun> compTestRuns = currentRequest.getTestRuns();
        currentRequest.setNonComparisonTestRuns(compTestRuns.stream()
                .filter(testRun -> !testCaseIds.contains(testRun.getTestCaseId())).collect(Collectors.toSet()));
        compTestRuns.removeAll(currentRequest.getNonComparisonTestRuns());
        return compTestRuns;
    }

    /**
     * Returns list of ERs with filled only requestUuid field.
     */
    public List<ExecutionRequest> getNotFinishedRequests() {
        return repository.findRequestsIdByExecutionStatusIn(
                Arrays.asList(ExecutionStatuses.NOT_STARTED, ExecutionStatuses.IN_PROGRESS));
    }

    /**
     * Stops ER with provided UUID.
     *
     * @deprecated use ExecutionRequestLoggingController instead of this
     */
    @Deprecated
    public void stopExecutionRequest(ExecutionRequest er, long lastFinishDate) {
        final UUID executionRequestId = er.getUuid();
        log.debug("Stop Execution Request: {}.", executionRequestId);
        er.setFinishDate(new Timestamp(lastFinishDate));
        ExecutionStatuses executionStatus = er.getExecutionStatus();
        log.trace("Execution Request: {} executionStatus {}", executionRequestId, executionStatus);
        if (executionStatus == null || executionStatus.getId() < ExecutionStatuses.FINISHED.getId()) {
            er.setExecutionStatus(ExecutionStatuses.FINISHED);
            log.trace("Execution Request: {} status was changed to {}", executionRequestId, ExecutionStatuses.FINISHED);
        }
        List<TestRun> testRuns = testRunService.findAllByExecutionRequestId(executionRequestId);
        if (!testRuns.isEmpty()) {
            log.debug("Start calculate rates for ER {}", executionRequestId);
            rateCalculator.calculateRates(er, testRuns);
            log.debug("Start calculate count of screenshots for every TR in ER {}", executionRequestId);
            logRecordService.saveCountScreenshots(executionRequestId, testRuns);
        }
        log.debug("Start calculate duration for ER {}", executionRequestId);
        calculateDuration(er);
        save(er);
        log.debug("Execution Request: {} was finished and calculate issues.", executionRequestId);
    }

    private void calculateDuration(ExecutionRequest er) {
        long finishTime = er.getFinishDate().getTime();
        long startTime = er.getStartDate().getTime();
        long duration = TimeUnit.MILLISECONDS.toSeconds(finishTime - startTime);
        er.setDuration(duration);
    }

    public List<ExecutionRequest> findAllByJointExecutionKey(String jointExecutionKey) {
        return repository.findAllByJointExecutionKey(jointExecutionKey);
    }

    /**
     * Get execution request project.
     *
     * @param executionRequestId er id
     * @return found project
     */
    public Project getProjectId(UUID executionRequestId) {
        ExecutionRequest executionRequest = get(executionRequestId);
        UUID projectId = executionRequest.getProjectId();

        return projectsService.getProjectById(projectId);
    }

    /**
     * Gets project id by execution request id if provided project id is Null.
     */
    public UUID getProjectId(UUID projectId, UUID executionRequestId) {
        if (isNull(projectId)) {
            return getProjectIdByExecutionRequestId(executionRequestId);
        }
        return projectId;
    }

    /**
     * Get project ID by execution request ID.
     *
     * @param id ID of Execution request
     * @return UUID of project.
     */
    public UUID getProjectIdByExecutionRequestId(UUID id) {
        ExecutionRequest executionRequest = repository.findProjectIdByUuid(id);
        if (isNull(executionRequest)) {
            throw new RamExecutionRequestIdNotFoundException(id);
        }
        return executionRequest.getProjectId();
    }

    /**
     * Get sorted and limited execution requests for current test plan.
     *
     * @param testPlanUuid for found
     * @return sorted by start date and limited list of {@link ExecutionRequest}
     */
    public List<ExecutionRequest> getSortedExecutionRequestsForTestPlan(UUID testPlanUuid) {
        PageRequest request = PageRequest.of(DEFAULT_NUMBER_PAGE, limit,
                Sort.by(Sort.Direction.DESC, START_DATE_FILED_NAME));
        return repository.findAllByTestPlanId(testPlanUuid, request);
    }

    /**
     * Update execution status of {@link ExecutionRequest}.
     *
     * @param requestUuid     for find {@link ExecutionRequest}
     * @param executionStatus new value of status
     * @return updated {@link ExecutionRequest}
     */
    public ExecutionRequest updateExecutionStatus(UUID requestUuid, ExecutionStatuses executionStatus) {
        try {
            ExecutionRequest executionRequest = findById(requestUuid);
            executionRequest.setExecutionStatus(executionStatus);
            if (ExecutionStatuses.FINISHED.equals(executionStatus)
                    || ExecutionStatuses.TERMINATED.equals(executionStatus)
                    || ExecutionStatuses.TERMINATED_BY_TIMEOUT.equals(executionStatus)) {
                testRunService.stopServiceTestRun(requestUuid);
                stopExecutionRequest(executionRequest, System.currentTimeMillis());
                testCaseService.updateCaseStatuses(getAllTestRuns(requestUuid));
            } else {
                save(executionRequest);
                log.trace("Set status {} for ER: {}", executionStatus.getName(), requestUuid);
            }
            if (ExecutionStatuses.FINISHED == executionStatus
                    || ExecutionStatuses.TERMINATED_BY_TIMEOUT == executionStatus
                    || ExecutionStatuses.TERMINATED == executionStatus) {
                try {
                    jiraIntegrationService.syncWithJira(executionRequest);
                } catch (Exception e) {
                    log.error("Unable synchronization with jira execution request: {}", requestUuid);
                }
            }
            return executionRequest;
        } catch (Exception e) {
            log.error("Unable update ER status", e);
            return new ExecutionRequest();
        }
    }

    public List<ExecutionRequest> findAllRequestsNotFinished(UUID projectId) {
        return repository.findAllByProjectIdAndExecutionStatusNotIn(projectId,
                Collections.singletonList(ExecutionStatuses.FINISHED));
    }

    /**
     * Returns all ERs with the specified ids.
     */
    public List<ExecutionRequest> findAllByUuidIn(List<UUID> uuids) {
        return this.repository.findAllByUuidIn(uuids);
    }

    /**
     * Returns page of ERs by the specified test plan
     * id with:
     * specified analyzed by qa value,
     * finish date between startDate and endDate,
     * sorted in a descending order by finish date.
     *
     * @param analyzedByQa if null ignored
     */
    public List<ExecutionRequest> findPageByTestPlanUuidBetweenPeriodAndAnalyzedByQa(
            UUID testPlanUuid,
            Timestamp startDate,
            Timestamp endDate,
            Boolean analyzedByQa,
            int startIndex,
            int endIndex) {
        PageRequest request = PageRequest.of(startIndex, endIndex - startIndex,
                Sort.by(Sort.Direction.DESC, FINISH_DATE_FILED_NAME));
        if (analyzedByQa == null) {
            return customRepository.getByTestPlanAndFinishDateBetweenOrEquals(testPlanUuid, startDate, endDate,
                    request);
        }
        return customRepository.getByTestPlanAndFinishDateBetweenOrEqualsAndAnalyzedByQa(testPlanUuid, startDate,
                analyzedByQa, request);
    }

    /**
     * Returns page of ERs by the specified test plan
     * id with specified analyzed by qa value
     * and sorted in a descending order by finish date.
     *
     * @param analyzedByQa may be null in this case it is not taken into account
     */
    public List<ExecutionRequest> findPageByTestPlanUuidAndAnalyzedByQa(
            UUID testPlanUuid,
            Boolean analyzedByQa,
            int startIndex,
            int endIndex) {
        PageRequest request = PageRequest.of(startIndex, endIndex - startIndex,
                Sort.by(Sort.Direction.DESC, FINISH_DATE_FILED_NAME));
        if (analyzedByQa == null) {
            return repository.findAllByTestPlanId(testPlanUuid, request);
        }
        return repository.findAllByTestPlanIdAndAnalyzedByQaEquals(testPlanUuid, analyzedByQa, request);
    }

    /**
     * Update analysed by qa field in provided execution requests.
     *
     * @param executionRequestIds execution request ids
     * @param value               analysed by qa value
     */
    public void updateAnalyzedByQa(Collection<UUID> executionRequestIds, boolean value) {
        log.info("Updating analyzed by qa parameter with value '{}' for ER's: {}", value, executionRequestIds);
        final List<ExecutionRequest> executionRequests = getExecutionRequestsByIds(executionRequestIds);
        if (!CollectionUtils.isEmpty(executionRequests)) {
            log.debug("Found execution requests: {}", executionRequestIds);
            executionRequests.forEach(executionRequest -> executionRequest.setAnalyzedByQa(value));
            saveAll(executionRequests);
        }
    }

    /**
     * Returns an Test Plan id by Execution Request Id.
     *
     * @param id Execution Request identifier
     * @return Test Plan identifier
     */
    public UUID getTestPlanIdByExecutionRequestId(UUID id) {
        ExecutionRequest executionRequest = repository.findByUuid(id);
        return executionRequest.getTestPlanId();
    }


    /**
     * Create link for execution request.
     *
     * @param executionRequestId ID of execution request
     * @param projectId          ID of project
     * @return generated link
     */
    public String generateErLink(UUID executionRequestId, UUID projectId) {
        String erLink = catalogueUrl
                + "/"
                + PROJECT
                + "/"
                + projectId
                + "/"
                + RAM
                + "/"
                + EXECUTION_REQUEST
                + "/"
                + executionRequestId;
        return String.format(A_HREF_OPEN + "%s" + A_HREF_CLOSE, erLink, erLink);
    }

    /**
     * Get execution request config.
     *
     * @param executionRequestId execution request identifier
     * @return config
     */
    public ExecutionRequestConfig getExecutionRequestConfig(UUID executionRequestId) {
        log.debug("Get config for execution request with id '{}'", executionRequestId);
        final ExecutionRequest executionRequest = get(executionRequestId);
        return getExecutionRequestConfig(executionRequest);
    }

    /**
     * Get execution request config.
     *
     * @param executionRequest execution request
     * @return config
     */
    public ExecutionRequestConfig getExecutionRequestConfig(ExecutionRequest executionRequest) {
        final UUID executionRequestId = executionRequest.getUuid();
        ExecutionRequestConfig resultExecutionRequestConfig = lockManager
                .executeWithLock("get_or_create_config_for_execution_request" + executionRequestId.toString(),
                lockDurationForCreatingConfigSec,
                () -> {
                    log.debug("Get config for execution request with id '{}'", executionRequestId);
                    ExecutionRequestConfig config = configRepository.findByExecutionRequestId(executionRequestId);
                    if (nonNull(config)) {
                        log.debug("Founded config: {}", config);
                        return config;
                    } else {
                        ExecutionRequestConfig executionRequestConfig = new ExecutionRequestConfig();
                        executionRequestConfig.setExecutionRequestId(executionRequestId);
                        final UUID widgetConfigTemplateId = executionRequest.getWidgetConfigTemplateId();
                        if (nonNull(widgetConfigTemplateId)) {
                            executionRequestConfig.setWidgetConfigTemplateId(widgetConfigTemplateId);
                        }
                        log.debug("Cannot found config, creating new one: {}", executionRequestConfig);
                        return configRepository.save(executionRequestConfig);
                    }

                }, () -> null);
        if (resultExecutionRequestConfig == null) {
            String message = String.format("Error occurred while obtain config lock for execution request %s",
                    executionRequest.getUuid());
            log.error(message);
            throw new RuntimeException(message);
        }
        return resultExecutionRequestConfig;

    }

    /**
     * Update execution request config.
     *
     * @param executionRequestId execution request
     * @param request            update request
     */
    public void updateExecutionRequestConfig(UUID executionRequestId, ExecutionRequestConfigUpdateRequest request) {
        final ExecutionRequest executionRequest = this.get(executionRequestId);
        this.updateExecutionRequestConfig(executionRequest, request);
    }

    /**
     * Update execution request config.
     *
     * @param executionRequest execution request
     * @param request          update request
     */
    @Caching(evict = {
            @CacheEvict(value = CacheConstants.ATP_RAM_REPORTS,
                    key = "{#executionRequest.getUuid(), #executionRequest.getCountLogRecords(), "
                            + "T(org.qubership.atp.ram.enums.ExecutionRequestWidgets).SUMMARY_STATISTIC}"),
            @CacheEvict(value = CacheConstants.ATP_RAM_REPORTS,
                    key = "{#executionRequest.getUuid(), #executionRequest.getCountLogRecords(), "
                            + "T(org.qubership.atp.ram.enums.ExecutionRequestWidgets).SUMMARY_STATISTIC_FOR_USAGES}"),
            @CacheEvict(value = CacheConstants.ATP_RAM_REPORTS,
                    key = "{#executionRequest.getUuid(), #executionRequest.getCountLogRecords(), "
                            + "T(org.qubership.atp.ram.enums.ExecutionRequestWidgets)"
                            + ".SUMMARY_STATISTIC_SCENARIO_TYPE}"),
            @CacheEvict(value = CacheConstants.ATP_RAM_REPORTS,
                    key = "{#executionRequest.getUuid(), #executionRequest.getCountLogRecords(), "
                            + "T(org.qubership.atp.ram.enums.ExecutionRequestWidgets).TEST_CASES}"),
            @CacheEvict(value = CacheConstants.ATP_RAM_REPORTS,
                    key = "{#executionRequest.getUuid(), #executionRequest.getCountLogRecords(), "
                            + "T(org.qubership.atp.ram.enums.ExecutionRequestWidgets).TOP_ISSUES}"),
    })
    public void updateExecutionRequestConfig(ExecutionRequest executionRequest,
                                             ExecutionRequestConfigUpdateRequest request) {
        log.debug("Update execution request '{}' config: {}", executionRequest.getUuid(), request);
        ExecutionRequestConfig existedConfig = configRepository.findByExecutionRequestId(executionRequest.getUuid());
        if (null != executionRequest.getWidgetConfigTemplateId()) {
            executionRequest.setWidgetConfigTemplateId(request.getWidgetConfigTemplateId());
            save(executionRequest);
        }
        existedConfig.setWidgetConfigTemplateId(request.getWidgetConfigTemplateId());
        checkDefaultLabelTemplateConfigOverride(existedConfig);
        configRepository.save(existedConfig);
    }

    private void checkDefaultLabelTemplateConfigOverride(ExecutionRequestConfig executionRequestConfig) {
        final UUID widgetConfigTemplateId = executionRequestConfig.getWidgetConfigTemplateId();
        if (!executionRequestConfig.isDefaultLabelTemplateChanged() && nonNull(widgetConfigTemplateId)) {
            final UUID executionRequestId = executionRequestConfig.getExecutionRequestId();
            final WidgetConfigTemplate widgetConfigTemplate = widgetConfigTemplateService.get(widgetConfigTemplateId);
            if (nonNull(widgetConfigTemplate)) {
                final ExecutionRequest executionRequest = get(executionRequestId);
                final UUID erLabelTemplateId = executionRequest.getLabelTemplateId();
                final WidgetConfigTemplate.WidgetConfig summaryStatisticWidgetConfig =
                        widgetConfigTemplate.getWidgetConfig(ExecutionRequestWidgets.SUMMARY_STATISTIC.getWidgetId());
                final UUID summaryWidgetLabelTemplateId = summaryStatisticWidgetConfig.getLabelTemplateId();
                if (nonNull(summaryWidgetLabelTemplateId) && !summaryWidgetLabelTemplateId.equals(erLabelTemplateId)) {
                    executionRequestConfig.setDefaultLabelTemplateChanged(true);
                    executionRequest.setLabelTemplateId(null);
                    save(executionRequest);
                }
            }
        }
    }

    /**
     * Get ExecutionRequests by filtering, sorting and pagination.
     *
     * @param testPlanUuid testPlanId
     * @param request      pagination, sorting and filtering params
     * @return list of ExecutionRequest
     */
    public PaginatedResponse<ExecutionRequest> getExecutionRequests(UUID testPlanUuid,
                                                                    ExecutionRequestSearchRequest request) {
        CriteriaDefinition searchCriteria = filteringService.buildSearchCriteria(testPlanUuid, request.getFilters());
        final List<SortingParams> requestSort = request.getSort();
        final Sort sort = nonNull(requestSort) ? sortingService.buildSort(requestSort) : Sort.unsorted();
        final Integer page = request.getPage();
        final Integer size = request.getSize();
        Pageable pageable = PageRequest.of(page, size, sort);
        List<ExecutionRequest> executionRequests = customRepository.searchExecutionRequests(searchCriteria, pageable);
        final int totalCount = customRepository.getExecutionRequestsCountByCriteria(searchCriteria);
        return new PaginatedResponse<>(totalCount, executionRequests);
    }

    /**
     * Get ExecutionRequests by filtering, sorting and pagination and build ExecutionRequestResponse from it.
     *
     * @param testPlanUuid testPlanId
     * @param req          pagination, sorting and filtering params
     * @return list of ExecutionRequestResponse
     */
    public PaginatedResponse<ExecutionRequestResponse> getExecutionRequestsResponse(UUID testPlanUuid,
                                                                                    ExecutionRequestSearchRequest req) {
        PaginatedResponse<ExecutionRequest> executionRequests = getExecutionRequests(testPlanUuid, req);
        List<ExecutionRequestResponse> data = buildExecutionRequestResponses(executionRequests.getData());
        return new PaginatedResponse<>(executionRequests.getTotal(), data);
    }

    /**
     * Get execution request rates info.
     *
     * @param executionRequestId execution request id
     * @return rates response
     */
    public ExecutionRequestRatesResponse getRates(UUID executionRequestId) {
        final ExecutionRequest executionRequest = get(executionRequestId);
        final ExecutionRequestRatesResponse response =
                StreamUtils.mapToClazz(executionRequest, ExecutionRequestRatesResponse.class);
        response.setTestCasesCount(executionRequest.getCountOfTestRuns());
        final Executor executor =
                new Executor(executionRequest.getExecutorId(), executionRequest.getExecutorName());
        response.setExecutor(executor.getUsername());
        final List<TestRun> testRuns = getAllTestRuns(executionRequestId);
        final Map<TestingStatuses, TestingStatusesStat> stats =
                rateCalculator.calculateTestRunsTestingStatusStats(executionRequest, testRuns);
        setStatusStat(response, TestingStatuses.PASSED, stats,
                ExecutionRequestRatesResponse::setPassedRateCount, ExecutionRequestRatesResponse::setPassedRate);
        setStatusStat(response, TestingStatuses.WARNING, stats,
                ExecutionRequestRatesResponse::setWarningRateCount, ExecutionRequestRatesResponse::setWarningRate);
        setStatusStat(response, TestingStatuses.FAILED, stats,
                ExecutionRequestRatesResponse::setFailedRateCount, ExecutionRequestRatesResponse::setFailedRate);
        setStatusStat(response, TestingStatuses.STOPPED, stats,
                ExecutionRequestRatesResponse::setStoppedRateCount, ExecutionRequestRatesResponse::setStoppedRate);
        setStatusStat(response, TestingStatuses.NOT_STARTED, stats,
                ExecutionRequestRatesResponse::setNotStartedRateCount,
                ExecutionRequestRatesResponse::setNotStartedRate);
        return response;
    }

    private void setStatusStat(ExecutionRequestRatesResponse response,
                               TestingStatuses status,
                               Map<TestingStatuses, TestingStatusesStat> statsMap,
                               BiConsumer<ExecutionRequestRatesResponse, Integer> countSetFunc,
                               BiConsumer<ExecutionRequestRatesResponse, Float> rateSetFunc) {
        TestingStatusesStat statusStats = statsMap.get(status);
        if (nonNull(statusStats)) {
            countSetFunc.accept(response, statusStats.getCount());
            rateSetFunc.accept(response, statusStats.getRate());
        }
    }

    /**
     * Search execution request failed log records by regex.
     *
     * @param executionRequestId execution request identifier
     * @param request            search request
     * @return search result
     */
    public LogRecordRegexSearchResponse searchFailedLogRecords(UUID executionRequestId,
                                                               LogRecordRegexSearchRequest request) {
        log.info("Search failed log records for ER '{}' by request: {}", executionRequestId, request);
        List<TestRun> allFailedTestRuns = getAllFailedTestRuns(executionRequestId);
        Set<UUID> allFailedTestRunIds = StreamUtils.extractIds(allFailedTestRuns);
        log.debug("Found failed test runs: {}", allFailedTestRunIds);
        List<LogRecord> allFailedLogRecords = testRunService.getAllFailedLogRecords(allFailedTestRunIds);
        log.debug("Found failed log records: {}", allFailedLogRecords.size());
        Pattern pattern = Pattern.compile(request.getRegex());
        List<LogRecord> matchedLogRecords = allFailedLogRecords.stream()
                .filter(logRecord -> !StringUtils.isEmpty(logRecord.getMessage()))
                .filter(logRecord -> pattern.matcher(logRecord.getMessage()).find())
                .sorted(Comparator.comparing(LogRecord::getMessage))
                .collect(Collectors.toList());
        log.debug("Matched log records: {}", StreamUtils.extractIds(matchedLogRecords));
        Integer page = request.getPage();
        Integer size = request.getSize();
        List<LogRecord> resultLogRecords = matchedLogRecords;
        if (nonNull(page) && nonNull(size)) {
            resultLogRecords = ListUtils.applyPagination(matchedLogRecords, page, size, null);
        }
        Map<UUID, TestRun> testRunMap = StreamUtils.toIdEntityMap(allFailedTestRuns);
        List<TestCaseLabelResponse> testRunTestCases = testCaseService.getTestCaseLabelsByIds(allFailedTestRuns);
        Map<UUID, TestCaseLabelResponse> testCaseMap = StreamUtils.toIdEntityMap(testRunTestCases);
        List<LogRecordRegexSearchResponse.LogRecordResponse> logRecords = resultLogRecords.stream()
                .map(logRecord -> {
                    TestRun testRun = testRunMap.get(logRecord.getTestRunId());
                    TestCaseLabelResponse testCase = testCaseMap.get(testRun.getTestCaseId());
                    return new LogRecordRegexSearchResponse.LogRecordResponse(testRun, logRecord, testCase);
                })
                .collect(Collectors.toList());
        return new LogRecordRegexSearchResponse(logRecords, matchedLogRecords.size());
    }

    /**
     * Get map of Test Runs with ER Id's as keys.
     *
     * @param executionRequests List of Execution Request Id's.
     * @return Map of Test Runs.
     */
    public Map<UUID, List<TestRun>> getMapTestRunsForExecutionRequests(List<UUID> executionRequests) {
        Map<UUID, List<TestRun>> resultMap = new HashMap<>();
        executionRequests.forEach(uuid -> {
            List<TestRun> testRuns = testRunService.findAllByExecutionRequestId(uuid);
            resultMap.put(uuid, testRuns);
        });
        return resultMap;
    }

    /**
     * Get map of Test Runs (with screenshots) with ER Id's as keys.
     *
     * @param executionRequests List of Execution Request Id's.
     * @return Map of Test Runs.
     */
    public Map<UUID, List<TestRun>> getMapTestRunsWithScreenShotsForExecutionRequests(List<UUID> executionRequests) {
        Map<UUID, List<TestRun>> resultMap = new LinkedHashMap<>();
        executionRequests.forEach(uuid -> {
            List<TestRun> testRuns = testRunService.findTestRunsWithScreenshotsByExecutionRequestId(uuid);
            resultMap.put(uuid, testRuns);
        });
        return resultMap;
    }

    /**
     * Searches test runs by names, statuses, labels, validation labels.
     *
     * @param id              execution request id
     * @param testRunsRequest filter for search
     * @param page            page
     * @param size            size
     * @return Found test runs with validation labels
     */
    public TestRunsByValidationLabelsResponse searchTestRunsByValidationLabels(
            UUID id, TestRunsByValidationLabelsRequest testRunsRequest, Integer page, Integer size) {
        List<TestRunWithValidationLabelsResponse> responses =
                testRunService.findTestRunsByNamesLabelsValidationLabels(id, testRunsRequest);
        Integer totalSize = responses.size();
        Set<String> validationLabels = new HashSet<>();
        responses.forEach(testRunResponse -> validationLabels.addAll(testRunResponse.getValidationLabels()));
        if (nonNull(page) && nonNull(size)) {
            responses = ListUtils.applyPagination(responses, page, size);
        }
        return new TestRunsByValidationLabelsResponse(validationLabels, responses, totalSize);
    }

    /**
     * Returns project ids by execution request ids.
     */
    public Set<UUID> getProjectIdsByExecutionRequestIds(List<UUID> uuids) {
        List<ExecutionRequest> executionRequests = repository.findAllByUuidIn(uuids);
        return executionRequests.stream().map(ExecutionRequest::getProjectId).collect(Collectors.toSet());
    }

    /**
     * Returns project ids by execution request ids.
     */
    public Set<UUID> getProjectIdsByExecutionRequestIds(String uuids) {
        List<UUID> ids = Splitter.on(',').trimResults().splitToList(uuids)
                .stream().map(UUID::fromString).collect(Collectors.toList());
        return getProjectIdsByExecutionRequestIds(ids);
    }

    /**
     * Returns project ids by testScopeId.
     */
    public UUID getProjectIdByTestScopeId(UUID testScopeId) {
        Optional<ExecutionRequest> executionRequest = findByTestScopeUuid(testScopeId).stream().findAny();
        return executionRequest.map(ExecutionRequest::getProjectId).orElse(null);
    }

    public List<ExecutionRequest> findByTestScopeUuid(UUID testScopeUuid) {
        return repository.findAllByTestScopeId(testScopeUuid);
    }

    /**
     * Get failure reasons for execution request.
     */
    public Collection<RootCause> getFailureReasons(UUID executionRequestId) {
        log.info("Get failure reasons for execution request with id '{}'", executionRequestId);
        final List<TestRun> testRuns = testRunService.findAllByExecutionRequestId(executionRequestId);
        final Set<UUID> failureReasonIds = StreamUtils.extractIds(testRuns, TestRun::getRootCauseId);
        log.debug("Found failure reasons: {}", failureReasonIds);

        if (CollectionUtils.isEmpty(failureReasonIds)) {
            return Collections.emptyList();
        }

        return rootCauseService.getByIds(failureReasonIds);
    }

    /**
     * Find all Execution Requests by expired period.
     */
    public List<ExecutionRequest> findExpireExecutionRequest(Timestamp qwe, UUID projectId) {
        List<ExecutionRequest> expired = repository.findAllByArrivedBetweenAndProjectId(qwe, projectId);
        return expired;
    }

    /**
     * Deleted ExecutionRequest.
     */
    public void deleteAllExecutionRequestByExecutionRequestId(List<UUID> executionRequestIds) {
        repository.deleteAllByUuidIn(executionRequestIds);
    }
}
