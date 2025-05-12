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

package org.qubership.atp.ram;

import java.util.UUID;

import org.qubership.atp.ram.enums.ExecutionRequestWidgets;
import org.qubership.atp.ram.models.WidgetConfigTemplate;

import lombok.experimental.UtilityClass;

@UtilityClass
public class WidgetConfigTemplateMock {

    public WidgetConfigTemplate generateWidgetConfigTemplate() {
        final WidgetConfigTemplate widgetConfigTemplate = new WidgetConfigTemplate();
        widgetConfigTemplate.setUuid(UUID.randomUUID());
        widgetConfigTemplate.setName("Widget Config Template");
        widgetConfigTemplate.setProjectId(UUID.randomUUID());

        return widgetConfigTemplate;
    }

    public WidgetConfigTemplate generateWidgetConfigTemplate(ExecutionRequestWidgets... widgets) {
        final WidgetConfigTemplate widgetConfigTemplate = generateWidgetConfigTemplate();

        for (ExecutionRequestWidgets widget : widgets) {
            final WidgetConfigTemplate.WidgetConfig widgetConfig = new WidgetConfigTemplate.WidgetConfig();
            widgetConfig.setWidgetId(widget.getWidgetId());

            widgetConfigTemplate.getWidgets().add(widgetConfig);
        }

        return widgetConfigTemplate;
    }
}
