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

package org.qubership.atp.ram.services;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JointExecutionRequestsReportDataModel {

    private EnvironmentsData environmentsData;
    private ExecutionRequestsData executionRequestsData;

    @Data
    public static class EnvironmentsData {
        private List<EnvironmentData> environments;


    }

    @Data
    public static class EnvironmentData {
        private String name;
        private String link;
        private List<QaSystemData> systems;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class QaSystemData {
        private String name;
        private String version;
        private String date;
    }

    @Data
    public static class ExecutionRequestsData {
        private List<ExecutionRequestCount> executionRequestCounts;
        private List<String> statuses;
        private Integer testCaseTotalCount;
        private List<StatusCount> totalStatusCounts;
    }

    @Data
    public static class ExecutionRequestCount {
        private String name;
        private String link;
        private Integer testCaseCount;
        private List<StatusCount> statusCounts;
        private List<TestCase> testCases;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StatusCount {
        private String status;
        private Integer count;
        private Float percent;
        private Integer total;

        /**
         * StatusCount constructor.
         */
        public StatusCount(String status, int count, float percent) {
            this.status = status;
            this.count = count;
            this.percent = percent;
        }
    }

    @Data
    public static class TestCase {
        private String name;
        private String status;
        private String duration;
        private String firstFailedStepName;
        private String failureReason;
        private String comment;
    }
}
