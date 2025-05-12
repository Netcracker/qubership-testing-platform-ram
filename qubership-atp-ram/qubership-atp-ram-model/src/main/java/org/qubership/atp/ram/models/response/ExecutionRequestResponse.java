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

package org.qubership.atp.ram.models.response;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import org.qubership.atp.ram.dto.response.Environment;
import org.qubership.atp.ram.dto.response.ExecutionRequestMainInfoResponse.Executor;
import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.models.InitialExecutionRequest;
import org.qubership.atp.ram.models.Label;

import lombok.Data;

@Data
public class ExecutionRequestResponse {

    private UUID uuid;
    private String name;
    private InitialExecutionRequest initialExecutionRequest;
    private ExecutionStatuses executionStatus;
    private Timestamp startDate;
    private Timestamp finishDate;
    private long duration;
    private Executor executor;
    private Environment environment;
    private int passedRate;
    private int warningRate;
    private int failedRate;
    private List<Label> labels;
    private boolean analyzedByQa;
    private List<Label> filteredByLabels;
}
