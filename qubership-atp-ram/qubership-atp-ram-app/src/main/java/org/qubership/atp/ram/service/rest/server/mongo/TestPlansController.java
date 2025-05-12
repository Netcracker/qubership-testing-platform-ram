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

import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import java.util.List;
import java.util.UUID;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.atp.ram.dto.request.TestPlansSearchRequest;
import org.qubership.atp.ram.entities.MailRecipients;
import org.qubership.atp.ram.models.TestPlan;
import org.qubership.atp.ram.services.TestPlansService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RequestMapping("/api/testplans")
@RestController()
@RequiredArgsConstructor
public class TestPlansController /*implements TestPlansControllerApi*/ {

    private final TestPlansService service;

    /**
     * Get list of TestPlans by ProjectUuid.
     */
    @GetMapping(value = "/{projectId}")
    @PreAuthorize("@entityAccess.checkAccess(#projectId,'READ')")
    @AuditAction(auditAction = "Get all test plans by projectId = {{#projectId}}")
    public List<TestPlan> getTestPlansByProjectUuid(@PathVariable("projectId") UUID projectId) {
        return service.getTestPlansByProjectUuid(projectId);
    }

    /**
     * Create new TestPlan.
     *
     * @deprecated use TestRunLoggingController#findOrCreateWithParents while logging of steps
     */
    @PostMapping(value = "/create")
    @PreAuthorize("@entityAccess.checkAccess(#testPlan.getProjectId(),'CREATE')")
    @Deprecated
    public ResponseEntity createTestPlan(@RequestBody TestPlan testPlan) {
        return new ResponseEntity<>(service.create(testPlan), HttpStatus.CREATED);
    }

    /**
     * Set TestPlan mailRecipients.
     */
    @PutMapping(value = "/{testPlanUuid}/recipients")
    @PreAuthorize("@entityAccess.checkAccess(@testPlansService.getProjectIdByTestPlanId(#testPlanUuid),'UPDATE')")
    @AuditAction(auditAction = "Set recipients from request body for test plan '{{#testPlanUuid}}'")
    public ResponseEntity setTestPlanRecipients(@PathVariable("testPlanUuid") UUID testPlanUuid,
                                                @RequestBody MailRecipients recipients) {
        service.saveRecipients(testPlanUuid, recipients);
        return ResponseEntity.ok().build();
    }

    /**
     * Get all recipients for specified {@link TestPlan}.
     *
     * @param testPlanUuid uuid of test plan
     * @return the list of recipients {@link MailRecipients}
     */
    @GetMapping(value = "/{testPlanUuid}/recipients")
    @PreAuthorize("@entityAccess.checkAccess(@testPlansService.getProjectIdByTestPlanId(#testPlanUuid),'READ')")
    @AuditAction(auditAction = "Get all recipients for test plan '{{#testPlanUuid}}'")
    public MailRecipients getRecipients(@PathVariable("testPlanUuid") UUID testPlanUuid) {
        return service.getRecipients(testPlanUuid);
    }

    /**
     * Save TestPlan.
     *
     * @deprecated For creating use TestRunLoggingController#findOrCreateWithParents while logging of steps
     */
    @Deprecated
    @PreAuthorize("@entityAccess.checkAccess(#testPlan.projectId(),'UPDATE')")
    @PutMapping(value = "/save", produces = TEXT_PLAIN_VALUE)
    public UUID saveTestPlan(@RequestBody TestPlan testPlan) {
        service. save(testPlan);
        return testPlan.getUuid();
    }

    @GetMapping(value = "/{testPlanUuid}/name", produces = TEXT_PLAIN_VALUE)
    @PreAuthorize("@entityAccess.checkAccess(@testPlansService.getProjectIdByTestPlanId(#testPlanUuid),'READ')")
    @AuditAction(auditAction = "Get test plan name by testPlanId = {{#testPlanUuid}}")
    public String getTestPlanName(@PathVariable("testPlanUuid") UUID testPlanUuid) {
        return service.getTestPlanName(testPlanUuid);
    }

    @GetMapping(value = "/{testPlanUuid}/testplanForNavigationPath")
    @PreAuthorize("@entityAccess.checkAccess(@testPlansService.getProjectIdByTestPlanId(#testPlanUuid),'READ')")
    @AuditAction(auditAction = "Get test plan '{{#testPlanUuid}}' for navigation path")
    public TestPlan getTestPlan(@PathVariable("testPlanUuid") UUID testPlanUuid) {
        return service.getTestPlanForNavigationPath(testPlanUuid);
    }

    /**
     * Search test plans.
     */
    @PostMapping(value = "/search")
    @PreAuthorize("@entityAccess.checkAccess(#request.getProjectId(),'READ')")
    public List<TestPlan> search(@RequestBody TestPlansSearchRequest request) {
        return service.search(request);
    }
}
