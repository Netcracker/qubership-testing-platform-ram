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
import org.qubership.atp.ram.dto.request.ValidationLabelConfigTemplateSearchRequest;
import org.qubership.atp.ram.models.ValidationLabelConfigTemplate;
import org.qubership.atp.ram.services.ValidationLabelConfigTemplateService;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequestMapping("/api/validationlabelconfigtemplates")
@RestController()
@RequiredArgsConstructor
@Slf4j
public class ValidationLabelConfigTemplateController /*implements ValidationLabelConfigTemplateControllerApi*/ {

    private final ValidationLabelConfigTemplateService service;

    /**
     * Returns a validation label config template by id.
     */
    @GetMapping(value = "/{id}")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).VALIDATION_LABEL_TEMPLATE.getName(),"
            + "@validationLabelConfigTemplateService.getProjectIdByConfigId(#id),'READ')")
    @AuditAction(auditAction = "Get validation label config template by id = {{#id}}")
    public ValidationLabelConfigTemplate getById(@PathVariable("id") UUID id) {
        return service.get(id);
    }

    /**
     * Returns list all validation label config templates.
     */
    @GetMapping
    @PostAuthorize("returnObject.size() == 0 ? true : @entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).VALIDATION_LABEL_TEMPLATE.getName(),"
            + "returnObject.get(0).getProjectId(),'READ')")
    @AuditAction(auditAction = "Get all validation label config templates by projectId = {{#searchRequest.projectId}}"
            + " and name = '{{#searchRequest.name}}'")
    public List<ValidationLabelConfigTemplate> getAll(ValidationLabelConfigTemplateSearchRequest searchRequest) {
        return service.getAll(searchRequest);
    }

    /**
     * Create a new validation label config template.
     */
    @PostMapping
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).VALIDATION_LABEL_TEMPLATE.getName(),"
            + "#template.getProjectId(),'CREATE')")
    @AuditAction(auditAction = "Create validation label config template for project '{{#template.projectId}}'")
    public ValidationLabelConfigTemplate create(@RequestBody ValidationLabelConfigTemplate template) {
        return service.create(template);
    }

    /**
     * Delete validation label config template.
     *
     * @param id deleted template identifier
     */
    @DeleteMapping(value = "/{id}")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).VALIDATION_LABEL_TEMPLATE.getName(),"
            + "@validationLabelConfigTemplateService.getProjectIdByConfigId(#id),'DELETE')")
    @AuditAction(auditAction = "Delete validation label config template by id = {{#id}}")
    public void delete(@PathVariable("id") UUID id) {
        service.delete(id);
    }

    /**
     * Update validation label config template.
     */
    @PutMapping(value = "/{id}")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).VALIDATION_LABEL_TEMPLATE.getName(),"
            + "#template.getProjectId(),'UPDATE')")
    @AuditAction(auditAction = "Update validation label config template by id = {{#id}}")
    public ValidationLabelConfigTemplate update(@PathVariable("id") UUID id,
                                                @RequestBody ValidationLabelConfigTemplate template) {
        return service.update(id, template);
    }
}
