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
import java.util.Objects;
import java.util.UUID;

import org.qubership.atp.ram.migration.mongoevolution.java.annotation.ChangeLog;
import org.qubership.atp.ram.migration.mongoevolution.java.annotation.ChangeSet;
import org.qubership.atp.ram.models.ColumnVisibility;
import org.qubership.atp.ram.models.WidgetConfigTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;

import lombok.extern.slf4j.Slf4j;

@ChangeLog(version = 12)
@Slf4j
public class CreateDefaultMailTemplateV12 {

    private static final String DEFAULT_REPORT_UUID = "578a9810-8e8d-49ec-ba9a-6b26a22d5d70";
    private static final String WIDGET_CONFIG_TEMPLATE_COLLECTION_NAME = "widgetConfigTemplates";

    /**
     * Generate default widget config template if not exists.
     * @param mongoTemplate mongo template
     */
    @ChangeSet(order = 1)
    public void addDefaultWidgetConfigTemplate(MongoTemplate mongoTemplate) {
        String processName = CreateDefaultMailTemplateV12.class.getName();
        log.info("Start mongo evolution process: {}", processName);
        WidgetConfigTemplate savedDefaultTemplate = mongoTemplate.findById(UUID.fromString(DEFAULT_REPORT_UUID),
                WidgetConfigTemplate.class, WIDGET_CONFIG_TEMPLATE_COLLECTION_NAME);
        if (Objects.nonNull(savedDefaultTemplate)) {
            log.info("Mongo evolution process: {}. Default mail template with id:{} already exists in db",
                    processName, DEFAULT_REPORT_UUID);
        } else {
            mongoTemplate.save(generateDefaultWidgetConfigTemplate());
            log.info("Mongo evolution process: {}. Default mail template with id:{} was created in db.",
                    processName, DEFAULT_REPORT_UUID);
        }
    }

    private WidgetConfigTemplate generateDefaultWidgetConfigTemplate() {
        WidgetConfigTemplate defaultTemplate = new WidgetConfigTemplate();
        defaultTemplate.setUuid(UUID.fromString(DEFAULT_REPORT_UUID));
        defaultTemplate.setName("System Default Widget Template");
        defaultTemplate.setWidgets(Arrays.asList(
                getDefaultExecutionSummaryConfig(),
                getDefaultServerSummaryConfig(),
                getDefaultTopIssuesConfig(),
                getDefaultTestCasesConfig(),
                getDefaultRootCausesStatisticConfig()
        ));
        return defaultTemplate;
    }

    private WidgetConfigTemplate.WidgetConfig getDefaultExecutionSummaryConfig() {
        WidgetConfigTemplate.WidgetConfig wc = new WidgetConfigTemplate.WidgetConfig();
        wc.setWidgetId(UUID.fromString("fcf40bf3-2610-461d-8d0a-3d05299ff396"));
        return wc;
    }

    private WidgetConfigTemplate.WidgetConfig getDefaultServerSummaryConfig() {
        WidgetConfigTemplate.WidgetConfig wc = new WidgetConfigTemplate.WidgetConfig();
        wc.setWidgetId(UUID.fromString("8de42425-9578-41c4-8225-f8fe46a572d2"));
        return wc;
    }

    private WidgetConfigTemplate.WidgetConfig getDefaultTopIssuesConfig() {
        WidgetConfigTemplate.WidgetConfig wc = new WidgetConfigTemplate.WidgetConfig();
        wc.setWidgetId(UUID.fromString("15a0119b-e7b0-41bb-9c26-f2729ed5a30b"));
        return wc;
    }

    private WidgetConfigTemplate.WidgetConfig getDefaultTestCasesConfig() {
        WidgetConfigTemplate.WidgetConfig wc = new WidgetConfigTemplate.WidgetConfig();
        wc.setWidgetId(UUID.fromString("df1571e8-ad62-45b3-ba30-1055330a4fb7"));
        wc.setColumnVisibilities(getDefaultTestCaseColumnVisibilities());
        return wc;
    }

    private WidgetConfigTemplate.WidgetConfig getDefaultRootCausesStatisticConfig() {
        WidgetConfigTemplate.WidgetConfig wc = new WidgetConfigTemplate.WidgetConfig();
        wc.setWidgetId(UUID.fromString("3896a389-cc01-46b1-8f5c-8ab963c979ad"));
        wc.setSizeLimit(5);
        return wc;
    }

    private List<ColumnVisibility> getDefaultTestCaseColumnVisibilities() {
        return Arrays.asList(
                new ColumnVisibility("NAME", true),
                new ColumnVisibility("STATUS", true),
                new ColumnVisibility("PASSED_RATE", false),
                new ColumnVisibility("JIRA_TICKET", true),
                new ColumnVisibility("DURATION", true),
                new ColumnVisibility("FAILURE_REASON", true),
                new ColumnVisibility("FAILED_STEP", true),
                new ColumnVisibility("DATA_SET", true),
                new ColumnVisibility("COMMENT", true),
                new ColumnVisibility("ISSUE", true),
                new ColumnVisibility("LABELS", false)
        );
    }
}
