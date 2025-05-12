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

import static java.sql.Timestamp.valueOf;
import static java.time.LocalDateTime.now;
import static java.util.Collections.singletonList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.qubership.atp.ram.repositories.impl.FieldConstants.CREATED_DATE;
import static org.qubership.atp.ram.repositories.impl.FieldConstants.EXECUTION_REQUESTS;
import static org.qubership.atp.ram.repositories.impl.FieldConstants.EXECUTION_REQUEST_ID;
import static org.qubership.atp.ram.repositories.impl.FieldConstants.JIRA_DEFECTS;
import static org.qubership.atp.ram.repositories.impl.FieldConstants.JIRA_TICKETS;
import static org.qubership.atp.ram.repositories.impl.FieldConstants.RESOLVED;
import static org.qubership.atp.ram.repositories.impl.FieldConstants.START_DATE;
import static org.qubership.atp.ram.repositories.impl.FieldConstants.URL;
import static org.qubership.atp.ram.repositories.impl.FieldConstants._ID;
import static org.qubership.atp.ram.utils.StreamUtils.extractIds;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.addFields;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.project;
import static org.springframework.data.mongodb.core.aggregation.AggregationUpdate.from;
import static org.springframework.data.mongodb.core.aggregation.ObjectOperators.MergeObjects.merge;
import static org.springframework.data.mongodb.core.aggregation.VariableOperators.mapItemsOf;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.sql.Timestamp;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bson.Document;
import org.qubership.atp.ram.migration.mongoevolution.java.annotation.ChangeLog;
import org.qubership.atp.ram.migration.mongoevolution.java.annotation.ChangeSet;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.FailPattern;
import org.qubership.atp.ram.models.Issue;
import org.qubership.atp.ram.models.JiraTicket;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Query;

import com.mongodb.client.result.UpdateResult;
import lombok.extern.slf4j.Slf4j;

@SuppressWarnings("PMD.TooManyStaticImports")
@ChangeLog(version = 14)
@Slf4j
public class V14MigrateJiraTicketsData {

    public static final String TICKET = "ticket";
    public static final String $$TICKET = "$$ticket";

    /**
     * Migrate jira tickets data for issues and fail patterns.
     */
    @ChangeSet(order = 1)
    public void run(MongoTemplate mongoTemplate) {
        final String processName = getClass().getName();
        log.info("Start mongo evolution process: {}", processName);

        final Timestamp currentDate = valueOf(now());

        updateFailPatterns(mongoTemplate, currentDate);
        updateIssues(mongoTemplate, currentDate);

        log.info("End mongo evolution process: {}", processName);
    }

    private void updateIssues(MongoTemplate mongoTemplate, Timestamp currentDate) {
        final List<ExecutionRequest> last5DaysExecutionRequests = loadLastDaysExecutionRequests(mongoTemplate, 5);
        final Set<UUID> last5DaysExecutionRequestIds = extractIds(last5DaysExecutionRequests);
        final UpdateResult updateResult = mongoTemplate.updateMulti(
                new Query().addCriteria(
                        where(EXECUTION_REQUEST_ID).in(last5DaysExecutionRequestIds)
                ),
                from(singletonList(
                        addFields().addField(JIRA_DEFECTS).withValueOf(
                                mapItemsOf(JIRA_TICKETS)
                                        .as(TICKET)
                                        .andApply(merge(singletonList(
                                                new Document()
                                                        .append(URL, $$TICKET)
                                                        .append(CREATED_DATE, currentDate)
                                                        .append(RESOLVED, false)
                                        ))))
                                .build()
                )),
                Issue.class
        );
        log.debug("Modified issues: {}", updateResult.getModifiedCount());
    }

    private void updateFailPatterns(MongoTemplate mongoTemplate, Timestamp currentDate) {
        log.info("Start updating fail patterns with jira defects");
        List<FailPattern> failPatterns = mongoTemplate.find(new Query(), FailPattern.class);
        failPatterns.forEach(failPattern -> {
            final List<String> jiraTickets = failPattern.getJiraTickets();
            if (!isEmpty(jiraTickets)) {
                final List<JiraTicket> jiraDefects = jiraTickets.stream()
                        .map(jiraTicket -> new JiraTicket(jiraTicket, currentDate, false))
                        .collect(Collectors.toList());
                failPattern.setJiraDefects(jiraDefects);

                log.debug("Update fail pattern '{}', jira defects: {}", failPattern.getUuid(), jiraTickets);
                mongoTemplate.save(failPattern);
            }
        });
        log.info("End updating fail patterns with jira defects");
    }

    private List<ExecutionRequest> loadLastDaysExecutionRequests(MongoTemplate mongoTemplate, int days) {
        final Timestamp fromDate = valueOf(now().minusDays(days));

        final Aggregation aggregation = Aggregation.newAggregation(
                match(where(START_DATE).gt(fromDate)),
                project(_ID)
        );
        log.debug("Aggregation query '{}'", aggregation);

        return mongoTemplate.aggregate(aggregation, EXECUTION_REQUESTS, ExecutionRequest.class).getMappedResults();
    }
}
