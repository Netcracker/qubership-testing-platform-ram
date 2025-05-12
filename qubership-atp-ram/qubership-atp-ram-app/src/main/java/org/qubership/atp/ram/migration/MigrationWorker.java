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

package org.qubership.atp.ram.migration;

import static org.qubership.atp.ram.migration.MigrationConstants._ID;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.util.Collection;

import org.qubership.atp.ram.models.RamObject;
import org.springframework.data.mongodb.core.FindAndReplaceOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class MigrationWorker {

    private final MongoTemplate mongoTemplate;

    /**
     * Insert new or fully update existed entities.
     *
     * @param entities input entities
     * @param <T> entities type
     */
    public <T extends RamObject> void upsert(Collection<T> entities) {
        log.debug("Start upsert process for entities: {}", entities);
        final FindAndReplaceOptions upsertOptions = FindAndReplaceOptions.options().upsert().returnNew();
        entities.forEach(entity -> {
            log.debug("Upsert entity: {}", entity);
            T result = mongoTemplate.findAndReplace(new Query(where(_ID).is(entity.getUuid())), entity, upsertOptions);
            log.debug("Result: {}", result);
        });
        log.debug("End upsert process");
    }
}
