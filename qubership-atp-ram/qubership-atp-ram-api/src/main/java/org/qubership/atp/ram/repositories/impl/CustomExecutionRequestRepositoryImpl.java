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

import static org.qubership.atp.ram.repositories.impl.FieldConstants._ID;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.repositories.CustomExecutionRequestRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.CriteriaDefinition;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@Repository
public class CustomExecutionRequestRepositoryImpl implements CustomExecutionRequestRepository {

    private final MongoTemplate mongoTemplate;
    private final String failedLogrecordsCounter = "failedLogrecordsCounter";

    @Override
    public List<ExecutionRequest> searchExecutionRequests(CriteriaDefinition filters,
                                                          Pageable pageable) {
        Query query = new Query(filters).with(pageable);
        log.debug("Query: {}", query);
        return mongoTemplate.find(query, ExecutionRequest.class);
    }

    @Override
    public int getExecutionRequestsCountByCriteria(CriteriaDefinition filters) {
        Query query = new Query(filters);
        log.debug("Query: {}", query);
        return (int) mongoTemplate.count(query, ExecutionRequest.class);
    }

    @Override
    public List<ExecutionRequest> getByTestPlanAndFinishDateBetweenOrEqualsAndAnalyzedByQa(UUID testPlanId,
                                                                                           Timestamp start,
                                                                                           boolean analyzedByQa,
                                                                                           Pageable page) {
        Query query = new Query(Criteria.where("testPlanId").is(testPlanId)
                .and("finishDate").gte(start)
                .and("analyzedByQa").is(analyzedByQa)).with(page);
        return mongoTemplate.find(query, ExecutionRequest.class);
    }

    @Override
    public List<ExecutionRequest> getByTestPlanAndFinishDateBetweenOrEquals(UUID testPlanId,
                                                                            Timestamp start,
                                                                            Timestamp end,
                                                                            Pageable page) {
        Query query = new Query(Criteria.where("testPlanId").is(testPlanId)
                .and("finishDate").gte(start).lte(end)).with(page);
        return mongoTemplate.find(query, ExecutionRequest.class);
    }

    @Override
    public void updateLogRecordsCount(UUID executionRequestId, int logRecordsCount) {
        Query idQuery = new Query().addCriteria(Criteria.where(_ID).is(executionRequestId));
        Update update = new Update()
                .set(failedLogrecordsCounter, logRecordsCount);
        UpdateResult updateResult = mongoTemplate.updateFirst(idQuery, update, ExecutionRequest.class);
        log.debug("ER: {} was update with params failedLogrecordsCounter: {} result: {}",
                executionRequestId, logRecordsCount, updateResult);
    }
}
