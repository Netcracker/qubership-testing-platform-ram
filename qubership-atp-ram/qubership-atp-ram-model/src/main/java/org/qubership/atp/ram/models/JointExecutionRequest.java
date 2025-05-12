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

import static com.google.common.collect.Sets.newHashSet;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
@Document(collection = "jointExecutionRequests")
public class JointExecutionRequest extends RamObject {

    private String key;

    private Integer count;

    private Integer timeout;

    private List<Run> runs = new ArrayList<>();

    private Timestamp startDate;

    private Status status;

    private String logs;

    /**
     * Add or update run.
     *
     * @param executionRequest execution request
     */
    public void upsertRun(ExecutionRequest executionRequest) {
        upsertRun(executionRequest.getUuid(), executionRequest.getExecutionStatus());
    }

    /**
     * Add or update run.
     *
     * @param executionRequestId execution request identifier
     * @param status execution status
     */
    public void upsertRun(UUID executionRequestId, ExecutionStatuses status) {
        log.info("Upsert joint execution run for ER '{}' with execution status: {}", executionRequestId, status);
        Map<UUID, Run> runsMap = runs.stream()
                .collect(Collectors.toMap(Run::getExecutionRequestId, Function.identity()));

        runsMap.merge(executionRequestId, new Run(executionRequestId, status), (oldValue, newValue) -> newValue);

        runs = new ArrayList<>(runsMap.values());
        log.debug("Runs: {}", runs);
    }

    /**
     * Get completed exectuion request identifiers.
     *
     * @return list of identifiers
     */
    public List<UUID> getCompletedExecutionRequestIds() {
        Set<ExecutionStatuses> completedStatuses = newHashSet(
                ExecutionStatuses.FINISHED,
                ExecutionStatuses.TERMINATED,
                ExecutionStatuses.TERMINATED_BY_TIMEOUT
        );

        return runs.stream()
                .filter(run -> completedStatuses.contains(run.getStatus()))
                .map(Run::getExecutionRequestId)
                .collect(Collectors.toList());
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Run {
        private UUID executionRequestId;
        private ExecutionStatuses status;

        public Run(ExecutionRequest executionRequest) {
            this.executionRequestId = executionRequest.getUuid();
            this.status = executionRequest.getExecutionStatus();
        }
    }

    public enum Status {
        COMPLETED, COMPLETED_BY_TIMEOUT, IN_PROGRESS, FAILED
    }
}
