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

import static java.util.Objects.nonNull;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.ram.enums.MonitoringSystem;
import org.qubership.atp.ram.enums.SystemStatus;
import org.qubership.atp.ram.models.SystemInfo;

public class SystemInfoAdapter {
    private SystemInfo systemInfo;

    public SystemInfoAdapter(SystemInfo systemInfo) {
        this.systemInfo = systemInfo;
    }

    /**
     * Get status.
     *
     * @return status
     */
    public String getStatus() {
        final SystemStatus status = systemInfo.getStatus();
        final String techName = status.getTechName();
        return nonNull(techName) ? techName : status.name();
    }

    public String getName() {
        return systemInfo.getName();
    }

    public String getVersion() {
        return systemInfo.getVersion();
    }

    public List<String> getUrls() {
        return systemInfo.getUrls();
    }

    public String getStatusColor() {
        return SystemStatusColor.of(systemInfo.getStatus()).getHtmlColor();
    }

    /**
     * Get monitoring system.
     */
    public String getMonitoringSystem() {
        final MonitoringSystem monitoringSystem = systemInfo.getMonitoringSystem();

        return nonNull(monitoringSystem) ? monitoringSystem.getName() : StringUtils.EMPTY;
    }
}
