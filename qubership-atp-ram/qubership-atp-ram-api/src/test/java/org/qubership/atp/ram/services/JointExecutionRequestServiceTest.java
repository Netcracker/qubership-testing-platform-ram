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

import static java.sql.Timestamp.valueOf;
import static java.time.LocalDateTime.now;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.qubership.atp.ram.dto.request.JointExecutionRequestSearchRequest;
import org.qubership.atp.ram.dto.response.JointExecutionRequestSearchResponse;
import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.exceptions.executionrequests.RamMultipleActiveJointExecutionRequestsException;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.JointExecutionRequest;
import org.qubership.atp.ram.models.JointExecutionRequest.Run;
import org.qubership.atp.ram.models.JointExecutionRequest.Status;
import org.qubership.atp.ram.repositories.JointExecutionRequestRepository;
import org.qubership.atp.ram.utils.RateCalculator;
import org.qubership.atp.ram.utils.StreamUtils;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class JointExecutionRequestServiceTest {

    @InjectMocks
    private JointExecutionRequestService service;

    @Mock
    private ExecutionRequestService executionRequestService;

    @Mock
    private TestRunService testRunService;

    @Mock
    private EnvironmentsInfoService environmentsInfoService;

    @Mock
    private EnvironmentsService environmentsService;

    @Mock
    private RateCalculator rateCalculator;

    @Mock
    private JointExecutionRequestRepository repository;

    @Captor
    private ArgumentCaptor<JointExecutionRequest> jointExecutionRequestCaptor;

    private String jointExecutionKey = "run_1";
    private ExecutionRequest executionRequest;
    private UUID executionRequestId;

    @BeforeEach
    public void setUp() {

        executionRequest = newExecutionRequest(ExecutionStatuses.IN_PROGRESS, jointExecutionKey, 3600, 1);
        executionRequestId = executionRequest.getUuid();

        when(executionRequestService.get(executionRequestId)).thenReturn(executionRequest);
    }

    @Test
    public void updateActiveJointExecutionRequestTest() {
        JointExecutionRequest existedJointExecutionRequest = new JointExecutionRequest();
        existedJointExecutionRequest.setKey(jointExecutionKey);
        existedJointExecutionRequest.setTimeout(60);
        existedJointExecutionRequest.setCount(2);
        existedJointExecutionRequest.setStatus(Status.IN_PROGRESS);
        existedJointExecutionRequest.setStartDate(valueOf(now()));

        List<JointExecutionRequest> jointExecutionRequests = singletonList(existedJointExecutionRequest);

        when(repository.findAllActiveJointExecutionRequestsByKey(jointExecutionKey)).thenReturn(jointExecutionRequests);
        when(repository.save(jointExecutionRequestCaptor.capture())).thenReturn(existedJointExecutionRequest);

        service.updateActiveJointExecutionRequest(executionRequest);

        final JointExecutionRequest updatedJointExecutionRequest = jointExecutionRequestCaptor.getValue();
        assertNotNull(updatedJointExecutionRequest);
        assertEquals(executionRequest.getJointExecutionKey(), updatedJointExecutionRequest.getKey());
        assertEquals(executionRequest.getJointExecutionTimeout(), updatedJointExecutionRequest.getTimeout());
        assertEquals(executionRequest.getJointExecutionCount(), updatedJointExecutionRequest.getCount());

        final List<Run> runs = updatedJointExecutionRequest.getRuns();
        assertFalse(isEmpty(runs));
        assertEquals(1, runs.size());

        final Run run = runs.get(0);
        assertEquals(executionRequest.getUuid(), run.getExecutionRequestId());
        assertEquals(executionRequest.getExecutionStatus(), run.getStatus());
    }

    @Test
    public void createJointExecutionRequest() {
        service.createJointExecutionRequest(executionRequest);

        verify(repository, times(1)).save(jointExecutionRequestCaptor.capture());

        final JointExecutionRequest savedJointExecutionRequest = jointExecutionRequestCaptor.getValue();
        assertNotNull(savedJointExecutionRequest);
        assertEquals(executionRequest.getJointExecutionKey(), savedJointExecutionRequest.getKey());
        assertEquals(executionRequest.getJointExecutionTimeout(), savedJointExecutionRequest.getTimeout());
        assertEquals(executionRequest.getJointExecutionCount(), savedJointExecutionRequest.getCount());
        assertEquals(Status.IN_PROGRESS, savedJointExecutionRequest.getStatus());

        final List<Run> runs = savedJointExecutionRequest.getRuns();
        assertFalse(isEmpty(runs));
        assertEquals(1, runs.size());
    }

    @Test
    public void isJointExecutionRequest() {
        boolean isJointExecutionRequest = service.isJointExecutionRequest(executionRequest.getUuid());

        assertTrue(isJointExecutionRequest);
    }

    @Test
    public void isJointExecutionRequestReady_whenCountIsZero() {
        JointExecutionRequest jointExecutionRequest = new JointExecutionRequest();
        jointExecutionRequest.setCount(0);

        boolean isJointExecutionRequestReady = service.isJointExecutionRequestReady(jointExecutionRequest);

        assertFalse(isJointExecutionRequestReady);
    }

    @Test
    public void isJointExecutionRequestReady_whenItsNoReady() {
        JointExecutionRequest jointExecutionRequest = newExecutionRequest(jointExecutionKey, 1, 3600, Status.IN_PROGRESS);

        ExecutionRequest executionRequest = newExecutionRequest(ExecutionStatuses.IN_PROGRESS, jointExecutionKey, 0, 0);
        JointExecutionRequest.Run run = new Run(executionRequest);
        List<Run> runs = new ArrayList<>();
        runs.add(run);

        jointExecutionRequest.setRuns(runs);

        final boolean isJointExecutionRequestReady = service.isJointExecutionRequestReady(jointExecutionRequest);
        assertFalse(isJointExecutionRequestReady);
    }

    @Test
    public void isJointExecutionRequestReady_whenItsReady() {
        JointExecutionRequest jointExecutionRequest = newExecutionRequest(jointExecutionKey, 1, 3600, Status.COMPLETED);

        ExecutionRequest executionRequest1 = newExecutionRequest(ExecutionStatuses.FINISHED, jointExecutionKey, 0, 0);
        JointExecutionRequest.Run run1 = new Run(executionRequest1);
        List<Run> runs = new ArrayList<>();
        runs.add(run1);

        jointExecutionRequest.setRuns(runs);

        final boolean isJointExecutionRequestReady = service.isJointExecutionRequestReady(jointExecutionRequest);
        assertTrue(isJointExecutionRequestReady);
    }

    @Test
    public void getActiveJointExecutionRequest() {
        JointExecutionRequest jointExecutionRequest = newExecutionRequest(jointExecutionKey, 0, 3600, Status.IN_PROGRESS);

        when(repository.findAllActiveJointExecutionRequestsByKey(jointExecutionKey)).thenReturn(singletonList(jointExecutionRequest));
        when(repository.save(jointExecutionRequestCaptor.capture())).thenReturn(jointExecutionRequest);

        JointExecutionRequest activeJointExecutionRequest = service.getActiveJointExecutionRequest(executionRequestId);

        JointExecutionRequest actualJointExecutionRequest = jointExecutionRequestCaptor.getValue();
        assertNotNull(activeJointExecutionRequest);
        assertNotNull(actualJointExecutionRequest);
        assertEquals(jointExecutionRequest, activeJointExecutionRequest);
    }

    @Test
    public void getActiveJointExecutionRequest_whenTwoActiveJointExecutionRequestPresent() {
        JointExecutionRequest jointExecutionRequest1 = newExecutionRequest(jointExecutionKey, 0, 3600, Status.IN_PROGRESS);
        JointExecutionRequest jointExecutionRequest2 = newExecutionRequest(jointExecutionKey, 0, 3600, Status.IN_PROGRESS);

        when(repository.findAllActiveJointExecutionRequestsByKey(jointExecutionKey)).thenReturn(asList(jointExecutionRequest1, jointExecutionRequest2));

        Assertions.assertThrows(RamMultipleActiveJointExecutionRequestsException.class, () -> {
            service.getActiveJointExecutionRequest(executionRequestId);
        });
    }

    @Test
    public void getJointExecutionRequest_whenOneJointExecutionRequestPresent() {
        // given
        final JointExecutionRequest jointExecutionRequest = new JointExecutionRequest();
        jointExecutionRequest.setKey(jointExecutionKey);

        when(repository.findAllByKey(jointExecutionKey)).thenReturn(singletonList(jointExecutionRequest));

        // when
        final JointExecutionRequest searchedJointExecutionRequest = service.getJointExecutionRequest(jointExecutionKey);

        // then
        assertNotNull(searchedJointExecutionRequest);
        assertEquals(jointExecutionKey, searchedJointExecutionRequest.getKey());
    }

    @Test
    public void getJointExecutionRequest_whenSeveralJointExecutionRequestsPresent() {
        // given
        final JointExecutionRequest jointExecutionRequest1 = new JointExecutionRequest();
        final JointExecutionRequest jointExecutionRequest2 = new JointExecutionRequest();

        when(repository.findAllByKey(jointExecutionKey)).thenReturn(asList(jointExecutionRequest1, jointExecutionRequest2));

        // when
        Assertions.assertThrows(IllegalStateException.class, () -> {
            service.getJointExecutionRequest(jointExecutionKey);
        });
    }

    @Test
    public void search_whenSeveralJointExecutionRequestsPresent() {
        // given
        final ExecutionRequest executionRequest1 = newExecutionRequest(ExecutionStatuses.FINISHED, jointExecutionKey, 0, 0);
        final ExecutionRequest executionRequest2 = newExecutionRequest(ExecutionStatuses.FINISHED, jointExecutionKey, 2, 3600);
        final List<ExecutionRequest> executionRequests = asList(executionRequest1, executionRequest2);

        final JointExecutionRequest jointExecutionRequest = new JointExecutionRequest();
        jointExecutionRequest.setKey(jointExecutionKey);
        jointExecutionRequest.upsertRun(executionRequest1.getUuid(), executionRequest1.getExecutionStatus());
        jointExecutionRequest.upsertRun(executionRequest2.getUuid(), executionRequest2.getExecutionStatus());

        final JointExecutionRequestSearchRequest request = new JointExecutionRequestSearchRequest();
        request.setKey(jointExecutionKey);

        when(repository.findAllByKey(jointExecutionKey)).thenReturn(singletonList(jointExecutionRequest));
        when(executionRequestService.getExecutionRequestsByIds(any())).thenReturn(executionRequests);

        // when
        final List<JointExecutionRequestSearchResponse> result = service.search(request);

        // then
        verify(executionRequestService, times(1)).getExecutionRequestsByIds(any());

        assertNotNull(result, "Search result shouldn't be nullable");
        assertFalse(result.isEmpty(), "Search result shouldn't be empty");

        final Set<UUID> executionRequestIds = StreamUtils.extractIds(executionRequests);
        final Set<UUID> resultIds = StreamUtils.extractIds(result, JointExecutionRequestSearchResponse::getId);
        assertThat(resultIds).hasSameElementsAs(executionRequestIds);

        final Set<String> executionRequestNames = StreamUtils.extractFields(executionRequests, ExecutionRequest::getName);
        final Set<String> resultNames = StreamUtils.extractFields(result, JointExecutionRequestSearchResponse::getName);
        assertThat(resultNames).hasSameElementsAs(executionRequestNames);
    }


    private JointExecutionRequest newExecutionRequest(String key, Integer count, Integer timeout, Status status) {
        JointExecutionRequest jointExecutionRequest = new JointExecutionRequest();
        jointExecutionRequest.setUuid(UUID.randomUUID());
        jointExecutionRequest.setKey(key);
        jointExecutionRequest.setTimeout(timeout);
        jointExecutionRequest.setCount(count);
        jointExecutionRequest.setStatus(status);
        jointExecutionRequest.setStartDate(valueOf(now()));

        return jointExecutionRequest;
    }

    private ExecutionRequest newExecutionRequest(ExecutionStatuses status, String jointExecutionKey,
                                                 Integer jointExecutionCount, Integer jointExecutionTimeout) {
        ExecutionRequest executionRequest = new ExecutionRequest();
        executionRequest.setUuid(UUID.randomUUID());
        executionRequest.setExecutionStatus(status);
        executionRequest.setJointExecutionKey(jointExecutionKey);
        executionRequest.setJointExecutionTimeout(jointExecutionTimeout);
        executionRequest.setJointExecutionCount(jointExecutionCount);

        return executionRequest;
    }
}

