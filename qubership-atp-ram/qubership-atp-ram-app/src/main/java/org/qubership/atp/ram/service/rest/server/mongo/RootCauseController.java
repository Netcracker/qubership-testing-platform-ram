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

import static java.util.Objects.nonNull;

import java.util.List;
import java.util.UUID;

import javax.validation.Valid;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.atp.ram.model.request.RootCauseUpsertRequest;
import org.qubership.atp.ram.models.RootCause;
import org.qubership.atp.ram.models.RootCauseTreeNode;
import org.qubership.atp.ram.service.history.ConcurrentModificationService;
import org.qubership.atp.ram.services.RootCauseService;
import org.qubership.atp.ram.validators.RootCauseUpsertRequestValidator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RequestMapping("/api/rootcauses")
@RestController
@RequiredArgsConstructor
public class RootCauseController /*implements RootCauseControllerApi*/ {

    private final RootCauseService service;
    private final RootCauseUpsertRequestValidator rootCauseUpsertValidator;
    private final ConcurrentModificationService concurrentModificationService;

    /**
     * Bind validators.
     *
     * @param binder binder
     */
    @InitBinder
    public void dataBindings(WebDataBinder binder) {
        Object target = binder.getTarget();
        if (target != null && RootCauseUpsertRequest.class.equals(target.getClass())) {
            binder.addValidators(rootCauseUpsertValidator);
        }
    }

    @Deprecated
    @GetMapping
    @AuditAction(auditAction = "Get all root causes")
    public List<RootCause> getAllRootCauses() {
        return service.getAllRootCauses();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).FAIL_REASON.getName(),"
            + "@rootCauseService.get(#id).getProjectId(),'READ')")
    public RootCause getRootCause(@PathVariable UUID id) {
        return service.getById(id);
    }

    @GetMapping(value = "/tree")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).FAIL_REASON.getName(),"
            + "#projectId,'READ')")
    @AuditAction(auditAction = "Get root causes tree nodes by projectId = {{#projectId}}")
    public List<RootCauseTreeNode> getRootCauseTree(@RequestParam UUID projectId,
                                                    @RequestParam(required = false) String filterDisabled) {
        return service.getRootCauseTree(projectId, nonNull(filterDisabled));
    }

    @DeleteMapping(value = "/{id}")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).FAIL_REASON.getName(),"
            + "@rootCauseService.get(#id).getProjectId(),'DELETE')")
    @AuditAction(auditAction = "Delete root cause '{{#id}}'")
    public void delete(@PathVariable UUID id) {
        service.deleteById(id);
    }

    @PostMapping
    @PreAuthorize("(#request.getProjectId() == null) ? true : "
            + "@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).FAIL_REASON.getName(),"
            + "#request.getProjectId(),'CREATE')")
    @AuditAction(auditAction = "Create root cause for project '{{#request.projectId}}'")
    public ResponseEntity<RootCause> create(@Valid @RequestBody RootCauseUpsertRequest request) {
        return new ResponseEntity<>(service.create(request), HttpStatus.CREATED);
    }

    /**
     * Update model RootCause.
     */
    @PutMapping("/{id}")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).FAIL_REASON.getName(),"
            + "#request.getProjectId(),'UPDATE')")
    @AuditAction(auditAction = "Update root cause '{{#id}}'")
    public ResponseEntity<RootCause> update(@PathVariable UUID id,
                                            @Valid @RequestBody RootCauseUpsertRequest request) {
        HttpStatus httpStatus = concurrentModificationService.getConcurrentModificationHttpStatus(id,
               request.getModifiedWhen(), service);
        RootCause rootCause = service.update(id, request);
        return new ResponseEntity<>(rootCause, httpStatus);
    }

    /**
     * Update disabled field model RootCause.
     */
    @Deprecated
    @PostMapping("/{id}/disable")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).FAIL_REASON.getName(),"
            + "@rootCauseService.get(#id).getProjectId(),'UPDATE')")
    @AuditAction(auditAction = "Disable root cause '{{#id}}'")
    public void disableRootCause(@PathVariable UUID id) {
        service.disable(id);
    }

    /**
     * Update disabled field model RootCause.
     */
    @Deprecated
    @PostMapping("/{id}/enable")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).FAIL_REASON.getName(),"
            + "@rootCauseService.get(#id).getProjectId(),'UPDATE')")
    @AuditAction(auditAction = "Enable root cause '{{#id}}'")
    public void enableRootCause(@PathVariable UUID id) {
        service.enable(id);
    }
}
