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

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.qubership.atp.ram.migration.mongoevolution.java.annotation.ChangeLog;
import org.qubership.atp.ram.migration.mongoevolution.java.annotation.ChangeSet;
import org.qubership.atp.ram.models.ReportTemplate;
import org.qubership.atp.ram.models.Widget;
import org.qubership.atp.ram.models.WidgetType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Query;

import com.mongodb.bulk.BulkWriteResult;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@ChangeLog(version = 4)
@Slf4j
public class ReportTemplateMigrationV4 {

    /**
     * Migrate old report template due to widget structure changes..
     *
     * @param mongoTemplate mongo template
     */
    @ChangeSet(order = 1)
    public void run(MongoTemplate mongoTemplate) {
        String mongoEvoProcessName = getClass().getName();
        log.info("Start mongo evolution process: {}", mongoEvoProcessName);

        String collectionName = ReportTemplate.class.getAnnotation(Document.class).collection();

        Query query = new Query().restrict(OldReportTemplate.class);
        List<OldReportTemplate> savedTemplates = mongoTemplate
                .find(query, OldReportTemplate.class, collectionName);

        if (savedTemplates.isEmpty()) { // nothing to do
            log.info("End mongo evolution process: {}", mongoEvoProcessName);
            return;
        }

        List<ReportTemplate> newTemplates = new ArrayList<>();
        ModelMapper modelMapper = new ModelMapper();
        savedTemplates.forEach(oldReportTemplate -> {
            log.debug("Process report template: {}", oldReportTemplate);
            ReportTemplate reportTemplate = modelMapper.map(oldReportTemplate, ReportTemplate.class);
            reportTemplate.setUuid(oldReportTemplate.id);

            List<Widget> widgets = oldReportTemplate.getWidgets();
            if (isNotEmpty(widgets)) {
                List<WidgetType> widgetTypes = widgets.stream()
                        .map(Widget::getType)
                        .collect(Collectors.toList());
                reportTemplate.setWidgets(widgetTypes);
            }

            newTemplates.add(reportTemplate);
            log.debug("New contents: {}", reportTemplate);
        });

        log.debug("Remove old templates");
        mongoTemplate.remove(new Query(), ReportTemplate.class);

        BulkOperations bulkOperations =
                mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, ReportTemplate.class);
        newTemplates.forEach(bulkOperations::insert);

        log.debug("Save updated templates");
        BulkWriteResult result = bulkOperations.execute();
        if (result.getInsertedCount() != newTemplates.size()) {
            log.error("The number of saved objects differs from the number at the beginning");
        }

        log.info("End mongo evolution process: {}", mongoEvoProcessName);
    }

    @Data
    private static class OldReportTemplate {
        @Id
        UUID id;
        String name;
        List<Widget> widgets;
        UUID projectId;
        boolean active;
        String subject;
        List<String> recipients;
    }
}
