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

import org.qubership.atp.ram.migration.mongoevolution.java.annotation.ChangeLog;
import org.qubership.atp.ram.migration.mongoevolution.java.annotation.ChangeSet;
import org.qubership.atp.ram.models.ColumnVisibility;
import org.qubership.atp.ram.models.WidgetConfigTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import lombok.extern.slf4j.Slf4j;

@ChangeLog(version = 10)
@Slf4j
public class SetDefaultLabelVisibilityOnWidgetsV10 {

    private static final String LABELS_COLUMN_NAME = "LABELS";
    private static final int VISIBILITIES_WIDGET_POSITION = 3;

    /**
     * Set default label visibility on widgets.
     *
     * @param mongoTemplate mongo template
     */
    @ChangeSet(order = 1)
    public void run(MongoTemplate mongoTemplate) {
        String processName = SetDefaultLabelVisibilityOnWidgetsV10.class.getName();
        log.info("Start mongo evolution process: {}", processName);

        Query query = Query.query(Criteria.where("widgets.columnVisibilities.name").nin(LABELS_COLUMN_NAME));

        ColumnVisibility labelColumn = new ColumnVisibility();
        labelColumn.setName(LABELS_COLUMN_NAME);
        labelColumn.setVisible(false);
        String updateKey = String.format("widgets.%d.columnVisibilities", VISIBILITIES_WIDGET_POSITION);
        Update update = new Update().push(updateKey, labelColumn);

        mongoTemplate.updateMulti(query, update, WidgetConfigTemplate.class);

        log.info("End mongo evolution process: {}", processName);
    }
}
