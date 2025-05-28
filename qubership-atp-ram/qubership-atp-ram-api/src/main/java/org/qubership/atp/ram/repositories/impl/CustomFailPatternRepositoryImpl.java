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
import static org.qubership.atp.ram.models.RootCauseType.GLOBAL;
import static org.springframework.data.mongodb.core.aggregation.ComparisonOperators.Eq.valueOf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.qubership.atp.ram.dto.response.BaseEntityResponse;
import org.qubership.atp.ram.dto.response.PaginationResponse;
import org.qubership.atp.ram.models.DefectPriority;
import org.qubership.atp.ram.models.FailPattern;
import org.qubership.atp.ram.models.FailPatternSearchRequest;
import org.qubership.atp.ram.models.PaginationSearchRequest;
import org.qubership.atp.ram.models.RootCause;
import org.qubership.atp.ram.repositories.CustomFailPatternRepository;
import org.qubership.atp.ram.utils.StreamUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators.Switch;
import org.springframework.data.mongodb.core.aggregation.StringOperators;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@Repository
public class CustomFailPatternRepositoryImpl implements CustomFailPatternRepository {

    private static final String FAIL_PATTERN_COLLECTION_NAME =
            FailPattern.class.getAnnotation(Document.class).collection();

    private static final String ROOT_CAUSE_COLLECTION_NAME =
            RootCause.class.getAnnotation(Document.class).collection();

    public static final String NAME = "name";
    public static final String PRIORITY = "priority";
    public static final String MESSAGE = "message";
    public static final String JIRA_TICKETS = "jiraTickets";
    public static final String $JIRA_TICKETS = "$jiraTickets";
    public static final String DISTINCT_JIRA_TICKETS = "distinctTickets";
    public static final String $DISTINCT_JIRA_TICKETS = "$distinctTickets";
    public static final String PROJECT_ID = "projectId";
    public static final String FAIL_REASON_ID = "failReasonId";
    public static final String TYPE = "type";

    private final MongoTemplate mongoTemplate;
    private final GenericEntitySearchComponent searchComponent;

    /**
     * Find all fail patterns by search request.
     * Pipeline example:
     * [
     *      {
     *          "$match": {
     *              "$and": [
     *                  {
     *                      "$or": [
     *                          {
     *                              "name": {
     *                                  "$regularExpression": {
     *                                      "pattern": ".*Test.*",
     *                                      "options": "i"
     *                                  }
     *                              }
     *                          }
     *                      ]
     *                  },
     *                  {
     *                      "priority": {
     *                          "$in": [
     *                              "LOW",
     *                              "Normal"
     *                          ]
     *                      }
     *                  },
     *                  {
     *                      "message": {
     *                          "$regularExpression": {
     *                              "pattern": ".*RuntimeException.*",
     *                              "options": "i"
     *                           }
     *                       }
     *                   },
     *                {
     *                  "jiraTickets": {
     *                      "$in": [
     *                          "https://service-address/browse/PRJ-98765",
     *                          "https://service-address/browse/PRJ-98764"
     *                      ]
     *                  }
     *                },
     *                {
     *                  "projectId": {
     *                      "$in": ["75ff7376-7231-4f8f-8144-ede2a5ea9762", "306fcb4e-8b5d-4608-9bb0-7ad16ae6df62"]
     *                  }
     *                },
     *                {
     *                  "failReasonId": {
     *                      "$in": ["c84285ca-ab28-449c-a32a-c0a194dbc2f8", "e928764f-81d1-4b7f-b95f-4430fed898ce"]
     *                  }
     *                }
     *              ]
     *        }
     *    },
     *    {
     *      "$sort": {
     *          "name": -1
     *      }
     *    },
     *    {
     *      "$skip": 0
     *    },
     *    {
     *      "$limit": 5
     *    }
     * ]
     *
     * @param request  search request
     * @param pageable pageable
     * @return list of fail patterns
     */
    @Override
    public PaginationResponse<FailPattern> findAllFailPatterns(FailPatternSearchRequest request, Pageable pageable) {
        log.info("Search fail patterns by request: {}", request);
        final List<AggregationOperation> aggregation = new ArrayList<>();
        final List<Criteria> criteria = new ArrayList<>();

        final Set<String> names = request.getNames();
        if (CollectionUtils.isNotEmpty(names)) {
            criteria.add(searchComponent.namesRegexIgnoreCase(NAME, names));
        }

        final Set<String> priorities = request.getPriorities();
        if (CollectionUtils.isNotEmpty(priorities)) {
            criteria.add(searchComponent.foundInArrayStrings(PRIORITY, StreamUtils.toUpperCase(priorities)));
        }

        final String message = request.getMessage();
        if (StringUtils.isNotEmpty(message)) {
            criteria.add(searchComponent.namesRegexIgnoreCase(MESSAGE, message));
        }

        final Set<String> issues = request.getIssues();
        if (CollectionUtils.isNotEmpty(issues)) {
            criteria.add(searchComponent.foundInArrayStrings(JIRA_TICKETS, issues));
        }

        final Set<UUID> projectIds = request.getProjects();
        if (CollectionUtils.isNotEmpty(projectIds)) {
            criteria.add(searchComponent.foundInArrayIds(PROJECT_ID, projectIds));
        }

        final Set<UUID> failReasonIds = request.getFailReasons();
        if (CollectionUtils.isNotEmpty(failReasonIds)) {
            criteria.add(searchComponent.foundInArrayIds(FAIL_REASON_ID, failReasonIds));
        }

        log.debug("Criteria for search fail patterns: {}", criteria);
        aggregation.add(criteria.isEmpty() ? Aggregation.match(new Criteria())
                : Aggregation.match(new Criteria().andOperator(criteria.toArray(new Criteria[0]))));

        final Query query = new Query();
        query.addCriteria(new Criteria().andOperator(criteria.toArray(new Criteria[0])));

        addSortOperations(aggregation, pageable.getSort());
        addPaginationOperations(aggregation, pageable);

        long count = mongoTemplate.count(query, FailPattern.class);
        log.debug("Count of fail patterns: {}", count);

        log.debug("Aggregation for search fail patterns: {}", aggregation);
        List<FailPattern> failPatterns = mongoTemplate.aggregate(Aggregation.newAggregation(aggregation),
                FAIL_PATTERN_COLLECTION_NAME, FailPattern.class).getMappedResults();
        log.debug("Found fail patterns: {}", StreamUtils.extractIds(failPatterns));

        return new PaginationResponse<>(failPatterns, count);
    }

    /**
     * Get all issues with pagination.
     * Pipeline example:
     * [
     *  {
     *      "$match": {
     *          "projectId": {
     *              "$in": ["9c98f53f-75e8-487a-a893-00d28df4d0ac"]
     *          }
     *      }
     *    },
     *    {
     *      "$match": {
     *          "jiraTickets": {
     *              "$regularExpression": {
     *                  "pattern": ".*ATP.*",
     *                  "options": "i"
     *              }
     *          }
     *       }
     *    },
     *    {
     *      "$unwind": "$jiraTickets"
     *    },
     *    {
     *      "$group": {
     *          "_id": null,
     *          "distinctTickets": {
     *              "$addToSet": "$jiraTickets"
     *           }
     *       }
     *    },
     *    {
     *      "$unwind": "$distinctTickets"
     *    },
     *    {
     *      "$sort": {
     *          "distinctTickets": 1
     *       }
     *    },
     *    {
     *      "$project": {
     *          "name": "$distinctTickets"
     *       }
     *    },
     *    {
     *      "$skip": 0
     *    },
     *    {
     *      "$limit": 100
     *    }
     * ]
     *
     * @param request search request
     */
    @Override
    public PaginationResponse getAllIssuesWithPagination(PaginationSearchRequest request) {
        log.info("Search all issues by request: {}", request);
        final List<AggregationOperation> aggregation = new ArrayList<>();
        final List<Criteria> criteria = new ArrayList<>();

        final Set<UUID> projectIds = request.getProjects();
        if (CollectionUtils.isNotEmpty(projectIds)) {
            aggregation.add(Aggregation.match(searchComponent.foundInArrayIds(PROJECT_ID, projectIds)));
        }

        final String name = request.getName();
        if (StringUtils.isNotEmpty(name)) {
            aggregation.add(Aggregation.match(searchComponent.namesRegexIgnoreCase(JIRA_TICKETS, name)));
        }

        aggregation.add(Aggregation.unwind($JIRA_TICKETS));

        aggregation.add(Aggregation.group().addToSet(JIRA_TICKETS).as(DISTINCT_JIRA_TICKETS));

        aggregation.add(Aggregation.unwind(DISTINCT_JIRA_TICKETS));

        final Sort.Direction direction = nonNull(request.getDirection()) ? request.getDirection() : Sort.Direction.ASC;
        aggregation.add(Aggregation.sort(Sort.by(direction, DISTINCT_JIRA_TICKETS)));

        aggregation.add(Aggregation.project().and($DISTINCT_JIRA_TICKETS).as(NAME));

        addPaginationOperations(aggregation, request);

        log.debug("Aggregation for search all issues: {}", aggregation);
        List<BaseEntityResponse> issues = mongoTemplate.aggregate(Aggregation.newAggregation(aggregation),
                FAIL_PATTERN_COLLECTION_NAME, BaseEntityResponse.class).getMappedResults();

        int totalCount = getTotalCount(aggregation, FAIL_PATTERN_COLLECTION_NAME);

        boolean isLastPage = isLastPage(request, issues.size(), totalCount);

        return new PaginationResponse(issues, totalCount, isLastPage);
    }

    /**
     * Get all fail reasons with pagination.
     * Pipeline example:
     *[
     *  {
     *      "$match": {
     *          "name": {
     *              "$regularExpression": {
     *                  "pattern": ".*ATP.*",
     *                  "options": "i"
     *                }
     *            }
     *        }
     *    },
     *    {
     *      "$sort": {
     *          "name": 1
     *        }
     *    },
     *    {
     *      "$skip": 0
     *    },
     *    {
     *      "$limit": 10
     *    }
     * ]
     * @param request search request
     */
    @Override
    public PaginationResponse getAllFailReasonsWithPagination(PaginationSearchRequest request) {
        log.info("Search all fail reasons by request: {}", request);
        final List<AggregationOperation> aggregation = new ArrayList<>();
        final List<Criteria> criteria = new ArrayList<>();

        final Set<UUID> projectIds = request.getProjects();
        if (CollectionUtils.isNotEmpty(projectIds)) {
            aggregation.add(Aggregation.match(new Criteria().orOperator(
                    searchComponent.foundInArrayIds(PROJECT_ID, projectIds), Criteria.where(TYPE).is(GLOBAL.name()))
            ));
        }

        final String name = request.getName();
        if (StringUtils.isNotEmpty(name)) {
            aggregation.add(Aggregation.match(searchComponent.namesRegexIgnoreCase(NAME, name)));
        }

        final Sort.Direction direction = nonNull(request.getDirection()) ? request.getDirection() : Sort.Direction.ASC;
        aggregation.add(Aggregation.sort(Sort.by(direction, NAME)));

        addPaginationOperations(aggregation, request);

        log.debug("Aggregation for search all fail reasons: {}", aggregation);
        List<BaseEntityResponse> failPatterns = mongoTemplate.aggregate(Aggregation.newAggregation(aggregation),
                ROOT_CAUSE_COLLECTION_NAME, BaseEntityResponse.class).getMappedResults();

        int totalCount = getTotalCount(aggregation, ROOT_CAUSE_COLLECTION_NAME);

        boolean isLastPage = isLastPage(request, failPatterns.size(), totalCount);

        return new PaginationResponse(failPatterns, totalCount, isLastPage);
    }

    private boolean isLastPage(PaginationSearchRequest request, int filteredCount, int totalCount) {
        return request.getPage() * request.getSize() + filteredCount >= totalCount;
    }

    private void addPaginationOperations(List<AggregationOperation> aggregation, Pageable pageable) {
        addPaginationOperations(aggregation, pageable.getPageNumber(), pageable.getPageSize());
    }

    private void addPaginationOperations(List<AggregationOperation> aggregation, PaginationSearchRequest request) {
        addPaginationOperations(aggregation, request.getPage(), request.getSize());
    }

    private void addPaginationOperations(List<AggregationOperation> aggregation, int pageNumber, int pageSize) {
        aggregation.add(Aggregation.skip(pageNumber * pageSize));
        aggregation.add(Aggregation.limit(pageSize));
    }

    private int getTotalCount(List<AggregationOperation> aggregation, String collectionName) {
        List<AggregationOperation> copy = new ArrayList<>(aggregation);
        copy.add(Aggregation.count().as("totalCount"));

        Map result = mongoTemplate.aggregate(Aggregation.newAggregation(copy),
                collectionName, Map.class).getUniqueMappedResult();

        return (int) result.get("totalCount");
    }

    private void addSortOperations(List<AggregationOperation> aggregation, Sort sort) {
        final boolean isSorted = sort.isSorted();
        log.debug("Is sorted: {}", isSorted);

        if (isSorted) {
            sort.get().findFirst().ifPresent(order -> {
                String sortingProperty = order.getProperty();
                log.debug("Sorting by: {}", sortingProperty);

                switch (sortingProperty) {
                    case "priority":
                        log.debug("Sorting by priority");
                        final List<Switch.CaseOperator> caseOperators = DefectPriority.getAll().stream()
                                .map(priority -> Switch.CaseOperator
                                        .when(valueOf("priority").equalToValue(priority.name()))
                                        .then(priority.getId()))
                                .collect(Collectors.toList());

                        aggregation.add(Aggregation.addFields().addFieldWithValue("priorityNumberValue",
                                Switch.switchCases(caseOperators).defaultTo(-1)).build());

                        sortingProperty = "priorityNumberValue";
                        break;
                    case "issue":
                        log.debug("Sorting by issue");
                        aggregation.add(Aggregation.addFields().addFieldWithValue("firstJiraTicket",
                                ArrayOperators.ArrayElemAt.arrayOf("jiraTickets").elementAt(0))
                                .build());
                        aggregation.add(Aggregation.addFields().addFieldWithValue("firstJiraTicket",
                                ArrayOperators.ArrayElemAt.arrayOf(StringOperators.Split.valueOf("$firstJiraTicket")
                                        .split("/")).elementAt(-1))
                                .build());
                        sortingProperty = "firstJiraTicket";
                        break;
                    case "project":
                        log.debug("Sorting by project");
                        aggregation.add(Aggregation.lookup("projects", "projectId", "_id", "projectInfo"));
                        sortingProperty = "projectInfo.name";
                        break;
                    case "failReason":
                        log.debug("Sorting by fail reason");
                        aggregation.add(Aggregation.lookup("rootCause", "failReasonId", "_id", "failReasonInfo"));
                        sortingProperty = "failReasonInfo.name";
                        break;
                    case "name":
                    case "message":
                    case "rule":
                    case "patternDescription":
                        log.debug("Skip custom sorting by string property: {}", sortingProperty);
                        // do nothing, default string order
                        break;
                    default:
                        log.warn("Unknown sorting property: {}", sortingProperty);
                }

                final Sort.Direction direction = order.getDirection();
                log.debug("Sorting direction: {}", direction);

                aggregation.add(Aggregation.sort(Sort.by(direction, sortingProperty)));
            });
        }
    }
}
