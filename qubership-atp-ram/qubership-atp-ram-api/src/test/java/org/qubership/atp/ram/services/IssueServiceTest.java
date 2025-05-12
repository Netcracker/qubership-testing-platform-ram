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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.modelmapper.ModelMapper;
import org.qubership.atp.common.lock.LockManager;
import org.qubership.atp.common.lock.provider.InMemoryLockProvider;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.Issue;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.repositories.CustomExecutionRequestRepository;
import org.qubership.atp.ram.repositories.CustomIssueRepository;
import org.qubership.atp.ram.repositories.ExecutionRequestRepository;
import org.qubership.atp.ram.repositories.IssueRepository;
import org.qubership.atp.ram.utils.IssueMock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ReflectionUtils;

@ExtendWith(SpringExtension.class)
@Isolated
public class IssueServiceTest {

    private static IssueService issueService;
    private static IssueRepository issueRepository;
    private static TestRunService testRunService;
    private static FailPatternService failPatternService;
    private static LogRecordService logRecordService;
    private static ExecutionRequestDetailsService executionRequestDetailsService;
    private static CustomIssueRepository customIssueRepository;
    private static ModelMapper modelMapper;
    private static ExecutionRequestRepository executionRequestRepository;
    private static LockManager lockManager;
    private static TestRun testRun;
    private ExecutionRequest executionRequest;
    private CustomExecutionRequestRepository customExecutionRequestRepository;

    @Captor
    ArgumentCaptor<List<Issue>> argCaptorListIssue;

    @BeforeEach
    public void set() throws NoSuchFieldException {
        issueRepository = mock(IssueRepository.class);
        testRunService = mock(TestRunService.class);
        failPatternService = mock(FailPatternService.class);
        logRecordService = mock(LogRecordService.class);
        executionRequestDetailsService = mock(ExecutionRequestDetailsService.class);
        modelMapper = new ModelMapper();
        customIssueRepository = mock(CustomIssueRepository.class);
        customExecutionRequestRepository = mock(CustomExecutionRequestRepository.class);
        executionRequestRepository = mock(ExecutionRequestRepository.class);
        lockManager = new LockManager(10, 10, 10, new InMemoryLockProvider());
        issueService = spy(
                new IssueService(
                        issueRepository,
                        customIssueRepository,
                        testRunService,
                        failPatternService,
                        logRecordService,
                        executionRequestDetailsService,
                        modelMapper,
                        executionRequestRepository,
                        customExecutionRequestRepository,
                        lockManager)
        );

        Field field_defaultLockDurationForCreatingIssuesSec
                = IssueService.class.getDeclaredField("lockDurationForCreatingIssuesSec");
        field_defaultLockDurationForCreatingIssuesSec.setAccessible(true);
        ReflectionUtils.setField(field_defaultLockDurationForCreatingIssuesSec, issueService, 300);
        Field field_regexpTimeout
                = IssueService.class.getDeclaredField("regexpTimeout");
        field_regexpTimeout.setAccessible(true);
        ReflectionUtils.setField(field_regexpTimeout, issueService, 300);
        Field field_logRecordStep
                = IssueService.class.getDeclaredField("logRecordStep");
        field_logRecordStep.setAccessible(true);
        ReflectionUtils.setField(field_logRecordStep, issueService, 500);

        UUID executionRequestId = UUID.randomUUID();
        testRun = new TestRun();
        testRun.setExecutionRequestId(executionRequestId);
        testRun.updateTestingStatus(TestingStatuses.FAILED);
        when(testRunService.getTestRunsIdByExecutionRequestIdAndTestingStatuses(any(), any()))
                .thenReturn(Collections.singletonList(testRun));

        executionRequest = new ExecutionRequest();
        executionRequest.setUuid(executionRequestId);
        executionRequest.setProjectId(UUID.randomUUID());

        when(executionRequestRepository.findProjectIdByUuid(any()))
                .thenReturn(executionRequest);
    }

    @Test
    public void recalculateIssuesInTestRun_OneLogRecord_OnePatternTwoStackTraces() {
        UUID testRunId = testRun.getUuid();
        UUID projectId = executionRequest.getProjectId();
        UUID executionRequestId = executionRequest.getUuid();

        List<LogRecord> logRecords = new ArrayList<>();
        LogRecord logRecordOne = IssueMock.logRecord(IssueMock.FIRST_STACKTRACE, testRunId);
        logRecords.add(logRecordOne);

        when(logRecordService.getAllFailedLogRecordsByTestRunIdsStream(any(), any())).thenReturn(logRecords.stream());
        when(logRecordService.countAllFailedLrByTestRunIds(any())).thenReturn(Long.valueOf(logRecords.size()));

        when(failPatternService
                .findPatternByProjectId(projectId))
                .thenReturn(Arrays.asList(
                        IssueMock.failPattern(IssueMock.FIRST_RULE), IssueMock.failPattern(IssueMock.SECOND_RULE)));
        ReflectionTestUtils.setField(issueService, "regexpTimeout", 300);
        issueService.recalculateIssuesForExecution(executionRequestId);

        verify(issueRepository, times(1)).saveAll(argCaptorListIssue.capture());
        Assertions.assertEquals(2, argCaptorListIssue.getAllValues().get(0).size());
    }

    @Test
    public void recalculateIssuesInTestRun_FailPatternId_Null() {
        UUID testRunId = testRun.getUuid();
        UUID projectId = executionRequest.getProjectId();
        UUID executionRequestId = executionRequest.getUuid();
        List<LogRecord> logRecords = new ArrayList<>();
        LogRecord logRecord1 = IssueMock.logRecord(IssueMock.FIRST_STACKTRACE, testRunId);
        LogRecord logRecord2 = IssueMock.logRecord(IssueMock.SECOND_STACKTRACE, testRunId);
        logRecords.add(logRecord2);
        logRecords.add(logRecord1);

        ExecutionRequest executionRequest = new ExecutionRequest();
        executionRequest.setUuid(executionRequestId);
        executionRequest.setProjectId(projectId);
        when(logRecordService.getAllFailedLogRecordsByTestRunIdsStream(any(), any())).thenReturn(logRecords.stream());
        when(logRecordService.countAllFailedLrByTestRunIds(any())).thenReturn(Long.valueOf(logRecords.size()));

        when(failPatternService
                .findPatternByProjectId(projectId))
                .thenReturn(Arrays.asList(
                        IssueMock.failPattern(IssueMock.FIRST_RULE), IssueMock.failPattern(IssueMock.SECOND_RULE)));
        issueService.recalculateIssuesForExecution(executionRequestId);

        verify(issueRepository, times(1)).saveAll(argCaptorListIssue.capture());
        Assertions.assertEquals(3, argCaptorListIssue.getAllValues().get(0).size());
    }

    @Test
    public void matchingTest() {
        Pattern p = Pattern.compile(IssueMock.FIRST_RULE);
        Matcher m = p.matcher(IssueMock.FIRST_STACKTRACE);
        Assertions.assertTrue(m.find());
    }

    @Test
    public void recalculateTopIssuesInER_FiveLogRecordsAddedTwoLogRecords_PassedResult() {
        UUID projectId = executionRequest.getProjectId();
        UUID executionRequestId = executionRequest.getUuid();

        TestRun testRun1 = new TestRun();
        TestRun testRun2 = new TestRun();
        testRun1.setExecutionRequestId(executionRequestId);
        testRun2.setExecutionRequestId(executionRequestId);

        UUID testRunId1 = testRun1.getUuid();
        UUID testRunId2 = testRun2.getUuid();

        List<LogRecord> logRecords = new ArrayList<>();
        LogRecord logRecord1 = IssueMock.logRecord(IssueMock.FIRST_STACKTRACE, testRunId1);
        LogRecord logRecord2 = IssueMock.logRecord(IssueMock.FIRST_STACKTRACE, testRunId1);
        LogRecord logRecord3 = IssueMock.logRecord(IssueMock.FIRST_STACKTRACE, testRunId2);
        LogRecord logRecord4 = IssueMock.logRecord(IssueMock.FIRST_STACKTRACE, testRunId2);
        LogRecord logRecord5 = IssueMock.logRecord(IssueMock.FIRST_STACKTRACE, testRunId2);
        logRecords.add(logRecord1);
        logRecords.add(logRecord2);
        logRecords.add(logRecord3);
        logRecords.add(logRecord4);
        logRecords.add(logRecord5);

        when(testRunService.getTestRunsIdByExecutionRequestIdAndTestingStatuses(any(), any()))
                .thenReturn(Arrays.asList(testRun1, testRun2));

        when(executionRequestRepository.findByUuid(any())).thenReturn(executionRequest);
        Stream<LogRecord> logRecordStream1 = logRecords.stream();
        when(logRecordService.getAllFailedLogRecordsByTestRunIdsStream(any(), any())).thenReturn(logRecordStream1);
        when(logRecordService.countAllFailedLrByTestRunIds(any())).thenReturn(Long.valueOf(logRecords.size()));

        when(failPatternService
                .findPatternByProjectId(projectId))
                .thenReturn(Arrays.asList(
                        IssueMock.failPattern(IssueMock.FIRST_RULE), IssueMock.failPattern(IssueMock.SECOND_RULE)));

        doCallRealMethod().when(issueService).calculateIssuesForExecution(any(), any());
        doCallRealMethod().when(issueService).calculateIssuesForExecution(any(), any(), any());
        List<Issue> createdIssues = new ArrayList<>();
        when(issueRepository.findShortByExecutionRequestId(any())).thenReturn(createdIssues);
        when(logRecordService.countAllFailedLrByTestRunIds(any())).thenReturn(Long.valueOf(logRecords.size()));

        issueService.recalculateTopIssues(executionRequestId);

        verify(customExecutionRequestRepository).updateLogRecordsCount(eq(executionRequestId), eq(5));

        LogRecord logRecord6 = IssueMock.logRecord(null, testRunId1);
        LogRecord logRecord7 = IssueMock.logRecord(null, testRunId2);
        logRecords.add(logRecord6);
        logRecords.add(logRecord7);
        Stream<LogRecord> logRecordStream2 = logRecords.stream();
        when(logRecordService.countAllFailedLrByTestRunIds(any())).thenReturn(Long.valueOf(logRecords.size()));
        when(logRecordService.getAllFailedLogRecordsByTestRunIdsStream(any(), any())).thenReturn(logRecordStream2);

        issueService.recalculateTopIssues(executionRequestId);
        verify(customExecutionRequestRepository).updateLogRecordsCount(eq(executionRequestId), eq(7));
    }

    @Test
    public void recalculateTopIssues_TwoPatternsTwoIssuesAndAddedTwoLogRecords_CheckPatternsOnlyForNewLogRecords() {
        UUID projectId = executionRequest.getProjectId();
        UUID executionRequestId = executionRequest.getUuid();

        TestRun testRun1 = new TestRun();
        TestRun testRun2 = new TestRun();
        testRun1.setExecutionRequestId(executionRequestId);
        testRun2.setExecutionRequestId(executionRequestId);

        UUID testRunId1 = testRun1.getUuid();
        UUID testRunId2 = testRun2.getUuid();

        List<LogRecord> logRecords = new ArrayList<>();
        LogRecord logRecord1 = IssueMock.logRecord(IssueMock.FIRST_STACKTRACE, testRunId1);
        LogRecord logRecord2 = IssueMock.logRecord(IssueMock.FIRST_STACKTRACE, testRunId1);

        logRecords.add(logRecord1);
        logRecords.add(logRecord2);

        when(testRunService.getTestRunsIdByExecutionRequestIdAndTestingStatuses(any(), any()))
                .thenReturn(Arrays.asList(testRun1, testRun2));

        when(executionRequestRepository.findByUuid(any())).thenReturn(executionRequest);
        when(failPatternService
                .findPatternByProjectId(projectId))
                .thenReturn(Arrays.asList(
                        IssueMock.failPattern(IssueMock.FIRST_RULE), IssueMock.failPattern(IssueMock.SECOND_RULE)));

        doCallRealMethod().when(issueService).calculateIssuesForExecution(any(), any());
        doCallRealMethod().when(issueService).calculateIssuesForExecution(any(), any(), any());
        List<Issue> createdIssues = new ArrayList<>();
        when(issueRepository.findShortByExecutionRequestId(any())).thenReturn(createdIssues);
        when(logRecordService.countAllFailedLrByTestRunIds(any())).thenReturn(Long.valueOf(logRecords.size()));
        Stream<LogRecord> logRecordStream1 = logRecords.stream();
        when(logRecordService.getAllFailedLogRecordsByTestRunIdsStream(any(), any())).thenReturn(logRecordStream1);

        issueService.recalculateTopIssues(executionRequestId);

        verify(customExecutionRequestRepository).updateLogRecordsCount(eq(executionRequestId), eq(2));

        LogRecord logRecord6 = IssueMock.logRecord(IssueMock.FIRST_STACKTRACE, testRunId1);
        LogRecord logRecord7 = IssueMock.logRecord(IssueMock.FIRST_STACKTRACE, testRunId2);
        logRecords.add(logRecord6);
        logRecords.add(logRecord7);

        List<Issue> createdIssues2 = new ArrayList<>();
        Issue issue = new Issue();
        List<UUID> lrIds = Arrays.asList(logRecord1.getUuid(), logRecord2.getUuid());
        issue.setLogRecordIds(lrIds);
        createdIssues2.add(issue);

        when(issueRepository.findShortByExecutionRequestId(any())).thenReturn(createdIssues2);

        List<LogRecord> logRecords2 = logRecords.stream()
                .filter(logRecord -> !lrIds.contains(logRecord.getUuid()))
                .collect(Collectors.toList());
        Stream<LogRecord> logRecordStream2 = logRecords2.stream();
        when(logRecordService.countAllFailedLrByTestRunIds(any())).thenReturn(Long.valueOf(logRecords.size()));
        when(logRecordService
                .getAllFailedLogRecordsByTestRunIdsStream(any(), any()))
                .thenReturn(logRecordStream2);

        issueService.recalculateTopIssues(executionRequestId);
        verify(issueService, times(4)).findMatchingFailPatterns(any(), any(), any());
        verify(customExecutionRequestRepository).updateLogRecordsCount(eq(executionRequestId), eq(4));

    }

    @Test
    public void deleteFailPattern_shouldSuccessfullyDeletedFromFailPatternRepositoryAndRelatedIssues() {
        // given
        UUID failPatternId = UUID.randomUUID();
        // when
        issueService.deleteFailPattern(failPatternId);
        // then
        verify(failPatternService).deleteByUuid(failPatternId);
        verify(issueRepository, times(1)).updateByRemovedPatternId(failPatternId);
    }

    @Test
    @Disabled
    public void calculateIssuesForExecution_whenTestRunHaveFailureReason_testRunSavedWithNotNullRootCauseId() {
        ArgumentCaptor<UUID> captorUuid = ArgumentCaptor.forClass(UUID.class);
        UUID executionRequestId = UUID.randomUUID();
        UUID failReasonId = UUID.fromString("f70b415a-e312-447e-b2c3-88caca636a88");
        UUID projectId = UUID.randomUUID();
        UUID firstTestRunUuid = UUID.fromString("56de2236-a45f-4102-b93c-c6a35b8d65bd");
        UUID failedTestRunId = UUID.fromString("450215fb-694b-4adc-858a-d1b2c5d2b84e");
        List<UUID> testRuns = new ArrayList<>();
        testRuns.add(firstTestRunUuid);
        testRuns.add(failedTestRunId);

        List<LogRecord> logLogRecords = generateListLogRecord(Arrays.asList(firstTestRunUuid, failedTestRunId));

        List<Issue> listIssue = generateListIssue(logLogRecords.get(0).getUuid(), logLogRecords.get(1).getUuid());
        List<UUID> logRecordIds = Arrays.asList(logLogRecords.get(0).getUuid(), logLogRecords.get(1).getUuid());
        List<LogRecord> logRecords = logLogRecords.stream()
                .filter(logRecord -> !logRecordIds.contains(logRecord.getUuid())).collect(Collectors.toList());

        listIssue.get(0).setFailedTestRunIds(Collections.singletonList(failedTestRunId));
        listIssue.get(0).setFailReasonId(failReasonId);

        when(logRecordService.getAllFailedLogRecordsByTestRunIdsStream(any(), any())).thenReturn(logRecords.stream());
        when(logRecordService.countAllFailedLrByTestRunIds(any())).thenReturn(Long.valueOf(logRecords.size()));

        when(failPatternService.findPatternByProjectId(projectId))
                .thenReturn(Arrays.asList(IssueMock.failPattern(IssueMock.FIRST_RULE), IssueMock.failPattern(IssueMock.SECOND_RULE)));
        when(issueRepository.findShortByExecutionRequestId(any())).thenReturn(listIssue);
        doNothing().when(testRunService).updateAnyFieldsForTestRunsByUuid(any(), any(), any());
        doCallRealMethod().when(testRunService).updateFieldRootCauseIdByTestRunsIds(any(), any());

        issueService.calculateIssuesForExecution(executionRequestId, testRuns, projectId);

        verify(testRunService, times(1)).updateAnyFieldsForTestRunsByUuid(captorUuid.capture(), any(), any());
        Assertions.assertNotNull(captorUuid.getValue());
        Assertions.assertEquals(captorUuid.getValue(), failedTestRunId, "TestRun will be saved with failReasonId");
    }

    public List<Issue> generateListIssue(UUID first, UUID second) {
        List<Issue> createdIssues2 = new ArrayList<>();
        Issue issue = new Issue();
        issue.setLogRecordIds(Arrays.asList(first, second));
        createdIssues2.add(issue);
        return createdIssues2;
    }

    public List<LogRecord> generateListLogRecord(List<UUID> listUuidTestruns) {
        List<LogRecord> logRecords = new ArrayList<>();
        LogRecord logRecord1 = IssueMock.logRecord(IssueMock.FIRST_STACKTRACE, listUuidTestruns.get(0));
        LogRecord logRecord2 = IssueMock.logRecord(IssueMock.FIRST_STACKTRACE, listUuidTestruns.get(1));
        logRecords.add(logRecord1);
        logRecords.add(logRecord2);
        return logRecords;
    }
}
