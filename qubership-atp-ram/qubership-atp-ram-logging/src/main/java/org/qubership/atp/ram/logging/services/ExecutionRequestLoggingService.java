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

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.logging.entities.requests.CreatedTestRunWithParentsRequest;
import org.qubership.atp.ram.logging.utils.ObjectsFieldsUtils;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.TestPlan;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.repositories.ExecutionRequestRepository;
import org.qubership.atp.ram.utils.RateCalculator;
import org.qubership.atp.ram.utils.TimeUtils;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExecutionRequestLoggingService {
    private final ExecutionRequestRepository executionRequestRepository;
    private final ModelMapper modelMapper;
    private final RateCalculator rateCalculator;
    private final LogRecordLoggingService logRecordLoggingService;
    private final TestRunLoggingService testRunLoggingService;

    ExecutionRequest findOrCreateExecutionRequest(CreatedTestRunWithParentsRequest request, TestPlan testPlan) {
        UUID executionRequestId = request.getAtpExecutionRequestId();
        ExecutionRequest executionRequest = executionRequestRepository.findByUuid(executionRequestId);
        if (Objects.nonNull(executionRequest)) {
            log.debug("Execution request is exist {}", executionRequest.getUuid());
            return executionRequest;
        }

        return createNewExecutionRequest(request, testPlan);
    }

    private ExecutionRequest createNewExecutionRequest(CreatedTestRunWithParentsRequest request, TestPlan testPlan) {
        String executionRequestName = request.getExecutionRequestName();
        UUID executionRequestId = request.getAtpExecutionRequestId();
        String mail = request.getMailList();

        ExecutionRequest executionRequest = modelMapper.map(request, ExecutionRequest.class);
        executionRequest.setUuid(executionRequestId);
        executionRequest.setName(executionRequestName);
        executionRequest.setTestPlanId(testPlan.getUuid());
        executionRequest.setProjectId(testPlan.getProjectId());
        executionRequest.setExecutionStatus(ExecutionStatuses.IN_PROGRESS);
        executionRequest.setStartDate(new Timestamp(System.currentTimeMillis()));
        executionRequest.setLegacyMailRecipients(mail);

        log.debug("Execution request will be creating. Name {}, ID {}", executionRequestName, executionRequestId);
        return executionRequestRepository.save(executionRequest);
    }

    /**
     * Stops execution request.
     * @param requestId requestId
     */
    public void stop(UUID requestId) {
        ExecutionRequest executionRequest = executionRequestRepository.findByUuid(requestId);
        log.trace("Start stopping Execution Request: {}", executionRequest.getUuid());
        executionRequest.setFinishDate(new Timestamp(System.currentTimeMillis()));

        ExecutionStatuses currentExecutionStatus = executionRequest.getExecutionStatus();
        if (Objects.isNull(currentExecutionStatus)
                || currentExecutionStatus.getId() < ExecutionStatuses.FINISHED.getId()) {
            executionRequest.setExecutionStatus(ExecutionStatuses.FINISHED);
        }
        log.trace("Execution Request: {} status was changed from {} to {}", executionRequest.getUuid(),
                currentExecutionStatus, executionRequest.getExecutionStatus());

        List<TestRun> testRuns = testRunLoggingService.findTestRunsByErId(executionRequest.getUuid());
        if (!testRuns.isEmpty()) {
            log.debug("Start calculate rates for ER {}", executionRequest.getUuid());
            rateCalculator.calculateRates(executionRequest, testRuns);
            log.debug("Start calculate count of screenshots for every TR in ER {}", executionRequest.getUuid());
            logRecordLoggingService.saveCountScreenshots(executionRequest.getUuid(), testRuns);
        }

        executionRequest.setDuration(
                TimeUtils.getDuration(executionRequest.getStartDate(), executionRequest.getFinishDate()));
        executionRequestRepository.save(executionRequest);

        log.debug("Execution Request: {} was finished and analyzed.", executionRequest.getUuid());
    }

    /**
     * Updates execution status.
     * @param id execution request id
     * @param newExecutionStatus new execution status
     */
    public void updateExecutionStatus(UUID id, ExecutionStatuses newExecutionStatus) {
        log.trace("Start updating execution status {} for ER {}", newExecutionStatus, id);

        if (ExecutionStatuses.FINISHED.equals(newExecutionStatus)) {
            stop(id);
        } else {
            ExecutionRequest executionRequest = executionRequestRepository.findByUuid(id);
            ObjectsFieldsUtils.setField(executionRequest::setExecutionStatus,
                    Objects.nonNull(newExecutionStatus)
                            ? newExecutionStatus
                            : executionRequest.getExecutionStatus());

            executionRequestRepository.save(executionRequest);
        }
        log.trace("Stop updating execution status for ER {}", id);
    }
}
