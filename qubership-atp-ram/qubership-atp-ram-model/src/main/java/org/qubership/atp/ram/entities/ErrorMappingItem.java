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

package org.qubership.atp.ram.entities;

import java.util.UUID;

import org.qubership.atp.ram.models.LogRecord;

import lombok.Data;

@Data
public class ErrorMappingItem {

    private UUID id;
    private String level;
    private UUID parent;

    public ErrorMappingItem() {
    }

    /**
     * Generate error mapping item.
     *
     * @param logRecord data for generation
     */
    public ErrorMappingItem(LogRecord logRecord) {
        id = logRecord.getUuid();
        level = logRecord.getTestingStatus().name();
        parent = logRecord.getTestRunId();
    }

}
