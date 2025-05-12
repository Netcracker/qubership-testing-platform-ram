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
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.qubership.atp.ram.TestRunsMock;
import org.qubership.atp.ram.TreeConsoleDrawer;
import org.qubership.atp.ram.dto.response.ExecutionRequestWidgetConfigTemplateResponse;
import org.qubership.atp.ram.dto.response.TestCaseLabelResponse;
import org.qubership.atp.ram.entities.treenodes.ExecutionRequestTreeNode;
import org.qubership.atp.ram.entities.treenodes.LabelTemplateTreeNode;
import org.qubership.atp.ram.entities.treenodes.LogRecordTreeNode;
import org.qubership.atp.ram.entities.treenodes.ScopeGroupTreeNode;
import org.qubership.atp.ram.entities.treenodes.TestRunTreeNode;
import org.qubership.atp.ram.entities.treenodes.TreeNode;
import org.qubership.atp.ram.entities.treenodes.TreeNodeType;
import org.qubership.atp.ram.entities.treenodes.labelparams.CountReportLabelParam;
import org.qubership.atp.ram.entities.treenodes.labelparams.ReportLabelParam;
import org.qubership.atp.ram.enums.ExecutionRequestWidgets;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.enums.TypeAction;
import org.qubership.atp.ram.model.datacontext.TestRunsDataContext;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.LabelTemplate;
import org.qubership.atp.ram.models.LabelTemplate.LabelTemplateNode;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.models.ValidationLabelConfigTemplate;
import org.qubership.atp.ram.models.WidgetConfigTemplate;
import org.qubership.atp.ram.models.logrecords.BvLogRecord;
import org.qubership.atp.ram.models.logrecords.parts.ValidationTable;
import org.qubership.atp.ram.models.logrecords.parts.ValidationTableLine;
import org.qubership.atp.ram.models.tree.TreeWalker;
import org.qubership.atp.ram.utils.StreamUtils;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class TreeNodeServiceGetErTreeTest {

    private static final String
            BPP = "BPP",
            REVENUE = "Revenue",
            PROPAGATED = "Propagated",
            VALIDATED = "Validated";
    private static final String BPP_REVENUE = "BPP+Revenue";
    private static final String EMPTY_COLUMN_NAME = "";
    private static final String BPP_PROPAGATED = "BPP,Propagated";
    private static final String _VALID = "_Valid";

    @InjectMocks
    private TreeNodeService treeNodeService;

    @Mock
    private LogRecordService logRecordService;

    @Mock
    private ExecutionRequestService executionRequestService;

    @Mock
    private WidgetConfigTemplateService widgetConfigTemplateService;

    @Mock
    private ValidationLabelConfigTemplateService validationLabelConfigTemplateService;

    @Mock
    private TestRunService testRunService;

    @Mock
    private LabelTemplateNodeService labelTemplateNodeService;

    private LabelTemplate labelTemplate;

    private LabelTemplateNode offlineNode;
    private LabelTemplateNode dataNationalNode;
    private LabelTemplateNode modifyNode;
    private LabelTemplateNode disconnectNode;
    private LabelTemplateNode mmsNode;
    private LabelTemplateNode onlineNode;
    private LabelTemplateNode dateNode;
    private LabelTemplateNode voiceNode;
    private LabelTemplateNode reorderedNode;

    private TestRun testRun1;
    private TestRun testRun2;
    private TestRun testRun3;
    private TestRun testRun4;
    private TestRun testRun5;
    private TestRun testRun6;

    private List<LogRecord> testRun1LogRecords;
    private List<LogRecord> testRun4LogRecords;
    private List<LogRecord> testRun5LogRecords;

    private List<TestRun> testRuns;
    private ExecutionRequest executionRequest;
    private ValidationLabelConfigTemplate validationTemplate;
    private UUID executionRequestId;


    @BeforeEach
    public void setUp() throws Exception {
        this.labelTemplate = new LabelTemplate();

        this.testRun1LogRecords = new ArrayList<>();
        this.testRun4LogRecords = new ArrayList<>();
        this.testRun5LogRecords = new ArrayList<>();

        this.testRun1 = TestRunsMock.generateTestRun("TR 1");
        this.testRun2 = TestRunsMock.generateTestRun("TR 2");
        this.testRun3 = TestRunsMock.generateTestRun("TR 3");
        this.testRun4 = TestRunsMock.generateTestRun("TR 4");
        this.testRun5 = TestRunsMock.generateTestRun("TR 5");
        this.testRun6 = TestRunsMock.generateTestRun("TR 6");

        this.offlineNode = new LabelTemplateNode(UUID.randomUUID(), "Offline");
        this.dataNationalNode = new LabelTemplateNode(UUID.randomUUID(), "Data National");
        this.modifyNode = new LabelTemplateNode(UUID.randomUUID(), "Modify");
        this.disconnectNode = new LabelTemplateNode(UUID.randomUUID(), "Disconnect");
        this.mmsNode = new LabelTemplateNode(UUID.randomUUID(), "MMS");
        this.onlineNode = new LabelTemplateNode(UUID.randomUUID(), "Online");
        this.dateNode = new LabelTemplateNode(UUID.randomUUID(), "Date");
        this.voiceNode = new LabelTemplateNode(UUID.randomUUID(), "Voice");
        this.reorderedNode = new LabelTemplateNode(UUID.randomUUID(), "Reordered");

        this.labelTemplate.setLabelNodes(new ArrayList<>(asList(offlineNode, onlineNode)));
        this.offlineNode.setChildren(new ArrayList<>(asList(dataNationalNode, mmsNode)));
        this.dataNationalNode.setChildren(new ArrayList<>(asList(modifyNode, disconnectNode)));
        this.onlineNode.setChildren(new ArrayList<>(asList(dateNode, voiceNode)));
        this.dateNode.setChildren(new ArrayList<>(singletonList(reorderedNode)));

        this.modifyNode.setTestRunIds(singleton(testRun1.getUuid()));
        this.dataNationalNode.setTestRunIds(singleton(testRun4.getUuid()));
        this.mmsNode.setTestRunIds(singleton(testRun5.getUuid()));

        this.testRuns = asList(testRun1, testRun2, testRun3, testRun4, testRun5, testRun6);

        UUID testRun1Id = this.testRun1.getUuid();
        LogRecord logRecord1 =
                generateLogRecordWithParams("LR1", testRun1Id, null, asList(BPP, REVENUE, PROPAGATED, VALIDATED),
                        emptyList());
        LogRecord logRecord2 =
                generateLogRecordWithParams("LR2", testRun1Id, logRecord1.getUuid(), asList(BPP, PROPAGATED, VALIDATED),
                        asList(REVENUE));
        LogRecord logRecord3 = generateLogRecordWithParams("LR3", testRun1Id, logRecord1.getUuid(), asList(VALIDATED),
                asList(REVENUE, BPP, PROPAGATED));
        LogRecord logRecord4 = generateLogRecordWithParams("LR4", testRun1Id, null, asList(BPP),
                asList(REVENUE, VALIDATED, PROPAGATED));
        testRun1LogRecords.addAll(asList(logRecord1, logRecord2, logRecord3, logRecord4));

        UUID testRun4Id = this.testRun4.getUuid();
        LogRecord logRecord5 = generateLogRecordWithParams("LR5", testRun4Id, null, asList(BPP, REVENUE, VALIDATED),
                asList(PROPAGATED));
        LogRecord logRecord6 =
                generateLogRecordWithParams("LR6", testRun4Id, logRecord5.getUuid(), asList(REVENUE, VALIDATED),
                        asList(BPP, PROPAGATED));
        LogRecord logRecord7 = generateLogRecordWithParams("LR7", testRun4Id, null, asList(BPP, REVENUE, VALIDATED),
                asList(PROPAGATED));
        testRun4LogRecords.addAll(asList(logRecord5, logRecord6, logRecord7));

        UUID testRun5Id = this.testRun5.getUuid();
        LogRecord logRecord8 = generateLogRecordWithParams("LR8", testRun5Id, null, asList(BPP, VALIDATED),
                asList(REVENUE, PROPAGATED));
        testRun5LogRecords.addAll(asList(logRecord8));

        TestRunsDataContext context = TestRunsDataContext.builder()
                .testRunsMap(StreamUtils.toIdEntityMap(testRuns))
                .testRunValidationLogRecordsMap(new HashMap<UUID, List<LogRecord>>() {{
                    put(testRun1.getUuid(), testRun1LogRecords);
                    put(testRun4.getUuid(), testRun4LogRecords);
                    put(testRun5.getUuid(), testRun5LogRecords);
                }})
                .testRunTestCasesMap(new HashMap<UUID, TestCaseLabelResponse>() {{
                    put(testRun1.getTestCaseId(), mock(TestCaseLabelResponse.class));
                }})
                .build();

        executionRequest = new ExecutionRequest();
        executionRequestId = UUID.randomUUID();
        executionRequest.setUuid(executionRequestId);
        executionRequest.setLabelTemplateId(labelTemplate.getUuid());

        validationTemplate = new ValidationLabelConfigTemplate();
        validationTemplate.setLabels(new TreeSet<>(asList(
                new ValidationLabelConfigTemplate.LabelConfig(new HashSet<>(asList(BPP, REVENUE)), BPP_REVENUE,
                        true, false, 1),
                new ValidationLabelConfigTemplate.LabelConfig(new HashSet<>(asList(BPP, PROPAGATED)), EMPTY_COLUMN_NAME,
                        true, false, 3),
                new ValidationLabelConfigTemplate.LabelConfig(new HashSet<>(asList(VALIDATED)), _VALID, true, false, 2)
        )));

        when(testRunService.getTestRunsDataContext(any(), any(), anyBoolean())).thenReturn(context);
        when(testRunService.findAllByExecutionRequestId(any())).thenReturn(testRuns);
        when(testRunService.getTopLevelLogRecords(testRun1Id, null)).thenReturn(asList(logRecord1, logRecord4));
        when(testRunService.getTopLevelLogRecords(testRun4Id, null)).thenReturn(asList(logRecord5, logRecord7));
        when(testRunService.getTopLevelLogRecords(testRun5Id, null)).thenReturn(asList(logRecord8));
        when(testRunService.getAllLogRecordsByTestRunId(testRun1Id)).thenReturn(testRun1LogRecords);
        when(testRunService.getAllLogRecordsByTestRunId(testRun4Id)).thenReturn(testRun4LogRecords);
        when(testRunService.getAllLogRecordsByTestRunId(testRun5Id)).thenReturn(testRun5LogRecords);
        when(labelTemplateNodeService.getLabelTemplate(any(UUID.class))).thenReturn(labelTemplate);
        when(labelTemplateNodeService.populateLabelTemplateWithTestRuns(any(), any(LabelTemplate.class)))
                .thenReturn(labelTemplate);
    }

    @Test
    public void getExecutionRequestWidgetTree_LabelTemplateSpecifiedAndRefreshTrue_LabelTemplateNotChanged() {
        WidgetConfigTemplate widgetConfigTemplate = Mockito.mock(WidgetConfigTemplate.class);
        when(executionRequestService.findById(any())).thenReturn(executionRequest);

        ExecutionRequestWidgetConfigTemplateResponse erWidgetConfigTemplateResponse = new ExecutionRequestWidgetConfigTemplateResponse();
        erWidgetConfigTemplateResponse.setTemplate(widgetConfigTemplate);
        when(widgetConfigTemplateService.getWidgetConfigTemplateForEr(executionRequest)).thenReturn(erWidgetConfigTemplateResponse);

        UUID labelTemplateId = UUID.randomUUID();
        labelTemplate.setUuid(labelTemplateId);
        UUID widgetId = ExecutionRequestWidgets.SUMMARY_STATISTIC.getWidgetId();
        UUID validationTemplateId = UUID.randomUUID();
        when(widgetConfigTemplate.getWidgetConfig(widgetId)).thenReturn(new WidgetConfigTemplate.WidgetConfig());
        when(validationLabelConfigTemplateService.get(validationTemplateId)).thenReturn(validationTemplate);

        TreeNode treeNode = treeNodeService.getExecutionRequestWidgetTree(executionRequestId, widgetId,
                labelTemplateId, validationTemplateId, false, true);
        Assertions.assertEquals(((ExecutionRequestTreeNode) treeNode).getLabelTemplateId(), labelTemplateId);
    }

    @Test
    public void getExecutionRequestWidgetTree_LabelTemplateNotSpecifiedAndRefreshFalse_LabelTemplateFromER() {
        WidgetConfigTemplate widgetConfigTemplate = Mockito.mock(WidgetConfigTemplate.class);
        when(executionRequestService.findById(any())).thenReturn(executionRequest);

        ExecutionRequestWidgetConfigTemplateResponse erWidgetConfigTemplateResponse = new ExecutionRequestWidgetConfigTemplateResponse();
        erWidgetConfigTemplateResponse.setTemplate(widgetConfigTemplate);
        when(widgetConfigTemplateService.getWidgetConfigTemplateForEr(executionRequest)).thenReturn(erWidgetConfigTemplateResponse);

        UUID labelTemplateId = UUID.randomUUID();
        labelTemplate.setUuid(labelTemplateId);
        UUID widgetId = ExecutionRequestWidgets.SUMMARY_STATISTIC.getWidgetId();
        UUID validationTemplateId = UUID.randomUUID();
        when(widgetConfigTemplate.getWidgetConfig(widgetId)).thenReturn(new WidgetConfigTemplate.WidgetConfig());
        when(validationLabelConfigTemplateService.get(validationTemplateId)).thenReturn(validationTemplate);

        TreeNode treeNode = treeNodeService.getExecutionRequestWidgetTree(executionRequestId, widgetId,
                null, validationTemplateId, false, false);
        Assertions.assertEquals(((ExecutionRequestTreeNode) treeNode).getLabelTemplateId(),
                executionRequest.getLabelTemplateId());
    }

    @Test
    public void getExecutionRequestWidgetTree_WidgetConfigIsEmptyButValidationTemplateIdIsNotNull_ValidationLabelTemplateWasTaken() {
        when(executionRequestService.findById(any())).thenReturn(executionRequest);

        ExecutionRequestWidgetConfigTemplateResponse erWidgetConfigTemplateResponse =
                new ExecutionRequestWidgetConfigTemplateResponse();
        when(widgetConfigTemplateService.getWidgetConfigTemplateForEr(executionRequest))
                .thenReturn(erWidgetConfigTemplateResponse);

        UUID widgetId = ExecutionRequestWidgets.SUMMARY_STATISTIC.getWidgetId();
        UUID validationTemplateId = UUID.randomUUID();

        treeNodeService.getExecutionRequestWidgetTree(executionRequestId, widgetId,
                null, validationTemplateId, false, false);
        Mockito.verify(validationLabelConfigTemplateService, atLeastOnce()).get(any());
    }

    /*
     * Validation template:
     *
     * |---------------|-----------------|
     * |    Column     |     Labels      |
     * |---------------|-----------------|
     * |  BPP+Revenue  | BPP, Revenue    |
     * |---------------|-----------------|
     * |               | BPP, Propagated |
     * |---------------|-----------------|
     * |    _Valid     | Validated       |
     * |---------------|-----------------|
     *
     * Expected Label template report label stats:
     *                           |   BPP   | REVENUE | PROPAGATED | VALIDATED | BPP+Revenue | BPP.Propagated |  _Valid   |
     *                           |---------|---------|------------|-----------|-------------|----------------|-----------|
     *   Offline                 | 33(1/3) | 33(1/3) |   0(0/3)   |  66(2/3)  |    50(1/2)  |     0(0/2)     |  66(2/3)  |
     *       Data National       | 0(0/2)  | 50(1/2) |   0(0/2)   |  50(1/2)  |    50(1/2)  |     0(0/2)     |  50(1/2)  |
     *           Modify          | 0(0/1)  | 0(0/1)  |   0(0/1)   |  0(0/1)   |    0(0/1)   |     0(0/1)     |  0(0/1)   |
     *              TR 1         | FAILED  | FAILED  |   FAILED   |  FAILED   |    FAILED   |     FAILED     |  FAILED   |
     *                 LR 1      |    +    |    +    |     +      |     +     |      +      |        +       |     +     |
     *                    LR 2   |    +    |    -    |     +      |     +     |     N\G     |        +       |     +     |
     *                    LR 3   |    -    |    -    |     -      |     +     |      -      |        -       |     +     |
     *                 LR 4      |    +    |    -    |     -      |     -     |     N\G     |       N\G      |     -     |
     *           Disconnect      | 0(0/0)  | 0(0/0)  |   0(0/0)   |  0(0/0)   |    0(0/0)   |     0(0/0)     |  0(0/0)   |
     *           TR 4            | FAILED  | PASSED  |   FAILED   |  PASSED   |    PASSED   |     FAILED     |  PASSED   |
     *              LR 5         |    +    |    +    |     -      |     +     |      +      |       N\G      |     +     |
     *                 LR 6      |    -    |    +    |     -      |     +     |     N\G     |        -       |     +     |
     *              LR 7         |    +    |    +    |     -      |     +     |      +      |       N\G      |     +     |
     *       MMS                 | 100(1/1)| 0(0/1)  |   0(0/1)   |  100(1/1) |     N\G     |       N\G      |  100(1/1) |
     *           TR 5            | PASSED  | FAILED  |   FAILED   |  PASSED   |     N\G     |       N\G      |  PASSED   |
     *              LR 8         |    +    |    -    |     -      |     +     |     N\G     |       N\G      |     +     |
     *   Online                  |         |         |            |           |             |                |           |
     *       Date                |         |         |            |           |             |                |           |
     *           Reordered       |         |         |            |           |             |                |           |
     *       Voice               |         |         |            |           |             |                |           |
     */
    @Test
    public void getExecutionRequestTree_shouldCreateRightTemplate() {
        TreeNode treeNode =
                treeNodeService.getExecutionRequestTree(executionRequest, UUID.randomUUID(), validationTemplate, true, false);

        Assertions.assertNotNull(treeNode);
        Assertions.assertEquals(ExecutionRequestTreeNode.class, treeNode.getClass());

        ExecutionRequestTreeNode executionRequestTreeNode = (ExecutionRequestTreeNode) treeNode;
        List<TreeNode> children = executionRequestTreeNode.getChildren();

        Assertions.assertNotNull(children);
        // Expected 1 node because of Online node should be removed as empty
        Assertions.assertEquals(1, children.size());

        // Offline node stats check
        TreeNode offlineTreeNode = children.get(0);
        Assertions.assertNotNull(offlineTreeNode);
        Assertions.assertEquals(LabelTemplateTreeNode.class, offlineTreeNode.getClass());

        LabelTemplateTreeNode offline = (LabelTemplateTreeNode) offlineTreeNode;

        Map<String, CountReportLabelParam> offlineParams = toParamMap(offline.getReportLabelParams());

        Assertions.assertNotNull(offlineParams);

        Assertions.assertEquals(33, offlineParams.get(BPP).getPassed());
        Assertions.assertEquals(33, offlineParams.get(REVENUE).getPassed());
        Assertions.assertEquals(0, offlineParams.get(PROPAGATED).getPassed());
        Assertions.assertEquals(67, offlineParams.get(VALIDATED).getPassed());
        Assertions.assertEquals(50, offlineParams.get(BPP_REVENUE).getPassed());
        Assertions.assertEquals(0, offlineParams.get(BPP_PROPAGATED).getPassed());
        Assertions.assertEquals(67, offlineParams.get(_VALID).getPassed());

        List<TreeNode> offlineChildren = offline.getChildren();

        Assertions.assertNotNull(offlineChildren);
        Assertions.assertEquals(2, offlineChildren.size());

        // Data National node stats check
        TreeNode dataNationalTreeNode = offlineChildren.get(0);
        Assertions.assertNotNull(dataNationalTreeNode);
        Assertions.assertEquals(LabelTemplateTreeNode.class, dataNationalTreeNode.getClass());

        LabelTemplateTreeNode dataNational = (LabelTemplateTreeNode) dataNationalTreeNode;

        Map<String, CountReportLabelParam> dataNationalParams = toParamMap(dataNational.getReportLabelParams());

        Assertions.assertNotNull(dataNationalParams);
        Assertions.assertEquals(0, dataNationalParams.get(BPP).getPassed());
        Assertions.assertEquals(50, dataNationalParams.get(REVENUE).getPassed());
        Assertions.assertEquals(0, dataNationalParams.get(PROPAGATED).getPassed());
        Assertions.assertEquals(50, dataNationalParams.get(VALIDATED).getPassed());
        Assertions.assertEquals(50, dataNationalParams.get(BPP_REVENUE).getPassed());
        Assertions.assertEquals(0, dataNationalParams.get(BPP_PROPAGATED).getPassed());
        Assertions.assertEquals(50, dataNationalParams.get(_VALID).getPassed());

        List<TreeNode> dataNationalChildren = dataNational.getChildren();

        Assertions.assertNotNull(dataNationalChildren);
        // Expected 2 nodes because of Disconnect node should be removed as empty
        Assertions.assertEquals(2, dataNationalChildren.size());

        // Modify node stats check
        TreeNode modifyTreeNode = findChildrenTreeNodeByName(dataNationalChildren, modifyNode.getLabelName());
        Assertions.assertNotNull(modifyTreeNode);
        Assertions.assertEquals(LabelTemplateTreeNode.class, modifyTreeNode.getClass());

        LabelTemplateTreeNode modify = (LabelTemplateTreeNode) modifyTreeNode;

        Map<String, CountReportLabelParam> modifyParams = toParamMap(modify.getReportLabelParams());

        Assertions.assertNotNull(modifyParams);
        Assertions.assertEquals(0, modifyParams.get(BPP).getPassed());
        Assertions.assertEquals(0, modifyParams.get(REVENUE).getPassed());
        Assertions.assertEquals(0, modifyParams.get(PROPAGATED).getPassed());
        Assertions.assertEquals(0, modifyParams.get(VALIDATED).getPassed());
        Assertions.assertEquals(0, modifyParams.get(BPP_REVENUE).getPassed());
        Assertions.assertEquals(0, modifyParams.get(BPP_PROPAGATED).getPassed());
        Assertions.assertEquals(0, modifyParams.get(_VALID).getPassed());

        TreeNode testRunTR4TreeNode = findChildrenTreeNodeByName(dataNationalChildren, testRun4.getName());
        Assertions.assertNotNull(testRunTR4TreeNode);
        Assertions.assertEquals(TestRunTreeNode.class, testRunTR4TreeNode.getClass());
        Assertions.assertTrue(((TestRunTreeNode) testRunTR4TreeNode).isTestCaseRemoved());

        List<TreeNode> modifyChildren = modify.getChildren();

        // MMS node stats check
        TreeNode mmsTreeNode = offlineChildren.get(1);
        Assertions.assertNotNull(mmsTreeNode);
        Assertions.assertEquals(LabelTemplateTreeNode.class, mmsTreeNode.getClass());

        LabelTemplateTreeNode mms = (LabelTemplateTreeNode) mmsTreeNode;

        Map<String, CountReportLabelParam> mmsParams = toParamMap(mms.getReportLabelParams());

        Assertions.assertNotNull(mmsParams);

        Assertions.assertEquals(100, mmsParams.get(BPP).getPassed());
        Assertions.assertEquals(0, mmsParams.get(REVENUE).getPassed());
        Assertions.assertEquals(0, mmsParams.get(PROPAGATED).getPassed());
        Assertions.assertEquals(100, mmsParams.get(VALIDATED).getPassed());
        Assertions.assertEquals(100, mmsParams.get(_VALID).getPassed());

        TreeNode testRunTR1TreeNode = findChildrenTreeNodeByName(modifyChildren, testRun1.getName());
        Assertions.assertNotNull(testRunTR1TreeNode);
        Assertions.assertEquals(TestRunTreeNode.class, testRunTR1TreeNode.getClass());
        Assertions.assertFalse(((TestRunTreeNode) testRunTR1TreeNode).isTestCaseRemoved());
    }

    private LogRecord generateLogRecordWithParams(String name,
                                                  UUID testRunId,
                                                  UUID parentRecordId,
                                                  List<String> passedParams,
                                                  List<String> failedParams) {
        LogRecord logRecord = new BvLogRecord();
        logRecord.setName(name);
        logRecord.setUuid(UUID.randomUUID());
        logRecord.setType(TypeAction.TECHNICAL);
        logRecord.setTestRunId(testRunId);
        logRecord.setParentRecordId(parentRecordId);
        logRecord.setTestingStatusHard(TestingStatuses.PASSED);

        logRecord.setValidationLabels(new HashSet<>(passedParams));

        ValidationTable validationTable = new ValidationTable();
        logRecord.setValidationTable(validationTable);

        List<ValidationTableLine> steps = new ArrayList<>();
        validationTable.setSteps(steps);

        setValidationTableSteps(steps, failedParams, TestingStatuses.FAILED);

        return logRecord;
    }

    private void setValidationTableSteps(List<ValidationTableLine> steps,
                                         List<String> params,
                                         TestingStatuses status) {
        ValidationTableLine step = new ValidationTableLine();
        step.setName(String.join("_", params));
        step.setImportant(true);
        step.setStatus(status);
        step.setValidationLabels(new HashSet<>(params));
        steps.add(step);
    }

    private Map<String, CountReportLabelParam> toParamMap(List<CountReportLabelParam> reportLabelParams) {
        Assertions.assertNotNull(reportLabelParams);
        return reportLabelParams.stream().collect(Collectors.toMap(ReportLabelParam::getName, Function.identity()));
    }

    private TreeNode findChildrenTreeNodeByName(List<TreeNode> children, String name) {
        return children.stream()
                .filter(node -> node.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Failed to find children tree node by name"));
    }

    @Test
    public void onTreeNodeService_whenGetLogRecordNodeByName_ResultListPopulatedWithCorrectNodes() {

        LogRecord parentLogRecord =
                generateLogRecordWithParams("Parent LR", testRun1.getUuid(), null, asList(REVENUE, VALIDATED),
                        asList(BPP, PROPAGATED));
        LogRecord childLogRecord =
                generateLogRecordWithParams("Child LR", testRun1.getUuid(), parentLogRecord.getUuid(),
                        asList(BPP, REVENUE, VALIDATED), asList(PROPAGATED));

        List<UUID> expectedNodeUuids = asList(testRun1.getUuid(), parentLogRecord.getUuid(), childLogRecord.getUuid());

        when(testRunService.findAllByExecutionRequestId(any())).thenReturn(Collections.singletonList(testRun1));
        when(logRecordService.getAllMatchesLogRecordsByTestRunIdCaseInsensitive(any(), any()))
                .thenReturn(Stream.of(parentLogRecord, childLogRecord));

        Set<TreeNode> result = treeNodeService.getExecutionRequestTreeNodesByName(UUID.randomUUID(), "TR");

        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.size());

        result.forEach(
                treeNode -> Assertions.assertTrue(
                        expectedNodeUuids.contains(treeNode.getNodeType() == TreeNodeType.TEST_RUN_NODE
                                ? ((TestRunTreeNode) treeNode).getTestRunId()
                                : ((LogRecordTreeNode) treeNode).getLogRecordId()),
                        String.format("Expected tree node %s wasn't included into result list.", treeNode)));
    }

    /*
     * ------------------------------------------------
     * | Tree                        | Node is empty? |
     * ------------------------------------------------
     * ER
     * |___Prerequisites
     * |
     * |___Execution
     * |   |
     * |   |___News
     * |   |   |___Offline
     * |   |   |___Online
     * |   |       |___Postpaid
     * |   |
     * |   |___Modify
     * |   |   |___Offline
     * |   |   |___Online
     * |   |       |___Postpaid
     * |   |       |___Prepaid               +
     * |   |
     * |   |___Features
     * |   |   |___Offline
     * |   |   |   |___Partial
     * |   |   |   |___Rainy
     * |   |   |   |___Rerate                +
     * |   |   |
     * |   |   |___Online
     * |   |       |___AutoReplan            +
     * |   |       |___EAP
     * |   |       |___SY                    +
     * |   |
     * |   |___Unknown                       +
     * |
     * |___Validation                        +
     */
    @Test
    public void testRemoveEmptyNodes_shouldBeSuccessfullyRemoved() {
        List<TreeNode> fakeTestRunNodes = asList(
                new TestRunTreeNode("TR 1"),
                new TestRunTreeNode("TR 2")
        );

        TreeNode erNode = new ExecutionRequestTreeNode();
        erNode.setName("ER");
        TreeNode prerequisites = new ScopeGroupTreeNode("Prerequisites");
        TreeNode execution = new ScopeGroupTreeNode("Execution");
        TreeNode news = new LabelTemplateTreeNode("News");
        TreeNode newsOffline = new LabelTemplateTreeNode("Offline");
        TreeNode newsOnline = new LabelTemplateTreeNode("Online");
        TreeNode newsOnlinePostpaid = new LabelTemplateTreeNode("Postpaid");
        TreeNode modify = new LabelTemplateTreeNode("Modify");
        TreeNode modifyOffline = new LabelTemplateTreeNode("Offline");
        TreeNode modifyOnline = new LabelTemplateTreeNode("Online");
        TreeNode modifyOnlinePostpaid = new LabelTemplateTreeNode("Postpaid");
        TreeNode modifyOnlinePrepaid = new LabelTemplateTreeNode("Prepaid_");
        TreeNode features = new LabelTemplateTreeNode("Features");
        TreeNode featuresOffline = new LabelTemplateTreeNode("Offline");
        TreeNode featuresOfflinePartial = new LabelTemplateTreeNode("Partial");
        TreeNode featuresOfflineRainy = new LabelTemplateTreeNode("Rainy");
        TreeNode featuresOfflineRerate = new LabelTemplateTreeNode("Rerate");
        TreeNode featuresOnline = new LabelTemplateTreeNode("Online");
        TreeNode featuresOnlineAutoReplan = new LabelTemplateTreeNode("AutoReplan");
        TreeNode featuresOnlineEap = new LabelTemplateTreeNode("EAP");
        TreeNode featuresOnlineSy = new LabelTemplateTreeNode("SY");
        TreeNode unknown = new LabelTemplateTreeNode("Unknown");
        TreeNode validation = new ScopeGroupTreeNode("Validation");

        erNode.setChildren(asList(prerequisites, execution, validation));
        execution.setChildren(asList(news, modify, features, unknown));
        news.setChildren(asList(newsOffline, newsOnline));
        newsOnline.setChildren(singletonList(newsOnlinePostpaid));
        modify.setChildren(asList(modifyOffline, modifyOnline));
        modifyOnline.setChildren(asList(modifyOnlinePostpaid, modifyOnlinePrepaid));
        features.setChildren(asList(featuresOffline, featuresOnline));
        featuresOffline.setChildren(asList(featuresOfflinePartial, featuresOfflineRainy, featuresOfflineRerate));
        featuresOnline.setChildren(asList(featuresOnlineAutoReplan, featuresOnlineEap, featuresOnlineSy));

        newsOffline.setChildren(fakeTestRunNodes);
        newsOnlinePostpaid.setChildren(fakeTestRunNodes);
        modifyOffline.setChildren(fakeTestRunNodes);
        modifyOnlinePostpaid.setChildren(fakeTestRunNodes);
        featuresOfflinePartial.setChildren(fakeTestRunNodes);
        featuresOfflineRainy.setChildren(fakeTestRunNodes);
        featuresOnlineEap.setChildren(fakeTestRunNodes);

        treeNodeService.removeEmptyNodes(erNode);

        //        Simple drawer to view tree in the console
        new TreeConsoleDrawer<>(erNode, TreeNode::getChildren, TreeNode::getName).draw();

        List<String> expectedExcludedNodes = asList("Prepaid_", "Rerate", "AutoReplan", "SY", "Unknown");

        TreeWalker<TreeNode> treeWalker = new TreeWalker<>();
        treeWalker.walkWithPreProcess(erNode, TreeNode::getChildren, new BiConsumer<TreeNode, TreeNode>() {
            @Override
            public void accept(TreeNode root, TreeNode child) {
                String nodeName = child.getName();
                Assertions.assertFalse(expectedExcludedNodes.contains(nodeName), nodeName + " node shouldn't exist in result tree");
            }
        });
    }

    /*
     * Validation template:
     *
     * |---------------|-----------------|
     * |    Column     |     Labels      |
     * |---------------|-----------------|
     * |  BPP+Revenue  | BPP, Revenue    |
     * |---------------|-----------------|
     * |               | BPP, Propagated |
     * |---------------|-----------------|
     * |    _Valid     | Validated       |
     * |---------------|-----------------|
     *
     * Expected Label template report label stats:
     *                           |   BPP   | REVENUE | PROPAGATED | VALIDATED | BPP+Revenue | BPP.Propagated |  _Valid   |
     *                           |---------|---------|------------|-----------|-------------|----------------|-----------|
     *   Offline                 | 33(1/3) | 33(1/3) |   0(0/3)   |  66(2/3)  |    50(1/2)  |     0(0/2)     |  66(2/3)  |
     *       Data National       | 0(0/2)  | 50(1/2) |   0(0/2)   |  50(1/2)  |    50(1/2)  |     0(0/2)     |  50(1/2)  |
     *           Modify          | 0(0/1)  | 0(0/1)  |   0(0/1)   |  0(0/1)   |    0(0/1)   |     0(0/1)     |  0(0/1)   |
     *              TR 1         | FAILED  | FAILED  |   FAILED   |  FAILED   |    FAILED   |     FAILED     |  FAILED   |
     *                 LR 1      |    +    |    +    |     +      |     +     |      +      |        +       |     +     |
     *                    LR 2   |    +    |    -    |     +      |     +     |     N\G     |        +       |     +     |
     *                    LR 3   |    -    |    -    |     -      |     +     |      -      |        -       |     +     |
     *                 LR 4      |    +    |    -    |     -      |     -     |     N\G     |       N\G      |     -     |
     *           Disconnect      | 0(0/0)  | 0(0/0)  |   0(0/0)   |  0(0/0)   |    0(0/0)   |     0(0/0)     |  0(0/0)   |
     *           TR 4            | FAILED  | PASSED  |   FAILED   |  PASSED   |    PASSED   |     FAILED     |  PASSED   |
     *              LR 5         |    +    |    +    |     -      |     +     |      +      |       N\G      |     +     |
     *                 LR 6      |    -    |    +    |     -      |     +     |     N\G     |        -       |     +     |
     *              LR 7         |    +    |    +    |     -      |     +     |      +      |       N\G      |     +     |
     *       MMS                 | 100(1/1)| 0(0/1)  |   0(0/1)   |  100(1/1) |     N\G     |       N\G      |  100(1/1) |
     *           TR 5            | PASSED  | FAILED  |   FAILED   |  PASSED   |     N\G     |       N\G      |  PASSED   |
     *              LR 8         |    +    |    -    |     -      |     +     |     N\G     |       N\G      |     +     |
     *   Online                  |         |         |            |           |             |                |           |
     *       Date                |         |         |            |           |             |                |           |
     *           Reordered       |         |         |            |           |             |                |           |
     *       Voice               |         |         |            |           |             |                |           |
     */
    @Test
    public void getExecutionRequestTree_withIncludeAllLogRecord_shouldCreateRightTemplate() {
        TreeNode treeNode =
                treeNodeService.getExecutionRequestTree(executionRequest, UUID.randomUUID(), validationTemplate, true, true);

        Assertions.assertNotNull(treeNode);
        Assertions.assertEquals(ExecutionRequestTreeNode.class, treeNode.getClass());

        ExecutionRequestTreeNode executionRequestTreeNode = (ExecutionRequestTreeNode) treeNode;
        List<TreeNode> children = executionRequestTreeNode.getChildren();

        Assertions.assertNotNull(children);
        // Expected 1 node because of Online node should be removed as empty
        Assertions.assertEquals(1, children.size());

        // Offline node stats check
        TreeNode offlineTreeNode = children.get(0);
        Assertions.assertNotNull(offlineTreeNode);
        Assertions.assertEquals(LabelTemplateTreeNode.class, offlineTreeNode.getClass());

        LabelTemplateTreeNode offline = (LabelTemplateTreeNode) offlineTreeNode;

        Map<String, CountReportLabelParam> offlineParams = toParamMap(offline.getReportLabelParams());

        Assertions.assertNotNull(offlineParams);

        Assertions.assertEquals(33, offlineParams.get(BPP).getPassed());
        Assertions.assertEquals(33, offlineParams.get(REVENUE).getPassed());
        Assertions.assertEquals(0, offlineParams.get(PROPAGATED).getPassed());
        Assertions.assertEquals(67, offlineParams.get(VALIDATED).getPassed());
        Assertions.assertEquals(50, offlineParams.get(BPP_REVENUE).getPassed());
        Assertions.assertEquals(0, offlineParams.get(BPP_PROPAGATED).getPassed());
        Assertions.assertEquals(67, offlineParams.get(_VALID).getPassed());

        List<TreeNode> offlineChildren = offline.getChildren();

        Assertions.assertNotNull(offlineChildren);
        Assertions.assertEquals(2, offlineChildren.size());

        // Data National node stats check
        TreeNode dataNationalTreeNode = offlineChildren.get(0);
        Assertions.assertNotNull(dataNationalTreeNode);
        Assertions.assertEquals(LabelTemplateTreeNode.class, dataNationalTreeNode.getClass());

        LabelTemplateTreeNode dataNational = (LabelTemplateTreeNode) dataNationalTreeNode;

        Map<String, CountReportLabelParam> dataNationalParams = toParamMap(dataNational.getReportLabelParams());

        Assertions.assertNotNull(dataNationalParams);
        Assertions.assertEquals(0, dataNationalParams.get(BPP).getPassed());
        Assertions.assertEquals(50, dataNationalParams.get(REVENUE).getPassed());
        Assertions.assertEquals(0, dataNationalParams.get(PROPAGATED).getPassed());
        Assertions.assertEquals(50, dataNationalParams.get(VALIDATED).getPassed());
        Assertions.assertEquals(50, dataNationalParams.get(BPP_REVENUE).getPassed());
        Assertions.assertEquals(0, dataNationalParams.get(BPP_PROPAGATED).getPassed());
        Assertions.assertEquals(50, dataNationalParams.get(_VALID).getPassed());

        List<TreeNode> dataNationalChildren = dataNational.getChildren();

        Assertions.assertNotNull(dataNationalChildren);
        // Expected 2 nodes because of Disconnect node should be removed as empty
        Assertions.assertEquals(2, dataNationalChildren.size());

        // Modify node stats check
        TreeNode modifyTreeNode = findChildrenTreeNodeByName(dataNationalChildren, modifyNode.getLabelName());
        Assertions.assertNotNull(modifyTreeNode);
        Assertions.assertEquals(LabelTemplateTreeNode.class, modifyTreeNode.getClass());

        LabelTemplateTreeNode modify = (LabelTemplateTreeNode) modifyTreeNode;

        Map<String, CountReportLabelParam> modifyParams = toParamMap(modify.getReportLabelParams());

        Assertions.assertNotNull(modifyParams);
        Assertions.assertEquals(0, modifyParams.get(BPP).getPassed());
        Assertions.assertEquals(0, modifyParams.get(REVENUE).getPassed());
        Assertions.assertEquals(0, modifyParams.get(PROPAGATED).getPassed());
        Assertions.assertEquals(0, modifyParams.get(VALIDATED).getPassed());
        Assertions.assertEquals(0, modifyParams.get(BPP_REVENUE).getPassed());
        Assertions.assertEquals(0, modifyParams.get(BPP_PROPAGATED).getPassed());
        Assertions.assertEquals(0, modifyParams.get(_VALID).getPassed());

        TreeNode testRunTR4TreeNode = findChildrenTreeNodeByName(dataNationalChildren, testRun4.getName());
        Assertions.assertNotNull(testRunTR4TreeNode);
        Assertions.assertEquals(TestRunTreeNode.class, testRunTR4TreeNode.getClass());
        Assertions.assertTrue(((TestRunTreeNode) testRunTR4TreeNode).isTestCaseRemoved());

        // MMS node stats check
        TreeNode mmsTreeNode = offlineChildren.get(1);
        Assertions.assertNotNull(mmsTreeNode);
        Assertions.assertEquals(LabelTemplateTreeNode.class, mmsTreeNode.getClass());

        LabelTemplateTreeNode mms = (LabelTemplateTreeNode) mmsTreeNode;

        Map<String, CountReportLabelParam> mmsParams = toParamMap(mms.getReportLabelParams());

        Assertions.assertNotNull(mmsParams);

        Assertions.assertEquals(100, mmsParams.get(BPP).getPassed());
        Assertions.assertEquals(0, mmsParams.get(REVENUE).getPassed());
        Assertions.assertEquals(0, mmsParams.get(PROPAGATED).getPassed());
        Assertions.assertEquals(100, mmsParams.get(VALIDATED).getPassed());
        Assertions.assertEquals(100, mmsParams.get(_VALID).getPassed());

        List<TreeNode> modifyChildren = modify.getChildren();

        TreeNode testRunTR1TreeNode = findChildrenTreeNodeByName(modifyChildren, testRun1.getName());
        Assertions.assertNotNull(testRunTR1TreeNode);
        Assertions.assertEquals(TestRunTreeNode.class, testRunTR1TreeNode.getClass());
        Assertions.assertFalse(((TestRunTreeNode) testRunTR1TreeNode).isTestCaseRemoved());

        List<TreeNode> tr1Children = testRunTR1TreeNode.getChildren();

        TreeNode lr1TreeNode = findChildrenTreeNodeByName(tr1Children, "LR1");
        Assertions.assertNotNull(lr1TreeNode);
        Assertions.assertEquals(LogRecordTreeNode.class, lr1TreeNode.getClass());
        Assertions.assertFalse(((LogRecordTreeNode) lr1TreeNode).isLeaf());

        List<TreeNode> lr1Children = lr1TreeNode.getChildren();

        TreeNode lr2TreeNode = findChildrenTreeNodeByName(lr1Children, "LR2");
        Assertions.assertNotNull(lr2TreeNode);
        Assertions.assertEquals(LogRecordTreeNode.class, lr2TreeNode.getClass());
        Assertions.assertTrue(((LogRecordTreeNode) lr2TreeNode).isLeaf());

        TreeNode lr3TreeNode = findChildrenTreeNodeByName(lr1Children, "LR3");
        Assertions.assertNotNull(lr3TreeNode);
        Assertions.assertEquals(LogRecordTreeNode.class, lr3TreeNode.getClass());
        Assertions.assertTrue(((LogRecordTreeNode) lr3TreeNode).isLeaf());

        TreeNode lr4TreeNode = findChildrenTreeNodeByName(tr1Children, "LR4");
        Assertions.assertNotNull(lr4TreeNode);
        Assertions.assertEquals(LogRecordTreeNode.class, lr4TreeNode.getClass());
        Assertions.assertTrue(((LogRecordTreeNode) lr4TreeNode).isLeaf());

        List<TreeNode> tr4Children = testRunTR4TreeNode.getChildren();

        TreeNode lr5TreeNode = findChildrenTreeNodeByName(tr4Children, "LR5");
        Assertions.assertNotNull(lr5TreeNode);
        Assertions.assertEquals(LogRecordTreeNode.class, lr5TreeNode.getClass());
        Assertions.assertFalse(((LogRecordTreeNode) lr5TreeNode).isLeaf());

        List<TreeNode> lr5Children = lr5TreeNode.getChildren();

        TreeNode lr6TreeNode = findChildrenTreeNodeByName(lr5Children, "LR6");
        Assertions.assertNotNull(lr6TreeNode);
        Assertions.assertEquals(LogRecordTreeNode.class, lr6TreeNode.getClass());
        Assertions.assertTrue(((LogRecordTreeNode) lr6TreeNode).isLeaf());

        TreeNode lr7TreeNode = findChildrenTreeNodeByName(tr4Children, "LR7");
        Assertions.assertNotNull(lr7TreeNode);
        Assertions.assertEquals(LogRecordTreeNode.class, lr7TreeNode.getClass());
        Assertions.assertTrue(((LogRecordTreeNode) lr7TreeNode).isLeaf());

        List<TreeNode> mmsChildren = mmsTreeNode.getChildren();

        TreeNode testRunTR5TreeNode = findChildrenTreeNodeByName(mmsChildren, testRun5.getName());
        Assertions.assertNotNull(testRunTR5TreeNode);
        Assertions.assertEquals(TestRunTreeNode.class, testRunTR5TreeNode.getClass());
        Assertions.assertTrue(((TestRunTreeNode) testRunTR5TreeNode).isTestCaseRemoved());

        TreeNode lr8TreeNode = findChildrenTreeNodeByName(testRunTR5TreeNode.getChildren(), "LR8");
        Assertions.assertNotNull(lr8TreeNode);
        Assertions.assertEquals(LogRecordTreeNode.class, lr8TreeNode.getClass());
        Assertions.assertTrue(((LogRecordTreeNode) lr8TreeNode).isLeaf());
    }

    @Test
    public void getExecutionRequestTreeTest_virtualExecutionRequestConfiguredshouldCreateRightTemplate() {
        // given
        ExecutionRequest executionRequest = new ExecutionRequest();
        executionRequest.setUuid(executionRequestId);
        executionRequest.setLabelTemplateId(labelTemplate.getUuid());
        executionRequest.setVirtual(true);
        // when
        TreeNode actualTreeNode =
                treeNodeService.getExecutionRequestTree(executionRequest, UUID.randomUUID(), validationTemplate, true, false);
        // then
        Assertions.assertNotNull(actualTreeNode);
        Assertions.assertTrue(((ExecutionRequestTreeNode) actualTreeNode).isExecutionRequestVirtual());
    }
}
