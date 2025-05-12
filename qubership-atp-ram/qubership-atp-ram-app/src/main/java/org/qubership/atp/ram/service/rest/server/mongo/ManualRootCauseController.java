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

import org.qubership.atp.ram.models.ManualRootCause;
import org.qubership.atp.ram.services.ManualRootCauseService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@Deprecated
@CrossOrigin
@RequestMapping("/api/manualRootCause")
@RestController()
@RequiredArgsConstructor
public class ManualRootCauseController /*implements ManualRootCauseControllerApi*/ {

    private final ManualRootCauseService service;

    @GetMapping
    @PreAuthorize("@entityAccess.isAdmin()")
    public List<ManualRootCause> getAll() {
        return service.getAll();
    }

    @GetMapping(value = "/{uuid}")
    @PreAuthorize("@entityAccess.checkAccess(@manualRootCauseService.getProjectIdByManualRootCauseId(#uuid),'READ')")
    public ManualRootCause getByUuid(@PathVariable("uuid") UUID uuid) {
        return service.findByUuid(uuid);
    }

    @DeleteMapping(value = "/delete/{uuid}")
    @PreAuthorize("@entityAccess.checkAccess(@manualRootCauseService.getProjectIdByManualRootCauseId(#uuid),'DELETE')")
    public void delete(@PathVariable("uuid") UUID uuid) {
        service.deleteByUuid(uuid);
    }

    @PostMapping(value = "/create")
    @PreAuthorize("@entityAccess.checkAccess(#manualRootCause.getProjectId(),'CREATE')")
    public ManualRootCause create(@RequestBody ManualRootCause manualRootCause) {
        return service.save(manualRootCause);
    }

    @PutMapping(value = "/save")
    @PreAuthorize("@entityAccess.checkAccess(#manualRootCause.getProjectId(),'UPDATE')")
    public ManualRootCause save(@RequestBody ManualRootCause manualRootCause) {
        return service.save(manualRootCause);
    }

    @GetMapping(value = "/project/{projectUuid}")
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid,'READ')")
    public List<ManualRootCause> getManualRootCauseByProjectUuid(
            @PathVariable("projectUuid") UUID projectUuid,
            @RequestParam(value = "withRootCauseName",
                    required = false,
                    defaultValue = "false") boolean withRootCauseName) {
        return service.getManualRootCauseByProjectUuid(projectUuid, withRootCauseName);
    }
}
