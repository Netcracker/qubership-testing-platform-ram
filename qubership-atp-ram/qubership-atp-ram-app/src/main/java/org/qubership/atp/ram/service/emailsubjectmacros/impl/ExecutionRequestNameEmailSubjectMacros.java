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

package org.qubership.atp.ram.service.emailsubjectmacros.impl;

import java.util.UUID;

import org.qubership.atp.ram.dto.response.ExecutionSummaryResponse;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.service.emailsubjectmacros.ResolvableEmailSubjectMacros;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ExecutionRequestNameEmailSubjectMacros implements ResolvableEmailSubjectMacros {

    @Override
    public String resolve(ExecutionRequest executionRequest, ExecutionSummaryResponse executionSummaryResponse) {
        UUID executionRequestId = executionRequest.getUuid();
        log.debug("Start resolving macros 'EXECUTION_REQUEST_NAME' for execution request with id: {}",
                executionRequestId);

        String executionRequestName = executionRequest.getName();
        log.debug("Resolved value: {}", executionRequestName);

        return executionRequestName;
    }
}
