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

import static org.springframework.data.mongodb.core.aggregation.Aggregation.addFields;

import java.util.Collections;

import org.qubership.atp.ram.migration.mongoevolution.java.annotation.ChangeLog;
import org.qubership.atp.ram.migration.mongoevolution.java.annotation.ChangeSet;
import org.qubership.atp.ram.models.Issue;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.DefaultIndexOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.AggregationUpdate;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Query;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ChangeLog(version = 24)
public class V24AddFailedTestRunsCountToIssue {

    /**
     * Create new index for Issue. Fields: executionRequestId, failedTestRunsCount.
     */
    @ChangeSet(order = 1)
    public void addFailedTestRunIdsIndex(MongoTemplate mongoTemplate) {
        String processName = V24AddFailedTestRunsCountToIssue.class.getName() + " addFailedTestRunIdsIndex";
        log.info("Start mongo evolution process: {}", processName);
        Index newIndex = new Index()
                .named("_executionRequestId_failedTestRunsCount")
                .on(Issue.EXECUTION_REQUEST_ID_FIELD, Sort.DEFAULT_DIRECTION)
                .on(Issue.FAILED_TEST_RUNS_COUNT_FIELD, Sort.DEFAULT_DIRECTION)
                .background();
        new DefaultIndexOperations(mongoTemplate, Issue.COLLECTION_NAME, null).ensureIndex(newIndex);
        log.info("Index '_executionRequestId_failedTestRunsCount' was created");
    }

    /**
     * Add new field 'failedTestRunsCount' for Issues.
     */
    @ChangeSet(order = 2)
    public void updateIssue(MongoTemplate mongoTemplate) {
        String processName = V24AddFailedTestRunsCountToIssue.class.getName() + " updateIssue";
        log.info("Start mongo evolution process: {}", processName);

        AggregationUpdate update = AggregationUpdate
                .from(Collections.singletonList(
                        addFields().addField(Issue.FAILED_TEST_RUNS_COUNT_FIELD)
                                .withValue(ArrayOperators.Size.lengthOfArray(Issue.FAILED_TEST_RUNS_IDS_FIELD)).build()
                ));
        mongoTemplate.updateMulti(new Query(), update, Issue.class);

        log.info("End mongo evolution process: {}", processName);
    }
}
