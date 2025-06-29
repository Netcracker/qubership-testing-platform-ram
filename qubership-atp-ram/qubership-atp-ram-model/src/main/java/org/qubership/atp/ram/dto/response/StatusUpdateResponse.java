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

import static java.util.Collections.emptyList;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.TestRun;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
public class StatusUpdateResponse {

    private Date lastLoaded;
    private List<TestRunStatusUpdateResponse> testRuns;

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class TestRunStatusUpdateResponse extends BaseStatusUpdateResponse {
        private List<BaseStatusUpdateResponse> logRecords;

        /**
         * TestRunStatusUpdateResponse constructor.
         *
         * @param testRun test run
         * @param logRecords test run log records
         */
        public TestRunStatusUpdateResponse(TestRun testRun,
                                           List<LogRecord> logRecords) {
            super(testRun.getUuid(), testRun.getExecutionStatus(), testRun.getTestingStatus());
            this.logRecords = !isEmpty(logRecords) ? logRecords.stream()
                    .map(logRecord -> new BaseStatusUpdateResponse(
                            logRecord.getUuid(),
                            logRecord.getExecutionStatus(),
                            logRecord.getTestingStatus()))
                    .collect(Collectors.toList()) : emptyList();
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BaseStatusUpdateResponse {
        protected UUID id;
        protected ExecutionStatuses executionStatus;
        protected TestingStatuses testingStatus;
    }
}
