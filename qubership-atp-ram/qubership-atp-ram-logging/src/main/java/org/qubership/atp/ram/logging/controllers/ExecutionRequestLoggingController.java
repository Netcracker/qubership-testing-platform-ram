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
import static org.qubership.atp.ram.logging.constants.ApiPathLogging.EXECUTION_REQUESTS_PATH;
import static org.qubership.atp.ram.logging.constants.ApiPathLogging.EXECUTION_STATUS_PATH;
import static org.qubership.atp.ram.logging.constants.ApiPathLogging.LOGGING_PATH;
import static org.qubership.atp.ram.logging.constants.ApiPathLogging.STOP_PATH;
import static org.qubership.atp.ram.logging.constants.ApiPathLogging.UPDATE_EXECUTION_STATUS;
import static org.qubership.atp.ram.logging.constants.ApiPathLogging.UUID_PATH;

import java.util.UUID;

import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.logging.constants.ApiPathLogging;
import org.qubership.atp.ram.logging.services.ExecutionRequestLoggingService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RequestMapping(API_PATH + LOGGING_PATH + EXECUTION_REQUESTS_PATH)
@RestController
@RequiredArgsConstructor
public class ExecutionRequestLoggingController {
    private final ExecutionRequestLoggingService executionRequestLoggingService;

    /**
     * Stop execution request.
     *
     * @param id of execution request
     */
    @PostMapping(value = UUID_PATH + STOP_PATH)
    public void stop(@PathVariable(ApiPathLogging.UUID) UUID id) {
        executionRequestLoggingService.stop(id);
    }

    /**
     * Update execution status of execution request.
     *
     * @param id              of execution request
     * @param executionStatus new value of execution status
     */
    @PostMapping(value = UUID_PATH + UPDATE_EXECUTION_STATUS + EXECUTION_STATUS_PATH)
    public void updateExecutionStatus(@PathVariable(ApiPathLogging.UUID) UUID id,
                                      @PathVariable(ApiPathLogging.EXECUTION_STATUS)
                                              ExecutionStatuses executionStatus) {
        executionRequestLoggingService.updateExecutionStatus(id, executionStatus);
    }
}
