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
import java.util.UUID;

import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.qubership.atp.ram.models.RootCause;
import org.qubership.atp.ram.models.RootCauseType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
@JaversSpringDataAuditable
public interface RootCauseRepository extends MongoRepository<RootCause, UUID> {

    RootCause findByUuid(UUID uuid);

    List<RootCause> findByUuidIsIn(List<UUID> uuids);

    void deleteByUuid(UUID uuid);

    List<RootCause> findAllByParentIdIsNullAndProjectIdAndType(UUID projectId, RootCauseType type);

    List<RootCause> findAllByProjectIdAndType(UUID projectId, RootCauseType type);

    RootCause findByProjectIdAndUuid(UUID projectId, UUID uuid);

    List<RootCause> findAllByParentIdIsNullAndType(RootCauseType type);

    List<RootCause> findAllByParentIdAndProjectId(UUID id, UUID projectId);

    List<RootCause> findAllByParentId(UUID parentId);

    List<RootCause> findAllByNameAndParentId(String name, UUID parentId);

    List<RootCause> findAllByProjectIdAndNameAndParentId(UUID projectId, String name, UUID parentId);

    @Query(fields = "{'name': 1}")
    RootCause findNameByUuid(UUID uuid);

    List<RootCause> findByUuidIn(Collection<UUID> ids);

    RootCause findByNameAndProjectId(String name, UUID projectId);

    RootCause findByNameAndType(String name, RootCauseType type);
}
