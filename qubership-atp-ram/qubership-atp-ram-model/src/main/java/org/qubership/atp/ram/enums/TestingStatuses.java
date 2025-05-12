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

package org.qubership.atp.ram.enums;

import lombok.Getter;

@Getter
public enum TestingStatuses {

    STOPPED("Stopped", 5, 6),
    FAILED("Failed", 4, 3),
    WARNING("Warning", 3, 2),
    PASSED("Passed", 2, 1),
    SKIPPED("Skipped", 1, 4),
    BLOCKED("Blocked", 1, 5),
    NOT_STARTED("Not Started", 1, 7),
    UNKNOWN("Unknown", 0, 8);

    private String name;
    private int id;
    private int reportsOrder;

    TestingStatuses(String name, int id, int reportsOrder) {
        this.name = name;
        this.id = id;
        this.reportsOrder = reportsOrder;
    }

    /**
     * Find {@link TestingStatuses} by name contains word.
     */
    public static TestingStatuses findByValue(String value) {
        for (TestingStatuses status : values()) {
            if (status.toString().equalsIgnoreCase(value)) {
                return status;
            }
        }
        return null;
    }

    /**
     * Find {@link TestingStatuses} by name.
     */
    public static TestingStatuses findByName(String value) {
        for (TestingStatuses status : values()) {
            if (status.getName().equalsIgnoreCase(value)) {
                return status;
            }
        }
        return null;
    }

    public static TestingStatuses compareAndGetPriority(TestingStatuses first, TestingStatuses second) {
        return first.getId() >= second.getId() ? first : second;
    }
}
