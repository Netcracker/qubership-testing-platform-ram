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

package org.qubership.atp.ram.models;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.qubership.atp.ram.enums.TestingStatuses;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PotsStatisticsPerTestCase {
    private TestingStatuses testingStatus;
    private String name;
    private List<PotsStatisticsPerAction> children;
    private UUID id;

    /**
     * Constructor. Set children as empty list.
     *
     * @param testingStatus testing status of TR
     * @param name name of TR
     * @param id ID of TR
     */
    public PotsStatisticsPerTestCase(TestingStatuses testingStatus, String name, UUID id) {
        this.testingStatus = testingStatus;
        this.name = name;
        this.id = id;
        this.children = new ArrayList<>();
    }
}
