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

package org.qubership.atp.ram.model;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.qubership.atp.ram.enums.TestingStatuses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LogRecordCompareScreenshotResponse {

    private String name;
    private String type;
    private List<LogRecordCompareScreenshotResponse> child = new LinkedList<>();

    private String subStepName;
    private List<SubStepCompareScreenshotResponse> row;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SubStepCompareScreenshotResponse {
        private UUID id;
        private TestingStatuses testingStatus;
        private UUID testPlanId;
        private String testPlanName;
        private String screenshot;
    }
}
