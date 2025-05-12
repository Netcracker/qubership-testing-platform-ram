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

package org.qubership.atp.ram.service.history.impl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.javers.core.Javers;
import org.javers.repository.jql.JqlQuery;
import org.javers.repository.jql.QueryBuilder;
import org.javers.shadow.Shadow;
import org.qubership.atp.auth.springbootstarter.exceptions.AtpEntityNotFoundException;
import org.qubership.atp.ram.controllers.api.dto.history.AbstractCompareEntityDto;
import org.qubership.atp.ram.controllers.api.dto.history.CompareEntityResponseDto;
import org.qubership.atp.ram.converters.history.AbstractVersioningMapper;
import org.qubership.atp.ram.models.DateAuditorEntity;
import org.qubership.atp.ram.service.history.VersioningHistoryService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractVersioningHistoryService<S extends DateAuditorEntity,
        D extends AbstractCompareEntityDto>
        implements VersioningHistoryService<S, D> {

    private Javers javers;
    private AbstractVersioningMapper<S, D> abstractVersioningMapper;

    public AbstractVersioningHistoryService(Javers javers,
                                            AbstractVersioningMapper abstractVersioningMapper) {
        this.abstractVersioningMapper = abstractVersioningMapper;
        this.javers = javers;
    }

    /**
     * Returns collection of entities with requested revision numbers.
     *
     * @param id       uuid of entity in DB.
     * @param versions collection of requested revision numbers.
     * @return collection of CompareEntityResponse.
     */
    @Override
    public List<CompareEntityResponseDto> getEntitiesByVersions(UUID id, List<String> versions) {
        return versions.stream()
                .map(version -> getEntityByVersion(version, id))
                .collect(Collectors.toList());
    }

    private CompareEntityResponseDto buildCompareEntity(String revision,
                                                        Optional<Shadow<Object>> entity) {
        log.debug("version={}, entity={}", revision, entity);
        Shadow<Object> objectShadow = entity.get();
        D resolvedEntity = abstractVersioningMapper.map((S) objectShadow.get());
        CompareEntityResponseDto response = new CompareEntityResponseDto();
        response.setCompareEntity(resolvedEntity);
        response.setRevision(revision);
        return response;
    }

    /**
     * Returns collection of entities with requested revision number.
     *
     * @param id       uuid of entity in DB.
     * @param version  revision number.
     * @return CompareEntityResponseDto.
     */
    public CompareEntityResponseDto getEntityByVersion(String version, UUID id) {
        Optional<Shadow<Object>> entity = getShadow(version, id);
        if (entity.isPresent()) {
            return buildCompareEntity(version, entity);
        } else {
            log.error("Failed to find entity with id: {}", id);
            throw new AtpEntityNotFoundException("entity", id);
        }
    }

    private Optional<Shadow<Object>> getShadow(String version, UUID uuid) {
        JqlQuery query = QueryBuilder.byInstanceId(uuid, getEntityClass())
                .withVersion(Long.parseLong(version))
                .withScopeDeepPlus()
                .build();
        List<Shadow<Object>> shadows = javers.findShadows(query);
        log.debug("Shadows found : {}", shadows);
        return shadows.stream().findFirst();
    }
}
