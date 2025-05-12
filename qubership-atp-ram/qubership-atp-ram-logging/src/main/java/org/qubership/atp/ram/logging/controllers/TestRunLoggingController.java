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

package org.qubership.atp.ram.logging.controllers;

import static org.qubership.atp.ram.logging.constants.ApiPathLogging.API_PATH;
import static org.qubership.atp.ram.logging.constants.ApiPathLogging.FIND_OR_CREATE_PATH;
import static org.qubership.atp.ram.logging.constants.ApiPathLogging.LOGGING_PATH;
import static org.qubership.atp.ram.logging.constants.ApiPathLogging.STOP_PATH;
import static org.qubership.atp.ram.logging.constants.ApiPathLogging.TEST_RUNS_PATH;
import static org.qubership.atp.ram.logging.constants.ApiPathLogging.UPDATE_PATH;
import static org.qubership.atp.ram.logging.constants.ApiPathLogging.WITH_PARENTS;

import org.qubership.atp.ram.logging.entities.requests.CreatedTestRunRequest;
import org.qubership.atp.ram.logging.entities.requests.CreatedTestRunWithParentsRequest;
import org.qubership.atp.ram.logging.entities.requests.StopTestRunRequest;
import org.qubership.atp.ram.logging.entities.responses.CreatedTestRunResponse;
import org.qubership.atp.ram.logging.entities.responses.StopTestRunResponse;
import org.qubership.atp.ram.logging.services.TestRunLoggingService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RequestMapping(API_PATH + LOGGING_PATH + TEST_RUNS_PATH)
@RestController()
@RequiredArgsConstructor
public class TestRunLoggingController {
    private final TestRunLoggingService testRunLoggingService;

    /**
     * Create/update test run, execution request, test plan and project.
     *
     * @param request for updated info
     * @return executionRequestId, testRunId
     */
    @PostMapping(value = WITH_PARENTS + FIND_OR_CREATE_PATH)
    public CreatedTestRunResponse findOrCreateWithParents(@RequestBody CreatedTestRunWithParentsRequest request) {
        return testRunLoggingService.findOrCreateWithParents(request);
    }

    /**
     * Update test run.
     *
     * @param request for updated info
     * @return testRunId
     */
    @PostMapping(value = UPDATE_PATH)
    public CreatedTestRunResponse update(@RequestBody CreatedTestRunRequest request) {
        return testRunLoggingService.update(request);
    }

    /**
     * Stop test run.
     *
     * @param request for updated info
     * @return execution status (string value)
     */
    @PostMapping(value = STOP_PATH)
    public StopTestRunResponse stop(@RequestBody StopTestRunRequest request) {
        return testRunLoggingService.stop(request);
    }
}
