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

package org.qubership.atp.ram.service.history;

import java.util.Date;
import java.util.UUID;

import org.qubership.atp.ram.models.DateAuditorEntity;
import org.qubership.atp.ram.services.CrudService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ConcurrentModificationService {

    /**
     * Check modify date was changing.
     *
     * @param modifyDateFromRequest modify date from request
     * @param modifyDateFromMongo   modify date from db
     * @return true if modify date different
     */
    public <T> Boolean isModifyDateChanging(T modifyDateFromRequest, T modifyDateFromMongo) {
        if (modifyDateFromRequest != null
                && !modifyDateFromRequest.equals(modifyDateFromMongo)) {
            return true;
        }
        return false;
    }

    /**
     * Provide http status for dirty reading.
     *
     * @param requestEntityId       entity uuid from request
     * @param modifyDateFromRequest modify date from request
     * @param service               abstract service for entity
     * @return http status
     */
    public <T extends DateAuditorEntity> HttpStatus getConcurrentModificationHttpStatus(
            UUID requestEntityId, Date modifyDateFromRequest, CrudService<T> service,
            Boolean skipTypeCheck) {
        DateAuditorEntity entityFromMongo = service.get(requestEntityId);
        Boolean isConcurrentModification = entityFromMongo != null
                && isModifyDateChanging(modifyDateFromRequest, entityFromMongo.getModifiedWhen());

        if (Boolean.TRUE.equals(isConcurrentModification)) {
            return skipTypeCheck == null || Boolean.FALSE.equals(skipTypeCheck)
                    ? HttpStatus.CONFLICT
                    : HttpStatus.IM_USED;
        }
        return HttpStatus.OK;
    }

    public <T extends DateAuditorEntity> HttpStatus getConcurrentModificationHttpStatus(
            UUID requestEntityId, Date modifyDateFromRequest, CrudService<T> service) {
        return getConcurrentModificationHttpStatus(requestEntityId, modifyDateFromRequest, service, false);
    }
}
