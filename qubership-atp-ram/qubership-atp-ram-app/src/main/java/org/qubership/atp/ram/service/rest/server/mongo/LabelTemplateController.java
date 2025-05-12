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
import org.qubership.atp.ram.services.LabelTemplateNodeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@CrossOrigin
@RequestMapping("/api/label-templates")
@RestController()
@RequiredArgsConstructor
public class LabelTemplateController {

    private final LabelTemplateNodeService service;


    @DeleteMapping(value = "/{uuid}")
    @PreAuthorize("@entityAccess.checkAccess(@labelTemplateNodeService.getProjectIdByLabelTemplateId("
            + "#uuid),'DELETE')")
    @AuditAction(auditAction = "Delete label template by id = {{#uuid}}")
    public void delete(@PathVariable("uuid") UUID uuid) {
        service.deleteLabelTemplate(uuid);
    }
}
