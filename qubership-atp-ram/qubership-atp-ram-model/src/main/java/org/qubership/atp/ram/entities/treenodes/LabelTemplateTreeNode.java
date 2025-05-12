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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.qubership.atp.ram.entities.treenodes.labelparams.CountReportLabelParam;
import org.qubership.atp.ram.models.LabelTemplate.LabelTemplateNode;
import org.springframework.util.CollectionUtils;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class LabelTemplateTreeNode extends TreeNode {
    private UUID labelId;
    private Map<UUID, String> errors = new HashMap<>();
    private int passedRate;
    private int failedRate;
    private int warningRate;
    private int testRunCount;
    private int testRunPassedCount;
    private List<CountReportLabelParam> reportLabelParams = new ArrayList<>();

    /**
     * LabelTreeNode constructor.
     *
     * @param labelTemplateNode label template node
     */
    public LabelTemplateTreeNode(LabelTemplateNode labelTemplateNode, UUID executionRequestId) {
        this.id = UUID.randomUUID();
        this.name = labelTemplateNode.getLabelName();
        this.nodeType = TreeNodeType.LABEL_TEMPLATE_NODE;
        this.executionRequestId = executionRequestId;
        this.labelId = labelTemplateNode.getLabelId();
        this.errors = labelTemplateNode.getErrors();
        this.passedRate = labelTemplateNode.getPassedRate();
        this.warningRate = labelTemplateNode.getWarningRate();
        this.failedRate = labelTemplateNode.getFailedRate();
        this.testRunCount = labelTemplateNode.getTestRunCount();
        this.testRunPassedCount = labelTemplateNode.getTestRunPassedCount();
    }

    public LabelTemplateTreeNode(String name) {
        super(name);
    }

    /**
     * Set children.
     *
     * @param nodes children nodes
     */
    public void setChildren(Collection<TreeNode> nodes) {
        if (CollectionUtils.isEmpty(this.children)) {
            this.children = new ArrayList<>();
        }

        this.children.addAll(nodes);
    }
}
