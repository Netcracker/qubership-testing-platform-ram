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

package org.qubership.atp.ram.service.template.impl.generictable;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Row {
    private List<Column> columns = new ArrayList<>();
    private List<Row> children = new ArrayList<>();
    private boolean isEven;

    public Row(List<Row> children) {
        this.children = children;
    }

    public void addColumns(Column... columns) {
        this.columns.addAll(asList(columns));
    }

    /**
     * Add column.
     *
     * @param column column name
     * @param isVisible column visibility
     */
    public void addColumn(Column column, boolean isVisible) {
        if (isVisible) {
            this.columns.add(column);
        }
    }
}
