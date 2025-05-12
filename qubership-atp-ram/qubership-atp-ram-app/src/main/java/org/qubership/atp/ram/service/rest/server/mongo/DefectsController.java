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

import org.qubership.atp.ram.models.Defect;
import org.qubership.atp.ram.services.AkbRecordsService;
import org.qubership.atp.ram.services.DefectsService;
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

@Deprecated
@RequestMapping("/api/defects")
@RestController()
@RequiredArgsConstructor
public class DefectsController /*implements DefectsControllerApi*/ {

    private final DefectsService service;
    private final AkbRecordsService akbRecordsService;

    @GetMapping
    @PreAuthorize("@entityAccess.isAdmin()")
    public List<Defect> getAll() {
        return service.getAll();
    }

    @GetMapping(value = "/{uuid}")
    @PreAuthorize("@entityAccess.checkAccess(@defectsService.getProjectIdByDefectId(#uuid),'READ')")
    public Defect getByUuid(@PathVariable("uuid") UUID uuid) {
        return service.getByUuid(uuid);
    }

    @DeleteMapping(value = "/delete/{uuid}")
    @PreAuthorize("@entityAccess.checkAccess(@defectsService.getProjectIdByDefectId(#uuid),'DELETE')")
    public void delete(@PathVariable("uuid") UUID uuid) {
        akbRecordsService.removeDefectFromAkbRecord(uuid);
        service.deleteByUuid(uuid);
    }

    @PostMapping(value = "/create")
    @PreAuthorize("@entityAccess.checkAccess(#defect.getProjectId(),'CREATE')")
    public Defect create(@RequestBody Defect defect) {
        return service.save(defect);
    }

    @PutMapping(value = "/save")
    @PreAuthorize("@entityAccess.checkAccess(#defect.getProjectId(),'UPDATE')")
    public Defect save(@RequestBody Defect defect) {
        return service.save(defect);
    }

    @GetMapping(value = "/project/{projectUuid}")
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid,'READ')")
    public List<Defect> getDefectsByProjectUuid(@PathVariable("projectUuid") UUID projectUuid) {
        return service.getDefectsByProjectUuid(projectUuid);
    }
}
