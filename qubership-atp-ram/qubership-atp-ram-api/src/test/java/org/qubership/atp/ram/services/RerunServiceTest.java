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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.qubership.atp.ram.clients.api.dto.catalogue.RerunRequestDto;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.model.request.RerunRequest;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.TestRun;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class RerunServiceTest {
    private final static UUID finalExecutionRequestId = UUID.randomUUID();
    @InjectMocks
    RerunService rerunService;
    @Mock
    TestRunService testRunService;
    @Mock
    OrchestratorService orchestratorService;
    @Mock
    ExecutionRequestService executionRequestService;
    @Mock
    CatalogueService catalogueService;

    @Captor
    ArgumentCaptor<List<UUID>> listArgumentCaptor;

    @Test
    public void testRerunTestRuns_testRunsInReverseOrderList_testRunsShouldBeSortedByOrderBeforeSentToOrchestrator() {
        List<UUID> testRunIds = new ArrayList<>();
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();
        UUID id4 = UUID.randomUUID();
        testRunIds.add(id1);
        testRunIds.add(id2);
        testRunIds.add(id3);
        testRunIds.add(id4);
        List<TestRun> listOfTestRuns = createTestRuns(testRunIds);
        when(testRunService.findAllByUuidInAndExecutionStatusIn(any(), any())).thenReturn(listOfTestRuns);

        rerunService.rerunTestRuns(testRunIds);

        Mockito.verify(orchestratorService, Mockito.times(1))
                .rerunTestRuns(any(), any(), listArgumentCaptor.capture());
        List<UUID> sortedTestRuns = listArgumentCaptor.getValue();
        Assertions.assertEquals(id4, sortedTestRuns.get(0));
        Assertions.assertEquals(id3, sortedTestRuns.get(1));
        Assertions.assertEquals(id2, sortedTestRuns.get(2));
        Assertions.assertEquals(id1, sortedTestRuns.get(3));
    }

    private List<TestRun> createTestRuns(List<UUID> testRunIds) {
        List<TestRun> listOfTestRuns = new ArrayList<>();
        int order = testRunIds.size();
        for (int i = 0; i < testRunIds.size(); i++, order--) {
            TestRun testRun = new TestRun();
            testRun.setUuid(testRunIds.get(i));
            testRun.setName("TestRun " + i);
            testRun.setOrder(order);
            testRun.setExecutionRequestId(finalExecutionRequestId);
            listOfTestRuns.add(testRun);
        }
        return listOfTestRuns;
    }

    @Test
    public void testRerunByFilter_WithOnTr_ValidRerunRequestWasCreated() {
        UUID erId = UUID.randomUUID();
        RerunRequest rerunRequest = new RerunRequest(erId, Collections.singletonList(TestingStatuses.FAILED));
        ExecutionRequest executionRequest = createExecutionRequest(erId);
        when(executionRequestService.get(any())).thenReturn(executionRequest);
        TestRun testRun = new TestRun();
        testRun.setUuid(UUID.randomUUID());
        when(testRunService.getTestRunsIdByExecutionRequestIdAndTestingStatuses(any(), any()))
                .thenReturn(Collections.singletonList(testRun));

        rerunService.rerunByFilter(rerunRequest);

        ArgumentCaptor<RerunRequestDto> rerunRequestDtoArgumentCaptor = ArgumentCaptor.forClass(RerunRequestDto.class);
        verify(catalogueService, times(1)).rerunExecutionRequest(rerunRequestDtoArgumentCaptor.capture());

        RerunRequestDto rerunRequestDto = rerunRequestDtoArgumentCaptor.getValue();
        Assertions.assertEquals(executionRequest.getUuid(), rerunRequestDto.getExecutionRequestId());
        Assertions.assertEquals(executionRequest.getProjectId(), rerunRequestDto.getProjectId());
        Assertions.assertEquals(executionRequest.getThreads(), rerunRequestDto.getThreads().intValue());
        Assertions.assertEquals(executionRequest.getEnvironmentId(), rerunRequestDto.getEnvironmentId());
        Assertions.assertEquals(executionRequest.getTaToolsGroupId(), rerunRequestDto.getTaToolsGroupId());
        Assertions.assertEquals(executionRequest.getTestScopeId(), rerunRequestDto.getScopeId());
        Assertions.assertEquals(1, rerunRequestDto.getTestRunIds().size());
    }

    private ExecutionRequest createExecutionRequest(UUID erId) {
        ExecutionRequest executionRequest = new ExecutionRequest();
        executionRequest.setUuid(erId);
        executionRequest.setProjectId(UUID.randomUUID());
        executionRequest.setTaToolsGroupId(UUID.randomUUID());
        executionRequest.setEnvironmentId(UUID.randomUUID());
        executionRequest.setThreads(2);
        executionRequest.setTestScopeId(UUID.randomUUID());
        return executionRequest;
    }
}
