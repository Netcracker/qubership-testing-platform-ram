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

package org.qubership.atp.ram.logging.services;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.qubership.atp.ram.logging.entities.requests.CreatedLogRecordRequest;
import org.qubership.atp.ram.logging.services.mocks.ModelMocks;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.repositories.ExecutionRequestRepository;
import org.qubership.atp.ram.repositories.LogRecordRepository;
import org.qubership.atp.ram.repositories.TestRunRepository;
import org.qubership.atp.ram.services.GridFsService;

public class LogRecordLoggingServiceTest {
    private TestRunRepository testRunRepository;
    private GridFsService gridFsService;
    private LogRecordLoggingService logRecordLoggingService;
    private LogRecordRepository logRecordRepository;

    @BeforeEach
    public void setUp() {
        testRunRepository = mock(TestRunRepository.class);
        gridFsService = mock(GridFsService.class);
        logRecordRepository = mock(LogRecordRepository.class);
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setAmbiguityIgnored(true);
        logRecordLoggingService = spy(new LogRecordLoggingService(
                gridFsService, logRecordRepository,
                testRunRepository, mock(ExecutionRequestRepository.class),
                modelMapper));
    }

    @Test
    public void saveCountScreenshots_ShouldBeValidSaveTestRun() {
        when(logRecordRepository.findAllUuidByTestRunId(any()))
                .thenReturn(Collections.singletonList(new LogRecord()));

        int countOfScreenshot = 8;
        when(gridFsService.getCountScreen(any())).thenReturn(countOfScreenshot);

        TestRun mockTestRun = new TestRun();
        mockTestRun.setNumberOfScreens(countOfScreenshot);


        logRecordLoggingService.saveCountScreenshots(UUID.randomUUID(), Collections.singletonList(new TestRun()));

        Assertions.assertEquals(countOfScreenshot, mockTestRun.getNumberOfScreens(),
                "Count of screenshots of TR is valid");
    }

    @Test
    public void findByRequestOrCreate_WhenLogRecordIsExist_ShouldReturnExistedLogRecord() {
        LogRecord existedLogRecord = ModelMocks.generateExistedLogRecord();
        CreatedLogRecordRequest createdLogRecordRequest = ModelMocks.generatedCreatedLogRecordRequest();
        when(logRecordRepository.findByUuid(createdLogRecordRequest.getLogRecordUuid())).thenReturn(existedLogRecord);

        LogRecord actualLogRecord =
                logRecordLoggingService.findByRequestOrCreate(createdLogRecordRequest);
        Assertions.assertEquals(existedLogRecord, actualLogRecord,
                "Return existed log record");
    }

    @Test
    public void findByRequestOrCreate_WhenLogRecordIsNotExist_ShouldReturnCreatedLogRecord() {
        LogRecord expLogRecord = ModelMocks.generateExistedLogRecord();
        when(logRecordRepository.save(any())).thenReturn(expLogRecord);

        LogRecord actualLogRecord =
                logRecordLoggingService.findByRequestOrCreate(ModelMocks.generatedCreatedLogRecordRequest());
        Assertions.assertEquals(expLogRecord, actualLogRecord,
                "Return created log record");
    }
}
