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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.qubership.atp.ram.converter.DtoConvertService;
import org.qubership.atp.ram.model.TestRunForRefreshFromJira;
import org.qubership.atp.ram.model.TestRunToJiraInfo;
import org.qubership.atp.ram.models.EnvironmentsInfo;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.SystemInfo;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.repositories.ExecutionRequestRepository;
import org.qubership.atp.ram.repositories.TestRunRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class JiraIntegrationService {

    private final TestRunRepository repository;
    private final TestRunService testRunService;
    private final ExecutionRequestRepository executionRequestRepository;
    private final CatalogueService catalogueService;
    private final EnvironmentsInfoService environmentsInfoService;
    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper;
    private final String catalogueUrl;

    private final DtoConvertService dtoConverter;

    /**
     * Creates JiraIntegrationService.
     *
     * @param repository                 Test Run Repository
     * @param testRunService             Test Run Service
     * @param executionRequestRepository execution request service
     * @param catalogueService           - catalog service
     * @param environmentsInfoService    - env info service
     * @param modelMapper                - modelMapper
     * @param objectMapper               - objectMapper
     * @param catalogueUrl               - url to atp-catalogue service
     * @param dtoConverter               dtoConverter
     */
    public JiraIntegrationService(TestRunRepository repository,
                                  TestRunService testRunService,
                                  ExecutionRequestRepository executionRequestRepository,
                                  CatalogueService catalogueService,
                                  EnvironmentsInfoService environmentsInfoService, ModelMapper modelMapper,
                                  ObjectMapper objectMapper,
                                  @Value("${catalogue.url}") String catalogueUrl, DtoConvertService dtoConverter) {
        this.repository = repository;
        this.testRunService = testRunService;
        this.executionRequestRepository = executionRequestRepository;
        this.catalogueService = catalogueService;
        this.environmentsInfoService = environmentsInfoService;
        this.modelMapper = modelMapper;
        this.objectMapper = objectMapper;
        this.catalogueUrl = catalogueUrl;
        this.dtoConverter = dtoConverter;
    }

    /**
     * Get test run info for Jira integration by ids.
     *
     * @param executionRequestId execution request id
     * @return test run info for Jira integration
     */
    public List<TestRunToJiraInfo> getTestRunsForJiraInfoByExecutionId(UUID executionRequestId) {
        List<TestRun> testRuns = testRunService.findAllByExecutionRequestId(executionRequestId);
        return getTestRunsForJiraInfo(testRuns);
    }

    /**
     * Get test run info for Jira integration by ids.
     *
     * @param testRunIds test run ids.
     * @return test run info for Jira integration
     */
    public List<TestRunToJiraInfo> getTestRunsForJiraInfoByIds(List<UUID> testRunIds) {
        List<TestRun> testRuns = repository.findAllByUuidIn(testRunIds);
        return getTestRunsForJiraInfo(testRuns);
    }

    /**
     * Get test run info for Jira integration.
     *
     * @param testRuns - test runs.
     * @return test run info for Jira integration
     */
    public List<TestRunToJiraInfo> getTestRunsForJiraInfo(List<TestRun> testRuns) {
        List<UUID> executionRequestIds = testRuns.stream()
                .map(TestRun::getExecutionRequestId)
                .distinct().collect(toList());
        Map<UUID, ExecutionRequest> erMap = executionRequestRepository.findAllByUuidIn(executionRequestIds)
                .stream().collect(toMap(ExecutionRequest::getUuid, Function.identity()));
        Map<UUID, EnvironmentsInfo> environmentsInfoMap = environmentsInfoService.findByRequestIds(executionRequestIds)
                .stream()
                .collect(toMap(EnvironmentsInfo::getEnvironmentId, Function.identity(),
                        (env1, env2) -> {
                            log.trace("Same env found, take any {}", env1);
                            return env1;
                        }));
        List<TestRunToJiraInfo> testRunToJiraInfos = new ArrayList<>();
        for (TestRun testRun : testRuns) {
            TestRun lastRun = testRunService.getByTestCase(testRun.getTestCaseId());
            ExecutionRequest executionRequest = erMap.get(testRun.getExecutionRequestId());
            boolean isLastRun = testRun.getUuid().equals(lastRun.getUuid());
            TestRunToJiraInfo testRunToJiraInfo = modelMapper.map(testRun, TestRunToJiraInfo.class);
            testRunToJiraInfo.setLastRun(isLastRun);
            testRunToJiraInfo.setTestRunAtpLink(generateTestRunLink(testRun, executionRequest));
            EnvironmentsInfo environmentsInfo = environmentsInfoMap.get(executionRequest.getEnvironmentId());
            testRunToJiraInfo.setEnvironmentInfo(formatComment(environmentsInfo));
            testRunToJiraInfos.add(testRunToJiraInfo);
        }
        try {
            log.debug("propagateRequests {}", objectMapper.writeValueAsString(testRunToJiraInfos));
        } catch (JsonProcessingException e) {
            log.error("Unable to parse propagateRequests json data", e);
        }
        return testRunToJiraInfos;
    }

    private String formatComment(EnvironmentsInfo environmentsInfo) {
        if (environmentsInfo == null
                || CollectionUtils.isEmpty(environmentsInfo.getQaSystemInfoList())) {
            return "Environments Info is not available";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("||Environment Name|| Urls|| Version||\n");
        for (SystemInfo systemInfo : environmentsInfo.getQaSystemInfoList()) {
            stringBuilder.append("|").append(systemInfo.getName())
                    .append("|").append(systemInfo.getUrls())
                    .append("|").append(
                            StringUtils.isBlank(systemInfo.getVersion()) ? "-" : systemInfo.getVersion())
                    .append("|\n");
        }
        return stringBuilder.toString();
    }

    private String generateTestRunLink(TestRun testRun, ExecutionRequest executionRequest) {
        final String atp2Link =
                catalogueUrl + "/project/" + executionRequest.getProjectId()
                        + "/ram/execution-request/" + executionRequest.getUuid() + "?node=" + testRun.getUuid();
        log.debug("Generated atp link {} for test run {}", atp2Link, testRun);
        return atp2Link;
    }

    /**
     * Get existing test run info for refreshing data from jira.
     * Returns test run only if its' jiraTicket is not empty.
     *
     * @param testRunIds - test run's ids.
     * @return test run existing info for Jira integration
     */
    public List<TestRunForRefreshFromJira> getTestRunsForRefreshFromJira(List<UUID> testRunIds) {
        List<TestRunForRefreshFromJira> testRunsForRefreshList = new ArrayList<>();
        List<TestRun> testRuns = repository.findAllByUuidIn(testRunIds);
        for (TestRun testRun : testRuns) {
            TestRun lastRun = testRunService.getByTestCase(testRun.getTestCaseId());
            boolean isLastRun = testRun.getUuid().equals(lastRun.getUuid());
            TestRunForRefreshFromJira testRunForRefresh = modelMapper.map(testRun, TestRunForRefreshFromJira.class);
            testRunForRefresh.setLastRun(isLastRun);
            testRunsForRefreshList.add(testRunForRefresh);
        }
        return testRunsForRefreshList;
    }

    /**
     * Call auto sync execution request with jira in catalogue.
     *
     * @param executionRequest - execution request for sync
     */
    public void syncWithJira(ExecutionRequest executionRequest) {
        UUID erId = executionRequest.getUuid();
        catalogueService.autoSyncTestRunsWithJira(
                executionRequest.getProjectId(),
                erId,
                executionRequest.isAutoSyncCasesWithJira(),
                executionRequest.isAutoSyncRunsWithJira(),
                getTestRunsForJiraInfoByExecutionId(erId));
    }
}
