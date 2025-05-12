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

package org.qubership.atp.ram.handlers;

import org.qubership.atp.ram.migration.MigrationConstants;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.DefaultIndexOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

import com.mongodb.client.MongoClient;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class UpdatingIndexesHandler {

    private final MongoTemplate mongoTemplate;

    public UpdatingIndexesHandler() {
        mongoTemplate = null;
    }

    public UpdatingIndexesHandler(MongoClient mongoClient, String database) {
        mongoTemplate = new MongoTemplate(mongoClient, database);
    }

    public UpdatingIndexesHandler(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Compare indexes for collections and recreate if needed.
     */
    public void checkAndRecreateIndexes(long logrecordsIndexExpireDate,
                                        long logrecordsContextIndexExpireDate) {
        log.info("Start checking and updating indexes");
        updateAndRecreateIndexesForCollection(MigrationConstants.LOG_RECORDS_COLLECTION_NAME,
                MigrationConstants.CREATED_DATE_INDEX_NAME,
                logrecordsIndexExpireDate);
        updateAndRecreateIndexesForCollection(MigrationConstants.LOG_RECORD_MESSAGE_PARAMETERS_COLLECTION_NAME,
                MigrationConstants.CREATED_DATE_INDEX_NAME_1,
                logrecordsIndexExpireDate);
        updateAndRecreateIndexesForCollection(MigrationConstants.LOG_RECORD_SCRIPT_REPORT_COLLECTION_NAME,
                MigrationConstants.CREATED_DATE_INDEX_NAME_1,
                logrecordsIndexExpireDate);

        updateAndRecreateIndexesForCollection(MigrationConstants.LOG_RECORD_CONTEXT_VARIABLES_COLLECTION_NAME,
                MigrationConstants.CREATED_DATE_INDEX_NAME,
                logrecordsContextIndexExpireDate);
        updateAndRecreateIndexesForCollection(MigrationConstants.LOG_RECORD_STEP_CONTEXT_VARIABLES_COLLECTION_NAME,
                MigrationConstants.CREATED_DATE_INDEX_NAME,
                logrecordsContextIndexExpireDate);
        updateAndRecreateIndexesForCollection(MigrationConstants.LOG_RECORD_BROWSER_CONSOLE_LOGS_COLLECTION_NAME,
                MigrationConstants.CREATED_DATE_INDEX_NAME,
                logrecordsContextIndexExpireDate);
        log.info("Finish checking and updating indexes");
    }

    /**
     * Find indexes for updating and update if value expireDate was changed in properties.
     *
     * @param collectionName name of collection
     * @param indexName name of index
     * @param newExpiredDate new expire date
     */
    private void updateAndRecreateIndexesForCollection(String collectionName, String indexName, long newExpiredDate) {
        log.info("Start checking and updating indexes for collectionName = {}, indexName = {}, newExpiredDate = {}",
                collectionName, indexName, newExpiredDate);
        mongoTemplate.getCollection(collectionName)
                .listIndexes().forEach(indexDocument -> {
                    if (indexName.equals(indexDocument.getString(MigrationConstants.NAME_INDEX_FIELD))) {
                        long expiredDateFromDb = indexDocument.containsKey(MigrationConstants.EXPIRE_DATE_INDEX_FIELD)
                                ? indexDocument.getLong(MigrationConstants.EXPIRE_DATE_INDEX_FIELD) : 0;
                        if (newExpiredDate > 0 && newExpiredDate != expiredDateFromDb) {
                            log.info("ExpireDate will be update for collection {}. new value = {}, oldValue = {}",
                                    collectionName, newExpiredDate, expiredDateFromDb);
                            recreateIndex(collectionName,
                                    indexName,
                                    newExpiredDate);
                        }
                    }
                });
        log.info("Finish checking and updating indexes for collectionName = {}, indexName = {}, newExpiredDate = {}",
                collectionName, indexName, newExpiredDate);
    }

    /**
     * Recreate index.
     *
     * @param collectionName name of collection
     * @param indexName name of index
     * @param newExpiredDate new expire date
     */
    protected void recreateIndex(String collectionName, String indexName, long newExpiredDate) {
        Index myIndex = new Index()
                .named(indexName)
                .on(MigrationConstants.CREATED_DATE_FIELD_NAME, Sort.Direction.ASC)
                .expire(newExpiredDate)
                .background();

        DefaultIndexOperations indexOperations = new DefaultIndexOperations(
                mongoTemplate,
                collectionName,
                null
        );
        try {
            indexOperations.dropIndex(indexName);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        indexOperations.ensureIndex(myIndex);
    }
}
