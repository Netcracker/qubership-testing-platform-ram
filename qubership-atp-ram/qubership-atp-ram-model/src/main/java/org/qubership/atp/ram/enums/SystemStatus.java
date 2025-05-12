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

import static java.util.Arrays.asList;

import java.util.List;

import lombok.Getter;

public enum SystemStatus {

    NOTHING(MonitoringSystem.HEALTHCHECK),
    PASS(MonitoringSystem.HEALTHCHECK),
    WARN(MonitoringSystem.HEALTHCHECK),
    FAIL(MonitoringSystem.HEALTHCHECK),
    GREEN("GOOD", MonitoringSystem.SSM),
    RED("ERROR", MonitoringSystem.SSM),
    AMBER("WARNING", MonitoringSystem.SSM),
    UNKNOWN(MonitoringSystem.SSM);

    private List<MonitoringSystem> monitoringSystems;

    @Getter
    private String techName;

    SystemStatus(MonitoringSystem... monitoringSystems) {
        this.monitoringSystems = asList(monitoringSystems);
    }

    SystemStatus(String techName, MonitoringSystem... monitoringSystems) {
        this.techName = techName;
        this.monitoringSystems = asList(monitoringSystems);
    }

    /**
     * Find {@link TestingStatuses} by name contains word.
     */
    public static SystemStatus findByValue(String strValue) {
        for (SystemStatus v : values()) {
            if (v.name().equalsIgnoreCase(strValue)) {
                return v;
            }
        }
        return null;
    }
}
