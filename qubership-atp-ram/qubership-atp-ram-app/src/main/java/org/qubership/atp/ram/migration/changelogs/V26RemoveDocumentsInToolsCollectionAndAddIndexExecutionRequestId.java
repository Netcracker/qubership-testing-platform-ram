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
import org.qubership.atp.ram.models.ToolsInfo;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.DefaultIndexOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Query;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ChangeLog(version = 26)
public class V26RemoveDocumentsInToolsCollectionAndAddIndexExecutionRequestId {

    private static final String COLLECTION_NAME = "tools";
    private static final String INDEX_FIELD = "executionRequestId";

    /**
     * Add Execution request id for Tools.
     */
    @ChangeSet(order = 1)
    public void removeDocumentsInToolsCollection(MongoTemplate mongoTemplate) {
        String processName = V26RemoveDocumentsInToolsCollectionAndAddIndexExecutionRequestId.class.getName();
        log.info("Start mongo evolution process: {}", processName);
        try {
            Query query = new Query();
            mongoTemplate.remove(query, ToolsInfo.class);
        } catch (Exception e) {
            log.error("Problem with deleted collections", e);
        }
        log.info("End mongo evolution process: {}", processName);
    }

    /**
     * Add index for ERId.
     */
    @ChangeSet(order = 2)
    public void addIndexExecutionRequestId(MongoTemplate mongoTemplate) {
        log.info("Start adding index execution request id");
        Index myIndex = new Index()
                .named(INDEX_FIELD)
                .on(INDEX_FIELD, Sort.Direction.ASC)
                .background();
        DefaultIndexOperations indexOperations = new DefaultIndexOperations(
                mongoTemplate,
                COLLECTION_NAME,
                null
        );
        try {
            indexOperations.dropIndex(INDEX_FIELD);
        } catch (Exception e) {
            log.error("Index {} is absent in the collection {}", INDEX_FIELD, COLLECTION_NAME);
        }
        indexOperations.ensureIndex(myIndex);
        log.info("End mongo evolution process");
    }
}
