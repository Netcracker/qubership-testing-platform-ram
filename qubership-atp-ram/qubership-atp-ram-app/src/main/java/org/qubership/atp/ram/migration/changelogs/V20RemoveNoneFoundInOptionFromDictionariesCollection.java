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
import java.util.stream.Collectors;

import org.qubership.atp.ram.migration.mongoevolution.java.annotation.ChangeLog;
import org.qubership.atp.ram.migration.mongoevolution.java.annotation.ChangeSet;
import org.qubership.atp.ram.models.dictionary.DefectFoundIn;
import org.qubership.atp.ram.models.dictionary.DefectFoundInEnum;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import lombok.extern.slf4j.Slf4j;

@ChangeLog(version = 20)
@Slf4j
public class V20RemoveNoneFoundInOptionFromDictionariesCollection {

    private static final String NAME = "name";

    /**
     * Remove "None" option from dictionaries if exists.
     */
    @ChangeSet(order = 1)
    public void run(MongoTemplate mongoTemplate) {
        String mongoEvoProcessName = getClass().getName();
        log.info("Start mongo evolution process: {}", mongoEvoProcessName);
        List<String> foundInNames = DefectFoundInEnum.getAll()
                .stream()
                .map(foundInEnum -> foundInEnum.getDefectFoundIn().getName())
                .collect(Collectors.toList());
        mongoTemplate.remove(new Query(Criteria.where(NAME).not().in(foundInNames)), DefectFoundIn.class);
        log.info("End mongo evolution process: {}", mongoEvoProcessName);
    }
}
