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

import java.util.List;

import lombok.Data;

@Data
public class ExecutionRequestEnvironment {

    private EnvironmentsGeneralDataModel generalInfo;
    private List<EnvironmentsInfoDataModel> qaInfo;
    private List<EnvironmentsInfoDataModel> taInfo;
    private EnvironmentsToolsInfoDataModel toolsInfo;

    @Data
    public static class EnvironmentsToolsInfoDataModel {
        private String wdShellVersion;
        private String shellVersion;
        private String sessionId;
        private String sessionIdUrl;
        private String selenoidId;
        private String selenoidIdUrl;
        private String dealer;
        private String dealerUrl;
        private String tool;
        private String toolUrl;
    }

    @Data
    public static class EnvironmentsInfoDataModel {
        private String name;
        private String nameUrl;
        private String status;
        private String version;
        private String url;
    }

    @Data
    public static class EnvironmentsGeneralDataModel {
        private String status;
        private String saveUrl;
        private String startDate;
        private String endDate;
        private String duration;
    }
}
