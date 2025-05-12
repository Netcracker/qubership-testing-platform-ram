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

import static org.springframework.data.mongodb.core.aggregation.Aggregation.project;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.qubership.atp.ram.dto.response.BaseEntityResponse;
import org.qubership.atp.ram.dto.response.CompareTreeTestRunResponse;
import org.qubership.atp.ram.dto.response.PaginationResponse;
import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.models.AnalyzedTestRunSortedColumns;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.RamObject;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.models.TestRunSearchRequest;
import org.qubership.atp.ram.models.logrecords.parts.FileType;
import org.qubership.atp.ram.repositories.CustomTestRunRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.GraphLookupOperation;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import com.mongodb.BasicDBObject;
import com.mongodb.client.result.UpdateResult;
import lombok.extern.slf4j.Slf4j;

/**
 * {@inheritDoc}
 * Implementation of {@link CustomTestRunRepository}.
 */
@Slf4j
@Repository
public class CustomTestRunRepositoryImpl extends CustomRamObjectImpl implements CustomTestRunRepository {

    private static final String REGEX_SYMBOLS = "([$.*+?~^!|\\-()\\[\\]{}\\\\])";
    private static final String OPERATOR = "$";
    private static final String ID = "_id";
    private static final String NAME = "name";
    private static final String TESTCASE_ID = "testCaseId";
    private static final String EXECUTION_REQUEST_ID = "executionRequestId";
    private static final String PARENT_TEST_RUN_ID = "parentTestRunId";
    private static final String EXECUTION_STATUS = "executionStatus";
    private static final String START_DATE = "startDate";
    private static final String FINISH_DATE = "finishDate";
    private static final String DURATION = "duration";
    private static final String TESTRUNS = "testRuns";
    private static final String TESTCASE_NAME = "testCaseName";
    private static final String TESTING_STATUS = "testingStatus";
    private static final String ROOT_CAUSE_ID = "rootCauseId";
    private static final String COMMENT_TEXT = "comment.text";
    private static final String $_ID = OPERATOR + ID;
    private static final String LOG_RECORD_TEST_RUN_ID = "testRunId";
    private static final String LOG_RECORD_FILE_METADATA = "fileMetadata.type";
    private static final String CHILDREN = "children";
    private static final String CHILDREN_0 = "children.0";
    private static final String LABEL_IDS = "labelIds";

    private static final String LOG_RECORD_COLLECTION_NAME =
            LogRecord.class.getAnnotation(Document.class).collection();

    private static final String TEST_RUN_COLLECTION_NAME =
            TestRun.class.getAnnotation(Document.class).collection();

    /**
     * Sortable columns.
     */
    private static final Map<AnalyzedTestRunSortedColumns, String> sortedColumns =
            new HashMap<AnalyzedTestRunSortedColumns, String>() {
                {
                    put(AnalyzedTestRunSortedColumns.TESTCASE_NAME, TESTCASE_NAME);
                    put(AnalyzedTestRunSortedColumns.TESTING_STATUS, TESTING_STATUS);
                    put(AnalyzedTestRunSortedColumns.FAILURE_REASON, ROOT_CAUSE_ID);
                }
            };

    public CustomTestRunRepositoryImpl(MongoTemplate mongoTemplate) {
        super(mongoTemplate);
    }


    /**
     * Find test runs by filter request.*
     * Query example:
     * {
     *      "$or":[
     *          {
     *              "_id":{
     *                  "$in":[ JUUID("d40b0d22-a438-493f-aad9-cb411627cbd9") ]
     *              }
     *          },
     *          {
     *              "$and":[
     *                  {
     *                      "name":{
     *                          "$regex":"Upload"
     *                      }
     *                  },
     *                  {
     *                      testingStatus":"FAILED"
     *                  }
     *              ]
     *          }
     *      ]
     * }
     */
    @Override
    public PaginationResponse<TestRun> findAllByFilter(int page,
                                                       int size,
                                                       AnalyzedTestRunSortedColumns sortColumn,
                                                       Sort.Direction sortType,
                                                       TestRunSearchRequest filter) {
        Query query = new Query();

        List<Criteria> criteria = new ArrayList<>();
        Criteria executionRequestCriteria = new Criteria();

        if (filter.getExecutionRequestId() != null) {
            executionRequestCriteria = where(EXECUTION_REQUEST_ID).is(filter.getExecutionRequestId());
        }

        if (filter.getNameContains() != null) {
            criteria.add(where(NAME).regex(filter.getNameContains(), "i"));
        }
        if (filter.getTestCaseName() != null) {
            criteria.add(where(TESTCASE_NAME).regex(filter.getTestCaseName(), "i"));
        }
        if (filter.getInTestingStatuses() != null) {
            criteria.add(where(TESTING_STATUS).in(filter.getInTestingStatuses()));
        }
        if (filter.getNotInTestingStatuses() != null) {
            criteria.add(where(TESTING_STATUS).nin(filter.getNotInTestingStatuses()));
        }
        if (filter.getFailureReasons() != null) {
            criteria.add(where(ROOT_CAUSE_ID).in(filter.getFailureReasons()));
        }
        if (filter.getComment() != null
                && filter.getComment().getText() != null
                && !filter.getComment().getText().isEmpty()) {
            criteria.add(where(COMMENT_TEXT).regex(filter.getComment().getText(),"i"));
        }

        if (!isEmpty(filter.getLabelIds())) {
            criteria.add(where(LABEL_IDS).in(filter.getLabelIds()));
        }

        if (filter.getTestRunIds() != null) {
            List<Criteria> orCriteria = new ArrayList<>();
            orCriteria.add(where(_ID).in(filter.getTestRunIds()));
            if (!isEmpty(criteria)) {
                orCriteria.add(new Criteria().andOperator(criteria.toArray(new Criteria[0])));
            }
            query.addCriteria(executionRequestCriteria.orOperator(orCriteria.toArray(new Criteria[0])));
        } else {
            query.addCriteria(executionRequestCriteria);
            criteria.forEach(query::addCriteria);
        }

        long totalCount = mongoTemplate.count(query, TestRun.class);

        PageRequest pageable;
        if (sortColumn != null && sortType != null) {
            pageable = PageRequest.of(page, size, Sort.by(sortType, sortedColumns.get(sortColumn)));
        } else {
            pageable = PageRequest.of(page, size);
        }
        query.with(pageable);

        List<TestRun> testRuns = mongoTemplate.find(query, TestRun.class);

        return new PaginationResponse<>(testRuns, totalCount);
    }

    @Override
    public List<TestRun> findTestRunsByExecutionRequestIdAndNamesAndLabelIds(UUID executionRequestId,
                                                                             List<String> testRunNames,
                                                                             List<UUID> labelIds) {
        List<Criteria> criteria = new ArrayList<>();
        if (executionRequestId != null) {
            criteria.add(where(EXECUTION_REQUEST_ID).is(executionRequestId));
        }
        if (!isEmpty(testRunNames)) {
            Criteria nameCriteria = new Criteria();
            nameCriteria.orOperator(testRunNames.stream()
                    .map(name -> where(NAME)
                            .regex(".*" + name.replaceAll(REGEX_SYMBOLS, "\\\\$1") + ".*", "i"))
                    .toArray(Criteria[]::new));
            criteria.add(nameCriteria);
        }
        if (!isEmpty(labelIds)) {
            criteria.add(where(LABEL_IDS).all(labelIds));
        }

        // need to search only Passed and Failed test runs
        criteria.add(where(TESTING_STATUS).in(Arrays.asList(TestingStatuses.PASSED, TestingStatuses.FAILED)));

        Query query = new Query();
        query.fields().include(ID);
        query.fields().include(NAME);
        query.fields().include(TESTING_STATUS);
        criteria.forEach(query::addCriteria);
        return mongoTemplate.find(query, TestRun.class);
    }

    @Override
    public List<TestRun> findTestRunsIdNameByExecutionRequestIdAndLabelIds(UUID executionRequestId,
                                                                             List<UUID> labelIds) {
        Query query = new Query();
        List<Criteria> criteria = new ArrayList<>();
        if (executionRequestId != null) {
            criteria.add(where(EXECUTION_REQUEST_ID).is(executionRequestId));
        }
        if (!isEmpty(labelIds)) {
            criteria.add(where(LABEL_IDS).all(labelIds));
        }

        criteria.forEach(query::addCriteria);
        query.fields().include(ID);
        query.fields().include(NAME);
        return mongoTemplate.find(query, TestRun.class);
    }

    /**
     * Find project id by test run id.
     * db.testrun.aggregate([
     *     {
     *         $match: {
     *            _id: LUUID("ff4f153e-5eb5-c5e9-579b-7f4c4d340aad")
     *         }
     *     },
     *     {
     *         $lookup: {
     *             from: "executionRequests",
     *             localField: "executionRequestId",
     *             foreignField: "_id",
     *             as: "executionRequest"
     *         }
     *     },
     *     {
     *         $unwind: "$executionRequest"
     *     },
     *     {
     *         $project: {
     *             "projectId": "$executionRequest.projectId",
     *             "_id": 0
     *         }
     *     }
     * ]
     * )
     */
    @Override
    public UUID findProjectIdByTestRunId(UUID testRunId) {
        return getProjectIdBySpecifiedConditions(
                Collections.singletonList(
                        Aggregation.match(where(_ID).is(testRunId))
                )
        );
    }

    /**
     * Find project id by test case id.
     * db.testrun.aggregate([
     *     {
     *         $match: {
     *             testCaseId: LUUID("3546872f-0008-d640-a2f8-55bd62abf089")
     *         }
     *     },
     *     {
     *         $limit: 1
     *     },
     *     {
     *         $lookup: {
     *             from: "executionRequests",
     *             localField: "executionRequestId",
     *             foreignField: "_id",
     *             as: "executionRequest"
     *         }
     *     },
     *     {
     *         $unwind: "$executionRequest"
     *     },
     *     {
     *         $project: {
     *             "projectId": "$executionRequest.projectId",
     *             "_id": 0
     *         }
     *     }
     * ]
     * )
     */
    @Override
    public UUID findProjectIdByTestCaseId(UUID testCaseId) {
        List<AggregationOperation> aggregationOperations = new ArrayList<>();
        aggregationOperations.add(Aggregation.match(where(TESTCASE_ID).is(testCaseId)));
        aggregationOperations.add(Aggregation.limit(1));
        return getProjectIdBySpecifiedConditions(aggregationOperations);
    }

    private UUID getProjectIdBySpecifiedConditions(List<AggregationOperation> conditions) {
        List<AggregationOperation> aggregationOperations = new ArrayList<>(conditions);
        aggregationOperations.add(
                LookupOperation.newLookup()
                        .from(EXECUTION_REQUESTS)
                        .localField(EXECUTION_REQUEST_ID)
                        .foreignField(_ID)
                        .as(EXECUTION_REQUEST)
        );
        aggregationOperations.add(Aggregation.unwind($EXECUTION_REQUEST));
        aggregationOperations.add(project().and($EXECUTION_REQUEST + "." + PROJECT_ID).as(_ID));
        final Aggregation aggregation = Aggregation.newAggregation(aggregationOperations);
        Optional<ExecutionRequest> result = Optional.ofNullable(
                mongoTemplate.aggregate(aggregation, TEST_RUN_COLLECTION_NAME, ExecutionRequest.class)
                        .getUniqueMappedResult()
        );
        return result.map(RamObject::getUuid).orElse(null);
    }

    @Override
    public List<CompareTreeTestRunResponse> compareByExecutionRequestIds(List<UUID> executionRequestIds) {
        log.debug("get test runs to compare by ERs: executionRequestIds [{}]",
                executionRequestIds);
        final List<AggregationOperation> aggregationOperations = new ArrayList<>(
                getTestRunsGroupedByTestCases(executionRequestIds));
        aggregationOperations.add(
                Aggregation.match(where(TESTRUNS + "." + EXECUTION_REQUEST_ID).all(executionRequestIds))
        );
        aggregationOperations.addAll(turnTestCaseToTestRuns());
        aggregationOperations.add(
                Aggregation.match(where(EXECUTION_REQUEST_ID).in(executionRequestIds))
        );
        String testRunCollectionName = TestRun.class.getAnnotation(Document.class).collection();
        final Aggregation aggregation = Aggregation.newAggregation(aggregationOperations);
        return mongoTemplate
                .aggregate(aggregation, testRunCollectionName, CompareTreeTestRunResponse.class)
                .getMappedResults();
    }

    @Override
    public List<BaseEntityResponse> getTestRunsNotInExecutionRequestCompareTable(
            List<UUID> executionRequestIds) {
        log.debug("get test runs not in ER compare table: executionRequestIds [{}]",
                executionRequestIds);
        final List<AggregationOperation> aggregationOperations = new ArrayList<>(
                getTestRunsGroupedByTestCases(executionRequestIds));
        aggregationOperations.add(
                Aggregation.match(new Criteria().andOperator(
                        where(TESTRUNS + "." + EXECUTION_REQUEST_ID).in(executionRequestIds),
                        where(TESTRUNS + "." + EXECUTION_REQUEST_ID).not().all(executionRequestIds)
                ))
        );
        aggregationOperations.addAll(turnTestCaseToTestRuns());
        aggregationOperations.add(
                project().andExpression(TESTCASE_NAME).as("name")
        );
        String testRunCollectionName = TestRun.class.getAnnotation(Document.class).collection();
        final Aggregation aggregation = Aggregation.newAggregation(aggregationOperations);
        return mongoTemplate
                .aggregate(aggregation, testRunCollectionName, BaseEntityResponse.class)
                .getMappedResults();
    }

    @Override
    public void updateStatusesAndFinishDateByTestRunId(UUID testRunId,
                                                       ExecutionStatuses executionStatus,
                                                       TestingStatuses testingStatus,
                                                       Timestamp finishDate, long duration) {
        Update update = new Update()
                .set(EXECUTION_STATUS, executionStatus)
                .set(TESTING_STATUS, testingStatus)
                .set(FINISH_DATE, finishDate)
                .set(DURATION, duration);

        UpdateResult updateResult = mongoTemplate.updateFirst(queryIsId(testRunId), update, TestRun.class);
        log.debug("TestRun: {} was update with params executionStatus: {}, testingStatus: {}, finishDate: {}, "
                        + "duration: {}. {}",
                testRunId, executionStatus, testingStatus, finishDate, duration, updateResult);
    }

    private List<AggregationOperation> getTestRunsGroupedByTestCases(List<UUID> executionRequestIds) {
        List<AggregationOperation> aggregationOperations = new ArrayList<>();
        aggregationOperations.add(
                Aggregation.match(where(EXECUTION_REQUEST_ID).in(executionRequestIds))
        );
        aggregationOperations.add(Aggregation
                .group(TESTCASE_ID)
                .push(new BasicDBObject(_ID, OPERATOR + _ID)
                        .append(EXECUTION_REQUEST_ID, OPERATOR + EXECUTION_REQUEST_ID)
                        .append(PARENT_TEST_RUN_ID, OPERATOR + PARENT_TEST_RUN_ID)
                        .append(TESTCASE_ID, OPERATOR + TESTCASE_ID)
                        .append(TESTCASE_NAME, OPERATOR + TESTCASE_NAME)
                        .append(EXECUTION_STATUS, OPERATOR + EXECUTION_STATUS)
                        .append(TESTING_STATUS, OPERATOR + TESTING_STATUS)
                        .append(START_DATE, OPERATOR + START_DATE)
                        .append(FINISH_DATE, OPERATOR + FINISH_DATE)
                        .append(DURATION, OPERATOR + DURATION)
                )
                .as(TESTRUNS));
        return aggregationOperations;
    }

    private List<AggregationOperation> turnTestCaseToTestRuns() {
        List<AggregationOperation> aggregationOperations = new ArrayList<>();
        aggregationOperations.add(
                Aggregation.unwind(OPERATOR + TESTRUNS)
        );
        aggregationOperations.add(
                Aggregation.replaceRoot(OPERATOR + TESTRUNS)
        );
        return aggregationOperations;
    }


    /**
     * Find test run with LR-s with file type.
     * Query example:
     * db.testrun.aggregate(
     *           {
     *               $match: {
     *                   executionRequestId: erIdVar,
     *               }
     *           },
     *           {
     *               $graphLookup: {
     *                        from: "logrecord",
     *                        startWith: "$_id",
     *                        connectFromField: "testRunId",
     *                        connectToField: "testRunId",
     *                        as: "children",
     *                        depthField: "depth",
     *                        restrictSearchWithMatch: {
     *                           "fileMetadata.type": "POT"
     *                        }
     *                     }
     *           },
     *           {
     *               $match: {
     *                   "children.0": {$exists:  true}
     *               }
     *           },
     *           {
     *               $project: {
     *                   children: 0
     *               }
     *           },
     *       )
     *
     * @param executionRequestId for find TR-s
     * @param fileType for find LR-s
     * @return list of TR-s
     */
    @Override
    public List<TestRun> findTestRunsByExecutionRequestAndHasLogRecordsWithFile(UUID executionRequestId,
                                                                                FileType fileType) {
        log.debug("Filter test runs by filter: fileType of LR-s = {}, execution request ID = {}", fileType,
                executionRequestId);
        final List<AggregationOperation> aggregationOperations = new ArrayList<>();
        aggregationOperations.add(
                Aggregation.match(where(EXECUTION_REQUEST_ID).is(executionRequestId)));
        aggregationOperations.add(getGraphLookUpLogRecords(fileType));
        aggregationOperations.add(Aggregation.match(where(CHILDREN_0).exists(true)));
        aggregationOperations.add(
                project().andExclude(CHILDREN)
        );

        final Aggregation aggregation = Aggregation.newAggregation(aggregationOperations);
        return mongoTemplate.aggregate(aggregation, TEST_RUN_COLLECTION_NAME, TestRun.class).getMappedResults();
    }

    private AggregationOperation getGraphLookUpLogRecords(FileType fileType) {
        GraphLookupOperation.GraphLookupOperationBuilder graphLookupBuilder =
                Aggregation.graphLookup(LOG_RECORD_COLLECTION_NAME)
                        .startWith($_ID)
                        .connectFrom(LOG_RECORD_TEST_RUN_ID)
                        .connectTo(LOG_RECORD_TEST_RUN_ID);

        if (Objects.nonNull(fileType)) {
            graphLookupBuilder.restrict(new Criteria(LOG_RECORD_FILE_METADATA).is(fileType));
        }
        return graphLookupBuilder.as(CHILDREN);
    }
}
