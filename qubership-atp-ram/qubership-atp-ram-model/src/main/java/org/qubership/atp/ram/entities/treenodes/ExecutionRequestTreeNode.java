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
import java.util.List;
import java.util.UUID;

import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.LabelTemplate;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ExecutionRequestTreeNode extends TreeNode {
    private Integer passedRate;
    private UUID labelTemplateId;
    private String labelTemplateName;
    private List<String> validationLabelsOrder = new ArrayList<>();
    private UUID testScopeId;

    /**
     * ExecutionRequestTreeNode constructor.
     *
     * @param executionRequest execution request
     */
    public ExecutionRequestTreeNode(ExecutionRequest executionRequest) {
        this.id = UUID.randomUUID();
        this.executionRequestId = executionRequest.getUuid();
        this.name = executionRequest.getName();
        this.passedRate = Math.round(executionRequest.getPassedRate());
        this.nodeType = TreeNodeType.EXECUTION_REQUEST_NODE;
        this.testScopeId = executionRequest.getTestScopeId();
        this.isExecutionRequestVirtual = executionRequest.isVirtual();
    }

    public void setLabelTemplateData(LabelTemplate labelTemplate) {
        this.labelTemplateId = labelTemplate.getUuid();
        this.labelTemplateName = labelTemplate.getName();
    }
}
