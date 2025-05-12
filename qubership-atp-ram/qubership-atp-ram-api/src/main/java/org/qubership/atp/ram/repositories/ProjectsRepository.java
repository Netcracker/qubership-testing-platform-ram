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

import org.qubership.atp.ram.models.Project;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectsRepository extends MongoRepository<Project, UUID> {
    Project findByUuid(UUID uuid);

    List<Project> findByUuidIn(Collection<UUID> ids);

    Project findByName(String name);

    @Query(fields = "{'name': 1}")
    Project findNameByUuid(UUID uuid);

    List<Project> findAllByTsgProjectNameExists();

    void deleteByUuid(UUID uuid);
}
