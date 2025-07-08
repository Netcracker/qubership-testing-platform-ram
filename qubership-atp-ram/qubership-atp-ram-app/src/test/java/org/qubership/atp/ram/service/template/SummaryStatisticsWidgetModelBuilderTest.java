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

package org.qubership.atp.ram.service.template;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.atp.ram.entities.treenodes.ExecutionRequestTreeNode;
import org.qubership.atp.ram.entities.treenodes.LabelTemplateTreeNode;
import org.qubership.atp.ram.entities.treenodes.TreeNode;
import org.qubership.atp.ram.entities.treenodes.TreeNodeType;
import org.qubership.atp.ram.models.WidgetType;
import org.qubership.atp.ram.service.mail.ReportParams;
import org.qubership.atp.ram.service.template.impl.AbstractWidgetModelBuilder;
import org.qubership.atp.ram.service.template.impl.SummaryStatisticsWidgetModelBuilder;
import org.qubership.atp.ram.services.TreeNodeService;

public class SummaryStatisticsWidgetModelBuilderTest {

    private ReportParams reportParams;
    private final TreeNodeService treeNodeService = mock(TreeNodeService.class);

    private final AbstractWidgetModelBuilder builder = new SummaryStatisticsWidgetModelBuilder(treeNodeService);

    @BeforeEach
    public void setUp(){
        reportParams = createReportParams();
    }

    private ExecutionRequestTreeNode createErTreeNode() {

        ExecutionRequestTreeNode exTreeNode = new ExecutionRequestTreeNode();
        exTreeNode.setPassedRate(45);
        exTreeNode.setName("Root ER Tree Node");
        exTreeNode.setChildren(getLabelTemplateNodes());
        exTreeNode.setNodeType(TreeNodeType.EXECUTION_REQUEST_NODE);
        exTreeNode.setValidationLabelsOrder(Collections.emptyList());
        return exTreeNode;
    }

    private List<TreeNode> getLabelTemplateNodes() {
        LabelTemplateTreeNode labelTemplateTreeNode = new LabelTemplateTreeNode();
        labelTemplateTreeNode.setName("Label Tree Node 1");
        labelTemplateTreeNode.setTestRunCount(100);
        labelTemplateTreeNode.setPassedRate(80);
        return Collections.singletonList(labelTemplateTreeNode);
    }

    private ReportParams createReportParams() {
        ReportParams reportParams = new ReportParams();
        reportParams.setExecutionRequestUuid(UUID.randomUUID());
        reportParams.setRecipients("example@example.com");
        reportParams.setSubject("Test Subject");
        reportParams.setDescriptions(new HashMap<String, String>(){{
            put(WidgetType.SERVER_SUMMARY.toString(), "Test description");
        }});

        return reportParams;
    }


    @Test
    public void onSummaryStatisticsWidgetModelBuilder_whenGetModel_AllDataStructureAdded(){
        when(treeNodeService.getExecutionRequestWidgetTree(any(), any(), any(), any(), eq(false), eq(true))).thenReturn(createErTreeNode());
        Map<String, Object> model = builder.getModel(reportParams);

        Assertions.assertNotNull(model);
        Assertions.assertNotNull(model.get("tableModel"));
    }
}
