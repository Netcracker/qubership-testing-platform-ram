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

package org.qubership.atp.ram.services;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.qubership.atp.ram.models.AkbRecord;
import org.qubership.atp.ram.models.RootCause;
import org.qubership.atp.ram.repositories.AkbRecordsRepository;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AkbRecordsService extends CrudService<AkbRecord> {

    private final AkbRecordsRepository repository;
    private final RootCauseService rootCauseService;

    @Override
    protected MongoRepository<AkbRecord, UUID> repository() {
        return repository;
    }

    /**
     * Get all akb records with root cause ids or root cause names.
     *
     * @param projectUuid       - project id
     * @param withRootCauseName - get names if true
     * @return all akb records
     */
    public List<AkbRecord> getRecordsByProjectUuid(UUID projectUuid, boolean withRootCauseName) {
        List<AkbRecord> allAkbRecords = repository.getAkbRecordsByProjectId(projectUuid);

        if (withRootCauseName) {
            List<RootCause> allRootCauses = this.rootCauseService.getAllRootCauses();
            return allAkbRecords.stream()
                    .map(akbRecord -> replaceRootCauseIdWithName(akbRecord, allRootCauses))
                    .collect(Collectors.toList());
        } else {
            return allAkbRecords;
        }
    }

    /**
     * Remove defectUuid from AKB records.
     *
     * @param defectUuid for removed
     */
    public void removeDefectFromAkbRecord(UUID defectUuid) {
        List<AkbRecord> akbRecords = repository.getAkbRecordsByUuid(defectUuid);
        akbRecords.forEach(akbRecord -> {
            akbRecord.setDefectId(null);
            save(akbRecord);
        });
        saveAll(akbRecords);
    }

    /**
     * To get all akb records with root cause names.
     *
     * @return list of akb records
     */
    public List<AkbRecord> getAllWithRootCauseName() {
        List<AkbRecord> allAkbRecords = getAll();
        List<RootCause> allRootCauses = this.rootCauseService.getAllRootCauses();

        return allAkbRecords.stream()
                .map(akbRecord -> replaceRootCauseIdWithName(akbRecord, allRootCauses))
                .collect(Collectors.toList());
    }

    /**
     * Replace rootCauseId with root cause name. If root cause with id from akb not found set null.
     *
     * @param akbRecord     - instance of {@link AkbRecord}
     * @param allRootCauses - list with all root causes
     * @return instance of {@link AkbRecord}
     */
    private AkbRecord replaceRootCauseIdWithName(AkbRecord akbRecord, List<RootCause> allRootCauses) {
        for (RootCause cause : allRootCauses) {
            if (cause.getUuid().equals(akbRecord.getRootCauseId())) {
                akbRecord.setRootCauseId(cause.getUuid());
                return akbRecord;
            }
        }

        akbRecord.setRootCauseId(null);
        return akbRecord;
    }

    public UUID getProjectIdByAkbRecordId(UUID id) {
        return repository.findProjectIdByUuid(id).getProjectId();
    }

    public AkbRecord findByUuid(UUID uuid) {
        return repository.findByUuid(uuid);
    }

    public void deleteByUuid(UUID uuid) {
        repository.deleteByUuid(uuid);
    }
}
