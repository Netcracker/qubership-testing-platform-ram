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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.qubership.atp.ram.client.CatalogueExecuteRequestFeignClient;
import org.qubership.atp.ram.client.CatalogueIntegrationFeignClient;
import org.qubership.atp.ram.client.CatalogueIssueFeignClient;
import org.qubership.atp.ram.client.CatalogueLabelFeignClient;
import org.qubership.atp.ram.client.CatalogueLabelTemplateFeignClient;
import org.qubership.atp.ram.client.CatalogueProjectFeignClient;
import org.qubership.atp.ram.client.CatalogueTestCaseFeignClient;
import org.qubership.atp.ram.client.CatalogueTestPlanFeignClient;
import org.qubership.atp.ram.client.CatalogueTestScenarioFeignClient;
import org.qubership.atp.ram.client.CatalogueTestScopeFeignClient;
import org.qubership.atp.ram.clients.api.dto.catalogue.CaseSearchRequestDto;
import org.qubership.atp.ram.clients.api.dto.catalogue.JiraIssueCreateRequestDto;
import org.qubership.atp.ram.clients.api.dto.catalogue.JiraIssueDto;
import org.qubership.atp.ram.clients.api.dto.catalogue.JiraIssueSearchRequestDto;
import org.qubership.atp.ram.clients.api.dto.catalogue.JiraIssueSearchResponseDto;
import org.qubership.atp.ram.clients.api.dto.catalogue.RerunRequestDto;
import org.qubership.atp.ram.clients.api.dto.catalogue.TestCaseLastStatusDto;
import org.qubership.atp.ram.clients.api.dto.catalogue.TestRunToJiraInfoDto;
import org.qubership.atp.ram.converter.DtoConvertService;
import org.qubership.atp.ram.dto.response.ProjectDataResponse;
import org.qubership.atp.ram.dto.response.TestCaseLabelResponse;
import org.qubership.atp.ram.model.CaseSearchRequest;
import org.qubership.atp.ram.model.CheckSumResponse;
import org.qubership.atp.ram.model.TestRunToJiraInfo;
import org.qubership.atp.ram.model.jira.JiraIssueCreateRequest;
import org.qubership.atp.ram.model.jira.JiraIssueCreateResponse;
import org.qubership.atp.ram.model.jira.JiraIssueResponse;
import org.qubership.atp.ram.models.Issue;
import org.qubership.atp.ram.models.JiraComponent;
import org.qubership.atp.ram.models.Label;
import org.qubership.atp.ram.models.LabelTemplate;
import org.qubership.atp.ram.models.Scope;
import org.qubership.atp.ram.models.TestCase;
import org.qubership.atp.ram.models.TestCaseLastStatus;
import org.qubership.atp.ram.models.TestPlan;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.utils.StreamUtils;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class CatalogueService {

    private final CatalogueTestScopeFeignClient catalogueTestScopeFeignClient;
    private final CatalogueProjectFeignClient catalogueProjectFeignClient;
    private final CatalogueIntegrationFeignClient catalogueIntegrationFeignClient;
    private final CatalogueTestPlanFeignClient catalogueTestPlanFeignClient;
    private final CatalogueTestCaseFeignClient catalogueTestCaseFeignClient;
    private final CatalogueTestScenarioFeignClient catalogueTestScenarioFeignClient;
    private final CatalogueLabelTemplateFeignClient catalogueLabelTemplateFeignClient;
    private final CatalogueLabelFeignClient catalogueLabelFeignClient;
    private final CatalogueIssueFeignClient catalogueIssueFeignClient;
    private final CatalogueExecuteRequestFeignClient catalogueExecuteRequestFeignClient;

    private final DtoConvertService dtoConvertService;

    /**
     * Auto sync test runs and test cases with jira.
     *
     * @param projectId - project id
     * @param executionRequestId - execution request id for sync
     * @param syncTestCases - is test cases' sync needed
     * @param syncTestRuns - is test runs' sync needed
     * @param testRunToJiraInfos - test runs info to sync
     */
    public void autoSyncTestRunsWithJira(UUID projectId, UUID executionRequestId, boolean syncTestCases,
                                         boolean syncTestRuns, List<TestRunToJiraInfo> testRunToJiraInfos) {
        if (Objects.isNull(executionRequestId)) {
            log.warn("Sync with jira is impossible - execution request id is null");
            return;
        }
        if (!(syncTestCases || syncTestRuns)) {
            log.info("Sync with jira not needed - sync test cases and test runs disabled");
            return;
        }
        catalogueIntegrationFeignClient.autoSyncTestRunsWithJira(projectId, executionRequestId, syncTestCases,
                syncTestRuns, dtoConvertService.convertList(testRunToJiraInfos, TestRunToJiraInfoDto.class));
    }

    /**
     * Send to catalogue status updates for a list of testCases.
     *
     * @param listTestRun set of test case ids
     */
    public void updateCaseStatuses(List<TestRun> listTestRun) {
        List<TestCaseLastStatus> testCaseLastStatuses = listTestRun.stream()
                .filter(testRun -> testRun.getTestCaseId() != null)
                .map(testRun -> {
                    TestCaseLastStatus status = new TestCaseLastStatus();
                    status.setStatus(testRun.getTestingStatus().getName());
                    status.setTestCaseId(testRun.getTestCaseId());
                    return status;
                })
                .collect(Collectors.toList());
        if (!testCaseLastStatuses.isEmpty()) {
            catalogueTestCaseFeignClient.updateCaseStatuses(dtoConvertService.convertList(
                    testCaseLastStatuses, TestCaseLastStatusDto.class));
        }
    }

    /**
     * Send request in catalog to get scenario id by test case id.
     *
     * @param testCaseId the test case id
     * @return the scenario id
     */
    public UUID getScenarioIdByTestCaseId(UUID testCaseId) {
        if (Objects.isNull(testCaseId)) {
            return null;
        }
        return catalogueTestCaseFeignClient.getScenarioIdByTestCaseId(testCaseId).getBody();
    }

    /**
     * Send request to catalog to check the hashsum for scenarios from  input map id-hashsum.
     * If one of the scenario from input map fails validation then method returns false.
     *
     * @param hashSums the hash sums
     * @return the boolean
     */
    public CheckSumResponse checkHashSumForScenario(Map<UUID, String> hashSums) {
        if (Objects.isNull(hashSums)) {
            return null;
        }
        Map<String, String> hashSumsConverted = new HashMap<>();
        for (Map.Entry<UUID, String> entry : hashSums.entrySet()) {
            hashSumsConverted.put(entry.getKey().toString(), entry.getValue());
        }
        return dtoConvertService.convert(
                catalogueTestScenarioFeignClient.checkHashSum(hashSumsConverted).getBody(), CheckSumResponse.class);
    }

    /**
     * Get project from catalogue.
     *
     * @param projectId project id
     * @return project or null, if ID = null
     */
    public ProjectDataResponse getProjectData(UUID projectId) {
        if (Objects.isNull(projectId)) {
            return null;
        }
        return dtoConvertService.convert(
                catalogueProjectFeignClient.getProjectById(projectId).getBody(), ProjectDataResponse.class);
    }

    /**
     * Found test cases by specified identifiers.
     *
     * @param testRuns list test runs
     * @return founded test cases
     */
    public List<TestCaseLabelResponse> getTestCaseLabelsByIds(List<TestRun> testRuns) {
        if (Objects.isNull(testRuns)) {
            log.error("List of Test Runs is null.");
            return new ArrayList<>();
        }
        Set<UUID> testCaseIds = StreamUtils.extractIds(testRuns, TestRun::getTestCaseId);
        log.debug("Found test runs test case references: {}", testCaseIds);
        if (testCaseIds.isEmpty()) {
            return new ArrayList<>();
        }
        return dtoConvertService.convertList(
            catalogueTestCaseFeignClient.getCaseLabels(testCaseIds).getBody(),
            TestCaseLabelResponse.class);
    }

    /**
     * Get label template data from catalogue.
     *
     * @param labelTemplateId label template identifier
     * @return label template data
     */
    public LabelTemplate getLabelTemplateById(UUID labelTemplateId) {
        if (Objects.isNull(labelTemplateId)) {
            return null;
        }
        return dtoConvertService.convert(
                catalogueLabelTemplateFeignClient.get(labelTemplateId).getBody(), LabelTemplate.class);
    }

    /**
     * Delete label template data from catalogue.
     *
     * @param labelTemplateId label template identifier
     */
    public void deleteLabelTemplateById(UUID labelTemplateId) {
        if (!Objects.isNull(labelTemplateId)) {
            catalogueLabelTemplateFeignClient.delete(labelTemplateId);
        }
    }


    /**
     * Get issues from catalogue.
     *
     * @param issueIds ids of issues you want to get
     * @return issues
     */
    public List<Issue> getIssues(List<UUID> issueIds) {
        if (Objects.isNull(issueIds)) {
            return null;
        }
        return dtoConvertService.convertList(
                catalogueIssueFeignClient.getByIds(issueIds).getBody(), Issue.class);
    }

    /**
     * Get test plan from catalogue.
     *
     * @param testPlanId test plan identifier
     * @return test plan data
     */

    public TestPlan getTestPlan(UUID testPlanId) {
        if (Objects.isNull(testPlanId)) {
            return null;
        }
        return dtoConvertService.convert(
                catalogueTestPlanFeignClient.getTestPlanByUuid(testPlanId).getBody(), TestPlan.class);
    }

    /**
     * Get test scope from catalogue.
     *
     * @param testScopeId scope id
     * @return scope or null, if ID = null
     */
    public Scope getTestScope(UUID testScopeId) {
        if (Objects.isNull(testScopeId)) {
            return null;
        }
        return dtoConvertService.convert(
                catalogueTestScopeFeignClient.getTestScopeByUuid(testScopeId).getBody(), Scope.class);
    }

    /**
     * Get test cases by search request.
     *
     * @param request search request
     * @return founded test cases
     */
    public List<TestCase> getTestCases(CaseSearchRequest request) {
        if (Objects.isNull(request)) {
            return null;
        }
        CaseSearchRequestDto caseSearchRequestDto = dtoConvertService.convert(request, CaseSearchRequestDto.class);
        return dtoConvertService.convertList(
                catalogueTestCaseFeignClient.getTestCasesByIds(caseSearchRequestDto).getBody(), TestCase.class);
    }

    /**
     * Get test case by id.
     *
     * @param testCaseId test case identifier
     * @return founded test cases
     */
    public TestCase getTestCaseById(UUID testCaseId) {
        if (Objects.isNull(testCaseId)) {
            return null;
        }
        return dtoConvertService.convert(
                catalogueTestCaseFeignClient.getTestCaseWithLabelsByUuid(testCaseId).getBody(), TestCase.class);
    }

    /**
     * Get labels by ids.
     *
     * @param labelIds label identifiers
     * @return founded labels
     */

    public List<Label> getLabelsByIds(Set<UUID> labelIds) {
        if (Objects.isNull(labelIds)) {
            return null;
        }
        return dtoConvertService.convertList(catalogueLabelFeignClient.getLabelsByIds(
                labelIds).getBody(), Label.class);
    }

    /**
     * Create Jira ticket.
     *
     * @param testPlanId test plan identifier
     * @param request    creation request
     * @return created issue
     */
    public JiraIssueCreateResponse createJiraTicket(UUID testPlanId, JiraIssueCreateRequest request) {
        if (Objects.isNull(testPlanId)) {
            return null;
        } else if (Objects.isNull(request)) {
            return null;
        }
        JiraIssueCreateRequestDto jiraIssueCreateRequestDto =
                dtoConvertService.convert(request, JiraIssueCreateRequestDto.class);
        return dtoConvertService.convert(
                catalogueIntegrationFeignClient.createJiraTicket(testPlanId, jiraIssueCreateRequestDto).getBody(),
                JiraIssueCreateResponse.class);
    }


    /**
     * Create Jira ticket.
     *
     * @param testPlanId test plan identifier
     * @param key        for search
     * @return issue
     */
    public JiraIssueResponse getJiraTicket(UUID testPlanId, String key) {
        if (Objects.isNull(testPlanId)) {
            return null;
        } else if (Objects.isNull(key)) {
            return null;
        }
        return dtoConvertService.convert(
                catalogueIntegrationFeignClient.getJiraTicketByKey(testPlanId, key).getBody(),
                JiraIssueResponse.class);
    }

    /**
     * Get test plan jira components.
     *
     * @param testPlanId test plan identifier
     * @return jiraComponentsListResponse
     */
    public List<JiraComponent> getTestPlanJiraComponents(UUID testPlanId) {
        if (Objects.isNull(testPlanId)) {
            return null;
        }
        return dtoConvertService.convertList(
                catalogueIntegrationFeignClient.getTestPlanJiraComponents(testPlanId).getBody(), JiraComponent.class);
    }

    public UUID rerunExecutionRequest(RerunRequestDto rerunRequestDto) {
        return catalogueExecuteRequestFeignClient.rerun(null, rerunRequestDto).getBody();
    }

    /**
     * Search issues by keys and fields.
     *
     * @param testPlanId    test plan identifier
     * @param keys          keys for search
     * @param fields        fields for search
     * @return              founded issues
     */
    public List<JiraIssueDto> searchIssues(UUID testPlanId, Set<String> keys, Set<String> fields) {
        final JiraIssueSearchRequestDto request = new JiraIssueSearchRequestDto();
        request.setTestPlanId(testPlanId);
        request.setFields(new ArrayList<>(fields));
        request.setKeys(new ArrayList<>(keys));

        final JiraIssueSearchResponseDto response = catalogueIntegrationFeignClient.searchIssues(request).getBody();

        return Objects.nonNull(response) ? response.getIssues() : new ArrayList<>();
    }
}
