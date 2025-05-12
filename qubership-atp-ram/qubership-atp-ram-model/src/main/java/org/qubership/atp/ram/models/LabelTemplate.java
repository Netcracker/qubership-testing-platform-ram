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

import static java.util.Objects.isNull;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.Gson;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LabelTemplate implements Serializable {

    public static final String UNKNOWN = "Unknown";

    private UUID uuid;
    private String name;
    private UUID projectId;
    private List<LabelTemplateNode> labelNodes;

    @JsonIgnore
    private LabelTemplateNode unknownNode;


    /**
     * Create Unknown node in template.
     *
     * @return template
     */
    public LabelTemplate createUnknownNode() {
        if (isNull(unknownNode)) {
            if (isEmpty(labelNodes)) {
                this.labelNodes = new ArrayList<>();
            }

            this.unknownNode = new LabelTemplateNode(UNKNOWN);
            this.labelNodes.add(unknownNode);
        }

        return this;
    }

    public void addToUnknown(UUID testRunId, String error) {
        this.unknownNode.addTestRun(testRunId);
        this.unknownNode.errors.put(testRunId, error);
    }

    /**
     * Deep clone entity instance.
     *
     * @return cloned instance
     */
    public LabelTemplate clone() {
        Gson gson = new Gson();

        return gson.fromJson(gson.toJson(this), LabelTemplate.class);
    }

    @Data
    @NoArgsConstructor
    public static class LabelTemplateNode implements Serializable {
        private UUID labelId;
        private String labelName;
        private List<LabelTemplateNode> children;
        private Set<UUID> testRunIds = new HashSet<>();
        private Map<UUID, String> errors = new HashMap<>();
        private int passedRate;
        private int failedRate;
        private int testRunFailedCount;
        private int warningRate;
        private int testRunWarnedCount;
        private int testRunCount;
        private int testRunPassedCount;

        public LabelTemplateNode(UUID labelId, String labelName) {
            this.labelId = labelId;
            this.labelName = labelName;
        }

        public LabelTemplateNode(String labelName) {
            this.labelName = labelName;
        }

        public void addTestRun(UUID id) {
            this.testRunIds.add(id);
        }
    }
}
