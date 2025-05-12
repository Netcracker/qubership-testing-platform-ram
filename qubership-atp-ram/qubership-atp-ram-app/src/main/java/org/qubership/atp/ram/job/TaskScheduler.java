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

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.qubership.atp.common.lock.LockManager;
import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.services.TestRunService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration("ramTaskScheduler")
@EnableScheduling
public class TaskScheduler {
    private static final Logger LOG = LoggerFactory.getLogger(TaskScheduler.class);
    private final TestRunService testRunService;
    private final LockManager lockManager;
    @Value("${terminate.timeout.in.minutes}")
    private int terminateTimeoutInMinutes;

    @Autowired
    public TaskScheduler(TestRunService service, LockManager lockManager) {
        this.testRunService = service;
        this.lockManager = lockManager;
    }

    /**
     * Schedule checking TestRuns that have finishDate more than 30 minutes ago and terminate them.
     */

    @Scheduled(fixedRateString = "${fixedRate.tr.in.milliseconds}")
    public void scheduleCheckInProgressTestRunsTask() {
        lockManager.executeWithLock("scheduleCheckInProgressTestRunsTask", this::checkInProgressTestRunsTask);
    }

    private void checkInProgressTestRunsTask() {
        LOG.debug("Start checking In Progress Test Runs.");
        List<TestRun> testRuns = testRunService.getAllInProgressTestRuns();
        for (TestRun testRun : testRuns) {
            long lastActiveTime;
            if (Objects.isNull(testRun.getFinishDate())) {
                LogRecord lastLogRecord = testRunService.getLastInProgressOrcLogRecord(testRun.getUuid());
                if (Objects.isNull(lastLogRecord)) {
                    lastActiveTime = testRun.getStartDate().getTime();
                    LOG.debug("Get start date for Tes Run {}", testRun.getUuid());
                } else {
                    lastActiveTime = Objects.isNull(lastLogRecord.getEndDate())
                            ? lastLogRecord.getStartDate().getTime() : lastLogRecord.getEndDate().getTime();
                    LOG.debug("Get date from Log Record {} for Tes Run {}", lastLogRecord.getUuid(), testRun.getUuid());
                }
            } else {
                lastActiveTime = testRun.getFinishDate().getTime();
            }
            long testRunDownTime = System.currentTimeMillis() - lastActiveTime;
            long testRunDownTimeInMinutes = TimeUnit.MILLISECONDS.toMinutes(testRunDownTime);
            if (testRunDownTimeInMinutes >= terminateTimeoutInMinutes) {
                LOG.info("[TR {}] The timeout ({}) expired. TR will be terminated",
                        testRun.getUuid(), terminateTimeoutInMinutes);
                Timestamp finishDate = new Timestamp(System.currentTimeMillis());
                long testRunDuration = TimeUnit.MILLISECONDS.toSeconds(
                        finishDate.getTime() - testRun.getStartDate().getTime());
                testRunService.updateStatusesAndFinishDateTestRuns(testRun.getUuid(),
                        ExecutionStatuses.TERMINATED_BY_TIMEOUT, TestingStatuses.STOPPED, finishDate, testRunDuration);

                LOG.info("[TR {}] The status set to {}/{}}",
                        testRun.getUuid(), ExecutionStatuses.TERMINATED_BY_TIMEOUT, TestingStatuses.STOPPED);
            } else {
                LOG.debug("The timeout hasn't come for TR = {} ", testRun.getUuid());
            }
        }
        LOG.debug("Finish checking In Progress Test Runs.");
    }
}
