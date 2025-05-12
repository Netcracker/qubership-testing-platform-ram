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
import java.util.List;
import java.util.UUID;

import org.qubership.atp.ram.dto.response.PaginationResponse;
import org.qubership.atp.ram.models.BrowserConsoleLog;
import org.qubership.atp.ram.models.BrowserConsoleLogsTable;
import org.qubership.atp.ram.repositories.CustomBrowserConsoleLogRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Repository
@Slf4j
public class CustomBrowserConsoleLogRepositoryImpl implements CustomBrowserConsoleLogRepository, FieldConstants {

    private final MongoTemplate mongoTemplate;
    private static final String BROWSER_CONSOLE_LOG_COLLECTION_NAME
            = BrowserConsoleLog.class.getAnnotation(Document.class).collection();

    /**
     * Example of generated query:
     * [
     * {
     * "$match": {
     * "logRecordId": {
     * "testCaseId": JUUID("9e432fe5-7e6c-5cd8-37bd-03ea7ef85296")
     * }
     * }
     * },
     * {
     * "$unwind": "$browserConsoleLogsTable"
     * },
     * {
     * "$project": {
     * "message": "$browserConsoleLogsTable.message",
     * "timestamp": "$browserConsoleLogsTable.timestamp",
     * "level": "$browserConsoleLogsTable.level",
     * "fileName": "$browserConsoleLogsTable.fileName"
     * }
     * },
     * {
     * "$sort": {
     * "timestamp": -1
     * }
     * },
     * {
     * "$facet": {
     * "entities": [
     * {
     * "$skip": {
     * "$numberLong": "0"
     * }
     * },
     * {
     * "$limit": {
     * "$numberLong": "5"
     * }
     * }
     * ],
     * "metadata": [
     * {
     * "$count": "totalCount"
     * }
     * ]
     * }
     * },
     * {
     * "$project": {
     * "entities": 1,
     * "totalCount": {
     * "$arrayElemAt": [
     * "$metadata.totalCount",
     * 0
     * ]
     * }
     * }
     * }
     * ]
     * @param logRecordId LodRecord identifier
     * @param pageable    Pageable
     * @return browser console logs table list and totalCount
     */

    @Override
    public PaginationResponse<BrowserConsoleLogsTable> findBrowserConsoleLogsByLogRecordIdWithPagination(
            UUID logRecordId, Pageable pageable) {
        List<AggregationOperation> aggregationOperations = new ArrayList<>();
        aggregationOperations.add(Aggregation.match(where(LOG_RECORD_ID).is(logRecordId)));
        aggregationOperations.add(Aggregation.unwind($BROWSER_CONSOLE_LOGS_TABLE));
        aggregationOperations.add(Aggregation.project()
                .andExpression($BROWSER_CONSOLE_LOGS_TABLE + "." + MESSAGE).as(MESSAGE)
                .andExpression($BROWSER_CONSOLE_LOGS_TABLE + "." + TIMESTAMP).as(TIMESTAMP)
                .andExpression($BROWSER_CONSOLE_LOGS_TABLE + "." + LEVEL).as(LEVEL)
                .andExpression($BROWSER_CONSOLE_LOGS_TABLE + "." + FILE_NAME).as(FILE_NAME));
        aggregationOperations.add(Aggregation.sort(pageable.getSort()));
        aggregationOperations.add(
                Aggregation.facet(Aggregation.skip(pageable.getPageNumber() * pageable.getPageSize()),
                                Aggregation.limit(pageable.getPageSize())).as(ENTITIES)
                        .and(Aggregation.count().as(TOTAL_COUNT)).as(METADATA)
        );
        aggregationOperations.add(Aggregation.project(ENTITIES)
                .and(ArrayOperators.ArrayElemAt.arrayOf($METADATA + "." + TOTAL_COUNT).elementAt(0)).as(TOTAL_COUNT)
        );
        final Aggregation aggregation = Aggregation.newAggregation(aggregationOperations);
        log.debug("Aggregation query '{}'", aggregation.toString());
        return mongoTemplate.aggregate(aggregation, BROWSER_CONSOLE_LOG_COLLECTION_NAME,
                BrowserConsoleLogPaginationResponse.class).getUniqueMappedResult();
    }

    private static class BrowserConsoleLogPaginationResponse extends PaginationResponse<BrowserConsoleLogsTable> {

    }
}
