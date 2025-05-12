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

package org.qubership.atp.ram.migration.changelogs;

import java.util.Arrays;
import java.util.List;

import org.qubership.atp.ram.enums.ExecutionRequestWidgets;
import org.qubership.atp.ram.migration.mongoevolution.java.annotation.ChangeLog;
import org.qubership.atp.ram.migration.mongoevolution.java.annotation.ChangeSet;
import org.qubership.atp.ram.models.ColumnVisibility;
import org.qubership.atp.ram.models.WidgetConfigTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;

import lombok.extern.slf4j.Slf4j;

@ChangeLog(version = 18)
@Slf4j
public class V18UpdateWidgetTestCasesTemplate {

    /**
     * Update users widget config template. Add new columns for test cases section.
     *
     * @param mongoTemplate mongo template
     */
    @ChangeSet(order = 1)
    public void updateWidgetTestCasesTemplate(MongoTemplate mongoTemplate) {
        String processName = V18UpdateWidgetTestCasesTemplate.class.getName();
        log.info("Start mongo evolution process: {}", processName);

        List<WidgetConfigTemplate> widgetConfigTemplates = mongoTemplate.findAll(WidgetConfigTemplate.class);
        widgetConfigTemplates.forEach(template -> {
            WidgetConfigTemplate.WidgetConfig widgetConfig =
                    template.getWidgetConfig(ExecutionRequestWidgets.TEST_CASES.getWidgetId());
            widgetConfig.setExecutionRequestsSummary(false);
            widgetConfig.setColumnVisibilities(getDefaultTestCaseColumnVisibilities());
            mongoTemplate.save(template);
        });

        log.info("End mongo evolution process: {}", processName);
    }

    private List<ColumnVisibility> getDefaultTestCaseColumnVisibilities() {
        return Arrays.asList(
                new ColumnVisibility("NAME", true),
                new ColumnVisibility("STATUS", true),
                new ColumnVisibility("FIRST_STATUS", false),
                new ColumnVisibility("FINAL_STATUS", false),
                new ColumnVisibility("PASSED_RATE", false),
                new ColumnVisibility("JIRA_TICKET", true),
                new ColumnVisibility("DURATION", true),
                new ColumnVisibility("FAILURE_REASON", true),
                new ColumnVisibility("FAILED_STEP", true),
                new ColumnVisibility("FINAL_RUN", false),
                new ColumnVisibility("DATA_SET", true),
                new ColumnVisibility("COMMENT", true),
                new ColumnVisibility("ISSUE", true),
                new ColumnVisibility("LABELS", false)
        );
    }
}