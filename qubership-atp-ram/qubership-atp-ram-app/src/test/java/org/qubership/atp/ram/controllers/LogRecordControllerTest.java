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

package org.qubership.atp.ram.controllers;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.atp.ram.dto.response.MessageParameter;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.service.rest.server.mongo.LogRecordController;
import org.qubership.atp.ram.services.ExecutionRequestService;
import org.qubership.atp.ram.services.FileResponseEntityService;
import org.qubership.atp.ram.services.GridFsService;
import org.qubership.atp.ram.services.IssueService;
import org.qubership.atp.ram.services.LogRecordService;
import org.qubership.atp.ram.services.ScriptReportService;
import org.qubership.atp.ram.services.TestRunService;
import org.qubership.atp.ram.testdata.LogRecordServiceMock;

public class LogRecordControllerTest {
    private final UUID uuid = UUID.randomUUID();
    private final TestingStatuses testingStatuses = TestingStatuses.PASSED;
    private LogRecordController controller;

    private final LogRecord newLr = new LogRecord();

    // TODO: 3/5/2019 change test for Controllers with WebMvcTest...
    @BeforeEach
    public void setUp() throws Exception {
        LogRecordService logRecordService = mock(LogRecordService.class);
        LogRecordServiceMock logRecordServiceMock = new LogRecordServiceMock();
        controller = new LogRecordController(logRecordService,
                mock(GridFsService.class),
                mock(ExecutionRequestService.class),
                mock(TestRunService.class),
                mock(IssueService.class),
                mock(FileResponseEntityService.class),
                mock(ScriptReportService.class));

        newLr.setUuid(uuid);
        newLr.setTestingStatus(testingStatuses);
        newLr.setName("Test");

        when(logRecordService.findById(any())).thenReturn(newLr);
        when(logRecordService.save(any())).thenReturn(newLr);
        when(logRecordService.getLogRecordChildren(any())).
                thenReturn(logRecordServiceMock.getAllChildrenLogRecordsForParentLogRecord().stream());
    }

    @Test
    public void updateStatusShouldBeSuccess() {
        LogRecord updEr = controller.updTestingStatus(uuid, testingStatuses);
        Assertions.assertEquals(uuid, updEr.getUuid());
        Assertions.assertEquals(testingStatuses, updEr.getTestingStatus());
    }

    @Test
    public void notShouldReturnEmptyArrayWithParentAndChild() {
        List<LogRecord> logRecords = controller.getChildLogRecordsAndParent(UUID.randomUUID());
        Assertions.assertFalse(logRecords.isEmpty());
    }

    @Test
    public void getByUuidReturnLogRecordWithMessageParametersPresentTrueSuccess() {
        // given
        newLr.setMessageParametersPresent(true);
        // when
        LogRecord actualLogRecord = controller.getByUuid(uuid);
        // then
        Assertions.assertEquals(uuid, actualLogRecord.getUuid());
        Assertions.assertTrue(actualLogRecord.isMessageParametersPresent());
    }

    @Test
    public void getByUuidReturnLogRecordWithMessageParametersSuccess() {
        // given
        newLr.setMessageParameters(Collections.singletonList(new MessageParameter()));
        newLr.setMessageParametersPresent(false);
        // when
        LogRecord actualLogRecord = controller.getByUuid(uuid);
        // then
        Assertions.assertEquals(uuid, actualLogRecord.getUuid());
        Assertions.assertTrue(actualLogRecord.isMessageParametersPresent());
    }
}
