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

package org.qubership.atp.ram.migration.changelogs;

import org.qubership.atp.ram.migration.MigrationConstants;
import org.qubership.atp.ram.migration.mongoevolution.java.annotation.ChangeLog;
import org.qubership.atp.ram.migration.mongoevolution.java.annotation.ChangeSet;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.DefaultIndexOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

import lombok.extern.slf4j.Slf4j;

@ChangeLog(version = 21)
@Slf4j
public class V21AddCreatedDateTimeToLiveIndexForLogRecordMessageParameters {


    /**
     * Add time to live index on createdDate.
     *
     * @param mongoTemplate mongo template
     */
    @ChangeSet(order = 1)
    public void addTimeToLiveIndex(MongoTemplate mongoTemplate) {
        String processName = V21AddCreatedDateTimeToLiveIndexForLogRecordMessageParameters.class.getName();
        log.info("Start mongo evolution process: {}", processName);
        Index expireIndex = new Index()
                .on(MigrationConstants.CREATED_DATE_FIELD_NAME, Sort.Direction.ASC)
                // expire takes seconds, 1209600 seconds is 14 days
                .expire(1209600);
        DefaultIndexOperations indexOperations = new DefaultIndexOperations(
                mongoTemplate,
                MigrationConstants.LOG_RECORD_MESSAGE_PARAMETERS_COLLECTION_NAME,
                null
        );
        indexOperations.ensureIndex(expireIndex);
        log.info("End mongo evolution process: {}", processName);
    }
}
