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

package org.qubership.atp.ram.dto.response;

import java.sql.Timestamp;
import java.util.UUID;

import org.qubership.atp.ram.enums.ExecutionStatuses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExecutionRequestMainInfoResponse {
    private UUID id;
    private String name;
    private Executor executor;
    private ExecutionStatuses executionStatus;
    private Integer passRate;
    private Integer passCount;
    private Timestamp startDate;
    private Timestamp finishDate;
    private long duration;
    private Integer countOfTestRuns;
    private String troubleShooterUrl;
    private String monitoringToolUrl;
    private String missionControlToolUrl;
    private boolean recompilation = true;
    private Integer threads;
    private UUID testScopeId;
    private UUID environmentId;
    private UUID taToolsGroupId;
    private UUID initialExecutionRequestId;
    private String jointExecutionKey;
    private boolean isVirtual;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Executor {
        private UUID userId;
        private String username;
    }
}
