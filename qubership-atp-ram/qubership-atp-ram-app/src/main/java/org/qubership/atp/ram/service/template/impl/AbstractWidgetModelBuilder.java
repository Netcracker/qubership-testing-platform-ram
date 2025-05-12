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

package org.qubership.atp.ram.service.template.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang.StringUtils;
import org.qubership.atp.ram.models.WidgetType;
import org.qubership.atp.ram.models.tree.TreeWalker;
import org.qubership.atp.ram.service.mail.ReportParams;
import org.qubership.atp.ram.service.template.WidgetModelBuilder;
import org.qubership.atp.ram.service.template.impl.generictable.Row;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractWidgetModelBuilder implements WidgetModelBuilder {

    @Override
    public Map<String, Object> getModel(ReportParams reportParams) {

        log.debug("Start building model for html email report, reportParams=" + reportParams);

        Map<String, Object> model = buildModel(reportParams);
        updateDescription(model, reportParams);

        log.debug(String.format("Model generated for reportParams=%s, model=%s",
                reportParams,
                model));
        return model;
    }

    private void updateDescription(Map<String, Object> model, ReportParams reportParams) {
        Map<String, String> descriptions = reportParams.getDescriptions();
        if (descriptions != null) {
            String description = descriptions.get(getType().toString());
            if (StringUtils.isNotBlank(description)) {
                model.put("description", description);
            }
        }
    }

    protected abstract Map<String, Object> buildModel(ReportParams reportParams);

    protected void markEvenAndOddRows(List<Row> rows) {
        TreeWalker<Row> rowTreeWalker = new TreeWalker<>();

        AtomicBoolean isEven = new AtomicBoolean(true);
        rowTreeWalker.walkWithPreProcess(new Row(rows), Row::getChildren, (root, row) -> {
            row.setEven(isEven.get());
            isEven.set(!isEven.get());
        });
    }

    @Override
    public abstract WidgetType getType();
}
