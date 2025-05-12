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
import org.qubership.atp.ram.models.TestRun;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class TestRunTreeNode extends TreeNode {
    private ExecutionStatuses executionStatus;
    private TestingStatuses testingStatus;
    private Integer passedRate;
    private UUID testRunId;
    private UUID testCaseId;
    private String testCaseName;
    private boolean isTestCaseRemoved;

    /**
     * TestRunTreeNode constructor.
     *
     * @param testRun testrun
     */
    public TestRunTreeNode(TestRun testRun) {
        this.id = UUID.randomUUID();
        this.testRunId = testRun.getUuid();
        this.executionRequestId = testRun.getExecutionRequestId();
        this.name = testRun.getName();
        this.passedRate = testRun.getPassedRate();
        this.nodeType = TreeNodeType.TEST_RUN_NODE;
        this.executionStatus = testRun.getExecutionStatus();
        this.testingStatus = testRun.getTestingStatus();
        this.testCaseId = testRun.getTestCaseId();
        this.testCaseName = testRun.getTestCaseName();
        this.isTestCaseRemoved = false;
    }

    /**
     * Instantiates a new Test run tree node.
     *
     * @param testRun         the test run
     * @param isTestCaseAlive the is alive
     */
    public TestRunTreeNode(TestRun testRun, boolean isTestCaseAlive) {
        this.id = UUID.randomUUID();
        this.testRunId = testRun.getUuid();
        this.executionRequestId = testRun.getExecutionRequestId();
        this.name = testRun.getName();
        this.passedRate = testRun.getPassedRate();
        this.nodeType = TreeNodeType.TEST_RUN_NODE;
        this.executionStatus = testRun.getExecutionStatus();
        this.testingStatus = testRun.getTestingStatus();
        this.testCaseId = testRun.getTestCaseId();
        this.testCaseName = testRun.getTestCaseName();
        this.isTestCaseRemoved = !isTestCaseAlive;
    }

    public TestRunTreeNode(String name) {
        super(name);
    }
}
