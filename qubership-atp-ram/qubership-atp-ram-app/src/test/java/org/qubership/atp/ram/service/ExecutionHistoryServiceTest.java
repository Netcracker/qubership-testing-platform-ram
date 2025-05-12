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

package org.qubership.atp.ram.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.qubership.atp.ram.dto.request.TestCaseExecutionHistorySearchRequest;
import org.qubership.atp.ram.dto.response.Environment;
import org.qubership.atp.ram.dto.response.PaginationResponse;
import org.qubership.atp.ram.models.TestCase;
import org.qubership.atp.ram.models.TestCaseExecutionHistory;
import org.qubership.atp.ram.repositories.impl.CustomExecutionHistoryRepository;
import org.qubership.atp.ram.services.EnvironmentsService;
import org.qubership.atp.ram.services.TestCaseService;

public class ExecutionHistoryServiceTest {

    @InjectMocks
    private ExecutionHistoryService executionHistoryService;

    @Mock
    private TestCaseService testCaseService;

    @Mock
    private EnvironmentsService environmentsService;

    @Mock
    private CustomExecutionHistoryRepository executionHistoryRepository;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void When_ExecutorNameChanged_Expect_SuccessfulExecution() {
        UUID testCaseId = UUID.randomUUID();
        TestCase testCase = new TestCase();
        testCase.setUuid(testCaseId);
        testCase.setName("testcase");
        when(testCaseService.getTestCaseById(testCaseId)).thenReturn(testCase);

        UUID executorId = UUID.randomUUID();
        String oldName = "user1";
        String newName = "user2";
        List<TestCaseExecutionHistory.TestCaseExecution> testCaseExecutionList = Arrays.asList(
                generateTestCaseExecutionWithExecutorInfo(executorId, oldName),
                generateTestCaseExecutionWithExecutorInfo(executorId, newName)
        );
        PaginationResponse<TestCaseExecutionHistory.TestCaseExecution> paginationResponse = new PaginationResponse<>();
        paginationResponse.setEntities(testCaseExecutionList);
        paginationResponse.setTotalCount(testCaseExecutionList.size());

        when(executionHistoryRepository.getTestCaseExecutions(any(), any()))
                .thenReturn(paginationResponse);
        when(environmentsService.searchEnvironments(any())).thenReturn(Arrays.asList(
                new Environment(UUID.randomUUID(), "env1"),
                new Environment(UUID.randomUUID(), "env2")
        ));

        TestCaseExecutionHistorySearchRequest request = new TestCaseExecutionHistorySearchRequest();
        TestCaseExecutionHistory testCaseExecutions = executionHistoryService.getTestCaseExecutions(request, testCaseId);
        List<String> executorNames = testCaseExecutions.getExecutions().stream()
                .map(TestCaseExecutionHistory.TestCaseExecution::getExecutorName).collect(Collectors.toList());
        Assertions.assertTrue(executorNames.contains(oldName));
        Assertions.assertTrue(executorNames.contains(newName));
    }

    private TestCaseExecutionHistory.TestCaseExecution generateTestCaseExecutionWithExecutorInfo(UUID uuid,
                                                                                        String executorName) {
        TestCaseExecutionHistory.TestCaseExecution testCaseExecution = new TestCaseExecutionHistory.TestCaseExecution();
        testCaseExecution.setExecutorName(executorName);
        testCaseExecution.setExecutorId(uuid);

        testCaseExecution.setEnvironmentId(UUID.randomUUID());
        testCaseExecution.setFilteredByLabelsIds(Collections.emptySet());

        return testCaseExecution;
    }
}
