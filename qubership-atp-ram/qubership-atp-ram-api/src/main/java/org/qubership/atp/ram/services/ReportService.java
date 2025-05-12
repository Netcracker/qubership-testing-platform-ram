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

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.qubership.atp.ram.utils.StreamUtils.filterList;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.apache.commons.collections.CollectionUtils;
import org.modelmapper.ModelMapper;
import org.qubership.atp.auth.springbootstarter.exceptions.AtpEntityNotFoundException;
import org.qubership.atp.ram.dto.response.ExecutionSummaryResponse;
import org.qubership.atp.ram.dto.response.LabelNodeReportResponse;
import org.qubership.atp.ram.dto.response.LabelNodeReportResponse.TestRunNodeResponse;
import org.qubership.atp.ram.dto.response.RootCausesStatisticResponse;
import org.qubership.atp.ram.dto.response.RootCausesStatisticResponse.RootCausesGroup;
import org.qubership.atp.ram.dto.response.ServerSummaryResponse;
import org.qubership.atp.ram.entities.treenodes.labelparams.ReportLabelParam;
import org.qubership.atp.ram.entities.treenodes.labelparams.TestingReportLabelParam;
import org.qubership.atp.ram.enums.ExecutionRequestWidgets;
import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.enums.TestScopeSections;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.model.datacontext.TestRunsDataContext;
import org.qubership.atp.ram.model.datacontext.TestRunsDataContextLoadOptions;
import org.qubership.atp.ram.models.EnvironmentsInfo;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.FinalRunData;
import org.qubership.atp.ram.models.LabelTemplate;
import org.qubership.atp.ram.models.RerunDetails;
import org.qubership.atp.ram.models.Scope;
import org.qubership.atp.ram.models.SystemInfo;
import org.qubership.atp.ram.models.TestCaseWidgetReportRequest;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.models.TestRunSearchRequest;
import org.qubership.atp.ram.models.ValidationLabelConfigTemplate;
import org.qubership.atp.ram.models.WidgetConfigTemplate;
import org.qubership.atp.ram.models.tree.TreeWalker;
import org.qubership.atp.ram.repositories.ExecutionRequestRepository;
import org.qubership.atp.ram.utils.RateCalculator;
import org.qubership.atp.ram.utils.StreamUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    @Value("${catalogue.url}")
    private String catalogueUrl;
    private final EnvironmentsInfoService environmentsInfoService;
    private final EnvironmentsService environmentsService;
    private final ExecutionRequestRepository executionRequestRepository;
    private final TestRunService testRunService;
    private final LabelTemplateNodeService labelTemplateNodeService;
    private final LabelsService labelsService;
    private final ModelMapper modelMapper;
    private final ValidationLabelConfigTemplateService validationLabelConfigTemplateService;
    private final WidgetConfigTemplateService widgetConfigService;
    private final TreeNodeService treeNodeService;
    private final RateCalculator rateCalculator;
    private final CatalogueService catalogueService;


    /**
     * Gets project id by execution request id.
     */
    @Nullable
    public UUID getProjectIdByExecutionRequestId(UUID executionRequestId) {
        ExecutionRequest executionRequest = executionRequestRepository.findProjectIdByUuid(executionRequestId);
        if (isNull(executionRequest)) {
            log.error("Failed to find execution request with id: {} ", executionRequestId);
            throw new AtpEntityNotFoundException("Execution request", executionRequestId);
        }
        return executionRequest.getProjectId();
    }

    /**
     * Generate {@link ServerSummaryResponse} for er by id.
     * Or throw {@link AtpEntityNotFoundException} if env info doesn't found
     *
     * @param erId for found {@link EnvironmentsInfo}
     * @return list of {@link ServerSummaryResponse}
     */
    public List<ServerSummaryResponse> getServerSummaryForExecutionRequest(UUID erId) {
        log.info("Getting environments info by ER {}", erId);
        EnvironmentsInfo environmentsInfo = environmentsInfoService.findQaTaSystemsByExecutionRequestId(erId);

        List<ServerSummaryResponse> summaryResponses = new ArrayList<>();
        summaryResponses.addAll(getServerSummaryBySystemInfo(environmentsInfo.getQaSystemInfoList()));
        summaryResponses.addAll(getServerSummaryBySystemInfo(environmentsInfo.getTaSystemInfoList()));

        return summaryResponses;
    }

    private List<ServerSummaryResponse> getServerSummaryBySystemInfo(List<SystemInfo> systemInfoList) {
        List<ServerSummaryResponse> summaryResponsesList = new ArrayList<>();
        systemInfoList.forEach(systemInfo -> systemInfo.getUrls().forEach(url -> {
            ServerSummaryResponse serverSummaryResponse = new ServerSummaryResponse();
            serverSummaryResponse.setBuild(Collections.singletonList(systemInfo.getVersion()));
            serverSummaryResponse.setServer(url);
            summaryResponsesList.add(serverSummaryResponse);
        }));
        return summaryResponsesList;
    }

    /**
     * Generate {@link RootCausesStatisticResponse} for er by id and previous, if exists.
     * Or throw {@link AtpEntityNotFoundException} if ER is null.
     *
     * @param erId for found ER
     * @return {@link RootCausesStatisticResponse}
     */
    public List<RootCausesStatisticResponse> getRootCausesStatisticForExecutionRequestAndPrevious(UUID erId) {
        ExecutionRequest executionRequest = executionRequestRepository.findNameStartDatePreviousErIdByUuid(erId);
        List<RootCausesStatisticResponse> result = new ArrayList<>();
        result.add(getRootCauseStatisticForEr(executionRequest));

        UUID previousExecutionRequestId = executionRequest.getPreviousExecutionRequestId();
        if (nonNull(previousExecutionRequestId)) {
            ExecutionRequest previousExecutionRequest =
                    executionRequestRepository.findNameStartDatePreviousErIdByUuid(previousExecutionRequestId);
            result.add(getRootCauseStatisticForEr(previousExecutionRequest));
        }
        return result;
    }

    /**
     * Create RC response with ER info and RC-s info.
     * Or throw {@link AtpEntityNotFoundException} if ER is null.
     *
     * @param executionRequest for setting info
     * @return {@link RootCausesStatisticResponse}
     */
    private RootCausesStatisticResponse getRootCauseStatisticForEr(ExecutionRequest executionRequest) {
        if (isNull(executionRequest)) {
            log.error("Failed to find Execution Request");
            throw new AtpEntityNotFoundException("Execution Request");
        }
        RootCausesStatisticResponse rootCausesStatisticResponse = new RootCausesStatisticResponse();
        rootCausesStatisticResponse.setExecutionRequestName(executionRequest.getName());
        rootCausesStatisticResponse.setStartDate(executionRequest.getStartDate());

        Map<String, Integer> rootCauses = testRunService.getTestRunsGroupedByRootCauses(executionRequest.getUuid());
        List<RootCausesGroup> rootCausesGroups = new ArrayList<>();
        Long countTestRunsForEr = testRunService.countAllByExecutionRequestId(executionRequest.getUuid());
        rootCauses.forEach((rootCauseName, rootCauseCount) -> {
            long percent = countTestRunsForEr > 0
                    ? Math.round(rootCauseCount.doubleValue() / countTestRunsForEr.doubleValue() * 100)
                    : 0;
            rootCausesGroups.add(new RootCausesGroup(rootCauseName, rootCauseCount, percent));
        });
        rootCausesStatisticResponse.setRootCausesGroups(rootCausesGroups);

        return rootCausesStatisticResponse;
    }

    /**
     * Generate tree of tests cases mapped to filled {@link LabelTemplate}.
     *
     * @param erId                       for found list TR-s and label template ID
     * @param isExecutionRequestsSummary flag to display complex reran details in comparison
     * @return tree of tests cases mapped to filled {@link LabelTemplate}
     */
    public LabelNodeReportResponse getTestCasesForExecutionRequest(UUID erId,
                                                                   UUID labelTemplateId,
                                                                   UUID validationTemplateId,
                                                                   boolean isExecutionRequestsSummary,
                                                                   TestCaseWidgetReportRequest request) {
        Map<UUID, RerunDetails> rerunDetailsMapping = new HashMap<>();
        ExecutionRequest executionRequest = executionRequestRepository.findByUuid(erId);
        final UUID testCasesWidgetId = ExecutionRequestWidgets.TEST_CASES.getWidgetId();
        validationTemplateId = isNull(validationTemplateId) ? widgetConfigService.getValidationTemplateIdByErWidget(
                executionRequest, testCasesWidgetId) : validationTemplateId;
        ValidationLabelConfigTemplate validationTemplate = null;
        if (nonNull(validationTemplateId)) {
            validationTemplate = validationLabelConfigTemplateService.get(validationTemplateId);
        }

        WidgetConfigTemplate.Filters widgetConfigFilters = resolveWidgetConfigFilters(request, executionRequest);
        List<TestRun> testRuns;
        if (isNull(widgetConfigFilters)) {
            testRuns = testRunService.findAllByExecutionRequestId(erId);
        } else {
            final Set<TestingStatuses> status = widgetConfigFilters.getTestingStatuses();
            final Set<UUID> failureReasons = widgetConfigFilters.getFailureReasons();
            final TestRunSearchRequest searchRequest = new TestRunSearchRequest();
            searchRequest.setExecutionRequestId(erId);
            searchRequest.setFailureReasons(failureReasons);
            searchRequest.setInTestingStatuses(status);
            testRuns = testRunService.search(searchRequest, 0, Integer.MAX_VALUE).getEntities();
        }

        UUID initialErId = executionRequest.getInitialExecutionRequestId();
        if (isExecutionRequestsSummary) {
            testRuns = getSummaryTestRunsAndSetRerunMapping(initialErId, testRuns, rerunDetailsMapping);
        } else {
            boolean isRerun = nonNull(initialErId);
            rerunDetailsMapping = isRerun
                    ? getRerunMapping(initialErId, testRuns, isRerun)
                    : getRerunMapping(erId, testRuns, isRerun);
        }

        if (testRuns != null && !testRuns.isEmpty()) {
            testRuns = testRuns.stream().sorted(Comparator.comparing(TestRun::getName)).collect(Collectors.toList());
        }
        TestRunsDataContextLoadOptions dataContextLoadOptions = new TestRunsDataContextLoadOptions()
                .includeRunFailedLogRecordsMap()
                .includeRunValidationLogRecordsMap()
                .includeRunTestCasesMap()
                .includeTestRunDslNamesMap()
                .includeRootCausesMap();
        TestRunsDataContext dataContext = testRunService.getTestRunsDataContext(testRuns, dataContextLoadOptions,
                false);
        dataContext.setExecutionRequestId(erId);
        UUID testScopeId = executionRequest.getTestScopeId();
        boolean isScopeRun = nonNull(testScopeId);
        log.debug("Scope run: {}", isScopeRun);

        LabelNodeReportResponse rootNode = new LabelNodeReportResponse();

        if (isNull(labelTemplateId)) {
            labelTemplateId = widgetConfigService.defineLabelTemplateId(executionRequest, testCasesWidgetId);
        }

        if (isNull(labelTemplateId)) {
            if (isScopeRun) {
                rootNode = getTestScopeResponse(rootNode, testRuns, null, validationTemplate, dataContext);
            } else {
                rootNode = getLabelNodeReportResponseWithoutTemplate(testRuns, validationTemplate, dataContext);
            }
        } else {
            LabelTemplate labelTemplate = labelTemplateNodeService.getLabelTemplate(labelTemplateId);
            rootNode.setLabelTemplateId(labelTemplate.getUuid());
            rootNode.setLabelTemplateName(labelTemplate.getName());
            if (isScopeRun) {
                rootNode = getTestScopeResponse(rootNode, testRuns, labelTemplateId, validationTemplate,
                        dataContext);
            } else {
                LabelTemplate filledTemplate = labelTemplateNodeService.populateLabelTemplateWithTestRuns(
                        testRuns, labelTemplate);
                Set<UUID> testRunIds = filledTemplate.getUnknownNode().getTestRunIds();
                List<TestRun> unknownTestRuns = filterList(testRuns, testRunIds);
                List<TestRunNodeResponse> testRunsForDefaultNode = testRunService.getTestRunNodeWithFailedLogRecords(
                        unknownTestRuns, validationTemplate, dataContext);
                rootNode.setTestRuns(testRunsForDefaultNode);
                List<LabelTemplate.LabelTemplateNode> labelNodes = filledTemplate.getLabelNodes();
                fillLabelReportsNode(labelNodes, rootNode, validationTemplate, testRuns, dataContext);
            }
        }

        setValidationLabelsOrder(rootNode, validationTemplate);
        removeEmptyNodes(rootNode);
        updateWithRerunDetailsRecursively(rootNode, rerunDetailsMapping, widgetConfigFilters);
        removeEmptyNodes(rootNode);

        return rootNode;
    }

    /**
     * Resolve widget config filters for test cases widget.
     */
    public WidgetConfigTemplate.Filters resolveWidgetConfigFilters(TestCaseWidgetReportRequest request,
                                                                   ExecutionRequest executionRequest) {
        WidgetConfigTemplate.Filters filters = request.getFilters();
        if (nonNull(filters)) {
            return filters;
        }

        final UUID widgetConfigTemplateId = executionRequest.getWidgetConfigTemplateId();
        WidgetConfigTemplate widgetConfigTemplate;
        if (nonNull(widgetConfigTemplateId)) {
            widgetConfigTemplate = widgetConfigService.get(widgetConfigTemplateId);
        } else {
            widgetConfigTemplate = widgetConfigService.getWidgetConfigTemplateForEr(executionRequest).getTemplate();
        }

        if (nonNull(widgetConfigTemplate)) {
            WidgetConfigTemplate.WidgetConfig testCasesWidgetConfig =
                    widgetConfigTemplate.getWidgetConfig(ExecutionRequestWidgets.TEST_CASES.getWidgetId());
            filters = testCasesWidgetConfig.getFilters();
        }

        return filters;
    }


    /**
     * Get summary list of Test Runs based on the initial Execution Request, all reruns and relative to current rerun.
     */
    protected List<TestRun> getSummaryTestRunsAndSetRerunMapping(UUID initialErId, List<TestRun> testRuns,
                                                                 Map<UUID, RerunDetails> rerunDetailsMapping) {
        Map<UUID, TestRun> finalTestRunMapping = getFinalTrMapping(initialErId);
        List<TestRun> initialTestRuns = testRunService.findAllByExecutionRequestId(initialErId);
        List<TestRun> summaryTestRuns = new ArrayList<>();

        initialTestRuns.forEach(initialTestRun -> {
            UUID initialTestRunId = initialTestRun.getUuid();
            Optional<TestRun> matchRun = testRuns.stream()
                    .filter(testRun -> testRun.getInitialTestRunId().equals(initialTestRunId))
                    .findFirst();
            TestRun summaryTestRun = matchRun.orElse(initialTestRun);

            RerunDetails rerunDetails = new RerunDetails();
            matchRun.ifPresent(testRun -> rerunDetails.setTestingStatus(testRun.getTestingStatus()));
            rerunDetails.setFirstStatus(initialTestRun.getTestingStatus());
            TestRun finalRun = summaryTestRun;
            for (Map.Entry<UUID, TestRun> entry : finalTestRunMapping.entrySet()) {
                TestRun finalTr = entry.getValue();
                if (finalTr.getUuid().equals(initialTestRunId)
                        || finalTr.getInitialTestRunId() != null
                        && finalTr.getInitialTestRunId().equals(initialTestRunId)) {
                    finalRun = finalTr;
                }
            }
            rerunDetails.setFinalStatus(finalRun.getTestingStatus());
            FinalRunData finalRunData = new FinalRunData(
                    finalRun.getExecutionRequestId(), finalRun.getUuid());
            rerunDetails.setFinalRunData(finalRunData);
            rerunDetailsMapping.put(finalRun.getUuid(), rerunDetails);
            summaryTestRuns.add(finalRun);
        });
        return summaryTestRuns;
    }

    /**
     * Get mapping for all final Test Runs from Initial Execution request.
     * Mapped: Test Run Id with Tes Run object.
     */
    protected Map<UUID, TestRun> getFinalTrMapping(UUID initialErId) {
        Map<UUID, TestRun> finalTestRuns = new HashMap<>();
        List<ExecutionRequest> rerunExecutionRequests = executionRequestRepository
                .findAllByInitialExecutionRequestId(initialErId);
        ExecutionRequest initialExecutionRequest = executionRequestRepository
                .findByUuid(initialErId);
        List<ExecutionRequest> targetExecutionRequests = new ArrayList<>(rerunExecutionRequests);
        targetExecutionRequests.add(initialExecutionRequest);
        targetExecutionRequests.forEach(executionRequest -> {
            List<TestRun> testRuns = testRunService.findAllByExecutionRequestId(executionRequest.getUuid());
            Map<UUID, TestRun> executionRequestFinalTestRuns = testRuns.stream()
                    .filter(TestRun::isFinalTestRun)
                    .collect(Collectors.toMap(TestRun::getUuid, Function.identity()));
            finalTestRuns.putAll(executionRequestFinalTestRuns);
        });
        return finalTestRuns;
    }

    /**
     * Get mapping for all Rerun Testing Statuses taking into account Initial Execution Request.
     * Mapped: initial Test Run Id with RerunDetails object.
     */
    private Map<UUID, RerunDetails> getRerunMapping(UUID initialErId, List<TestRun> testRuns, boolean isRerun) {
        Map<UUID, RerunDetails> rerunDetailsMapping = new HashMap<>();
        if (isRerun) {
            List<TestRun> initialTestRuns = testRunService.findAllByExecutionRequestId(initialErId);
            Map<UUID, TestRun> initialTestRunsMapping = StreamUtils.toIdEntityMap(initialTestRuns, TestRun::getUuid);
            testRuns.forEach(run -> {
                UUID initialTestRunId = run.getInitialTestRunId();
                TestRun initialTestRun = initialTestRunsMapping.get(initialTestRunId);
                RerunDetails rerunDetails = new RerunDetails();
                rerunDetails.setTestingStatus(run.getTestingStatus());
                if (!isNull(initialTestRun)) {
                    rerunDetails.setFirstStatus(initialTestRun.getTestingStatus());
                }
                rerunDetails.setFinalStatus(run.getTestingStatus());
                rerunDetails.setFinalRunData(new FinalRunData(run.getExecutionRequestId(), run.getUuid()));
                rerunDetailsMapping.put(run.getUuid(), rerunDetails);
            });
        } else {
            Map<UUID, TestRun> finalTrMapping = getFinalTrMapping(initialErId);
            testRuns.forEach(run -> {
                UUID testRunId = run.getUuid();
                TestRun finalTestRun = finalTrMapping.getOrDefault(testRunId, run);
                RerunDetails rerunDetails = new RerunDetails();
                rerunDetails.setTestingStatus(run.getTestingStatus());
                rerunDetails.setFinalRunData(new FinalRunData(finalTestRun.getExecutionRequestId(),
                        finalTestRun.getUuid()));
                rerunDetailsMapping.put(testRunId, rerunDetails);
            });
        }
        return rerunDetailsMapping;
    }

    /**
     * Update Test Runs with captured testing statuses.
     */
    void updateWithRerunDetailsRecursively(LabelNodeReportResponse rootNode,
                                           Map<UUID, RerunDetails> rerunDetailsMapping,
                                           WidgetConfigTemplate.Filters widgetConfigFilters) {
        List<TestRunNodeResponse> testRuns = rootNode.getTestRuns();
        if (CollectionUtils.isNotEmpty(testRuns)) {
            List<TestRunNodeResponse> processedTestRuns = testRuns.stream()
                    .peek(testRun -> {
                        RerunDetails details = rerunDetailsMapping.get(testRun.getUuid());
                        testRun.setTestingStatus(details.getTestingStatus());
                        testRun.setFirstStatus(details.getFirstStatus());
                        testRun.setFinalStatus(details.getFinalStatus());
                        testRun.setFinalRun(details.getFinalRunData());
                    })
                    .filter(testRun -> filterByWidgetConfigFilters(testRun, widgetConfigFilters))
                    .collect(Collectors.toList());
            rootNode.setTestRuns(processedTestRuns);
        }
        List<LabelNodeReportResponse> childrenNodes = rootNode.getChildren();
        if (CollectionUtils.isNotEmpty(childrenNodes)) {
            childrenNodes.forEach(childrenNode -> updateWithRerunDetailsRecursively(childrenNode, rerunDetailsMapping,
                    widgetConfigFilters));
        }
    }

    boolean filterByWidgetConfigFilters(TestRunNodeResponse testRunNode, WidgetConfigTemplate.Filters filters) {
        final Set<Predicate<TestRunNodeResponse>> conditions = new HashSet<>();

        if (nonNull(filters)) {
            final Set<TestingStatuses> firstStatuses = filters.getFirstStatuses();
            if (!isEmpty(firstStatuses)) {
                conditions.add(node -> firstStatuses.contains(node.getFirstStatus()));
            }

            final Set<TestingStatuses> finalStatuses = filters.getFinalStatuses();
            if (!isEmpty(finalStatuses)) {
                conditions.add(node -> finalStatuses.contains(node.getFinalStatus()));
            }

            return conditions.stream().allMatch(condition -> condition.test(testRunNode));
        }

        return true;
    }

    /**
     * Remove nodes without children nodes and test runs.
     *
     * @param rootNode root of the node tree
     */
    public void removeEmptyNodes(LabelNodeReportResponse rootNode) {
        TreeWalker<LabelNodeReportResponse> treeWalker = new TreeWalker<>();

        treeWalker.walkWithPostProcess(rootNode, LabelNodeReportResponse::getChildren, (root, child) -> {
            if (nonNull(root)) {
                List<LabelNodeReportResponse> rootChildren = root.getChildren();
                // filter only test run nodes or label and scope non empty nodes
                if (isEmpty(child.getChildren()) && isEmpty(child.getTestRuns())) {
                    List<LabelNodeReportResponse> nodesWithNonEmptyChildren = rootChildren.stream()
                            .filter(rootChild -> !rootChild.getLabelName().equals(child.getLabelName()))
                            .collect(Collectors.toList());
                    root.setChildren(nodesWithNonEmptyChildren);
                }

            }
        });
    }

    private LabelNodeReportResponse getTestScopeResponse(LabelNodeReportResponse rootNode, List<TestRun> testRuns,
                                                         UUID labelTemplateId,
                                                         ValidationLabelConfigTemplate validationTemplate,
                                                         TestRunsDataContext dataContext) {
        List<LabelNodeReportResponse> scopeGroupNodes = Arrays.asList(
                new LabelNodeReportResponse(TestScopeSections.PREREQUISITES.getName(), true,
                        () -> StreamUtils.filterByTestScopeSection(testRuns, TestRun::getTestScopeSection,
                                TestScopeSections.PREREQUISITES)),
                new LabelNodeReportResponse(TestScopeSections.EXECUTION.getName(), true,
                        () -> StreamUtils.filterByTestScopeSection(testRuns, TestRun::getTestScopeSection,
                                TestScopeSections.EXECUTION)),
                new LabelNodeReportResponse(TestScopeSections.VALIDATION.getName(), true,
                        () -> StreamUtils.filterByTestScopeSection(testRuns, TestRun::getTestScopeSection,
                                TestScopeSections.VALIDATION))
        );
        scopeGroupNodes.forEach(node -> {
            List<TestRun> scopeGroupTestRuns = node.getTestRunsFilterFunc().get();
            if (!scopeGroupTestRuns.isEmpty()) {
                List<TestRunNodeResponse> testRunResponses;

                if (nonNull(labelTemplateId)) {
                    LabelTemplate filledGroupNodeTemplate = labelTemplateNodeService.populateLabelTemplateWithTestRuns(
                            scopeGroupTestRuns, labelTemplateId);

                    List<LabelTemplate.LabelTemplateNode> labelNodes = filledGroupNodeTemplate.getLabelNodes();
                    fillLabelReportsNode(labelNodes, node, validationTemplate, scopeGroupTestRuns, dataContext);

                    Set<UUID> unknownTestRunIds = filledGroupNodeTemplate.getUnknownNode().getTestRunIds();
                    List<TestRun> unknownTestRuns = filterList(testRuns, unknownTestRunIds);

                    testRunResponses = testRunService.getTestRunNodeWithFailedLogRecords(unknownTestRuns,
                            validationTemplate, dataContext);
                } else {
                    scopeGroupTestRuns = treeNodeService.sortScopeGroupTestRuns(scopeGroupTestRuns);
                    testRunResponses = testRunService.getTestRunNodeWithFailedLogRecords(scopeGroupTestRuns,
                            validationTemplate, dataContext);
                }

                Map<String, TestingStatuses> labelParamMap = new HashMap<>();
                mergeLabelParams(labelParamMap, node.getChildren(), child -> child.getLabelParams().stream());
                node.setLabelParams(labelParamMap.entrySet()
                        .stream()
                        .map(entry -> new TestingReportLabelParam(entry.getKey(), entry.getValue()))
                        .collect(Collectors.toList())
                );

                node.setTestRuns(testRunResponses);
            }
        });
        rootNode.setChildren(scopeGroupNodes);
        setValidationLabelsOrder(rootNode, validationTemplate);

        return rootNode;
    }

    private void setValidationLabelsOrder(LabelNodeReportResponse rootNode, ValidationLabelConfigTemplate template) {
        final List<LabelNodeReportResponse> children = rootNode.getChildren();
        final List<TestRunNodeResponse> testRuns = rootNode.getTestRuns();
        Set<String> validationLabels = new HashSet<>();

        if (isEmpty(children) && isEmpty(testRuns)) {
            return;
        }

        if (!isEmpty(children)) {
            validationLabels = children.stream()
                    .filter(child -> !isEmpty(child.getLabelParams()))
                    .flatMap(child -> child.getLabelParams().stream())
                    .map(ReportLabelParam::getName)
                    .collect(Collectors.toSet());
        } else if (!isEmpty(testRuns)) {
            validationLabels = testRuns.stream()
                    .filter(testRun -> !isEmpty(testRun.getLabelParams()))
                    .flatMap(testRun -> testRun.getLabelParams().stream())
                    .map(ReportLabelParam::getName)
                    .collect(Collectors.toSet());
        }

        List<String> orderedValidationLabels = treeNodeService.orderValidationLabels(validationLabels, template);

        rootNode.setValidationLabelsOrder(orderedValidationLabels);
    }

    private LabelNodeReportResponse getLabelNodeReportResponseWithoutTemplate(List<TestRun> testRuns,
                                                                              ValidationLabelConfigTemplate template,
                                                                              TestRunsDataContext dataContext) {
        LabelNodeReportResponse labelNodeReportResponse = new LabelNodeReportResponse();
        List<TestRunNodeResponse> testRunsResponses =
                testRunService.getTestRunNodeWithFailedLogRecords(testRuns, template, dataContext);
        labelNodeReportResponse.setTestRuns(testRunsResponses);

        return labelNodeReportResponse;
    }

    private void fillLabelReportsNode(List<LabelTemplate.LabelTemplateNode> labelNodes,
                                      LabelNodeReportResponse rootNode,
                                      ValidationLabelConfigTemplate template,
                                      List<TestRun> testRuns,
                                      TestRunsDataContext dataContext) {
        labelNodes.forEach(labelTemplateNode -> {
            if (!LabelTemplate.UNKNOWN.equals(labelTemplateNode.getLabelName())) {
                LabelNodeReportResponse reportNode = new LabelNodeReportResponse();
                reportNode.setPassedRate(labelTemplateNode.getPassedRate());
                reportNode.setLabelName(labelTemplateNode.getLabelName());
                if (!isEmpty(labelTemplateNode.getTestRunIds())) {
                    List<TestRun> nodeTestRuns = filterList(testRuns, labelTemplateNode.getTestRunIds());
                    List<TestRunNodeResponse> testRunsForCurrentNode =
                            testRunService.getTestRunNodeWithFailedLogRecords(nodeTestRuns, template, dataContext);
                    reportNode.setTestRuns(testRunsForCurrentNode);
                }
                addNodeToReport(Collections.singletonList(reportNode), rootNode);

                if (!isEmpty(labelTemplateNode.getChildren())) {
                    fillLabelReportsNode(labelTemplateNode.getChildren(), reportNode, template, testRuns, dataContext);
                }
                calculateParamsForLabelReportNode(reportNode);
            }
        });

    }

    private void calculateParamsForLabelReportNode(LabelNodeReportResponse reportNode) {
        long duration = 0;
        List<TestingStatuses> currentStatuses = new ArrayList<>();
        Map<String, TestingStatuses> labelParamMap = new HashMap<>();

        List<LabelNodeReportResponse> children = reportNode.getChildren();
        if (!isEmpty(children)) {
            duration += children.stream()
                    .mapToLong(LabelNodeReportResponse::getDuration)
                    .sum();
            currentStatuses.addAll(children.stream()
                    .map(LabelNodeReportResponse::getStatus)
                    .collect(Collectors.toList())
            );
            mergeLabelParams(labelParamMap, children, node -> node.getLabelParams().stream());
        }

        List<TestRunNodeResponse> testRuns = reportNode.getTestRuns();
        if (!isEmpty(testRuns)) {
            duration += testRuns.stream()
                    .mapToLong(TestRunNodeResponse::getDuration).sum();
            currentStatuses.addAll(testRuns.stream()
                    .map(TestRunNodeResponse::getTestingStatus)
                    .collect(Collectors.toList()));
            mergeLabelParams(labelParamMap, testRuns, node -> node.getLabelParams().stream());
        }

        reportNode.setDuration(duration);
        reportNode.setStatus(calculateStatus(currentStatuses));
        reportNode.setLabelParams(labelParamMap.entrySet()
                .stream()
                .map(entry -> new TestingReportLabelParam(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList())
        );
    }

    private <T> void mergeLabelParams(Map<String, TestingStatuses> paramMap,
                                      Collection<T> entities,
                                      Function<? super T, ? extends Stream<? extends TestingReportLabelParam>> mapper) {
        BiFunction<TestingStatuses, TestingStatuses, TestingStatuses> statusMergeFunc = (oldStatus, newStatus) -> {
            if (TestingStatuses.FAILED.equals(newStatus) || TestingStatuses.FAILED.equals(oldStatus)) {
                return TestingStatuses.FAILED;
            }

            return newStatus;
        };

        entities.stream()
                .flatMap(mapper)
                .forEach(label -> paramMap.merge(label.getName(), label.getStatus(), statusMergeFunc));
    }

    private TestingStatuses calculateStatus(List<TestingStatuses> testingStatuses) {
        Optional<TestingStatuses> foundStatuses = testingStatuses.stream()
                .filter(statuses -> statuses.equals(TestingStatuses.FAILED)
                        || statuses.equals(TestingStatuses.STOPPED)).findAny();
        if (foundStatuses.isPresent()) {
            return TestingStatuses.FAILED;
        }
        foundStatuses =
                testingStatuses.stream().filter(TestingStatuses.WARNING::equals).findAny();
        if (foundStatuses.isPresent()) {
            return TestingStatuses.WARNING;
        }
        return TestingStatuses.PASSED;
    }

    private void addNodeToReport(List<LabelNodeReportResponse> children, LabelNodeReportResponse rootNode) {
        List<LabelNodeReportResponse> currentChildren = rootNode.getChildren();
        currentChildren.addAll(children);
        rootNode.setChildren(currentChildren);
    }

    /**
     * Generate {@link ExecutionSummaryResponse}.
     *
     * @param erId execution request identifier
     * @return {@link ExecutionSummaryResponse}
     */
    public ExecutionSummaryResponse getExecutionSummary(UUID erId, boolean isExecutionSummaryRunsSummary) {
        ExecutionRequest executionRequest = executionRequestRepository.findByUuid(erId);

        return getExecutionSummary(executionRequest, isExecutionSummaryRunsSummary);
    }


    /**
     * Generate {@link ExecutionSummaryResponse}.
     *
     * @param executionRequest execution request
     * @return {@link ExecutionSummaryResponse}
     */
    public ExecutionSummaryResponse getExecutionSummary(ExecutionRequest executionRequest,
                                                        boolean isExecutionSummaryRunsSummary) {
        UUID erId = executionRequest.getUuid();
        ExecutionSummaryResponse executionSummaryResponse = modelMapper.map(executionRequest,
                ExecutionSummaryResponse.class);
        List<TestRun> testRuns = testRunService.findTestRunForExecutionSummaryByExecutionRequestId(erId);

        List<TestRun> notInProgressTestRuns = testRuns.stream()
                .filter(testRun -> !ExecutionStatuses.IN_PROGRESS.equals(testRun.getExecutionStatus()))
                .collect(Collectors.toList());

        Scope testScope = catalogueService.getTestScope(executionRequest.getTestScopeId());
        Set<UUID> flagIds = executionRequest.getFlagIds();
        List<UUID> prerequisitesCases = testScope != null ? testScope.getPrerequisitesCases() : null;
        List<UUID> validationCases = testScope != null ? testScope.getValidationCases() : null;

        Predicate<TestRun> isNotIgnoredByScopeFlags = testRun -> !rateCalculator
                .isTestRunIgnoredByFlag(testRun, flagIds, prerequisitesCases, validationCases);

        int inProgressTrCount;
        if (isExecutionSummaryRunsSummary) {
            List<TestRun> summarizeTestRuns = getSummarizeTestRuns(
                    executionRequest.getInitialExecutionRequestId(), notInProgressTestRuns);
            notInProgressTestRuns = summarizeTestRuns;
            inProgressTrCount = (int) summarizeTestRuns.stream()
                    .filter(testRun -> ExecutionStatuses.IN_PROGRESS.equals(testRun.getExecutionStatus()))
                    .count();
        } else {
            inProgressTrCount = getInProgressTestRunCount(testRuns.size(), notInProgressTestRuns.size());
        }

        notInProgressTestRuns.stream()
                .filter(isNotIgnoredByScopeFlags)
                .forEach(testRun -> {
                    if (!Strings.isNullOrEmpty(testRun.getUrlToBrowserSession())) {
                        List<String> browserSessions = executionSummaryResponse.getBrowserSessionLink();
                        browserSessions.add(testRun.getUrlToBrowserSession());
                        executionSummaryResponse.setBrowserSessionLink(browserSessions);
                    }
                    updateStatusCount(testRun, TestingStatuses.PASSED, executionSummaryResponse.getPassedCount(),
                            executionSummaryResponse::setPassedCount);
                    updateStatusCount(testRun, TestingStatuses.WARNING, executionSummaryResponse.getWarningCount(),
                            executionSummaryResponse::setWarningCount);
                    updateStatusCount(testRun, TestingStatuses.FAILED, executionSummaryResponse.getFailedCount(),
                            executionSummaryResponse::setFailedCount);
                    updateStatusCount(testRun, TestingStatuses.STOPPED, executionSummaryResponse.getStoppedCount(),
                            executionSummaryResponse::setStoppedCount);
                    updateStatusCount(testRun, TestingStatuses.SKIPPED, executionSummaryResponse.getSkippedCount(),
                            executionSummaryResponse::setSkippedCount);
                    updateStatusCount(testRun, TestingStatuses.BLOCKED, executionSummaryResponse.getBlockedCount(),
                            executionSummaryResponse::setBlockedCount);
                    updateStatusCount(testRun, TestingStatuses.NOT_STARTED,
                            executionSummaryResponse.getNotStartedCount(),
                            executionSummaryResponse::setNotStartedCount);
                });

        int trCount = getTotalNumberOfTestRun(executionSummaryResponse) + inProgressTrCount;

        executionSummaryResponse.setInProgressCount(inProgressTrCount);
        calculateRates(executionSummaryResponse, trCount);

        Set<UUID> labelIds = executionRequest.getFilteredByLabels();
        if (CollectionUtils.isNotEmpty(labelIds)) {
            executionSummaryResponse.setLabels(labelsService.getLabels(labelIds));
        }

        final String envLink = getEnvLink(executionRequest);
        final String envName = environmentsService.getEnvironmentNameById(executionRequest.getEnvironmentId());

        executionSummaryResponse.setEnvironmentLink(envLink);
        executionSummaryResponse.setEnvironmentName(envName);
        executionSummaryResponse.setTestCasesCount(trCount);

        if (nonNull(testScope)) {
            final String scopeName = testScope.getName();
            final String scopeLink = getScopeLink(executionRequest, testScope);

            executionSummaryResponse.setScopeName(scopeName);
            executionSummaryResponse.setScopeLink(scopeLink);
        }
        return executionSummaryResponse;
    }


    protected List<TestRun> getSummarizeTestRuns(UUID initialErId, List<TestRun> testRuns) {
        List<TestRun> initialTestRuns = testRunService.findAllByExecutionRequestId(initialErId);
        List<TestRun> summaryTestRuns = new ArrayList<>();

        initialTestRuns.forEach(initialTestRun -> {
            UUID initialTestRunId = initialTestRun.getUuid();
            Optional<TestRun> matchRun = testRuns.stream()
                    .filter(testRun -> testRun.getInitialTestRunId().equals(initialTestRunId))
                    .findFirst();

            TestRun summaryTestRun;
            if (matchRun.isPresent() && matchRun.get().isFinalTestRun() && !initialTestRun.isFinalTestRun()) {
                summaryTestRun = matchRun.get();
            } else if (initialTestRun.isFinalTestRun()) {
                summaryTestRun = initialTestRun;
            } else {
                summaryTestRun = matchRun.orElse(initialTestRun);
            }
            summaryTestRuns.add(summaryTestRun);
        });

        return summaryTestRuns;
    }

    private String getScopeLink(ExecutionRequest executionRequest, Scope scope) {
        final UUID testScopeId = scope.getUuid();
        final UUID projectId = executionRequest.getProjectId();
        final UUID testPlanId = executionRequest.getTestPlanId();

        return catalogueUrl + "/project/" + projectId + "/plan/" + testPlanId + "/scopes/" + testScopeId;
    }

    private String getEnvLink(ExecutionRequest executionRequest) {
        final UUID projectId = executionRequest.getProjectId();
        final UUID environmentId = executionRequest.getEnvironmentId();

        return catalogueUrl + "/project/" + projectId + "/environments/environment/" + environmentId;
    }

    private void calculateRates(ExecutionSummaryResponse erSummary, int trCount) {
        if (trCount != 0) {
            erSummary.setPassedRate(RateCalculator.calculateRateFloat(erSummary.getPassedCount(), trCount));
            erSummary.setFailedRate(RateCalculator.calculateRateFloat(erSummary.getFailedCount(), trCount));
            erSummary.setStoppedRate(RateCalculator.calculateRateFloat(erSummary.getStoppedCount(), trCount));
            erSummary.setWarningRate(RateCalculator.calculateRateFloat(erSummary.getWarningCount(), trCount));
            erSummary.setNotStartedRate(RateCalculator.calculateRateFloat(erSummary.getNotStartedCount(), trCount));
            erSummary.setBlockedRate(RateCalculator.calculateRateFloat(erSummary.getBlockedCount(),
                    erSummary.getFailedCount() + erSummary.getBlockedCount()));
        }
    }

    private int getTotalNumberOfTestRun(ExecutionSummaryResponse executionSummaryResponse) {
        return executionSummaryResponse.getPassedCount()
                + executionSummaryResponse.getFailedCount()
                + executionSummaryResponse.getWarningCount()
                + executionSummaryResponse.getNotStartedCount()
                + executionSummaryResponse.getStoppedCount();
    }

    private int getInProgressTestRunCount(int allTestRunCount, int finishedTestRunCount) {
        return allTestRunCount - finishedTestRunCount;
    }

    private void updateStatusCount(TestRun testRun, TestingStatuses currentStatus, int currentCount,
                                   Consumer<Integer> setMethod) {
        if (currentStatus.equals(testRun.getTestingStatus())) {
            setMethod.accept(++currentCount);
        }
    }
}
