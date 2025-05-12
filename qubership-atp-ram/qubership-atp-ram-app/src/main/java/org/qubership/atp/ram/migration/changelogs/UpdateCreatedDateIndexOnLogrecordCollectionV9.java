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

@ChangeLog(version = 9)
@Slf4j
public class UpdateCreatedDateIndexOnLogrecordCollectionV9 {
    /**
     * Update expire time of "created_date" indexes of logrecord collection to 14 days (1209600).
     * @param mongoTemplate mongo template
     */
    @ChangeSet(order = 1)
    public void updateCreatedDateIndex(MongoTemplate mongoTemplate) {
        String processName = UpdateCreatedDateIndexOnLogrecordCollectionV9.class.getName();
        log.info("Start mongo evolution process: {}", processName);

        Index myIndex = new Index()
                .named(MigrationConstants.CREATED_DATE_INDEX_NAME)
                .on(MigrationConstants.CREATED_DATE_FIELD_NAME, Sort.Direction.ASC)
                .expire(1209600)
                .background();

        DefaultIndexOperations indexOperations = new DefaultIndexOperations(
                mongoTemplate,
                MigrationConstants.LOG_RECORDS_COLLECTION_NAME,
                null
        );
        try {
            indexOperations.dropIndex(MigrationConstants.CREATED_DATE_INDEX_NAME);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        indexOperations.ensureIndex(myIndex);

        log.info("End mongo evolution process: {}", processName);
    }
}
