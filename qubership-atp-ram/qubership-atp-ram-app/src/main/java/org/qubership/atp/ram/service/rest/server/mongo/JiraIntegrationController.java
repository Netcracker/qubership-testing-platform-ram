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

package org.qubership.atp.ram.service.rest.server.mongo;

import java.util.List;
import java.util.UUID;

import org.qubership.atp.ram.controllers.api.JiraIntegrationControllerApi;
import org.qubership.atp.ram.controllers.api.dto.jira.TestRunForRefreshFromJiraDto;
import org.qubership.atp.ram.controllers.api.dto.jira.TestRunToJiraInfoDto;
import org.qubership.atp.ram.converters.ModelConverter;
import org.qubership.atp.ram.model.TestRunForRefreshFromJira;
import org.qubership.atp.ram.model.TestRunToJiraInfo;
import org.qubership.atp.ram.services.JiraIntegrationService;
import org.qubership.atp.ram.services.TestRunService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@Slf4j
public class JiraIntegrationController implements JiraIntegrationControllerApi {

    private final JiraIntegrationService integrationService;
    private final TestRunService testRunService;
    private final ModelConverter modelConverter;

    @Override
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@testRunService.getProjectIdByTestRunId(#testRunIds.get(0)),'READ')")
    public ResponseEntity<List<TestRunToJiraInfoDto>> getTestRunsForJiraInfo(List<UUID> testRunIds) {
        log.info("Request to propagate test runs {} to JIRA ", testRunIds);
        List<TestRunToJiraInfo> result =
                integrationService.getTestRunsForJiraInfoByIds(testRunIds);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                .body(modelConverter.convertJiraInfoModelToDto(result));
    }

    @Override
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@executionRequestService.getProjectIdByExecutionRequestId(#id),'READ')")
    public ResponseEntity<List<TestRunToJiraInfoDto>> getTestRunsForJiraInfoByExecutionRequest(UUID id) {
        log.info("Request to propagate test runs by executionId {} to JIRA ", id);
        List<TestRunToJiraInfo> result =
                integrationService.getTestRunsForJiraInfoByExecutionId(id);
        return ResponseEntity.ok(modelConverter.convertJiraInfoModelToDto(result));
    }

    @Override
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@testRunService.getProjectIdByTestRunId(#testRunIds.get(0)),'READ')")
    public ResponseEntity<List<TestRunForRefreshFromJiraDto>> getTestRunsForRefreshFromJira(List<UUID> testRunIds) {
        log.info("Request to request test runs {} from JIRA ", testRunIds);
        List<TestRunForRefreshFromJira> result =
                integrationService.getTestRunsForRefreshFromJira(testRunIds);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                .body(modelConverter.convertRefreshFromJiraModelToDto(result));
    }
}
