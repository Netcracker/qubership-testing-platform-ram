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
import java.util.UUID;

import org.javers.core.Javers;
import org.javers.repository.jql.JqlQuery;
import org.javers.repository.jql.QueryBuilder;
import org.javers.shadow.Shadow;
import org.qubership.atp.ram.controllers.api.dto.history.HistoryItemTypeDto;
import org.qubership.atp.ram.exceptions.history.RamHistoryFailReasonDoesNotExistException;
import org.qubership.atp.ram.exceptions.history.RamHistoryRevisionRestoreException;
import org.qubership.atp.ram.models.FailPattern;
import org.qubership.atp.ram.services.FailPatternService;
import org.qubership.atp.ram.services.RootCauseService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FailPatternRestoreHistoryService extends AbstractRestoreHistoryService<FailPattern> {

    private final RootCauseService rootCauseService;

    public FailPatternRestoreHistoryService(
            Javers javers,
            FailPatternService failPatternService,
            ValidateReferenceExistsService<FailPattern> validateReferenceExistsService,
            RootCauseService rootCauseService) {
        super(javers, failPatternService, validateReferenceExistsService);
        this.rootCauseService = rootCauseService;
    }

    @Override
    public HistoryItemTypeDto getItemType() {
        return HistoryItemTypeDto.FAILPATTERN;
    }

    @Override
    public Class<FailPattern> getEntityClass() {
        return FailPattern.class;
    }

    @Override
    public void restoreToRevision(UUID id, long revisionId) {
        JqlQuery query = QueryBuilder.byInstanceId(id, getEntityClass())
                .withVersion(revisionId)
                .build();
        FailPattern actualObject = getObject(id);
        if (actualObject.getFailReasonId() != null && !rootCauseService.existsById(actualObject.getFailReasonId())) {
            throw new RamHistoryFailReasonDoesNotExistException();
        }
        validateReferenceExistsService.validateEntity(actualObject);
        List<Shadow<Object>> shadows = javers.findShadows(query);
        if (CollectionUtils.isEmpty(shadows)) {
            log.error("No shadows found for entity '{}' with revision='{}' and uuid='{}'",
                    getItemType(), revisionId, id);
            throw new RamHistoryRevisionRestoreException();
        }
        Shadow<Object> objectShadow = shadows.iterator().next();
        Object restoredObject = restoreValues(objectShadow, actualObject);
        saveRestoredObject((FailPattern) restoredObject);
    }


}
