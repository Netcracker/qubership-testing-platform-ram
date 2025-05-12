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

package org.qubership.atp.ram.repositories.impl;

import java.util.Map;
import java.util.UUID;

import org.qubership.atp.ram.repositories.CustomRamObjectRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@Repository
public class CustomRamObjectImpl implements FieldConstants, CustomRamObjectRepository {

    protected final MongoTemplate mongoTemplate;

    @Override
    public void updateAnyFieldsRamObjectByIdDocument(UUID id,
                                                     Map<String, Object> fieldsToUpdate,
                                                     Class<?> entityClass) {
        Update update = new Update();
        fieldsToUpdate.forEach(update::set);

        UpdateResult updateResult = mongoTemplate.updateFirst(queryIsId(id), update, entityClass);
        log.debug("Abstract update fields with id: {}, fields to update: {}, for class entity: {}. Update results: {}",
                id, fieldsToUpdate, entityClass, updateResult);
    }

    public Query queryIsId(UUID id) {
        return new Query().addCriteria(Criteria.where(_ID).is(id));
    }
}
