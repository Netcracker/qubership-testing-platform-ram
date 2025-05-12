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
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@ChangeLog(version = 6)
public class ProjectsDateAndTimeParamsSetV6 {

    public static final String DEFAULT_PROJECT_TIME_ZONE = "GMT+03:00";
    public static final String DEFAULT_PROJECT_DATE_FORMAT = "d MMM yyyy";
    public static final String DEFAULT_PROJECT_TIME_FORMAT = "hh:mm";

    /**
     * Set default values for newly added {@link Project} entity fields -'dateFormat', 'timeFormat', 'timeZone' in
     * existed project entities.
     */
    @ChangeSet(order = 1)
    public void setProjectsDateAndTimeParams(MongoTemplate mongoTemplate) {
        String processName = ProjectsDateAndTimeParamsSetV6.class.getName();

        log.info("Start mongo evolution process: {}", processName);

        Update update = new Update().set("dateFormat", DEFAULT_PROJECT_DATE_FORMAT)
                .set("timeFormat", DEFAULT_PROJECT_TIME_FORMAT)
                .set("timeZone", DEFAULT_PROJECT_TIME_ZONE);

        mongoTemplate.updateMulti(new Query(), update, Project.class);

        log.info("End mongo evolution process: {}", processName);
    }
}
