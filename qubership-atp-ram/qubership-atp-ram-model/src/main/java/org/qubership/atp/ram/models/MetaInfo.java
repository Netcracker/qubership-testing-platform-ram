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

import org.qubership.atp.ram.enums.TypeAction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Meta info about source action.
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetaInfo {

    private UUID scenarioId;
    /**
     * Line number in scenario (in case of top-level action) or row number (in case if action has a type
     * {@link TypeAction#COMPOUND}).
     */
    private Integer line;
    private String scenarioHashSum;
    /**
     * id of corresponding Compound or Action.
     * */
    private UUID definitionId;
    /**
     * Flag: step is hidden.
     */
    private boolean hidden;

    private StepLinkMetaInfo editorMetaInfo;

    /**
     * Constructor.
     *
     * @param scenarioId      scenario Id
     * @param line            line
     * @param scenarioHashSum scenarioHashSum
     * @param definitionId    definitionId
     * @param hidden          hidden
     */
    public MetaInfo(UUID scenarioId, Integer line, String scenarioHashSum, UUID definitionId, boolean hidden) {
        this.scenarioId = scenarioId;
        this.line = line;
        this.scenarioHashSum = scenarioHashSum;
        this.definitionId = definitionId;
        this.hidden = hidden;
    }

}
