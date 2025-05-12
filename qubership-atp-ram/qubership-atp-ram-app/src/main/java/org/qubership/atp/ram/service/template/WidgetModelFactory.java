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

package org.qubership.atp.ram.service.template;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.qubership.atp.ram.models.WidgetType;
import org.qubership.atp.ram.service.mail.ReportParams;
import org.springframework.data.util.Optionals;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class WidgetModelFactory {

    List<WidgetModelBuilder> modelBuilders;

    public WidgetModelFactory(List<WidgetModelBuilder> modelBuilders) {
        this.modelBuilders = modelBuilders;
    }

    /**
     * Returns a concrete implementation of {@link WidgetModelBuilder} depending on {@link WidgetType}.
     * @param widgetType type of UI Widget Template to be populeted with data.
     * @return {@link WidgetModelBuilder}.
     */
    public Optional<WidgetModelBuilder> getModelBuilder(WidgetType widgetType) {
        Optional<WidgetModelBuilder> matchedBuilder = modelBuilders.stream()
                .filter(builder -> widgetType == builder.getType())
                .findFirst();

        log.debug(String.format("Model builder %s found for widget %s",
                matchedBuilder,
                widgetType));
        return matchedBuilder;
    }

    /**
     * Generates data model map for template engine.
     * @param reportParams Params used as a source data for model generation.
     * @param widgets collection of UI widget templates to be populated.
     * @return map of populated data to be used by template engine, where key = {@link WidgetType}
          and value = collection of key-value entries to be used in template.
     */
    public Map<WidgetType, Map<String, Object>> generateModel(ReportParams reportParams,
                                                              Collection<WidgetType> widgets) {
        Map<WidgetType, Map<String, Object>> model = new HashMap<>();
        widgets.forEach(widget -> {
            Optional<WidgetModelBuilder> modelBuilder = getModelBuilder(widget);

            Optionals.ifPresentOrElse(modelBuilder,
                    builder -> model.put(widget, getModel(builder, reportParams)),
                    () ->  log.info("Skipping unknown Widget type =" + widget));
        });
        return model;
    }

    private Map<String, Object> getModel(WidgetModelBuilder builder, ReportParams reportParams) {
        try {
            return builder.getModel(reportParams);
        } catch (Exception e) {
            log.error("Error occured while collecting data for widget=" + builder.getType(), e);
        }
        return new HashMap<>();
    }
}
