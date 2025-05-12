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
import static org.qubership.atp.ram.logging.constants.ApiPathLogging.LOGGING_PATH;
import static org.qubership.atp.ram.logging.constants.ApiPathLogging.TEST_PLANS_PATH;

import org.qubership.atp.ram.logging.constants.ApiPathLogging;
import org.qubership.atp.ram.logging.entities.requests.CreatedTestPlanRequest;
import org.qubership.atp.ram.logging.entities.responses.CreatedTestPlanResponse;
import org.qubership.atp.ram.logging.services.TestPlanLoggingService;
import org.qubership.atp.ram.models.TestPlan;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RequestMapping(API_PATH + LOGGING_PATH + TEST_PLANS_PATH)
@RestController()
@RequiredArgsConstructor
public class TestPlanLoggingController {

    private final TestPlanLoggingService testPlanLoggingService;

    /**
     * Find existed test plan or created new by info from request.
     *
     * @param createdTestPlanRequest info for created new test plan
     * @return ID of test plan
     */
    @PostMapping(ApiPathLogging.FIND_OR_CREATE_PATH)
    public CreatedTestPlanResponse findOrCreate(@RequestBody CreatedTestPlanRequest createdTestPlanRequest) {
        TestPlan testPlan = testPlanLoggingService.findByUuidNameOrCreateNew(
                createdTestPlanRequest.getProjectId(), createdTestPlanRequest.getTestPlanId(),
                createdTestPlanRequest.getTestPlanName());
        return new CreatedTestPlanResponse(testPlan.getUuid());
    }
}
