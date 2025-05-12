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

import static org.qubership.atp.ram.services.JointExecutionRequestsReportDataModel.TestCase;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.qubership.atp.ram.clients.api.dto.environments.environment.SystemFullVer1ViewDto;
import org.qubership.atp.ram.dto.request.JointExecutionRequestSearchRequest;
import org.qubership.atp.ram.dto.response.Environment;
import org.qubership.atp.ram.dto.response.JointExecutionRequestSearchResponse;
import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.exceptions.executionrequests.RamMultipleActiveJointExecutionRequestsException;
import org.qubership.atp.ram.models.Comment;
import org.qubership.atp.ram.models.EnvironmentsInfo;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.JointExecutionRequest;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.RootCause;
import org.qubership.atp.ram.models.SystemInfo;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.repositories.JointExecutionRequestRepository;
import org.qubership.atp.ram.services.JointExecutionRequestsReportDataModel.EnvironmentData;
import org.qubership.atp.ram.services.JointExecutionRequestsReportDataModel.EnvironmentsData;
import org.qubership.atp.ram.services.JointExecutionRequestsReportDataModel.ExecutionRequestCount;
import org.qubership.atp.ram.services.JointExecutionRequestsReportDataModel.ExecutionRequestsData;
import org.qubership.atp.ram.services.JointExecutionRequestsReportDataModel.QaSystemData;
import org.qubership.atp.ram.services.JointExecutionRequestsReportDataModel.StatusCount;
import org.qubership.atp.ram.utils.RateCalculator;
import org.qubership.atp.ram.utils.RateCalculator.TestingStatusesStat;
import org.qubership.atp.ram.utils.StreamUtils;
import org.qubership.atp.ram.utils.TimeUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class JointExecutionRequestService {

    private static final String DEFAULT_PROJECT_TIME_ZONE = "GMT+03:00";
    private static final String DEFAULT_PROJECT_DATE_FORMAT = "d MMM yyyy";
    private static final String DEFAULT_PROJECT_TIME_FORMAT = "hh:mm";

    @Value("${catalogue.url}")
    private String catalogueUrl;

    @Value("${jointExecutionRequests.complete.timeout.seconds}")
    private Integer defaultTimeout;

    private final ExecutionRequestService executionRequestService;
    private final TestRunService testRunService;
    private final EnvironmentsInfoService environmentsInfoService;
    private final EnvironmentsService environmentService;
    private final RateCalculator rateCalculator;
    private final JointExecutionRequestRepository repository;
    private final RootCauseService rootCauseService;

    /**
     * Update active joint execution request.
     *
     * @param executionRequest execution request
     */
    public void updateActiveJointExecutionRequest(ExecutionRequest executionRequest) {
        final UUID executionRequestId = executionRequest.getUuid();
        log.info("Updating active joint execution request for ER with id '{}'", executionRequestId);

        final JointExecutionRequest activeJointExecutionRequest = getActiveJointExecutionRequest(executionRequest);

        activeJointExecutionRequest.setKey(executionRequest.getJointExecutionKey());
        activeJointExecutionRequest.setCount(executionRequest.getJointExecutionCount());
        activeJointExecutionRequest.setTimeout(executionRequest.getJointExecutionTimeout());
        activeJointExecutionRequest.upsertRun(executionRequestId, executionRequest.getExecutionStatus());

        log.debug("Saving updated joint execution request");
        repository.save(activeJointExecutionRequest);
    }

    /**
     * Create joint execution request.
     *
     * @param executionRequest execution request
     * @return joint execution request
     */
    public JointExecutionRequest createJointExecutionRequest(ExecutionRequest executionRequest) {
        final UUID executionRequestId = executionRequest.getUuid();
        log.info("Creating new joint execution request for ER with id '{}'", executionRequestId);

        final JointExecutionRequest request = new JointExecutionRequest();

        final Timestamp currentDate = Timestamp.valueOf(LocalDateTime.now());
        request.setName("Joint Execution Request [" + currentDate + "]");
        request.setStartDate(currentDate);
        request.setKey(executionRequest.getJointExecutionKey());
        request.setCount(executionRequest.getJointExecutionCount());
        request.setTimeout(executionRequest.getJointExecutionTimeout());
        request.setStatus(JointExecutionRequest.Status.IN_PROGRESS);
        request.upsertRun(executionRequestId, executionRequest.getExecutionStatus());

        log.debug("Saving new joint execution request: {}", request);
        return repository.save(request);
    }

    /**
     * Check if provided execution request is joint.
     *
     * @param executionRequestId execution request identifier
     * @return check result value
     */
    public boolean isJointExecutionRequest(UUID executionRequestId) {
        ExecutionRequest executionRequest = executionRequestService.get(executionRequestId);

        return isJointExecutionRequest(executionRequest);
    }

    /**
     * Check if provided execution request is joint.
     *
     * @param executionRequest execution request
     * @return check result value
     */
    public boolean isJointExecutionRequest(ExecutionRequest executionRequest) {
        String jointExecutionKey = executionRequest.getJointExecutionKey();
        log.info("Checked joint execution key for ER id: {} with ststus is {}", executionRequest.getUuid(),
                !StringUtils.isEmpty(jointExecutionKey) ? "joint ER" : "not joint");
        return !StringUtils.isEmpty(jointExecutionKey);
    }

    /**
     * Check if joint execution request completed.
     *
     * @param jointExecutionRequest joint execution request
     * @return check result value
     */
    public boolean isJointExecutionRequestReady(JointExecutionRequest jointExecutionRequest) {
        final UUID id = jointExecutionRequest.getUuid();
        log.info("Checking is joint execution request '{}' is ready for report sending", id);

        final Integer count = jointExecutionRequest.getCount();
        log.debug("Joint execution request count: {} for joint ER with id {}", count, id);

        if (count == 0) {
            log.debug("Found count is zero for joint ER with id {}", id);
            return false;
        }

        log.debug("Joint execution request runs: {}  for joint ER with id {}" , jointExecutionRequest.getRuns(), id);
        final List<UUID> completedExecutionRequestIds = jointExecutionRequest.getCompletedExecutionRequestIds();

        final long completedErs = completedExecutionRequestIds.size();
        log.debug("Completed ER's count: {}, with ids: {} for joint ER with id {}",
                completedErs, completedExecutionRequestIds, id);

        return completedErs == count;
    }

    /**
     * Get active joint execution request.
     *
     * @param executionRequestId execution request identifier
     * @return joint execution request
     */
    public JointExecutionRequest getActiveJointExecutionRequest(UUID executionRequestId) {
        ExecutionRequest executionRequest = executionRequestService.get(executionRequestId);

        return getActiveJointExecutionRequest(executionRequest);
    }

    /**
     * Get active joint execution request.
     *
     * @param executionRequest execution request
     * @return joint execution request
     */
    public JointExecutionRequest getActiveJointExecutionRequest(ExecutionRequest executionRequest) {
        final String jointExecutionKey = executionRequest.getJointExecutionKey();
        log.info("Getting active joint execution request for ER with id: {} and joint execution key :{}",
                executionRequest.getUuid(), jointExecutionKey);

        final List<JointExecutionRequest> jointExecutionRequests =
                repository.findAllActiveJointExecutionRequestsByKey(jointExecutionKey);
        log.debug("Founded joint execution requests: {}", jointExecutionRequests);

        if (isEmpty(jointExecutionRequests)) {
            return createJointExecutionRequest(executionRequest);
        }

        if (jointExecutionRequests.size() > 1) {
            log.error("More than one active joint execution requests with the same key: {}", jointExecutionKey);
            throw new RamMultipleActiveJointExecutionRequestsException();
        }

        final JointExecutionRequest jointExecutionRequest = jointExecutionRequests.get(0);
        jointExecutionRequest.upsertRun(executionRequest);

        return repository.save(jointExecutionRequest);
    }

    /**
     * Check and complete all joint execution requests which exceedes their timeouts.
     *
     * @param mailSendFunc mail sending function
     */
    public void checkAndCompleteJointExecutionRequestsByTimeout(Consumer<JointExecutionRequest> mailSendFunc) {
        log.info("Start joint execution request completion process");
        List<JointExecutionRequest> allActiveJointExecutionRequests = repository.findAllActiveJointExecutionRequests();

        Set<UUID> ids = StreamUtils.extractIds(allActiveJointExecutionRequests);
        log.debug("Found active joint execution requests: {}", ids);

        LocalDateTime now = LocalDateTime.now();
        log.debug("Current date: {}", now);

        allActiveJointExecutionRequests.forEach(jointExecutionRequest -> {
            log.debug("Checking joint execution request '{}'", jointExecutionRequest.getUuid());
            try {
                LocalDateTime startDate = jointExecutionRequest.getStartDate().toLocalDateTime();
                Integer timeout = jointExecutionRequest.getTimeout();

                if (timeout == 0) {
                    log.debug("Found zero timeout value for joint execution request with id: {}",
                            jointExecutionRequest.getUuid());
                    timeout = defaultTimeout;
                }

                LocalDateTime timeoutDate = startDate.plusSeconds(timeout);
                log.debug("Timeout date: {}", timeoutDate);

                if (now.isAfter(timeoutDate)) {
                    log.debug("Timeout is exceeded, sending report...");
                    mailSendFunc.accept(jointExecutionRequest);

                    log.debug("Updating joint execution request with id {} and status {}",
                            jointExecutionRequest.getUuid(),
                            JointExecutionRequest.Status.COMPLETED_BY_TIMEOUT);
                    jointExecutionRequest.setStatus(JointExecutionRequest.Status.COMPLETED_BY_TIMEOUT);
                    repository.save(jointExecutionRequest);
                }
            } catch (Exception e) {
                log.error("Failed to complete by timeout joint execution request", e);
                completeFailedJointExecutionRequest(jointExecutionRequest, e);
            }
        });
        log.info("End joint execution request completion process");
    }

    /**
     * Update all active joint execution request runs with provided execution status.
     *
     * @param executionRequest execution request
     */
    public void updateJointExecutionRequestRunStatus(ExecutionRequest executionRequest) {
        final UUID executionRequestId = executionRequest.getUuid();
        final ExecutionStatuses executionStatus = executionRequest.getExecutionStatus();
        log.info("Update all active joint execution request runs for ER '{}' with status '{}'",
                executionRequestId, executionStatus);
        final String jointExecutionKey = executionRequest.getJointExecutionKey();
        final List<JointExecutionRequest> jointExecutionRequests =
                repository.findAllActiveJointExecutionRequestsByKey(jointExecutionKey);

        jointExecutionRequests.forEach(jointExecutionRequest -> {
            jointExecutionRequest.upsertRun(executionRequestId, executionStatus);
        });
        repository.saveAll(jointExecutionRequests);
    }

    /**
     * Complete joint execution request.
     *
     * @param activeJointExecutionRequest active joint execution request
     */
    public void completeJointExecutionRequest(JointExecutionRequest activeJointExecutionRequest) {
        log.info("Complete joint execution request with id '{}' and status {} ",
                activeJointExecutionRequest.getUuid(), JointExecutionRequest.Status.COMPLETED);
        activeJointExecutionRequest.setStatus(JointExecutionRequest.Status.COMPLETED);
        repository.save(activeJointExecutionRequest);
        log.debug("Joint execution request has been completed");
    }

    /**
     * Complete failed joint execution request.
     *
     * @param jointExecutionRequest joint execution request
     * @param logs                  error or fail logs
     */
    public void completeFailedJointExecutionRequest(JointExecutionRequest jointExecutionRequest, String... logs) {
        log.info("Complete joint execution request with id '{}' and logs as failed", jointExecutionRequest.getUuid());
        final String joinedLogs = String.join("\n", logs);
        jointExecutionRequest.setLogs(joinedLogs);
        jointExecutionRequest.setStatus(JointExecutionRequest.Status.FAILED);
        log.debug("Logs: {}", joinedLogs);

        repository.save(jointExecutionRequest);
    }

    /**
     * Complete failed joint execution request.
     *
     * @param e exception
     */
    public void completeFailedJointExecutionRequest(JointExecutionRequest jointExecutionRequest, Exception e) {
        log.info("Complete joint execution request with id '{}' and error as failed", jointExecutionRequest.getUuid());
        final String stackTrace = ExceptionUtils.getStackTrace(e);
        jointExecutionRequest.setLogs(stackTrace);
        jointExecutionRequest.setStatus(JointExecutionRequest.Status.FAILED);
        log.debug("Stacktrace: {}", stackTrace);

        repository.save(jointExecutionRequest);
    }

    /**
     * Get joint execution requests report data model for the report.
     *
     * @param erIds execution request identifiers
     * @return data model
     */
    public JointExecutionRequestsReportDataModel getJointExecutionRequestsReportDataModel(List<UUID> erIds) {
        log.info("Getting joint execution request report data model");
        final List<ExecutionRequest> executionRequests = executionRequestService
                .getOrderedExecutionRequestsByIds(erIds);
        final EnvironmentsData environmentsData = getEnvironmentsData(executionRequests);
        final ExecutionRequestsData executionRequestsData = getExecutionRequestsData(executionRequests);

        return new JointExecutionRequestsReportDataModel(environmentsData, executionRequestsData);
    }

    /**
     * Get joint execution request for provided key.
     *
     * @param key joint execution request key
     * @return found joint execution request
     */
    public JointExecutionRequest getJointExecutionRequest(String key) {
        log.info("Getting joint execution request for key: {}", key);
        List<JointExecutionRequest> jointExecutionRequests = repository.findAllByKey(key);

        final JointExecutionRequest jointExecutionRequest = StreamUtils.checkAndReturnSingular(jointExecutionRequests);
        log.debug("Found joint execution request: {}", jointExecutionRequest);

        return jointExecutionRequest;
    }

    /**
     * Search joint execution requests.
     *
     * @param request search request
     * @return found execution requests
     */
    public List<JointExecutionRequestSearchResponse> search(JointExecutionRequestSearchRequest request) {
        log.info("Search execution requests by request: {}", request);
        final String key = request.getKey();

        final JointExecutionRequest jointExecutionRequest = getJointExecutionRequest(key);

        final List<UUID> completedExecutionRequestIds = jointExecutionRequest.getCompletedExecutionRequestIds();

        final List<ExecutionRequest> executionRequests =
                executionRequestService.getExecutionRequestsByIds(completedExecutionRequestIds);
        log.debug("Found execution requests: {}", StreamUtils.extractIds(executionRequests));

        if (!isEmpty(executionRequests)) {
            return StreamUtils.map(executionRequests, JointExecutionRequestSearchResponse::new);
        }

        return Collections.emptyList();
    }

    private ExecutionRequestsData getExecutionRequestsData(List<ExecutionRequest> executionRequests) {
        ExecutionRequestsData executionRequestsData = new ExecutionRequestsData();

        List<ExecutionRequestCount> executionRequestCounts = getExecutionRequestCounts(executionRequests);
        executionRequestsData.setExecutionRequestCounts(executionRequestCounts);

        int totalTestCaseCount = executionRequestCounts.stream()
                .mapToInt(ExecutionRequestCount::getTestCaseCount)
                .sum();

        Map<String, Long> statusTotalCount = executionRequestCounts.stream()
                .flatMap(executionRequestCount -> executionRequestCount.getStatusCounts().stream())
                .collect(Collectors.groupingBy(StatusCount::getStatus, Collectors.summingLong(StatusCount::getCount)));

        List<StatusCount> statusOverallCounts = statusTotalCount.entrySet()
                .stream()
                .sorted(Comparator.comparingInt(entry -> TestingStatuses.findByName(entry.getKey()).getReportsOrder()))
                .map(entry -> {
                    String status = entry.getKey();
                    int totalStatusCount = Math.toIntExact(entry.getValue());
                    float rate = RateCalculator.calculateRateFloat(totalStatusCount, totalTestCaseCount);

                    return new StatusCount(status, totalStatusCount, rate);
                })
                .collect(Collectors.toList());

        List<String> statuses = Arrays.stream(TestingStatuses.values())
                .sorted(Comparator.comparingInt(TestingStatuses::getReportsOrder))
                .map(testingStatus -> testingStatus.getName().toUpperCase())
                .collect(Collectors.toList());

        executionRequestsData.setStatuses(statuses);
        executionRequestsData.setTotalStatusCounts(statusOverallCounts);
        executionRequestsData.setTestCaseTotalCount(totalTestCaseCount);

        return executionRequestsData;
    }

    private List<ExecutionRequestCount> getExecutionRequestCounts(List<ExecutionRequest> executionRequests) {
        return executionRequests.stream()
                .map(executionRequest -> {
                    final UUID executionRequestId = executionRequest.getUuid();
                    final UUID projectId = executionRequest.getProjectId();
                    final List<TestRun> testRuns = executionRequestService.getAllTestRuns(executionRequestId);

                    final ExecutionRequestCount executionRequestCount = new ExecutionRequestCount();

                    final List<StatusCount> statusCounts = getStatusCounts(executionRequest, testRuns);
                    final List<TestCase> testCases = getTestCases(testRuns);

                    executionRequestCount.setStatusCounts(statusCounts);
                    executionRequestCount.setTestCaseCount(executionRequest.getCountOfTestRuns());
                    executionRequestCount.setName(executionRequest.getName());
                    executionRequestCount.setLink(getExecutionRequestLink(catalogueUrl, projectId, executionRequestId));
                    executionRequestCount.setTestCases(testCases);

                    return executionRequestCount;
                })
                .collect(Collectors.toList());
    }

    private List<TestCase> getTestCases(List<TestRun> testRuns) {
        final Set<UUID> rootCauseIds = StreamUtils.extractIds(testRuns, TestRun::getRootCauseId);
        final List<RootCause> rootCauses = rootCauseService.getByIds(rootCauseIds);
        final Map<UUID, String> rootCauseMap = StreamUtils.toIdNameEntityMap(rootCauses);

        return testRuns.stream()
                .map(testRun -> {
                    final UUID testRunId = testRun.getUuid();

                    final TestCase testCase = new TestCase();
                    testCase.setName(testRun.getName());

                    final TestingStatuses testingStatus = testRun.getTestingStatus();
                    testCase.setStatus(testingStatus.getName().toUpperCase());

                    final String duration = DurationFormatUtils.formatDuration(
                            testRun.getDuration() * 1000,
                            "HH:mm:ss",
                            true);
                    testCase.setDuration(duration);

                    if (testingStatus.equals(TestingStatuses.FAILED)) {
                        final LogRecord firstFailedStep = testRunService.getFirstFailedStep(testRunId);
                        testCase.setFirstFailedStepName(firstFailedStep.getName());
                    }

                    final Comment comment = testRun.getComment();
                    if (Objects.nonNull(comment)) {
                        testCase.setComment(comment.getHtml());
                    }

                    final UUID rootCauseId = testRun.getRootCauseId();
                    if (Objects.nonNull(rootCauseId)) {
                        final String rootCauseName = rootCauseMap.get(rootCauseId);
                        testCase.setFailureReason(rootCauseName);
                    }

                    return testCase;
                })
                .collect(Collectors.toList());
    }

    private List<StatusCount> getStatusCounts(ExecutionRequest executionRequest, List<TestRun> testRuns) {
        final Map<TestingStatuses, TestingStatusesStat> stats =
                rateCalculator.calculateTestRunsTestingStatusStats(executionRequest, testRuns);

        return stats.entrySet()
                .stream()
                .sorted(Comparator.comparingInt(entry -> entry.getKey().getReportsOrder()))
                .map(entry -> {
                    TestingStatuses status = entry.getKey();
                    TestingStatusesStat statusStats = entry.getValue();

                    return new StatusCount(status.getName(), statusStats.getCount(), statusStats.getRate());
                })
                .collect(Collectors.toList());
    }

    private EnvironmentsData getEnvironmentsData(List<ExecutionRequest> executionRequests) {
        final Map<UUID, ExecutionRequest> executionRequestMap = StreamUtils.toIdEntityMap(executionRequests);
        final Set<UUID> executionRequestIds = StreamUtils.extractIds(executionRequests);
        final List<EnvironmentsInfo> environmentsInfos = environmentsInfoService.findByRequestIds(executionRequestIds);

        final EnvironmentsData environmentsData = new EnvironmentsData();
        final List<EnvironmentData> environments = environmentsInfos.stream()
                .map(environmentsInfo -> {
                    final EnvironmentData environmentData = new EnvironmentData();

                    final UUID executionRequestId = environmentsInfo.getExecutionRequestId();
                    final ExecutionRequest executionRequest = executionRequestMap.get(executionRequestId);
                    final UUID projectId = executionRequest.getProjectId();
                    final UUID environmentId = environmentsInfo.getEnvironmentId();
                    final String environmentLink = getEnvironmentLink(catalogueUrl, projectId, environmentId);
                    final List<SystemInfo> qaSystems = environmentsInfo.getQaSystemInfoList();

                    final List<SystemFullVer1ViewDto> systems = environmentService.getEnvironmentSystems(environmentId);
                    final Map<String, SystemFullVer1ViewDto> systemsMap = systems.stream()
                            .collect(Collectors.toMap(SystemFullVer1ViewDto::getName, Function.identity()));

                    final List<QaSystemData> qaSystemsData = qaSystems.stream()
                            .map(qaSystem -> {
                                String name = qaSystem.getName();
                                String version = qaSystem.getVersion();
                                SystemFullVer1ViewDto system = systemsMap.get(name);
                                String timestamp = getLastVersionCheckDate(system);

                                return new QaSystemData(name, version, timestamp);
                            })
                            .collect(Collectors.toList());

                    final Environment environment = environmentService.getEnvironmentById(environmentId);
                    environmentData.setName(environment.getName());
                    environmentData.setLink(environmentLink);
                    environmentData.setSystems(qaSystemsData);

                    return environmentData;
                })
                .collect(Collectors.toList());

        environmentsData.setEnvironments(environments);

        return environmentsData;
    }

    private String getLastVersionCheckDate(SystemFullVer1ViewDto system) {
        Long lastVersionCheck = system.getDateOfLastCheck();
        String timestamp = null;

        if (lastVersionCheck != null) {
            String dateTimeFormat = String.format("%s %s", DEFAULT_PROJECT_DATE_FORMAT, DEFAULT_PROJECT_TIME_FORMAT);
            Timestamp versionTimestamp = new Timestamp(lastVersionCheck);
            timestamp = TimeUtils.formatDateTime(versionTimestamp, dateTimeFormat, DEFAULT_PROJECT_TIME_ZONE);
        }

        return timestamp;
    }

    private String getExecutionRequestLink(String host, UUID projectId, UUID executionRequestId) {
        return host + "/project/" + projectId + "/ram/execution-request/" + executionRequestId;
    }

    private String getEnvironmentLink(String host, UUID projectId, UUID environmentId) {
        return host + "/project/" + projectId + "/environments/environment/" + environmentId;
    }
}
