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

package org.qubership.atp.ram.service.template;

import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.models.SystemInfo;

public enum TestingStatusColor {
    PASSED(TestingStatuses.PASSED,"#00BB5B"),
    FAILED(TestingStatuses.FAILED, "#FF5260"),
    WARNING(TestingStatuses.WARNING, "#FFB02E"),
    STOPPED(TestingStatuses.STOPPED, "#8F9EB4"),
    SKIPPED(TestingStatuses.SKIPPED, "#8F9EB4"),
    BLOCKED(TestingStatuses.BLOCKED,"#8F9EB4"),
    UNKNOWN(TestingStatuses.UNKNOWN, "#8F9EB4"),
    NOT_STARTED(TestingStatuses.NOT_STARTED, "#8F9EB4");


    private TestingStatuses status;
    private String color;

    TestingStatusColor(TestingStatuses status, String htmlColor) {
        this.status = status;
        this.color = htmlColor;
    }

    public String getStatus() {
        return status.toString();
    }

    public String getColor() {
        return color;
    }

    /**
     * Returns instance of {@link TestingStatusColor} depending on {@link TestingStatuses}.
     * @param testingStatus  status of a system in  {@link SystemInfo}.
     * @return {@link SystemStatusColor}
     */
    public static TestingStatusColor of(TestingStatuses testingStatus) {
        for (TestingStatusColor color : values()) {
            if (color.status == testingStatus) {
                return color;
            }
        }
        return UNKNOWN;
    }
}
