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
import static java.util.Collections.singletonList;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.qubership.atp.auth.springbootstarter.exceptions.AtpIllegalNullableArgumentException;
import org.qubership.atp.ram.TestRunsMock;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.exceptions.labeltemplates.RamLabelTemplateWithoutChildrenNodesException;
import org.qubership.atp.ram.models.Label;
import org.qubership.atp.ram.models.LabelTemplate;
import org.qubership.atp.ram.models.LabelTemplate.LabelTemplateNode;
import org.qubership.atp.ram.models.TestRun;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class LabelTemplateNodeServiceTest {

    @InjectMocks
    private LabelTemplateNodeService service;

    @Mock
    private CatalogueService catalogueService;
    @Mock
    private TestRunService testRunService;

    private LabelTemplate labelTemplate = new LabelTemplate();

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
    private TestRun testRun7;
    private TestRun testRun8;

    private List<TestRun> testRuns;
    @BeforeEach
    public void setUp() {
        this.labelTemplate = new LabelTemplate();

        Label offlineLabel = generateLabel("Offline");
        Label onlineLabel = generateLabel("Online");
        Label dataNationalLabel = generateLabel("Data National");
        Label modifyLabel = generateLabel("Modify");
        Label mmsLabel = generateLabel("MMS");
        Label reorderedLabel = generateLabel("Reordered");

        this.offlineNode = new LabelTemplateNode(offlineLabel.getUuid(), "Offline");
        this.dataNationalNode = new LabelTemplateNode(dataNationalLabel.getUuid(), "Data National");
        this.modifyNode = new LabelTemplateNode(modifyLabel.getUuid(), "Modify");
        this.disconnectNode = new LabelTemplateNode(UUID.randomUUID(), "Disconnect");
        this.mmsNode = new LabelTemplateNode(mmsLabel.getUuid(), "MMS");
        this.onlineNode = new LabelTemplateNode(onlineLabel.getUuid(), "Online");
        this.dateNode = new LabelTemplateNode(UUID.randomUUID(), "Date");
        this.voiceNode = new LabelTemplateNode(UUID.randomUUID(), "Voice");
        this.reorderedNode = new LabelTemplateNode(reorderedLabel.getUuid(), "Reordered");

        this.labelTemplate.setLabelNodes(new ArrayList<>(asList(offlineNode, onlineNode)));
        this.offlineNode.setChildren(new ArrayList<>(asList(dataNationalNode, mmsNode)));
        this.dataNationalNode.setChildren(new ArrayList<>(asList(modifyNode, disconnectNode)));
        this.onlineNode.setChildren(new ArrayList<>(asList(dateNode, voiceNode)));
        this.dateNode.setChildren(new ArrayList<>(singletonList(reorderedNode)));

        this.testRun1 = TestRunsMock.generateTestRun("TR 1", TestingStatuses.PASSED);
        this.testRun1.setLabelIds(new HashSet<>(asList(offlineLabel.getUuid(), dataNationalLabel.getUuid(),
                modifyLabel.getUuid())));
        this.testRun2 = TestRunsMock.generateTestRun("TR 2", TestingStatuses.PASSED);
        this.testRun2.setLabelIds(new HashSet<>(asList(offlineLabel.getUuid(), modifyLabel.getUuid())));
        this.testRun3 = TestRunsMock.generateTestRun("TR 3", TestingStatuses.FAILED);
        this.testRun3.setLabelIds(new HashSet<>(asList(offlineLabel.getUuid(), onlineLabel.getUuid(),
                modifyLabel.getUuid())));
        this.testRun4 = TestRunsMock.generateTestRun("TR 4", TestingStatuses.FAILED);
        this.testRun4.setLabelIds(new HashSet<>(asList(offlineLabel.getUuid(), dataNationalLabel.getUuid())));
        this.testRun5 = TestRunsMock.generateTestRun("TR 5", TestingStatuses.PASSED);
        this.testRun5.setLabelIds(new HashSet<>(asList(offlineLabel.getUuid(), mmsLabel.getUuid(),
                reorderedLabel.getUuid())));
        this.testRun6 = TestRunsMock.generateTestRun("TR 6", TestingStatuses.FAILED);
        this.testRun6.setLabelIds(new HashSet<>(emptyList()));
        this.testRun7 = TestRunsMock.generateTestRun("TR 7", TestingStatuses.SKIPPED);
        this.testRun7.setLabelIds(new HashSet<>(asList(offlineLabel.getUuid(), dataNationalLabel.getUuid())));
        this.testRun8 = TestRunsMock.generateTestRun("TR 8", TestingStatuses.SKIPPED);
        this.testRun8.setLabelIds(new HashSet<>(emptyList()));

        this.testRuns = asList(testRun1, testRun2, testRun3, testRun4, testRun5, testRun6, testRun7, testRun8);

    }

    @Test
    public void testGetLabelTemplate_shouldThrowException_whenLabelTemplateIdIsNull() {
        Assertions.assertThrows(AtpIllegalNullableArgumentException.class, () -> {
            service.getLabelTemplate(null);
        });
    }

    @Test
    public void testGetLabelTemplate_shouldThrowException_whenLabelTemplateHasNoNodes() {
        when(catalogueService.getLabelTemplateById(any())).thenReturn(new LabelTemplate());
        Assertions.assertThrows(RamLabelTemplateWithoutChildrenNodesException.class, () -> {
            service.getLabelTemplate(UUID.randomUUID());
        });
    }

    /*
     * Label template:
     *   Offline
     *       Data National
     *           Modify
     *           Disconnect
     *       MMS
     *   Online
     *       Date
     *           Reordered
     *       Voice
     *
     * Test runs -> Test cases:
     *   TR 1
     *      TC 1 -> Labels: ["Offline", "Data National", "Modify"]
     *   TR 2
     *      TC 2 -> Labels: ["Offline", "Modify"]
     *   TR 3
     *      TC 3 -> Labels: ["Offline", "Online", "Modify"]
     *   TR 4
     *      TC 4 -> Labels: ["Offline", "Data National"]
     *   TR 5
     *      TC 5 -> Labels: ["Offline", "MMS", "Reordered"]
     *   TR 6
     *      TC 6 -> Labels: []
     *
     * Expected Label template result:
     *   Offline
     *       Data National
     *           Modify
     *              TR 1
     *           Disconnect
     *           TR 4
     *           TR 7 (skipped)
     *       MMS
     *           TR 5
     *   Online
     *       Date
     *           Reordered
     *       Voice
     *   Unknown
     *       TR 2
     *       TR 3
     *       TR 6
     *       TR 8 (skipped)
     * */
    @Test
    public void populateLabelTemplateWithTestRuns_shouldCreateRightTemplate() {
        when(catalogueService.getLabelTemplateById(any())).thenReturn(labelTemplate);

        LabelTemplate resultLabelTemplate = service.populateLabelTemplateWithTestRuns(testRuns, UUID.randomUUID());

        // 'Unknown' node checks
        this.assertUnknownNode(resultLabelTemplate);

        // Assert template root nodes
        List<LabelTemplateNode> resultTemplateTopLabelNodes = resultLabelTemplate.getLabelNodes();
        Assertions.assertNotNull(resultTemplateTopLabelNodes, "Top level nodes shouldn't be null");
        Assertions.assertFalse(resultTemplateTopLabelNodes.isEmpty(), "Top level nodes shouldn't be empty");

        // 'Offline' node checks
        this.assertOfflineNode(resultTemplateTopLabelNodes);

        // 'Online' node checks
        this.assertOnlineNode(resultTemplateTopLabelNodes);
    }

    private void assertUnknownNode(LabelTemplate resultLabelTemplate) {
        LabelTemplateNode unknownNode = resultLabelTemplate.getUnknownNode();
        Assertions.assertNotNull(unknownNode, "An unknown node shouldn't be null");

        Set<UUID> unknownNodeTestRunIds = unknownNode.getTestRunIds();
        Assertions.assertNotNull(unknownNodeTestRunIds, "An unknown node test run ids shouldn't be null");
        Assertions.assertFalse(unknownNodeTestRunIds.isEmpty(), "An unknown node test run ids shouldn't be empty");

        Assertions.assertTrue(unknownNodeTestRunIds.contains(this.testRun2.getUuid()), "An unknown node should contain TR 2");
        Assertions.assertTrue(unknownNodeTestRunIds.contains(this.testRun3.getUuid()), "An unknown node should contain TR 3");
        Assertions.assertTrue(unknownNodeTestRunIds.contains(this.testRun6.getUuid()), "An unknown node should contain TR 6");

        Map<UUID, String> unknownNodeErrors = unknownNode.getErrors();
        Assertions.assertNotNull(unknownNodeErrors, "An unknown node errors shouldn't be null");
        Assertions.assertFalse(unknownNodeErrors.isEmpty(), "An unknown node errors shouldn't be empty");
    }

    private void assertOfflineNode(List<LabelTemplateNode> resultTemplateTopLabelNodes) {
        LabelTemplateNode offlineNode = findNode(resultTemplateTopLabelNodes, this.offlineNode.getLabelId());
        Assertions.assertNotNull(offlineNode, "Top level nodes should contain 'Offline' label node");

        List<LabelTemplateNode> children = offlineNode.getChildren();
        Assertions.assertNotNull(children, "Result 'Offline' node children shouldn't be null");
        Assertions.assertFalse(children.isEmpty(), "Result 'Offline node children shouldn't be empty");

        Assertions.assertEquals(2, children.size(), "Result 'Offline' node children size should be 2");

        LabelTemplateNode dataNationalNode = findNode(children, this.dataNationalNode.getLabelId());
        this.assertDataNationalNode(dataNationalNode);

        LabelTemplateNode mmsNode = findNode(children, this.mmsNode.getLabelId());
        this.assertMmsNode(mmsNode);
    }

    private void assertDataNationalNode(LabelTemplateNode dataNationalNode) {
        Assertions.assertNotNull(dataNationalNode, "Result 'Data National' node shouldn't be null");

        List<LabelTemplateNode> children = dataNationalNode.getChildren();
        Assertions.assertNotNull(children, "Result 'Data National' node children shouldn't be null");
        Assertions.assertFalse(children.isEmpty(), "Result 'Data National' node children shouldn't be empty");

        Assertions.assertEquals(2, children.size(), "Result 'Offline' node children size should be 2");

        Set<UUID> testRunIds = dataNationalNode.getTestRunIds();
        Assertions.assertNotNull(testRunIds, "Result 'Data National' node test run ids shouldn't be null");
        Assertions.assertFalse(testRunIds.isEmpty(), "Result 'Data National' node test run ids shouldn't be empty");
        Assertions.assertTrue(testRunIds.contains(testRun4.getUuid()), "Result 'Data National' node should contain TR 4");

        LabelTemplateNode modifyNode = findNode(children, this.modifyNode.getLabelId());
        this.assertModifyNode(modifyNode);

        LabelTemplateNode disconnectNode = findNode(children, this.disconnectNode.getLabelId());
        this.assertDisconnectNode(disconnectNode);
    }

    private void assertModifyNode(LabelTemplateNode modifyNode) {
        Assertions.assertNotNull(modifyNode, "Result 'Modify' node shouldn't be null");

        List<LabelTemplateNode> children = modifyNode.getChildren();
        Assertions.assertNull(children, "Result 'Modify' node children should be null");

        Set<UUID> testRunIds = modifyNode.getTestRunIds();
        Assertions.assertNotNull(testRunIds, "Result 'Modify' node test run ids shouldn't be null");
        Assertions.assertFalse(testRunIds.isEmpty(), "Result 'Modify' node test run ids shouldn't be empty");
        Assertions.assertTrue(testRunIds.contains(testRun1.getUuid()), "Result 'Modify' node should contain TR 1");
    }

    private void assertDisconnectNode(LabelTemplateNode disconnectNode) {
        Assertions.assertNotNull(disconnectNode, "Result 'Disconnect' node shouldn't be null");

        List<LabelTemplateNode> children = disconnectNode.getChildren();
        Assertions.assertNull(children, "Result 'Disconnect' node children should be null");
    }

    private void assertMmsNode(LabelTemplateNode mmsNode) {
        Assertions.assertNotNull(mmsNode, "MMS' node shouldn't be null");

        List<LabelTemplateNode> children = mmsNode.getChildren();
        Assertions.assertNull(children, "Result 'MMS' node children should be null");
    }

    private void assertOnlineNode(List<LabelTemplateNode> resultTemplateTopLabelNodes) {
        LabelTemplateNode onlineNode = findNode(resultTemplateTopLabelNodes, this.onlineNode.getLabelId());
        Assertions.assertNotNull(onlineNode, "Top level nodes should contain 'Online' label node");

        List<LabelTemplateNode> children = onlineNode.getChildren();
        Assertions.assertNotNull(children, "Result 'Online' node children shouldn't be null");
        Assertions.assertFalse(children.isEmpty(), "Result 'Online node children shouldn't be empty");

        Assertions.assertEquals(2, children.size(), "Result 'Online' node children size should be 2");

        LabelTemplateNode dateNode = findNode(children, this.dateNode.getLabelId());
        this.assertDateNode(dateNode);

        LabelTemplateNode voiceNode = findNode(children, this.voiceNode.getLabelId());
        this.assertVoiceNode(voiceNode);
    }

    private void assertDateNode(LabelTemplateNode dateNode) {
        Assertions.assertNotNull(dateNode, "Result 'Date' node shouldn't be null");

        List<LabelTemplateNode> children = dateNode.getChildren();
        Assertions.assertNotNull(children, "Result 'Date' node children shouldn't be null");
        Assertions.assertFalse(children.isEmpty(), "Result 'Date' node children shouldn't be empty");

        Assertions.assertEquals(1, children.size(), "Result 'Offline' node children size should be 1");

        LabelTemplateNode reorderedNode = findNode(children, this.reorderedNode.getLabelId());
        this.assertReorderedNode(reorderedNode);
    }

    private void assertReorderedNode(LabelTemplateNode reorderedNode) {
        Assertions.assertNotNull(reorderedNode, "Result 'Reordered' node shouldn't be null");

        List<LabelTemplateNode> children = reorderedNode.getChildren();
        Assertions.assertNull(children, "Result 'Reordered' node children should be null");
    }

    private void assertVoiceNode(LabelTemplateNode voiceNode) {
        Assertions.assertNotNull(voiceNode, "Result 'Voice' node shouldn't be null");

        List<LabelTemplateNode> children = voiceNode.getChildren();
        Assertions.assertNull(children, "Result 'Voice' node children should be null");
    }

    private LabelTemplateNode findNode(List<LabelTemplateNode> nodes, UUID nodeLabelId) {
        return nodes.stream()
                .filter(node -> node.getLabelId().equals(nodeLabelId))
                .findFirst()
                .orElse(null);
    }

    private Label generateLabel(String name) {
        Label label = new Label();
        label.setUuid(UUID.randomUUID());
        label.setName(name);

        return label;
    }

    @Test
    public void populateLabelTemplateWithTestRuns_shouldCreateRightTemplateWithCalculatedRates() {
        when(catalogueService.getLabelTemplateById(any())).thenReturn(labelTemplate);

//        when(testRunService.findAllRatesByUuidIn(Collections.singleton(testRun1.getUuid())))
//                .thenReturn(Collections.singletonList(testRun1));

        Set<UUID> dataNationalTestRunIds = new HashSet<>(Arrays.asList(testRun4.getUuid(), testRun7.getUuid()));
//        when(testRunService.findAllRatesByUuidIn(dataNationalTestRunIds))
//                .thenReturn(Arrays.asList(testRun4, testRun7));

//        when(testRunService.findAllRatesByUuidIn(Collections.singleton(testRun5.getUuid())))
//                .thenReturn(Collections.singletonList(testRun5));

        Set<UUID> unknownTestRunIds = new HashSet<>(Arrays.asList(testRun2.getUuid(), testRun6.getUuid(),
                testRun3.getUuid(), testRun8.getUuid()));
//        when(testRunService.findAllRatesByUuidIn(unknownTestRunIds))
//                .thenReturn(Arrays.asList(testRun2, testRun6, testRun3, testRun8));

        LabelTemplate resultLabelTemplate = service.populateLabelTemplateWithTestRuns(testRuns, UUID.randomUUID());

        LabelTemplateNode unknownNode = resultLabelTemplate.getUnknownNode();
        validateUnknownNodeRates(unknownNode);

        List<LabelTemplateNode> resultTemplateTopLabelNodes = resultLabelTemplate.getLabelNodes();

        validateOnlineNodeRate(resultTemplateTopLabelNodes);
        validateOfflineNodeRate(resultTemplateTopLabelNodes);
    }

    private void validateOfflineNodeRate(List<LabelTemplateNode> resultTemplateTopLabelNodes) {
        int warningRateExp = 0;
        int failedRateExp = 33;
        int passedRateExp = 67;

        LabelTemplateNode offlineNode = findNode(resultTemplateTopLabelNodes, this.offlineNode.getLabelId());

        Assertions.assertEquals( warningRateExp, offlineNode.getWarningRate(),
                "Warning rate for offline node is " + warningRateExp);
        Assertions.assertEquals(failedRateExp, offlineNode.getFailedRate(),
                "Failed rate for offline node is " + failedRateExp);
        Assertions.assertEquals(passedRateExp, offlineNode.getPassedRate(),
                "Passed rate for offline node is " + passedRateExp);

        LabelTemplateNode dataNationalNode = findNode(offlineNode.getChildren(), this.dataNationalNode.getLabelId());
        validateDataNationalNodeRate(dataNationalNode);

        LabelTemplateNode mmsNode = findNode(offlineNode.getChildren(), this.mmsNode.getLabelId());
        validateMmsNodeRate(mmsNode);
    }

    private void validateMmsNodeRate(LabelTemplateNode mmsNode) {
        int warningRateExp = 0;
        int failedRateExp = 0;
        int passedRateExp = 100;

        Assertions.assertEquals(warningRateExp, mmsNode.getWarningRate(),
                "Warning rate for mmsNode is " + warningRateExp);
        Assertions.assertEquals(failedRateExp, mmsNode.getFailedRate(),
                "Failed rate for mmsNode is " + failedRateExp);
        Assertions.assertEquals(passedRateExp, mmsNode.getPassedRate(),
                "Passed rate for mmsNode is " + passedRateExp);
    }

    private void validateDataNationalNodeRate(LabelTemplateNode dataNationalNode) {
        int warningRateExp = 0;
        int failedRateExp = 50;
        int passedRateExp = 50;
        int totalTestRunCountExp = 2; // minus one skipped test run
        int testRunIdsExp = 2;

        Assertions.assertEquals(testRunIdsExp, dataNationalNode.getTestRunIds().size(),
                "Failed to validate data national node total test run ids size");
        Assertions.assertEquals(totalTestRunCountExp, dataNationalNode.getTestRunCount(),
                "Failed to validate data national node total test runs count");
        Assertions.assertEquals(warningRateExp, dataNationalNode.getWarningRate(),
                "Failed to validate data national node warning rate");
        Assertions.assertEquals(failedRateExp, dataNationalNode.getFailedRate(),
                "Failed to validate data national node failed rate");
        Assertions.assertEquals(passedRateExp, dataNationalNode.getPassedRate(),
                "Failed to validate data national node passed rate");

        LabelTemplateNode modifyNode = findNode(dataNationalNode.getChildren(), this.modifyNode.getLabelId());
        validateModifyNodeRate(modifyNode);

        LabelTemplateNode disconnectNode = findNode(dataNationalNode.getChildren(), this.disconnectNode.getLabelId());
        validateDisconnectNodeRate(disconnectNode);
    }

    private void validateDisconnectNodeRate(LabelTemplateNode disconnectNode) {
        int warningRateExp = 0;
        int failedRateExp = 0;
        int passedRateExp = 0;

        Assertions.assertEquals(warningRateExp, disconnectNode.getWarningRate(),
                "Warning rate for disconnectNode is " + warningRateExp);
        Assertions.assertEquals(failedRateExp, disconnectNode.getFailedRate(),
                "Failed rate for disconnectNode is " + failedRateExp);
        Assertions.assertEquals(passedRateExp, disconnectNode.getPassedRate(),
                "Passed rate for disconnectNode is " + passedRateExp);
    }

    private void validateModifyNodeRate(LabelTemplateNode modifyNode) {
        int warningRateExp = 0;
        int failedRateExp = 0;
        int passedRateExp = 100;

        Assertions.assertEquals(warningRateExp, modifyNode.getWarningRate(),
                "Warning rate for modifyNode is " + warningRateExp);
        Assertions.assertEquals(failedRateExp, modifyNode.getFailedRate(),
                "Failed rate for modifyNode is " + failedRateExp);
        Assertions.assertEquals(passedRateExp, modifyNode.getPassedRate(),
                "Passed rate for modifyNode is " + passedRateExp);
    }

    private void validateOnlineNodeRate(List<LabelTemplateNode> resultTemplateTopLabelNodes) {
        int warningRateExp = 0;
        int failedRateExp = 0;
        int passedRateExp = 0;

        LabelTemplateNode onlineNode = findNode(resultTemplateTopLabelNodes, this.onlineNode.getLabelId());
        Assertions.assertEquals(warningRateExp, onlineNode.getWarningRate(),
                "Warning rate for onlineNode is " + warningRateExp);
        Assertions.assertEquals(failedRateExp, onlineNode.getFailedRate(),
                "Failed rate for onlineNode is " + failedRateExp);
        Assertions.assertEquals(passedRateExp, onlineNode.getPassedRate(),
                "Passed rate for onlineNode is " + passedRateExp);

        LabelTemplateNode dateNode = findNode(onlineNode.getChildren(), this.dateNode.getLabelId());
        validateDateNodeRate(dateNode);

        LabelTemplateNode voiceNode = findNode(onlineNode.getChildren(), this.voiceNode.getLabelId());
        validateVoiceNodeRate(voiceNode);
    }

    private void validateVoiceNodeRate(LabelTemplateNode voiceNode) {
        int warningRateExp = 0;
        int failedRateExp = 0;
        int passedRateExp = 0;

        Assertions.assertEquals( warningRateExp, voiceNode.getWarningRate(),
                "Warning rate for voiceNode is " + warningRateExp);
        Assertions.assertEquals(failedRateExp, voiceNode.getFailedRate(),
                "Failed rate for voiceNode is " + failedRateExp);
        Assertions.assertEquals(passedRateExp, voiceNode.getPassedRate(),
                "Passed rate for voiceNode is " + passedRateExp);
    }

    private void validateDateNodeRate(LabelTemplateNode dateNode) {
        int warningRateExp = 0;
        int failedRateExp = 0;
        int passedRateExp = 0;

        Assertions.assertEquals(warningRateExp, dateNode.getWarningRate(),
                "Warning rate for dateNode is " + warningRateExp);
        Assertions.assertEquals(failedRateExp, dateNode.getFailedRate(),
                "Failed rate for dateNode is " + failedRateExp);
        Assertions.assertEquals(passedRateExp, dateNode.getPassedRate(),
                "Passed rate for dateNode is " + passedRateExp);

        LabelTemplateNode reorderedNode = findNode(dateNode.getChildren(), this.reorderedNode.getLabelId());
        validateReorderedNodeRate(reorderedNode);
    }

    private void validateReorderedNodeRate(LabelTemplateNode reorderedNode) {
        int warningRateExp = 0;
        int failedRateExp = 0;
        int passedRateExp = 0;

        Assertions.assertEquals(warningRateExp, reorderedNode.getWarningRate(),
                "Warning rate for reorderedNode is " + warningRateExp);
        Assertions.assertEquals(failedRateExp, reorderedNode.getFailedRate(),
                "Failed rate for reorderedNode is " + failedRateExp);
        Assertions.assertEquals(passedRateExp, reorderedNode.getPassedRate(),
                "Passed rate for reorderedNode is " + passedRateExp);
    }

    private void validateUnknownNodeRates(LabelTemplateNode unknownNode) {
        int warningRateExp = 0;
        int failedRateExp = 67;
        int passedRateExp = 33;
        int totalTestRunCountExp = 3; // minus one skipped test run
        int testRunIdsExp = 4;

        Assertions.assertEquals(testRunIdsExp, unknownNode.getTestRunIds().size(),
                "Failed to validate unknown node total test run ids size");
        Assertions.assertEquals(totalTestRunCountExp, unknownNode.getTestRunCount(),
                "Failed to validate unknown node total test runs count");
        Assertions.assertEquals(warningRateExp, unknownNode.getWarningRate(),
                "Failed to validate unknown node warning rate");
        Assertions.assertEquals(failedRateExp, unknownNode.getFailedRate(),
                "Failed to validate unknown node failed rate");
        Assertions.assertEquals(passedRateExp, unknownNode.getPassedRate(),
                "Failed to validate unknown node passed rate");
    }
}
