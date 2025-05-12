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

import org.qubership.atp.ram.migration.mongoevolution.java.annotation.ChangeLog;
import org.qubership.atp.ram.migration.mongoevolution.java.annotation.ChangeSet;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.DefaultIndexOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ChangeLog(version = 25)
public class V25AddLastUpdatedIndexForLogRecord {


    /**
     * Add ttl index for lastUpdated field to logrecord collection.
     */
    @ChangeSet(order = 1)
    public void updateLastUpdatedIndex(MongoTemplate mongoTemplate) {
        String processName = V25AddLastUpdatedIndexForLogRecord.class.getName();
        log.info("Start mongo evolution process: {}", processName);

        Index myIndex = new Index()
                .named("last_updated")
                .on("lastUpdated", Sort.Direction.ASC)
                .expire(1209600)
                .background();

        DefaultIndexOperations indexOperations = new DefaultIndexOperations(
                mongoTemplate,
                "logrecord",
                null
        );
        try {
            indexOperations.dropIndex("last_updated");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        indexOperations.ensureIndex(myIndex);

        log.info("End mongo evolution process: {}", processName);
    }
}
