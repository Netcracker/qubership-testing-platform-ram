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

package org.qubership.atp.ram.service.rest.server.executor.request;

import java.sql.Timestamp;
import java.util.Set;
import java.util.UUID;

import org.qubership.atp.ram.enums.TestScopeSections;
import org.qubership.atp.ram.models.MetaInfo;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class StartRunRequest {

    private String projectName;
    private UUID projectId;
    private String testPlanName;
    private UUID testPlanId;
    private String testSuiteName;
    private String testCaseName;
    private UUID testCaseId;
    private String executionRequestName;
    private UUID atpExecutionRequestId;
    private String testRunName;
    private String testRunId;
    private boolean isFinalTestRun;
    private UUID initialTestRunId;
    private Timestamp startDate;
    private String taHost;
    private String qaHost;
    private String executor;
    private UUID executorId;
    private String solutionBuild;
    private String mailList;
    private UUID testScopeId;
    private UUID environmentId;
    private MetaInfo metaInfo;
    private String labelTemplateId;
    private String widgetConfigTemplateId;
    private String dataSetListId;
    private String dataSetId;
    private int threads;
    private boolean autoSyncCasesWithJira;
    private boolean autoSyncRunsWithJira;
    private Set<UUID> flagIds;
    private TestScopeSections testScopeSection;
    private int order;
    private Set<UUID> labelIds;
}
