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

package org.qubership.atp.ram.logging.services;

import static org.qubership.atp.ram.logging.constants.ApiPathLogging.END_DATE;
import static org.qubership.atp.ram.logging.constants.ApiPathLogging.START_DATE;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.logging.entities.requests.CreatedTestRunRequest;
import org.qubership.atp.ram.logging.entities.requests.CreatedTestRunWithParentsRequest;
import org.qubership.atp.ram.logging.entities.requests.StopTestRunRequest;
import org.qubership.atp.ram.logging.entities.responses.CreatedTestRunResponse;
import org.qubership.atp.ram.logging.entities.responses.StopTestRunResponse;
import org.qubership.atp.ram.logging.utils.ListUtils;
import org.qubership.atp.ram.logging.utils.ObjectsFieldsUtils;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.Project;
import org.qubership.atp.ram.models.TestPlan;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.repositories.TestRunRepository;
import org.qubership.atp.ram.services.IssueService;
import org.qubership.atp.ram.utils.TimeUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class TestRunLoggingService {
    private final ProjectLoggingService projectLoggingService;
    private final TestPlanLoggingService testPlanLoggingService;
    private final ExecutionRequestLoggingService executionRequestLoggingService;
    private final TestRunRepository testRunRepository;
    private final ModelMapper modelMapper;
    private final IssueService issueService;

    /**
     * Find or create test runs.
     */
    public CreatedTestRunResponse findOrCreateWithParents(CreatedTestRunWithParentsRequest request) {
        log.trace("Request for create/update TR, TP, ER, project {}", request);

        Project project = projectLoggingService.findByUuidNameOrCreateNew(request);
        TestPlan testPlan = testPlanLoggingService.findByUuidNameOrCreateNew(request, project);
        ExecutionRequest executionRequest = executionRequestLoggingService
                .findOrCreateExecutionRequest(request, testPlan);

        TestRun testrun = configuredCreatedOrExistedTestRun(request, executionRequest.getUuid());
        log.trace("Finish create/update TR, TP, ER, project. Test run ID {}", testrun.getUuid());
        return new CreatedTestRunResponse(testrun.getUuid(), executionRequest.getUuid());
    }

    TestRun configuredCreatedOrExistedTestRun(CreatedTestRunWithParentsRequest request,
                                              UUID executionRequestId) {
        TestRun existedTestRun;
        if (Objects.isNull(request.getTestRunId())) {
            existedTestRun = testRunRepository.findByExecutionRequestIdAndName(executionRequestId,
                    request.getTestRunName());
        } else {
            existedTestRun = testRunRepository.findByUuid(request.getTestRunId());
        }
        TestRun configuredTestRun = configureTestRunObject(request, executionRequestId);
        if (Objects.nonNull(existedTestRun)) {
            log.debug("Test run was exist, {}", existedTestRun.getUuid());
            updateListParams(existedTestRun, configuredTestRun);
            return testRunRepository.save(existedTestRun);
        } else {
            log.debug("Test run will be creating for ER {}", executionRequestId);
            return testRunRepository.save(configuredTestRun);
        }
    }

    private void updateListParams(TestRun existedTestRun, TestRun configuredTestRun) {
        log.debug("Start updating list params for TR {}", existedTestRun.getUuid());
        existedTestRun.setQaHost(
                ListUtils.mergeTwoListsWithoutDuplicates(
                        existedTestRun.getQaHost(), configuredTestRun.getQaHost()));

        existedTestRun.setSolutionBuild(
                ListUtils.mergeTwoListsWithoutDuplicates(
                        existedTestRun.getSolutionBuild(), configuredTestRun.getSolutionBuild()));

        existedTestRun.setTaHost(
                ListUtils.mergeTwoListsWithoutDuplicates(
                        existedTestRun.getTaHost(), configuredTestRun.getTaHost()));
    }

    private TestRun configureTestRunObject(CreatedTestRunRequest request, UUID requestId) {
        log.debug("Request for TR: {}", request);
        TestRun testRun = modelMapper.map(request, TestRun.class);
        testRun.setExecutionRequestId(requestId);
        testRun.setExecutionStatus(ExecutionStatuses.IN_PROGRESS);
        ObjectsFieldsUtils.setField(testRun::setStartDate, new Timestamp(System.currentTimeMillis()));
        testRun.updateTestingStatus(TestingStatuses.UNKNOWN);
        testRun.setName(request.getTestRunName());
        return testRun;
    }

    /**
     * Update test run.
     */
    public CreatedTestRunResponse update(CreatedTestRunRequest request) {
        log.trace("Start updating TR {}", request.getTestRunId());
        TestRun testRun = testRunRepository.findByUuid(request.getTestRunId());
        ObjectsFieldsUtils.setField(testRun::setExecutionStatus, Objects.nonNull(request.getExecutionStatus())
                ? ExecutionStatuses.findByValue(request.getExecutionStatus())
                : ExecutionStatuses.IN_PROGRESS);
        ObjectsFieldsUtils.setField(testRun::updateTestingStatus, Objects.nonNull(request.getTestingStatus())
                ? TestingStatuses.findByValue(request.getTestingStatus())
                : TestingStatuses.UNKNOWN);
        ObjectsFieldsUtils.setField(testRun::setFinishDate, new Timestamp(request.getFinishDate()));
        testRun.setDuration(TimeUtils.getDuration(testRun.getStartDate(), testRun.getFinishDate()));
        updateListParams(testRun, modelMapper.map(request, TestRun.class));
        ObjectsFieldsUtils.setField(testRun::setUrlToBrowserOrLogs,
                getUrlToBrowserLog(testRun, request.getUrlToBrowserOrLogs()));
        testRunRepository.save(testRun);
        log.trace("Finish updating TR {}", request.getTestRunId());
        return new CreatedTestRunResponse(testRun.getUuid(), null);
    }

    /**
     * Stop test run.
     */
    public StopTestRunResponse stop(StopTestRunRequest request) {
        log.info("Start stopping test run {}", request.getTestRunId());
        TestRun testRun = testRunRepository.findByUuid(request.getTestRunId());
        ObjectsFieldsUtils.setField(testRun::setFinishDate, new Timestamp(System.currentTimeMillis()));
        testRun.setDuration(TimeUtils.getDuration(testRun.getStartDate(), testRun.getFinishDate()));

        ObjectsFieldsUtils.setField(testRun::updateTestingStatus, Objects.nonNull(request.getTestingStatus())
                ? TestingStatuses.findByValue(request.getTestingStatus())
                : TestingStatuses.UNKNOWN);
        ObjectsFieldsUtils.setField(testRun::setExecutionStatus, Objects.nonNull(request.getExecutionStatus())
                ? ExecutionStatuses.findByValue(request.getExecutionStatus())
                : ExecutionStatuses.FINISHED);
        ObjectsFieldsUtils.setField(testRun::setUrlToBrowserOrLogs,
                getUrlToBrowserLog(testRun, request.getUrlToBrowserOrLogs()));
        testRunRepository.save(testRun);
        if (TestingStatuses.FAILED.equals(testRun.getTestingStatus())) {
            issueService.calculateIssuesForExecution(testRun.getExecutionRequestId(),
                    Collections.singletonList(testRun.getUuid()));
        }
        log.trace("Finish stopping test run {}", request.getTestRunId());
        return new StopTestRunResponse(testRun.getExecutionStatus().toString());
    }

    Set<String> getUrlToBrowserLog(TestRun testRun, Set<String> urls) {
        log.debug("Start preparing browser log for TR {}, urls {}", testRun.getUuid(), urls);
        if (CollectionUtils.isEmpty(urls)) {
            log.debug("Urls for test run {} are empty", testRun.getUuid());
        } else {
            String forReplace = "T";
            String startDateStr = testRun.getStartDate().toString().replace(" ", forReplace);
            String finishDateStr = testRun.getFinishDate().toString().replace(" ", forReplace);

            Set<String> newUrls = new HashSet<>();
            for (String url : urls) {
                newUrls.add(url.replace(START_DATE, startDateStr).replace(END_DATE, finishDateStr));
            }
            log.debug("Finish preparing browser log for TR {}, result urls {}", testRun.getUuid(), newUrls);
            return newUrls;
        }
        return urls;
    }

    /**
     * Find list of Test Runs with uuid and testing status by Execution Request id.
     *
     * @param executionRequestId TestRun uuid.
     * @return list of Test Runs.
     */
    public List<TestRun> findTestRunsByErId(UUID executionRequestId) {
        return testRunRepository.findAllByExecutionRequestId(executionRequestId);
    }
}
