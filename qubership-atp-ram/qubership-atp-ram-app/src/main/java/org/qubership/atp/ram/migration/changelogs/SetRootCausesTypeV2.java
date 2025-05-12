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

import static org.qubership.atp.ram.migration.MigrationConstants._ID;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.qubership.atp.ram.enums.RootCauseEnum;
import org.qubership.atp.ram.migration.MigrationWorker;
import org.qubership.atp.ram.migration.mongoevolution.java.annotation.ChangeLog;
import org.qubership.atp.ram.migration.mongoevolution.java.annotation.ChangeSet;
import org.qubership.atp.ram.models.RootCause;
import org.qubership.atp.ram.models.RootCauseType;
import org.qubership.atp.ram.utils.StreamUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import lombok.extern.slf4j.Slf4j;

@ChangeLog(version = 2)
@Slf4j
public class SetRootCausesTypeV2 {

    /**
     * Populate db with common root causes.
     *
     * @param mongoTemplate mongo template
     */
    @ChangeSet(order = 1)
    public void run(MongoTemplate mongoTemplate) {
        String mongoEvoProcessName = SetRootCausesTypeV2.class.getName();
        log.info("Start mongo evolution process: {}", mongoEvoProcessName);

        Set<UUID> defaultCauseIds = StreamUtils.extractIds(RootCauseEnum.getAll());
        List<RootCause> savedDefaultCauses =
                mongoTemplate.find(new Query(where(_ID).in(defaultCauseIds)), RootCause.class);

        savedDefaultCauses.forEach(rootCause -> rootCause.setType(RootCauseType.GLOBAL));

        new MigrationWorker(mongoTemplate).upsert(savedDefaultCauses);
        log.info("End mongo evolution process: {}", mongoEvoProcessName);
    }
}
