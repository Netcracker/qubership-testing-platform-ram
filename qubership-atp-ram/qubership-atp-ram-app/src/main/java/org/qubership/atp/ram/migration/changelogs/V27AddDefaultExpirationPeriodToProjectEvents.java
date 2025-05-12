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
import org.qubership.atp.ram.models.Project;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor
@ChangeLog(version = 27)
public class V27AddDefaultExpirationPeriodToProjectEvents {
    private static final String EXPIRATION_PERIOD = "executionRequestsExpirationPeriodWeeks";

    /**
     * Add expirationPeriodWeek to projects table.
     */
    @ChangeSet(order = 1)
    public void setExpirationPeriod(MongoTemplate mongoTemplate) {
        String processName = V27AddDefaultExpirationPeriodToProjectEvents.class.getName();
        log.info("Start mongo evolution process: {}", processName);
        Query query = new Query(new Criteria()
                .orOperator(
                        Criteria.where(EXPIRATION_PERIOD).exists(false),
                        Criteria.where(EXPIRATION_PERIOD).is(0)));
        Update update = new Update().set(EXPIRATION_PERIOD, 24);
        mongoTemplate.updateMulti(query, update, Project.class);
        log.info("End mongo evolution process: {}", processName);
    }
}
