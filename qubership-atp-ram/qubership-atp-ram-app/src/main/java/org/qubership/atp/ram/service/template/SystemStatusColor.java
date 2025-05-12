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

import org.qubership.atp.ram.enums.SystemStatus;
import org.qubership.atp.ram.models.SystemInfo;

public enum SystemStatusColor {
    PASS(SystemStatus.PASS, "#00BB5B"),
    FAIL(SystemStatus.FAIL, "#FF5260"),
    WARN(SystemStatus.WARN, "#FFB02E"),
    NOTHING(SystemStatus.NOTHING,"#8F9EB4"),
    GOOD(SystemStatus.GREEN, "#00BB5B"),
    FAILED(SystemStatus.RED, "#FF5260"),
    WARNING(SystemStatus.AMBER, "#FFB02E"),
    UNKNOWN(SystemStatus.UNKNOWN, "#8F9EB4");

    private SystemStatus status;
    private String htmlColor;

    SystemStatusColor(SystemStatus status, String htmlColor) {
        this.status = status;
        this.htmlColor = htmlColor;
    }

    public String getHtmlColor() {
        return htmlColor;
    }

    /**
     * Returns instance of {@link SystemStatusColor} depending on {@link SystemStatus}.
     * @param systemStatus  status of a system in  {@link SystemInfo}.
     * @return {@link SystemStatusColor}
     */
    public static SystemStatusColor of(SystemStatus systemStatus) {
        for (SystemStatusColor color : values()) {
            if (color.status == systemStatus) {
                return color;
            }
        }
        return NOTHING;
    }
}
