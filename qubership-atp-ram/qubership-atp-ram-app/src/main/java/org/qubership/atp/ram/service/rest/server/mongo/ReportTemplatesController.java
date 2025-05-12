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

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.atp.ram.models.ReportTemplate;
import org.qubership.atp.ram.service.history.ConcurrentModificationService;
import org.qubership.atp.ram.services.ReportTemplatesService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RequestMapping("/api/templates/project")
@RestController
@RequiredArgsConstructor
public class ReportTemplatesController /*implements ReportTemplatesControllerApi*/ {

    private final ReportTemplatesService service;
    private final ConcurrentModificationService concurrentModificationService;

    @GetMapping("/{projectId}")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).MAIL_TEMPLATE.getName(),"
            + "#projectId,'READ')")
    @AuditAction(auditAction = "Get all report templates for project '{{#projectId}}'")
    public List<ReportTemplate> getAll(@PathVariable("projectId") UUID projectId) {
        return service.getTemplatesByProjectId(projectId);
    }

    @GetMapping(value = "{projectId}/template/{uuid}")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).MAIL_TEMPLATE.getName(),"
            + "#projectId,'READ')")
    @AuditAction(auditAction = "Get report template for project '{{#projectId}}' by template id = {{#uuid}}")
    public ReportTemplate getByUuid(@PathVariable("projectId") UUID projectId, @PathVariable("uuid") UUID uuid) {
        return service.get(uuid);
    }

    @GetMapping(value = "{projectId}/template/default")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).MAIL_TEMPLATE.getName(),"
            + "#projectId,'READ')")
    @AuditAction(auditAction = "Get default report template")
    public ReportTemplate getDefaultTemplate(@PathVariable UUID projectId) {
        return service.getDefaultTemplate();
    }

    @PostMapping(value = "/{projectId}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).MAIL_TEMPLATE.getName(),"
            + "#projectId,'CREATE')")
    @AuditAction(auditAction = "Create report template for project '{{#projectId}}'")
    public ReportTemplate create(@RequestBody ReportTemplate template, @PathVariable("projectId") UUID projectId) {
        return service.save(template);
    }

    /**
     * Update model ReportTemplate.
     */
    @PutMapping(value = "/{projectId}/template/{uuid}")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).MAIL_TEMPLATE.getName(),"
            + "#projectId,'UPDATE')")
    @AuditAction(auditAction = "Update report template '{{#template.uuid}}' for project '{{#projectId}}'")
    public ResponseEntity<ReportTemplate> save(@RequestBody ReportTemplate template,
                                              @PathVariable("projectId") UUID projectId) {
        HttpStatus httpStatus =
                concurrentModificationService.getConcurrentModificationHttpStatus(template.getUuid(),
                        template.getModifiedWhen(), service);
        return ResponseEntity.status(httpStatus).body(service.update(projectId, template));
    }

    @DeleteMapping(value = "/{projectId}/template/{uuid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).MAIL_TEMPLATE.getName(),"
            + "#projectId,'DELETE')")
    @AuditAction(auditAction = "Delete report template '{{#uuid}}' for project '{{#projectId}}'")
    public void delete(@PathVariable UUID uuid, @PathVariable("projectId") UUID projectId) {
        service.deleteByUuid(uuid);
    }
}
