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
import org.qubership.atp.ram.dto.response.ExecutionSummaryResponse;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.RamObject;
import org.qubership.atp.ram.service.emailsubjectmacros.ResolvableEmailSubjectMacros;
import org.qubership.atp.ram.services.CatalogueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TestPlanEmailSubjectMacros implements ResolvableEmailSubjectMacros {

    @Autowired
    private CatalogueService catalogueService;

    @Override
    public String resolve(ExecutionRequest executionRequest, ExecutionSummaryResponse executionSummaryResponse) {
        UUID executionRequestId = executionRequest.getUuid();
        log.debug("Start resolving macros 'TEST_PLAN_NAME' for execution request with id: {}", executionRequestId);

        UUID testPlanId = executionRequest.getTestPlanId();

        if (isNull(testPlanId)) {
            log.error("Found illegal nullable test plan id for the execution request with id: {}", executionRequestId);
            throw new AtpIllegalNullableArgumentException("test plan id", "execution request");
        }
        log.debug("Execution request test plan id: {}", testPlanId);

        RamObject testPlan = catalogueService.getTestPlan(testPlanId);
        log.debug("Received test plan data: {}", testPlan);

        String testPlanName = testPlan.getName();
        log.debug("Resolved value: {}", testPlanName);

        return testPlanName;
    }
}
