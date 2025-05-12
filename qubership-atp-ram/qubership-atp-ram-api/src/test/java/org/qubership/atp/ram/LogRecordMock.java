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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.qubership.atp.ram.entities.ComparisonStep;
import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.models.LogRecord;

import lombok.experimental.UtilityClass;

@UtilityClass
public class LogRecordMock {
    public List<LogRecord> findLogRecordsByTestRunId() {
        LogRecord logRecord1 = generateLogRecord(UUID.randomUUID(), true, true, null, TestingStatuses.PASSED);
        LogRecord logRecord2 = generateLogRecord(UUID.randomUUID(), true, true, logRecord1.getUuid(),
                TestingStatuses.WARNING);

        List<LogRecord> logRecords = new ArrayList<>();
        logRecords.add(logRecord1);
        logRecords.add(logRecord2);
        logRecords.add(generateLogRecord(UUID.randomUUID(), false, false, logRecord2.getUuid(), TestingStatuses.FAILED));
        logRecords.add(generateLogRecord(UUID.randomUUID(), false, false, null, TestingStatuses.PASSED));
        return logRecords;
    }

    public Set<ComparisonStep> findComparisonStepByTestRunIds() {
        return findLogRecordsByTestRunId().stream().map(lr -> {
            ComparisonStep step = new ComparisonStep();
            step.setStepName(lr.getName());
            step.setStatuses(lr.getTestingStatus());
            step.setDuration(lr.getDuration());
            return step;
        }).collect(Collectors.toSet());
    }

    private LogRecord generateLogRecord(UUID id, boolean isCompound, boolean isSection, UUID parent,
                                        TestingStatuses testingStatuses) {
        LogRecord logRecord = new LogRecord();
        logRecord.setName("Step " + id);
        logRecord.setUuid(id);
        logRecord.setCompaund(isCompound);
        logRecord.setSection(isSection);
        logRecord.setParentRecordId(parent);
        logRecord.setTestingStatus(testingStatuses);
        return logRecord;
    }

    public LogRecord generateLogRecord(String name, UUID testRunId) {
        LogRecord logRecord = new LogRecord();
        logRecord.setUuid(UUID.randomUUID());
        logRecord.setName(name);
        logRecord.setTestRunId(testRunId);
        return logRecord;
    }

    public LogRecord generateLogRecord(String name, UUID testRunId, String message) {
        final LogRecord logRecord = generateLogRecord(name, testRunId);
        logRecord.setMessage(message);

        return logRecord;
    }

    public LogRecord generateLogRecordWithParentAndTestRunId(String name, UUID testRunId, UUID parentLogRecordId) {
        LogRecord logRecord = new LogRecord();
        logRecord.setUuid(UUID.randomUUID());
        logRecord.setName(name);
        logRecord.setTestRunId(testRunId);
        logRecord.setParentRecordId(parentLogRecordId);
        return logRecord;
    }

    public LogRecord generateLogRecordWithTestRunId(UUID testRunId) {
        LogRecord logRecord = generateLogRecord(UUID.randomUUID(), false, false, null, TestingStatuses.FAILED);
        logRecord.setTestRunId(testRunId);
        return logRecord;
    }

    public LogRecord generateLogRecordWithTestRunId(UUID testRunId,
                                                    Date lastUpdated,
                                                    TestingStatuses testingStatus,
                                                    ExecutionStatuses executionStatus) {
        LogRecord logRecord = generateLogRecord(UUID.randomUUID(), false, false, null, TestingStatuses.FAILED);
        logRecord.setTestRunId(testRunId);
        logRecord.setLastUpdated(lastUpdated);
        logRecord.setTestingStatus(testingStatus);
        logRecord.setExecutionStatus(executionStatus);

        return logRecord;
    }

    public LogRecord generateLogRecordWithValidationLabels(UUID testRunId, Set<String> validationLabels) {
        LogRecord logRecord = generateLogRecord(UUID.randomUUID(), false, false, null, TestingStatuses.FAILED);
        logRecord.setTestRunId(testRunId);
        logRecord.setValidationLabels(validationLabels);
        return logRecord;
    }
}
