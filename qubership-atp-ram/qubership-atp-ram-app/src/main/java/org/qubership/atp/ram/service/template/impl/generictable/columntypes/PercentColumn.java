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

package org.qubership.atp.ram.service.template.impl.generictable.columntypes;

import org.qubership.atp.ram.service.template.impl.generictable.Column;

import lombok.Data;

@Data
public class PercentColumn extends Column {

    public PercentColumn(int percent) {
        this(percent, EMPTY);
    }

    public PercentColumn(int percent, String suffix) {
        this(percent, suffix, null, BOLD);
        super.setColor(determineColor(percent));
    }

    public PercentColumn(int percent, String suffix, String color) {
        this(percent, suffix, color, BOLD);
    }

    public PercentColumn(int percent, String suffix, String color, String fontWeight) {
        super(ColumnType.TEXT, percent + "%", fontWeight, color, null, null, suffix, null);
    }

    private String determineColor(int percent) {
        return percent >= 75 ? GREEN : percent < 25 ? RED : ORANGE;
    }
}
