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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;
import org.qubership.atp.ram.dto.request.UpdateLogRecordExecutionStatusRequest;
import org.qubership.atp.ram.dto.response.LogRecordShort;
import org.qubership.atp.ram.entities.ErrorMappingItem;
import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.enums.TypeAction;
import org.qubership.atp.ram.mapper.LogRecordMapper;
import org.qubership.atp.ram.mapper.Mapper;
import org.qubership.atp.ram.model.SubstepScreenshotResponse;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.LogRecordContextVariable;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.models.logrecords.UiLogRecord;
import org.qubership.atp.ram.models.logrecords.parts.ContextVariable;
import org.qubership.atp.ram.repositories.AkbRecordsRepository;
import org.qubership.atp.ram.repositories.CustomLogRecordRepository;
import org.qubership.atp.ram.repositories.ExecutionRequestRepository;
import org.qubership.atp.ram.repositories.IssueRepository;
import org.qubership.atp.ram.repositories.LogRecordContextVariableRepository;
import org.qubership.atp.ram.repositories.LogRecordMessageParametersRepository;
import org.qubership.atp.ram.repositories.LogRecordRepository;
import org.qubership.atp.ram.repositories.LogRecordStepContextVariableRepository;
import org.qubership.atp.ram.repositories.TestRunRepository;
import org.qubership.atp.ram.utils.SourceShot;
import org.springframework.test.util.ReflectionTestUtils;

public class LogRecordServiceTest {

    private LogRecordService logRecordService;
    private TestRunService testRunService;
    private ExecutionRequestService erService;
    private GridFsService gridFsService;
    private TestRunRepository testRunRepository;
    private CatalogueService catalogueService;
    private ModelMapper modelMapper;
    private Mapper<LogRecord, LogRecordShort> logRecordMapper = new LogRecordMapper();
    private CustomLogRecordRepository customLogRecordRepository;
    private LogRecordRepository repository;
    private IssueRepository issueRepository;
    private LogRecordContextVariableService logRecordContextVariableService;
    private LogRecordStepContextVariableRepository logRecordStepContextRepository;
    private LogRecordMessageParametersRepository logRecordMessageParametersRepository;
    private LogRecordContextVariableRepository logRecordContextRepository;
    private BrowserConsoleLogService browserConsoleLogService;

    @BeforeEach
    public void setUp() {
        testRunService = Mockito.mock(TestRunService.class);
        erService = Mockito.mock(ExecutionRequestService.class);
        ExecutionRequestRepository executionRequestRepository = mock(ExecutionRequestRepository.class);
        gridFsService = mock(GridFsService.class);
        testRunRepository = mock(TestRunRepository.class);
        catalogueService = mock(CatalogueService.class);
        repository = mock(LogRecordRepository.class);
        issueRepository = mock(IssueRepository.class);
        customLogRecordRepository = mock(CustomLogRecordRepository.class);
        logRecordContextVariableService = mock(LogRecordContextVariableService.class);
        logRecordContextRepository = mock(LogRecordContextVariableRepository.class);
        logRecordStepContextRepository = mock(LogRecordStepContextVariableRepository.class);
        logRecordMessageParametersRepository = mock(LogRecordMessageParametersRepository.class);
        browserConsoleLogService = mock(BrowserConsoleLogService.class);
        logRecordService = new LogRecordService(
                logRecordMapper,
                repository,
                logRecordContextRepository,
                logRecordStepContextRepository,
                logRecordMessageParametersRepository,
                testRunRepository,
                gridFsService,
                mock(AkbRecordsRepository.class),
                executionRequestRepository,
                modelMapper,
                customLogRecordRepository,
                issueRepository,
                catalogueService,
                logRecordContextVariableService,
                browserConsoleLogService);
    }

    @Test
    public void getSubstepScreenshots_whenHaveTwoUuid() {
        List<UUID> list = new ArrayList<>();
        list.add(UUID.fromString("7e9494ae-c77d-4409-b1c7-5b0b338adf4f"));
        list.add(UUID.fromString("5c9cfcf9-9f1e-4693-b2f7-492ff17a2ee6"));
        SourceShot sourceShot = new SourceShot();
        sourceShot.setContent("data:image/png;base64,content");
        sourceShot.setSnapshotSource("snapshotSource");

        when(gridFsService.getScreenShot(any())).thenReturn(sourceShot);
        List<SubstepScreenshotResponse> rep = logRecordService.getSubstepScreenshots(list);
        SubstepScreenshotResponse expectedResult = rep.get(0);

        assertEquals(expectedResult.getId().toString(), "7e9494ae-c77d-4409-b1c7-5b0b338adf4f");
        assertEquals(expectedResult.getScreenshot(), "content");
        assertEquals(expectedResult.getScreenshotSource(), "snapshotSource");
    }

    @Test
    public void testGetErrorMappingReturnsLogRecordsFromTestRun() {
        UUID parentUuid = UUID.randomUUID();
        LogRecord logRecord = new LogRecord();
        logRecord.setTestingStatus(TestingStatuses.FAILED);
        UUID expectedId = UUID.randomUUID();
        logRecord.setUuid(expectedId);
        Mockito.when(repository.findLogRecordsWithSpecificFieldsByTestRunIdOrderByStartDateAsc(parentUuid))
                .thenReturn(Collections.singletonList(logRecord));
        List<ErrorMappingItem> mapping = logRecordService.getErrorMapping(
                erService, "testRun", parentUuid);
        Assertions.assertFalse(mapping.isEmpty());
        ErrorMappingItem item = mapping.iterator().next();
        assertEquals(expectedId, item.getId());
        assertEquals(TestingStatuses.FAILED.name(), item.getLevel());
    }

    @Test
    public void testGetErrorMappingReturnsLogRecordsFromExecutionRequest() {
        UUID parentUuid = UUID.randomUUID();
        LogRecord logRecord = new LogRecord();
        logRecord.setTestingStatus(TestingStatuses.FAILED);
        UUID expectedId = UUID.randomUUID();
        logRecord.setUuid(expectedId);
        TestRun testRun = new TestRun();
        testRun.setUuid(expectedId);
        Mockito.when(erService.getAllTestRuns(parentUuid)).thenReturn(Collections.singletonList(testRun));
        Mockito.when(repository.findLogRecordsWithSpecificFieldsByTestRunIdOrderByStartDateAsc(expectedId))
                .thenReturn(Collections.singletonList(logRecord));
        List<ErrorMappingItem> mapping = logRecordService.getErrorMapping(
                erService, "executionRequest", parentUuid);
        Assertions.assertFalse(mapping.isEmpty());
        ErrorMappingItem item = mapping.iterator().next();
        assertEquals(expectedId, item.getId());
        assertEquals(TestingStatuses.FAILED.name(), item.getLevel());
        Mockito.verify(erService).getAllTestRuns(parentUuid);
    }

    @Test
    public void testGetErrorThrowsErrorIfSourceIsEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            logRecordService.getErrorMapping(null, "", UUID.randomUUID());
        });
    }

    @Test
    public void testGetErrorThrowsErrorIfParentUuidIsEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            logRecordService.getErrorMapping(null, "123", null);
        });
    }

    @Test
    public void saveCountScreenshots_ShouldBeValidSaveTestRun() {
        when(logRecordService.getAllLogRecordsUuidByTestRunId(any()))
                .thenReturn(Collections.singletonList(new LogRecord()));

        int countOfScreenshot = 8;
        when(gridFsService.getCountScreen(any())).thenReturn(countOfScreenshot);

        TestRun mockTestRun = new TestRun();
        mockTestRun.setNumberOfScreens(countOfScreenshot);


        logRecordService.saveCountScreenshots(UUID.randomUUID(), Collections.singletonList(new TestRun()));

        assertEquals(countOfScreenshot, mockTestRun.getNumberOfScreens(),
                "Count of screenshots of TR is valid");
    }

    @Test
    public void updateExecutionStatus_shouldBeSuccessfullyUpdated() {
        LogRecord logRecord = new LogRecord();
        logRecord.setUuid(UUID.randomUUID());
        logRecord.setExecutionStatus(ExecutionStatuses.IN_PROGRESS);

        UpdateLogRecordExecutionStatusRequest request = new UpdateLogRecordExecutionStatusRequest();
        request.setExecutionStatus(ExecutionStatuses.FINISHED);

        when(repository.findById(logRecord.getUuid())).thenReturn(Optional.of(logRecord));
        ArgumentCaptor<LogRecord> argCaptor = ArgumentCaptor.forClass(LogRecord.class);

        logRecordService.updateExecutionStatus(logRecord.getUuid(), request);

        Mockito.verify(repository).save(argCaptor.capture());

        LogRecord updatedLogRecord = argCaptor.getValue();

        assertNotNull(updatedLogRecord);
        assertEquals(logRecord.getUuid(), updatedLogRecord.getUuid());
        assertEquals(request.getExecutionStatus(), updatedLogRecord.getExecutionStatus());
    }

    /*
     * COMP 1
     *   LR 1
     *       ULR 1
     *       ULR 2
     *   LR 2
     *       LR 3
     *           ULR3
     *       ULR4
     *   ULR5
     * */
    @Test
    public void getAllHierarchicalChildrenLogRecords_shouldBeSuccessfullyCalled() {
        LogRecord comp1 = generateLogRecord("Comp 1", TypeAction.COMPOUND, null);
        LogRecord lr1 = generateLogRecord("LR 1", TypeAction.TECHNICAL, comp1);
        LogRecord ulr1 = generateLogRecord("ULR 1", TypeAction.UI, lr1);
        LogRecord ulr2 = generateLogRecord("ULR 2", TypeAction.UI, lr1);
        LogRecord lr2 = generateLogRecord("LR 2", TypeAction.TECHNICAL, comp1);
        LogRecord lr3 = generateLogRecord("LR 3", TypeAction.TECHNICAL, lr2);
        LogRecord ulr3 = generateLogRecord("ULR 3", TypeAction.UI, lr3);
        LogRecord ulr4 = generateLogRecord("ULR 4", TypeAction.UI, lr2);
        LogRecord ulr5 = generateLogRecord("ULR 5", TypeAction.UI, comp1);

        when(repository.findAllByParentRecordIdIsOrderByCreatedDateStampAsc(comp1.getUuid()))
                .thenReturn(Stream.of(lr1, lr2, ulr5));
        when(repository.findAllByParentRecordIdIsOrderByCreatedDateStampAsc(lr1.getUuid()))
                .thenReturn(Stream.of(ulr1, ulr2));
        when(repository.findAllByParentRecordIdIsOrderByCreatedDateStampAsc(lr2.getUuid()))
                .thenReturn(Stream.of(lr3, ulr4));
        when(repository.findAllByParentRecordIdIsOrderByCreatedDateStampAsc(lr3.getUuid())).thenReturn(Stream.of(ulr3));

        List<LogRecord> result = logRecordService.getAllHierarchicalChildrenLogRecords(comp1.getUuid());

        assertNotNull(result);
        assertEquals(5, result.size());
        assertEquals(ulr1.getUuid(), result.get(0).getUuid());
        assertEquals(ulr2.getUuid(), result.get(1).getUuid());
        assertEquals(ulr3.getUuid(), result.get(2).getUuid());
        assertEquals(ulr4.getUuid(), result.get(3).getUuid());
        assertEquals(ulr5.getUuid(), result.get(4).getUuid());
    }

    private List<ContextVariable> generateRandomContextVariables(int count) {
        ArrayList<ContextVariable> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String name = "name_" + RandomStringUtils.random(10, true, true);
            String before = "before_" + RandomStringUtils.random(10, true, true);
            String after = "after_" + RandomStringUtils.random(10, true, true);
            result.add(new ContextVariable(name, before, after));
        }
        return result;
    }

    private LogRecord generateLogRecord(String name, TypeAction type, LogRecord parent) {
        LogRecord logRecord = new LogRecord();
        logRecord.setUuid(UUID.randomUUID());
        logRecord.setName(name);
        logRecord.setType(type);
        if (TypeAction.UI.equals(type)) {
            logRecord.setPreview("Lorem ipsum...");
        }
        if (parent != null && parent.getParentRecordId() != null) {
            logRecord.setParentRecordId(parent.getParentRecordId());
        }

        return logRecord;
    }

    @Test
    public void findById_givenUiLogRecord_canGenerateBrowserMonitoringLink() {
        UUID logRecordId = UUID.randomUUID();
        Timestamp startDay = Timestamp.valueOf("2022-01-01 00:00:00.001");
        Timestamp endDay = Timestamp.valueOf("2022-01-01 00:01:00.002");
        String browserName = "BrowserTestName";
        String linkTemplate = "https://dashboard.some-domain.com/d/kzqCPg_Wk/atp-cloud-pods?orgId=3&var-gr_prefix="
                + "teams.oshobj&var-cluster=atp-cloud&var-ns=prod&var-app_type=atp-ram&var-pod=%{browser_pod}&from=%"
                + "{from_timestamp}&to=%{to_timestamp}";
        String expectedLink = "https://dashboard.some-domain.com/d/kzqCPg_Wk/atp-cloud-pods?orgId=3&var-gr_prefix="
                + "teams.oshobj&var-cluster=atp-cloud&var-ns=prod&var-app_type=atp-ram&var-pod=" + browserName
                + "&from=" + startDay.getTime() + "&to=" + endDay.getTime();
        ReflectionTestUtils.setField(logRecordService, "browserMonitoringLinkTemplate", linkTemplate);
        UiLogRecord uiLogRecord = new UiLogRecord();
        uiLogRecord.setUuid(logRecordId);
        uiLogRecord.setBrowserName(browserName);
        uiLogRecord.setStartDate(startDay);
        uiLogRecord.setEndDate(endDay);
        when(repository.findByUuid(logRecordId)).thenReturn(uiLogRecord);

        UiLogRecord actualLogRecord = (UiLogRecord) logRecordService.findById(logRecordId);

        assertEquals(expectedLink, actualLogRecord.getBrowserMonitoringLink());
    }

    @Test
    public void findById_givenUiLogRecordWithoutBrowserName_returnLogRecordWithoutBrowserMonitoringLink() {
        UUID logRecordId = UUID.randomUUID();
        Timestamp startDay = Timestamp.valueOf("2022-01-01 00:00:00.001");
        Timestamp endDay = Timestamp.valueOf("2022-01-01 00:01:00.002");
        String linkTemplate = "https://dashboard.some-domain.com/d/kzqCPg_Wk/atp-cloud-pods?orgId=3&var-gr_prefix="
                + "teams.oshobj&var-cluster=atp-cloud&var-ns=prod&var-app_type=atp-ram&var-pod=%{browser_pod}&from=%"
                + "{from_timestamp}&to=%{to_timestamp}";
        ReflectionTestUtils.setField(logRecordService, "browserMonitoringLinkTemplate", linkTemplate);
        UiLogRecord uiLogRecord = new UiLogRecord();
        uiLogRecord.setUuid(logRecordId);
        uiLogRecord.setStartDate(startDay);
        uiLogRecord.setEndDate(endDay);
        when(repository.findByUuid(logRecordId)).thenReturn(uiLogRecord);

        UiLogRecord actualLogRecord = (UiLogRecord) logRecordService.findById(logRecordId);

        assertNotNull(actualLogRecord);
    }

    @Test
    public void test_getAllContextVariables_returnAllContextVariablesByLogRecordId() {
        UUID logRecordId = UUID.randomUUID();
        List<ContextVariable> contextVariables = new ArrayList<>();
        contextVariables.add(new ContextVariable("cv1", "value1", "value11"));
        contextVariables.add(new ContextVariable("cv2", "value2", "value22"));
        LogRecordContextVariable logRecordContextVariable = new LogRecordContextVariable();
        logRecordContextVariable.setContextVariables(contextVariables);
        when(logRecordContextRepository.getById(eq(logRecordId))).thenReturn(logRecordContextVariable);
        List<ContextVariable> result = logRecordService.getAllContextVariables(logRecordId);
        assertEquals(result, contextVariables);
    }

    @Test
    public void test_getAllContextVariablesByIds_returnAllContextVariablesByLogRecordIds() {
        UUID logRecordId = UUID.randomUUID();
        List<ContextVariable> contextVariables = new ArrayList<>();
        contextVariables.add(new ContextVariable("cv1", "value1", "value11"));
        contextVariables.add(new ContextVariable("cv2", "value2", "value22"));
        LogRecordContextVariable logRecordContextVariable = new LogRecordContextVariable();
        logRecordContextVariable.setContextVariables(contextVariables);
        when(logRecordContextRepository.findAllByIdIn(eq(Collections.singletonList(logRecordId)))).thenReturn(Collections.singletonList(logRecordContextVariable));
        List<ContextVariable> result = logRecordService.getContextVariablesByIds(Collections.singletonList(logRecordId));
        assertEquals(result, contextVariables);
    }
}
