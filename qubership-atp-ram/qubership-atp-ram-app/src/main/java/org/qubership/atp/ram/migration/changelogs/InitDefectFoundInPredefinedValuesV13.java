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
import org.qubership.atp.ram.models.dictionary.DefectFoundInEnum;
import org.springframework.data.mongodb.core.MongoTemplate;

import lombok.extern.slf4j.Slf4j;

@ChangeLog(version = 13)
@Slf4j
public class InitDefectFoundInPredefinedValuesV13 {

    /**
     * Init DefectFoundIn dictionaries.
     */
    @ChangeSet(order = 1)
    public void run(MongoTemplate mongoTemplate) {
        String mongoEvoProcessName = getClass().getName();
        log.info("Start mongo evolution process: {}", mongoEvoProcessName);

        DefectFoundInEnum.getAll()
                .stream()
                .map(DefectFoundInEnum::getDefectFoundIn)
                .forEach(mongoTemplate::save);

        log.info("End mongo evolution process: {}", mongoEvoProcessName);
    }
}
