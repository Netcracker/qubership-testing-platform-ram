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

import static java.util.Objects.nonNull;

import java.util.List;
import java.util.UUID;

import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.TestRun;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LogRecordRegexSearchResponse {

    private List<LogRecordResponse> logRecords;
    private int total;

    @Data
    public static class LogRecordResponse {
        private UUID logRecordId;
        private String logRecordName;
        private String logRecordMessage;
        private UUID projectId;
        private UUID testPlanId;
        private UUID testCaseId;
        private UUID scenarioId;
        private UUID testRunId;
        private String testCaseName;

        /**
         * LogRecordResponse constructor.
         */
        public LogRecordResponse(TestRun testRun, LogRecord logRecord, TestCaseLabelResponse testCase) {
            if (nonNull(logRecord)) {
                this.logRecordId = logRecord.getUuid();
                this.logRecordName = logRecord.getName();
                this.logRecordMessage = logRecord.getMessage();
            }
            if (nonNull(testRun)) {
                this.testRunId = testRun.getUuid();
                this.testCaseName = testRun.getTestCaseName();
                this.testCaseId = testRun.getTestCaseId();
            }
            if (nonNull(testCase)) {
                this.projectId = testCase.getProjectId();
                this.testPlanId = testCase.getTestPlanId();
                this.scenarioId = testCase.getScenarioId();
            }
        }
    }
}
