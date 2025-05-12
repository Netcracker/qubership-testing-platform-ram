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

package org.qubership.atp.ram.logging.services.mocks;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.logging.entities.requests.CreatedLogRecordRequest;
import org.qubership.atp.ram.logging.entities.requests.CreatedTestRunRequest;
import org.qubership.atp.ram.logging.entities.requests.CreatedTestRunWithParentsRequest;
import org.qubership.atp.ram.logging.entities.requests.StopTestRunRequest;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.TestPlan;
import org.qubership.atp.ram.models.TestRun;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ModelMocks {
    public final static UUID ATP_EXECUTION_REQUEST_ID = UUID.fromString("1ccf9445-095e-4022-bc02-041d4fbc5773");
    private final static UUID PROJECT_ID = UUID.fromString("9f052227-79d7-4f3f-bd55-aeb2efbcb103");
    private final static UUID TEST_PLAN_ID = UUID.fromString("9ca354ba-1c1d-4601-84ad-bdd598470964");
    private final UUID testRunUuid = UUID.randomUUID();
    private final UUID logRecordUuid = UUID.randomUUID();
    private final ModelMapper modelMapper = new ModelMapper();

    public CreatedTestRunWithParentsRequest generateCreatedTestRunWithParentsRequest() {
        CreatedTestRunWithParentsRequest request = new CreatedTestRunWithParentsRequest();
        request.setProjectName("[VFHU]");
        request.setTestPlanName("Trunk AT (new)");
        request.setTestCaseId(UUID.fromString("dfe5aeb9-476d-4751-b558-fa816cb8a922"));
        request.setTestCaseName("testcase");
        request.setExecutionRequestName("[Create SO with Vodafone Internet 300");
        request.setTestRunName("[Create SO with Vodafone Internet 300");
        request.setTaHost(Collections.singletonList("debug-fd30c895-a87f-4b4b-8bf7-85d1e6da089c"));
        request.setQaHost(Collections.singletonList("http://127.0.0.1:9876"));
        request.setExecutor("?");
        request.setSolutionBuild(Collections.singletonList("325_VFHU.Pseudo_Localization_rev14270"));
        request.setAtpExecutionRequestId(ATP_EXECUTION_REQUEST_ID);
        request.setProjectId(PROJECT_ID);
        request.setTestPlanId(TEST_PLAN_ID);
        return request;
    }

    public CreatedTestRunRequest generateCreatedTestRunRequest() {
        CreatedTestRunRequest request = modelMapper.map(generateCreatedTestRunWithParentsRequest(),
                CreatedTestRunRequest.class);
        request.setTestingStatus("FAILED");
        request.setExecutionStatus("FINISHED");
        request.setUrlToBrowserOrLogs(Collections.singleton("<url>"));
        return request;
    }

    public ExecutionRequest generateExecutionRequest(UUID id) {
        ExecutionRequest executionRequest = new ExecutionRequest();
        executionRequest.setUuid(id);
        executionRequest.setName("[Create SO with Vodafone Internet 300");
        executionRequest.setExecutionStatus(ExecutionStatuses.IN_PROGRESS);
        executionRequest.setStartDate(new Timestamp(System.currentTimeMillis()));
        return executionRequest;
    }

    public ExecutionRequest generateExecutionRequest() {
        ExecutionRequest executionRequest = new ExecutionRequest();
        executionRequest.setUuid(ATP_EXECUTION_REQUEST_ID);
        executionRequest.setName("[Create SO with Vodafone Internet 300");
        executionRequest.setExecutionStatus(ExecutionStatuses.IN_PROGRESS);
        executionRequest.setStartDate(new Timestamp(System.currentTimeMillis()));
        return executionRequest;
    }

    public TestPlan generateTestPlan() {
        TestPlan testPlan = new TestPlan();
        testPlan.setProjectId(PROJECT_ID);
        testPlan.setUuid(TEST_PLAN_ID);
        testPlan.setName("Trunk AT (new)");
        return testPlan;
    }

    public TestRun generateExistTestRun() {
        TestRun testRun = new TestRun();
        testRun.setStartDate(new Timestamp(System.currentTimeMillis()));
        testRun.setExecutionRequestId(ATP_EXECUTION_REQUEST_ID);
        testRun.setTestCaseId(UUID.fromString("dfe5aeb9-476d-4751-b558-fa816cb8a922"));
        testRun.setTestCaseName("testcase");
        testRun.setName("[Create SO with Vodafone Internet 300");
        testRun.setExecutor("?");
        testRun.setUuid(testRunUuid);
        return testRun;
    }

    public TestRun generateExpectedTestRun(CreatedTestRunRequest request, UUID executionRequestId) {
        TestRun expectedTestRun = new ModelMapper().map(request, TestRun.class);
        expectedTestRun.setExecutionRequestId(executionRequestId);
        expectedTestRun.setExecutionStatus(ExecutionStatuses.IN_PROGRESS);
        expectedTestRun.setStartDate(new Timestamp(System.currentTimeMillis()));
        expectedTestRun.updateTestingStatus(TestingStatuses.UNKNOWN);
        expectedTestRun.setName(request.getTestRunName());
        expectedTestRun.setUuid(testRunUuid);
        expectedTestRun.setUrlToBrowserOrLogs(Collections.singleton("<url>"));
        return expectedTestRun;
    }

    public StopTestRunRequest generateStopTestRunRequest() {
        StopTestRunRequest stopTestRunRequest = new StopTestRunRequest();
        stopTestRunRequest.setTestingStatus(TestingStatuses.FAILED.toString());
        stopTestRunRequest.setTestRunId(testRunUuid);
        stopTestRunRequest.setUrlToBrowserOrLogs(Collections.singleton("http"));
        return stopTestRunRequest;
    }

    public LogRecord generateExistedLogRecord() {
        LogRecord logRecord = new LogRecord();
        logRecord.setUuid(logRecordUuid);
        logRecord.setName("LR");
        logRecord.setCompaund(false);
        logRecord.setSection(true);
        logRecord.setTestRunId(testRunUuid);
        logRecord.setParentRecordId(UUID.randomUUID());
        logRecord.setTestingStatus(TestingStatuses.FAILED);
        logRecord.setMessage("str");
        return logRecord;
    }

    public CreatedLogRecordRequest generatedCreatedLogRecordRequest() {
        CreatedLogRecordRequest createdLogRecordRequest = new CreatedLogRecordRequest();
        createdLogRecordRequest.setLogRecordUuid(logRecordUuid);
        createdLogRecordRequest.setTestRunId(testRunUuid);
        createdLogRecordRequest.setName("str");
        createdLogRecordRequest.setStartDate(new Timestamp(System.currentTimeMillis()).toString());
        createdLogRecordRequest.setParentRecordUuid(UUID.randomUUID());
        createdLogRecordRequest.setTestingStatus(TestingStatuses.FAILED.toString());
        return createdLogRecordRequest;
    }
}
