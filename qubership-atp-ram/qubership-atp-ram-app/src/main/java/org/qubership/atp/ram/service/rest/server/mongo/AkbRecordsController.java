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

import org.qubership.atp.ram.models.AkbRecord;
import org.qubership.atp.ram.services.AkbRecordsService;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/akbRecords")
@RestController()
@RequiredArgsConstructor
public class AkbRecordsController /*implements AkbRecordsControllerApi*/ {

    private final AkbRecordsService service;

    /**
     * To get all akb records with root cause ids or root cause names.
     *
     * @param withRootCauseName get names if true
     * @return all akb records
     */
    @GetMapping
    @PreAuthorize("@entityAccess.isAdmin()")
    public List<AkbRecord> getAll(@RequestParam(value = "withRootCauseName",
            required = false,
            defaultValue = "false") boolean withRootCauseName) {
        if (withRootCauseName) {
            return service.getAllWithRootCauseName();
        } else {
            return service.getAll();
        }
    }

    @GetMapping(value = "/{recordUuid}")
    @PreAuthorize("@entityAccess.checkAccess(@akbRecordsService.getProjectIdByAkbRecordId(#uuid), 'READ')")
    public AkbRecord getByUuid(@PathVariable("recordUuid") UUID uuid) {
        return service.findByUuid(uuid);
    }

    @DeleteMapping(value = "/delete/{uuid}")
    @PreAuthorize("@entityAccess.checkAccess(@akbRecordsService.getProjectIdByAkbRecordId(#uuid),'DELETE')")
    public void delete(@PathVariable("uuid") UUID uuid) {
        service.deleteByUuid(uuid);
    }

    @PostMapping(value = "/create")
    @PreAuthorize("@entityAccess.checkAccess(#akbRecord.getProjectId(),'CREATE')")
    public AkbRecord create(@RequestBody AkbRecord akbRecord) {
        return service.save(akbRecord);
    }

    @PutMapping(value = "/save")
    @PreAuthorize("@entityAccess.checkAccess(#akbRecord.getProjectId(),'UPDATE')")
    public AkbRecord save(@RequestBody AkbRecord akbRecord) {
        return service.save(akbRecord);
    }

    @GetMapping(value = "/project/{projectId}")
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid,'READ')")
    public List<AkbRecord> getRecordsByProjectUuid(@PathVariable("projectId") UUID projectUuid,
                                                   @RequestParam(value = "withRootCauseName",
                                                           required = false,
                                                           defaultValue = "false") boolean withRootCauseName) {
        return service.getRecordsByProjectUuid(projectUuid, withRootCauseName);
    }
}
