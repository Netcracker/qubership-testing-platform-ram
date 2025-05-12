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

import org.qubership.atp.common.lock.LockManager;
import org.qubership.atp.ram.services.GridFsService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration("ramExpiredGridFsFilesScheduler")
@ConditionalOnProperty(value = "files.cleanup.job.enable",
        matchIfMissing = true)
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class ExpiredGridFsFilesScheduler {

    private static final String jobName = "atp-ram-gridfs-cleaner-thread";

    private final GridFsService gridFsService;
    private final LockManager lockManager;

    /**
     * Schedule checking expired files that have uploadDate more than 2 weeks ago by default.
     * Scheduler run everyday in midnight.
     */
    @Scheduled(cron = "${files.expiration.schedule.interval}")
    public void scheduleExpiredGridFsFiles() {
        log.info("Start deleting expired files.");
        lockManager.executeWithLockNoWait(jobName, () -> {
            gridFsService.deleteExpiredFiles();
            log.info("Finish deleting expired files.");
        });
    }
}
