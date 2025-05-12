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

import java.util.List;
import java.util.UUID;

import org.qubership.atp.auth.springbootstarter.exceptions.AtpIllegalNullableArgumentException;
import org.qubership.atp.ram.models.ScriptConsoleLog;
import org.qubership.atp.ram.repositories.ScriptReportRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScriptReportService {

    private final ScriptReportRepository repository;

    /**
     * Get preScript by logRecordUUID.
     *
     * @param logRecordId LogRecordUUID
     * @return pre script body
     */
    public String getPreScript(UUID logRecordId) {
        if (isNull(logRecordId)) {
            log.error("Found illegal nullable log record id");
            throw new AtpIllegalNullableArgumentException("log record id", "method parameter");
        }
        return repository.findPreScriptByLogRecordId(logRecordId).getPreScript();
    }

    /**
     * Get preScript by logRecordUUID.
     *
     * @param logRecordId LogRecordUUID
     * @return post script body
     */
    public String getPostScript(UUID logRecordId) {
        if (isNull(logRecordId)) {
            log.error("Found illegal nullable log record id");
            throw new AtpIllegalNullableArgumentException("log record id", "method parameter");
        }
        return repository.findPostScriptByLogRecordId(logRecordId).getPostScript();
    }

    /**
     * Get preScript by logRecordUUID.
     *
     * @param logRecordId LogRecordUUID
     * @return list of ScriptConsoleLog
     */
    public List<ScriptConsoleLog> getScriptConsoleLogs(UUID logRecordId) {
        if (isNull(logRecordId)) {
            log.error("Found illegal nullable log record id");
            throw new AtpIllegalNullableArgumentException("log record id", "method parameter");
        }
        return repository.findScriptConsoleLogsByLogRecordId(logRecordId).getScriptConsoleLogs();
    }
}
