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

package org.qubership.atp.ram.dto.response;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.models.Comment;
import org.qubership.atp.ram.models.JiraTicket;
import org.qubership.atp.ram.models.Label;
import org.qubership.atp.ram.models.RamObject;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Test run's info for analyze.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnalyzedTestRunResponse {

    private List<AnalyzedTestRun> testRuns;
    private Integer totalNumberOfEntities;

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class AnalyzedTestRun extends RamObject {

        private UUID executionRequestId;

        private UUID testCaseId;

        private String testCaseName;

        private String jiraTicket;

        private String testRunJiraTicket;

        private UUID projectId;

        private UUID testPlanId;

        private UUID scenarioId;

        private TestingStatuses testingStatus;

        private List<Label> labels;

        private UUID failureReasonId;

        private Comment comment;

        private Set<JiraTicket> defects;
    }
}
