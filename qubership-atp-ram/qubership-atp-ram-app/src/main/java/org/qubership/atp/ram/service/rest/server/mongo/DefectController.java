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

import javax.validation.Valid;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.atp.ram.dto.request.DefectCreateRequest;
import org.qubership.atp.ram.dto.response.DefectPredefineResponse;
import org.qubership.atp.ram.model.jira.JiraIssueCreateResponse;
import org.qubership.atp.ram.service.DefectPredefineService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RequestMapping("/api/defects")
@RestController()
@RequiredArgsConstructor
public class DefectController /*implements DefectsControllerApi*/ {

    private final DefectPredefineService service;

    @PostMapping(value = "/predefine")
    @PreAuthorize("@entityAccess.checkAccess(@issueService.getProjectIdByIssueId(#issueId),'READ')")
    @AuditAction(auditAction = "Get defect predefined data by issueId = {{#issueId}}")
    public DefectPredefineResponse predefine(@RequestParam UUID issueId) throws Exception {
        return service.predefine(issueId);
    }

    @PostMapping
    @PreAuthorize("@entityAccess.checkAccess(@testPlansService.getProjectIdByTestPlanId(#testPlanId),'CREATE')")
    @AuditAction(auditAction = "Create defect by testPlanId = {{#testPlanId}} and issueId = {{#issueId}}")
    public JiraIssueCreateResponse createDefect(@RequestParam UUID testPlanId,
                                                @RequestParam UUID issueId,
                                                @RequestBody @Valid DefectCreateRequest request) throws Exception {
        return service.createDefect(testPlanId, issueId, request);
    }
}
