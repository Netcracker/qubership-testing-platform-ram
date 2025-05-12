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

package org.qubership.atp.ram;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.modelmapper.ModelMapper;
import org.qubership.atp.ram.dto.response.BaseEntityResponse;
import org.qubership.atp.ram.dto.response.SimpleTestRunResponse;
import org.qubership.atp.ram.dto.response.TestCaseLabelResponse;
import org.qubership.atp.ram.dto.response.TestRunWithValidationLabelsResponse;
import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.models.EnrichedTestRun;
import org.qubership.atp.ram.models.Label;
import org.qubership.atp.ram.models.RootCause;
import org.qubership.atp.ram.models.TestRun;
import org.springframework.util.CollectionUtils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class TestRunsMock {

    private final ModelMapper modelMapper = new ModelMapper();
    final static UUID initialUuid = UUID.fromString("b90dfeda-1fac-4e10-bef9-d852a23d2003");

    public List<TestRun> findByExecutionRequestId() {
        List<TestRun> result = findByExecutionRequestIdWithoutSystemTestRun(null);
        result.add(generateExecutionRequestsLog());
        return result;
    }

    public List<TestRun> findByExecutionRequestIdWithoutSystemTestRun(List<RootCause> rootCauses) {
        List<TestRun> result = new ArrayList<>();
        result.add(generateTestRun(TestingStatuses.PASSED, null));
        result.add(generateTestRun(TestingStatuses.PASSED, null));

        UUID uuidRc1 = UUID.randomUUID();
        UUID uuidRc2 = UUID.randomUUID();
        UUID uuidRc3 = UUID.randomUUID();
        int rootCausesSizeMock = 4;
        if (!CollectionUtils.isEmpty(rootCauses) && rootCauses.size() == rootCausesSizeMock) {
            uuidRc1 = rootCauses.get(1).getUuid();
            uuidRc3 = rootCauses.get(3).getUuid();
            uuidRc2 = rootCauses.get(2).getUuid();
        }

        result.add(generateTestRun(TestingStatuses.WARNING, uuidRc1));
        result.add(generateTestRun(TestingStatuses.WARNING, uuidRc1));
        result.add(generateTestRun(TestingStatuses.WARNING, uuidRc2));
        result.add(generateTestRun(TestingStatuses.SKIPPED, uuidRc3));
        return result;
    }

    public List<TestRun> findByExecutionRequestIdWithOtherIds() {
        List<TestRun> result = new ArrayList<>();
        result.add(generateTestRun(TestingStatuses.PASSED, null));
        result.add(generateTestRun(TestingStatuses.PASSED, null));

        UUID uuidRc = UUID.randomUUID();
        result.add(generateTestRun(TestingStatuses.WARNING, uuidRc));
        result.add(generateTestRun(TestingStatuses.WARNING, UUID.randomUUID()));
        result.add(generateTestRun(TestingStatuses.WARNING, uuidRc));
        return result;
    }

    private TestRun generateTestRun(TestingStatuses testingStatuses, UUID idRootCause) {
        TestRun testRun = new TestRun();
        UUID uuid = UUID.randomUUID();
        testRun.setName("Test Run " + uuid);
        testRun.setUuid(uuid);
        testRun.updateTestingStatus(testingStatuses);
        testRun.setRootCauseId(idRootCause);
        testRun.setTestCaseId(uuid);
        return testRun;
    }

    private TestRun generateExecutionRequestsLog() {
        TestRun testRun = new TestRun();
        testRun.setName("Execution Request's Logs");
        testRun.setUuid(UUID.randomUUID());
        testRun.updateTestingStatus(TestingStatuses.FAILED);
        testRun.setRootCauseId(null);
        return testRun;
    }

    public TestRun generateSimpleTestRun(UUID parentId) {
        TestRun simpleTestRun = new TestRun();
        simpleTestRun.setName("New");
        simpleTestRun.setUuid(UUID.randomUUID());
        simpleTestRun.setParentTestRunId(parentId);
        simpleTestRun.setExecutionStatus(ExecutionStatuses.IN_PROGRESS);
        simpleTestRun.updateTestingStatus(TestingStatuses.PASSED);
        simpleTestRun.setLogCollectorData("http");
        simpleTestRun.setStartDate(new Timestamp(System.currentTimeMillis()));
        simpleTestRun.setFinishDate(new Timestamp(System.currentTimeMillis()));
        simpleTestRun.setDuration(80);
        simpleTestRun.setTaHost(Collections.singletonList("ta host"));
        simpleTestRun.setUrlToBrowserSession("browser");
        simpleTestRun.setSolutionBuild(Collections.singletonList("solution build"));
        simpleTestRun.setDataSetUrl("http DS");
        simpleTestRun.setTestCaseId(UUID.randomUUID());
        simpleTestRun.setTestCaseName("test case");
        simpleTestRun.setRootCauseId(UUID.randomUUID());
        simpleTestRun.setExecutionRequestId(UUID.randomUUID());
        simpleTestRun.setJiraTicket("TMSSBOX-1111");

        return simpleTestRun;
    }

    public EnrichedTestRun generateEnrichedTestRun(UUID parentId) {
        EnrichedTestRun simpleTestRun = new EnrichedTestRun();
        simpleTestRun.setName("New");
        simpleTestRun.setUuid(UUID.randomUUID());
        simpleTestRun.setParentTestRunId(parentId);
        simpleTestRun.setExecutionStatus(ExecutionStatuses.IN_PROGRESS);
        simpleTestRun.updateTestingStatus(TestingStatuses.PASSED);
        simpleTestRun.setLogCollectorData("http");
        simpleTestRun.setStartDate(new Timestamp(System.currentTimeMillis()));
        simpleTestRun.setFinishDate(new Timestamp(System.currentTimeMillis()));
        simpleTestRun.setDuration(80);
        simpleTestRun.setTaHost(Collections.singletonList("ta host"));
        simpleTestRun.setUrlToBrowserSession("browser");
        simpleTestRun.setSolutionBuild(Collections.singletonList("solution build"));
        simpleTestRun.setDataSetUrl("http DS");
        simpleTestRun.setTestCaseId(UUID.randomUUID());
        simpleTestRun.setTestCaseName("test case");
        simpleTestRun.setRootCauseId(UUID.randomUUID());
        simpleTestRun.setExecutionRequestId(UUID.randomUUID());

        return simpleTestRun;
    }

    public TestRun generateGroupedTestRun(UUID uuid, String name, UUID parentId) {
        TestRun groupedTestRun = new TestRun();
        groupedTestRun.setName(name);
        groupedTestRun.setUuid(uuid);
        groupedTestRun.setParentTestRunId(parentId);
        groupedTestRun.setExecutionStatus(ExecutionStatuses.IN_PROGRESS);
        groupedTestRun.setGroupedTestRun(true);
        return groupedTestRun;
    }

    public SimpleTestRunResponse generateSimpleTrResponse(TestRun simpleTestRun, String rootCauseName,
                                                          long allLogRecordsCount, long passedLogRecordsCount) {
        SimpleTestRunResponse simpleTestRunExp = modelMapper.map(simpleTestRun, SimpleTestRunResponse.class);
        simpleTestRunExp.setRootCause(new BaseEntityResponse(simpleTestRun.getRootCauseId(),
                rootCauseName));
        simpleTestRunExp.setAllLogRecordsCount(allLogRecordsCount);
        simpleTestRunExp.setPassedLogRecordsCount(passedLogRecordsCount);
        return simpleTestRunExp;
    }

    public List<Label> generateLabels() {
        List<Label> labels = new ArrayList<>();
        Label label1 = new Label();
        label1.setUuid(UUID.randomUUID());
        label1.setName("label1");
        labels.add(label1);
        Label label2 = new Label();
        label2.setUuid(UUID.randomUUID());
        label2.setName("label2");
        labels.add(label2);
        return labels;
    }

    public TestCaseLabelResponse generateTestCaseLabelResponse(UUID testCaseId) {
        return generateTestCaseLabelResponseWithLabelIds(testCaseId, null);
    }

    public TestCaseLabelResponse generateTestCaseLabelResponseWithLabelIds(UUID testCaseId, List<Label> labels) {
        TestCaseLabelResponse testCaseLabelResponse = new TestCaseLabelResponse();
        testCaseLabelResponse.setUuid(testCaseId);
        testCaseLabelResponse.setName("TestCaseLabelResponse1");
        if (CollectionUtils.isEmpty(labels)) {
            testCaseLabelResponse.setLabels(generateLabels());
        } else {
            testCaseLabelResponse.setLabels(labels);
        }
        return testCaseLabelResponse;
    }

    public TestRun generateTestRun(String name, TestingStatuses testingStatus) {
        TestRun testRun = generateTestRun(name);
        testRun.updateTestingStatus(testingStatus);

        return testRun;
    }

    public TestRun generateTestRun(String name, UUID testCaseId, TestingStatuses testingStatus) {
        TestRun testRun = generateTestRunWithId(name, testCaseId);
        testRun.updateTestingStatus(testingStatus);

        return testRun;
    }

    public TestRun generateTestRun(String name) {
        TestRun testRun = new TestRun();
        testRun.setUuid(UUID.randomUUID());
        testRun.setName(name);
        testRun.setTestCaseId(UUID.randomUUID());

        testRun.setPassedRate(30);
        testRun.setWarningRate(20);
        testRun.setFailedRate(50);
        testRun.setRootCauseId(UUID.randomUUID());
        testRun.setDataSetListUrl(UUID.randomUUID().toString());

        return testRun;
    }

    public TestRun generateTestRunWithId(String name, UUID testCaseId) {
        TestRun testRun = new TestRun();
        testRun.setUuid(UUID.randomUUID());
        testRun.setName(name);
        testRun.setTestCaseId(testCaseId);

        testRun.setPassedRate(30);
        testRun.setWarningRate(20);
        testRun.setFailedRate(50);
        testRun.setRootCauseId(UUID.randomUUID());
        testRun.setDataSetListUrl(UUID.randomUUID().toString());

        return testRun;
    }

    public TestRun generateTestRun(UUID id, UUID erId) {
        TestRun testRun = new TestRun();
        testRun.setUuid(id);
        testRun.setExecutionRequestId(erId);
        return testRun;
    }

    public List<TestRun> generateSetCountTestRuns(int passedCount, int warningCount, int failedCount,
                                                  int skippedCount, int stoppedCount, int notStartedCount,
                                                  int inProgressCount, int blockedCount) {
        List<TestRun> testRuns = getTestRunsByCountAndStatus(TestingStatuses.PASSED, passedCount);
        testRuns.addAll(getTestRunsByCountAndStatus(TestingStatuses.WARNING, warningCount));
        testRuns.addAll(getTestRunsByCountAndStatus(TestingStatuses.FAILED, failedCount));
        testRuns.addAll(getTestRunsByCountAndStatus(TestingStatuses.SKIPPED, skippedCount));
        testRuns.addAll(getTestRunsByCountAndStatus(TestingStatuses.STOPPED, stoppedCount));
        testRuns.addAll(getTestRunsByCountAndStatus(TestingStatuses.NOT_STARTED, notStartedCount));
        testRuns.addAll(getTestRunsByCountAndStatus(ExecutionStatuses.IN_PROGRESS, inProgressCount));
        testRuns.addAll(getTestRunsByCountAndStatus(TestingStatuses.BLOCKED, blockedCount));
        return testRuns;
    }

    public List<TestRun> generateInitialTestRuns(int passedCount, int warningCount, int failedCount,
                                                  int skippedCount, int stoppedCount, int notStartedCount,
                                                  int inProgressCount, int blockedCount) {
        List<TestRun> testRuns = getTestRunsByCountAndStatusAndInitialId(TestingStatuses.PASSED, passedCount, initialUuid);
        testRuns.addAll(getTestRunsByCountAndStatusAndInitialId(TestingStatuses.WARNING, warningCount, initialUuid));
        testRuns.addAll(getTestRunsByCountAndStatusAndInitialIdAndFinalTestRun(TestingStatuses.FAILED, failedCount, initialUuid, true));
        testRuns.addAll(getTestRunsByCountAndStatusAndInitialId(TestingStatuses.SKIPPED, skippedCount, initialUuid));
        testRuns.addAll(getTestRunsByCountAndStatusAndInitialId(TestingStatuses.STOPPED, stoppedCount, initialUuid));
        testRuns.addAll(getTestRunsByCountAndStatusAndInitialId(TestingStatuses.NOT_STARTED, notStartedCount, initialUuid));
        testRuns.addAll(getTestRunsByCountAndStatus(ExecutionStatuses.IN_PROGRESS, inProgressCount));
        testRuns.addAll(getTestRunsByCountAndStatusAndInitialId(TestingStatuses.BLOCKED, blockedCount, initialUuid));
        return testRuns;
    }

    public static List<TestRun> generateInitialTestRunsAndFinalFailedTestRun(int passedCount, int warningCount, int failedCount,
                                                                      int skippedCount, int stoppedCount, int notStartedCount,
                                                                      int inProgressCount, int blockedCount) {
        List<TestRun> testRuns = getTestRunsByCountAndStatusAndInitialId(TestingStatuses.PASSED, passedCount, initialUuid);
        testRuns.addAll(getTestRunsByCountAndStatusAndInitialId(TestingStatuses.WARNING, warningCount, initialUuid));
        testRuns.addAll(getTestRunsByCountAndStatusAndInitialIdAndFinalTestRun(TestingStatuses.FAILED, failedCount, initialUuid, true));
        testRuns.addAll(getTestRunsByCountAndStatusAndInitialId(TestingStatuses.SKIPPED, skippedCount, initialUuid));
        testRuns.addAll(getTestRunsByCountAndStatusAndInitialId(TestingStatuses.STOPPED, stoppedCount, initialUuid));
        testRuns.addAll(getTestRunsByCountAndStatusAndInitialId(TestingStatuses.NOT_STARTED, notStartedCount, initialUuid));
        testRuns.addAll(getTestRunsByCountAndStatus(ExecutionStatuses.IN_PROGRESS, inProgressCount));
        testRuns.addAll(getTestRunsByCountAndStatusAndInitialId(TestingStatuses.BLOCKED, blockedCount, initialUuid));
        return testRuns;
    }

    private List<TestRun> getTestRunsByCountAndStatus(TestingStatuses status, int count) {
        List<TestRun> testRuns = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            TestRun testRun = new TestRun();
            testRun.updateTestingStatus(status);
            testRuns.add(testRun);
        }
        return testRuns;
    }

    public List<TestRun> getTestRunsByCountAndStatusAndInitialId(TestingStatuses status, int count, UUID uuid) {
        List<TestRun> testRuns = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            TestRun testRun = new TestRun();
            testRun.setInitialTestRunId(uuid);
            testRun.updateTestingStatus(status);
            testRuns.add(testRun);
        }
        return testRuns;
    }

    private List<TestRun> getParentTestRunsByCountAndStatus(TestingStatuses status, int count, List<UUID> uuid) {
        List<TestRun> testRuns = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            TestRun testRun = new TestRun();
            testRun.setUuid(uuid.get(0));
            uuid.remove(0);
            testRun.updateTestingStatus(status);
            testRuns.add(testRun);
        }
        return testRuns;
    }

    private List<TestRun> getTestRunsByCountAndStatusAndInitialIdAndFinalTestRun(TestingStatuses status, int count, UUID uuid, boolean isFinalTestRun) {
        List<TestRun> testRuns = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            TestRun testRun = new TestRun();
            testRun.setInitialTestRunId(uuid);
            testRun.updateTestingStatus(status);
            testRun.setFinalTestRun(isFinalTestRun);
            testRuns.add(testRun);
        }
        return testRuns;
    }

    private List<TestRun> getTestRunsByCountAndStatus(ExecutionStatuses status, int count) {
        return IntStream.range(0, count)
                .mapToObj(num -> {
                    TestRun testRun = new TestRun();
                    testRun.setExecutionStatus(status);
                    return testRun;
                })
                .collect(Collectors.toList());
    }

    public static List<TestRun> generateTestRunsWithRootCause(RootCause rootCause, UUID erId, int count) {
        List<TestRun> testRuns = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            TestRun testRun = new TestRun();
            testRun.setExecutionRequestId(erId);
            testRun.setRootCauseId(rootCause.getUuid());

            testRuns.add(testRun);
        }

        return testRuns;
    }

    public static TestRunWithValidationLabelsResponse generateTestRunWithValidationLabelsResponse(
            UUID id, String name, String status, Set<String> validationLabels) {
        return new TestRunWithValidationLabelsResponse(id, name, status, validationLabels);
    }
}
