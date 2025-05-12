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

import java.util.List;
import java.util.UUID;

import org.qubership.atp.orchestrator.clients.dto.TerminateRequestDto;
import org.qubership.atp.ram.client.OrchestratorFeignClient;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrchestratorService {
    private final OrchestratorFeignClient orchestratorFeignClient;

    /**
     * Terminate execution requests.
     *
     * @param terminateRequestDto entity with set of execution requests ids
     */
    public void terminate(TerminateRequestDto terminateRequestDto) {
        orchestratorFeignClient.terminateProcess(terminateRequestDto);
    }

    /**
     * Stop execution requests.
     *
     * @param executionRequestsIds set of execution requests ids
     */

    public void stop(List<UUID> executionRequestsIds) {
        orchestratorFeignClient.stopProcess(executionRequestsIds);
    }

    /**
     * Resume execution requests.
     *
     * @param executionRequestsIds set of execution requests ids
     */
    public void resume(List<UUID> executionRequestsIds) {
        orchestratorFeignClient.resumeProcess(executionRequestsIds);
    }

    /**
     * Rerun execution requests.
     *
     * @param executionRequestsIds set of execution requests ids
     */
    public void rerun(List<UUID> executionRequestsIds) {
        orchestratorFeignClient.restartProcess(executionRequestsIds);
    }

    /**
     * Terminate test runs.
     *
     * @param testRunIds set of test runs ids
     */
    public void terminateTestRun(List<UUID> testRunIds) {
        orchestratorFeignClient.terminateTestRunProcess(testRunIds);
    }

    /**
     * Stop test runs.
     *
     * @param testRunIds set of test runs ids
     */
    public void stopTestRun(@RequestBody List<UUID> testRunIds,
                     @RequestParam(name = "executionRequestId") UUID executionRequestId) {
        orchestratorFeignClient.stopTestRunProcess(executionRequestId, testRunIds);
    }

    /**
     * Resume test runs.
     *
     * @param testRunIds set of test runs ids
     */
    public void resumeTestRun(List<UUID> testRunIds,
                              @RequestParam(name = "executionRequestId") UUID executionRequestId) {
        orchestratorFeignClient.resumeTestRunProcess(executionRequestId, testRunIds);
    }

    /**
     * Rerun test runs.
     *
     * @param testRunIds set of test runs ids
     */
    public void rerunTestRun(@RequestBody List<UUID> testRunIds,
                             @RequestParam(name = "executionRequestId") UUID executionRequestId) {
        orchestratorFeignClient.restartTestRunProcess(executionRequestId, testRunIds);
    }

    /**
     * Rerun test runs.
     *
     * @param testRunIds set of test runs ids
     */
    public UUID rerunTestRuns(@PathVariable(value = "uuid") UUID executionRequestId,
                              String token, List<UUID> testRunIds) {
        return orchestratorFeignClient.rerunTestRunsProcess(executionRequestId, token, testRunIds).getBody();
    }

    /**
     * Gets process id by execution request id.
     *
     * @param id the id
     * @return the process id by execution request id
     */
    public UUID getProcessIdByExecutionRequestId(@PathVariable(value = "id") UUID id) {
        return orchestratorFeignClient.getRunnerProcessIdByExecutionRequestId(id).getBody();
    }
}
