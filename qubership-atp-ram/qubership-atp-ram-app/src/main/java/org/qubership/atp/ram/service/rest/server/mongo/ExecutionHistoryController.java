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

import java.util.UUID;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.atp.ram.dto.request.TestCaseExecutionHistorySearchRequest;
import org.qubership.atp.ram.models.TestCaseExecutionHistory;
import org.qubership.atp.ram.service.ExecutionHistoryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/execution-history")
@RequiredArgsConstructor
public class ExecutionHistoryController {

    private final ExecutionHistoryService service;

    /**
     * Returns test case execution history.
     *
     * @return found test case execution history
     */
    @PostMapping(value = "/testcase/{testCaseId}/executions")
    @PreAuthorize("@entityAccess.checkAccess(@testRunService.getProjectIdByTestCaseId(#testCaseId),'READ')")
    @AuditAction(auditAction = "Get test case '{{#testCaseId}}' execution history")
    public TestCaseExecutionHistory getTestCaseExecutions(@RequestBody TestCaseExecutionHistorySearchRequest request,
                                                          @PathVariable UUID testCaseId) {
        return service.getTestCaseExecutions(request, testCaseId);
    }
}
