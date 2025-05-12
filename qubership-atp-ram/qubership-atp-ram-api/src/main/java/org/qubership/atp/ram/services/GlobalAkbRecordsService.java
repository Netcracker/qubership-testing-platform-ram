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

import org.qubership.atp.ram.models.GlobalAkbRecord;
import org.qubership.atp.ram.models.RootCause;
import org.qubership.atp.ram.repositories.GlobalAkbRecordsRepository;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class GlobalAkbRecordsService extends CrudService<GlobalAkbRecord> {

    private final RootCauseService rootCauseService;
    private final GlobalAkbRecordsRepository recordsRepository;

    @Override
    protected MongoRepository<GlobalAkbRecord, UUID> repository() {
        return recordsRepository;
    }

    /**
     * Get all akb records with root cause names.
     *
     * @return list of akb records
     */
    public List<GlobalAkbRecord> getAllWithRootCauseName() {
        List<GlobalAkbRecord> allGlobalAkbRecords = getAll();
        List<RootCause> allRootCauses = this.rootCauseService.getAllRootCauses();

        return allGlobalAkbRecords.stream()
                .map(globalAkbRecord -> replaceRootCauseIdWithName(globalAkbRecord, allRootCauses))
                .collect(Collectors.toList());
    }

    /**
     * Replace rootCauseId with root cause name. If root cause with id from global akb not found set null.
     *
     * @param globalAkbRecord - instance of {@link GlobalAkbRecord}
     * @param allRootCauses   - list with all root causes
     * @return instance of {@link GlobalAkbRecord}
     */
    private GlobalAkbRecord replaceRootCauseIdWithName(GlobalAkbRecord globalAkbRecord,
                                                       List<RootCause> allRootCauses) {
        for (RootCause cause : allRootCauses) {
            if (cause.getUuid().equals(globalAkbRecord.getRootCauseId())) {
                globalAkbRecord.setRootCauseId(cause.getUuid());
                return globalAkbRecord;
            }
        }

        globalAkbRecord.setRootCauseId(null);
        return globalAkbRecord;
    }

    public void deleteByUuid(UUID uuid) {
        recordsRepository.deleteByUuid(uuid);
    }

    public GlobalAkbRecord findByUuid(UUID uuid) {
        return recordsRepository.findByUuid(uuid);
    }

    /**
     * Returns project id by record id.
     */
    public UUID getProjectIdByGlobalAkbRecordId(UUID uuid) {
        GlobalAkbRecord globalAkbRecord = recordsRepository.findByUuid(uuid);
        RootCause rootCause = rootCauseService.get(globalAkbRecord.getRootCauseId());
        return rootCause.getProjectId();
    }

    /**
     * Returns project id by record.
     */
    public UUID getProjectIdByGlobalAkbRecord(GlobalAkbRecord globalAkbRecord) {
        RootCause rootCause = rootCauseService.get(globalAkbRecord.getRootCauseId());
        return rootCause.getProjectId();
    }
}
