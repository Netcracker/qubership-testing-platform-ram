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

import java.util.concurrent.TimeUnit;

import org.qubership.atp.ram.migration.mongoevolution.java.annotation.ChangeLog;
import org.qubership.atp.ram.migration.mongoevolution.java.annotation.ChangeSet;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@ChangeLog(version = 16)
public class V16AddTtlIndexForJointExecutionRequests {

    private static final String COLLECTION_NAME = "jointExecutionRequests";
    private static final String DELETE_FIELD = "startDate";

    /**
     * Add indexes to the field 'delete'.
     *
     * @param mongoDatabase the mongo database
     */
    @ChangeSet(order = 1)
    public void addIndexToCollectionFields(MongoDatabase mongoDatabase) {
        String processName = V16AddTtlIndexForJointExecutionRequests.class.getName();
        log.info("Start mongo evolution process: {}", processName);
        IndexOptions indexOptions = new IndexOptions()
                .expireAfter(7L, TimeUnit.DAYS)
                .background(true);
        mongoDatabase.getCollection(COLLECTION_NAME).createIndex(Indexes.ascending(DELETE_FIELD), indexOptions);
        log.info("End mongo evolution process: {}", processName);
    }
}
