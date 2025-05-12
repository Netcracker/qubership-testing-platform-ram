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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.qubership.atp.ram.utils.StreamUtils.extractIds;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.qubership.atp.ram.TestRunsMock;
import org.qubership.atp.ram.entities.treenodes.ExecutionRequestTreeNode;
import org.qubership.atp.ram.entities.treenodes.TreeNode;
import org.qubership.atp.ram.model.datacontext.TestRunsDataContext;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.LabelTemplate;
import org.qubership.atp.ram.models.TestCase;
import org.qubership.atp.ram.models.TestCaseOrder;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.models.ValidationLabelConfigTemplate;
import org.qubership.atp.ram.models.ValidationLabelConfigTemplate.LabelConfig;
import org.qubership.atp.ram.utils.StreamUtils;
import org.qubership.atp.ram.utils.TestCaseMock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.ImmutableSet;

@ExtendWith(SpringExtension.class)
public class TreeNodeServiceTest {

    @InjectMocks
    private TreeNodeService treeNodeService;

    @Mock
    private CatalogueService catalogueService;

    @Mock
    private TestRunService testRunService;

    @Mock
    private LabelTemplateNodeService labelTemplateNodeService;

    @Mock
    private ExecutionRequestService executionRequestService;

    @Test
    public void sortScopeGroupTestRuns_shuffledTestRunsInScopeGroup_shouldBeSortedInCorrectOrder() {
        // given
        List<TestRun> scopeGroupTestRuns = new ArrayList<>();

        for (int i = 1; i < 10; i++) {

            TestRun testRun = TestRunsMock.generateTestRun("TR " + i);
            testRun.setOrder(i);

            scopeGroupTestRuns.add(testRun);
        }

        List<UUID> rightOrderIds = StreamUtils.extractIdsToList(scopeGroupTestRuns);

        Collections.shuffle(scopeGroupTestRuns);

        // when
        List<TestRun> result = treeNodeService.sortScopeGroupTestRuns(scopeGroupTestRuns);

        // then
        assertNotNull(result);

        List<UUID> resultOrderIds = StreamUtils.extractIdsToList(result);
        assertFalse(resultOrderIds.isEmpty());

        assertThat(resultOrderIds, is(rightOrderIds));
    }

    @Test
    public void sortScopeGroupTestRuns_shuffledTestRunsInScopeGroupWithIdenticalCaseIds_shouldBeSortedInCorrectOrder() {
        // given
        UUID testScopeId = UUID.randomUUID();
        List<TestRun> scopeGroupTestRuns = new ArrayList<>();
        List<TestCase> testCases = new ArrayList<>();

        UUID caseId = UUID.randomUUID();
        for (int i = 1; i < 5; i++) {
            TestCase testCase = TestCaseMock.generateTestCase("TC " + i);
            testCase.setUuid(caseId);
            testCase.setOrder(Collections.singletonList(new TestCaseOrder(testScopeId, i)));

            TestRun testRun = TestRunsMock.generateTestRun("TR " + i);
            testRun.setTestCaseId(testCase.getUuid());

            scopeGroupTestRuns.add(testRun);
            testCases.add(testCase);
        }

        caseId = UUID.randomUUID();
        for (int i = 5; i < 10; i++) {
            TestCase testCase = TestCaseMock.generateTestCase("TC " + i);
            testCase.setUuid(caseId);
            testCase.setOrder(Collections.singletonList(new TestCaseOrder(testScopeId, i)));

            TestRun testRun = TestRunsMock.generateTestRun("TR " + i);
            testRun.setTestCaseId(testCase.getUuid());

            scopeGroupTestRuns.add(testRun);
            testCases.add(testCase);
        }

        Set<UUID> rightOrderIds = StreamUtils.extractIds(scopeGroupTestRuns);

        Collections.shuffle(scopeGroupTestRuns);
        Collections.shuffle(testCases);

        Set<UUID> testCaseIds = extractIds(scopeGroupTestRuns, TestRun::getTestCaseId);

        List<TestRun> result = treeNodeService.sortScopeGroupTestRuns(scopeGroupTestRuns);

        assertNotNull(result);

        Set<UUID> resultOrderIds = StreamUtils.extractIds(result);
        assertFalse(resultOrderIds.isEmpty());

        assertThat(resultOrderIds, is(rightOrderIds));
    }

    @Test
    public void orderValidationLabels() {
        Set<String> labels = ImmutableSet.of("testVL2", "testVL3", "testVL5");
        ValidationLabelConfigTemplate template = new ValidationLabelConfigTemplate();
        TreeSet<LabelConfig> configs = new TreeSet<>();
        configs.add(new LabelConfig(ImmutableSet.of("TESTVL2"), "TESTVL2", false, false, 0));
        configs.add(new LabelConfig(ImmutableSet.of("TESTVL3"), "TESTVL3", false, false, 1));
        template.setLabels(configs);
        List<String> expectedResult = Arrays.asList("testVL5");
        List<String> actualResult = treeNodeService.orderValidationLabels(labels, template);

        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void getExecutionRequestTree_givenTestRunsInsideOneLabel_sortTestRuns() {
        List<String> sortedTestRunNames = Arrays.asList(
                "1TestRun",
                "Test",
                "TestRun1",
                "[TestRun]",
                "testRun"
        );
        String labelName = "label";
        UUID labelId = UUID.randomUUID();
        UUID labelTemplateId = UUID.randomUUID();
        LabelTemplate.LabelTemplateNode labelNode = new LabelTemplate.LabelTemplateNode(labelId, labelName);
        LabelTemplate labelTemplate = new LabelTemplate();
        labelTemplate.setUuid(labelTemplateId);
        labelTemplate.setName("labelTemplate");
        labelTemplate.setLabelNodes(Arrays.asList(labelNode));
        Set labels = new HashSet();
        labels.add(labelId);
        List<TestRun> testruns = new ArrayList<>();
        for (String name: sortedTestRunNames) {
            TestRun tr = TestRunsMock.generateTestRun(name);
            tr.setLabelIds(labels);
            testruns.add(tr);
            labelNode.addTestRun(tr.getUuid());
        }
        Collections.shuffle(testruns);
        UUID executionRequestId = UUID.randomUUID();
        ExecutionRequest executionRequest = new ExecutionRequest();
        executionRequest.setUuid(executionRequestId);
        executionRequest.setLabelTemplateId(labelTemplate.getUuid());
        TestRunsDataContext context = TestRunsDataContext.builder()
                .testRunsMap(StreamUtils.toIdEntityMap(testruns))
                .testRunValidationLogRecordsMap(Collections.EMPTY_MAP)
                .testRunTestCasesMap(Collections.EMPTY_MAP)
                .build();
        when(testRunService.findAllByExecutionRequestId(eq(executionRequestId))).thenReturn(testruns);
        when(testRunService.getTestRunsDataContext(any(), any(), anyBoolean())).thenReturn(context);
        when(labelTemplateNodeService.getLabelTemplate(eq(labelTemplateId))).thenReturn(labelTemplate);
        when(labelTemplateNodeService.populateLabelTemplateWithTestRuns(any(), (LabelTemplate) any())).thenReturn(labelTemplate);

        TreeNode treeNode =
                treeNodeService.getExecutionRequestTree(executionRequest, labelTemplateId, null, true, false);

        assertEquals( 1, treeNode.getChildren().size(), "Root node should contain 1 children with label");
        assertEquals(testruns.size(), treeNode.getChildren().get(0).getChildren().size(), "All testRuns should be inside the label");
        List<String> actualTestRunNames = treeNode.getChildren().get(0).getChildren()
                .stream().map(TreeNode::getName).collect(Collectors.toList());
        assertEquals(sortedTestRunNames, actualTestRunNames, "TestRuns should be ordered by name inside label");
    }

    @Test
    public void getExecutionRequestTestRunTreeTest_virtualExecutionRequestConfigured_shouldReturnTreeNodeWithVirtualFlag() {
        // given
        ExecutionRequest executionRequest = new ExecutionRequest();
        executionRequest.setUuid(UUID.randomUUID());
        executionRequest.setVirtual(true);
        // when
        when(executionRequestService.findById(any())).thenReturn(executionRequest);
        TreeNode actualTreeNode = treeNodeService.getExecutionRequestTestRunTree(executionRequest.getUuid());
        // then
        assertNotNull(actualTreeNode);
        Assertions.assertTrue(((ExecutionRequestTreeNode) actualTreeNode).isExecutionRequestVirtual());
    }
}
