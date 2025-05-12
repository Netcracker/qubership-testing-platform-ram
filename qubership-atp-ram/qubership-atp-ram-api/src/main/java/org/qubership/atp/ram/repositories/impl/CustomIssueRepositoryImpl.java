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

import static org.qubership.atp.ram.repositories.impl.FieldConstants.FAIL_PATTERN;
import static org.qubership.atp.ram.repositories.impl.FieldConstants.FAIL_REASON;
import static org.qubership.atp.ram.repositories.impl.FieldConstants._ID;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.qubership.atp.ram.model.IssueDto;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.FailPattern;
import org.qubership.atp.ram.models.Issue;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.RamObject;
import org.qubership.atp.ram.models.RootCause;
import org.qubership.atp.ram.pojo.IssueFilteringParams;
import org.qubership.atp.ram.repositories.CustomIssueRepository;
import org.qubership.atp.ram.repositories.operations.CustomLookupOperation;
import org.qubership.atp.ram.utils.SortUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.MongoExpression;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationExpression;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationPipeline;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;
import org.springframework.data.mongodb.core.aggregation.CountOperation;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.SetOperators;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@RequiredArgsConstructor
@Slf4j
@Repository
public class CustomIssueRepositoryImpl implements CustomIssueRepository {

    private final MongoTemplate mongoTemplate;

    private static final String ISSUE_COLLECTION_NAME =
            Issue.class.getAnnotation(Document.class).collection();
    private static final String FAIL_REASON_COLLECTION_NAME =
            RootCause.class.getAnnotation(Document.class).collection();
    private static final String FAIL_PATTERN_COLLECTION_NAME =
            FailPattern.class.getAnnotation(Document.class).collection();
    private static final String LOG_RECORD_COLLECTION_NAME =
            LogRecord.class.getAnnotation(Document.class).collection();
    private static final String REGEX = ".*?";
    private static final String EMPTY_ARRAY = "emptyArray";
    private static final String ISSUE_MESSAGE = "issue_message";
    private static final String ISSUE_LRS = "issue_lrs";
    private static final String LOGRECORDS = "logrecords";

    /**
     * Example of generated query:
     * db.issue.aggregate( [
     * {
     *     $match: {
     *         executionRequestId: executionRequestIdVar
     *     }
     * },
     *  {
     *      $match : {
     *          message : {
     *              $regex:  /.*?message.*?/
     *           }
     *      }
     * },
     *  {
     *      $match : {
     *          jiraTicket : {
     *              $regex:  /.*?ATPII-1.*?/
     *           }
     *      }
     * },     *
     * {
     *     $lookup: {
     *         from: "failPattern",
     *         localField: "failPatternId",
     *         foreignField: "_id",
     *         as: "failPattern"
     *     }
     * },
     * {
     *     $unwind: {
     *         path: "$failPattern",
     *         preserveNullAndEmptyArrays: true
     *     }
     * },
     *  {
     *      $match : {
     *          "failPattern.name" : {
     *              $regex:  /.*?fail pattern.*?/
     *           }
     *      }
     * },
     * {
     *     $lookup: {
     *         from: "rootCause",
     *         localField: "failReasonId",
     *         foreignField: "_id",
     *         as: "failReason"
     *     }
     * },
     * {
     *     $unwind: {
     *         path: "$failReason",
     *         preserveNullAndEmptyArrays: true
     *     }
     * },
     * {
     *      $match : {
     *          "failReason.name" : {
     *              $regex:  /.*?fail reason.*?/
     *          }
     *      }
     * },
     * {
     *     $facet: {
     *         metadata: [
     *             {
     *                 $count: "totalCount"
     *             }
     *         ],
     *         data: [
     *             {
     *                 $skip: 5
     *             },
     *             {
     *                 $limit: 5
     *             }
     *         ]
     *     }
     * }
     * ] )
     */
    @Override
    public IssueDto getSortedAndPaginatedIssuesByFilters(IssueFilteringParams issueFilteringParams,
                                                         String columnType,
                                                         String sortType, int startIndex, int endIndex) {
        List<AggregationOperation> aggregationOperations = new ArrayList<>();
        if (issueFilteringParams.getExecutionRequestId() != null) {
            aggregationOperations.add(Aggregation.match(where(Issue.EXECUTION_REQUEST_ID_FIELD)
                    .is(issueFilteringParams.getExecutionRequestId())));
        }
        if (issueFilteringParams.getLogRecordIds() != null) {
            aggregationOperations.add(Aggregation.match(where(Issue.LOG_RECORD_IDS_FIELD)
                    .in(issueFilteringParams.getLogRecordIds())));
        }
        if (issueFilteringParams.getPriority() != null) {
            aggregationOperations.add(Aggregation.match(where(Issue.PRIORITY_FIELD)
                    .is(issueFilteringParams.getPriority())));
        }
        if (issueFilteringParams.getMessage() != null) {
            aggregationOperations.add(Aggregation.match(where(Issue.MESSAGE_FIELD)
                    .regex(REGEX + issueFilteringParams.getMessage() + REGEX)));
        }
        if (issueFilteringParams.getJiraTicket() != null) {
            aggregationOperations.add(Aggregation.match(where(Issue.JIRA_TICKETS_FIELD)
                    .regex(REGEX + issueFilteringParams.getJiraTicket() + REGEX)));
        }
        if (issueFilteringParams.getFailPattern() != null || FAIL_PATTERN.equals(columnType)) {
            aggregationOperations.add(
                    Aggregation.lookup(FAIL_PATTERN_COLLECTION_NAME,
                            Issue.FAIL_PATTERN_ID_FIELD, _ID, FAIL_PATTERN)
            );
            aggregationOperations.add(
                    Aggregation.unwind(FAIL_PATTERN, true)
            );
        }
        if (issueFilteringParams.getFailReason() != null || FAIL_REASON.equals(columnType)) {
            aggregationOperations.add(
                    Aggregation.lookup(FAIL_REASON_COLLECTION_NAME,
                            Issue.FAIL_REASON_ID_FIELD, _ID, FAIL_REASON)
            );
            aggregationOperations.add(
                    Aggregation.unwind(FAIL_REASON, true)
            );

        }
        if (issueFilteringParams.getFailPattern() != null) {
            aggregationOperations.add(
                    Aggregation.match(where(FieldConstants.FAIL_PATTERN_NAME_FIELD)
                            .regex(REGEX + issueFilteringParams.getFailPattern() + REGEX))
            );
        }
        if (issueFilteringParams.getFailReason() != null) {
            aggregationOperations.add(
                    Aggregation.match(where(FieldConstants.FAIL_REASON_NAME_FIELD)
                            .regex(REGEX + issueFilteringParams.getFailReason() + REGEX))
            );
        }
        if (columnType != null && sortType != null) {
            if (columnType.equals("failedCasesCount") || columnType.equals("failedTestRuns")) {
                aggregationOperations.add(Aggregation.sort(getFieldSort(Issue.FAILED_TEST_RUNS_COUNT_FIELD,
                        sortType)));
            } else {
                aggregationOperations.add(Aggregation.sort(getFieldSort(columnType, sortType)));
            }
        }
        CountOperation countAggregation = Aggregation.count().as("totalCount");
        AggregationOperation[] paginationAggregation = new AggregationOperation[]{
                Aggregation.skip((long) startIndex),
                Aggregation.limit((long) endIndex - startIndex)
        };
        aggregationOperations.add(
                Aggregation.facet(countAggregation).as("metadata")
                        .and(paginationAggregation).as("data")
        );

        List<IssueDto> issues = mongoTemplate.aggregate(Aggregation.newAggregation(aggregationOperations),
                ISSUE_COLLECTION_NAME, IssueDto.class).getMappedResults();

        return issues.get(0);
    }

    private Sort getFieldSort(String columnType, String sortType) {
        if (columnType.equals("message")) {
            return Sort.by(SortUtils.parseSortDirection(sortType), Issue.MESSAGE_FIELD);
        }
        if (columnType.equals("priority")) {
            return Sort.by(SortUtils.parseSortDirection(sortType), Issue.PRIORITY_FIELD);
        }
        if (columnType.equals("jiraTickets")) {
            return Sort.by(SortUtils.parseSortDirection(sortType), Issue.JIRA_TICKETS_FIELD);
        }
        if (columnType.equals("failPattern")) {
            return Sort.by(SortUtils.parseSortDirection(sortType), FieldConstants.FAIL_PATTERN_NAME_FIELD);
        }
        if (columnType.equals("failReason")) {
            return Sort.by(SortUtils.parseSortDirection(sortType), FieldConstants.FAIL_REASON_NAME_FIELD);
        }
        if (columnType.equals("failedTestRunsCount")) {
            return Sort.by(SortUtils.parseSortDirection(sortType), Issue.FAILED_TEST_RUNS_COUNT_FIELD);
        }
        return Sort.unsorted();
    }

    /**
     * Example of generated query:
     * db.issue.aggregate([
     *     {
     *         $match: {
     *             _id: JUUID("2dcdefd1-a4a4-44fe-b608-514806d981ed")
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
     *             "_id" : "$executionRequest.projectId"
     *         }
     *     }
     * ])
     */
    @Override
    public UUID getProjectIdByIssueId(UUID issueId) {
        List<AggregationOperation> aggregationOperations = new ArrayList<>();
        aggregationOperations.add(Aggregation.match(where(_ID).is(issueId)));
        aggregationOperations.add(
                LookupOperation.newLookup()
                        .from(FieldConstants.EXECUTION_REQUESTS)
                        .localField(FieldConstants.EXECUTION_REQUEST_ID)
                        .foreignField(_ID)
                        .as(FieldConstants.EXECUTION_REQUEST)
        );
        aggregationOperations.add(Aggregation.unwind(FieldConstants.EXECUTION_REQUEST));
        aggregationOperations.add(Aggregation.project().and(FieldConstants.EXECUTION_REQUEST + "."
                        + FieldConstants.PROJECT_ID).as(_ID));

        final Aggregation aggregation = Aggregation.newAggregation(aggregationOperations);

        Optional<ExecutionRequest> result = Optional.ofNullable(
                mongoTemplate.aggregate(aggregation, ISSUE_COLLECTION_NAME, ExecutionRequest.class)
                        .getUniqueMappedResult()
        );
        return result.map(RamObject::getUuid).orElse(null);
    }

    /**
     * Example of generated query:
     * db.issue.aggregate( [
     * {
     *      "$match": {
     *          "executionRequestId": JUUID("481fde38-238f-4a78-9bd5-a7bf923babf8")
     *      }
     * },
     * {
     *      "$lookup": {
     *          "from": "logrecord",
     *          "let": {
     *              "issue_message": "$message",
     *              "issue_lrs": "$logRecordIds"
     *          },
     *          "pipeline": [
     *              {
     *                  "$match": {
     *                      "_id": {
     *                          "$in": [JUUID("481fde38-238f-4a78-9bd5-a7bf923babf8")]
     *                      }
     *                  }
     *              },
     *              {
     *                  "$match": {
     *                      "$expr": {
     *                          "$and": [
     *                              { "$eq": ["$message", "$$issue_message"]},
     *                              { "$not": {
     *                                  "$in": ["$_id","$$issue_lrs"]
     *                                  }
     *                              }
     *                          ]
     *                      }
     *                  }
     *              },
     *              {
     *                  "$project": {
     *                      "_id": 1,
     *                      "testRunId": 1,
     *                      "emptyArray": []
     *                  }
     *              }
     *          ],
     *          "as": "logrecords"
     *      }
     * },
     * {
     *      "$match": {
     *          "logrecords": {
     *              "$ne": []
     *          }
     *      }
     * },
     * {
     *      "$project": {
     *          "logRecordIds": {
     *              "$setDifference": [
     *                  {
     *                      "$concatArrays": ["$logRecordIds","$logrecords._id"]
     *                  },
     *                  "$logrecords.emptyArray"
     *              ]
     *          },
     *          "failedTestRunIds": {
     *              "$setDifference": [
     *                  {
     *                      "$concatArrays": ["$failedTestRunIds","$logrecords.testRunId"]
     *                  },
     *                  "$logrecords.emptyArray"
     *              ]
     *          },
     *          "failedTestRunsCount": {
     *              "$size": {
     *                  "$setDifference": [
     *                      {
     *                          "$concatArrays": ["$failedTestRunIds","$logrecords.testRunId"]
     *                      },
     *                      "$logrecords.emptyArray"
     *                  ]
     *              }
     *          },
     *          "executionRequestId": 1,
     *          "message": 1
     *      }
     * }
     * ])
     */
    public List<Issue> getCreatedIssuesByLogRecordsMessage(List<UUID> logRecords, UUID executionRequestId) {
        List<AggregationOperation> aggregationOperations = new ArrayList<>();
        aggregationOperations.add(Aggregation.match(where(Issue.EXECUTION_REQUEST_ID_FIELD).is(executionRequestId)));

        MatchOperation matchOperation = Aggregation.match(where(_ID).in(logRecords));
        MatchOperation pipelineMatch = Aggregation.match(
                        AggregationExpression.from(MongoExpression.create("$expr: {"
                                + "$and: ["
                                + "{$eq: ['$message', '$$issue_message']},"
                                + "{$not: { $in: ['$_id', '$$issue_lrs']}}"
                                + "]"
                                + "}")));
        ProjectionOperation projectionOperation = Aggregation.project(_ID)
                .andInclude(FieldConstants.TEST_RUN_ID)
                .andArrayOf(Collections.emptyList()).as(EMPTY_ARRAY);
        aggregationOperations.add(new CustomLookupOperation(
                LOG_RECORD_COLLECTION_NAME,
                new org.bson.Document(ISSUE_MESSAGE, FieldConstants.OPERATOR + FieldConstants.MESSAGE)
                        .append(ISSUE_LRS, FieldConstants.OPERATOR + Issue.LOG_RECORD_IDS_FIELD),
                new AggregationPipeline(Arrays.asList(matchOperation, pipelineMatch, projectionOperation)),
                LOGRECORDS));

        SetOperators.SetDifference setDifference = SetOperators.SetDifference
                .arrayAsSet(ArrayOperators.ConcatArrays
                        .arrayOf(FieldConstants.OPERATOR + Issue.FAILED_TEST_RUNS_IDS_FIELD)
                        .concat("$logrecords.testRunId"))
                .differenceTo("$logrecords.emptyArray");
        aggregationOperations.add(Aggregation.match(where(LOGRECORDS).ne(Collections.emptyList())));
        aggregationOperations.add(Aggregation.project()
                .and(SetOperators.SetDifference
                        .arrayAsSet(ArrayOperators.ConcatArrays
                                .arrayOf(FieldConstants.OPERATOR + Issue.LOG_RECORD_IDS_FIELD)
                                .concat("$logrecords._id"))
                        .differenceTo("$logrecords.emptyArray"))
                .as(Issue.LOG_RECORD_IDS_FIELD)
                .and(setDifference)
                .as(Issue.FAILED_TEST_RUNS_IDS_FIELD)
                .and(ArrayOperators.Size.lengthOfArray(setDifference))
                .as(FieldConstants.FAIL_TEST_RUNS_COUNT_FIELD)
                .andInclude(Issue.EXECUTION_REQUEST_ID_FIELD, Issue.MESSAGE_FIELD));

        return mongoTemplate
                .aggregate(Aggregation.newAggregation(aggregationOperations), ISSUE_COLLECTION_NAME, Issue.class)
                .getMappedResults();
    }
}
