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

import java.sql.Timestamp;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Document(collection = "executionRequests")
@CompoundIndexes({
        @CompoundIndex(name = "_id_testPlanId", def = "{'_id': 1, 'testPlanId': 1}"),
        @CompoundIndex(name = "projectId_executionStatus_finishDate",
                def = "{'projectId': 1, 'executionStatus': 1, 'finishDate': 1}"),
})
public class ExecutionRequest extends RamObject {

    public static final String EXECUTION_STATUS_FIELD = "executionStatus";
    public static final String TEST_PLAN_ID_FIELD = "testPlanId";
    public static final String ANALYZED_BY_QA_FIELD = "analyzedByQa";
    public static final String START_DATE_FIELD = "startDate";
    public static final String PASSED_RATE_FIELD = "passedRate";
    public static final String WARNING_RATE_FIELD = "warningRate";
    public static final String FAILED_RATE_FIELD = "failedRate";
    public static final String ENVIRONMENT_ID = "environmentId";
    public static final String PROJECT_ID = "projectId";
    public static final String INITIAL_EXECUTION_REQUEST_ID = "initialExecutionRequestId";

    private UUID previousExecutionRequestId;
    @Field(PROJECT_ID)
    private UUID projectId;
    @Field(TEST_PLAN_ID_FIELD)
    private UUID testPlanId;
    @Field(EXECUTION_STATUS_FIELD)
    private ExecutionStatuses executionStatus;
    @Field(ANALYZED_BY_QA_FIELD)
    private boolean analyzedByQa;
    @Field(PASSED_RATE_FIELD)
    private int passedRate;
    @Field(WARNING_RATE_FIELD)
    private int warningRate;
    @Field(FAILED_RATE_FIELD)
    private int failedRate;
    private int countOfTestRuns;
    private String solutionBuild;
    @Field(START_DATE_FIELD)
    @Indexed(background = true)
    private Timestamp startDate;
    private Timestamp finishDate;
    private long duration;
    private String legacyMailRecipients;
    private String ciJobUrl;
    private UUID testScopeId;
    @Field(ENVIRONMENT_ID)
    private UUID environmentId;
    private UUID taToolsGroupId;
    private List<UUID> labels;
    private UUID executorId;
    private String executorName;
    private UUID labelTemplateId;
    private UUID widgetConfigTemplateId;
    private int threads;
    private int numberOfStarts;

    private boolean autoSyncCasesWithJira;
    private boolean autoSyncRunsWithJira;

    private UUID emailTemplateId;
    private String emailSubject;
    private UUID logCollectorConditionId;
    private Set<UUID> flagIds;
    private Long countLogRecords;
    private Set<UUID> filteredByLabels;
    private int failedLogrecordsCounter;
    private String jointExecutionKey;
    private Integer jointExecutionCount;
    private Integer jointExecutionTimeout;
    @Field(INITIAL_EXECUTION_REQUEST_ID)
    private UUID initialExecutionRequestId;
    private boolean virtual;
}
