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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.qubership.atp.ram.enums.TestingStatuses;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Document(collection = "widgetConfigTemplates")
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class WidgetConfigTemplate extends RamObject {

    @Indexed(background = true)
    private UUID projectId;
    private List<WidgetConfig> widgets = new ArrayList<>();

    /**
     * Get widget config by id.
     *
     * @param widgetId widget id
     * @return widget config
     */
    public WidgetConfig getWidgetConfig(UUID widgetId) {
        return widgets.stream()
                .filter(widgetConfig -> widgetConfig.getWidgetId().equals(widgetId))
                .findFirst()
                .orElse(new WidgetConfig(widgetId));
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class WidgetConfig {
        private UUID widgetId;
        private UUID labelTemplateId;
        private UUID validationTemplateId;
        private List<ColumnVisibility> columnVisibilities;
        private boolean isExecutionRequestsSummary;
        private Integer sizeLimit;
        private Filters filters;

        public WidgetConfig(UUID widgetId) {
            this.widgetId = widgetId;
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Filters {
        private Set<TestingStatuses> testingStatuses;
        private Set<TestingStatuses> firstStatuses;
        private Set<TestingStatuses> finalStatuses;
        private Set<UUID> failureReasons;
    }
}
