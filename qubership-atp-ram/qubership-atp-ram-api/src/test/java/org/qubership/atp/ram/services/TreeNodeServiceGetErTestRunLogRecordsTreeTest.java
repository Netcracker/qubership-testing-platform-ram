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

package org.qubership.atp.ram.services;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.qubership.atp.ram.LogRecordMock;
import org.qubership.atp.ram.TestRunsMock;
import org.qubership.atp.ram.entities.treenodes.LogRecordTreeNode;
import org.qubership.atp.ram.entities.treenodes.TestRunTreeNode;
import org.qubership.atp.ram.entities.treenodes.TreeNode;
import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.TestRun;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class TreeNodeServiceGetErTestRunLogRecordsTreeTest {

    @InjectMocks
    private TreeNodeService treeNodeService;

    @Mock
    private LogRecordService logRecordService;

    @Mock
    private TestRunService testRunService;

    @Mock
    private LabelTemplateNodeService labelTemplateNodeService;

    private TestRun testRun;

    private LogRecord logRecord1;
    private LogRecord logRecord2;
    private LogRecord logRecord3;
    private LogRecord logRecord4;
    private LogRecord logRecord5;

    private List<LogRecord> testRunTopLogRecords;

    @BeforeEach
    public void setUp() throws Exception {
        this.testRun = TestRunsMock.generateTestRun("TR 1");

        UUID testRunId = this.testRun.getUuid();

        this.logRecord1 = LogRecordMock.generateLogRecord("LR 1", testRunId);
        this.logRecord1.setExecutionStatus(ExecutionStatuses.IN_PROGRESS);
        this.logRecord2 = LogRecordMock.generateLogRecord("LR 2", testRunId);
        this.logRecord2.setParentRecordId(this.logRecord1.getUuid());
        this.logRecord2.setExecutionStatus(ExecutionStatuses.IN_PROGRESS);
        this.logRecord3 = LogRecordMock.generateLogRecord("LR 3", testRunId);
        this.logRecord3.setExecutionStatus(ExecutionStatuses.FINISHED);
        this.logRecord3.setParentRecordId(this.logRecord1.getUuid());
        this.logRecord4 = LogRecordMock.generateLogRecord("LR 4", testRunId);
        this.logRecord4.setParentRecordId(this.logRecord3.getUuid());
        this.logRecord5 = LogRecordMock.generateLogRecord("LR 5", testRunId);

        this.testRunTopLogRecords = asList(logRecord1, logRecord5);
    }

    /**
     * TR 1
     *    LR 1
     *       LR 2
     *       LR 3
     *          LR 4
     *    LR 5
     */
    @Test
    public void getExecutionRequestTestRunLogRecordsTree_shouldReturnRightTreeNode() {
        UUID testRunId = testRun.getUuid();

        when(testRunService.getTestRunForNodeTree(testRunId)).thenReturn(testRun);
        when(testRunService.getTopLevelLogRecords(testRunId, null)).thenReturn(this.testRunTopLogRecords);
//        when(logRecordService.getLogRecordChildren(logRecord1.getUuid())).thenReturn(asList(logRecord2, logRecord3));
//        when(logRecordService.getLogRecordChildren(logRecord3.getUuid())).thenReturn(singletonList(logRecord4));

        TreeNode result = treeNodeService.getExecutionRequestTestRunLogRecordsTree(testRunId, null);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(TestRunTreeNode.class, result.getClass());

        TestRunTreeNode testRunTreeNode = (TestRunTreeNode) result;
        Assertions.assertEquals(testRun.getUuid(), testRunTreeNode.getTestRunId());
        Assertions.assertEquals(testRun.getName(), testRunTreeNode.getName());
        Assertions.assertNotNull(testRunTreeNode.getChildren());
        Assertions.assertEquals(2, testRunTreeNode.getChildren().size());

        TreeNode resultChild1TreeNode = testRunTreeNode.getChildren().get(0);
        Assertions.assertEquals(LogRecordTreeNode.class, resultChild1TreeNode.getClass());
        LogRecordTreeNode resultChild1LogRecordTreeNode = (LogRecordTreeNode) resultChild1TreeNode;

        Assertions.assertEquals(logRecord1.getUuid(), resultChild1LogRecordTreeNode.getLogRecordId());
        Assertions.assertEquals(logRecord1.getName(), resultChild1LogRecordTreeNode.getName());
        Assertions.assertEquals(logRecord1.getExecutionStatus(), resultChild1LogRecordTreeNode.getExecutionStatus());
        Assertions.assertNotNull(resultChild1LogRecordTreeNode.getChildren());
    }
}
