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

package org.qubership.atp.ram.services;

import static java.util.Objects.isNull;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import org.qubership.atp.auth.springbootstarter.exceptions.AtpIllegalNullableArgumentException;
import org.qubership.atp.ram.dto.response.PaginationResponse;
import org.qubership.atp.ram.models.BrowserConsoleLog;
import org.qubership.atp.ram.models.BrowserConsoleLogsTable;
import org.qubership.atp.ram.repositories.BrowserConsoleLogRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BrowserConsoleLogService {

    private final BrowserConsoleLogRepository repository;

    /**
     * Get BrowserConsoleLogs by logRecordUUID.
     *
     * @param logRecordId LogRecordUUID
     * @param pageable      Pageable (page starts from 0)
     * @return list of logs on page as entities and totalCount
     */
    public PaginationResponse<BrowserConsoleLogsTable> getBrowserConsoleLogsTable(UUID logRecordId, Pageable pageable) {
        if (isNull(logRecordId)) {
            log.error("Found illegal nullable log record id");
            throw new AtpIllegalNullableArgumentException("log record id", "method parameter");
        }
        return repository.findBrowserConsoleLogsByLogRecordIdWithPagination(logRecordId, pageable);
    }

    /**
     * Create browserConsoleLog for LogRecord.
     *
     * @param logRecordId logRecordId
     * @param logs logs to save
     */
    public void createBrowserConsoleLog(UUID logRecordId, List<BrowserConsoleLogsTable> logs) {
        if (isNull(logRecordId)) {
            log.error("Found illegal nullable log record id");
            throw new AtpIllegalNullableArgumentException("log record id", "method parameter");
        }
        log.info("Create browser console logs for log record '{}'.", logRecordId);
        BrowserConsoleLog browserConsoleLog = repository.findBrowserConsoleLogByLogRecordId(logRecordId)
                .orElse(new BrowserConsoleLog(UUID.randomUUID(), logRecordId,
                        new Timestamp(System.currentTimeMillis()), null));
        browserConsoleLog.setBrowserConsoleLogsTable(logs);
        log.debug("Create browser console logs for log record '{}' with logs [{}]",
                logRecordId, logs);
        repository.save(browserConsoleLog);
    }

    public boolean isBrowserConsoleLogsPresent(UUID logRecordUuid) {
        return repository.existsByLogRecordId(logRecordUuid);
    }
}
