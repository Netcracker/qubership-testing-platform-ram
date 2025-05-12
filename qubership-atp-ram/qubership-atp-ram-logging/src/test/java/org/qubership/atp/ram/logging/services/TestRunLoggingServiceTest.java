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

import java.sql.Timestamp;
import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.logging.constants.ApiPathLogging;
import org.qubership.atp.ram.logging.entities.requests.CreatedTestRunRequest;
import org.qubership.atp.ram.logging.entities.requests.CreatedTestRunWithParentsRequest;
import org.qubership.atp.ram.logging.entities.requests.StopTestRunRequest;
import org.qubership.atp.ram.logging.entities.responses.CreatedTestRunResponse;
import org.qubership.atp.ram.logging.entities.responses.StopTestRunResponse;
import org.qubership.atp.ram.logging.services.mocks.ModelMocks;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.repositories.TestRunRepository;
import org.qubership.atp.ram.services.IssueService;
import org.springframework.util.CollectionUtils;

public class TestRunLoggingServiceTest {

    private TestRunRepository testRunRepository;
    private TestRunLoggingService testRunLoggingService;

    private CreatedTestRunWithParentsRequest request;
    private ExecutionRequest executionRequest;
    private IssueService issueService;

    @BeforeEach
    public void setUp() {
        testRunRepository = mock(TestRunRepository.class);
        ProjectLoggingService projectLoggingService = mock(ProjectLoggingService.class);
        TestPlanLoggingService testPlanLoggingService = mock((TestPlanLoggingService.class));
        ExecutionRequestLoggingService executionRequestLoggingService = mock(ExecutionRequestLoggingService.class);
        issueService = mock(IssueService.class);

        ModelMapper modelMapper = new ModelMapper();
        testRunLoggingService = spy(new TestRunLoggingService(
                projectLoggingService, testPlanLoggingService, executionRequestLoggingService,
                testRunRepository, modelMapper, issueService));

        request = ModelMocks.generateCreatedTestRunWithParentsRequest();
        executionRequest = ModelMocks.generateExecutionRequest();
    }

    @Test
    public void configuredCreatedOrExistedTestRun_WhenTestRunIsNotExist_ShouldReturnCreatedTr() {
        TestRun expectedTestRun = ModelMocks.generateExpectedTestRun(request, executionRequest.getUuid());

        when(testRunRepository.save(any())).thenReturn(expectedTestRun);
        TestRun actualTestRun = testRunLoggingService.configuredCreatedOrExistedTestRun(request,
                executionRequest.getUuid());

        Assertions.assertEquals(expectedTestRun, actualTestRun, "Created test run is valid");
    }

    @Test
    public void configuredCreatedOrExistedTestRun_WhenTestRunIsExist_ShouldReturnUpdatedTr() {
        TestRun existsTestRun = ModelMocks.generateExistTestRun();
        request.setAtpExecutionRequestId(null);
        TestRun expectedTestRun = ModelMocks.generateExpectedTestRun(request, executionRequest.getUuid());

        when(testRunRepository.findByExecutionRequestIdAndName(any(), any()))
                .thenReturn(existsTestRun);
        when(testRunRepository.save(any())).thenReturn(expectedTestRun);
        TestRun actualTestRun = testRunLoggingService.configuredCreatedOrExistedTestRun(request,
                executionRequest.getUuid());

        Assertions.assertFalse(CollectionUtils.isEmpty(actualTestRun.getTaHost()), "TA hosts are not empty");
        Assertions.assertFalse(CollectionUtils.isEmpty(actualTestRun.getQaHost()), "QA hosts are not empty");
        Assertions.assertFalse(
                CollectionUtils.isEmpty(actualTestRun.getSolutionBuild()), "Solution build are not empty");
        Assertions.assertEquals(existsTestRun.getUuid(), actualTestRun.getUuid(), "UUID is not change");
        Assertions.assertEquals(executionRequest.getUuid(),
                actualTestRun.getExecutionRequestId(), "ER ID is not change");
    }

    @Test
    public void update_RequestHasChangedFields_ShouldReturnUpdatedTr() {
        TestRun existsTestRun = ModelMocks.generateExistTestRun();
        CreatedTestRunRequest request = ModelMocks.generateCreatedTestRunRequest();

        TestRun expectedTestRun = ModelMocks.generateExpectedTestRun(request, executionRequest.getUuid());
        when(testRunRepository.findByUuid(any())).thenReturn(existsTestRun);
        when(testRunRepository.save(any())).thenReturn(expectedTestRun);

        CreatedTestRunResponse response = testRunLoggingService.update(request);

        Assertions.assertFalse(CollectionUtils.isEmpty(expectedTestRun.getTaHost()), "TA hosts are not empty");
        Assertions.assertFalse(CollectionUtils.isEmpty(expectedTestRun.getQaHost()), "QA hosts are not empty");
        Assertions.assertFalse(CollectionUtils.isEmpty(existsTestRun.getSolutionBuild()), "Solution build are not empty");
        Assertions.assertEquals(existsTestRun.getUuid(), expectedTestRun.getUuid(), "UUID is not change");
        Assertions.assertEquals(executionRequest.getUuid(),
                existsTestRun.getExecutionRequestId(), "ER ID is not change");
        Assertions.assertNotEquals(existsTestRun.getUrlToBrowserOrLogs(),
                expectedTestRun.getExecutionRequestId(), "Url to browser will changing");


        Assertions.assertEquals(existsTestRun.getUuid(), response.getTestRunId(), "Response should has testRunId");
        Assertions.assertNull(response.getExecutionRequestId(), "Response shouldn't has executionRequestId");
    }

    @Test
    public void stop_ShouldReturnValidResponse() {
        TestRun existsTestRun = ModelMocks.generateExistTestRun();
        when(testRunRepository.findByUuid(any())).thenReturn(existsTestRun);
        StopTestRunRequest stopTestRunRequest = ModelMocks.generateStopTestRunRequest();
        StopTestRunResponse stopTestRunResponse = testRunLoggingService.stop(stopTestRunRequest);
        Assertions.assertEquals(ExecutionStatuses.FINISHED.toString(),
                stopTestRunResponse.getExecutionStatus(), "Response has valid execution status");
    }

    @Test
    public void getUrlToBrowserLog_ShouldValidPreparedUrlToLog() {
        TestRun existsTestRun = ModelMocks.generateExistTestRun();
        existsTestRun.setFinishDate(new Timestamp(System.currentTimeMillis() + 1000L));
        Set<String> urlForPreparing = Collections.singleton("http://graylog.some-domain.com/streams/" +
                "000000000000000000000001/search?rangetype=absolute" +
                "&from=" + ApiPathLogging.START_DATE +
                "&to=" + ApiPathLogging.END_DATE +
                "&q=\"bdd3b8083ab4ce63fc4948ee62044640\"");
        Set<String> actualUrl = testRunLoggingService.getUrlToBrowserLog(existsTestRun, urlForPreparing);
        Assertions.assertFalse(CollectionUtils.isEmpty(actualUrl), "Preparing urls should not be empty");
        Assertions.assertFalse(actualUrl.contains(ApiPathLogging.START_DATE),
                "Preparing urls should replace '" + ApiPathLogging.START_DATE + "'");
        Assertions.assertFalse(actualUrl.contains(ApiPathLogging.END_DATE),
                "Preparing urls should replace '" + ApiPathLogging.END_DATE + "'");

    }
}
