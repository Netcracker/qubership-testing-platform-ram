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

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.qubership.atp.ram.dto.request.TestCaseExecutionHistorySearchRequest;
import org.qubership.atp.ram.dto.response.PaginationResponse;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.models.TestCaseExecutionHistory.TestCaseExecution;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators.ArrayElemAt;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@Repository
public class CustomExecutionHistoryRepository implements FieldConstants {

    private final MongoTemplate mongoTemplate;

    /**
     * Get test case executions by test case id with pagination support.
     * Example of generated query:
     [
       {
          "$match":{
             "testCaseId":JUUID("942dec40-19f0-4c1a-b8ce-0e0faaaac18c"),
             "testingStatus":{
                "$in":["PASSED", "FAILED"]
             },
             "rootCauseId":{
                "$in":[JUUID("5bbf39f7-9aaf-44c2-b8cd-b44562d017ac")]
             }
          }
       },
       {
          "$lookup":{
             "from":"executionRequests",
             "localField":"executionRequestId",
             "foreignField":"_id",
             "as":"executionRequest"
          }
       },
       {
          "$lookup":{
             "from":"rootCause",
             "localField":"rootCauseId",
             "foreignField":"_id",
             "as":"rootCause"
          }
       },
       {
          "$unwind":"$executionRequest"
       },
       {
          "$unwind":"$rootCause"
       },
       {
          "$match":{
             "executionRequest.environmentId":{
                "$in":[JUUID("42cc4271-aa0f-4571-a7e0-d46f37001708")]
             },
             "executionRequest.executorId":{
                "$in":[JUUID("0ba2b3a5-3546-47cc-a4cf-2fffe6e58348")]
             },
             "executionRequest.analyzedByQa":false
          }
       },
       {
          "$project":{
             "projectId":"$executionRequest.projectId",
             "executionRequestId":"$executionRequest._id",
             "executionRequestName":"$executionRequest.name",
             "testingStatus":1,
             "analyzedByQa":"$executionRequest.analyzedByQa",
             "startDate":1,
             "finishDate":1,
             "duration":1,
             "passedRate":"$executionRequest.passedRate",
             "warningRate":"$executionRequest.warningRate",
             "failedRate":"$executionRequest.failedRate",
             "environmentId":"$executionRequest.environmentId",
             "executorId":"$executionRequest.executorId",
             "executorName":"$executionRequest.executorName",
             "filteredByLabelsIds":"$executionRequest.filteredByLabels",
             "failReason":"$rootCause.name",
             "testRunId":"$_id"
          }
       },
       {
          "$sort":{
             "startDate":-1
          }
       },
       {
          "$facet":{
             "entities":[
                {
                   "$skip":0
                },
                {
                   "$limit":15
                }
             ],
             "metadata":[
                {
                   "$group":{
                      "_id":null,
                      "totalCount":{
                         "$sum":1
                      }
                   }
                }
             ]
          }
       },
       {
          "$project":{
             "entities":1,
             "totalCount":{
                "$arrayElemAt":[
                   "$metadata.totalCount",
                   0
                ]
             }
          }
       }
    ]
     *
     * @param request search request
     * @param testCaseId test case identifier
     * @return test case executions list
     */
    public PaginationResponse<TestCaseExecution> getTestCaseExecutions(TestCaseExecutionHistorySearchRequest request,
                                                                       UUID testCaseId) {
        Criteria preCriteria = new Criteria();
        preCriteria.and(TESTCASE_ID).is(testCaseId);

        final Set<TestingStatuses> testingStatuses = request.getTestingStatuses();
        if (!CollectionUtils.isEmpty(testingStatuses)) {
            preCriteria.and(TESTING_STATUS).in(testingStatuses);
        }

        final Set<UUID> failureReasons = request.getFailureReasons();
        if (!CollectionUtils.isEmpty(failureReasons)) {
            preCriteria.and(ROOT_CAUSE_ID).in(failureReasons);
        }

        final Timestamp startDate = request.getStartDate();
        final Timestamp finishDate = request.getFinishDate();

        if (startDate != null && finishDate != null) {
            preCriteria.and(START_DATE).gte(startDate).lte(finishDate);
        } else if (startDate != null) {
            preCriteria.and(START_DATE).gte(startDate);
        } else if (finishDate != null) {
            preCriteria.and(START_DATE).lte(finishDate);
        }

        List<AggregationOperation> aggregationOperations = new ArrayList<>();
        aggregationOperations.add(Aggregation.match(preCriteria));

        aggregationOperations.add(
                Aggregation.lookup(EXECUTION_REQUESTS, EXECUTION_REQUEST_ID, _ID, EXECUTION_REQUEST)
        );

        aggregationOperations.add(
                Aggregation.lookup(ROOT_CAUSE, ROOT_CAUSE_ID, _ID, ROOT_CAUSE)
        );

        aggregationOperations.add(
                Aggregation.unwind($EXECUTION_REQUEST)
        );

        aggregationOperations.add(
                Aggregation.unwind($ROOT_CAUSE)
        );

        Criteria postCriteria = new Criteria();

        final Set<UUID> environments = request.getEnvironments();
        if (!CollectionUtils.isEmpty(environments)) {
            postCriteria.and(EXECUTION_REQUEST + "." + ENVIRONMENT_ID).in(environments);
        }

        final Set<UUID> executors = request.getExecutors();
        if (!CollectionUtils.isEmpty(executors)) {
            postCriteria.and(EXECUTION_REQUEST + "." + EXECUTOR_ID).in(executors);
        }

        final Boolean analyzedByQa = request.getAnalyzedByQa();
        if (analyzedByQa != null) {
            postCriteria.and(EXECUTION_REQUEST + "." + ANALYZED_BY_QA).is(analyzedByQa);
        }

        aggregationOperations.add(Aggregation.match(postCriteria));

        aggregationOperations.add(
                Aggregation.project()
                        .and($EXECUTION_REQUEST + "." + PROJECT_ID).as(PROJECT_ID)
                        .and($EXECUTION_REQUEST + "." + _ID).as(EXECUTION_REQUEST_ID)
                        .and($EXECUTION_REQUEST + "." + NAME).as(EXECUTION_REQUEST_NAME)
                        .and($TESTING_STATUS).as(TESTING_STATUS)
                        .and($EXECUTION_REQUEST + "." + ANALYZED_BY_QA).as(ANALYZED_BY_QA)
                        .and($START_DATE).as(START_DATE)
                        .and($FINISH_DATE).as(FINISH_DATE)
                        .and($DURATION).as(DURATION)
                        .and($EXECUTION_REQUEST + "." + PASSED_RATE).as(PASSED_RATE)
                        .and($EXECUTION_REQUEST + "." + WARNING_RATE).as(WARNING_RATE)
                        .and($EXECUTION_REQUEST + "." + FAILED_RATE).as(FAILED_RATE)
                        .and($EXECUTION_REQUEST + "." + ENVIRONMENT_ID).as(ENVIRONMENT_ID)
                        .and($EXECUTION_REQUEST + "." + EXECUTOR_ID).as(EXECUTOR_ID)
                        .and($EXECUTION_REQUEST + "." + EXECUTOR_NAME).as(EXECUTOR_NAME)
                        .and($EXECUTION_REQUEST + "." + FILTERED_BY_LABELS).as(FILTERED_BY_LABELS_IDS)
                        .and($ROOT_CAUSE + "." + NAME).as(FAIL_REASON)
                        .and($_ID).as(TEST_RUN_ID)
        );

        final String sort = request.getSort();
        final String direction = request.getDirection();
        if (!StringUtils.isEmpty(sort) && !StringUtils.isEmpty(direction)) {
            aggregationOperations.add(
                    Aggregation.sort(Sort.Direction.fromString(direction), sort)
            );
        }

        GroupOperation groupOperation = Aggregation.group()
                .count().as(TOTAL_COUNT);

        final Integer page = request.getPage();
        final Integer size = request.getSize();
        aggregationOperations.add(
                Aggregation.facet(Aggregation.skip(page * size), Aggregation.limit(size)).as(ENTITIES)
                        .and(groupOperation).as(METADATA)
        );

        aggregationOperations.add(
                Aggregation.project(ENTITIES)
                        .and(ArrayElemAt.arrayOf($METADATA + "." + TOTAL_COUNT).elementAt(0)).as(TOTAL_COUNT)
        );

        final Aggregation aggregation = Aggregation.newAggregation(aggregationOperations);
        log.debug("Aggregation query '{}'", aggregation.toString());

        return mongoTemplate.aggregate(aggregation, TEST_RUN, TestCaseExecutionPaginationResponse.class)
                .getUniqueMappedResult();
    }

    private static class TestCaseExecutionPaginationResponse extends PaginationResponse<TestCaseExecution> {
    }
}
