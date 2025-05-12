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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.qubership.atp.ram.models.Defect;
import org.qubership.atp.ram.repositories.DefectsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefectsService extends CrudService<Defect> {
    private static final Logger LOG = LoggerFactory.getLogger(DefectsService.class);

    private final DefectsRepository repository;

    @Override
    protected MongoRepository<Defect, UUID> repository() {
        return repository;
    }


    @Override
    public List<Defect> getAll() {
        return repository.findAll();
    }

    public List<Defect> getDefectsByProjectUuid(UUID projectUuid) {
        return repository.findAllByProjectId(projectUuid);
    }

    public UUID getProjectIdByDefectId(UUID id) {
        return repository.findProjectIdByUuid(id).getProjectId();
    }

    /**
     * We get a list of project id which includes all defects, it needs for check permissions on
     * many projects.
     */
    public Set<UUID> getSetProjectIdByListDefectId(List<UUID> idList) {
        return idList.stream()
                .map(this::getProjectIdByDefectId)
                .collect(Collectors.toSet());
    }

    public Defect getByUuid(UUID defectUuid) {
        return repository.findByUuid(defectUuid);
    }

    public void deleteByUuid(UUID uuid) {
        repository.deleteByUuid(uuid);
    }
}
