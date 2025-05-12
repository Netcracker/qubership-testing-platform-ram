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

package org.qubership.atp.ram.testdata;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.qubership.atp.ram.enums.SystemStatus;
import org.qubership.atp.ram.models.EnvironmentsInfo;
import org.qubership.atp.ram.models.SystemInfo;

public class EnvironmentsInfoMock {

    public static EnvironmentsInfo newEnvironmentsInfo(String name, UUID environmentId, UUID executionRequestId,
                                                       List<SystemInfo> qaSystemInfoList, List<SystemInfo> taSystemInfoList) {
        EnvironmentsInfo environmentsInfo = new EnvironmentsInfo();
        environmentsInfo.setName(name);
        environmentsInfo.setEnvironmentId(environmentId);
        environmentsInfo.setExecutionRequestId(executionRequestId);
        environmentsInfo.setQaSystemInfoList(qaSystemInfoList);
        environmentsInfo.setTaSystemInfoList(taSystemInfoList);

        return environmentsInfo;
    }

    public static SystemInfo newSystemInfo(String name, SystemStatus status, String version, String... urls) {
        SystemInfo systemInfo = new SystemInfo();
        systemInfo.setName(name);
        systemInfo.setStatus(status);
        systemInfo.setVersion(version);
        systemInfo.setUrls(Arrays.asList(urls));

        return systemInfo;
    }
}
