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

package org.qubership.atp.ram.dto.response;

import java.util.List;
import java.util.UUID;

import org.qubership.atp.ram.models.JiraComponent;
import org.qubership.atp.ram.models.Label;
import org.qubership.atp.ram.models.RamObject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TestCaseLabelResponse extends RamObject {
    private UUID projectId;
    private UUID testPlanId;
    private UUID scenarioId;
    private String jiraTicket;
    private List<Label> labels;
    private List<JiraComponent> components;

    public TestCaseLabelResponse(UUID uuid, String name, List<Label> labels) {
        super(uuid, name);
        this.labels = labels;
    }

    /**
     * TestCaseLabelResponse constructor.
     */
    public TestCaseLabelResponse(UUID uuid, String name, List<Label> labels, List<JiraComponent> components) {
        super(uuid, name);
        this.labels = labels;
        this.components = components;
    }
}

