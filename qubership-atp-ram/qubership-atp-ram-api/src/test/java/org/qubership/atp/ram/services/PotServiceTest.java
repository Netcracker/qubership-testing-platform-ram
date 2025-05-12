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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.model.LogRecordWithParentResponse;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.PotsStatisticsPerAction;
import org.qubership.atp.ram.models.PotsStatisticsPerTestCase;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.models.logrecords.parts.FileMetadata;
import org.qubership.atp.ram.models.logrecords.parts.FileType;
import org.qubership.atp.ram.repositories.CustomLogRecordRepository;
import org.qubership.atp.ram.repositories.LogRecordRepository;
import org.qubership.atp.ram.repositories.TestRunRepository;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

@ExtendWith(SpringExtension.class)
public class PotServiceTest {

    private PotService potService;
    @Mock
    private LogRecordRepository logRecordRepository;
    @Mock
    private CustomLogRecordRepository customLogRecordRepository;

    @BeforeEach
    public void setup() {
        TestRunRepository testRunRepository = mock(TestRunRepository.class);
        GridFsService gridFsService = mock(GridFsService.class);
        ArchiveService archiveService  = mock(ArchiveService.class);
        FileNamesService fileNamesService = mock(FileNamesService.class);
        this.potService = new PotService(logRecordRepository, testRunRepository,gridFsService,
                archiveService, fileNamesService, new ObjectMapper(), customLogRecordRepository);
    }

    @Test
    public void collectStatisticForExecutionRequest() {
        String expectedName = "name1";
        TestingStatuses expectedTestingStatus = TestingStatuses.PASSED;
        String potFileName = "potFile.doc";
        UUID expectedId = UUID.randomUUID();
        String testCaseName1 = "tc1";
        TestingStatuses testingStatuses = TestingStatuses.PASSED;
        TestRun testRun = new TestRun();
        testRun.setName(testCaseName1);
        testRun.updateTestingStatus(testingStatuses);

        LogRecord reportRecord = buildRecord(false);
        reportRecord.setUuid(expectedId);
        LogRecord actionRecord = buildRecord(true);
        actionRecord.setTestingStatus(TestingStatuses.PASSED);
        actionRecord.setName(expectedName);

        reportRecord.setParentRecordId(actionRecord.getUuid());
        reportRecord.setFileMetadata(Collections.singletonList(new FileMetadata(FileType.POT, potFileName)));

        PotsStatisticsPerTestCase expectedResult = new PotsStatisticsPerTestCase(
                testingStatuses, testCaseName1, testRun.getUuid());

        assertEquals(expectedResult, potService.buildPotStatisticPerTestCase(testRun));
    }

    @Test
    public void findParentSectionRecord() {
        LogRecord reportRecord = buildRecord(false);
        LogRecord actionRecord = buildRecord(true);
        reportRecord.setParentRecordId(actionRecord.getUuid());

        Map<UUID, LogRecord> logRecordMap = ImmutableMap.of(
                reportRecord.getUuid(), reportRecord,
                actionRecord.getUuid(), actionRecord
        );
        assertEquals(actionRecord, potService.findParentSectionRecord(reportRecord, logRecordMap));
    }

    @Test
    public void recordHasPotFileMetadata_True() {
        LogRecord record = new LogRecord();
        record.setFileMetadata(Collections.singletonList(new FileMetadata(FileType.POT, "someName.doc")));
        assertTrue(potService.recordHasPotFileMetadata(record));
    }

    @Test
    public void recordHasPotFileMetadata_False() {
        LogRecord record = new LogRecord();
        record.setFileMetadata(null);
        assertFalse(potService.recordHasPotFileMetadata(record));
    }

    private LogRecord buildRecord(boolean isSection) {
        return buildRecord(isSection, RandomStringUtils.random(10), UUID.randomUUID(), TestingStatuses.UNKNOWN);
    }

    private LogRecord buildRecord(boolean isSection, String name, UUID logRecordId, TestingStatuses testingStatuses) {
        LogRecord record = new LogRecord();
        record.setName(name);
        record.setSection(isSection);
        record.setUuid(logRecordId);
        record.setTestingStatus(testingStatuses);
        return record;
    }

    @Test
    public void collectTrIdsToListOfReportRecordsMap() {
        UUID trId = UUID.randomUUID();
        LogRecord reportRecord = buildRecord(false);
        reportRecord.setFileMetadata(Collections.singletonList(new FileMetadata(FileType.POT, "someName.doc")));
        reportRecord.setTestRunId(trId);

        Map<UUID, List<LogRecord>> expectedResult = ImmutableMap.of(
                trId, Collections.singletonList(reportRecord)
        );

        assertEquals(expectedResult,
                potService.collectTrIdsToListOfReportRecordsMap(Collections.singletonList(reportRecord)));

    }

    @Test
    public void collectStatisticForTestRun_ShouldReturnListOfPotLogRecords() {
        UUID testRunId = UUID.randomUUID();
        UUID parentLr1 = UUID.randomUUID();
        UUID parentLr2 = UUID.randomUUID();

        LogRecord children1 = new LogRecord();
        children1.setFileMetadata(Collections.singletonList(new FileMetadata(FileType.POT, "someName.doc")));
        children1.setUuid(UUID.randomUUID());

        LogRecord children2 = new LogRecord();
        children2.setFileMetadata(Collections.singletonList(new FileMetadata(FileType.POT, "someName2.doc")));
        children2.setUuid(UUID.randomUUID());


        LogRecord parent1 = new LogRecord();
        parent1.setUuid(parentLr1);
        parent1.setName("name1");
        parent1.setTestingStatus(TestingStatuses.PASSED);

        LogRecord parent2 = new LogRecord();
        parent2.setUuid(parentLr2);
        parent2.setName("name2");
        parent2.setTestingStatus(TestingStatuses.PASSED);

        LogRecordWithParentResponse logRecordLookupResponse1 = new LogRecordWithParentResponse();
        logRecordLookupResponse1.setFileLogRecord(children1);
        logRecordLookupResponse1.setParent(parent1);

        LogRecordWithParentResponse logRecordLookupResponse2 = new LogRecordWithParentResponse();
        logRecordLookupResponse2.setFileLogRecord(children2);
        logRecordLookupResponse2.setParent(parent2);



        when(customLogRecordRepository.getTopLogRecordsIdAndChildLogRecordsByFileTypeFilterLookup(testRunId,
                FileType.POT))
                .thenReturn(Arrays.asList(logRecordLookupResponse1, logRecordLookupResponse2));


        PotsStatisticsPerAction potsStatisticsPerAction1 = new PotsStatisticsPerAction(parent1.getName(),
                parent1.getTestingStatus(), children1.getFileMetadata().get(0).getFileName(),
                children1.getUuid().toString());
        PotsStatisticsPerAction potsStatisticsPerAction2 = new PotsStatisticsPerAction(parent2.getName(),
                parent2.getTestingStatus(), children2.getFileMetadata().get(0).getFileName(),
                children2.getUuid().toString());

        List<PotsStatisticsPerAction> exp = Arrays.asList(potsStatisticsPerAction1, potsStatisticsPerAction2);

        List<PotsStatisticsPerAction> actual = potService.collectStatisticForTestRun(testRunId);
        assertEquals(exp.size(), actual.size());
        actual.forEach(potsStatisticsPerAction -> assertTrue(exp.contains(potsStatisticsPerAction)));
    }
}
