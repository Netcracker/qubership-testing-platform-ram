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

import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.enums.TypeAction;
import org.qubership.atp.ram.model.LogRecordQueryResult;
import org.qubership.atp.ram.model.LogRecordWithChildrenResponse;
import org.qubership.atp.ram.model.LogRecordWithParentListResponse;
import org.qubership.atp.ram.model.LogRecordWithParentResponse;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.RamObject;
import org.qubership.atp.ram.models.logrecords.parts.FileType;
import org.qubership.atp.ram.repositories.CustomLogRecordRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.GraphLookupOperation;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@Repository
public class CustomLogRecordRepositoryImpl implements CustomLogRecordRepository, FieldConstants {

    private final MongoTemplate mongoTemplate;
    private final ModelMapper modelMapper;

    private static final String LOG_RECORD_COLLECTION_NAME =
            LogRecord.class.getAnnotation(Document.class).collection();

    private static final long SINGLE_LIMIT = 1;

    /**
     * Filter log records by lookup.
     * See here for details: https://docs.mongodb.com/manual/reference/operator/aggregation/graphLookup/
     * Query example:
     *  {
     *     $match: {
     *        testRunId: JUUID("00a2361c-0e54-467c-ab95-094df7f1eaeb"),
     *        parentRecordId: {
     *           "$exists": false
     *        },
     *        rootCause: {
     *           "$exists": true
     *        }
     *     }
     *  },
     *  {
     *     $graphLookup: {
     *        from: "logrecord",
     *        startWith: "$_id",
     *        connectFromField: "_id",
     *        connectToField: "parentRecordId",
     *        as: "children",
     *        depthField: "depth",
     *        restrictSearchWithMatch: {
     *           testingStatus: {
     *              $in: ["PASSED"]
     *           },
     *           type: {
     *              $in: ["UI", "COMPOUND"]
     *           }
     *        }
     *     }
     *  },
     *  {
     *     $match: {
     *        "children.0": {
     *           $exists: true
     *        }
     *     }
     *  },
     *  {
     *    $project: {
     *       children: 0
     *    }
     *  }
     *
     * @param testRunId test run id
     * @param statuses testing statuses
     * @param types steps types
     * @param showNotAnalyzedItemsOnly is root cause present
     * @return search result
     */
    @Override
    public List<LogRecord> getTopLogRecordsByFilterLookup(UUID testRunId,
                                                          List<String> statuses,
                                                          List<String> types,
                                                          boolean showNotAnalyzedItemsOnly) {
        log.debug("Filter top log records by filter: statuses [{}], types [{}], showNotAnalyzedItemsOnly [{}]",
                statuses, types, showNotAnalyzedItemsOnly);

        final List<AggregationOperation> aggregationOperations = new ArrayList<>();

        aggregationOperations.add(
                Aggregation.match(where(TEST_RUN_ID).is(testRunId)
                        .and(PARENT_RECORD_ID).exists(false)
                        .and(ROOT_CAUSE).exists(showNotAnalyzedItemsOnly))
        );

        aggregationOperations.add(
                getLogRecordHierarchyGraphLookup(statuses, types)
        );

        aggregationOperations.add(
                Aggregation.match(where(CHILDREN_0).exists(true))
        );

        aggregationOperations.add(
                Aggregation.project().andExclude(CHILDREN)
        );

        aggregationOperations.add(
                Aggregation.sort(Sort.Direction.ASC, CREATED_DATE_STAMP)
        );

        final Aggregation aggregation = Aggregation.newAggregation(aggregationOperations);
        log.debug("Aggregation query '{}'", aggregation.toString());

        return mongoTemplate.aggregate(aggregation, LOG_RECORD_COLLECTION_NAME, LogRecord.class).getMappedResults();
    }

    @Override
    public List<LogRecord> getAllHierarchicalChildrenLogRecords(UUID parentLogRecord) {
        log.debug("Get all hierarchical children log records for parent log record '{}'", parentLogRecord);

        final List<AggregationOperation> aggregationOperations = new ArrayList<>();

        aggregationOperations.add(
                Aggregation.match(where(_ID).is(parentLogRecord))
        );

        aggregationOperations.add(
                getLogRecordHierarchyGraphLookup(null, null)
        );

        aggregationOperations.add(
                Aggregation.unwind($CHILDREN)
        );

        aggregationOperations.add(
                Aggregation.project()
                        .andExpression($CHILDREN + "." + _ID).as(_ID)
                        .andExpression($CHILDREN + "." + PREVIEW).as(PREVIEW)
                        .andExpression($CHILDREN + "." + TYPE).as(TYPE)
        );

        final Aggregation aggregation = Aggregation.newAggregation(aggregationOperations);
        log.debug("Aggregation query '{}'", aggregation.toString());

        return mongoTemplate.aggregate(aggregation, LOG_RECORD_COLLECTION_NAME, LogRecord.class).getMappedResults();
    }

    private AggregationOperation getLogRecordHierarchyGraphLookup(List<String> statuses,
                                                                  List<String> types) {
        GraphLookupOperation.GraphLookupOperationBuilder graphLookupBuilder =
                Aggregation.graphLookup(LOG_RECORD_COLLECTION_NAME)
                        .startWith($ID)
                        .connectFrom(_ID)
                        .connectTo(PARENT_RECORD_ID);

        if (!CollectionUtils.isEmpty(statuses)) {
            graphLookupBuilder.restrict(new Criteria(TESTING_STATUS).in(statuses));
        }

        if (!CollectionUtils.isEmpty(types)) {
            graphLookupBuilder.restrict(new Criteria(TYPE).in(types));
        }

        return graphLookupBuilder.as(CHILDREN);
    }


    /**
     * Get parent LR (top level) and list of child with pot files.
     * Query example:
     * db.logrecord.aggregate(
     *           {
     *               $match: {
     *                   testRunId: testRunIdVar,
     *                  "fileMetadata.type": "POT",
     *               }
     *           },
     *         {
     *               $graphLookup: {
     *                        from: "logrecord",
     *                        startWith: "$parentRecordId",
     *                        connectFromField: "parentRecordId",
     *                        connectToField: "_id",
     *                        as: "parent",
     *                        depthField: "depth",
     *                        restrictSearchWithMatch: {
     *                          testRunId: testRunIdVar
     *                       }
     *                     }
     *           }
     *       )
     *
     * @param testRunId id of TR
     * @param fileType type of file
     * @return map with parent ID and child
     */
    @Override
    public List<LogRecordWithParentResponse> getTopLogRecordsIdAndChildLogRecordsByFileTypeFilterLookup(
            UUID testRunId,
            FileType fileType) {
        log.debug("Filter top log records by filter: fileType = {}", fileType);

        List<AggregationOperation> aggregationOperations = new ArrayList<>();
        AggregationOperation testRunIdMatch = Aggregation.match(where(TEST_RUN_ID).is(testRunId).and(FILE_TYPE)
                .is(fileType));
        AggregationOperation parentLookup = getParentLogRecordHierarchyGraphLookup(testRunId);

         aggregationOperations.add(testRunIdMatch);
         aggregationOperations.add(parentLookup);

        final Aggregation aggregation = Aggregation.newAggregation(aggregationOperations);
        log.debug("Aggregation query for get LR with file type '{}'", aggregation.toString());

        org.bson.Document document = mongoTemplate.aggregate(aggregation, LOG_RECORD_COLLECTION_NAME,
                LogRecord.class).getRawResults();
        return processParentAndChildLogRecordsLookupResults((List<org.bson.Document>) document.get(RESULTS));
    }

    /**
     * Process lookup results.
     *
     * @param results raw results
     * @return list parent, child LR-s
     */
    private List<LogRecordWithParentResponse> processParentAndChildLogRecordsLookupResults(
            List<org.bson.Document> results) {
        List<LogRecordWithParentResponse> logRecordLookupResponses = new ArrayList<>();
        results.forEach(document -> {
            LogRecordWithParentResponse logRecordLookupResponse = new LogRecordWithParentResponse();

            LogRecordQueryResult logRecordFile = modelMapper.map(document, LogRecordQueryResult.class);
            logRecordFile.setUuid((UUID) document.get(_ID));
            logRecordLookupResponse.setFileLogRecord(logRecordFile);

            Optional<LogRecord> logRecordParent =
                    processListDocumentsToLogRecord(logRecordFile.getParent());
            logRecordParent.ifPresent(logRecordLookupResponse::setParent);

            logRecordLookupResponses.add(logRecordLookupResponse);
        });

        return logRecordLookupResponses;
    }

    private Optional<LogRecord> processListDocumentsToLogRecord(List<org.bson.Document> logRecordList) {
         Optional<org.bson.Document> documentOpt =
                    logRecordList.stream().filter(document -> document.get(PARENT_RECORD_ID) == null).findFirst();
         if (documentOpt.isPresent()) {
             org.bson.Document document = documentOpt.get();
             LogRecord logRecord = modelMapper.map(document, LogRecord.class);
             logRecord.setUuid((UUID) document.get(_ID));
             return Optional.of(logRecord);
         }
         return Optional.empty();
    }

    private AggregationOperation getParentLogRecordHierarchyGraphLookup(UUID testRunId) {
        return getParentLogRecordHierarchyGraphLookup(testRunId, false);
    }

    private AggregationOperation getParentLogRecordHierarchyGraphLookup(UUID testRunId, boolean needDepthField) {
        GraphLookupOperation.GraphLookupOperationBuilder graphLookupBuilder =
                Aggregation.graphLookup(LOG_RECORD_COLLECTION_NAME)
                        .startWith($PARENT_LOG_RECORD_ID)
                        .connectFrom(PARENT_RECORD_ID)
                        .connectTo(_ID);

        if (needDepthField) {
            graphLookupBuilder.depthField("depth");
        }
        if (Objects.nonNull(testRunId)) {
            graphLookupBuilder.restrict(new Criteria(TEST_RUN_ID).is(testRunId));
        }

        return graphLookupBuilder.as(PARENT);
    }

    /**
     * Find first parent and children LR by TR is and status.
     * Query example:
     *  var testRunIdVar = JUUID("12851ab0-5d4f-41b9-bfe2-e91a6c5c9783")
     *  db.logrecord.aggregate(
     *           {
     *               $match: {
     *                   testRunId: testRunIdVar,
     *                   parentRecordId: {$exists:false}
     *               }
     *           },
     *           {
     *           $sort: {
     *                 lastUpdated: 1
     *             }
     *           },
     *           {
     *               $graphLookup: {
     *                       from: "logrecord",
     *                       startWith: "$_id",
     *                       connectFromField: "_id",
     *                       connectToField: "parentRecordId",
     *                       as: "children",
     *                       depthField: "depth",
     *                       restrictSearchWithMatch: {
     *                           "testingStatus": "FAILED",
     *                       }
     *                     }
     *           },
     *           {
     *               $match: {
     *                   "children.0": {$exists: true}
     *               }
     *           },
     *           {
     *              $unwind: "$children"
     *             },
     *          {
     *              $sort: {
     *                  "children.depth": -1
     *              }
     *          },
     *          {
     *              $limit: 1
     *          }
     *       )
     *
     * @param tesRunId TR id
     * @param testingStatus status
     * @return first LR with children
     */
    @Override
    public LogRecordWithChildrenResponse getLogRecordParentAndChildrenByTestingStatusAndTestRunId(UUID tesRunId,
                                                                                                  TestingStatuses
                                                                                                       testingStatus) {
        List<AggregationOperation> aggregationOperations = new ArrayList<>();
        aggregationOperations.add(Aggregation.match(where(TEST_RUN_ID).is(tesRunId)
                .and(PARENT_RECORD_ID).exists(false)));
        aggregationOperations.add(Aggregation.sort(Sort.Direction.ASC, LAST_UPDATED));
        aggregationOperations.add(getChildrenLogRecordHierarchyGraphLookup(testingStatus));
        aggregationOperations.add(Aggregation.match(where(CHILDREN_0).exists(true)));
        aggregationOperations.add(Aggregation.unwind($CHILDREN));
        aggregationOperations.add(Aggregation.sort(Sort.Direction.DESC, CHILDREN_DEPTH));
        aggregationOperations.add(Aggregation.limit(SINGLE_LIMIT));

        final Aggregation aggregation = Aggregation.newAggregation(aggregationOperations);
        log.debug("Aggregation query for get LR with testing status '{}'", aggregation);

        return mongoTemplate.aggregate(aggregation, LOG_RECORD_COLLECTION_NAME,
                LogRecordWithChildrenResponse.class).getUniqueMappedResult();
    }

    /**
     * Create graphLookup operation for get children.
     *
     * @param testingStatuses status
     * @return operation
     */
    private AggregationOperation getChildrenLogRecordHierarchyGraphLookup(TestingStatuses testingStatuses) {
        GraphLookupOperation.GraphLookupOperationBuilder graphLookupBuilder =
                Aggregation.graphLookup(LOG_RECORD_COLLECTION_NAME)
                        .startWith($ID)
                        .connectFrom(_ID)
                        .connectTo(PARENT_RECORD_ID);

        if (Objects.nonNull(testingStatuses)) {
            graphLookupBuilder.restrict(new Criteria(TESTING_STATUS).is(testingStatuses));
        }

        return graphLookupBuilder.as(CHILDREN);
    }

    /**
     * Find all LR-s by test runs ID and exist validation.
     *
     * @param testRunIds for found LR-s
     * @return list of LR-s
     */
    public List<LogRecord> findLogRecordsByTestRunIdsAndValidationWithHint(Collection<UUID> testRunIds) {
        if (CollectionUtils.isEmpty(testRunIds)) {
            return new ArrayList<>();
        }


        Criteria validationOr = new Criteria();
        validationOr.orOperator(
                where(VALIDATION_LABELS).exists(true).not().size(0),
                where(VALIDATION_STEPS_LABELS).exists(true).not().size(0));
        Query query = new Query();
        query
                .addCriteria(where(TEST_RUN_ID).in(testRunIds))
                .addCriteria(validationOr)
                .withHint(new org.bson.Document(TEST_RUN_ID, 1).toJson())
                .fields()
                .include(UUID)
                .include(NAME)
                .include(TEST_RUN_ID)
                .include(VALIDATION_LABELS)
                .include(VALIDATION_TABLE)
                .include(TESTING_STATUS);


        List<LogRecord> logRecords = mongoTemplate.find(query, LogRecord.class);
        return logRecords;
    }

    /**
     * Find all LR's with preview and list of parents (LR) by TR id.
     * Query example:
     * [
     *        {
     *          "$match": {
     *          "testRunId": JUUID("26f1555b-85c6-489c-a2cf-691fa96e9f2f"),
     *          "preview": {
     *              "$exists": true,
     *              "$ne": ""
     *            }
     *        }
     *    },
     *    {
     *       "$graphLookup": {
     *          "from": "logrecord",
     *          "startWith": "$parentRecordId",
     *          "connectFromField": "parentRecordId",
     *          "connectToField": "_id",
     *          "as": "parent",
     *          "depthField": "depth"
     *        }
     *    },
     *    {
     *       "$project": {
     *          "name": 1,
     *          "testingStatus": 1,
     *          "startDate": 1,
     *          "parent": 1
     *        }
     *    },
     *    {
     *       "$sort": {
     *          "startDate": 1
     *        }
     *    }
     * ]
     * Response example:
     * [
     *   {
     *     "_id":"4fc1dbbe-ab58-4090-9d4b-6363a068a315",
     *     "name":"Message",
     *     "startDate":2022-04-25T16:54:56.461Z,
     *     "testingStatus":"PASSED",
     *     "parent":[
     *        {
     *          "_id": "32a63b59-18b2-40fa-9a35-dd137a7f5647",
     *          "name": "Login as \"user\" with password \"password\"",
     *          "type": "UI",
     *          "depth": 0
     *        }
     *     ]
     *   }
     * ]
     * @param testRunId Test Run id.
     * @return All LR's with preview and list of parents (LR).
     */
    @Override
    public List<LogRecordWithParentListResponse> findLogRecordsWithParentsByPreviewExists(UUID testRunId) {
        List<AggregationOperation> aggregationOperations = new ArrayList<>();
        aggregationOperations.add(Aggregation.match(where(TEST_RUN_ID).is(testRunId)
                .and(PREVIEW).exists(true).ne("")
        ));
        aggregationOperations.add(getParentLogRecordHierarchyGraphLookup(null, true));
        aggregationOperations.add(Aggregation.project(NAME, TESTING_STATUS, START_DATE, PARENT));
        aggregationOperations.add(Aggregation.sort(Sort.Direction.ASC, START_DATE));
        return mongoTemplate.aggregate(
                Aggregation.newAggregation(aggregationOperations), LOG_RECORD_COLLECTION_NAME,
                        LogRecordWithParentListResponse.class)
                .getMappedResults();
    }

    /**
     * Find orchestrator log records.
     * Query example:
     * var testRunIdVar = JUUID("20d9e670-81e9-48c9-b0e9-8d22f4b850fa")
     * var status = "IN PROGRESS";
     * db.logrecord.aggregate( [
     *     {
     *         $match: {
     *             testRunId: testRunIdVar,
     *             executionStatus: status
     *         }
     *     },
     *     {
     *         $sort: {
     *             createdDateStamp: -1
     *         }
     *     },
     *     {
     *       $graphLookup: {
     *          from: "logrecord",
     *          startWith: "$parentRecordId",
     *          connectFromField: "parentRecordId",
     *          connectToField: "_id",
     *          as: "parent",
     *          restrictSearchWithMatch: {
     *             testRunId: testRunIdVar
     *          }
     *       }
     *     },
     *     {
     *         $match: {
     *             $or: [
     *                 {
     *                     $and: [
     *                         {
     *                             "parentRecordId": { $exists: false }
     *                         },
     *                         {
     *                             "type": {$ne: "COMPOUND"}
     *                         }
     *                     ]
     *                 },
     *                 {
     *                     $and: [
     *                         {
     *                             "parent" : {
     *                                 $not: {
     *                                     $elemMatch: {
     *                                         "type": {
     *                                             $nin: ["COMPOUND"]
     *                                         }
     *                                     }
     *                                 }
     *                             }
     *                         },
     *                         {
     *                             "parentRecordId": { $exists: true }
     *                         }
     *                     ]
     *                 }
     *             ]
     *         }
     *     },
     *     {
     *         $limit: 1
     *     }
     * ] )
     * @param testRunId TR id
     * @param status execution status
     */
    public LogRecord findLastOrcLogRecordByTestRunAndExecutionStatus(UUID testRunId,
                                                                     ExecutionStatuses status) {
        List<AggregationOperation> aggregationOperations = new ArrayList<>();
        aggregationOperations.add(Aggregation.match(where(TEST_RUN_ID).is(testRunId)
                .and(EXECUTION_STATUS).is(status)));
        aggregationOperations.add(Aggregation.sort(Sort.Direction.DESC, CREATED_DATE_STAMP));
        aggregationOperations.add(getParentLogRecordHierarchyGraphLookup(testRunId));

        Criteria validationOr = new Criteria();
        validationOr.orOperator(
                where(PARENT_RECORD_ID).exists(false).and(TYPE).ne(TypeAction.COMPOUND),
                //every parent element type is compound
                where(PARENT).not().elemMatch(where(TYPE).nin(TypeAction.COMPOUND))
        );

        aggregationOperations.add(Aggregation.match(validationOr));
        aggregationOperations.add(Aggregation.limit(SINGLE_LIMIT));
        aggregationOperations.add(Aggregation.project(START_DATE, END_DATE));

        final Aggregation aggregation = Aggregation.newAggregation(aggregationOperations);
        log.debug("Aggregation query to get last orc log record '{}'", aggregation);

        return mongoTemplate.aggregate(aggregation, LOG_RECORD_COLLECTION_NAME,
                LogRecord.class).getUniqueMappedResult();
    }

    /**
     * Get project id by logRecord id.
     * db.logrecord.aggregate([
     *     {
     *         $match: {
     *            _id: LUUID("ff4ebd21-947b-86c0-b765-0237ac4adca4")
     *         }
     *     },
     *     {
     *         $lookup: {
     *             from: "testrun",
     *             localField: "testRunId",
     *             foreignField: "_id",
     *             as: "testrun"
     *         }
     *     },
     *     {
     *         $unwind: "$testrun"
     *     },
     *     {
     *         $lookup: {
     *             from: "executionRequests",
     *             localField: "testrun.executionRequestId",
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
    public UUID getProjectIdByLogRecordId(UUID logRecordId) {
        List<AggregationOperation> aggregationOperations = new ArrayList<>();
        aggregationOperations.add(Aggregation.match(where(_ID).is(logRecordId)));
        aggregationOperations.add(
                LookupOperation.newLookup()
                        .from(TEST_RUN)
                        .localField(TEST_RUN_ID)
                        .foreignField(_ID)
                        .as(TEST_RUN)
        );
        aggregationOperations.add(Aggregation.unwind($TEST_RUN));
        aggregationOperations.add(
                LookupOperation.newLookup()
                        .from(EXECUTION_REQUESTS)
                        .localField(TEST_RUN + "." + EXECUTION_REQUEST_ID)
                        .foreignField(_ID)
                        .as(EXECUTION_REQUEST)
        );
        aggregationOperations.add(Aggregation.unwind($EXECUTION_REQUEST));
        aggregationOperations.add(Aggregation.project().and($EXECUTION_REQUEST + "." + PROJECT_ID).as(_ID));

        final Aggregation aggregation = Aggregation.newAggregation(aggregationOperations);

        Optional<ExecutionRequest> result = Optional.ofNullable(
                mongoTemplate.aggregate(aggregation, LOG_RECORD_COLLECTION_NAME, ExecutionRequest.class)
                        .getUniqueMappedResult()
        );
        return result.map(RamObject::getUuid).orElse(null);
    }
}
