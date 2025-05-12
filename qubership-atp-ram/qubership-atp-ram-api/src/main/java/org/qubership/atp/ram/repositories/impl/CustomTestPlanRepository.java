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

package org.qubership.atp.ram.repositories.impl;

import static java.util.Objects.nonNull;
import static org.qubership.atp.ram.models.RamObject.NAME_FIELD;
import static org.qubership.atp.ram.repositories.impl.FieldConstants.PROJECT_ID;

import java.util.List;
import java.util.UUID;

import org.qubership.atp.ram.dto.request.TestPlansSearchRequest;
import org.qubership.atp.ram.models.TestPlan;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@Repository
public class CustomTestPlanRepository {

    private final MongoTemplate mongoTemplate;

    /**
     * Search test plans.
     *
     * @param request search request
     * @return found test plans
     */
    public List<TestPlan> search(TestPlansSearchRequest request) {
        Query query = new Query();

        final String name = request.getName();
        if (nonNull(name)) {
            query.addCriteria(Criteria.where(NAME_FIELD).is(name));
        }

        final UUID projectId = request.getProjectId();
        if (nonNull(projectId)) {
            query.addCriteria(Criteria.where(PROJECT_ID).is(projectId));
        }

        return mongoTemplate.find(query, TestPlan.class);
    }
}
