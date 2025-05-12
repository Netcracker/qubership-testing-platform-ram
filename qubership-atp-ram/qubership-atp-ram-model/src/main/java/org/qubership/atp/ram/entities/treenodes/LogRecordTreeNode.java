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

package org.qubership.atp.ram.entities.treenodes;

import java.util.UUID;

import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.enums.TypeAction;
import org.qubership.atp.ram.models.LogRecord;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class LogRecordTreeNode extends TreeNode {

    private TestingStatuses testingStatus;
    private ExecutionStatuses executionStatus;
    private TypeAction actionType;
    private UUID testRunId;
    private UUID logRecordId;
    private boolean isRootCauseAvailable;
    @JsonIgnore
    private boolean isLeaf = false;

    /**
     * LogRecordTreeNode constructor.
     *
     * @param logRecord          log record
     * @param executionRequestId execution request identifier
     */
    public LogRecordTreeNode(LogRecord logRecord, UUID executionRequestId) {
        this.id = UUID.randomUUID();
        this.testRunId = logRecord.getTestRunId();
        this.logRecordId = logRecord.getUuid();
        this.executionRequestId = executionRequestId;
        this.name = logRecord.getName();
        this.nodeType = TreeNodeType.LOG_RECORD_NODE;
        this.actionType = logRecord.getType();
        this.testingStatus = logRecord.getTestingStatus();
        this.isRootCauseAvailable = logRecord.getRootCause() != null;
        this.executionStatus = logRecord.getExecutionStatus();
    }
}
