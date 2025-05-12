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

package org.qubership.atp.ram.model;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import org.qubership.atp.ram.dto.response.Environment;
import org.qubership.atp.ram.models.ExecutionRequest;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ExecutionRequestTestResult {
    private UUID executionRequestId;
    private String executionRequestName;
    private UUID projectId;
    private int passedRate;
    private Timestamp startDate;
    private int countOfTestRuns;
    private long duration;
    private UUID testScopeId;

    private Environment environment;
    private List<String> hosts;
    private List<String> solutionBuilds;

    /**
     * Create new ExecutionRequestTestResult by ExecutionRequest, EnvironmentsInfo,
     * lists of hosts and solutionBuilds.
     */
    public ExecutionRequestTestResult(ExecutionRequest executionRequest,
                                      Environment environment,
                                      List<String> hosts,
                                      List<String> solutionBuilds) {
        this.executionRequestId = executionRequest.getUuid();
        this.executionRequestName = executionRequest.getName();
        this.projectId = executionRequest.getProjectId();
        this.passedRate = executionRequest.getPassedRate();
        this.startDate = executionRequest.getStartDate();
        this.countOfTestRuns = executionRequest.getCountOfTestRuns();
        this.duration = executionRequest.getDuration();
        this.testScopeId = executionRequest.getTestScopeId();
        this.solutionBuilds = solutionBuilds;
        this.hosts = hosts;
        this.environment = environment;
    }
}
