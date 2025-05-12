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
import org.qubership.atp.ram.dto.request.WidgetConfigTemplateSearchRequest;
import org.qubership.atp.ram.models.WidgetConfigTemplate;
import org.qubership.atp.ram.services.WidgetConfigTemplateService;
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

@RequestMapping("/api/widgetconfigtemplates")
@RestController()
@RequiredArgsConstructor
@Slf4j
public class WidgetConfigTemplateController /*implements WidgetConfigTemplateControllerApi*/ {

    private final WidgetConfigTemplateService service;

    /**
     * Returns a widget config template by id.
     */
    @GetMapping(value = "/{id}")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).WIDGET_CONFIGURATION.getName(),"
            + "@widgetConfigTemplateService.getProjectIdByTemplateId(#id),'READ')")
    public WidgetConfigTemplate getById(@PathVariable("id") UUID id) {
        return service.get(id);
    }

    /**
     * Returns default widget config template by id.
     */
    @GetMapping(value = "/default")
    @AuditAction(auditAction = "Get default widget config template")
    public WidgetConfigTemplate getDefaultWidgetConfigTemplate() {
        return service.getDefaultTemplate();
    }

    /**
     * Returns list all widget config templates.
     */
    @GetMapping
    @AuditAction(auditAction = "Get filtered widget config templates for project '{{#searchRequest.projectId}}'")
    public List<WidgetConfigTemplate> getAll(WidgetConfigTemplateSearchRequest searchRequest) {
        return service.getAll(searchRequest);
    }

    /**
     * Create a new widget config template.
     */
    @PostMapping
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).WIDGET_CONFIGURATION.getName(),"
            + "#template.getProjectId(),'CREATE')")
    @AuditAction(auditAction = "Create widget config template for project '{{#template.projectId}}'")
    public WidgetConfigTemplate create(@RequestBody WidgetConfigTemplate template) {
        return service.create(template);
    }

    /**
     * Delete widget config template.
     *
     * @param id deleted template identifier
     */
    @DeleteMapping(value = "/{id}")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).WIDGET_CONFIGURATION.getName(),"
            + "@widgetConfigTemplateService.getProjectIdByTemplateId(#id),'DELETE')")
    @AuditAction(auditAction = "Delete widget config template by id = {{#id}}")
    public void delete(@PathVariable("id") UUID id) {
        service.delete(id);
    }

    /**
     * Update widget config template.
     */
    @PutMapping(value = "/{id}")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).WIDGET_CONFIGURATION.getName(),"
            + "#template.getProjectId(),'UPDATE')")
    @AuditAction(auditAction = "Update widget config template by id = {{#id}}")
    public WidgetConfigTemplate update(@PathVariable("id") UUID id,
                                       @RequestBody WidgetConfigTemplate template) {
        return service.update(id, template);
    }
}
