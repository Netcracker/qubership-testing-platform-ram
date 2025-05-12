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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.atp.ram.models.ScriptConsoleLog;
import org.qubership.atp.ram.models.ScriptReport;
import org.qubership.atp.ram.repositories.ScriptReportRepository;

public class ScriptReportServiceTest {

    private ScriptReportRepository repository;
    private ScriptReportService scriptReportService;


    @BeforeEach
    public void setUp() throws Exception {
        repository = mock(ScriptReportRepository.class);
        scriptReportService = new ScriptReportService(repository);
    }

    @Test
    public void test_getPreScript_gotPreScriptStr() {
        UUID logRecordId = UUID.randomUUID();
        String expectedScript = "preScript";
        ScriptReport scriptReport = new ScriptReport();
        scriptReport.setPreScript(expectedScript);
        when(repository.findPreScriptByLogRecordId(eq(logRecordId))).thenReturn(scriptReport);
        assertEquals(expectedScript, scriptReportService.getPreScript(logRecordId));
    }

    @Test
    public void test_getPostScript_gotPostScriptStr() {
        UUID logRecordId = UUID.randomUUID();
        String expectedScript = "postScript";
        ScriptReport scriptReport = new ScriptReport();
        scriptReport.setPostScript(expectedScript);
        when(repository.findPostScriptByLogRecordId(eq(logRecordId))).thenReturn(scriptReport);
        assertEquals(expectedScript, scriptReportService.getPostScript(logRecordId));
    }

    @Test
    public void test_getScriptConsoleLogs_gotListWithConsoleLogs() {
        UUID logRecordId = UUID.randomUUID();
        ScriptConsoleLog consoleLog = new ScriptConsoleLog("message", System.currentTimeMillis(), "INFO");
        ScriptReport scriptReport = new ScriptReport();
        scriptReport.setScriptConsoleLogs(Collections.singletonList(consoleLog));
        when(repository.findScriptConsoleLogsByLogRecordId(eq(logRecordId))).thenReturn(scriptReport);
        assertEquals(Collections.singletonList(consoleLog), scriptReportService.getScriptConsoleLogs(logRecordId));

    }
}
