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

package org.qubership.atp.ram.repositories;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.qubership.atp.ram.models.FailPattern;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
@JaversSpringDataAuditable
public interface FailPatternRepository extends MongoRepository<FailPattern, UUID> {

    FailPattern findByUuid(UUID id);

    List<FailPattern> findByUuidIn(Collection<UUID> id);

    void deleteByUuid(UUID id);

    List<FailPattern> findAllByProjectId(UUID projectId, Pageable pageable);

    List<FailPattern> findAllByProjectId(UUID projectId);

    FailPattern findByProjectIdAndUuid(UUID projectId, UUID uuid);

    long countByProjectId(UUID projectId);

    FailPattern findProjectByUuid(UUID uuid);

    @Query(fields = "{name : 1, uuid : 1}")
    Page<FailPattern> findAllNamesByProjectIdInAndNameContainsIgnoreCase(Set<UUID> projectIds, String name,
                                                                         Pageable pageable);

    @Query(fields = "{name : 1, uuid : 1}")
    Page<FailPattern> findAllNamesByNameContainsIgnoreCase(String name, Pageable pageable);
}
