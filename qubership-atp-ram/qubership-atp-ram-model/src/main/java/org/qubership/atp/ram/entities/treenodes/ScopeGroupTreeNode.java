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

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import org.qubership.atp.ram.models.TestRun;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ScopeGroupTreeNode extends TreeNode {
    @JsonIgnore
    private Supplier<List<TestRun>> testRunsFilterFunc;

    /**
     * ScopeGroupTreeNode constructor.
     *
     * @param executionRequestId execution request id
     */
    public ScopeGroupTreeNode(UUID executionRequestId,
                              String name,
                              Supplier<List<TestRun>> testRunsFilterFunc) {
        this.id = UUID.randomUUID();
        this.executionRequestId = executionRequestId;
        this.name = name;
        this.nodeType = TreeNodeType.SCOPE_GROUP_NODE;
        this.testRunsFilterFunc = testRunsFilterFunc;
    }

    public ScopeGroupTreeNode(String name) {
        super(name);
    }
}
