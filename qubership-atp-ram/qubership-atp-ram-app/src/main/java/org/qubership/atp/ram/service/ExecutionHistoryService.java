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

import static java.util.Objects.nonNull;
import static org.qubership.atp.ram.dto.response.ExecutionRequestMainInfoResponse.Executor;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.qubership.atp.ram.dto.request.TestCaseExecutionHistorySearchRequest;
import org.qubership.atp.ram.dto.response.Environment;
import org.qubership.atp.ram.dto.response.PaginationResponse;
import org.qubership.atp.ram.model.BaseSearchRequest;
import org.qubership.atp.ram.models.Label;
import org.qubership.atp.ram.models.TestCase;
import org.qubership.atp.ram.models.TestCaseExecutionHistory;
import org.qubership.atp.ram.models.TestCaseExecutionHistory.TestCaseExecution;
import org.qubership.atp.ram.repositories.impl.CustomExecutionHistoryRepository;
import org.qubership.atp.ram.services.EnvironmentsService;
import org.qubership.atp.ram.services.ExecutionRequestService;
import org.qubership.atp.ram.services.LabelsService;
import org.qubership.atp.ram.services.TestCaseService;
import org.qubership.atp.ram.utils.StreamUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExecutionHistoryService {

    private final ExecutionRequestService executionRequestService;
    private final TestCaseService testCaseService;
    private final CustomExecutionHistoryRepository executionHistoryRepository;
    private final EnvironmentsService environmentsService;
    private final LabelsService labelsService;

    /**
     * Get test case executions by test case id with pagination support.
     *
     * @param request search request
     * @param testCaseId test case identifier
     * @return test case executions list
     */
    public TestCaseExecutionHistory getTestCaseExecutions(TestCaseExecutionHistorySearchRequest request,
                                                          UUID testCaseId) {
        TestCase testCase = testCaseService.getTestCaseById(testCaseId);

        TestCaseExecutionHistory testCaseExecutionHistory = new TestCaseExecutionHistory();
        testCaseExecutionHistory.setTestCaseName(testCase.getName());

        PaginationResponse<TestCaseExecution> response =
                executionHistoryRepository.getTestCaseExecutions(request, testCaseId);
        testCaseExecutionHistory.setTotalCount(response.getTotalCount());
        List<TestCaseExecution> testCaseExecutions = response.getEntities();

        Set<UUID> environmentIds = StreamUtils.extractIds(testCaseExecutions, TestCaseExecution::getEnvironmentId);
        BaseSearchRequest envSearchRequest = BaseSearchRequest.builder().ids(environmentIds).build();
        List<Environment> environments = environmentsService.searchEnvironments(envSearchRequest);
        Map<UUID, Environment> environmentMap = StreamUtils.toKeyEntityMap(environments, Environment::getId);
        List<Label> labels = getTestCaseExecutionsLabels(testCaseExecutions);
        Map<UUID, List<Label>> filteredByLabelsMap = getTestCaseExecutionsAndFilteredByLabelsMap(testCaseExecutions,
                labels);

        testCaseExecutions.forEach(testCaseExecution -> {
            Environment environment = environmentMap.get(testCaseExecution.getEnvironmentId());
            if (nonNull(environment)) {
                testCaseExecution.setEnvironmentName(environment.getName());
            }

            Executor executor = new Executor(testCaseExecution.getExecutorId(), testCaseExecution.getExecutorName());
            if (nonNull(executor.getUserId()) && nonNull(executor.getUsername())) {
                testCaseExecution.setExecutorName(executor.getUsername());
            }

            testCaseExecution.setFilteredByLabels(filteredByLabelsMap.get(testCaseExecution.getExecutionRequestId()));
        });

        testCaseExecutionHistory.setExecutions(testCaseExecutions);

        return testCaseExecutionHistory;
    }

    /**
     * Get labels by list of execution requests.
     *
     * @param testCaseExecutions list of test case executions
     * @return list of found labels
     */
    public List<Label> getTestCaseExecutionsLabels(List<TestCaseExecution> testCaseExecutions) {
        Set<UUID> labelIds = StreamUtils.extractFlatIds(testCaseExecutions, TestCaseExecution::getFilteredByLabelsIds);
        return labelIds.isEmpty() ? Collections.emptyList() : labelsService.getLabels(labelIds);
    }

    /**
     * Get map of execution request ids and execution request's labels.
     *
     * @param testCaseExecutions list of test case executions
     * @param labels             list of labels
     * @return map of execution request ids and execution request's labels
     */
    public Map<UUID, List<Label>> getTestCaseExecutionsAndFilteredByLabelsMap(
            List<TestCaseExecution> testCaseExecutions, List<Label> labels) {
        Map<UUID, List<Label>> executionRequestAndLabelsMap = new HashMap<>();
        testCaseExecutions.forEach(execution -> {
            if (!CollectionUtils.isEmpty(execution.getFilteredByLabelsIds())) {
                executionRequestAndLabelsMap.put(execution.getExecutionRequestId(),
                        labels.stream()
                                .filter(label -> execution.getFilteredByLabelsIds().contains(label.getUuid()))
                                .collect(Collectors.toList()));
            }
        });
        return executionRequestAndLabelsMap;
    }
}
