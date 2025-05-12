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

package org.qubership.atp.ram.models;

import java.util.UUID;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class RootCauseTreeNode extends TreeNode<RootCause> {
    private UUID projectId;
    private RootCauseType type;

    /**
     * RootCauseTreeNode constructor.
     *
     * @param rootCause root cause
     */
    public RootCauseTreeNode(RootCause rootCause) {
        this.id = rootCause.getUuid();
        this.name = rootCause.getName();
        this.parentId = rootCause.getParentId();
        this.projectId = rootCause.getProjectId();
        this.type = rootCause.getType();
        this.isDisabled = rootCause.isDisabled();
        this.isDefault = rootCause.isDefault();
        this.modifiedWhen = rootCause.getModifiedWhen();
    }
}
