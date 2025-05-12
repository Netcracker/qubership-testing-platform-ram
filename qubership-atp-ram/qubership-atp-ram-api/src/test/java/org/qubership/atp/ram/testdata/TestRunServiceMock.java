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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.TestRun;

public class TestRunServiceMock {

    public List<TestRun> findByExecutionRequestId() {
        List<TestRun> testRuns = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            testRuns.add(generateTestRun(i, TestingStatuses.PASSED));
        }

        for (int i = 0; i < 2; i++) {
            TestRun testRun = generateTestRun(i, TestingStatuses.PASSED);
            testRuns.add(testRun);
        }

        return testRuns;
    }

    public List<TestRun> findByExecutionRequestIdWithFailedTr() {
        List<TestRun> testRuns = findByExecutionRequestId();
        TestRun newTestRun = generateTestRun(3, TestingStatuses.FAILED);
        newTestRun.setRootCauseId(UUID.randomUUID());
        testRuns.add(newTestRun);
        return testRuns;
    }

    public static TestRun newTestRun() {
        TestRun testRun = new TestRun();
        testRun.setUuid(UUID.randomUUID());
        testRun.setExecutionRequestId(UUID.randomUUID());
        testRun.setTestCaseId(UUID.randomUUID());

        return testRun;
    }

    private TestRun generateTestRun(int uuid, TestingStatuses status) {
        TestRun testRun = newTestRun();
        testRun.setName("TR" + uuid);
        testRun.updateTestingStatus(status);
        testRun.setExecutionStatus(ExecutionStatuses.FINISHED);
        return testRun;
    }

    public List<LogRecord> getTopLevelLogRecords(TestingStatuses status) {
        List<LogRecord> logRecords = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            LogRecord logRecord = new LogRecord();
            logRecord.setUuid(UUID.randomUUID());
            logRecord.setName("LR" + i);
            logRecord.setTestingStatus(status);
            logRecord.setSection(true);
            logRecords.add(logRecord);
        }
        return logRecords;
    }

    public TestRun generateTestRun(String name, UUID executionRequestId,
                                   int order, Long duration, TestingStatuses testingStatus) {
        TestRun testRun = new TestRun();
        testRun.setName(name);
        testRun.setExecutionRequestId(executionRequestId);
        testRun.setOrder(order);
        testRun.setUuid(UUID.randomUUID());
        testRun.setDuration(duration);
        testRun.setTestingStatus(testingStatus);
        return testRun;
    }
}
