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

package org.qubership.atp.ram.job;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.qubership.atp.common.lock.LockManager;
import org.qubership.atp.common.lock.provider.InMemoryLockProvider;
import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.services.TestRunService;

public class TaskSchedulerTest {

    private TaskScheduler taskScheduler;
    private TestRunService testRunService;
    private LockManager lockManager;

    @BeforeEach
    public void setUp() {
        lockManager = new LockManager(10, 10, 10, new InMemoryLockProvider());
        testRunService = Mockito.mock(TestRunService.class);
        taskScheduler = new TaskScheduler(testRunService, lockManager);
    }

    @Test
    public void scheduleCheckInProgressTestRunsTask_WhenTrHasFinishDate_TrWasStoppedAndTrFinishDateWasUsed() {
        TestRun testRun = new TestRun();
        testRun.setUuid(UUID.randomUUID());
        testRun.setStartDate(new Timestamp(System.currentTimeMillis() - 400000));
        testRun.setFinishDate(new Timestamp(System.currentTimeMillis() - 300000));
        Mockito.when(testRunService.getAllInProgressTestRuns()).thenReturn(Collections.singletonList(testRun));

        taskScheduler.scheduleCheckInProgressTestRunsTask();

        Mockito.verify(testRunService).updateStatusesAndFinishDateTestRuns(eq(testRun.getUuid()),
                eq(ExecutionStatuses.TERMINATED_BY_TIMEOUT), eq(TestingStatuses.STOPPED), any(), anyLong());
    }

    @Test
    public void scheduleCheckInProgressTestRunsTask_WhenTrHasNotFinishDate_TrWasStoppedAndLrFinishDateWasUsed() {
        TestRun testRun = new TestRun();
        testRun.setUuid(UUID.randomUUID());
        testRun.setStartDate(new Timestamp(System.currentTimeMillis() - 400000));
        LogRecord logRecord = new LogRecord();
        logRecord.setUuid(UUID.randomUUID());
        logRecord.setEndDate(new Timestamp(System.currentTimeMillis() - 300000));
        Mockito.when(testRunService.getAllInProgressTestRuns()).thenReturn(Collections.singletonList(testRun));
        Mockito.when(testRunService.getLastInProgressOrcLogRecord(any())).thenReturn(logRecord);

        taskScheduler.scheduleCheckInProgressTestRunsTask();

        Mockito.verify(testRunService).getLastInProgressOrcLogRecord(eq(testRun.getUuid()));
        Mockito.verify(testRunService).updateStatusesAndFinishDateTestRuns(eq(testRun.getUuid()),
                eq(ExecutionStatuses.TERMINATED_BY_TIMEOUT), eq(TestingStatuses.STOPPED), any(), anyLong());
    }
}
