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

package org.qubership.atp.ram;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.qubership.atp.ram.enums.SystemStatus;
import org.qubership.atp.ram.models.EnvironmentsInfo;
import org.qubership.atp.ram.models.SystemInfo;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EnvironmentsInfoMock {
    public SystemInfo generateSystemInfoWithStatus(SystemStatus systemStatus) {
        SystemInfo systemInfo = new SystemInfo();
        systemInfo.setStatus(systemStatus);
        return systemInfo;
    }

    public EnvironmentsInfo generateEnvInfo() {
        List<SystemInfo> qaLists = Arrays.asList(generateQaSystemInfo("qa1"), generateQaSystemInfo("qa2"));
        List<SystemInfo> taLists = Arrays.asList(generateQaSystemInfo("ta1"), generateQaSystemInfo("ta2"));

        EnvironmentsInfo environmentsInfo = new EnvironmentsInfo();
        environmentsInfo.setQaSystemInfoList(qaLists);
        environmentsInfo.setTaSystemInfoList(taLists);
        return environmentsInfo;
    }

    public EnvironmentsInfo generateEnvInfoByUuids(UUID executionRequestUuid, UUID envUuid) {
        List<SystemInfo> qaLists = Arrays.asList(generateQaSystemInfo("qa1"), generateQaSystemInfo("qa2")
        , generateQaSystemInfoWithoutVersion("qa3"));
        List<SystemInfo> taLists = Arrays.asList(generateQaSystemInfo("ta1"), generateQaSystemInfo("ta2"));

        EnvironmentsInfo environmentsInfo = new EnvironmentsInfo();
        environmentsInfo.setQaSystemInfoList(qaLists);
        environmentsInfo.setTaSystemInfoList(taLists);
        environmentsInfo.setExecutionRequestId(executionRequestUuid);
        environmentsInfo.setEnvironmentId(envUuid);
        return environmentsInfo;
    }

    private static SystemInfo generateQaSystemInfo(String prefix) {
        SystemInfo systemInfo = new SystemInfo();
        systemInfo.setName(prefix);
        systemInfo.setUrls(Arrays.asList(prefix + ".some-domain.com", prefix + ".dev.some-domain.com"));
        systemInfo.setVersion(prefix + "Build");
        return systemInfo;
    }
    private static SystemInfo generateQaSystemInfoWithoutVersion(String prefix) {
        SystemInfo systemInfo = new SystemInfo();
        systemInfo.setName(prefix);
        systemInfo.setUrls(Arrays.asList(prefix + ".some-domain.com", prefix + ".dev.some-domain.com"));
        systemInfo.setVersion("");
        return systemInfo;
    }
}
