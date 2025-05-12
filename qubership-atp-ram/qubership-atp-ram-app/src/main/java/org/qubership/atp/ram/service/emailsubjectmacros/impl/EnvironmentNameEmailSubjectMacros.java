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

import static java.util.Objects.isNull;

import java.util.UUID;

import org.qubership.atp.auth.springbootstarter.exceptions.AtpIllegalNullableArgumentException;
import org.qubership.atp.ram.dto.response.Environment;
import org.qubership.atp.ram.dto.response.ExecutionSummaryResponse;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.service.emailsubjectmacros.ResolvableEmailSubjectMacros;
import org.qubership.atp.ram.services.EnvironmentsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class EnvironmentNameEmailSubjectMacros implements ResolvableEmailSubjectMacros {

    @Autowired
    private EnvironmentsService environmentsService;

    @Override
    public String resolve(ExecutionRequest executionRequest, ExecutionSummaryResponse executionSummaryResponse) {
        UUID executionRequestId = executionRequest.getEnvironmentId();
        log.debug("Start resolving macros 'ENVIRONMENT_NAME' for execution request with id: {}", executionRequestId);

        UUID environmentId = executionRequest.getEnvironmentId();

        if (isNull(environmentId)) {
            log.error("Found illegal nullable environment id for the validated method parameter");
            throw new AtpIllegalNullableArgumentException("environment id", "execution request");
        }
        log.debug("Execution request environment id: {}", environmentId);

        Environment environment = environmentsService.getEnvironmentById(environmentId);
        log.debug("Received environment data: {}", environment);

        String environmentName = environment.getName();
        log.debug("Resolved value: {}", environmentName);

        return environmentName;
    }
}
