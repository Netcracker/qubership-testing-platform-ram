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

import org.qubership.atp.ram.models.ManualRootCause;
import org.qubership.atp.ram.models.RootCause;
import org.qubership.atp.ram.repositories.ManualRootCauseRepository;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ManualRootCauseService extends CrudService<ManualRootCause> {
    private final ManualRootCauseRepository repository;
    private final RootCauseService rootCauseService;

    @Override
    protected MongoRepository<ManualRootCause, UUID> repository() {
        return repository;
    }

    /**
     * Get all manual root causes with root cause ids or root cause names.
     *
     * @param projectUuid       - project id
     * @param withRootCauseName - get names if true
     * @return all manual root causes
     */
    public List<ManualRootCause> getManualRootCauseByProjectUuid(UUID projectUuid, boolean withRootCauseName) {
        List<ManualRootCause> allManualRootCauses = repository.findAllByProjectId(projectUuid);

        if (withRootCauseName) {
            List<RootCause> allRootCauses = this.rootCauseService.getAllRootCauses();
            return allManualRootCauses.stream()
                    .map(akbRecord -> replaceRootCauseIdWithName(akbRecord, allRootCauses))
                    .collect(Collectors.toList());
        } else {
            return allManualRootCauses;
        }
    }

    /**
     * Replace rootCauseId with root cause name. If root cause with id from manual rc not found set null.
     *
     * @param manualRootCause - instance of {@link ManualRootCause}
     * @param allRootCauses   - list with all root causes
     * @return instance of {@link ManualRootCause}
     */
    private ManualRootCause replaceRootCauseIdWithName(ManualRootCause manualRootCause, List<RootCause> allRootCauses) {
        for (RootCause cause : allRootCauses) {
            if (cause.getUuid().equals(manualRootCause.getRootCauseId())) {
                manualRootCause.setRootCauseId(cause.getUuid());
                return manualRootCause;
            }
        }

        manualRootCause.setRootCauseId(null);
        return manualRootCause;
    }

    public ManualRootCause findByUuid(UUID uuid) {
        return repository.findByUuid(uuid);
    }

    public void deleteByUuid(UUID uuid) {
        repository.deleteByUuid(uuid);
    }

    public UUID getProjectIdByManualRootCauseId(UUID uuid) {
        ManualRootCause rootCause = repository.findProjectByUuid(uuid);
        return rootCause.getProjectId();
    }

}
