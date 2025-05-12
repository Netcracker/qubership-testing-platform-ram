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
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.compress.utils.Sets;
import org.modelmapper.ModelMapper;
import org.qubership.atp.ram.RamConstants;
import org.qubership.atp.ram.client.DataSetListFeignClient;
import org.qubership.atp.ram.clients.api.dto.catalogue.JiraIssueDto;
import org.qubership.atp.ram.constants.CacheConstants;
import org.qubership.atp.ram.converter.DtoConvertService;
import org.qubership.atp.ram.dto.request.AnalyzedTestRunRequest;
import org.qubership.atp.ram.dto.request.JiraTicketUpdateRequest;
import org.qubership.atp.ram.dto.request.LabelRequest;
import org.qubership.atp.ram.dto.request.LabelsPathSearchRequest;
import org.qubership.atp.ram.dto.request.StatusUpdateRequest;
import org.qubership.atp.ram.dto.request.TestRunDefectsPropagationRequest;
import org.qubership.atp.ram.dto.request.TestRunsByValidationLabelsRequest;
import org.qubership.atp.ram.dto.request.TestingStatusUpdateRequest;
import org.qubership.atp.ram.dto.request.ValidationLabelFilterRequest;
import org.qubership.atp.ram.dto.response.AnalyzedTestRunResponse;
import org.qubership.atp.ram.dto.response.BaseEntityResponse;
import org.qubership.atp.ram.dto.response.CompareTreeTestRunResponse;
import org.qubership.atp.ram.dto.response.LabelNodeReportResponse;
import org.qubership.atp.ram.dto.response.LabelNodeReportResponse.TestRunNodeResponse;
import org.qubership.atp.ram.dto.response.LogRecordPreviewResponse;
import org.qubership.atp.ram.dto.response.NonGroupedTestRunResponse;
import org.qubership.atp.ram.dto.response.PaginationResponse;
import org.qubership.atp.ram.dto.response.SimpleTestRunResponse;
import org.qubership.atp.ram.dto.response.StatusUpdateResponse;
import org.qubership.atp.ram.dto.response.StatusUpdateResponse.TestRunStatusUpdateResponse;
import org.qubership.atp.ram.dto.response.TestCaseLabelResponse;
import org.qubership.atp.ram.dto.response.TestRunDefectsPropagationResponse;
import org.qubership.atp.ram.dto.response.TestRunResponse;
import org.qubership.atp.ram.dto.response.TestRunTreeResponse;
import org.qubership.atp.ram.dto.response.TestRunWithValidationLabelsResponse;
import org.qubership.atp.ram.entities.treenodes.labelparams.TestingReportLabelParam;
import org.qubership.atp.ram.enums.DefaultRootCauseType;
import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.model.CaseSearchRequest;
import org.qubership.atp.ram.model.DatasetListDataSetsResponse;
import org.qubership.atp.ram.model.LogRecordFilteringRequest;
import org.qubership.atp.ram.model.LogRecordWithChildrenResponse;
import org.qubership.atp.ram.model.datacontext.TestRunsDataContext;
import org.qubership.atp.ram.model.datacontext.TestRunsDataContextLoadOptions;
import org.qubership.atp.ram.models.AnalyzedTestRunSortedColumns;
import org.qubership.atp.ram.models.BrowserInfo;
import org.qubership.atp.ram.models.Comment;
import org.qubership.atp.ram.models.EnrichedTestRun;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.Issue;
import org.qubership.atp.ram.models.JiraTicket;
import org.qubership.atp.ram.models.Label;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.Project;
import org.qubership.atp.ram.models.RamObject;
import org.qubership.atp.ram.models.RootCause;
import org.qubership.atp.ram.models.SetBulkFinalTestRuns;
import org.qubership.atp.ram.models.TestCase;
import org.qubership.atp.ram.models.TestPlan;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.models.TestRunSearchRequest;
import org.qubership.atp.ram.models.TestRunStatistic;
import org.qubership.atp.ram.models.TestRunStatistic.ReportLabelParameterData;
import org.qubership.atp.ram.models.TestRunsCommentSetBulkRequest;
import org.qubership.atp.ram.models.TestRunsFailureReasonSetBulkRequest;
import org.qubership.atp.ram.models.ValidationLabelConfigTemplate;
import org.qubership.atp.ram.models.logrecords.parts.ContextVariable;
import org.qubership.atp.ram.models.logrecords.parts.ValidationTable;
import org.qubership.atp.ram.models.logrecords.parts.ValidationTableLine;
import org.qubership.atp.ram.models.response.LogRecordRatesResponse;
import org.qubership.atp.ram.models.response.TestRunsRatesResponse;
import org.qubership.atp.ram.repositories.ExecutionRequestRepository;
import org.qubership.atp.ram.repositories.RootCauseRepository;
import org.qubership.atp.ram.repositories.TestRunRepository;
import org.qubership.atp.ram.repositories.impl.FieldConstants;
import org.qubership.atp.ram.utils.JsonHelper;
import org.qubership.atp.ram.utils.PatchHelper;
import org.qubership.atp.ram.utils.StreamUtils;
import org.qubership.atp.ram.utils.TimeUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class TestRunService extends CrudService<TestRun> {

    private static final String NOT_ANALYZED = "NOT ANALYZED";
    private static final String START_DATE = "${START_DATE}";
    private static final String END_DATE = "${END_DATE}";
    private static final String VALIDATION_LABEL_N_A = "N/A";

    @Value("${browser.monitoring.link}")
    private String browserMonitoringLinkTemplate;

    @Lazy
    private final MongoTemplate mongoTemplate;

    @Lazy
    private final LogRecordService logRecordService;
    private final TestRunRepository testRunRepository;
    private final RootCauseService rootCauseService;
    private final ProjectsService projectsService;
    private final TestPlansService testPlansService;
    private final ModelMapper modelMapper;
    private final CatalogueService catalogueService;
    private final DataSetListFeignClient dataSetListFeignClient;

    @Lazy
    private final ExecutionRequestRepository executionRequestRepository;
    private final RootCauseRepository rootCauseRepository;
    private final TreeNodeService treeNodeService;
    private final TestCaseService testCaseService;
    private final IssueService issueService;
    private final PatchHelper patchHelper;
    private final LabelsService labelsService;

    /**
     * Get Test Ru by uuid.
     *
     * @param testRunId Test Run identifier.
     * @return found Test Run.
     */
    @Nonnull
    public TestRun getByUuid(UUID testRunId) {
        return get(testRunId);
    }

    public List<TestRun> getShortTestRunsByIds(Collection<UUID> ids) {
        return testRunRepository.findShortTestRunsByUuidIn(ids);
    }

    public List<TestRun> getByIds(Collection<UUID> ids) {
        return testRunRepository.findAllByUuidIn(ids);
    }

    @Override
    protected MongoRepository<TestRun, UUID> repository() {
        return testRunRepository;
    }

    public TestRun getByTestCase(UUID testCaseId) {
        return testRunRepository.findFirstByTestCaseIdOrderByStartDateDesc(testCaseId);
    }

    public List<TestRun> getAllInProgressTestRuns() {
        return testRunRepository
                .findAllByExecutionStatusOrderByStartDateDesc(ExecutionStatuses.IN_PROGRESS);
    }

    public LogRecord getLastInProgressLogRecord(UUID testRunId) {
        return logRecordService.findLastInProgressLogRecordByTestRunId(testRunId);
    }

    public LogRecord getLastInProgressOrcLogRecord(UUID testRunId) {
        return logRecordService.findLastInProgressOrcLogRecordByTestRunId(testRunId);
    }

    public void deleteByUuid(UUID uuid) {
        testRunRepository.deleteByUuid(uuid);
    }

    /**
     * Create test run.
     *
     * @param testRun             creation request
     * @param newProject          project data
     * @param newTestPlan         test plan data
     * @param newExecutionRequest execution request data
     * @return created test run
     * @deprecated use method from TestRunLoggingController
     */
    @Deprecated
    public TestRun create(Project newProject, TestPlan newTestPlan, TestRun testRun,
                          ExecutionRequest newExecutionRequest) {
        Project project = projectsService.findByUuidNameOrCreateNew(newProject);
        TestPlan testPlan = testPlansService.findByUuidNameOrCreateNew(newTestPlan, project);
        ExecutionRequest executionRequest = findOrCreateRequest(project, testPlan, newExecutionRequest);
        return findOrCreateTestRun(testRun, executionRequest);
    }

    /**
     * Patch test run. Null properties, empty maps and collections are ignored.
     *
     * @param testRun contains properties to be patched
     * @return patched test run
     */
    public TestRun patch(TestRun testRun) {
        log.debug("Patching test run with id {}", testRun.getUuid());
        TestRun testRunToPatch = testRunRepository.findByUuid(testRun.getUuid());
        if (testRun.getFinishDate() != null) {
            long duration =
                    TimeUnit.MILLISECONDS.toSeconds(
                            testRun.getFinishDate().getTime() - testRunToPatch.getStartDate().getTime());
            testRun.setDuration(duration);
        }
        patchHelper.partialUpdate(testRun, testRunToPatch, PatchHelper.nullProperties,
                PatchHelper.emptyCollectionsProperties, PatchHelper.emptyMapsPropertiesFilter);
        log.debug("Merge completed, result entity: {} will be stored in DB", testRunToPatch);
        return testRunRepository.save(testRunToPatch);
    }

    /**
     * Find or create ExecutionRequests.
     */
    @Deprecated
    private ExecutionRequest findOrCreateRequest(Project project, TestPlan testPlan, ExecutionRequest request) {
        String name = request.getName();
        log.info("Start of search (or creating - if the Execution Request was not found) with name:\n{}.", name);
        log.trace("Request for ER : {}.", request);
        ExecutionRequest executionRequest = executionRequestRepository.findByUuid(request.getUuid());
        if (nonNull(executionRequest)) {
            log.debug("ER already exist {}.", name);
            return executionRequest;
        }
        executionRequest = request;
        executionRequest.setStartDate(new Timestamp(System.currentTimeMillis()));
        executionRequest.setExecutionStatus(ExecutionStatuses.IN_PROGRESS);
        executionRequest.setProjectId(project.getUuid());
        executionRequest.setTestPlanId(testPlan.getUuid());
        executionRequest.setDuration(0);
        executionRequest.setPassedRate(0);
        executionRequest.setWarningRate(0);
        executionRequest.setFailedRate(0);
        executionRequestRepository.save(executionRequest);
        log.debug("ER: {} was created.", executionRequest.getUuid());
        return executionRequest;
    }

    public List<LogRecord> getAllSectionNotCompoundLogRecords(UUID testRunId) {
        return logRecordService.getAllSectionNotCompoundLogRecordsByTestRunId(testRunId);
    }

    public List<LogRecord> getAllLogRecordsByTestRunId(UUID id) {
        return logRecordService.findAllByTestRunIdOrderByStartDateAsc(id, null);
    }

    public List<LogRecord> getAllFilteredLogRecordsByTestRunId(UUID id, @Nullable LogRecordFilteringRequest filter) {
        return logRecordService.findAllByTestRunIdOrderByStartDateAsc(id, filter);
    }

    public List<LogRecord> getAllLogRecordsUuidByTestRunId(UUID testRunId) {
        return logRecordService.getAllLogRecordsUuidByTestRunId(testRunId);
    }

    public List<LogRecord> getAllFailedLogRecords(UUID testRunUuid) {
        return logRecordService.getAllFailedLogRecordsByTestRunId(testRunUuid);
    }

    public List<LogRecord> getAllFailedLogRecords(Set<UUID> testRunIds) {
        return logRecordService.getAllFailedLogRecordsByTestRunIds(testRunIds);
    }

    public List<LogRecord> getAllTestingStatusLogRecordsByTestRunId(UUID testRunUuid) {
        return logRecordService.getAllTestingStatusLogRecordsByTestRunId(testRunUuid);
    }

    /**
     * Update NotStarted log records testing status.
     *
     * @param testRunUuid testRunId UUID.
     * @param statuses    TestingStatuses.
     * @return List of LogRecords.
     */
    public List<LogRecord> updateLogRecordsTestingStatus(UUID testRunUuid, TestingStatuses statuses) {
        List<LogRecord> allNotStartedLogRecordsByTestRunId =
                logRecordService.getAllNotStartedLogRecordsByTestRunId(testRunUuid);
        if (allNotStartedLogRecordsByTestRunId != null) {
            allNotStartedLogRecordsByTestRunId.forEach(logRecord -> {
                logRecord.setTestingStatus(statuses);
                logRecordService.save(logRecord);
            });
        }
        return allNotStartedLogRecordsByTestRunId;
    }

    /**
     * Getting data for test results (use in atp-catalogue).
     *
     * @param erId for found data
     * @return map with data about test runs for current ER
     */
    public Map<UUID, TestRun> getDataForTestResultByExecutionRequestId(List<UUID> erId) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(new Criteria("executionRequests_id").is(erId)),
                Aggregation.unwind("qaHost"),
                Aggregation.unwind("taHost"),
                Aggregation.group("executionRequests_id", "qaHost", "taHost"),
                Aggregation.project("executionRequests_id", "qaHost", "taHost").andExclude("_id")
        );
        AggregationResults<TestRun> res = mongoTemplate.aggregate(aggregation, "testrun", TestRun.class);
        Map<UUID, TestRun> testRuns = new HashMap<>();
        res.iterator().forEachRemaining(testRun -> testRuns.put(testRun.getExecutionRequestId(), testRun));
        return testRuns;
    }

    public List<LogRecord> getTopLevelLogRecords(UUID uuid, LogRecordFilteringRequest filteringRequest) {
        return logRecordService.findByTestRunIdAndParentUuid(uuid, null, filteringRequest);
    }

    public TestRun save(TestRun testRun) {
        return testRunRepository.save(testRun);
    }

    public void updateStatusesAndFinishDateTestRuns(UUID testRunId, ExecutionStatuses executionStatus,
                                                    TestingStatuses testingStatus, Timestamp finishDate,
                                                    long duration) {
        testRunRepository.updateStatusesAndFinishDateByTestRunId(testRunId, executionStatus, testingStatus, finishDate,
                duration);
    }

    /**
     * Find execution request by specified identifier.
     *
     * @param executionRequestId execution request identifier
     * @return founded execution request
     */
    public List<TestRun> findAllByExecutionRequestId(UUID executionRequestId) {
        List<TestRun> executionRequestTestRuns = testRunRepository.findAllByExecutionRequestId(executionRequestId);
        log.info("Found test run with ids '{}' for execution request with id '{}'",
                StreamUtils.extractIds(executionRequestTestRuns), executionRequestId);

        return executionRequestTestRuns;
    }

    /**
     * Find list of TestRuns by specified identifier.
     *
     * @param executionRequestId execution request identifier
     * @return list of TestRuns
     */
    public List<TestRun> findTestRunsForFdrByExecutionRequestId(UUID executionRequestId) {
        List<TestRun> executionRequestTestRuns =
                testRunRepository.findTestRunsForFdrByExecutionRequestId(executionRequestId);
        log.info("Found test run with ids '{}' for execution request with id '{}'",
                StreamUtils.extractIds(executionRequestTestRuns), executionRequestId);

        return executionRequestTestRuns;
    }

    /**
     * Find set of TestCase id by execution request id.
     *
     * @param executionRequestId execution request identifier
     * @return founded list of TestRuns
     */
    public List<TestRun> findTestCaseIdsByExecutionRequestId(UUID executionRequestId) {
        List<TestRun> executionRequestTestRuns =
                testRunRepository.findTestCaseIdByExecutionRequestId(executionRequestId);
        log.info("Found test run with ids '{}' for execution request with id '{}'",
                StreamUtils.extractIds(executionRequestTestRuns), executionRequestId);

        return executionRequestTestRuns;
    }

    /**
     * Get test run children.
     *
     * @param parentTestRunId parent testrun id
     * @return list of testruns
     */
    public List<TestRun> getTestRunChildren(UUID parentTestRunId) {
        return testRunRepository.findAllByParentTestRunId(parentTestRunId);
    }

    public List<TestRun> findSortedByExecutionRequestId(UUID erId) {
        return testRunRepository.findAllByExecutionRequestIdOrderByStartDateAsc(erId);
    }

    /**
     * Set fdr link in testrun.
     */
    public void setFdrLink(UUID testRunId, String fdrLink) {
        TestRun testRun = getByUuid(testRunId);
        testRun.setFdrLink(fdrLink);
        save(testRun);
    }

    /**
     * Stop {@link TestRun} found by testRunUuid.
     *
     * @return finish message if {@link TestRun} was stop or throw IllegalStateException otherwise
     * @deprecated use method from TestRunLoggingController
     */
    @Deprecated
    public String stopTestRun(JsonObject request) {
        UUID testRunUuid = UUID.fromString(JsonHelper.getStringValue(request, "testRunId"));
        log.debug("Start of stop Test Run {}.", testRunUuid);
        HashSet<String> urlToBrowserOrLogs = JsonHelper
                .getHashSet(request, "urlToBrowserOrLogs", new HashSet<>());

        TestRun testRun = testRunRepository.findByUuid(testRunUuid);
        terminateTestRun(testRun);
        log.trace("Test run stopped, uuid = {}, url to browser log = {}.", testRunUuid, urlToBrowserOrLogs);

        TestingStatuses testingStatus = TestingStatuses
                .findByValue(JsonHelper.getStringValue(request, FieldConstants.TESTING_STATUS));
        testRun.updateTestingStatus(testingStatus);

        Timestamp startDate = testRun.getStartDate();
        Timestamp finishDate = testRun.getFinishDate();
        testRun.setUrlToBrowserOrLogs(getUrlToBrowserLog(startDate, finishDate, urlToBrowserOrLogs));
        testRun.setLogCollectorData(JsonHelper.getStringValue(request, FieldConstants.LOG_COLLECTOR_DATA));
        testRunRepository.save(testRun);
        return testRun.getExecutionStatus().getName();
    }

    /**
     * Find test run by uuid and set rootCauseId for test run.
     *
     * @return update object {@link TestRun}
     */
    public TestRun saveRootCause(UUID uuid, UUID rootCauseId) {
        TestRun testRun = getByUuid(uuid);
        testRun.setRootCauseId(rootCauseId);
        save(testRun);
        return testRun;
    }

    /**
     * Find test runs by uuids and set rootCauseId for test runs.
     *
     * @return update objects {@link TestRun}
     */
    public List<TestRun> saveRootCausesForListOfTestRuns(List<UUID> uuids, UUID rootCauseId) {
        List<TestRun> testRuns = new ArrayList<>();
        uuids.forEach(uuid -> {
            TestRun testRun = getByUuid(uuid);
            testRun.setRootCauseId(rootCauseId);
            save(testRun);
            testRuns.add(testRun);
        });
        return testRuns;
    }

    /**
     * Update rootCauseId for testRuns.
     */
    public void updateFieldRootCauseIdByTestRunsIds(List<UUID> listTestRunIds, UUID rootCauseId) {
        if (rootCauseId != null) {
            listTestRunIds.forEach(testRunId -> {
                Map<String, Object> fieldsToUpdate = new HashMap<>();
                fieldsToUpdate.put(FieldConstants.ROOT_CAUSE_ID, rootCauseId);
                updateAnyFieldsForTestRunsByUuid(testRunId, fieldsToUpdate, TestRun.class);
            });
        }
    }

    /**
     * Update rootCauseId for testRuns.
     */
    public void updateAnyFieldsForTestRunsByUuid(UUID testRunId,
                                                 Map<String, Object> fieldsToUpdate,
                                                 Class<?> entityClass) {
        testRunRepository.updateAnyFieldsRamObjectByIdDocument(testRunId, fieldsToUpdate, entityClass);
    }

    List<LogRecord> getAllMatchesLogRecords(UUID testRunUuid, String searchValue) {
        return logRecordService.getAllMatchesLogRecordsByTestRunId(testRunUuid, searchValue);
    }

    void deleteListTestRuns(List<UUID> testRunsUuidList) {
        log.debug("Start deleting the list of Test Runs: {}", testRunsUuidList);
        testRunRepository.deleteAllByUuidIn(testRunsUuidList);
    }

    /**
     * Find test run for current project.
     * 1. Find ER-s by project ID
     * 2. Find TR-s for found ER-s
     *
     * @param projectUuid for find
     * @return list of {@link TestRun}
     */
    public List<TestRun> getTestRunsForProject(UUID projectUuid) {
        List<UUID> executionRequestsId =
                executionRequestRepository.findUuidByProjectId(projectUuid)
                        .stream().map(ExecutionRequest::getUuid)
                        .collect(Collectors.toList());
        return testRunRepository.findAllByExecutionRequestIdIn(executionRequestsId);
    }

    public long countAllByExecutionRequestIdAndExecutionStatusIn(UUID uuid, List<ExecutionStatuses> executionStatuses) {
        return testRunRepository.countAllByExecutionRequestIdAndExecutionStatusIn(uuid, executionStatuses);
    }

    /**
     * Groups Test Runs by Root Causes names.
     *
     * @param erId Execution Request Id.
     * @return Map with Root Cause name and number of Test Runs.
     */
    public Map<String, Integer> getTestRunsGroupedByRootCauses(UUID erId) {
        Map<String, Integer> rootCausesAndCount = new LinkedHashMap<>();
        List<TestRun> testRuns = testRunRepository.findAllTestRunRootCausesByExecutionRequestId(erId);
        List<RootCause> rootCauses = rootCauseService.getAllRootCauses();

        for (TestRun testRun : testRuns) {
            Optional<RootCause> foundRc = rootCauses.stream()
                    .filter(rootCause -> rootCause.getUuid().equals(testRun.getRootCauseId()))
                    .findFirst();
            String rootCauseName = foundRc.isPresent() ? foundRc.get().getName() : "";

            if (Strings.isNullOrEmpty(rootCauseName)) {
                rootCauseName = NOT_ANALYZED;
            }

            Integer count = rootCausesAndCount.get(rootCauseName);
            rootCausesAndCount.put(rootCauseName, count == null ? 1 : count + 1);
        }
        return rootCausesAndCount;
    }

    /**
     * Return finish date of last TR or 0, if TR is null.
     */
    public long getFinishDateOfLastTestRun(UUID requestUuid) {
        try {
            return testRunRepository
                    .findFinishDateByExecutionRequestIdAndFinishDateIsNotNullOrderByFinishDateDesc(requestUuid)
                    .getFinishDate().getTime();
        } catch (Exception e) {
            log.error("Unable get finish date of TR, ER = " + requestUuid, e);
            return 0;
        }
    }

    /**
     * Returns names of testCases for project.
     */
    public List<TestRun> getTestCasesNamesForProject(UUID projectId) {
        List<UUID> requestsId =
                executionRequestRepository.findUuidByProjectId(projectId)
                        .stream().map(ExecutionRequest::getUuid)
                        .collect(Collectors.toList());
        return testRunRepository.findAllTestCasesNamesByExecutionRequestIdIn(requestsId);
    }

    /**
     * Returns TR by name or create new.
     */
    public TestRun findByNameAndRequestUuidOrCreateNew(TestRun tr) {
        TestRun res;
        if (isNull(tr.getUuid())) {
            res = testRunRepository.findByExecutionRequestIdAndName(tr.getExecutionRequestId(), tr.getName());
        } else {
            res = testRunRepository.findByUuid(tr.getUuid());
        }
        if (isNull(res)) {
            log.debug("Created Test Run: {}.", tr.getUuid());
            testRunRepository.save(tr);
            return tr;
        } else {
            log.debug("Test Run: {} already exist.", res.getUuid());
            return res;
        }
    }

    public List<TestRun> findTestRunsWithFillStatusByRequestId(UUID executionRequestId) {
        return testRunRepository.findAllByExecutionRequestIdAndTestingStatusIsNotNull(executionRequestId);
    }

    /**
     * Return id of the project that owns the test run by test run id.
     */
    public UUID getProjectIdByTestRunId(UUID id) {
        return testRunRepository.findProjectIdByTestRunId(id);
    }

    /**
     * Return id of the project that owns the test run by test run object.
     */
    public UUID getProjectIdByTestRun(TestRun testRun) {
        UUID erId = testRun.getExecutionRequestId();
        return executionRequestRepository.findByUuid(erId).getProjectId();
    }

    /**
     * Return id of the project that owns the test case by test run object.
     */
    public UUID getProjectIdByTestCaseId(UUID testCaseId) {
        return testRunRepository.findProjectIdByTestCaseId(testCaseId);
    }

    /**
     * Return id of the project that owns the test runs.
     */
    public UUID getProjectIdByTestRunIds(Set<UUID> testRuns) {
        UUID testRunId = testRuns.stream().findAny().orElse(null);
        if (isNull(testRunId)) {
            return null;
        }
        return getProjectIdByTestRunId(testRunId);
    }

    /**
     * Find or create TestRun.
     */
    @Deprecated
    private TestRun findOrCreateTestRun(TestRun request, ExecutionRequest executionRequest) {
        log.trace("Request for create Test Run:\n{}", request);
        request.setExecutionRequestId(executionRequest.getUuid());
        request.setExecutionStatus(ExecutionStatuses.NOT_STARTED);
        request.updateTestingStatus(TestingStatuses.NOT_STARTED);
        return findByNameAndRequestUuidOrCreateNew(request);
    }

    /**
     * Update TestRun using request.
     */
    @Deprecated
    private TestRun updateTestRun(TestRun testRun, JsonObject request) {
        testRun.setExecutionStatus(JsonHelper.getExecutionStatus(request, "executionStatus",
                ExecutionStatuses.IN_PROGRESS));
        testRun.updateTestingStatus(JsonHelper.getTestingStatus(request, "testingStatus",
                TestingStatuses.UNKNOWN));
        testRun.setSolutionBuild(JsonHelper.getListString(request, "solutionBuild", testRun.getSolutionBuild()));
        testRun.setTaHost(JsonHelper.getListString(request, "taHost", testRun.getTaHost()));
        testRun.setQaHost(JsonHelper.getListString(request, "qaHost", testRun.getQaHost()));
        testRun.setExecutor(JsonHelper.getStringValue(request, "executor",
                String.valueOf(testRun.getExecutionRequestId())));
        testRun.setDataSetUrl(JsonHelper.getStringValue(request, "dataSetUrl", testRun.getDataSetUrl()));
        testRun.setDataSetListUrl(JsonHelper.getStringValue(request, "dataSetListUrl", testRun.getDataSetListUrl()));
        long finishDate;
        try {
            finishDate = JsonHelper.getLongValue(request, "finishDate");
        } catch (Exception e) {
            log.warn("Can not get the finish date");
            finishDate = System.currentTimeMillis();
        }
        testRun.setFinishDate(new Timestamp(finishDate));
        testRun.setDuration(TimeUtils.getDuration(testRun.getStartDate(), testRun.getFinishDate()));
        testRun.setLogCollectorData(
                JsonHelper.getStringValue(request, "logCollectorData", testRun.getLogCollectorData()));
        save(testRun);
        return testRun;
    }

    @Deprecated
    private HashSet<String> getUrlToBrowserLog(Timestamp startDate, Timestamp finishDate, HashSet<String> urls) {
        if (!urls.isEmpty()) {
            String firstDate = startDate.toString().replace(" ", "T");
            String secondDate = finishDate.toString().replace(" ", "T");

            HashSet<String> newUrls = new HashSet<>();
            for (String url : urls) {
                newUrls.add(url.replace(START_DATE, firstDate).replace(END_DATE, secondDate));
            }
            return newUrls;
        }
        return urls;
    }

    /**
     * Find TestRuns with In Progress status.
     *
     * @param testRunIds set of TR ids.
     * @return ids of TRs with In Progress status.
     */
    public List<UUID> getTestRunsForStoppingOrTerminating(List<UUID> testRunIds) {
        List<UUID> finishedListTestRuns = testRunRepository.findAllByUuidInAndExecutionStatusIn(testRunIds,
                        asList(ExecutionStatuses.IN_PROGRESS, ExecutionStatuses.NOT_STARTED))
                .stream().map(TestRun::getUuid).collect(Collectors.toList());
        log.debug("Test runs for stopping or terminating: {}", finishedListTestRuns);
        return finishedListTestRuns;
    }

    /**
     * Find ExecutionRequests with Suspended status.
     *
     * @param testRunIds set of TR ids.
     * @return ids of TRs with Suspended status.
     */
    public List<UUID> getTestRunsForResuming(List<UUID> testRunIds) {
        return testRunRepository.findAllByUuidInAndExecutionStatusIn(testRunIds,
                        Collections.singletonList(ExecutionStatuses.SUSPENDED))
                .stream().map(TestRun::getUuid).collect(Collectors.toList());
    }

    /**
     * Find "Execution Request's Logs" {@link TestRun} for stopping.
     *
     * @param requestUuid for search testrun
     */
    void stopServiceTestRun(UUID requestUuid) {
        log.debug("Stopping service Test Run for Execution Request: {}", requestUuid);
        TestRun testRunForSearch = new TestRun();
        testRunForSearch.setExecutionRequestId(requestUuid);
        testRunForSearch.setName("Execution Request's Logs");

        TestRun systemTestRun = testRunRepository
                .findByExecutionRequestIdAndName(testRunForSearch.getExecutionRequestId(),
                        testRunForSearch.getName());
        if (isNull(systemTestRun)) {
            log.trace("Cannot find testrun 'Execution Request's Logs' for ER = {}", requestUuid);
            return;
        }

        if (ExecutionStatuses.FINISHED.equals(systemTestRun.getExecutionStatus())) {
            log.trace("System testrun [id = {}] has execution status = {} already", systemTestRun.getUuid(),
                    ExecutionStatuses.FINISHED);
            return;
        }

        systemTestRun.setExecutionStatus(ExecutionStatuses.FINISHED);
        systemTestRun.setFinishDate(new Timestamp(System.currentTimeMillis()));
        save(systemTestRun);
    }

    /**
     * Terminate not finished test runs for current ER.
     *
     * @param executionRequestId for found test runs
     */
    public void updateTestRunsStatusToTerminatedByErId(UUID executionRequestId) {
        log.debug("Terminate Test Runs for execution request {}", executionRequestId);
        List<TestRun> testRuns = testRunRepository.findAllByExecutionRequestIdAndExecutionStatusIn(executionRequestId,
                asList(ExecutionStatuses.NOT_STARTED, ExecutionStatuses.IN_PROGRESS));
        if (testRuns.isEmpty()) {
            log.debug("Test runs don't exist for execution request {}", executionRequestId);
            return;
        }
        testRuns.forEach(testRun -> {
            testRun.setExecutionStatus(ExecutionStatuses.TERMINATED);
            testRun.setFinishDate(new Timestamp(System.currentTimeMillis()));
        });
        testRunRepository.saveAll(testRuns);
    }

    /**
     * Update or create test run.
     *
     * @param request upsert request
     * @return created or updated test run identifier
     * @deprecated use method from TestRunLoggingController
     */
    @Deprecated
    public UUID updateOrCreate(JsonObject request) {
        UUID testRunId = UUID.fromString(JsonHelper.getStringValue(request, "testRunId"));
        String erId = JsonHelper.getStringValue(request, "executionRequestId");
        log.debug("Update Test Run: {} for ER {}.", testRunId, erId);
        TestRun testRun = getByUuid(testRunId);
        log.debug("TestRunService: before update, testing status {} for ID {}", testRun.getTestingStatus(), testRunId);
        testRunId = updateTestRun(testRun, request).getUuid();

        log.debug("TestRunService: after update, testing status {} for ID {}", testRun.getTestingStatus(), testRunId);
        return testRunId;
    }

    List<TestRun> findNotPassedTestRunByErId(UUID erId) {
        return testRunRepository.findByExecutionRequestIdAndTestingStatus(erId, TestingStatuses.PASSED);
    }

    /**
     * Find list of Test Runs with uuid and testing status by Execution Request id.
     *
     * @param executionRequestId TestRun uuid.
     * @return list of Test Runs.
     */
    public List<TestRun> findTestRunsUuidAndTestingStatusByErId(UUID executionRequestId) {
        return testRunRepository.findTestRunsUuidAndTestingStatusByExecutionRequestId(executionRequestId);
    }

    List<TestRun> getAllMatchedTestRunsByRequestId(UUID requestId, String searchValue) {
        return testRunRepository.findAllByExecutionRequestIdAndNameContains(requestId, searchValue);
    }

    /**
     * Found info about current test run and his parent, if exists.
     *
     * @param uuid for found test run
     * @return current test run and his parent
     */
    public TestRunTreeResponse getTestRunByIdWithParent(UUID uuid) {
        TestRun testRun = getByUuid(uuid);
        TestRunTreeResponse response = new TestRunTreeResponse();
        if (!testRun.isGroupedTestRun()) {
            SimpleTestRunResponse simpleTestRunResponse = preparingSimpleTestRun(testRun);
            response.setSimpleTestRun(simpleTestRunResponse);
        }
        return response;
    }

    private SimpleTestRunResponse preparingSimpleTestRun(TestRun testRun) {
        SimpleTestRunResponse simpleTestRunResponse = modelMapper.map(testRun, SimpleTestRunResponse.class);
        simpleTestRunResponse.setTestCase(new BaseEntityResponse(testRun.getTestCaseId(),
                testRun.getTestCaseName()));

        UUID rootCauseId = testRun.getRootCauseId();
        if (nonNull(rootCauseId)) {
            String rootCause = rootCauseService.getRootCauseNameById(rootCauseId);
            simpleTestRunResponse.setRootCause(new BaseEntityResponse(rootCauseId, rootCause));
        }
        simpleTestRunResponse.setBrowserInfos(generateBrowserInfo(testRun));

        Set<UUID> labelIds = testRun.getLabelIds();
        if (!isEmpty(labelIds)) {
            simpleTestRunResponse.setLabels(labelsService.getLabels(labelIds));
        }

        Long allLogRecordsByTestRunCount = logRecordService.countLrsByTestRunsId(Sets.newHashSet(testRun.getUuid()));
        simpleTestRunResponse.setAllLogRecordsCount(
                nonNull(allLogRecordsByTestRunCount) ? allLogRecordsByTestRunCount : 0L);
        Long passedLogRecordsByTestRunCount =
                logRecordService.countAllPassedLrByTestRunIds(Sets.newHashSet(testRun.getUuid()));
        simpleTestRunResponse.setPassedLogRecordsCount(nonNull(passedLogRecordsByTestRunCount)
                ? passedLogRecordsByTestRunCount : 0L);

        return simpleTestRunResponse;
    }

    private List<BrowserInfo> generateBrowserInfo(TestRun testRun) {
        if (isNull(testRun.getBrowserNames())) {
            return null;
        } else if (Strings.isNullOrEmpty(browserMonitoringLinkTemplate) || isNull(testRun.getStartDate())) {
            log.warn("Can't generate browser monitoring links for testrun '{}' due to empty template"
                            + " or testrun StartDate. Only browser names will be set. Template={}, StartDate={},"
                            + " BrowserNames={},", testRun.getUuid(), browserMonitoringLinkTemplate,
                    testRun.getStartDate(), testRun.getBrowserNames());
            return testRun.getBrowserNames().stream().map((name) -> new BrowserInfo(name, null))
                    .collect(Collectors.toList());
        } else {
            long startInMillis = testRun.getStartDate().getTime();
            long finishInMillis = isNull(testRun.getFinishDate())
                    ? System.currentTimeMillis() : testRun.getFinishDate().getTime();
            String templateWithTimestamps = browserMonitoringLinkTemplate
                    .replace(RamConstants.FROM_TIMESTAMP, Long.toString(startInMillis))
                    .replace(RamConstants.TO_TIMESTAMP, Long.toString(finishInMillis));
            return testRun.getBrowserNames().stream().map((name) -> new BrowserInfo(name, templateWithTimestamps
                    .replace(RamConstants.BROWSER_POD, name))).collect(Collectors.toList());
        }
    }

    private List<TestRun> getPreviousTestRunsWithNames(TestRun testRun) {
        List<TestRun> testRuns = new ArrayList<>();
        testRuns.add(testRun);
        while (nonNull(testRun.getParentTestRunId())) {
            testRun = testRunRepository.findNameParentIdByUuid(testRun.getParentTestRunId());
            testRuns.add(testRun);
        }
        return testRuns;
    }

    /**
     * Get test run log record previews.
     *
     * @param testRunId        test run id
     * @param filteringRequest Filtering request
     * @return response
     */
    public List<LogRecordPreviewResponse> getAllLogRecordPreviews(UUID testRunId,
                                                                  LogRecordFilteringRequest filteringRequest) {
        return logRecordService.findLogRecordsWithPreviewByTestRunIdOrderByStartDateAsc(testRunId, filteringRequest)
                .stream()
                .filter(logRecord -> !Strings.isNullOrEmpty(logRecord.getPreview()))
                .map(logRecord -> new LogRecordPreviewResponse(testRunId, logRecord.getUuid(),
                        logRecord.getPreview(), logRecord.getTestingStatus()))
                .collect(Collectors.toList());
    }

    /**
     * Get test run test case.
     *
     * @param testRunId test run id
     * @return test case info
     */
    public BaseEntityResponse getTestRunTestCase(UUID testRunId) {
        TestRun testRun = this.get(testRunId);
        return new BaseEntityResponse(testRun.getTestCaseId(), testRun.getTestCaseName());
    }

    /**
     * Get non-grouped test runs by execution request.
     *
     * @param executionRequestId execution request id
     * @return list of test runs
     */
    public List<NonGroupedTestRunResponse> getNonGroupedTestRuns(UUID executionRequestId) {
        return testRunRepository
                .findAllByExecutionRequestIdAndIsGroupedTestRun(executionRequestId, Boolean.FALSE)
                .stream()
                .map(testRun -> new NonGroupedTestRunResponse(testRun.getUuid(), testRun.getName(),
                        testRun.getTestingStatus()))
                .collect(Collectors.toList());
    }

    /**
     * Stop set of {@link TestRun} found by uuids.
     *
     * @return {@link List} of {@link UUID} of stopped test runs
     */
    public List<UUID> stopTestRuns(List<UUID> testRunUuids) {
        log.debug("Start of stop Test Runs {}.", testRunUuids);
        List<TestRun> testRuns = testRunRepository.findAllByUuidIn(testRunUuids);
        testRuns.forEach(this::terminateTestRun);
        testRunRepository.saveAll(testRuns);
        List<UUID> terminatedUuids = testRuns.stream().map(RamObject::getUuid).collect(Collectors.toList());
        log.debug("Finished terminating Test Runs {}", terminatedUuids);
        return terminatedUuids;
    }

    /**
     * Stop set of {@link TestRun} found by uuids.
     *
     * @return {@link List} of {@link UUID} of stopped test runs
     */
    public List<UUID> finishTestRuns(List<UUID> testRunUuids, boolean isDelayed) {
        log.debug("Test runs {} are going to be finished. Delayed: {}", testRunUuids, isDelayed);
        List<TestRun> testRuns = testRunRepository.findAllByUuidIn(testRunUuids);
        testRuns.forEach(tr -> finishTestRun(tr, isDelayed));
        testRunRepository.saveAll(testRuns);
        List<UUID> finishedUuids = testRuns
                .stream()
                .map(RamObject::getUuid)
                .collect(Collectors.toList());
        log.debug("Finished Test Runs {}", finishedUuids);
        return finishedUuids;
    }

    /**
     * Get enriched test runs by specified execution request id.
     *
     * @param executionRequestId execution request id
     * @return found enriched test runs
     */
    public List<EnrichedTestRun> getEnrichedTestRunsByExecutionRequestId(UUID executionRequestId) {
        List<EnrichedTestRun> executionRequestTestRuns =
                testRunRepository.findAllEnrichedTestRunsByExecutionRequestId(executionRequestId);
        log.debug("executionRequestTestRuns: {}", executionRequestTestRuns);
        ExecutionRequest executionRequest = executionRequestRepository.findByUuid(executionRequestId);
        if (!executionRequest.isVirtual()) {
            List<TestRun> testRuns = new ArrayList<>(executionRequestTestRuns);
            List<TestCaseLabelResponse> testCases = catalogueService.getTestCaseLabelsByIds(testRuns);
            log.debug("Found test run with ids '{}' for execution request with id '{}'",
                    StreamUtils.extractIds(executionRequestTestRuns), executionRequestId);
            executionRequestTestRuns.forEach(enrichedTestRun ->
                    enrichedTestRun.setLabels(getLabelsByTestCaseId(testCases, enrichedTestRun.getTestCaseId())));
        }
        return executionRequestTestRuns;
    }

    private List<Label> getLabelsByTestCaseId(List<TestCaseLabelResponse> testCases, UUID testCaseId) {
        //there are some test data in DB, need to remove
        if (testCaseId == null) {
            return new ArrayList<>();
        }
        Optional<TestCaseLabelResponse> testCaseLabel = testCases.stream()
                .filter(testCase -> testCaseId.equals(testCase.getUuid()))
                .findFirst();
        if (testCaseLabel.isPresent()) {
            return testCaseLabel.get().getLabels();
        } else {
            return new ArrayList<>();
        }
    }

    private void terminateTestRun(TestRun testRun) {
        testRun.setFinishDate(new Timestamp(System.currentTimeMillis()));
        if (!isFinalStatus(testRun.getExecutionStatus())) {
            testRun.setExecutionStatus(ExecutionStatuses.TERMINATED);
        }
        Timestamp startDate = testRun.getStartDate();
        Timestamp finishDate = testRun.getFinishDate();
        long duration = TimeUtils.getDuration(startDate, finishDate);
        testRun.setDuration(duration);
    }

    /**
     * Create or update test run statistic report label param.
     *
     * @param testRunId test run id
     * @param data      param data
     */
    public void upsertTestRunStatisticReportLabelParam(UUID testRunId,
                                                       String paramName,
                                                       ReportLabelParameterData data) {
        TestRun testRun = getByUuid(testRunId);

        TestRunStatistic testRunStatistic = testRun.getStatistic();

        if (isNull(testRunStatistic)) {
            testRunStatistic = new TestRunStatistic();
            testRun.setStatistic(testRunStatistic);
        }

        Map<String, ReportLabelParameterData> reportLabelParams = testRunStatistic.getReportLabelParams();
        reportLabelParams.put(paramName, data);

        this.save(testRun);
    }

    public List<TestRun> findAllByExecutionRequestIdAndNameNotIs(UUID execReqId, String excludeName) {
        return testRunRepository.findAllByExecutionRequestIdAndNameNot(execReqId, excludeName);
    }

    public List<TestRun> findAllRatesByUuidIn(Collection<UUID> testRunsIds) {
        return testRunRepository.findAllRatesByUuidIn(testRunsIds);
    }

    /**
     * Get test runs for analyze with filtration.
     *
     * @param startIndex start index
     * @param endIndex   end index
     * @param columnType column for sorting
     * @param sortType   sorting type
     * @param filter     filter
     * @return filtered test runs for analyze
     */
    public AnalyzedTestRunResponse getAnalyzedTestRuns(int startIndex, int endIndex,
                                                       AnalyzedTestRunSortedColumns columnType,
                                                       Sort.Direction sortType, TestRunSearchRequest filter) {
        final UUID executionRequestId = filter.getExecutionRequestId();

        if (!isEmpty(filter.getLabelNames()) || !isEmpty(filter.getLabelNameContains())) {
            List<TestRun> executionRequestTestRuns =
                    findTestCaseIdsByExecutionRequestId(executionRequestId);
            Set<UUID> filteredLabelIds = collectFilteredLabelIdsFromTestCases(executionRequestTestRuns, filter);
            if (isEmpty(filteredLabelIds)) {
                // labels filter is not empty and not found any labels from execution request's test cases
                AnalyzedTestRunResponse response = new AnalyzedTestRunResponse();
                response.setTestRuns(new ArrayList<>());
                response.setTotalNumberOfEntities(0);
                return response;
            }
            filter.setLabelIds(filteredLabelIds);
        }
        PaginationResponse<TestRun> paginationResponse = testRunRepository.findAllByFilter(startIndex, endIndex,
                columnType, sortType, filter);
        List<TestRun> testRuns = paginationResponse.getEntities();
        long totalNumberOfEntities = paginationResponse.getTotalCount();

        List<TestCaseLabelResponse> testCaseLabelResponses = catalogueService.getTestCaseLabelsByIds(testRuns);

        Map<UUID, TestCaseLabelResponse> testCaseLabelResponseMap = StreamUtils.toIdEntityMap(testCaseLabelResponses);

        Map<UUID, RootCause> rootCauseMap = StreamUtils.toIdEntityMap(rootCauseRepository.findAll());

        Set<UUID> testRunIds = StreamUtils.extractIds(testRuns);
        Map<UUID, Set<JiraTicket>> testRunsDefectsMap = getTestRunsIssuesMap(executionRequestId, testRunIds);

        List<AnalyzedTestRunResponse.AnalyzedTestRun> responseTestRuns = testRuns.stream()
                .map(testRun -> {
                    TestCaseLabelResponse testCaseLabelResponse = testCaseLabelResponseMap.get(testRun.getTestCaseId());

                    return toAnalyzedTestRunResponse(testRun, testCaseLabelResponse, rootCauseMap, testRunsDefectsMap);
                })
                .collect(Collectors.toList());

        AnalyzedTestRunResponse response = new AnalyzedTestRunResponse();
        response.setTestRuns(responseTestRuns);
        response.setTotalNumberOfEntities((int) totalNumberOfEntities);
        return response;
    }

    /**
     * Collects label ids from test cases by filters.
     *
     * @param testRuns test runs by execution request id
     * @param filter   filter with label names or label parts
     * @return set of label ids after filtering
     */
    private Set<UUID> collectFilteredLabelIdsFromTestCases(List<TestRun> testRuns, TestRunSearchRequest filter) {
        List<TestCaseLabelResponse> testCaseLabelResponses = catalogueService.getTestCaseLabelsByIds(testRuns);
        List<String> filterLabelNames = filter.getLabelNames();
        List<String> filterLabelParts = filter.getLabelNameContains();
        Set<UUID> filteredLabelIds = new HashSet<>();

        testCaseLabelResponses.forEach(testCaseLabelResponse -> {
            List<Label> labels = testCaseLabelResponse.getLabels();
            if (!isEmpty(filterLabelNames)) {
                filteredLabelIds.addAll(labels.stream()
                        .filter(label -> isLabelInFilteredLabelNames(label.getName(), filterLabelNames))
                        .map(RamObject::getUuid)
                        .collect(Collectors.toSet()));
            }
            if (!isEmpty(filterLabelParts)) {
                filteredLabelIds.addAll(labels.stream()
                        .filter(label -> isLabelPartInLabelName(label.getName(), filterLabelParts))
                        .map(RamObject::getUuid)
                        .collect(Collectors.toSet()));
            }
        });

        return filteredLabelIds;
    }

    private boolean isLabelInFilteredLabelNames(String labelName, List<String> filterLabelNames) {
        return filterLabelNames.stream().anyMatch(filteredLabel -> filteredLabel.equals(labelName));
    }

    private boolean isLabelPartInLabelName(String labelName, List<String> filterLabelParts) {
        return filterLabelParts.stream().anyMatch(labelName::contains);
    }

    public PaginationResponse<TestRun> search(TestRunSearchRequest filter, int page, int size) {
        return testRunRepository.findAllByFilter(page, size, null, null, filter);
    }

    /**
     * Search an return enriched test runs by filter request.
     *
     * @param page   page number
     * @param size   page size
     * @param filter search request
     * @return founded test runs
     */
    public PaginationResponse<EnrichedTestRun> searchEnriched(TestRunSearchRequest filter, int page, int size) {
        PaginationResponse<TestRun> result = testRunRepository.findAllByFilter(page, size, null, null, filter);
        List<TestRun> testRuns = result.getEntities();

        List<EnrichedTestRun> enrichedTestRuns = StreamUtils.mapToClazz(testRuns, EnrichedTestRun.class);

        Set<UUID> rootCauseIds = StreamUtils.extractIds(enrichedTestRuns, EnrichedTestRun::getRootCauseId);

        List<RootCause> testRunRootCauses = rootCauseService.getByIds(rootCauseIds);
        Map<UUID, RootCause> rootCauseMap = StreamUtils.toIdEntityMap(testRunRootCauses);

        enrichedTestRuns.forEach(enrichedTestRun -> {
            final UUID rootCauseId = enrichedTestRun.getRootCauseId();
            final RootCause failureReason = rootCauseMap.get(rootCauseId);
            enrichedTestRun.setFailureReason(failureReason);
        });

        return new PaginationResponse<>(enrichedTestRuns, result.getTotalCount());
    }

    private AnalyzedTestRunResponse.AnalyzedTestRun toAnalyzedTestRunResponse(TestRun testRun,
                                                                              TestCaseLabelResponse labelResponses,
                                                                              Map<UUID, RootCause> rootCausesMap,
                                                                              Map<UUID, Set<JiraTicket>> defectsMap) {
        final AnalyzedTestRunResponse.AnalyzedTestRun response =
                modelMapper.map(testRun, AnalyzedTestRunResponse.AnalyzedTestRun.class);
        response.setTestRunJiraTicket(testRun.getJiraTicket());

        final RootCause rootCause = rootCausesMap.get(testRun.getRootCauseId());
        if (nonNull(rootCause)) {
            response.setFailureReasonId(rootCause.getUuid());
        }

        if (nonNull(labelResponses)) {
            response.setProjectId(labelResponses.getProjectId());
            response.setTestPlanId(labelResponses.getTestPlanId());
            response.setScenarioId(labelResponses.getScenarioId());
            response.setLabels(labelResponses.getLabels());
            response.setJiraTicket(labelResponses.getJiraTicket());
        }

        final Set<JiraTicket> defects = defectsMap.get(testRun.getUuid());
        response.setDefects(defects);

        return response;
    }

    /**
     * Update test run from analyze tab.
     *
     * @param testRunId       test run id
     * @param analyzedTestRun info to update
     */
    public void updateAnalyzedTestRun(UUID testRunId, AnalyzedTestRunRequest analyzedTestRun) {
        TestRun testRun = get(testRunId);

        if (analyzedTestRun.getTestingStatus() != null) {
            updateStatusAndPropagateTestCase(testRun, analyzedTestRun.getTestingStatus());
        }

        if (analyzedTestRun.getJiraTicket() != null) {
            testRun.setJiraTicket(analyzedTestRun.getJiraTicket());
        }

        testRunRepository.save(testRun);
    }

    /**
     * Get classifier of testing statuses.
     *
     * @return classifier of testing statuses
     */
    public Map<TestingStatuses, String> getTestStatuses() {
        return Arrays.stream(TestingStatuses.values())
                .collect(Collectors.toMap(testingStatus -> testingStatus,
                        TestingStatuses::getName));
    }

    /**
     * Get classifier of failure reasons.
     *
     * @return classifier of failure reasons
     */
    public Map<DefaultRootCauseType, String> getFailureReasons() {
        return Arrays.stream(DefaultRootCauseType.values())
                .collect(Collectors.toMap(failureReason -> failureReason,
                        DefaultRootCauseType::getName));
    }

    public Long countAllByExecutionRequestId(UUID executionRequestId) {
        return testRunRepository.countAllByExecutionRequestId(executionRequestId);
    }

    public List<TestRun> findTestRunForExecutionSummaryByExecutionRequestId(UUID erId) {
        return testRunRepository.findTestRunForExecutionSummaryByExecutionRequestId(erId);
    }

    /**
     * Get test runs data context for further processing performance.
     *
     * @param testRuns input test runs
     * @param options  options for getting info
     * @return result map
     */
    public TestRunsDataContext getTestRunsDataContext(List<TestRun> testRuns,
                                                      TestRunsDataContextLoadOptions options, boolean isVirtual) {
        final TestRunsDataContext.TestRunsDataContextBuilder builder = TestRunsDataContext.builder();

        if (options.isIncludeRunMap()) {
            builder.testRunsMap(StreamUtils.toIdEntityMap(testRuns));
        } else {
            builder.testRunsMap(new HashMap<>());
        }

        if (options.isIncludeRunTestCasesMap() && !isVirtual) {
            builder.testRunTestCasesMap(this.getTestRunTestCasesLabelsMap(testRuns));
        } else {
            builder.testRunTestCasesMap(new HashMap<>());
        }

        if (options.isIncludeRunValidationLogRecordsMap() && options.isIncludeRunFailedLogRecordsMap()) {
            Set<UUID> testRunIds = StreamUtils.extractIds(testRuns);

            Supplier<Stream<LogRecord>> testRunLogRecords =
                    () -> logRecordService.findLogRecordsWithValidationParamsAndFailureByTestRunIds(testRunIds);

            List<LogRecord> logRecordsWithValidationParams =
                    filterLogRecordsWithValidationParams(testRunLogRecords.get());
            Map<UUID, List<LogRecord>> logRecordsWithValidationParamsToTestRunMap =
                    StreamUtils.toMapWithListEntitiesValues(logRecordsWithValidationParams, LogRecord::getTestRunId);
            builder.testRunValidationLogRecordsMap(logRecordsWithValidationParamsToTestRunMap);

            List<LogRecord> failedLogRecordsWithMetaInfo = filterFailedLogRecordsWithMetaInfo(testRunLogRecords.get());
            Map<UUID, List<LogRecord>> failedLogRecordsWithMetaInfoToTestRunMap =
                    StreamUtils.toMapWithListEntitiesValues(failedLogRecordsWithMetaInfo, LogRecord::getTestRunId);
            builder.testRunFailedLogRecordsMap(failedLogRecordsWithMetaInfoToTestRunMap);

        } else if (options.isIncludeRunValidationLogRecordsMap()) {
            builder.testRunValidationLogRecordsMap(this.getTestRunValidationLogRecordsMap(testRuns));
            builder.testRunFailedLogRecordsMap(new HashMap<>());
        } else if (options.isIncludeRunFailedLogRecordsMap()) {
            builder.testRunFailedLogRecordsMap(this.getTestRunFailedLogRecordsMap(testRuns));
            builder.testRunValidationLogRecordsMap(new HashMap<>());
        } else {
            builder.testRunValidationLogRecordsMap(new HashMap<>());
            builder.testRunFailedLogRecordsMap(new HashMap<>());
        }

        if (options.isIncludeTestRunDslNamesMap()) {
            builder.testRunDslNamesMap(this.getTestRunDslNamesMap(testRuns));
        } else {
            builder.testRunDslNamesMap(new HashMap<>());
        }

        if (options.isIncludeRootCausesMap()) {
            builder.rootCausesMap(this.getRootCauseNamesMap());
        } else {
            builder.rootCausesMap(new HashMap<>());
        }

        return builder.build();
    }

    /**
     * Get test run validation log records map for further processing performance.
     *
     * @param testRuns input test runs
     * @return result map
     */
    public Map<UUID, List<LogRecord>> getTestRunValidationLogRecordsMap(Collection<TestRun> testRuns) {
        Set<UUID> testRunIds = StreamUtils.extractIds(testRuns);
        List<LogRecord> testRunLogRecords =
                logRecordService.findLogRecordsWithValidationParamsByTestRunIds(testRunIds);

        return StreamUtils.toEntityListMap(testRunLogRecords, LogRecord::getTestRunId);
    }

    /**
     * Get test run failed log records map for further processing performance.
     *
     * @param testRuns input test runs
     * @return result map
     */
    public Map<UUID, List<LogRecord>> getTestRunFailedLogRecordsMap(Collection<TestRun> testRuns) {
        Set<UUID> testRunIds = StreamUtils.extractIds(testRuns);
        List<LogRecord> testRunLogRecords = logRecordService.findFailedLogRecordsWithMetaInfoByTestRunIds(testRunIds);

        return StreamUtils.toEntityListMap(testRunLogRecords, LogRecord::getTestRunId);
    }

    /**
     * Filter log records with validation params.
     *
     * @param logRecords input log records
     * @return log records with validation params
     */
    public List<LogRecord> filterLogRecordsWithValidationParams(Stream<LogRecord> logRecords) {
        return logRecords
                .filter(logRecord -> {
                    final Set<String> validationLabels = logRecord.getValidationLabels();
                    final ValidationTable validationTable = logRecord.getValidationTable();

                    if (nonNull(validationTable)) {
                        final List<ValidationTableLine> steps = validationTable.getSteps();
                        if (!isEmpty(steps)) {
                            final List<String> validationTableLabels = steps.stream()
                                    .filter(step -> !isEmpty(step.getValidationLabels()))
                                    .flatMap(step -> step.getValidationLabels().stream())
                                    .collect(Collectors.toList());

                            return !isEmpty(validationLabels) || !isEmpty(validationTableLabels);
                        }
                    }

                    return !isEmpty(validationLabels);
                })
                .collect(Collectors.toList());
    }

    /**
     * Filter failed log records with metainfo.
     *
     * @param logRecords input log records
     * @return failed log records with meta info
     */
    public List<LogRecord> filterFailedLogRecordsWithMetaInfo(Stream<LogRecord> logRecords) {
        return logRecords
                .filter(logRecord -> nonNull(logRecord.getMetaInfo())
                        && TestingStatuses.FAILED.equals(logRecord.getTestingStatus()))
                .collect(Collectors.toList());
    }

    /**
     * Get test run test cases map for further processing performance.
     *
     * @param testRuns input test runs
     * @return result map
     */
    public Map<UUID, TestCaseLabelResponse> getTestRunTestCasesLabelsMap(List<TestRun> testRuns) {
        List<TestCaseLabelResponse> testCases = catalogueService.getTestCaseLabelsByIds(testRuns);

        return StreamUtils.toIdEntityMap(testCases);
    }

    /**
     * Get root cause names map for further processing performance.
     *
     * @return result map
     */
    public Map<UUID, String> getRootCauseNamesMap() {
        return rootCauseService.getAll()
                .stream()
                .collect(Collectors.toMap(RootCause::getUuid, RootCause::getName));
    }

    /**
     * Get root cause names map for current Ids.
     *
     * @return result map
     */
    public Map<UUID, String> getRootCauseNamesMap(Set<UUID> rootCausesId) {
        return rootCauseService.getByIds(rootCausesId)
                .stream()
                .collect(Collectors.toMap(RootCause::getUuid, RootCause::getName));
    }

    /**
     * Get test run data set lists map for further processing performance.
     *
     * @param testRuns input test runs
     * @return result map
     */
    public Map<String, String> getTestRunDslNamesMap(List<TestRun> testRuns) {
        List<UUID> dslIds = StreamUtils.extractFields(testRuns, TestRun::getDataSetListUrl)
                .stream()
                .map(UUID::fromString)
                .collect(Collectors.toList());

        List<DatasetListDataSetsResponse> dslResponse = new DtoConvertService(new ModelMapper())
                .convertList(dataSetListFeignClient
                        .getDataSetsWithNameAndDataSetList(dslIds).getBody(), DatasetListDataSetsResponse.class);

        return dslResponse.stream()
                .collect(Collectors.toMap(response -> response.getDataSetId().toString(),
                        DatasetListDataSetsResponse::getDataSetName));
    }

    /**
     * Get test run issues map.
     *
     * @param executionRequestId execution request identifier
     * @param testRunIds         test run identifiers
     * @return result map
     */
    public Map<UUID, Set<JiraTicket>> getTestRunsIssuesMap(UUID executionRequestId, Collection<UUID> testRunIds) {
        List<Issue> testRunIssues = issueService.getAllIssuesByTestRunIds(executionRequestId, testRunIds);
        Map<UUID, Set<JiraTicket>> issuesMap = testRunIds.stream()
                .collect(Collectors.toMap(Function.identity(), uuid -> new HashSet<>()));

        testRunIssues.stream()
                .filter(issue -> !isEmpty(issue.getJiraDefects()) && !isEmpty(issue.getFailedTestRunIds()))
                .forEach(issue -> issue.getFailedTestRunIds().forEach(testRunId -> {
                    issuesMap.merge(testRunId, new HashSet<>(issue.getJiraDefects()), (old, newOne) -> {
                        old.addAll(newOne);
                        return old;
                    });
                }));

        return issuesMap;
    }

    /**
     * List of TR node with list of failed LR-s.
     *
     * @param testRuns test runs
     * @param template validation template
     * @return list of TR node with list of failed LR-s
     */
    public List<TestRunNodeResponse> getTestRunNodeWithFailedLogRecords(List<TestRun> testRuns,
                                                                        ValidationLabelConfigTemplate template,
                                                                        TestRunsDataContext context) {
        UUID executionRequestId = context.getExecutionRequestId();
        Set<UUID> testRunIds = StreamUtils.extractIds(testRuns, TestRun::getUuid);
        List<TestCaseLabelResponse> testCases = catalogueService.getTestCaseLabelsByIds(testRuns);
        Map<UUID, TestCaseLabelResponse> testCasesMap =
                StreamUtils.toKeyEntityMap(testCases, TestCaseLabelResponse::getUuid);
        Map<UUID, Set<JiraTicket>> issuesMap = getTestRunsIssuesMap(executionRequestId, testRunIds);

        List<TestRunNodeResponse> testRunNodes = testRuns.stream()
                .map(testRun -> {
                    final UUID testRunId = testRun.getUuid();

                    TestRunNodeResponse treeNode = modelMapper.map(testRun, TestRunNodeResponse.class);
                    treeNode.setFailureReason(context.getRootCausesMap().get(testRun.getRootCauseId()));

                    List<LogRecord> failedLogRecords = context.getTestRunFailedLogRecordsMap().get(testRunId);
                    if (nonNull(failedLogRecords) && !failedLogRecords.isEmpty()) {
                        LogRecord firstFailedStep = getFirstFailedStep(failedLogRecords, failedLogRecords.get(0));
                        treeNode.setFailedStep(
                                Collections.singletonList(modelMapper.map(firstFailedStep,
                                        LabelNodeReportResponse.FailedLogRecordNodeResponse.class))
                        );
                    }
                    TestCaseLabelResponse testCase = testCasesMap.get(testRun.getTestCaseId());
                    if (nonNull(testCase)) {
                        treeNode.setJiraTicket(testCase.getJiraTicket());
                        treeNode.setLabels(testCase.getLabels());
                    }
                    Set<JiraTicket> issues = issuesMap.getOrDefault(testRunId, new HashSet<>());
                    Set<String> issueUrls = StreamUtils.extractFields(issues, JiraTicket::getUrl);
                    treeNode.setIssues(issueUrls);
                    treeNode.setComment(testRun.getComment());
                    String dataSetName = context.getTestRunDslNamesMap().get(testRun.getDataSetUrl());
                    treeNode.setDataSetName(dataSetName);
                    List<TestingReportLabelParam> validationLabelParams = treeNodeService.getTestRunValidationLabels(
                            testRunId, template, context.getTestRunValidationLogRecordsMap());

                    treeNode.setLabelParams(validationLabelParams);

                    return treeNode;
                })
                .collect(Collectors.toList());

        return testRunNodes;
    }

    private LogRecord getFirstFailedStep(List<LogRecord> logRecords, LogRecord firstLogRecord) {
        Optional<LogRecord> optionalLogRecord = logRecords.stream()
                .filter(record -> firstLogRecord.getUuid().equals(record.getParentRecordId()))
                .findFirst();
        return optionalLogRecord.isPresent() ? getFirstFailedStep(logRecords, optionalLogRecord.get()) : firstLogRecord;
    }

    /**
     * Get first failed step for test run.
     *
     * @param testRunId test run identifier
     * @return first failed log record
     */
    public LogRecord getFirstFailedStep(UUID testRunId) {
        log.debug("Get first failed log record for test run with id '{}'", testRunId);
        List<LogRecord> failedLogRecords = logRecordService.getAllFailedLogRecordsByTestRunId(testRunId);
        List<LogRecord> failedRootLogRecords = StreamUtils.filterList(failedLogRecords,
                logRecord -> logRecord.getParentRecordId() == null,
                Comparator.comparingLong(LogRecord::getCreatedDateStamp));

        return getFirstFailedStep(failedLogRecords, failedRootLogRecords.get(0));
    }

    /**
     * Get all test runs by executionRequestIds.
     *
     * @param executionRequestIds upsert request.
     * @return created or updated test run identifier
     */
    public List<CompareTreeTestRunResponse> compareByExecutionRequestIds(List<UUID> executionRequestIds) {
        return testRunRepository.compareByExecutionRequestIds(executionRequestIds);
    }

    /**
     * Get all test runs by executionRequestIds.
     *
     * @param executionRequestIds upsert request.
     * @return created or updated test run identifier
     */
    public List<BaseEntityResponse> getTestRunsNotInExecutionRequestCompareTable(List<UUID> executionRequestIds) {
        return testRunRepository.getTestRunsNotInExecutionRequestCompareTable(executionRequestIds);
    }

    /**
     * Find Test Runs by specified Execution Request Id and part of Test Run name.
     *
     * @param executionRequestId execution request identifier
     * @param searchValue        part of test run name
     * @return list of Test Runs
     */
    public List<TestRun> findByExecutionRequestIdAndName(UUID executionRequestId, String searchValue) {
        List<TestRun> executionRequestTestRuns =
                testRunRepository.findAllByExecutionRequestIdAndNameContains(executionRequestId, searchValue);
        log.info("Found test run with ids '{}' for execution request with id '{}'",
                StreamUtils.extractIds(executionRequestTestRuns), executionRequestId);

        return executionRequestTestRuns;
    }

    /**
     * Get status update for test runs and their log records.
     *
     * @param request status update request
     * @return status update response
     */
    public StatusUpdateResponse getStatusUpdate(StatusUpdateRequest request) {
        Date lastLoaded = request.getLastLoaded();
        if (isNull(lastLoaded)) {
            lastLoaded = new Date();
        }

        List<UUID> testRunIds = request.getTestRunsIds();
        log.info("Get status update for test runs '{}' with last update date after '{}'", testRunIds, lastLoaded);

        List<TestRun> testRuns = testRunRepository.findShortTestRunsByUuidIn(testRunIds);
        log.debug("Founded test runs: {}", StreamUtils.extractIds(testRuns));

        List<LogRecord> logRecords = logRecordService.findAllByLastUpdatedAfterAndTestRunIdIn(lastLoaded, testRunIds);
        log.debug("Founded log records: {}", StreamUtils.extractIds(logRecords));

        Map<UUID, List<LogRecord>> testRunToLogRecordMap = logRecords.stream()
                .collect(Collectors.groupingBy(LogRecord::getTestRunId));

        List<TestRunStatusUpdateResponse> testRunStatusUpdateResponses = testRuns.stream()
                .map(testRun -> new TestRunStatusUpdateResponse(testRun, testRunToLogRecordMap.get(testRun.getUuid())))
                .collect(Collectors.toList());
        log.debug("Test run status update responses: {}", testRunStatusUpdateResponses);

        Date currentLastLoadedDate = new Date();
        log.debug("Current last loaded date: {}", currentLastLoadedDate);

        return new StatusUpdateResponse(currentLastLoadedDate, testRunStatusUpdateResponses);
    }

    /**
     * Upd testing status test run.
     *
     * @param testRunId the test run id
     * @return the test run
     */
    public TestRun updTestingStatus(UUID testRunId, boolean isReplaceTestRunStatus) {
        log.info("start updTestingStatus(testRunId: {}, isReplaceTestRunStatus: {})", testRunId,
                isReplaceTestRunStatus);
        List<LogRecord> logRecords = logRecordService.getAllSectionNotCompoundLogRecordsByTestRunId(testRunId);
        log.debug("logRecords size = {}", logRecords.size());
        TestRun testRun = getByUuid(testRunId);
        TestingStatuses finalTestingStatus = TestingStatuses.UNKNOWN;
        for (LogRecord logRecord : logRecords) {
            if (logRecord.getParentRecordId() != null) {
                continue;
            }
            finalTestingStatus =
                    TestingStatuses.compareAndGetPriority(logRecord.getTestingStatus(), finalTestingStatus);
        }
        if (!isReplaceTestRunStatus) {
            finalTestingStatus = TestingStatuses.compareAndGetPriority(finalTestingStatus, testRun.getTestingStatus());
        }
        log.debug("finalTestingStatus  = {}", finalTestingStatus);
        testRun.setTestingStatus(finalTestingStatus);

        testRun = save(testRun);
        log.debug("testRun = {}", testRun);

        return testRun;
    }

    /**
     * Revert testing status for log record and correct test run status as well.
     *
     * @param logRecordId the log record id
     * @return the log record
     */
    public LogRecord revertTestingStatusForLogRecord(UUID logRecordId) {
        LogRecord logRecord = logRecordService.revertTestingStatusForLogRecord(logRecordId);
        log.debug("revertTestingStatusForLogRecord: update testing status for {}", logRecordId);
        updTestingStatus(logRecord.getTestRunId(), false);
        return logRecord;
    }

    /**
     * Get list of Test Run id's by Execution Request id.
     *
     * @param executionRequestId Execution Request id.
     * @return list of Test Run id's.
     */
    public List<UUID> findTestRunsUuidByExecutionRequestId(UUID executionRequestId) {
        return testRunRepository.findTestRunsUuidByExecutionRequestId(executionRequestId)
                .stream()
                .map(TestRun::getUuid)
                .collect(Collectors.toList());
    }

    /**
     * Return test run with not all fields.
     *
     * @param testRunId for find test run
     * @return found test run
     */
    public TestRun getTestRunForNodeTree(UUID testRunId) {
        return testRunRepository.findTestRunForTreeNodeByUuid(testRunId);
    }

    /**
     * Return TR with field 'executionRequestId' only.
     *
     * @param testRunId for find TR
     * @return found test run
     */
    public TestRun findTestRunExecReqIdByUuid(UUID testRunId) {
        return testRunRepository.findTestRunExecReqIdByUuid(testRunId);
    }

    /**
     * Update test runs with jira tickets keys.
     *
     * @param updateRequests - jira ticket update request
     */
    public void updateTestRunsWithJiraTickets(List<JiraTicketUpdateRequest> updateRequests) {
        List<UUID> testRunIds = updateRequests.stream()
                .map(JiraTicketUpdateRequest::getTestRunId)
                .distinct().collect(Collectors.toList());
        Map<UUID, String> runIdToJiraKeyMap = updateRequests.stream()
                .collect(Collectors.toMap(JiraTicketUpdateRequest::getTestRunId,
                        JiraTicketUpdateRequest::getJiraTicket));
        List<TestRun> testRuns = testRunRepository.findAllByUuidIn(testRunIds);
        testRuns.forEach(testRun -> testRun.setJiraTicket(runIdToJiraKeyMap.get(testRun.getUuid())));
        testRunRepository.saveAll(testRuns);
    }

    /**
     * Return list of LR with fields: validationLabels, validationTable, testingStatus.
     *
     * @param testRunId for search of log records
     * @return list of log records for current test run
     */
    public List<LogRecord> findLogRecordsWithValidationParamsAndStatusByTrId(UUID testRunId) {
        return logRecordService.findLogRecordsWithValidationParamsAndStatusByTrId(testRunId);
    }

    /**
     * Get test run validation labels.
     *
     * @param testRunId test run identifier
     * @return label params list
     */
    public List<TestingReportLabelParam> getTestRunValidationLabels(UUID testRunId) {
        log.debug("Get validation labels map for test run '{}'", testRunId);
        List<LogRecord> logRecords = getAllLogRecordsByTestRunId(testRunId);
        Map<String, TestingStatuses> validationLabelMap = new HashMap<>();

        BiFunction<TestingStatuses, TestingStatuses, TestingStatuses> statusMergeFunc = (oldStatus, newStatus) -> {
            if (TestingStatuses.FAILED.equals(newStatus) || TestingStatuses.FAILED.equals(oldStatus)) {
                return TestingStatuses.FAILED;
            }

            return newStatus;
        };

        logRecords.forEach(logRecord -> {
            final Set<String> logRecordValidationLabels = logRecord.getValidationLabels();
            if (!isEmpty(logRecordValidationLabels)) {
                logRecordValidationLabels.forEach(label ->
                        validationLabelMap.merge(label, logRecord.getTestingStatus(), statusMergeFunc));
            }

            final ValidationTable validationTable = logRecord.getValidationTable();
            if (nonNull(validationTable)) {
                final List<ValidationTableLine> validationSteps = validationTable.getSteps();
                if (!isEmpty(validationSteps)) {
                    validationSteps.stream()
                            .filter(step -> !isEmpty(step.getValidationLabels()))
                            .forEach(step -> step.getValidationLabels().forEach(label -> {
                                        validationLabelMap.merge(label, step.getStatus(), statusMergeFunc);
                                    })
                            );
                }
            }
        });

        List<TestingReportLabelParam> validationLabelsMap = validationLabelMap.entrySet()
                .stream()
                .map(entry -> new TestingReportLabelParam(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        log.debug("Result map: {}", validationLabelsMap);

        return validationLabelsMap;
    }

    /**
     * Unset root cause for linked test runs.
     *
     * @param rootCauseId root cause identifier
     */
    public void unsetRootCauseForLinkedTestRuns(UUID rootCauseId) {
        log.debug("Unset root cause '{}' for linked test runs", rootCauseId);
        List<TestRun> updTestRuns = testRunRepository.findAllByRootCauseId(rootCauseId)
                .stream()
                .peek(testRun -> testRun.setRootCauseId(null))
                .collect(Collectors.toList());

        testRunRepository.saveAll(updTestRuns);
    }

    /**
     * Get TR-s with field 'uuid'.
     *
     * @param executionRequestId for search
     * @param testingStatuses    for search
     * @return list of TR-s
     */
    public List<TestRun> getTestRunsIdByExecutionRequestIdAndTestingStatus(UUID executionRequestId,
                                                                           TestingStatuses testingStatuses) {
        return testRunRepository.findTestRunsIdByExecutionRequestIdAndTestingStatus(executionRequestId,
                testingStatuses);
    }

    /**
     * Get TR-s with field 'uuid'.
     *
     * @param executionRequestId for search
     * @param testingStatuses    for search
     * @return list of TR-s
     */
    public List<TestRun> getTestRunsIdByExecutionRequestIdAndTestingStatuses(UUID executionRequestId,
                                                                             List<TestingStatuses> testingStatuses) {
        return testRunRepository.findTestRunsByExecutionRequestIdAndTestingStatusIn(executionRequestId,
                testingStatuses);
    }

    /**
     * Get TR-s with fields: 'uuid' and 'execution request id'.
     *
     * @param testRunIds      for search
     * @param testingStatuses for search
     * @return list of TR-s
     */
    public List<TestRun> getTestRunsUuidTestingStatusErIdByUuidIn(List<UUID> testRunIds,
                                                                  TestingStatuses testingStatuses) {
        return testRunRepository.findTestRunsUuidErIdByTestingStatusAndUuidIn(testingStatuses, testRunIds);
    }

    /**
     * Get TR-s with fields: 'uuid' and 'execution request id'.
     *
     * @param testRunIds      for search
     * @param testingStatuses for search
     * @return list of TR-s
     */
    public List<TestRun> getTestRunsUuidErIdByTestingStatusesAndUuidIn(List<UUID> testRunIds,
                                                                       List<TestingStatuses> testingStatuses) {
        return testRunRepository.findTestRunsUuidErIdByTestingStatusInAndUuidIn(testingStatuses, testRunIds);
    }

    /**
     * Set failure reason to several test runs.
     *
     * @param request bulk request
     * @return updated test runs
     */
    public List<TestRun> setFailureReasonToTestRuns(TestRunsFailureReasonSetBulkRequest request) {
        final Set<UUID> testRunIds = request.getTestRunIds();
        final UUID failureReasonId = request.getFailureReasonId();
        log.info("Set failure reason '{}' to test runs: {}", failureReasonId, testRunIds);

        List<TestRun> testRuns = testRunRepository.findAllByUuidIn(testRunIds);

        testRuns.forEach(testRun -> testRun.setRootCauseId(failureReasonId));

        return testRunRepository.saveAll(testRuns);
    }

    /**
     * Bulk update test runs with testing status hard.
     *
     * @param testingStatusUpdateRequests - test run testing status update request
     */
    public void updateTestRunsTestingStatus(List<TestingStatusUpdateRequest> testingStatusUpdateRequests) {
        Map<UUID, String> testingStatusesUpdMap = testingStatusUpdateRequests.stream()
                .collect(Collectors.toMap(TestingStatusUpdateRequest::getTestRunId,
                        TestingStatusUpdateRequest::getTestingStatus));
        List<TestRun> testRuns = testRunRepository.findAllByUuidIn(new ArrayList<>(testingStatusesUpdMap.keySet()));

        for (TestRun testRun : testRuns) {
            TestingStatuses status = TestingStatuses.findByValue(testingStatusesUpdMap.get(testRun.getUuid()));
            testRun.setTestingStatus(status);
        }
        testRunRepository.saveAll(testRuns);
    }

    /**
     * Change testing status of Test run without compare and propagate to test case if test run is last for the
     * test case.
     *
     * @param uuid            of TR
     * @param testingStatuses is new testing status
     * @return updated {@link TestRun}
     */
    public TestRun updTestingStatusHard(UUID uuid,
                                        TestingStatuses testingStatuses) {
        TestRun testRun = getByUuid(uuid);
        log.info("Status update for Test Run [{}]. Old status [{}]. New status [{}].", uuid, testRun.getTestingStatus(),
                testingStatuses);
        updateStatusAndPropagateTestCase(testRun, testingStatuses);
        return save(testRun);
    }

    /**
     * Change browser names of Test run.
     *
     * @param testRun         testRun
     * @param testingStatuses is new browser names
     * @param browserNames    is new browser names
     * @return updated {@link TestRun}
     */
    public TestRun updTestingStatusHardAndBrowserNames(TestRun testRun, TestingStatuses testingStatuses,
                                                       List<String> browserNames) {
        updateStatusAndPropagateTestCase(testRun, testingStatuses);
        testRun.setBrowserNames(browserNames);
        return save(testRun);
    }

    private void updateStatusAndPropagateTestCase(TestRun testRun, TestingStatuses testingStatuses) {
        testRun.setTestingStatus(testingStatuses);
        TestRun lastRun = getByTestCase(testRun.getTestCaseId());
        if (testRun.getUuid().equals(lastRun.getUuid())) {
            testCaseService.updateCaseStatuses(Collections.singletonList(testRun));
        }
    }

    /**
     * Get TR-s with first failed LR.
     *
     * @param executionRequestId ER id
     * @return TR-s with failed LT
     */
    public List<TestRunsRatesResponse> getTestRunsRatesWithFailedLr(UUID executionRequestId) {
        List<TestRunsRatesResponse> response = testRunRepository
                .findTestRunsRatesResponseByExecutionRequestId(executionRequestId);
        Map<UUID, String> rootCauseMap = getRootCauseNamesMap(StreamUtils.extractIds(response,
                TestRunsRatesResponse::getRootCauseId));

        Map<UUID, TestCase> testCaseMap = getTestRunTestCasesMap(StreamUtils.extractIds(response,
                TestRunsRatesResponse::getTestCaseId));

        response.forEach(testRun -> {
            testRun.setLinkToTestRunJira(testRun.getJiraTicket());
            testRun.setEndDate(testRun.getFinishDate());

            String rootCauseName = rootCauseMap.get(testRun.getRootCauseId());
            if (nonNull(rootCauseName)) {
                testRun.setFailureReason(rootCauseName);
            }

            TestCase testCase = testCaseMap.get(testRun.getTestCaseId());
            if (nonNull(testCase)) {
                testRun.setLinkToTestRunJira(testCase.getJiraTicket());
                testRun.setTestScenarioId(testCase.getTestScenarioUuid());
            }
            testRun.setFirstFailedLogrecord(getFailedLogRecord(testRun.getUuid()));
        });
        return response;
    }

    /**
     * Get failed LR response (parent info + children info).
     *
     * @param testRunId for find TR
     * @return response
     */
    private LogRecordRatesResponse getFailedLogRecord(UUID testRunId) {
        LogRecordWithChildrenResponse parentLr =
                logRecordService.getLogRecordWithChildrenByTrIdAndStatus(testRunId, TestingStatuses.FAILED);
        if (parentLr == null) {
            return new LogRecordRatesResponse();
        }
        parentLr.setMessage(parentLr.getChildren().getMessage());
        return parentLr;
    }

    private Map<UUID, TestCase> getTestRunTestCasesMap(Set<UUID> testRunsId) {
        CaseSearchRequest caseSearchRequest = new CaseSearchRequest(testRunsId);
        List<TestCase> testCases;
        try {
            testCases = catalogueService.getTestCases(caseSearchRequest);
        } catch (Exception e) {
            log.error("Cannot collect test cases from catalog.", e);
            testCases = new ArrayList<>();
        }
        return StreamUtils.toIdEntityMap(testCases, TestCase::getUuid);
    }

    public List<TestRun> findAllByUuidInAndExecutionStatusIn(List<UUID> testRunIds, List<ExecutionStatuses> list) {
        return testRunRepository.findAllByUuidInAndExecutionStatusIn(testRunIds, list);
    }

    /**
     * Finish test run: set finished status if final status not already set,
     * calculates finish date and duration.
     *
     * @param testRun   - test run.
     * @param isDelayed - if set to true, finishDate and duration are not updated.
     */
    public void finishTestRun(TestRun testRun, boolean isDelayed) {
        if (!isFinalStatus(testRun.getExecutionStatus())) {
            testRun.setExecutionStatus(ExecutionStatuses.FINISHED);
        }
        log.trace("Test Run {} execution status is set to {}", testRun.getUuid(),
                testRun.getExecutionStatus());
        if (!isDelayed) {
            testRun.setFinishDate(new Timestamp(System.currentTimeMillis()));
            testRun.setDuration(TimeUtils.getDuration(testRun.getStartDate(), testRun.getFinishDate()));
            log.trace("Finish date {} and duration {} are set for Test Run {}",
                    testRun.getFinishDate(),
                    testRun.getDuration(),
                    testRun.getUuid());
        }
    }

    private boolean isFinalStatus(ExecutionStatuses executionStatus) {
        return ExecutionStatuses.FINISHED == executionStatus
                || ExecutionStatuses.TERMINATED == executionStatus
                || ExecutionStatuses.TERMINATED_BY_TIMEOUT == executionStatus;
    }

    /**
     * Bulk set comments to test runs.
     *
     * @param request input data request
     */
    public void setCommentToTestRuns(TestRunsCommentSetBulkRequest request) {
        final List<UUID> testRunIds = request.getTestRunIds();
        final Comment comment = request.getComment();
        final List<TestRun> testRuns = getByIds(testRunIds);

        if (!isEmpty(testRuns)) {
            testRuns.forEach(testRun -> {
                testRun.setComment(comment);
            });
            testRunRepository.saveAll(testRuns);
        }
    }

    /**
     * Bulk set Test Runs as Final.
     */
    public void setFinalTestRuns(SetBulkFinalTestRuns request) {
        final List<UUID> testRunIds = request.getTestRunIds();
        final List<TestRun> updateTestRunsList = getByIds(testRunIds);
        final UUID executionRequestId = request.getExecutionRequestId();

        List<UUID> testCasesList = new ArrayList<>();
        if (!isEmpty(updateTestRunsList)) {
            updateTestRunsList.forEach(testRun -> {
                testRun.setFinalTestRun(true);
                testCasesList.add(testRun.getTestCaseId());
            });
        }

        List<ExecutionRequest> targetExecutionRequests = new ArrayList<>();
        ExecutionRequest executionRequest = executionRequestRepository.findByUuid(executionRequestId);
        UUID initialExecutionRequestId = executionRequest.getInitialExecutionRequestId();
        if (nonNull(initialExecutionRequestId)) {
            List<ExecutionRequest> rerunExecutionRequests = executionRequestRepository
                    .findAllByInitialExecutionRequestId(initialExecutionRequestId);
            ExecutionRequest initialExecutionRequest = executionRequestRepository
                    .findByUuid(initialExecutionRequestId);
            targetExecutionRequests.addAll(rerunExecutionRequests);
            targetExecutionRequests.add(initialExecutionRequest);
            targetExecutionRequests.remove(executionRequest);
        } else {
            List<ExecutionRequest> rerunExecutionRequests = executionRequestRepository
                    .findAllByInitialExecutionRequestId(executionRequestId);
            targetExecutionRequests.addAll(rerunExecutionRequests);
        }
        if (!isEmpty(targetExecutionRequests)) {
            targetExecutionRequests.forEach(execRequest -> {
                List<TestRun> intermediateTestRunsList =
                        testRunRepository.findAllByExecutionRequestId(execRequest.getUuid())
                                .stream().filter(testRun -> testRun.isFinalTestRun()
                                        && testCasesList.contains(testRun.getTestCaseId()))
                                .collect(Collectors.toList());
                if (!isEmpty(intermediateTestRunsList)) {
                    intermediateTestRunsList.forEach(testRun -> {
                        testRun.setFinalTestRun(false);
                        updateTestRunsList.add(testRun);
                    });
                }
            });
        }

        testRunRepository.saveAll(updateTestRunsList);
    }

    /**
     * Get count of LR-s for current ER.
     *
     * @param executionRequestId for found TR-s
     * @return count of LR-s
     */
    public Long getCountLrsForCurrentEr(UUID executionRequestId) {
        List<TestRun> testRuns = testRunRepository.findTestRunsIdByExecutionRequestId(executionRequestId);
        Set<UUID> testRunIds = StreamUtils.extractIds(testRuns);
        return logRecordService.countLrsByTestRunsId(testRunIds);
    }

    public List<TestRun> findTestRunsWithScreenshotsByExecutionRequestId(UUID executionRequestId) {
        return testRunRepository.findTestRunsByExecutionRequestId(executionRequestId);
    }

    /**
     * Searches test runs filtered by names, statuses, labels, validation labels.
     *
     * @param filter filter with labels, test run names, validation labels and test run statuses
     * @return list of test runs info
     */
    @Cacheable(value = CacheConstants.TEST_RUNS_INFO_CACHE)
    public List<TestRunWithValidationLabelsResponse> findTestRunsByNamesLabelsValidationLabels(
            UUID executionRequestId,
            TestRunsByValidationLabelsRequest filter) {

        List<UUID> labelIds = new ArrayList<>();
        if (!isEmpty(filter.getLabelsPath())) {
            labelIds = filter.getLabelsPath().stream()
                    .map(LabelRequest::getId)
                    .collect(Collectors.toList());
        }
        log.debug("Get test runs by execution request {} and labels {}", executionRequestId, filter.getLabelsPath());
        List<TestRun> testRuns =
                testRunRepository.findTestRunsByExecutionRequestIdAndNamesAndLabelIds(executionRequestId,
                        filter.getTestRunNames(),
                        labelIds);
        List<UUID> testRunIds = StreamUtils.extractIdsToList(testRuns);
        log.debug("Get log records for test runs {}", testRunIds);
        List<LogRecord> logRecords = logRecordService.getAllLogRecordsByTestRunIds(testRunIds);

        Map<UUID, Set<String>> testRunIdAndValidationLabelsMap = new HashMap<>();
        for (TestRun testRun : testRuns) {
            List<LogRecord> testRunLogRecords = logRecords.stream()
                    .filter(logRecord -> testRun.getUuid().equals(logRecord.getTestRunId()))
                    .collect(Collectors.toList());
            Set<String> validationLabels = new HashSet<>();
            for (LogRecord logRecord : testRunLogRecords) {
                if (!isEmpty(logRecord.getValidationLabels())) {
                    validationLabels.addAll(logRecord.getValidationLabels());
                }
            }
            testRunIdAndValidationLabelsMap.put(testRun.getUuid(), validationLabels);
        }

        List<TestRunWithValidationLabelsResponse> result = new ArrayList<>();
        if (!isEmpty(filter.getValidationLabelFilters())) {
            testRuns.forEach(testRun -> {
                if (testRunIdAndValidationLabelsMap.containsKey(testRun.getUuid())) {
                    Set<String> testRunValidationLabels = testRunIdAndValidationLabelsMap.get(testRun.getUuid());
                    if (isTestRunMatchesValidationLabelsFilter(testRun, testRunValidationLabels,
                            filter.getValidationLabelFilters())) {
                        result.add(new TestRunWithValidationLabelsResponse(testRun, testRunValidationLabels));
                    }
                }
            });
        } else {
            testRuns.forEach(testRun -> {
                if (testRunIdAndValidationLabelsMap.containsKey(testRun.getUuid())) {
                    Set<String> testRunValidationLabels = testRunIdAndValidationLabelsMap.get(testRun.getUuid());
                    result.add(new TestRunWithValidationLabelsResponse(testRun, testRunValidationLabels));
                } else {
                    result.add(new TestRunWithValidationLabelsResponse(testRun, new HashSet<>()));
                }
            });
        }

        return result;
    }

    private boolean isTestRunMatchesValidationLabelsFilter(
            TestRun testRun,
            Set<String> testRunValidationLabels,
            List<ValidationLabelFilterRequest> validationLabelFilters) {

        for (ValidationLabelFilterRequest validationLabelFilter : validationLabelFilters) {
            if (!validationLabelFilter.getStatuses().contains(VALIDATION_LABEL_N_A)) {
                if (!testRunValidationLabels.contains(validationLabelFilter.getName())) {
                    return false;
                }
                if (!validationLabelFilter.getStatuses().contains(testRun.getTestingStatus().getName().toUpperCase())) {
                    return false;
                }
            }
            if (testRunValidationLabels.contains(validationLabelFilter.getName())
                    && !validationLabelFilter.getStatuses().contains(
                    testRun.getTestingStatus().getName().toUpperCase())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Searches test runs by execution request id and labels.
     *
     * @param searchRequest search request
     * @return test runs with ids and names
     */
    public List<TestRunResponse> labelsPathSearch(LabelsPathSearchRequest searchRequest) {
        List<UUID> labelIds = searchRequest.getLabelsPath().stream()
                .map(LabelRequest::getId)
                .collect(Collectors.toList());
        List<TestRun> testRuns = testRunRepository.findTestRunsIdNameByExecutionRequestIdAndLabelIds(
                searchRequest.getExecutionRequestId(), labelIds);
        return testRuns.stream()
                .map(testRun -> new TestRunResponse(testRun.getUuid(), testRun.getName()))
                .collect(Collectors.toList());
    }

    /**
     * Gets all context variables from logRecords by testRunId.
     *
     * @param testRunId testRunId
     * @return list of context variables
     */
    public List<ContextVariable> getAllContextVariables(UUID testRunId) {
        List<LogRecord> logRecords = logRecordService.getAllLogRecordsUuidByTestRunId(testRunId);
        if (isEmpty(logRecords)) {
            return Collections.emptyList();
        }
        return logRecordService.getContextVariablesByIds(logRecords.stream().map(RamObject::getUuid)
                .filter(Objects::nonNull).collect(Collectors.toList()));
    }

    /**
     * Propagate defects to comments of test runs.
     *
     * @param request request
     * @return response
     */
    public TestRunDefectsPropagationResponse propagateDefectsToComments(TestRunDefectsPropagationRequest request) {
        final UUID executionRequestId = request.getExecutionRequestId();
        final UUID testPlanId = executionRequestRepository.findByUuid(executionRequestId).getTestPlanId();
        Set<UUID> testRunIds = request.getTestRunIds();

        List<TestRun> testRuns;
        if (isEmpty(testRunIds)) {
            testRuns = findAllByExecutionRequestId(executionRequestId);
            testRunIds = StreamUtils.extractIds(testRuns);
        } else {
            testRuns = getByIds(testRunIds);
        }

        final TestRunDefectsPropagationResponse response = new TestRunDefectsPropagationResponse();

        final Map<UUID, Set<JiraTicket>> testRunsIssuesMap = getTestRunsIssuesMap(executionRequestId, testRunIds);
        final Map<String, JiraIssueDto> testRunsJiraIssuesMap = getTestRunJiraIssuesMap(testRunsIssuesMap, testPlanId);
        testRuns.forEach(testRun -> {
            try {
                final Set<JiraTicket> issues = testRunsIssuesMap.get(testRun.getUuid());
                if (isEmpty(issues)) {
                    return;
                }

                final Set<String> issueKeys = issues.stream()
                        .map(JiraTicket::getKey)
                        .collect(Collectors.toSet());

                Comment comment = testRun.getComment();

                if (comment == null) {
                    comment = new Comment();
                    testRun.setComment(comment);
                }

                // update comment text summary
                final String issueSummaries = getIssueSummary(issueKeys, testRunsJiraIssuesMap,
                        issue -> issue.getKey() + " " + issue.getFields().getSummary());

                String text = comment.getText();
                text = (text != null ? text + "\n" : "") + issueSummaries;
                comment.setText(text);

                // update comment html summary
                final String issueHtmlSummaries = getIssueSummary(issueKeys, testRunsJiraIssuesMap,
                        issue -> getIssueHtmlLink(issue) + " " + issue.getFields().getSummary());

                String html = comment.getHtml();
                html = (html != null ? html + "\n" : "") + issueHtmlSummaries;
                comment.setHtml(html);

                save(testRun);
                response.addSuccess(testRun);
            } catch (Exception e) {
                log.error("Error while updating comment for Test Run '{}'", testRun.getUuid(), e);
                response.addFailed(testRun);
            }
        });

        return response;
    }

    private Map<String, JiraIssueDto> getTestRunJiraIssuesMap(Map<UUID, Set<JiraTicket>> testRunsIssuesMap,
                                                              UUID testPlanId) {
        final Set<String> allIssueKeys = testRunsIssuesMap.values()
                .stream()
                .flatMap(Collection::stream)
                .map(JiraTicket::getKey)
                .collect(Collectors.toSet());

        final Set<String> fields = Sets.newHashSet("summary");

        final List<JiraIssueDto> jiraIssues = catalogueService.searchIssues(testPlanId, allIssueKeys, fields);

        return jiraIssues.stream()
                .collect(Collectors.toMap(JiraIssueDto::getKey, Function.identity()));
    }

    private String getIssueSummary(Set<String> issueKeys, Map<String, JiraIssueDto> jiraIssuesMap,
                                   Function<JiraIssueDto, String> func) {
        return issueKeys.stream()
                .map(jiraIssuesMap::get)
                .filter(Objects::nonNull)
                .map(func)
                .collect(Collectors.joining("\n"));
    }

    private String getIssueHtmlLink(JiraIssueDto issue) {
        final String self = issue.getSelf();
        final String host = self.substring(0, self.indexOf("/rest"));
        final String fullUrl = host + "/browse/" + issue.getKey();
        final String htmlLinkPattern = "<a href=\"%s\" target=\"_blank\">%s</a>";

        return String.format(htmlLinkPattern, fullUrl, issue.getKey());
    }
}
