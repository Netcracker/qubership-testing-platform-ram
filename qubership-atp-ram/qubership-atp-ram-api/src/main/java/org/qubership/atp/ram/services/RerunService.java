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
import static org.springframework.util.CollectionUtils.isEmpty;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.qubership.atp.auth.springbootstarter.exceptions.AtpException;
import org.qubership.atp.auth.springbootstarter.utils.ExceptionUtils;
import org.qubership.atp.integration.configuration.mdc.MdcUtils;
import org.qubership.atp.ram.clients.api.dto.catalogue.RerunRequestDto;
import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.exceptions.testruns.RamTestRunsRerunNotAppropriateStatusException;
import org.qubership.atp.ram.mdc.MdcField;
import org.qubership.atp.ram.model.request.RerunRequest;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.utils.StreamUtils;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class RerunService {

    private final ExecutionRequestService executionRequestService;
    private final ExecutionRequestDetailsService executionRequestDetailsService;
    private final TestRunService testRunService;
    private final OrchestratorService orchestratorService;

    private final CatalogueService catalogueService;

    /**
     * Find ExecutionRequests with Terminated status.
     *
     * @param executionRequestsIds set of ER ids.
     * @return ids of ERs with Terminated status.
     */
    public List<UUID> getRequestsForRerun(List<UUID> executionRequestsIds) {
        return executionRequestsIds.stream().filter(executionRequestsId ->
                ExecutionStatuses.TERMINATED
                        .equals(executionRequestService.findById(executionRequestsId).getExecutionStatus())
                        || ExecutionStatuses.TERMINATED_BY_TIMEOUT
                        .equals(executionRequestService.findById(executionRequestsId)
                                .getExecutionStatus())
                        || ExecutionStatuses.FINISHED.equals(executionRequestService.findById(executionRequestsId)
                        .getExecutionStatus())
        ).collect(Collectors.toList());
    }


    /**
     * Rerun ER-s.
     *
     * @param uuidList of ER-s for rerun
     */
    public List<UUID> rerunExecutionRequests(List<UUID> uuidList) {
        List<UUID> requestsForRerun = getRequestsForRerun(uuidList);
        List<UUID> newExecutionRequests = new ArrayList<>();
        requestsForRerun.forEach(erId -> {
            List<UUID> testRunIds = testRunService.findTestRunsUuidByExecutionRequestId(erId);
            if (!isEmpty(testRunIds)) {
                log.debug("rerunExecutionRequests: start rerun ER {}, TR ids [{}]", erId, testRunIds);
                newExecutionRequests.add(rerunTestRuns(testRunIds));
            }
        });
        UUID erId = newExecutionRequests.get(0);
        MdcUtils.put(MdcField.EXECUTION_REQUEST_ID.toString(), erId);
        UUID projectId = executionRequestService.getProjectIdByExecutionRequestId(uuidList.get(0));
        String msg =
                "ER has been restarted.\n"
                        + "TestRuns were formed by ram for rerun.\n"
                        + "Source ER link: "
                        + executionRequestService.generateErLink(erId, projectId);
        executionRequestDetailsService.createDetails(erId, TestingStatuses.UNKNOWN, msg);
        return newExecutionRequests;
    }

    /**
     * Rerun TRs from ER with filter by statuses.
     */
    public UUID rerunByFilter(RerunRequest request) {
        UUID executionRequestId = request.getExecutionRequestId();
        MdcUtils.put(MdcField.EXECUTION_REQUEST_ID.toString(), executionRequestId);

        List<TestRun> testRuns = testRunService.getTestRunsIdByExecutionRequestIdAndTestingStatuses(executionRequestId,
                request.getStatuses());
        if (isEmpty(testRuns)) {
            String message = String.format("Test Runs with statuses %s for rerun ER %s were not found.",
                    request.getStatuses(), executionRequestId);
            log.error(message);
            throw new AtpException(message);
        }
        List<UUID> testRunIds = new ArrayList<>(StreamUtils.extractIds(testRuns, TestRun::getUuid));
        log.debug("Test Runs {} were found by statuses {}.", testRunIds, request.getStatuses());
        ExecutionRequest executionRequest = executionRequestService.get(executionRequestId);
        RerunRequestDto rerunRequestDto = new RerunRequestDto();
        rerunRequestDto.setExecutionRequestId(executionRequestId);
        rerunRequestDto.setProjectId(executionRequest.getProjectId());
        rerunRequestDto.setRecompilation(true);
        rerunRequestDto.setEnvironmentId(executionRequest.getEnvironmentId());
        rerunRequestDto.setScopeId(executionRequest.getTestScopeId());
        rerunRequestDto.setTaToolsGroupId(executionRequest.getTaToolsGroupId());
        rerunRequestDto.setThreads(executionRequest.getThreads());
        rerunRequestDto.setTestRunIds(testRunIds);
        return catalogueService.rerunExecutionRequest(rerunRequestDto);
    }


    /**
     * Finds TestRuns with Terminated status and sends the request to orchestrator for rerun.
     *
     * @param testRunIds set of TR ids.
     * @return ids of TRs with Terminated status.
     * @throws RuntimeException in case no testruns for rerun found or some of testcases have different ER id
     */
    public UUID rerunTestRuns(List<UUID> testRunIds) {
        List<TestRun> testRuns = getTestRunsForRerun(testRunIds);
        if (isEmpty(testRuns)) {
            ExceptionUtils.throwWithLog(log, new RamTestRunsRerunNotAppropriateStatusException());
        }
        UUID finalExecutionRequestId = testRuns.get(0).getExecutionRequestId();
        if (testRuns
                .stream()
                .anyMatch(tr -> !tr.getExecutionRequestId().equals(finalExecutionRequestId))) {
            throw new RuntimeException("All selected testruns must have same ER id");
        }
        MdcUtils.put(MdcField.EXECUTION_REQUEST_ID.toString(), finalExecutionRequestId);
        return orchestratorService.rerunTestRuns(finalExecutionRequestId, null,
                testRuns.stream()
                        .sorted(Comparator.comparingInt(TestRun::getOrder))
                        .map(TestRun::getUuid)
                        .collect(Collectors.toList()));
    }


    /**
     * Find TestRuns with Terminated status.
     *
     * @param testRunIds set of TR ids.
     * @return ids of TRs with Terminated status.
     */
    public List<TestRun> getTestRunsForRerun(List<UUID> testRunIds) {
        return testRunService.findAllByUuidInAndExecutionStatusIn(testRunIds,
                asList(ExecutionStatuses.TERMINATED, ExecutionStatuses.TERMINATED_BY_TIMEOUT,
                        ExecutionStatuses.FINISHED));
    }
}
