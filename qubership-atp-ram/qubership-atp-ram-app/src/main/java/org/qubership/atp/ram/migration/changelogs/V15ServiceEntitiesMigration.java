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

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.qubership.atp.auth.springbootstarter.entities.ServiceEntities;
import org.qubership.atp.auth.springbootstarter.services.UsersService;
import org.qubership.atp.ram.enums.UserManagementEntities;
import org.qubership.atp.ram.migration.mongoevolution.java.annotation.ChangeLog;
import org.qubership.atp.ram.migration.mongoevolution.java.annotation.ChangeSet;
import org.springframework.core.env.Environment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.client.MongoDatabase;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor
@ChangeLog(version = 15)
public class V15ServiceEntitiesMigration {

    private static final UUID entitiesId = UUID.fromString("18894b59-34f2-47a9-a0cd-3b7d88c05cd5");

    /**
     * Send service entities to kafka for sink connector.
     *
     * @param database the mongo database is unnecessary but should be.
     * @param beans spring beans
     */
    @ChangeSet(order = 1)
    public void sendEntities(MongoDatabase database, Map<String, Object> beans)
            throws JsonProcessingException {
        Environment env = (Environment) beans.get("environments");
        String serviceName = env.getProperty("spring.application.name");

        ServiceEntities entities = new ServiceEntities();
        entities.setUuid(entitiesId);
        entities.setService(serviceName);
        entities.setEntities(Arrays.stream(UserManagementEntities.values())
                .map(UserManagementEntities::getName)
                .collect(Collectors.toList()));

        UsersService usersService = (UsersService) beans.get("usersService");
        usersService.sendEntities(entities);
    }
}
