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

package org.qubership.atp.ram.logging.entities.requests;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreatedTestRunRequest {
    private List<String> qaHost = new ArrayList<>();
    private List<String> solutionBuild = new ArrayList<>();
    private List<String> taHost = new ArrayList<>();
    private String executor;
    private String testCaseName;
    private UUID testCaseId;
    private String testRunName;
    private String dataSetUrl;
    private String dataSetListUrl;
    private long createdDate;
    private long startDate;
    private long finishDate;
    private String logCollectorData;
    private UUID testRunId;
    private String executionStatus;
    private String testingStatus;
    private Set<String> urlToBrowserOrLogs = new HashSet<>();
}
