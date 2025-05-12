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

import java.util.List;
import java.util.UUID;

import org.qubership.atp.ram.enums.RootCauseEnum;
import org.qubership.atp.ram.migration.mongoevolution.java.annotation.ChangeLog;
import org.qubership.atp.ram.migration.mongoevolution.java.annotation.ChangeSet;
import org.qubership.atp.ram.models.RootCause;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.extern.slf4j.Slf4j;

@ChangeLog(version = 6)
@Slf4j
public class SetRootCauseDefaultV6 {


    /**
     * Set default and disabled flags for existed root causes.
     *
     * @param mongoTemplate mongo template
     */
    @ChangeSet(order = 1)
    public void run(MongoTemplate mongoTemplate) {
        String mongoEvoProcessName = getClass().getName();
        log.info("Start mongo evolution process: {}", mongoEvoProcessName);

        String collectionName = RootCause.class.getAnnotation(Document.class).collection();

        List<RootCause> existedRootCauses = mongoTemplate.findAll(RootCause.class, collectionName);

        existedRootCauses.forEach(rootCause -> {
            UUID notAnalysedRootCauseId = RootCauseEnum.NOT_ANALYZED.getRootCause().getUuid();
            UUID currentRootCauseId = rootCause.getUuid();
            rootCause.setDefault(notAnalysedRootCauseId.equals(currentRootCauseId));
            rootCause.setDisabled(false);

            mongoTemplate.save(rootCause);
        });

        log.info("End mongo evolution process: {}", mongoEvoProcessName);
    }
}
