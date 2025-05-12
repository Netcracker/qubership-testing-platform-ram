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
import org.qubership.atp.ram.service.mail.MailService;
import org.qubership.atp.ram.services.JointExecutionRequestService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class JointExecutionRequestCompleteScheduler {

    private final LockManager lockManager;
    private final JointExecutionRequestService jointExecutionRequestService;
    private final MailService mailService;

    /**
     * Schedule joint execution request completion.
     */
    @Scheduled(cron = "${jointExecutionRequests.complete.period.cron}")
    public void scheduleJointExecutionRequestCompletionWithLockManager() {
        lockManager.executeWithLock("scheduleJointExecutionRequestCompletion",
                this::scheduleJointExecutionRequestCompletion);
    }

    /**
     * Schedule joint execution request completion.
     */
    public void scheduleJointExecutionRequestCompletion() {
        log.info("Triggered joint execution request completion job");
        jointExecutionRequestService.checkAndCompleteJointExecutionRequestsByTimeout(
                mailService::sendJointExecutionRequestReport);
    }
}
