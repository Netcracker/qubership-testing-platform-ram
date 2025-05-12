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

package org.qubership.atp.ram.service.rest.server;

import java.util.List;
import java.util.UUID;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.atp.ram.model.request.RerunRequest;
import org.qubership.atp.ram.services.OrchestratorService;
import org.qubership.atp.ram.services.RerunService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequestMapping("/api")
@RestController()
@CrossOrigin
@RequiredArgsConstructor
@Slf4j
public class RerunController /*implements RerunControllerApi*/ {
    private final OrchestratorService orchestratorService;
    private final RerunService rerunService;

    /**
     * Run or rerun ExecutionRequests.
     *
     * @param uuidList set of ExecutionRequests that needs rerunning
     * @deprecated use runRerunExecutionRequests method
     */
    @Deprecated
    @PostMapping(value = "/executionrequests/v0/runrerun")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "@executionRequestService.getProjectIdByExecutionRequestId(#uuidList.get(0)),'UPDATE')")
    public void runRerunExecutionRequestsOld(@RequestBody List<UUID> uuidList) {
        List<UUID> requestsForRerun = rerunService.getRequestsForRerun(uuidList);
        orchestratorService.rerun(requestsForRerun);
    }

    /**
     * Run or rerun ExecutionRequests.
     *
     * @param uuidList set of ExecutionRequests that needs rerunning.
     */
    @PostMapping(value = "/executionrequests/runrerun")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "@executionRequestService.getProjectIdByExecutionRequestId(#uuidList.get(0)),'UPDATE')")
    @AuditAction(auditAction = "Rerun provided execution requests")
    public ResponseEntity<List<UUID>> runRerunExecutionRequests(@RequestBody List<UUID> uuidList) {
        return new ResponseEntity<>(rerunService.rerunExecutionRequests(uuidList), HttpStatus.OK);
    }

    @PostMapping(value = "/executionrequests/rerun/filtering")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "@executionRequestService.getProjectIdByExecutionRequestId(#request.getExecutionRequestId()),'UPDATE')")
    @AuditAction(auditAction = "Rerun provided execution requests by testing statuses")
    public UUID rerunByFilter(@RequestBody RerunRequest request) {
        return rerunService.rerunByFilter(request);
    }

    /**
     * Run or rerun ExecutionRequests.
     *
     * @param uuidList set of ExecutionRequests that needs rerunning.
     */
    @PostMapping(value = "/testruns/runrerun")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@testRunService.getProjectIdByTestRunId(#uuidList.get(0)),'UPDATE')")
    @AuditAction(auditAction = "Rerun provided test runs")
    public UUID runRerun(@RequestBody List<UUID> uuidList) {
        return rerunService.rerunTestRuns(uuidList);
    }
}
