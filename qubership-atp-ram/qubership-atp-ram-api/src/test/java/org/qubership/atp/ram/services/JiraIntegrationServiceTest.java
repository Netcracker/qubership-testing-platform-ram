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
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.modelmapper.ModelMapper;
import org.qubership.atp.ram.EnvironmentsInfoMock;
import org.qubership.atp.ram.ExecutionRequestsMock;
import org.qubership.atp.ram.TestRunsMock;
import org.qubership.atp.ram.model.TestRunForRefreshFromJira;
import org.qubership.atp.ram.model.TestRunToJiraInfo;
import org.qubership.atp.ram.models.EnvironmentsInfo;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.repositories.ExecutionRequestRepository;
import org.qubership.atp.ram.repositories.TestRunRepository;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(SpringExtension.class)
public class JiraIntegrationServiceTest {

    @Mock
    TestRunRepository repositoryMock;
    @Mock
    TestRunService testRunServiceMock;
    @Mock
    ExecutionRequestRepository executionRequestRepositoryMock;
    @Mock
    CatalogueService catalogueService;
    @Mock
    EnvironmentsInfoService environmentsInfoServiceMock;
    @Spy
    ModelMapper modelMapperSpy = new ModelMapper();
    @Mock
    ObjectMapper objectMapper;

    String catalogueUrl = "http://atp-catalogue.com";

    @InjectMocks
    JiraIntegrationService jiraIntegrationService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(jiraIntegrationService);
        ReflectionTestUtils.setField(jiraIntegrationService, // inject into this object
                "catalogueUrl", // assign to this field
                catalogueUrl); // object to be injected
    }

    @Test
    public void testPropagateTestRunsToJira_shouldFormRequest_whenLastTestRun() {

        TestRun lastTestRun = TestRunsMock.generateSimpleTestRun(UUID.randomUUID());
        ExecutionRequest executionRequest = ExecutionRequestsMock.generateRequestById(lastTestRun.getExecutionRequestId());
        EnvironmentsInfo environmentsInfo = EnvironmentsInfoMock.generateEnvInfoByUuids(executionRequest.getUuid(),
                executionRequest.getEnvironmentId());
        List<UUID> testRunsUuids = Arrays.asList(lastTestRun.getUuid());
        List<UUID> executionRequestUuids = Arrays.asList(executionRequest.getUuid());

        List<TestRun> testRuns = Arrays.asList(lastTestRun);
        List<EnvironmentsInfo> environmentsInfos = Arrays.asList(environmentsInfo);
        List<ExecutionRequest> executionRequests = Arrays.asList(executionRequest);

        given(repositoryMock.findAllByUuidIn(testRunsUuids)).willReturn(testRuns);
        given(executionRequestRepositoryMock.findAllByUuidIn(executionRequestUuids)).willReturn(executionRequests);
        given(testRunServiceMock.getByTestCase(lastTestRun.getTestCaseId())).willReturn(lastTestRun);
        given(modelMapperSpy.map(lastTestRun, TestRunToJiraInfo.class)).willCallRealMethod();
        given(environmentsInfoServiceMock.findByRequestIds(executionRequestUuids)).willReturn(environmentsInfos);

        TestRunToJiraInfo expectedTestRunToJiraInfo = getExpectedTestRunToJiraRequest(lastTestRun, executionRequest);
        expectedTestRunToJiraInfo.setLastRun(true);

        List<TestRunToJiraInfo> actualTestRunInfo = jiraIntegrationService.getTestRunsForJiraInfoByIds(testRunsUuids);

        assertEquals(Arrays.asList(expectedTestRunToJiraInfo), actualTestRunInfo);
        verify(repositoryMock, times(1)).findAllByUuidIn(testRunsUuids);
        verify(executionRequestRepositoryMock, times(1)).findAllByUuidIn(executionRequestUuids);
        verify(testRunServiceMock, times(1)).getByTestCase(lastTestRun.getTestCaseId());
        verify(environmentsInfoServiceMock, times(1)).findByRequestIds(executionRequestUuids);
    }

    @Test
    public void testPropagateTestRunsToJira_shouldFormRequest_whenEnvListIsEmpty() {

        TestRun lastTestRun = TestRunsMock.generateSimpleTestRun(UUID.randomUUID());
        ExecutionRequest executionRequest = ExecutionRequestsMock.generateRequestById(lastTestRun.getExecutionRequestId());
        List<UUID> testRunsUuids = Arrays.asList(lastTestRun.getUuid());
        List<UUID> executionRequestUuids = Arrays.asList(executionRequest.getUuid());

        List<TestRun> testRuns = Arrays.asList(lastTestRun);
        List<ExecutionRequest> executionRequests = Arrays.asList(executionRequest);

        given(repositoryMock.findAllByUuidIn(testRunsUuids)).willReturn(testRuns);
        given(executionRequestRepositoryMock.findAllByUuidIn(executionRequestUuids)).willReturn(executionRequests);
        given(testRunServiceMock.getByTestCase(lastTestRun.getTestCaseId())).willReturn(lastTestRun);
        given(modelMapperSpy.map(lastTestRun, TestRunToJiraInfo.class)).willCallRealMethod();
        given(environmentsInfoServiceMock.findByRequestIds(executionRequestUuids)).willReturn(Collections.emptyList());

        TestRunToJiraInfo expectedTestRunToJiraInfo = getExpectedTestRunToJiraRequestEmptyEnvironmentInfo(lastTestRun,
                executionRequest);
        expectedTestRunToJiraInfo.setLastRun(true);

        List<TestRunToJiraInfo> actualTestRunInfo = jiraIntegrationService.getTestRunsForJiraInfoByIds(testRunsUuids);

        assertEquals(Arrays.asList(expectedTestRunToJiraInfo), actualTestRunInfo);
        verify(repositoryMock, times(1)).findAllByUuidIn(testRunsUuids);
        verify(executionRequestRepositoryMock, times(1)).findAllByUuidIn(executionRequestUuids);
        verify(testRunServiceMock, times(1)).getByTestCase(lastTestRun.getTestCaseId());
        verify(environmentsInfoServiceMock, times(1)).findByRequestIds(executionRequestUuids);
    }
    @Test
    public void testPropagateTestRunsToJira_shouldNotSendRequest_whenNotLastTestRunWithoutJira() {

        TestRun notLastRun = TestRunsMock.generateSimpleTestRun(UUID.randomUUID());
        notLastRun.setJiraTicket(null);
        TestRun lastTestRun = TestRunsMock.generateSimpleTestRun(UUID.randomUUID());

        ExecutionRequest executionRequest = ExecutionRequestsMock.generateRequestById(notLastRun.getExecutionRequestId());
        EnvironmentsInfo environmentsInfo = EnvironmentsInfoMock.generateEnvInfoByUuids(executionRequest.getUuid(),
                executionRequest.getEnvironmentId());
        List<UUID> testRunsUuids = Arrays.asList(notLastRun.getUuid());
        List<UUID> executionRequestUuids = Arrays.asList(executionRequest.getUuid());

        List<TestRun> testRuns = Arrays.asList(notLastRun);
        List<EnvironmentsInfo> environmentsInfos = Arrays.asList(environmentsInfo);
        List<ExecutionRequest> executionRequests = Arrays.asList(executionRequest);

        given(repositoryMock.findAllByUuidIn(testRunsUuids)).willReturn(testRuns);
        given(executionRequestRepositoryMock.findAllByUuidIn(executionRequestUuids)).willReturn(executionRequests);
        given(testRunServiceMock.getByTestCase(notLastRun.getTestCaseId())).willReturn(lastTestRun);
        given(modelMapperSpy.map(notLastRun, TestRunToJiraInfo.class)).willCallRealMethod();
        given(environmentsInfoServiceMock.findByRequestIds(executionRequestUuids)).willReturn(environmentsInfos);

        TestRunToJiraInfo expectedTestRunToJiraInfo = getExpectedTestRunToJiraRequest(notLastRun, executionRequest);
        expectedTestRunToJiraInfo.setLastRun(false);

        List<TestRunToJiraInfo> actualData = jiraIntegrationService.getTestRunsForJiraInfoByIds(testRunsUuids);

        assertEquals(Arrays.asList(expectedTestRunToJiraInfo), actualData);
        verify(repositoryMock, times(1)).findAllByUuidIn(testRunsUuids);
        verify(executionRequestRepositoryMock, times(1)).findAllByUuidIn(executionRequestUuids);
        verify(testRunServiceMock, times(1)).getByTestCase(notLastRun.getTestCaseId());
        verify(environmentsInfoServiceMock, times(1)).findByRequestIds(executionRequestUuids);
    }

    @Test
    public void testPropagateTestRunsToJira_shouldFormRequest_whenBothLastAndNotLastRunWithJiraTicket() {

        TestRun lastTestRun = TestRunsMock.generateSimpleTestRun(UUID.randomUUID());
        TestRun notLastTestRun = TestRunsMock.generateSimpleTestRun(UUID.randomUUID());
        //under one request
        notLastTestRun.setExecutionRequestId(lastTestRun.getExecutionRequestId());

        ExecutionRequest executionRequest = ExecutionRequestsMock.generateRequestById(lastTestRun.getExecutionRequestId());

        EnvironmentsInfo environmentsInfo = EnvironmentsInfoMock.generateEnvInfoByUuids(executionRequest.getUuid(),
                executionRequest.getEnvironmentId());
        List<UUID> testRunsUuids = Arrays.asList(lastTestRun.getUuid(), notLastTestRun.getUuid());
        List<UUID> executionRequestUuids = Arrays.asList(executionRequest.getUuid());

        List<TestRun> testRuns = Arrays.asList(lastTestRun, notLastTestRun);
        List<EnvironmentsInfo> environmentsInfos = Arrays.asList(environmentsInfo);
        List<ExecutionRequest> executionRequests = Arrays.asList(executionRequest);

        given(repositoryMock.findAllByUuidIn(testRunsUuids)).willReturn(testRuns);
        given(executionRequestRepositoryMock.findAllByUuidIn(executionRequestUuids)).willReturn(executionRequests);
        given(testRunServiceMock.getByTestCase(lastTestRun.getTestCaseId())).willReturn(lastTestRun);
        given(testRunServiceMock.getByTestCase(notLastTestRun.getTestCaseId())).willReturn(lastTestRun);
        given(modelMapperSpy.map(lastTestRun, TestRunToJiraInfo.class)).willCallRealMethod();
        given(environmentsInfoServiceMock.findByRequestIds(executionRequestUuids)).willReturn(environmentsInfos);

        TestRunToJiraInfo expectedTestRunToJiraInfo = getExpectedTestRunToJiraRequest(lastTestRun,
                executionRequest);
        expectedTestRunToJiraInfo.setLastRun(true);

        TestRunToJiraInfo expectedTestRunToJiraInfoNotLast = getExpectedTestRunToJiraRequest(notLastTestRun,
                executionRequest);
        expectedTestRunToJiraInfoNotLast.setLastRun(false);
        List<TestRunToJiraInfo> expectedData = Arrays.asList(expectedTestRunToJiraInfo, expectedTestRunToJiraInfoNotLast);

        List<TestRunToJiraInfo> actualData = jiraIntegrationService.getTestRunsForJiraInfoByIds(testRunsUuids);

        assertEquals(expectedData, actualData);
        verify(repositoryMock, times(1)).findAllByUuidIn(testRunsUuids);
        verify(executionRequestRepositoryMock, times(1)).findAllByUuidIn(executionRequestUuids);
        verify(testRunServiceMock, times(2)).getByTestCase(any(UUID.class));
        verify(environmentsInfoServiceMock, times(1)).findByRequestIds(executionRequestUuids);
    }

    private TestRunToJiraInfo getExpectedTestRunToJiraRequest(TestRun lastTestRun, ExecutionRequest executionRequest) {
        TestRunToJiraInfo expectedTestRunToJiraInfo = new TestRunToJiraInfo();
        expectedTestRunToJiraInfo.setUuid(lastTestRun.getUuid());
        expectedTestRunToJiraInfo.setName(lastTestRun.getName());
        expectedTestRunToJiraInfo.setJiraTicket(lastTestRun.getJiraTicket());
        expectedTestRunToJiraInfo.setTestingStatus(lastTestRun.getTestingStatus().toString());
        expectedTestRunToJiraInfo.setTestRunAtpLink(catalogueUrl + "/project/" + executionRequest.getProjectId()
                + "/ram/execution-request/" + executionRequest.getUuid()
                + "?node=" + lastTestRun.getUuid());
        expectedTestRunToJiraInfo.setTestCaseId(lastTestRun.getTestCaseId());
        expectedTestRunToJiraInfo.setExecutionRequestId(lastTestRun.getExecutionRequestId());
        expectedTestRunToJiraInfo.setEnvironmentInfo("||Environment Name|| Urls|| Version||\n" +
                "|qa1|[qa1.some-domain.com, qa1.dev.some-domain.com]|qa1Build|\n" +
                "|qa2|[qa2.some-domain.com, qa2.dev.some-domain.com]|qa2Build|\n" +
                "|qa3|[qa3.some-domain.com, qa3.dev.some-domain.com]|-|\n");
        return expectedTestRunToJiraInfo;
    }

    private TestRunToJiraInfo getExpectedTestRunToJiraRequestEmptyEnvironmentInfo(TestRun lastTestRun,
                                                                ExecutionRequest executionRequest) {
        TestRunToJiraInfo expectedTestRunToJiraInfo = new TestRunToJiraInfo();
        expectedTestRunToJiraInfo.setUuid(lastTestRun.getUuid());
        expectedTestRunToJiraInfo.setName(lastTestRun.getName());
        expectedTestRunToJiraInfo.setJiraTicket(lastTestRun.getJiraTicket());
        expectedTestRunToJiraInfo.setTestingStatus(lastTestRun.getTestingStatus().toString());
        expectedTestRunToJiraInfo.setTestRunAtpLink(catalogueUrl + "/project/" + executionRequest.getProjectId()
                + "/ram/execution-request/" + executionRequest.getUuid()
                + "?node=" + lastTestRun.getUuid());
        expectedTestRunToJiraInfo.setTestCaseId(lastTestRun.getTestCaseId());
        expectedTestRunToJiraInfo.setExecutionRequestId(lastTestRun.getExecutionRequestId());
        expectedTestRunToJiraInfo.setEnvironmentInfo("Environments Info is not available");
        return expectedTestRunToJiraInfo;
    }
    @Test
    public void testGetTestRunsForRefreshFromJira_shouldReturnTestRunInfo_whenJiraTicketExists() {
        TestRun testRunLast = TestRunsMock.generateSimpleTestRun(UUID.randomUUID());
        TestRun testRunNotLast = TestRunsMock.generateSimpleTestRun(UUID.randomUUID());

        List<TestRun> testRuns = Arrays.asList(testRunLast, testRunNotLast);
        List<UUID> testRunUuids = Arrays.asList(testRunLast.getUuid(), testRunNotLast.getUuid());

        List<TestRunForRefreshFromJira> expectedInfo = getExpectedTestRunForRefreshJiraInfo(testRunLast, testRunNotLast);

        when(repositoryMock.findAllByUuidIn(testRunUuids)).thenReturn(testRuns);
        when(testRunServiceMock.getByTestCase(testRunLast.getTestCaseId())).thenReturn(testRunLast);
        when(testRunServiceMock.getByTestCase(testRunNotLast.getTestCaseId())).thenReturn(testRunLast);

        List<TestRunForRefreshFromJira> actualInfo = jiraIntegrationService.getTestRunsForRefreshFromJira(testRunUuids);
        assertEquals(expectedInfo, actualInfo, "Actual TestRun info for refresh from Jira is different from expected");
    }

    private List<TestRunForRefreshFromJira> getExpectedTestRunForRefreshJiraInfo(TestRun testRunLast, TestRun testRunNotLast) {
        List<TestRunForRefreshFromJira> expectedTestRunsForRefresh = new ArrayList<>();
        TestRunForRefreshFromJira lastRun = new TestRunForRefreshFromJira();
        lastRun.setUuid(testRunLast.getUuid());
        lastRun.setName(testRunLast.getName());
        lastRun.setTestCaseId(testRunLast.getTestCaseId());
        lastRun.setJiraTicket(testRunLast.getJiraTicket());
        lastRun.setLastRun(true);
        expectedTestRunsForRefresh.add(lastRun);

        TestRunForRefreshFromJira notLastRun = new TestRunForRefreshFromJira();
        notLastRun.setUuid(testRunNotLast.getUuid());
        notLastRun.setName(testRunNotLast.getName());
        notLastRun.setTestCaseId(testRunNotLast.getTestCaseId());
        notLastRun.setJiraTicket(testRunNotLast.getJiraTicket());
        notLastRun.setLastRun(false);
        expectedTestRunsForRefresh.add(notLastRun);

        return expectedTestRunsForRefresh;
    }

    @Test
    public void testGetTestRunsForRefreshFromJira_shouldReturnTestRunInfo_whenJiraTicketNotExist() {
        TestRun testRunLast = TestRunsMock.generateTestRun("Test run without Jira");
        TestRun testRunNotLast = TestRunsMock.generateTestRun("Test run 2 without Jira");

        List<TestRun> testRuns = Arrays.asList(testRunLast, testRunNotLast);
        List<UUID> testRunUuids = Arrays.asList(testRunLast.getUuid(), testRunNotLast.getUuid());

        List<TestRunForRefreshFromJira> expectedInfo = getExpectedTestRunForRefreshJiraInfo(testRunLast,
                testRunNotLast);

        when(repositoryMock.findAllByUuidIn(testRunUuids)).thenReturn(testRuns);
        when(testRunServiceMock.getByTestCase(testRunLast.getTestCaseId())).thenReturn(testRunLast);
        when(testRunServiceMock.getByTestCase(testRunNotLast.getTestCaseId())).thenReturn(testRunLast);

        List<TestRunForRefreshFromJira> actualInfo = jiraIntegrationService.getTestRunsForRefreshFromJira(testRunUuids);
        assertEquals(expectedInfo, actualInfo, "Actual TestRun info for refresh from Jira is different from expected");
    }
}
