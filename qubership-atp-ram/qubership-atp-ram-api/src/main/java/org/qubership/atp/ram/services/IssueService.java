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

import static java.util.Objects.nonNull;
import static org.qubership.atp.ram.utils.StreamUtils.extractIds;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.modelmapper.ModelMapper;
import org.qubership.atp.common.lock.LockManager;
import org.qubership.atp.common.utils.regex.TimeoutRegexCharSequence;
import org.qubership.atp.common.utils.regex.TimeoutRegexException;
import org.qubership.atp.ram.dto.response.FailPatternResponse;
import org.qubership.atp.ram.dto.response.IssueResponse;
import org.qubership.atp.ram.dto.response.IssueResponsesModel;
import org.qubership.atp.ram.dto.response.IssueTestRunResponse;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.exceptions.topissues.RamTopIssuesCalculationException;
import org.qubership.atp.ram.model.IssueDto;
import org.qubership.atp.ram.model.LogRecordWithFailPatternsDto;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.FailPattern;
import org.qubership.atp.ram.models.Issue;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.RamObject;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.pojo.IssueFilteringParams;
import org.qubership.atp.ram.repositories.CustomExecutionRequestRepository;
import org.qubership.atp.ram.repositories.CustomIssueRepository;
import org.qubership.atp.ram.repositories.ExecutionRequestRepository;
import org.qubership.atp.ram.repositories.IssueRepository;
import org.qubership.atp.ram.utils.StreamUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Strings;
import com.google.common.collect.Iterators;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class IssueService extends CrudService<Issue> {

    @Value("${atp.ram.regexp.timeout.sec}")
    private int regexpTimeout;

    @Value("${atp.logrecord.step.for.recalculating.topissues}")
    private int logRecordStep;

    private final IssueRepository repository;
    private final CustomIssueRepository customIssueRepository;
    private final TestRunService testRunService;
    private final FailPatternService failPatternService;
    private final LogRecordService logRecordService;
    private final ExecutionRequestDetailsService executionRequestDetailsService;
    private final ModelMapper modelMapper;
    private final ExecutionRequestRepository executionRequestRepository;
    private final CustomExecutionRequestRepository customExecutionRequestRepository;

    private final LockManager lockManager;

    /**
     * IssueService constructor.
     */
    public IssueService(IssueRepository repository, CustomIssueRepository customIssueRepository,
                        @Lazy TestRunService testRunService, FailPatternService failPatternService,
                        LogRecordService logRecordService,
                        ExecutionRequestDetailsService executionRequestDetailsService,
                        ModelMapper modelMapper, ExecutionRequestRepository executionRequestRepository,
                        CustomExecutionRequestRepository customExecutionRequestRepository,
                        LockManager lockManager) {
        this.repository = repository;
        this.customIssueRepository = customIssueRepository;
        this.testRunService = testRunService;
        this.failPatternService = failPatternService;
        this.logRecordService = logRecordService;
        this.executionRequestDetailsService = executionRequestDetailsService;
        this.modelMapper = modelMapper;
        this.executionRequestRepository = executionRequestRepository;
        this.customExecutionRequestRepository = customExecutionRequestRepository;
        this.lockManager = lockManager;
    }

    @Value("${atp.ram.services.issues_creating.lock.duration.sec:300}")
    private Integer lockDurationForCreatingIssuesSec;

    public Issue create(Issue issue) {
        return repository.save(issue);
    }

    public Issue update(Issue issue) {
        return repository.save(issue);
    }

    /**
     * Get all issues present in test cases of a particular execution request with filtration and pagination.
     *
     * @return issue ids
     */
    private IssueResponsesModel getAllIssuesByParameters(IssueFilteringParams issueFilteringParams, String columnType,
                                                         String sortType, int startIndex, int endIndex) {
        return getIssueResponsesModel(startIndex, endIndex, columnType, sortType, issueFilteringParams);
    }

    public List<Issue> getAllIssuesByTestRunIds(UUID executionRequestId, Collection<UUID> testRunIds) {
        return repository.findByExecutionRequestIdAndFailedTestRunIdsIn(executionRequestId, testRunIds);
    }

    public IssueResponsesModel getAllIssuesByExecutionRequestId(UUID id) {
        List<IssueResponse> issues = getResponses(repository.findByExecutionRequestId(id));
        return new IssueResponsesModel((int) repository.countByExecutionRequestId(id), issues);
    }

    /**
     * Get all issues present in test cases of a particular execution request.
     *
     * @return issues
     */
    public IssueResponsesModel getAllIssuesByExecutionRequestId(
            UUID executionRequestId,
            int startIndex,
            int endIndex,
            String columnType,
            String sortType) {

        IssueFilteringParams filteringParams = new IssueFilteringParams();
        filteringParams.setExecutionRequestId(executionRequestId);

        return getAllIssuesByParameters(
                filteringParams,
                columnType, sortType,
                startIndex, endIndex);
    }

    /**
     * Get all issues present in test cases of a particular execution request.
     *
     * @return issues
     */
    public IssueResponsesModel getAllIssuesByExecutionRequestId(IssueFilteringParams issueFilteringParams,
                                                                int startIndex, int endIndex,
                                                                String columnType, String sortType) {
        return getAllIssuesByParameters(issueFilteringParams, columnType, sortType, startIndex, endIndex);
    }

    /**
     * Get all issues present in a particular log record.
     *
     * @return issues
     */
    public IssueResponsesModel getAllIssuesByLogRecordId(
            IssueFilteringParams issueFilteringParams,
            int startIndex, int endIndex,
            String columnType, String sortType) {
        log.debug("Finding issues in a Log Record {} from {} to {}, Execution Request ids [{}]",
                issueFilteringParams.getLogRecordIds().get(0),
                startIndex, endIndex, issueFilteringParams.getExecutionRequestId());
        return getAllIssuesByParameters(issueFilteringParams,
                columnType, sortType,
                startIndex, endIndex);
    }

    @Override
    protected MongoRepository<Issue, UUID> repository() {
        return repository;
    }

    public List<Issue> getAll() {
        return repository.findAll();
    }

    /**
     * Get TR-s and recalculate issues.
     *
     * @param testRunIds TR-s ID-s
     */
    public void mapTestRunsAndRecalculateIssues(List<UUID> testRunIds) {
        List<TestRun> testRunList = getTestRunsWithUuidAndErIdForIssueCalculation(testRunIds);
        Map<UUID, List<TestRun>> testRunMap = StreamUtils.toMapWithListEntitiesValues(testRunList,
                TestRun::getExecutionRequestId);
        log.debug("Start recalculate issues for failed Test Runs from Execution Request ids [{}]",
                testRunMap.keySet());
        testRunMap.forEach((uuid, testRuns) ->
                calculateIssuesForExecution(uuid, StreamUtils.extractIdsToList(testRuns)));
    }


    /**
     * Recalculate Top issues.
     *
     * @param executionRequestId ER ID
     */
    public void recalculateTopIssues(UUID executionRequestId) {
        log.debug("Start recalculating Top Issues for ER {}", executionRequestId);
        ExecutionRequest executionRequest = executionRequestRepository.findByUuid(executionRequestId);

        long failedLogrecordsCounter = executionRequest.getFailedLogrecordsCounter();

        List<UUID> testRunIds = getTestRunIdsForIssueCalculation(executionRequestId);

        Long updatedFailedLogrecordsCounter = logRecordService.countAllFailedLrByTestRunIds(testRunIds);

        if (failedLogrecordsCounter != updatedFailedLogrecordsCounter) {
            try {
                calculateIssuesForExecution(executionRequestId, testRunIds);
                log.debug("Complete recalculating Top Issues for ER {}", executionRequestId);
                return;
            } catch (Exception e) {
                log.error("Failed to calculate top issues for the execution request: {}", executionRequestId, e);
                throw new RamTopIssuesCalculationException();
            }
        }
        log.debug("Recalculating for ER {} wasn't started,"
                + "because failed log records count wasn't changed", executionRequestId);
    }

    /**
     * Recalculate them using current failed patterns.
     *
     * @param executionRequestId ID of ER
     */
    public void recalculateIssuesForExecution(UUID executionRequestId) {
        log.info("Start recalculating issues process for the ER: {}", executionRequestId);

        List<Issue> existedIssues = repository.findByExecutionRequestId(executionRequestId);
        List<UUID> issueIds = StreamUtils.extractIdsToList(existedIssues);
        log.debug("Existed issues: {}", issueIds);

        repository.deleteAllById(issueIds);
        log.debug("Issues has been deleted");

        List<UUID> testRunIds = getTestRunIdsForIssueCalculation(executionRequestId);
        log.debug("Found not passed test runs: {}", testRunIds);

        UUID projectId = executionRequestRepository.findProjectIdByUuid(executionRequestId).getProjectId();

        calculateIssuesForExecution(executionRequestId, testRunIds, projectId);
    }

    private List<UUID> getTestRunIdsForIssueCalculation(UUID executionRequestId) {
        List<TestingStatuses> testRunStatuses = getTestRunStatusesForIssueCalculation();
        List<TestRun> testRuns = testRunService.getTestRunsIdByExecutionRequestIdAndTestingStatuses(executionRequestId,
                testRunStatuses);
        return StreamUtils.extractIdsToList(testRuns);
    }

    private List<TestRun> getTestRunsWithUuidAndErIdForIssueCalculation(List<UUID> testRunIds) {
        List<TestingStatuses> testRunStatuses = getTestRunStatusesForIssueCalculation();
        return testRunService.getTestRunsUuidErIdByTestingStatusesAndUuidIn(testRunIds, testRunStatuses);
    }

    private List<TestingStatuses> getTestRunStatusesForIssueCalculation() {
        return Arrays.asList(TestingStatuses.FAILED, TestingStatuses.STOPPED, TestingStatuses.WARNING);
    }

    /**
     * Recalculate them using current failed patterns.
     *
     * @param executionRequestId ID of ER
     */
    public void calculateIssuesForExecution(UUID executionRequestId, List<UUID> testRunsIds) {
        UUID projectId = executionRequestRepository.findProjectIdByUuid(executionRequestId).getProjectId();
        calculateIssuesForExecution(executionRequestId, testRunsIds, projectId);
    }

    /**
     * Recalculate them using current failed patterns.
     *
     * @param executionRequestId ID of ER
     * @param testRunIds         ID of TR-s
     * @param projectId          ID of project
     */
    public void calculateIssuesForExecution(UUID executionRequestId, List<UUID> testRunIds, UUID projectId) {
        List<Issue> createdIssues = repository.findShortByExecutionRequestId(executionRequestId);
        List<UUID> linkedLogRecordsId = createdIssues.stream()
                .flatMap(issue -> issue.getLogRecordIds().stream()).collect(Collectors.toList());
        Stream<LogRecord> failedLogRecordsInExecutionRequest =
                logRecordService.getAllFailedLogRecordsByTestRunIdsStream(testRunIds, linkedLogRecordsId);
        Long failedLrsCount = logRecordService.countAllFailedLrByTestRunIds(testRunIds);
        List<FailPattern> patternByProjectId = failPatternService.findPatternByProjectId(projectId);
        lockManager.executeWithLock("calculateIssuesForExecution_" + executionRequestId.toString(),
                lockDurationForCreatingIssuesSec,
                () -> {
                    analyzeFailedLogRecords(executionRequestId, failedLogRecordsInExecutionRequest, patternByProjectId);
                    customExecutionRequestRepository.updateLogRecordsCount(executionRequestId,
                            Math.toIntExact(failedLrsCount));
                });
    }

    private void createdIssues(UUID executionRequestId,
                               Stream<LogRecord> failedLogRecordsInExecutionRequest,
                               List<FailPattern> patternByProjectId,
                               List<Issue> createdIssues) {
        analyzeFailedLogRecords(executionRequestId, failedLogRecordsInExecutionRequest,
                patternByProjectId);
    }

    /**
     * Delete an issue.
     */
    public void delete(UUID id) {
        repository.deleteById(id);
    }

    private IssueResponsesModel getIssueResponsesModel(int startIndex, int endIndex,
                                                       String columnType, String sortType,
                                                       IssueFilteringParams issueFilteringParams) {
        IssueDto issueDto = customIssueRepository.getSortedAndPaginatedIssuesByFilters(issueFilteringParams, columnType,
                sortType, startIndex, endIndex);
        List<IssueResponse> responses = getResponses(issueDto.getData());
        return new IssueResponsesModel(
                issueDto.getMetadata().isEmpty() ? 0 : (int) issueDto.getMetadata().get(0).getTotalCount(),
                responses
        );
    }

    private void analyzeFailedLogRecords(UUID executionRequestId, Stream<LogRecord> logRecords,
                                         List<FailPattern> failPatterns) {
        try {
            Map<Pattern, FailPattern> compiledPatterns = collectCompiledPatternsToMap(failPatterns);
            Iterators.partition(logRecords.iterator(), logRecordStep)
                    .forEachRemaining(partOfLogRecords -> {
                        List<Issue> createdIssues =
                            prepareIssueByLogRecord(partOfLogRecords, compiledPatterns, executionRequestId);
                        saveAll(createdIssues);
                        createdIssues.forEach(issue ->
                                testRunService.updateFieldRootCauseIdByTestRunsIds(issue.getFailedTestRunIds(),
                                        issue.getFailReasonId()));
                        log.trace("Created issues: {}", createdIssues);
                    });
        } catch (Exception e) {
            log.error("Analyze failed Log Records finished with error, Fail Patterns: {}, "
                            + "Log Records: {}, Execution Request ids: {}",
                    failPatterns, logRecords, executionRequestId, e);
            throw e;
        }
    }


    /**
     * Create issue by log record.
     *
     * @param logRecords list of {@link LogRecord}
     * @param compiledPatterns compiled failed patterns
     * @param executionRequestId ER id
     */
    private List<Issue> prepareIssueByLogRecord(List<LogRecord> logRecords, Map<Pattern, FailPattern> compiledPatterns,
                                                UUID executionRequestId) {
        Map<UUID, LogRecordWithFailPatternsDto> logRecordsWithPatterns = new HashMap<>();
        List<LogRecord> logRecordsWithoutPatterns = new ArrayList<>();
        logRecords.forEach(logRecord -> {
            String message = logRecord.getMessage();
            List<FailPattern> matchingPatterns = findMatchingFailPatterns(executionRequestId,
                    message, compiledPatterns);
            if (matchingPatterns.isEmpty() && !Strings.isNullOrEmpty(message)) {
                logRecordsWithoutPatterns.add(logRecord);
            } else {
                logRecordsWithPatterns.put(logRecord.getUuid(),
                        new LogRecordWithFailPatternsDto(logRecord, matchingPatterns));
            }
        });

        List<Issue> createdIssues =
                updateCreatedIssuesAndCreateNewByMessage(logRecordsWithoutPatterns, executionRequestId);

        createdIssues.addAll(updateCreatedIssuesAndCreateNewByPatterns(logRecordsWithPatterns, executionRequestId));
        return createdIssues;
    }

    private List<Issue> updateCreatedIssuesAndCreateNewByMessage(List<LogRecord> logRecords, UUID executionRequestId) {
        if (!logRecords.isEmpty()) {
            List<Issue> createdIssues = new ArrayList<>(customIssueRepository
                    .getCreatedIssuesByLogRecordsMessage(logRecords
                            .stream()
                            .map(RamObject::getUuid)
                            .collect(Collectors.toList()), executionRequestId));
            List<UUID> updatedLogRecords = createdIssues.stream()
                    .flatMap(issue -> issue.getLogRecordIds().stream())
                    .collect(Collectors.toList());
            List<Issue> newIssues = new ArrayList<>();
            logRecords.stream()
                    .filter(logRecord -> !updatedLogRecords.contains(logRecord.getUuid()))
                    .forEach(logRecord ->
                            addIssueToList(
                                    issue -> issue.getMessage().equals(logRecord.getMessage()),
                                    () -> createIssue(logRecord, executionRequestId),
                                    newIssues, logRecord));
            createdIssues.addAll(newIssues);
            return createdIssues;
        } else {
            return new ArrayList<>();
        }
    }

    private List<Issue> updateCreatedIssuesAndCreateNewByPatterns(
            Map<UUID, LogRecordWithFailPatternsDto> logRecordsWithPatterns,
            UUID executionRequestId) {
        Collection<LogRecordWithFailPatternsDto> values = logRecordsWithPatterns.values();
        List<Issue> createdIssues = new ArrayList<>(repository
                .findByExecutionRequestIdAndFailPatternIdIn(
                        executionRequestId,
                        values
                                .stream()
                                .flatMap(entity -> entity.getFailPatterns().stream())
                                .map(FailPattern::getUuid)
                                .collect(Collectors.toList())
                        ));
        createdIssues.forEach(issue ->
            values.stream()
                    .filter(entity -> entity.getFailPatterns().stream()
                            .anyMatch(pattern -> pattern.getUuid().equals(issue.getFailPatternId())))
                    .forEach(entity -> updateExistingIssue(issue, entity.getLogRecord())));

        List<UUID> updatedLogRecords = createdIssues.stream()
                .flatMap(issue -> issue.getLogRecordIds().stream())
                .collect(Collectors.toList());
        List<Issue> newIssues = new ArrayList<>();
        values.stream()
                .filter(logRecordDto -> !updatedLogRecords.contains(logRecordDto.getLogRecord().getUuid()))
                .forEach(logRecordDto ->
                    logRecordDto.getFailPatterns().forEach(failPattern -> {
                        LogRecord logRecord = logRecordDto.getLogRecord();
                        addIssueToList(
                                issue -> issue.getFailPatternId().equals(failPattern.getUuid()),
                                () -> createIssue(logRecord, executionRequestId, failPattern),
                                newIssues, logRecord);
                    }));
        createdIssues.addAll(newIssues);
        return createdIssues;
    }

    private Map<Pattern, FailPattern> collectCompiledPatternsToMap(List<FailPattern> failPatterns) {
        final Map<Pattern, FailPattern> compiledFailPatternsMap = new HashMap<>();
        failPatterns.forEach(failPattern -> {
            try {
                compiledFailPatternsMap.put(Pattern.compile(failPattern.getRule()), failPattern);
            } catch (PatternSyntaxException e) {
                log.error("Could not parse pattern {}", failPattern.getUuid(), e);
            }
        });
        log.debug("Compiled patterns: {}", compiledFailPatternsMap);

        return compiledFailPatternsMap;
    }

    /**
     * Updates fail pattern and it's log records.
     */
    public FailPattern saveFailPattern(FailPattern failPattern, UUID executionRequestId) {
        updateIssuesByFailPattern(failPattern, executionRequestId);
        return failPatternService.save(failPattern);
    }

    /**
     * Update issues after fail pattern update.
     *
     * @param executionRequestId test run to log.
     * @param failPattern        we want to update.
     */
    private void updateIssuesByFailPattern(FailPattern failPattern, UUID executionRequestId) {
        purgeOldPatternIssues(failPattern.getUuid(), executionRequestId);
        log.debug("Updating issues for the execution request {} and pattern {}", executionRequestId,
                failPattern.getUuid());
        List<LogRecord> logRecords = testRunService.findAllByExecutionRequestId(executionRequestId)
                .stream()
                .flatMap(testRun -> logRecordService.getAllFailedLogRecordsByTestRunId(testRun.getUuid()).stream())
                .collect(Collectors.toList());
        List<Issue> createdIssues = new ArrayList<>();
        Pattern compiledFailPattern = Pattern.compile(failPattern.getRule());
        logRecords.forEach(logRecord -> {
            try {
                boolean isMatching = compiledFailPattern
                        .matcher(logRecord.getMessage())
                        .find();
                if (!isMatching) {
                    if (!Strings.isNullOrEmpty(logRecord.getMessage())) {
                        addIssueToList(
                                issue -> issue.getMessage().equals(logRecord.getMessage()),
                                () -> createIssue(logRecord, executionRequestId),
                                createdIssues, logRecord);
                    }
                } else {
                    addIssueToList(
                            issue -> issue.getFailPatternId().equals(failPattern.getUuid()),
                            () -> createIssue(logRecord, executionRequestId, failPattern),
                            createdIssues, logRecord);
                }
            } catch (StackOverflowError error) {
                processStackOverflowErrorDuringPatternMatch(executionRequestId, error,
                        Pattern.compile(failPattern.getRule()), logRecord.getMessage());
            }
        });
        log.debug("Final issue list: {}, Execution Request ids: {}", createdIssues, executionRequestId);
        repository.saveAll(createdIssues);
    }

    /**
     * Deletes fail pattern and updates the log records.
     */
    public void deleteFailPattern(UUID failPatternId, UUID executionRequestId) {
        deleteIssuesByFailPattern(failPatternId, executionRequestId);
        failPatternService.deleteByUuid(failPatternId);
    }

    /**
     * Deletes removed fail pattern id from all related issues.
     *
     * @param failPatternId remove fail pattern id
     */
    @Transactional
    public void deleteFailPattern(UUID failPatternId) {
        failPatternService.deleteByUuid(failPatternId);
        log.info("Removing deleted failPatternId = {} from issues.", failPatternId);
        repository.updateByRemovedPatternId(failPatternId);
        log.info("Removing deleted failPatternId = {} from issues... DONE", failPatternId);
    }

    /**
     * Delete issues after fail pattern update.
     *
     * @param executionRequestId request update limit.
     * @param failPatternId      we want to delete.
     */
    private void deleteIssuesByFailPattern(UUID failPatternId, UUID executionRequestId) {
        purgeOldPatternIssues(failPatternId, executionRequestId);
        log.debug("Updating issues that belonged to Fail Pattern: {}, Execution Request id: {}",
                failPatternId, executionRequestId);
        List<LogRecord> logRecords = testRunService.findAllByExecutionRequestId(executionRequestId)
                .stream()
                .flatMap(testRun -> logRecordService.getAllFailedLogRecordsByTestRunId(testRun.getUuid()).stream())
                .collect(Collectors.toList());
        List<Issue> createdIssues = new ArrayList<>();
        logRecords.forEach(logRecord -> {
            if (!Strings.isNullOrEmpty(logRecord.getMessage())) {
                addIssueToList(
                        issue -> issue.getMessage().equals(logRecord.getMessage()),
                        () -> createIssue(logRecord, executionRequestId),
                        createdIssues, logRecord);
            }
        });
        log.debug("Final issue list: {}, Execution Request id: {}", createdIssues, executionRequestId);
        repository.saveAll(createdIssues);
    }

    private void purgeOldPatternIssues(UUID failPatternId, UUID executionRequestId) {
        log.debug("Issue purge by Fail Pattern: {}, Execution Request id: {}",
                failPatternId, executionRequestId);
        List<Issue> oldPatternIssues = repository
                .findByFailPatternIdAndExecutionRequestId(failPatternId, executionRequestId);
        repository.deleteAll(oldPatternIssues);
    }

    private Issue createIssue(LogRecord logRecord, UUID executionRequestId) {
        final UUID logRecordId = logRecord.getUuid();
        log.debug("Creating a new issue for log record '{}' and execution request '{}'", logRecord, executionRequestId);

        final Issue issue = new Issue();
        issue.setLogRecordIds(new ArrayList<>());
        issue.getLogRecordIds().add(logRecordId);
        issue.setFailedTestRunIds(new ArrayList<>());
        issue.setExecutionRequestId(executionRequestId);
        issue.setMessage(logRecord.getMessage());
        issue.setFailedTestRunsCount(0);

        log.debug("Created issue: {}", issue);

        return issue;
    }

    private Issue createIssue(LogRecord logRecord, UUID executionRequestId, FailPattern failPattern) {
        final UUID logRecordId = logRecord.getUuid();
        final UUID failPatternId = failPattern.getUuid();
        log.debug("Creating a new issue for log record '{}' and and execution request '{}' and fail pattern '{}'",
                logRecordId, executionRequestId, failPattern);

        final Issue issue = createIssue(logRecord, executionRequestId);
        issue.setMessage(failPattern.getMessage());
        issue.setJiraTickets(failPattern.getJiraTickets());
        issue.setJiraDefects(failPattern.getJiraDefects());
        issue.setFailPatternId(failPatternId);
        issue.setFailReasonId(failPattern.getFailReasonId());
        issue.setPriority(failPattern.getPriority());

        log.debug("Created issue: {}", issue);

        return issue;
    }

    /**
     * Find fail pattern matching.
     */
    public List<FailPattern> findMatchingFailPatterns(UUID executionRequestId, String message,
                                                      Map<Pattern, FailPattern> compiledPatterns) {
        List<FailPattern> matchedFailPatterns = new ArrayList<>();
        compiledPatterns.keySet().forEach(pattern -> {
            try {
                if (nonNull(message) && pattern.matcher(new TimeoutRegexCharSequence(message, regexpTimeout)).find()) {
                    matchedFailPatterns.add(compiledPatterns.get(pattern));
                }
            } catch (StackOverflowError error) {
                processStackOverflowErrorDuringPatternMatch(executionRequestId, error, pattern, message);
            } catch (TimeoutRegexException e) {
                log.error("Timeout error on regexp processing, pattern:{}, Execution Request id {}",
                        pattern.pattern(), executionRequestId);
                log.error("Timeout error on regexp processing, error message: ", e);
            }
        });
        return matchedFailPatterns;
    }

    private void processStackOverflowErrorDuringPatternMatch(UUID executionRequestId, StackOverflowError error,
                                                             Pattern pattern, String message) {
        String errorMessage = String.format("StackOverflowError during matching: pattern = %s, message = %s",
                pattern, message);
        log.error(errorMessage, error);
        executionRequestDetailsService.createDetails(executionRequestId, TestingStatuses.WARNING, errorMessage);
    }

    private void addIssueToList(Predicate<Issue> issueCheck, Supplier<Issue> newIssueSupplier,
                                List<Issue> createdIssues, LogRecord logRecord) {
        final Issue existingIssue = createdIssues
                .stream()
                .filter(issueCheck)
                .findFirst()
                .orElseGet(() -> {
                    Issue newIssue = newIssueSupplier.get();
                    createdIssues.add(newIssue);

                    return newIssue;
                });

        updateExistingIssue(existingIssue, logRecord);
    }

    private void updateExistingIssue(Issue existingIssue, LogRecord logRecord) {
        final List<UUID> testRuns = existingIssue.getFailedTestRunIds();

        final UUID logRecordTestRunId = logRecord.getTestRunId();
        if (!testRuns.contains(logRecordTestRunId)) {
            testRuns.add(logRecordTestRunId);
            existingIssue.setFailedTestRunsCount(existingIssue.getFailedTestRunsCount() + 1);
        }

        final List<UUID> logRecords = existingIssue.getLogRecordIds();
        final UUID logRecordId = logRecord.getUuid();
        if (!logRecords.contains(logRecordId)) {
            logRecords.add(logRecordId);
        }

        log.debug("Updated Test Runs and Log Records with issue: {}", existingIssue);
    }

    private List<IssueResponse> getResponses(List<Issue> issues) {
        final Set<UUID> issueFailPatternIds = extractIds(issues, Issue::getFailPatternId);
        final List<FailPattern> failPatterns = failPatternService.getFailPatternsByIds(issueFailPatternIds);
        final Map<UUID, FailPattern> failPatternMap = StreamUtils.toIdEntityMap(failPatterns);
        final Set<UUID> issueFailedTestRunIds = StreamUtils.extractFlatIds(issues, Issue::getFailedTestRunIds);
        final List<TestRun> issueFailedTestRuns = testRunService.getShortTestRunsByIds(issueFailedTestRunIds);
        final List<FailPatternResponse> failPatternResponses = failPatternService.getResponses(failPatterns);
        final Map<UUID, FailPatternResponse> failPatternResponsesMap = StreamUtils.toIdEntityMap(failPatternResponses);
        return issues
                .stream()
                .map(issue -> {
                    IssueResponse issueResponse = modelMapper.map(issue, IssueResponse.class);
                    List<TestRun> testRuns = StreamUtils.filterList(issueFailedTestRuns, issue.getFailedTestRunIds());
                    List<IssueTestRunResponse> testRunResponses =
                            StreamUtils.mapToClazz(testRuns, IssueTestRunResponse.class);
                    issueResponse.setTestRuns(testRunResponses);
                    FailPatternResponse issueFailPattern = issueResponse.getFailPattern();
                    if (nonNull(issueFailPattern)) {
                        FailPattern failPattern = failPatternMap.get(issue.getFailPatternId());
                        if (nonNull(failPattern)) {
                            FailPatternResponse failPatternResponse =
                                    failPatternResponsesMap.get(failPattern.getUuid());
                            failPatternResponse.setJiraTickets(issue.getJiraTickets());
                            issueResponse.setFailReason(failPatternResponse.getFailReason());
                            issueResponse.setFailPattern(failPatternResponse);
                        }
                    }
                    return issueResponse;
                })
                .collect(Collectors.toList());
    }

    public UUID getProjectIdByIssueId(UUID issueId) {
        return customIssueRepository.getProjectIdByIssueId(issueId);
    }

    /**
     * Deleted Issue by ER ids.
     */
    public void deleteAllIssueByExecutionRequestIds(List<UUID> executionRequestIds) {
        repository.deleteAllByExecutionRequestIdIn(executionRequestIds);
    }
}
