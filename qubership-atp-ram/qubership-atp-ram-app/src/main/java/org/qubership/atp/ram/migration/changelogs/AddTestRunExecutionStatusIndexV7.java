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

import static org.springframework.data.mongodb.core.query.Criteria.where;

import org.qubership.atp.ram.migration.mongoevolution.java.annotation.ChangeLog;
import org.qubership.atp.ram.migration.mongoevolution.java.annotation.ChangeSet;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.DefaultIndexOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.PartialIndexFilter;

import lombok.extern.slf4j.Slf4j;

@ChangeLog(version = 7)
@Slf4j
public class AddTestRunExecutionStatusIndexV7 {

    /**
     * Add partial index for testrun.ExecutionStatus field.
     * @param mongoTemplate mongo template
     */
    @ChangeSet(order = 1)
    public void addTestRunExecutionStatusIndex(MongoTemplate mongoTemplate) {
        String processName = AddTestRunExecutionStatusIndexV7.class.getName();
        log.info("Start mongo evolution process: {}", processName);
        Index myIndex = new Index()
                .named("executionStatusInProgress")
                .on("executionStatus", Sort.Direction.ASC)
                .background()
                .partial(PartialIndexFilter.of(
                        where("executionStatus").is("IN_PROGRESS")
                ));

        DefaultIndexOperations indexOperations = new DefaultIndexOperations(
                mongoTemplate,
                "testrun",
                null
        );
        indexOperations.ensureIndex(myIndex);
        log.info("End mongo evolution process: {}", processName);
    }
}
