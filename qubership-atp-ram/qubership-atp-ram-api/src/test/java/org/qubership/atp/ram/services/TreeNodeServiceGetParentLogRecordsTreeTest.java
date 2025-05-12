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
import org.qubership.atp.ram.entities.treenodes.TreeNode;
import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.TestRun;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class TreeNodeServiceGetParentLogRecordsTreeTest {

    @InjectMocks
    private TreeNodeService treeNodeService;

    @Mock
    private LogRecordService logRecordService;

    @Mock
    private TestRunService testRunService;

    @Mock
    private LabelTemplateNodeService labelTemplateNodeService;

    private LogRecord parentLr;
    private LogRecord logRecord1;
    private LogRecord logRecord2;
    private LogRecord logRecord3;
    private List<LogRecord> childLrs;

    @BeforeEach
    public void setUp() throws Exception {
        UUID testRunId = UUID.randomUUID();

        this.parentLr = LogRecordMock.generateLogRecord("parent LR", testRunId);
        UUID parentLrId = parentLr.getUuid();

        this.logRecord1 = LogRecordMock.generateLogRecordWithParentAndTestRunId("LR 1", testRunId, parentLrId);
        this.logRecord1.setExecutionStatus(ExecutionStatuses.IN_PROGRESS);

        this.logRecord2 = LogRecordMock.generateLogRecordWithParentAndTestRunId("LR 2", testRunId, parentLrId);
        this.logRecord2.setExecutionStatus(ExecutionStatuses.IN_PROGRESS);

        this.logRecord3 = LogRecordMock.generateLogRecordWithParentAndTestRunId("LR 3", testRunId, parentLrId);
        this.logRecord3.setExecutionStatus(ExecutionStatuses.FINISHED);


        this.childLrs = asList(logRecord1, logRecord2, logRecord3);

        when(logRecordService.findLogRecordForTreeByUuid(parentLrId)).thenReturn(parentLr);
        TestRun testRun = TestRunsMock.generateTestRun(testRunId, UUID.randomUUID());
        when(testRunService.findTestRunExecReqIdByUuid(testRunId)).thenReturn(testRun);
        when(logRecordService.findByTestRunIdAndParentUuid(testRunId, parentLrId, null))
                .thenReturn(childLrs);
    }

    @Test
    public void getLogRecordsTreeForLogRecordParent_ShouldReturnValidNodeWithChild() {
        TreeNode result = treeNodeService.getLogRecordsTreeForLogRecordParent(this.parentLr.getUuid(), null);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(LogRecordTreeNode.class, result.getClass());

        LogRecordTreeNode logRecordTreeNode = (LogRecordTreeNode) result;
        Assertions.assertEquals(parentLr.getUuid(), logRecordTreeNode.getLogRecordId());
        Assertions.assertEquals(parentLr.getName(), logRecordTreeNode.getName());

        Assertions.assertNotNull(logRecordTreeNode.getChildren());
        Assertions.assertEquals(3, logRecordTreeNode.getChildren().size());

        TreeNode resultChild1TreeNode = logRecordTreeNode.getChildren().get(0);
        Assertions.assertEquals(LogRecordTreeNode.class, resultChild1TreeNode.getClass());
        LogRecordTreeNode resultChild1LogRecordTreeNode = (LogRecordTreeNode) resultChild1TreeNode;

        Assertions.assertEquals(logRecord1.getUuid(), resultChild1LogRecordTreeNode.getLogRecordId());
        Assertions.assertEquals(logRecord1.getName(), resultChild1LogRecordTreeNode.getName());
        Assertions.assertEquals(logRecord1.getExecutionStatus(), resultChild1LogRecordTreeNode.getExecutionStatus());
        Assertions.assertNotNull(resultChild1LogRecordTreeNode.getChildren());

        TreeNode resultChild2TreeNode = logRecordTreeNode.getChildren().get(1);
        Assertions.assertEquals(LogRecordTreeNode.class, resultChild2TreeNode.getClass());
        LogRecordTreeNode resultChild2LogRecordTreeNode = (LogRecordTreeNode) resultChild2TreeNode;

        Assertions.assertEquals(logRecord2.getUuid(), resultChild2LogRecordTreeNode.getLogRecordId());
        Assertions.assertEquals(logRecord2.getName(), resultChild2LogRecordTreeNode.getName());
        Assertions.assertEquals(logRecord2.getExecutionStatus(), resultChild2LogRecordTreeNode.getExecutionStatus());
        Assertions.assertNotNull(resultChild2LogRecordTreeNode.getChildren());

        TreeNode resultChild3TreeNode = logRecordTreeNode.getChildren().get(2);
        Assertions.assertEquals(LogRecordTreeNode.class, resultChild3TreeNode.getClass());
        LogRecordTreeNode resultChild3LogRecordTreeNode = (LogRecordTreeNode) resultChild3TreeNode;

        Assertions.assertEquals(logRecord3.getUuid(), resultChild3LogRecordTreeNode.getLogRecordId());
        Assertions.assertEquals(logRecord3.getName(), resultChild3LogRecordTreeNode.getName());
        Assertions.assertEquals(logRecord3.getExecutionStatus(), resultChild3LogRecordTreeNode.getExecutionStatus());
        Assertions.assertNotNull(resultChild3LogRecordTreeNode.getChildren());


    }
}
