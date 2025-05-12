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

import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.models.LogRecord;

public class LogRecordServiceMock {
    public List<LogRecord> getAllChildrenLogRecordsForParentLogRecord() {
        List<LogRecord> logRecords = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            LogRecord logRecord = new LogRecord();
            logRecord.setUuid(UUID.randomUUID());
            logRecord.setName("child ");
            logRecord.setTestingStatus(TestingStatuses.PASSED);
            logRecords.add(logRecord);
        }
        return logRecords;
    }

    public static LogRecord newLogRecord(String name) {
        LogRecord logRecord = new LogRecord();
        logRecord.setUuid(UUID.randomUUID());
        logRecord.setTestRunId(UUID.randomUUID());
        logRecord.setName(name);

        return logRecord;
    }
}
