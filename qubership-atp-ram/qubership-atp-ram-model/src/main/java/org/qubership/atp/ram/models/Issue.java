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

package org.qubership.atp.ram.models;

import static org.springframework.util.CollectionUtils.isEmpty;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = Issue.COLLECTION_NAME)
public class Issue extends RamObject {

    public static final String COLLECTION_NAME = "issue";
    public static final String PRIORITY_FIELD = "priority";
    public static final String JIRA_TICKETS_FIELD = "jiraTickets";
    public static final String JIRA_DEFECTS_FIELD = "jiraDefects";
    public static final String FAIL_PATTERN_ID_FIELD = "failPatternId";
    public static final String FAIL_REASON_ID_FIELD = "failReasonId";
    public static final String MESSAGE_FIELD = "message";
    public static final String FAILED_TEST_RUNS_IDS_FIELD = "failedTestRunIds";
    public static final String FAILED_TEST_RUNS_COUNT_FIELD = "failedTestRunsCount";
    public static final String LOG_RECORD_IDS_FIELD = "logRecordIds";
    public static final String EXECUTION_REQUEST_ID_FIELD = "executionRequestId";

    private DefectPriority priority;

    @Field(JIRA_TICKETS_FIELD)
    private List<String> jiraTickets;

    private List<JiraTicket> jiraDefects;

    @Field(FAIL_PATTERN_ID_FIELD)
    private UUID failPatternId;

    @Field(FAIL_REASON_ID_FIELD)
    private UUID failReasonId;

    @Field(MESSAGE_FIELD)
    private String message;

    @Field(FAILED_TEST_RUNS_IDS_FIELD)
    private List<UUID> failedTestRunIds;

    @Field(LOG_RECORD_IDS_FIELD)
    private List<UUID> logRecordIds;

    @Field(EXECUTION_REQUEST_ID_FIELD)
    @Indexed(background = true)
    private UUID executionRequestId;

    @Field(FAILED_TEST_RUNS_COUNT_FIELD)
    private int failedTestRunsCount;


    /**
     * Propagate jira tickets.
     */
    public void propagateJiraTickets() {
        if (!isEmpty(jiraDefects)) {
            jiraTickets = jiraDefects.stream()
                    .map(JiraTicket::getUrl)
                    .collect(Collectors.toList());
        }
    }
}
