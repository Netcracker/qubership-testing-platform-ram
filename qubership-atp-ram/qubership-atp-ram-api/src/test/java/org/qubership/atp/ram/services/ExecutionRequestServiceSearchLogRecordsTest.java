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

import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;
import org.modelmapper.ModelMapper;
import org.qubership.atp.common.lock.LockManager;
import org.qubership.atp.ram.LogRecordMock;
import org.qubership.atp.ram.TestRunsMock;
import org.qubership.atp.ram.dto.request.LogRecordRegexSearchRequest;
import org.qubership.atp.ram.dto.response.LogRecordRegexSearchResponse;
import org.qubership.atp.ram.dto.response.LogRecordRegexSearchResponse.LogRecordResponse;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.repositories.CustomExecutionRequestRepository;
import org.qubership.atp.ram.repositories.ExecutionRequestConfigRepository;
import org.qubership.atp.ram.repositories.ExecutionRequestRepository;
import org.qubership.atp.ram.services.filtering.ExecutionRequestFilteringService;
import org.qubership.atp.ram.services.sorting.ExecutionRequestSortingService;
import org.qubership.atp.ram.utils.RateCalculator;
import org.qubership.atp.ram.utils.StreamUtils;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@Isolated
public class ExecutionRequestServiceSearchLogRecordsTest {

    private static ExecutionRequestService service;
    private static ExecutionRequestRepository repository;
    private static ExecutionRequestConfigRepository configRepository;
    private static TestRunService testRunService;

    private UUID executionRequestId;

    private TestRun testRun1;
    private TestRun testRun2;
    private TestRun testRun3;

    private LogRecord logRecord1;
    private LogRecord logRecord2;
    private LogRecord logRecord3;
    private LogRecord logRecord4;

    @BeforeAll
    public static void setUp() {
        repository = mock(ExecutionRequestRepository.class);
        configRepository = mock(ExecutionRequestConfigRepository.class);
        testRunService = mock(TestRunService.class);
        service = new ExecutionRequestService(repository,
                mock(CustomExecutionRequestRepository.class),
                mock(LogRecordService.class),
                testRunService,
                mock(TestCaseService.class),
                mock(RateCalculator.class),
                mock(ProjectsService.class),
                mock(UserService.class),
                new ModelMapper(),
                configRepository,
                mock(WidgetConfigTemplateService.class),
                mock(ExecutionRequestFilteringService.class),
                mock(ExecutionRequestSortingService.class),
                mock(JiraIntegrationService.class),
                mock(EnvironmentsInfoService.class),
                mock(OrchestratorService.class),
                mock(EnvironmentsService.class),
                mock(LabelsService.class),
                mock(LockManager.class),
                mock(RootCauseService.class));
    }

    @BeforeEach
    public void beforeTest() {
        executionRequestId = UUID.randomUUID();

        testRun1 = TestRunsMock.generateTestRun("TR1", TestingStatuses.PASSED);
        testRun1.setTestCaseId(UUID.randomUUID());
        testRun1.setTestCaseName("TC 1");
        logRecord1 = LogRecordMock.generateLogRecord("LR 1", testRun1.getUuid(),
                "[C] Situation 'Check discounts for $tc.Account.AccountNum': incoming message validation is failed");
        logRecord2 = LogRecordMock.generateLogRecord("LR 2", testRun1.getUuid(),
                "[B] Situation '0 - $tc.saved.key voice.ccri.mult - $tc.saved.SessionId - $tc.saved.ResultCode': incoming message validation is failed");

        testRun2 = TestRunsMock.generateTestRun("TR2", TestingStatuses.PASSED);
        testRun2.setTestCaseId(UUID.randomUUID());
        testRun2.setTestCaseName("TC 2");
        logRecord3 = LogRecordMock.generateLogRecord("LR 3", testRun2.getUuid(),
                "[D] HTTP Response code (500) is not in the allowed range!<pre>java.lang.RuntimeException: HTTP Response code (500) is not in the allowed range!");

        testRun3 = TestRunsMock.generateTestRun("TR3", TestingStatuses.PASSED);
        testRun3.setTestCaseId(UUID.randomUUID());
        testRun3.setTestCaseName("TC 3");
        logRecord4 = LogRecordMock.generateLogRecord("LR 4", testRun3.getUuid(),
                "[A] Engine replied with exception! null");

        List<TestRun> testRuns = asList(testRun1, testRun2, testRun3);
        Set<UUID> testRunIds = StreamUtils.extractIds(testRuns);

        List<LogRecord> logRecords = asList(logRecord1, logRecord2, logRecord3, logRecord4);

        when(testRunService.findNotPassedTestRunByErId(executionRequestId)).thenReturn(testRuns);
        when(testRunService.getAllFailedLogRecords(testRunIds)).thenReturn(logRecords);
    }

    @Test
    public void searchLogRecords_checkAllFieldsMappingAndOrder_shouldReturnCorrectResponse() {
        final int size = 2;
        final LogRecordRegexSearchRequest request = new LogRecordRegexSearchRequest(".*", 0, size);

        LogRecordRegexSearchResponse result = service.searchFailedLogRecords(executionRequestId, request);
        Assertions.assertNotNull(result);

        List<LogRecordResponse> resultLogRecords = result.getLogRecords();
        Assertions.assertNotNull(resultLogRecords);

        Assertions.assertEquals(size, resultLogRecords.size());

        LogRecordResponse first = resultLogRecords.get(0);
        Assertions.assertEquals(logRecord4.getUuid(), first.getLogRecordId());
        Assertions.assertEquals(logRecord4.getName(), first.getLogRecordName());
        Assertions.assertEquals(logRecord4.getMessage(), first.getLogRecordMessage());
        Assertions.assertEquals(testRun3.getUuid(), first.getTestRunId());
        Assertions.assertEquals(testRun3.getTestCaseName(), first.getTestCaseName());
        Assertions.assertEquals(testRun3.getTestCaseId(), first.getTestCaseId());

        LogRecordResponse second = resultLogRecords.get(1);
        Assertions.assertEquals(logRecord2.getUuid(), second.getLogRecordId());
        Assertions.assertEquals(logRecord2.getName(), second.getLogRecordName());
        Assertions.assertEquals(logRecord2.getMessage(), second.getLogRecordMessage());
        Assertions.assertEquals(testRun1.getUuid(), second.getTestRunId());
        Assertions.assertEquals(testRun1.getTestCaseName(), second.getTestCaseName());
        Assertions.assertEquals(testRun1.getTestCaseId(), second.getTestCaseId());
    }

    @Test
    public void searchLogRecords_checkPartialRegex1_shouldReturnCorrectResponse() {
        LogRecordRegexSearchRequest request = new LogRecordRegexSearchRequest("Situation", 0, 4);

        LogRecordRegexSearchResponse result = service.searchFailedLogRecords(executionRequestId, request);
        Assertions.assertNotNull(result);

        List<LogRecordResponse> resultLogRecords = result.getLogRecords();
        Assertions.assertNotNull(resultLogRecords);

        Assertions.assertEquals(2, resultLogRecords.size());

        Assertions.assertEquals(logRecord2.getUuid(), resultLogRecords.get(0).getLogRecordId());
        Assertions.assertEquals(logRecord1.getUuid(), resultLogRecords.get(1).getLogRecordId());
    }

    @Test
    public void searchLogRecords_checkPartialRegex2_shouldReturnCorrectResponse() {
        LogRecordRegexSearchRequest request = new LogRecordRegexSearchRequest("Response code \\(500\\)", 0, 4);

        LogRecordRegexSearchResponse result = service.searchFailedLogRecords(executionRequestId, request);
        Assertions.assertNotNull(result);

        List<LogRecordResponse> resultLogRecords = result.getLogRecords();
        Assertions.assertNotNull(resultLogRecords);

        Assertions.assertEquals(1, resultLogRecords.size());

        Assertions.assertEquals(logRecord3.getUuid(), resultLogRecords.get(0).getLogRecordId());
    }
}
