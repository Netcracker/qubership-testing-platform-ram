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

import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Document("validationLabelConfigTemplates")
@Data
@EqualsAndHashCode(callSuper = true)
public class ValidationLabelConfigTemplate extends RamObject {
    private UUID projectId;
    private TreeSet<LabelConfig> labels = new TreeSet<>();
    private boolean useTcCount;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LabelConfig implements Comparable<LabelConfig> {
        private Set<String> labelNames;
        private String columnName;
        private boolean displayed;
        private boolean displayErAr;
        private Integer order;

        @Override
        public int compareTo(LabelConfig config) {
            return this.order - config.order;
        }

        public String resolveColumnName() {
            return String.join(",", labelNames);
        }
    }
}
