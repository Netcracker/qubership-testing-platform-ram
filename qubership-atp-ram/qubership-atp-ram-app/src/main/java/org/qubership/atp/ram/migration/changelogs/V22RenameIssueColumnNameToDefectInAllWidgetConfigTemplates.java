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

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.qubership.atp.ram.migration.mongoevolution.java.annotation.ChangeLog;
import org.qubership.atp.ram.migration.mongoevolution.java.annotation.ChangeSet;
import org.qubership.atp.ram.models.WidgetConfigTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;

import lombok.extern.slf4j.Slf4j;

@ChangeLog(version = 22)
@Slf4j
public class V22RenameIssueColumnNameToDefectInAllWidgetConfigTemplates {

    private static final String WIDGET_CONFIG_TEMPLATE_COLLECTION_NAME = "widgetConfigTemplates";
    private static final String OLD_COLUMN_NAME_TO_CHANGE = "ISSUE";
    private static final String NEW_COLUMN_NAME = "DEFECT";

    /**
     * Rename test cases widget's column "ISSUES" to "DEFECT" for all widget config templates.
     *
     * @param mongoTemplate mongo template
     */
    @ChangeSet(order = 1)
    public void renameIssueColumnNameToDefectInAllWidgetConfigTemplate(MongoTemplate mongoTemplate) {
        String processName = V22RenameIssueColumnNameToDefectInAllWidgetConfigTemplates.class.getName();
        log.info("Start mongo evolution process: {}", processName);
        List<WidgetConfigTemplate> widgetConfigTemplates = mongoTemplate.findAll(
                WidgetConfigTemplate.class, WIDGET_CONFIG_TEMPLATE_COLLECTION_NAME);
        if (CollectionUtils.isEmpty(widgetConfigTemplates)) {
            log.info("Mongo evolution process: {}. There are no widget templates.", processName);
        } else {
            widgetConfigTemplates.forEach(widgetConfigTemplate -> {
                widgetConfigTemplate.getWidgets().forEach(widgetConfig -> {
                    if (CollectionUtils.isNotEmpty(widgetConfig.getColumnVisibilities())) {
                        widgetConfig.getColumnVisibilities().forEach(columnVisibility -> {
                            if (OLD_COLUMN_NAME_TO_CHANGE.equals(columnVisibility.getName())) {
                                columnVisibility.setName(NEW_COLUMN_NAME);
                            }
                        });
                    }
                });
                mongoTemplate.save(widgetConfigTemplate);
            });
            log.info("Mongo evolution process: {}. Widget config templates were updated. "
                    + "Changed column 'ISSUE' to 'DEFECT'", processName);
        }
    }
}
