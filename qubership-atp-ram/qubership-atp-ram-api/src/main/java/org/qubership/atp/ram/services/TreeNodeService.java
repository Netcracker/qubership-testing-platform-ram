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
import static org.qubership.atp.ram.utils.StreamUtils.filterByTestScopeSection;
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
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections.CollectionUtils;
import org.qubership.atp.ram.constants.CacheConstants;
import org.qubership.atp.ram.dto.response.ExecutionRequestWidgetConfigTemplateResponse;
import org.qubership.atp.ram.entities.treenodes.ExecutionRequestTreeNode;
import org.qubership.atp.ram.entities.treenodes.LabelTemplateTreeNode;
import org.qubership.atp.ram.entities.treenodes.LogRecordTreeNode;
import org.qubership.atp.ram.entities.treenodes.ScopeGroupTreeNode;
import org.qubership.atp.ram.entities.treenodes.TestRunTreeNode;
import org.qubership.atp.ram.entities.treenodes.TreeNode;
import org.qubership.atp.ram.entities.treenodes.TreeNodeType;
import org.qubership.atp.ram.entities.treenodes.labelparams.CountReportLabelParam;
import org.qubership.atp.ram.entities.treenodes.labelparams.ReportLabelParam;
import org.qubership.atp.ram.entities.treenodes.labelparams.TestingReportLabelParam;
import org.qubership.atp.ram.enums.ExecutionRequestWidgets;
import org.qubership.atp.ram.enums.TestScopeSections;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.model.LogRecordFilteringRequest;
import org.qubership.atp.ram.model.datacontext.TestRunsDataContext;
import org.qubership.atp.ram.model.datacontext.TestRunsDataContextLoadOptions;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.LabelTemplate;
import org.qubership.atp.ram.models.LabelTemplate.LabelTemplateNode;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.models.ValidationLabelConfigTemplate;
import org.qubership.atp.ram.models.WidgetConfigTemplate;
import org.qubership.atp.ram.models.logrecords.parts.ValidationTable;
import org.qubership.atp.ram.models.logrecords.parts.ValidationTableLine;
import org.qubership.atp.ram.models.tree.TreeWalker;
import org.qubership.atp.ram.utils.PercentUtils;
import org.qubership.atp.ram.utils.StreamUtils;
import org.qubership.atp.ram.utils.Utils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TreeNodeService {

    public static final String ER_SUFFIX = "-[ER]";
    public static final String AR_SUFFIX = "-[AR]";

    private TestRunService testRunService;
    private ExecutionRequestService executionRequestService;
    private LogRecordService logRecordService;
    private LabelTemplateNodeService labelTemplateNodeService;
    private WidgetConfigTemplateService widgetConfigTemplateService;
    private ValidationLabelConfigTemplateService validationLabelConfigTemplateService;
    private ObjectMapper objectMapper;

    private BiFunction<TestingReportLabelParam, TestingReportLabelParam, TestingReportLabelParam> statusMergeFunc =
            (oldLabel, newLabel) -> {
                final TestingStatuses oldStatus = oldLabel.getStatus();
                final TestingStatuses newStatus = newLabel.getStatus();
                if (TestingStatuses.FAILED.equals(newStatus) || TestingStatuses.FAILED.equals(oldStatus)) {
                    return TestingStatuses.FAILED.equals(newStatus) ? newLabel : oldLabel;
                }
                return newLabel;
            };

    /**
     * TreeNodeService constructor.
     */
    public TreeNodeService(@Lazy TestRunService testRunService,
                           @Lazy ExecutionRequestService executionRequestService,
                           LogRecordService logRecordService,
                           @Lazy LabelTemplateNodeService labelTemplateNodeService,
                           @Lazy WidgetConfigTemplateService widgetConfigTemplateService,
                           @Lazy ValidationLabelConfigTemplateService validationLabelConfigTemplateService,
                           @Lazy ObjectMapper objectMapper) {
        this.testRunService = testRunService;
        this.executionRequestService = executionRequestService;
        this.logRecordService = logRecordService;
        this.labelTemplateNodeService = labelTemplateNodeService;
        this.widgetConfigTemplateService = widgetConfigTemplateService;
        this.validationLabelConfigTemplateService = validationLabelConfigTemplateService;
        this.objectMapper = objectMapper;
    }

    /**
     * Gets project id by execution request id.
     */
    public UUID getProjectIdByExecutionRequestId(UUID id) {
        return executionRequestService.getProjectIdByExecutionRequestId(id);
    }

    /**
     * Return id of the project that owns the test run by test run id.
     */
    public UUID getProjectIdByTestRunId(UUID testRunId) {
        return testRunService.getProjectIdByTestRunId(testRunId);
    }

    /**
     * Get execution request tree.
     *
     * @param labelTemplateId    label template id
     * @param executionRequestId execution request id
     * @return tree node
     */
    public TreeNode getExecutionRequestTree(UUID executionRequestId, UUID labelTemplateId, boolean includeAll) {
        log.info("Get tree for execution request with id '{}'", executionRequestId);
        final ExecutionRequest executionRequest = executionRequestService.findById(executionRequestId);
        if (nonNull(labelTemplateId)) {
            return getExecutionRequestTree(executionRequest, labelTemplateId, null, false, includeAll);
        } else {
            labelTemplateId = executionRequest.getLabelTemplateId();
        }
        final ExecutionRequestWidgetConfigTemplateResponse configResponse =
                widgetConfigTemplateService.getWidgetConfigTemplateForEr(executionRequest);
        final WidgetConfigTemplate widgetConfigTemplate = configResponse.getTemplate();
        final boolean defaultLabelTemplateChanged = configResponse.isDefaultLabelTemplateChanged();
        if (nonNull(widgetConfigTemplate) && (defaultLabelTemplateChanged || isNull(labelTemplateId))) {
            final UUID summaryStatisticWidgetId = ExecutionRequestWidgets.SUMMARY_STATISTIC.getWidgetId();
            labelTemplateId = widgetConfigTemplate.getWidgetConfig(summaryStatisticWidgetId).getLabelTemplateId();
        }
        return getExecutionRequestTree(executionRequest, labelTemplateId, null, false, includeAll);
    }

    /**
     * Get execution request tree.
     *
     * @param executionRequest execution request
     * @param labelTemplateId  label validationTemplate id
     * @return execution request tree
     */
    public TreeNode getExecutionRequestTree(ExecutionRequest executionRequest,
                                            UUID labelTemplateId,
                                            ValidationLabelConfigTemplate validationTemplate,
                                            boolean isWidget,
                                            boolean includeAll) {
        final UUID executionRequestId = executionRequest.getUuid();
        final UUID testScopeId = executionRequest.getTestScopeId();
        log.info("Get tree for execution request '{}' with label validation template '{}'",
                executionRequest, labelTemplateId);
        final List<TestRun> testRuns = testRunService.findAllByExecutionRequestId(executionRequestId);
        TestRunsDataContextLoadOptions dataContextLoadOptions = new TestRunsDataContextLoadOptions()
                .includeRunValidationLogRecordsMap(isWidget)
                .includeRunTestCasesMap()
                .includeTestRunMap();
        TestRunsDataContext dataContext = testRunService.getTestRunsDataContext(testRuns, dataContextLoadOptions,
                executionRequest.isVirtual());
        final ExecutionRequestTreeNode erRootNode = new ExecutionRequestTreeNode(executionRequest);
        boolean isScopeRun = nonNull(testScopeId);
        log.debug("Scope run: {}", isScopeRun);
        LabelTemplate labelTemplate = null;
        if (nonNull(labelTemplateId)) {
            labelTemplate = labelTemplateNodeService.getLabelTemplate(labelTemplateId);
            erRootNode.setLabelTemplateData(labelTemplate);
            log.debug("Label validationTemplate set: {}", labelTemplate);
        }
        TreeNode resultTree;
        if (isScopeRun && !isWidget) {
            resultTree = getExecutionRequestScopeTree(erRootNode, dataContext, labelTemplate, isWidget, includeAll);
        } else {
            resultTree = getExecutionRequestTree(erRootNode, dataContext, labelTemplate, validationTemplate, isWidget,
                    includeAll);
        }
        removeEmptyNodes(resultTree);
        if (erRootNode.isExecutionRequestVirtual()) {
            setVirtualForEachChildInResultTree(resultTree);
        }
        return resultTree;
    }

    /**
     * Get execution request tre.
     *
     * @param executionRequestId execution request id
     * @return tree node
     */
    public TreeNode getExecutionRequestTree(UUID executionRequestId,
                                            boolean includeTestRuns,
                                            boolean includeLogRecords) {
        final ExecutionRequest executionRequest = executionRequestService.findById(executionRequestId);
        final ExecutionRequestTreeNode executionRequestRootNode = new ExecutionRequestTreeNode(executionRequest);
        if (includeTestRuns) {
            List<TestRun> topLevelTestRuns = testRunService.findAllByExecutionRequestId(executionRequestId);
            List<TreeNode> topLevelTestRunNodes = topLevelTestRuns.stream()
                    .map(testRun -> getTestRunNode(testRun, includeLogRecords, null))
                    .sorted(Comparator.comparing(TreeNode::getName))
                    .collect(Collectors.toList());
            executionRequestRootNode.setChildren(topLevelTestRunNodes);
        }
        if (executionRequestRootNode.isExecutionRequestVirtual()) {
            setVirtualForEachChildInResultTree(executionRequestRootNode);
        }
        return executionRequestRootNode;
    }

    private TreeNode getExecutionRequestTree(ExecutionRequestTreeNode erRootNode,
                                             TestRunsDataContext dataContext,
                                             LabelTemplate labelTemplate,
                                             ValidationLabelConfigTemplate template,
                                             boolean isWidget,
                                             boolean includeAll) {
        log.debug("Create and fill ER sub node, root node '{}', label template '{}'", erRootNode, labelTemplate);
        final UUID executionRequestId = erRootNode.getExecutionRequestId();
        final Map<UUID, TestRun> testRunsMap = dataContext.getTestRunsMap();
        final List<TestRun> testRuns = new ArrayList<>(testRunsMap.values())
                .stream().sorted(Comparator.comparing(TestRun::getName)).collect(Collectors.toList());
        List<TreeNode> topLevelLabelTreeNodes;
        if (nonNull(labelTemplate)) {
            LabelTemplate filledLabelTemplate =
                    labelTemplateNodeService.populateLabelTemplateWithTestRuns(testRuns, labelTemplate);
            List<LabelTemplateNode> topLevelLabelNodes = filledLabelTemplate.getLabelNodes();
            topLevelLabelTreeNodes = getTreeNodes(topLevelLabelNodes, null, topLevelNode ->
                    convertToTreeNode(executionRequestId, topLevelNode, template, dataContext, isWidget, includeAll));
        } else {
            topLevelLabelTreeNodes = getTreeNodes(testRuns, null,
                    includeAll ? testRun -> this.getTestRunNodeIncludeAllSteps(testRun, dataContext)
                            : testRun -> this.getTestRunNode(testRun, false, null, dataContext));
        }
        erRootNode.getChildren().addAll(topLevelLabelTreeNodes);
        setValidationLabelsOrder(erRootNode, template);
        return erRootNode;
    }

    private void setVirtualForEachChildInResultTree(TreeNode rootNode) {
        if (isEmpty(rootNode.getChildren())) {
            return;
        }
        rootNode.getChildren().forEach(childNode -> {
            childNode.setExecutionRequestVirtual(true);
            setVirtualForEachChildInResultTree(childNode);
        });
    }

    /**
     * Remove nodes without children nodes.
     *
     * @param rootNode root of the node tree
     */
    public void removeEmptyNodes(TreeNode rootNode) {
        TreeWalker<TreeNode> treeWalker = new TreeWalker<>();
        Predicate<TreeNode> isLabelOrScopeTreeNode = treeNode ->
                treeNode instanceof LabelTemplateTreeNode || treeNode instanceof ScopeGroupTreeNode;
        treeWalker.walkWithPostProcess(rootNode, TreeNode::getChildren, (root, child) -> {
            if (nonNull(root) && isLabelOrScopeTreeNode.test(child)) {
                List<TreeNode> rootChildren = root.getChildren();
                // filter only test run nodes or label and scope non empty nodes
                List<TreeNode> nodesWithNonEmptyChildren = rootChildren.stream()
                        .filter(rootChild -> rootChild instanceof TestRunTreeNode
                                || isLabelOrScopeTreeNode.test(rootChild) && !isEmpty(rootChild.getChildren()))
                        .collect(Collectors.toList());
                root.setChildren(nodesWithNonEmptyChildren);
            }
        });
    }

    private void setValidationLabelsOrder(ExecutionRequestTreeNode erRootNode,
                                          ValidationLabelConfigTemplate template) {
        final List<TreeNode> children = erRootNode.getChildren();
        Set<String> reportLabelParams = getReportLabelParams(children);
        erRootNode.setValidationLabelsOrder(orderValidationLabels(reportLabelParams, template));
    }

    /**
     * Order validation labels according to the template.
     *
     * @param reportLabelParams validation labels
     * @param template          validation template
     * @return ordered labels list
     */
    public List<String> orderValidationLabels(Set<String> reportLabelParams,
                                              ValidationLabelConfigTemplate template) {
        final List<String> resultParams = new ArrayList<>();
        if (nonNull(template)) {
            final Function<ValidationLabelConfigTemplate.LabelConfig, String> labelNameResolveFunc = labelConfig ->
                    StringUtils.isEmpty(labelConfig.getColumnName())
                            ? labelConfig.resolveColumnName() : labelConfig.getColumnName();
            TreeSet<ValidationLabelConfigTemplate.LabelConfig> templateLabels = template.getLabels();
            // found not displayed labels
            Set<String> nonDisplayedParams = templateLabels.stream()
                    .filter(labelConfig -> !labelConfig.isDisplayed())
                    .map(labelConfig -> labelNameResolveFunc.apply(labelConfig).toLowerCase())
                    .collect(Collectors.toSet());
            List<String> displayedTemplateParams = templateLabels
                    .stream()
                    .filter(ValidationLabelConfigTemplate.LabelConfig::isDisplayed)
                    .map(labelNameResolveFunc)
                    .collect(Collectors.toList());
            Set<String> templateOverrides = templateLabels
                    .stream()
                    .filter(labelConfig -> !isEmpty(labelConfig.getLabelNames())
                            && labelConfig.getLabelNames().size() == 1)
                    .map(labelConfig -> labelConfig.getLabelNames().stream().findFirst().get())
                    .collect(Collectors.toSet());
            List<String> templateParamsDiff = reportLabelParams.stream()
                    .filter(label -> !templateOverrides.contains(label) && !displayedTemplateParams.contains(label)
                            && !nonDisplayedParams.contains(label.toLowerCase()))
                    .sorted()
                    .collect(Collectors.toList());
            Set<String> displayedParamsWithErAr = templateLabels.stream()
                    .filter(labelConfig -> labelConfig.isDisplayed() && labelConfig.isDisplayErAr())
                    .map(labelNameResolveFunc)
                    .collect(Collectors.toSet());
            List<String> templateParams = new ArrayList<>();
            displayedTemplateParams.forEach(label -> {
                templateParams.add(label);
                if (displayedParamsWithErAr.contains(label)) {
                    templateParams.add(label + ER_SUFFIX);
                    templateParams.add(label + AR_SUFFIX);
                }
            });
            log.debug("Add labels [{}], template labels [{}]", templateParamsDiff, displayedTemplateParams);
            resultParams.addAll(templateParamsDiff);
            resultParams.addAll(templateParams);
        } else {
            log.debug("Add labels [{}]", reportLabelParams);
            resultParams.addAll(reportLabelParams);
        }
        return resultParams;
    }

    private Set<String> getReportLabelParams(List<TreeNode> children) {
        Set<String> labelTemplateNodesLabelNames = children.stream()
                .filter(node -> node.getClass().equals(LabelTemplateTreeNode.class))
                .map(node -> (LabelTemplateTreeNode) node)
                .flatMap(node -> node.getReportLabelParams().stream())
                .map(ReportLabelParam::getName)
                .collect(Collectors.toSet());
        return labelTemplateNodesLabelNames;
    }

    /**
     * Sort scope group test runs.
     *
     * @param scopeGroupTestRuns scope test rubs
     * @return result list
     */
    public List<TestRun> sortScopeGroupTestRuns(List<TestRun> scopeGroupTestRuns) {
        log.debug("Sort test group test runs [{}]", StreamUtils.extractIds(scopeGroupTestRuns));
        scopeGroupTestRuns.sort(Comparator.comparingInt(o -> o.getOrder()));
        log.debug("Sorted test runs: [{}]", StreamUtils.extractIds(scopeGroupTestRuns));
        return scopeGroupTestRuns;
    }

    private <T> List<TreeNode> getTreeNodes(List<T> testRuns,
                                            Comparator<TreeNode> order,
                                            Function<T, TreeNode> convertFunc) {
        Stream<TreeNode> stream = testRuns.stream().map(convertFunc);
        if (nonNull(order)) {
            stream = stream.sorted(order);
        }
        return stream.collect(Collectors.toList());
    }

    /**
     * Get execution request tree grouped by scope sections: PREREQUISITES, EXECUTION, VALIDATION.
     *
     * @param executionRequestRootNode tree ER root node
     * @param dataContext              data context
     * @param labelTemplate            label template(if set, possible null)
     * @param isWidget                 flag (widget or not)
     * @return result tree
     */
    public TreeNode getExecutionRequestScopeTree(ExecutionRequestTreeNode executionRequestRootNode,
                                                 TestRunsDataContext dataContext,
                                                 LabelTemplate labelTemplate,
                                                 boolean isWidget,
                                                 boolean includeAll) {
        log.debug("Get execution request tree grouped by scope sections");
        final UUID executionRequestId = executionRequestRootNode.getExecutionRequestId();
        final Map<UUID, TestRun> testRunsMap = dataContext.getTestRunsMap();
        final List<TestRun> testRuns = nonNull(labelTemplate) ? new ArrayList<>(testRunsMap.values()) :
                new ArrayList<>(testRunsMap.values()).stream()
                        .sorted(Comparator.comparing(TestRun::getName)).collect(Collectors.toList());
        List<ScopeGroupTreeNode> scopeGroupTreeNodes = Arrays.asList(
                new ScopeGroupTreeNode(executionRequestId, TestScopeSections.PREREQUISITES.getName(),
                        () -> filterByTestScopeSection(testRuns, TestRun::getTestScopeSection,
                                TestScopeSections.PREREQUISITES)),
                new ScopeGroupTreeNode(executionRequestId, TestScopeSections.EXECUTION.getName(),
                        () -> filterByTestScopeSection(testRuns, TestRun::getTestScopeSection,
                                TestScopeSections.EXECUTION)),
                new ScopeGroupTreeNode(executionRequestId, TestScopeSections.VALIDATION.getName(),
                        () -> filterByTestScopeSection(testRuns, TestRun::getTestScopeSection,
                                TestScopeSections.VALIDATION))
        );
        log.debug("Created test scope grouping sections: {}", scopeGroupTreeNodes);
        scopeGroupTreeNodes.forEach(node -> {
            List<TestRun> scopeGroupTestRuns = node.getTestRunsFilterFunc().get();
            List<TreeNode> topLevelLabelTreeNodes = new ArrayList<>();
            if (!isEmpty(scopeGroupTestRuns)) {
                if (nonNull(labelTemplate)) {
                    LabelTemplate filledLabelTemplate = labelTemplateNodeService.populateLabelTemplateWithTestRuns(
                            scopeGroupTestRuns, labelTemplate.clone());
                    executionRequestRootNode.setLabelTemplateData(labelTemplate);
                    List<LabelTemplateNode> topLevelNodes = filledLabelTemplate.getLabelNodes();
                    topLevelLabelTreeNodes = getTreeNodes(topLevelNodes, null, topLevelNode ->
                            convertToTreeNode(executionRequestId, topLevelNode, null, dataContext, isWidget,
                                    includeAll));
                } else {
                    scopeGroupTestRuns = sortScopeGroupTestRuns(scopeGroupTestRuns);
                    topLevelLabelTreeNodes = getTreeNodes(scopeGroupTestRuns, null,
                            includeAll ? testRun -> this.getTestRunNodeIncludeAllSteps(testRun, dataContext)
                                    : testRun -> this.getTestRunNode(testRun, false, null, dataContext));
                }
            }
            node.setChildren(topLevelLabelTreeNodes);
            executionRequestRootNode.getChildren().add(node);
        });
        return executionRequestRootNode;
    }

    /**
     * Get execution request tree for specified executionRequestId by node name.
     *
     * @param executionRequestId execution request id
     * @param searchValue        part of test run name to search
     * @return List of tree nodes
     */
    public Set<TreeNode> getExecutionRequestTreeNodesByName(UUID executionRequestId,
                                                            String searchValue) {
        log.info("Get tree nodes for executionRequestId='{}' and name like '{}'",
                executionRequestId,
                searchValue);
        Set<TreeNode> treeNodes = new HashSet<>();
        List<TestRun> allTestRuns = testRunService.findAllByExecutionRequestId(executionRequestId);
        allTestRuns
                .stream()
                .filter(testRun -> testRun.getName().toLowerCase().contains(searchValue.toLowerCase()))
                .forEach(matchedTestRun -> treeNodes.add(new TestRunTreeNode(matchedTestRun)));
        allTestRuns.forEach(testRun -> treeNodes.addAll(getLogRecordNodesByName(testRun, searchValue)));
        return treeNodes;
    }

    private LabelTemplateTreeNode convertToTreeNode(UUID executionRequestId,
                                                    LabelTemplateNode labelTemplateNode,
                                                    ValidationLabelConfigTemplate template,
                                                    TestRunsDataContext dataContext,
                                                    boolean isWidget,
                                                    boolean includeAll) {
        LabelTemplateTreeNode labelTreeNode = new LabelTemplateTreeNode(labelTemplateNode, executionRequestId);
        Map<UUID, TestRun> testRunsMap = dataContext.getTestRunsMap();
        List<LabelTemplateNode> labelTemplateNodeChildren = labelTemplateNode.getChildren();
        setLabelTemplateTreeNodeChildren(
                labelTreeNode,
                labelTemplateNodeChildren,
                childNode -> convertToTreeNode(executionRequestId, childNode, template, dataContext, isWidget,
                        includeAll)
        );
        List<UUID> testRunIds = labelTemplateNode.getTestRunIds()
                .stream()
                .distinct()
                .filter(testRunsMap::containsKey)
                .sorted(Comparator.comparing((key) -> testRunsMap.get(key).getName()))
                .collect(Collectors.toList());
        setLabelTemplateTreeNodeChildren(
                labelTreeNode,
                testRunIds,
                includeAll ? testRunId -> getTestRunNodeIncludeAllSteps(testRunsMap.get(testRunId), dataContext)
                        : testRunId -> getTestRunNode(testRunsMap.get(testRunId), false, null, dataContext)
        );
        log.debug("convertLabelTemplateNodeToTreeNode: will be calculate params {}", isWidget);
        if (isWidget) {
            calculateReportLabelParams(labelTreeNode, template, dataContext);
        }
        return labelTreeNode;
    }

    /**
     * Calculate label report params.
     *
     * @param labelTreeNode label tree node
     * @param dataContext   data context
     */
    private void calculateReportLabelParams(LabelTemplateTreeNode labelTreeNode,
                                            ValidationLabelConfigTemplate template,
                                            TestRunsDataContext dataContext) {
        Map<String, CountReportLabelParam> nodeLabelParamMap = labelTreeNode.getReportLabelParams()
                .stream()
                .collect(Collectors.toMap(ReportLabelParam::getName, Function.identity()));
        List<TreeNode> children = labelTreeNode.getChildren();
        if (!isEmpty(children)) {
            calculateTestRunNodesReportLabelParams(children, template, nodeLabelParamMap, dataContext);
            calculateLabelNodesReportLabelParams(children, nodeLabelParamMap);
            nodeLabelParamMap.values().forEach(param -> {
                int passed = param.getPassedCount();
                int nodeTestRunCount = labelTreeNode.getTestRunCount();
                int passAndFailCountSum = param.getPassedCount() + param.getFailedCount();
                int total = nonNull(template) && template.isUseTcCount() ? nodeTestRunCount : passAndFailCountSum;
                param.setTotalCount(total);
                if (total > 0) {
                    param.setPassed(PercentUtils.round(passed * 100.0 / total));
                }
            });
        }
        labelTreeNode.setReportLabelParams(new ArrayList<>(nodeLabelParamMap.values()));
    }

    private void calculateTestRunNodesReportLabelParams(List<TreeNode> childrenLabelNodes,
                                                        ValidationLabelConfigTemplate template,
                                                        Map<String, CountReportLabelParam> parentLabelNodeParamsMap,
                                                        TestRunsDataContext dataContext) {
        final Map<UUID, List<LogRecord>> testRunLogRecordsMap = dataContext.getTestRunValidationLogRecordsMap();
        childrenLabelNodes.stream()
                .filter(node -> TreeNodeType.TEST_RUN_NODE.equals(node.getNodeType()))
                .map(node -> (TestRunTreeNode) node)
                .forEach(testRunTreeNode -> {
                    final UUID testRunId = testRunTreeNode.getTestRunId();
                    List<TestingReportLabelParam> labelParamsMap =
                            getTestRunValidationLabels(testRunId, template, testRunLogRecordsMap);
                    labelParamsMap.forEach(labelParam -> {
                        final String paramName = labelParam.getName();
                        final TestingStatuses paramStatus = labelParam.getStatus();
                        CountReportLabelParam countLabelParam = parentLabelNodeParamsMap.get(paramName);
                        if (isNull(countLabelParam)) {
                            countLabelParam = new CountReportLabelParam(paramName);
                            parentLabelNodeParamsMap.put(paramName, countLabelParam);
                        }
                        if (TestingStatuses.PASSED.equals(paramStatus)) {
                            countLabelParam.incrPassed();
                        } else {
                            countLabelParam.incrFailed();
                        }
                    });
                });
    }

    private void calculateLabelNodesReportLabelParams(List<TreeNode> childrenTestRunNodes,
                                                      Map<String, CountReportLabelParam> parentNodeLabelParamMap) {
        childrenTestRunNodes.stream()
                .filter(node -> TreeNodeType.LABEL_TEMPLATE_NODE.equals(node.getNodeType()))
                .map(node -> (LabelTemplateTreeNode) node)
                .flatMap(node -> node.getReportLabelParams().stream())
                .forEach(reportLabelParam -> {
                    final String paramName = reportLabelParam.getName();
                    CountReportLabelParam reportLabelParamData = parentNodeLabelParamMap.get(paramName);
                    if (isNull(reportLabelParamData)) {
                        reportLabelParamData = new CountReportLabelParam(paramName);
                        parentNodeLabelParamMap.put(paramName, reportLabelParamData);
                    }
                    int failedCount = reportLabelParam.getFailedCount();
                    reportLabelParamData.setFailedCount(reportLabelParamData.getFailedCount() + failedCount);
                    int passedCount = reportLabelParam.getPassedCount();
                    reportLabelParamData.setPassedCount(reportLabelParamData.getPassedCount() + passedCount);
                });
    }

    /**
     * Get test run validation labels.
     *
     * @param testRunId test run id
     * @param template  validation template
     * @return result labels list
     */
    public List<TestingReportLabelParam> getTestRunValidationLabels(UUID testRunId,
                                                                    ValidationLabelConfigTemplate template,
                                                                    Map<UUID, List<LogRecord>> testRunLogRecordsMap) {
        log.debug("Get validation labels map for test run '{}'", testRunId);
        List<LogRecord> logRecords = testRunLogRecordsMap.getOrDefault(testRunId, Collections.emptyList());
        Map<String, TestingReportLabelParam> validationLabelMap = new HashMap<>();
        logRecords.forEach(logRecord -> {
            final Set<String> logRecordValidationLabels = logRecord.getValidationLabels();
            if (!isEmpty(logRecordValidationLabels)) {
                final TestingStatuses status = logRecord.getTestingStatus();
                logRecordValidationLabels.forEach(label -> {
                    validationLabelMap.merge(label, new TestingReportLabelParam(label, status), statusMergeFunc);
                });
                if (nonNull(template)) {
                    processValidationLabelTemplate(validationLabelMap, logRecordValidationLabels, status, template);
                }
            }
            final ValidationTable validationTable = logRecord.getValidationTable();
            if (nonNull(validationTable)) {
                final List<ValidationTableLine> validationSteps = validationTable.getSteps();
                if (!isEmpty(validationSteps)) {
                    validationSteps.forEach(step -> {
                        final Set<String> stepValidationLabels = step.getValidationLabels();
                        final TestingStatuses status = step.getStatus();
                        if (CollectionUtils.isNotEmpty(stepValidationLabels)) {
                            stepValidationLabels.forEach(label -> {
                                TestingReportLabelParam newLabelParam = new TestingReportLabelParam(label, status);
                                validationLabelMap.merge(label, newLabelParam, statusMergeFunc);
                            });
                        }
                        if (nonNull(template)) {
                            processValidationLabelTemplate(logRecord, step, validationLabelMap, status, template);
                        }
                    });
                }
            }
        });
        List<TestingReportLabelParam> labelParams = new ArrayList<>(validationLabelMap.values());
        log.debug("Result label params: {}", labelParams);
        return labelParams;
    }

    private <T> void setLabelTemplateTreeNodeChildren(LabelTemplateTreeNode labelTreeNode,
                                                      Collection<T> entities,
                                                      Function<T, TreeNode> childrenMapFunc) {
        if (!isEmpty(entities)) {
            List<TreeNode> children = entities.stream()
                    .map(childrenMapFunc)
                    .collect(Collectors.toList());
            labelTreeNode.getChildren().addAll(children);
        }
    }

    /**
     * Get testrun node.
     *
     * @param testRun           root testrun
     * @param includeLogRecords log records inclusion flag
     * @param filteringRequest  filters for log records
     * @return testrun node
     */
    private TreeNode getTestRunNode(TestRun testRun, boolean includeLogRecords,
                                    LogRecordFilteringRequest filteringRequest) {
        return getTestRunNode(testRun, includeLogRecords, filteringRequest, null);
    }

    private TreeNode getTestRunNode(TestRun testRun, boolean includeLogRecords,
                                    LogRecordFilteringRequest filteringRequest,
                                    TestRunsDataContext dataContext) {
        final UUID testRunId = testRun.getUuid();
        boolean isTestCaseAlive = isTestCaseAlive(dataContext, testRun);
        final TestRunTreeNode testRunTreeNode = new TestRunTreeNode(testRun, isTestCaseAlive);
        if (includeLogRecords) {
            List<LogRecord> topLevelLogRecords = testRunService.getTopLevelLogRecords(testRunId, null);
            List<TreeNode> topLevelLogRecordNodes = topLevelLogRecords.stream()
                    .map(logRecord -> getLogRecordNode(logRecord, testRun.getExecutionRequestId(), filteringRequest))
                    .filter(node -> !node.isLeaf() || filterLogRecordNodeByRequest(node, filteringRequest))
                    .collect(Collectors.toList());
            testRunTreeNode.setChildren(topLevelLogRecordNodes);
        }
        return testRunTreeNode;
    }

    private TreeNode getTestRunNodeIncludeAllSteps(TestRun testRun, TestRunsDataContext dataContext) {
        boolean isTestCaseAlive = isTestCaseAlive(dataContext, testRun);
        final TestRunTreeNode testRunTreeNode = new TestRunTreeNode(testRun, isTestCaseAlive);
        List<LogRecord> topLevelLogRecords = testRunService.getTopLevelLogRecords(testRun.getUuid(), null);
        Map<UUID, List<LogRecord>> childrenLogRecords = testRunService.getAllLogRecordsByTestRunId(
                testRun.getUuid())
                .stream()
                .filter(lr -> nonNull(lr.getParentRecordId()))
                .collect(Collectors.groupingBy(LogRecord::getParentRecordId));
        List<TreeNode> topLevelLogRecordNodes = topLevelLogRecords.stream()
                .map(logRecord -> getLogRecordNode(logRecord, childrenLogRecords, testRun.getExecutionRequestId()))
                .collect(Collectors.toList());
        testRunTreeNode.setChildren(topLevelLogRecordNodes);
        return testRunTreeNode;
    }

    private boolean isTestCaseAlive(TestRunsDataContext dataContext, TestRun testRun) {
        return dataContext != null && dataContext.getTestRunTestCasesMap() != null
                && dataContext.getTestRunTestCasesMap().containsKey(testRun.getTestCaseId());
    }

    /**
     * Get logrecord node.
     *
     * @param logRecord          root logrecord
     * @param executionRequestId execution request id
     * @param filteringRequest   Filtering request
     * @return logrecord node
     */
    private LogRecordTreeNode getLogRecordNode(LogRecord logRecord, UUID executionRequestId,
                                               LogRecordFilteringRequest filteringRequest) {
        final UUID logRecordId = logRecord.getUuid();
        final LogRecordTreeNode logRecordTreeNode = new LogRecordTreeNode(logRecord, executionRequestId);
        if (isFiltersSet(filteringRequest)) {
            Stream<LogRecord> logRecordChildren = logRecordService.getLogRecordChildren(logRecordId);
            if (logRecordChildren != null) {
                List<TreeNode> logRecordChildrenNodes = logRecordChildren
                        .map(childLogRecord -> getLogRecordNode(childLogRecord, executionRequestId, filteringRequest))
                        .filter(logRecordNode -> {
                                    if (logRecordNode.isLeaf()) {
                                        boolean flag = filterLogRecordNodeByRequest(logRecordNode, filteringRequest);
                                        log.trace("filtering log record: nodeId {} status {} type {} flag {} ",
                                                logRecordNode.getId(), logRecordNode.getTestingStatus(),
                                                logRecordNode.getActionType(), flag);
                                        return flag;
                                    } else {
                                        return true;
                                    }
                                }
                        )
                        .collect(Collectors.toList());
                logRecordTreeNode.setChildren(logRecordChildrenNodes);
                if (isEmpty(logRecordChildrenNodes)) {
                    logRecordTreeNode.setLeaf(true);
                }
            } else {
                logRecordTreeNode.setLeaf(true);
            }
        } else {
            long countOfChild = logRecordService.getChildrenCount(logRecord);
            logRecordTreeNode.setChildren(Collections.emptyList());
            if (countOfChild == 0) {
                logRecordTreeNode.setLeaf(true);
                log.debug("LogRecord {} isLeaf", logRecordId);
            }
        }
        return logRecordTreeNode;
    }

    /**
     * Get logRecord node without filtering.
     *
     * @param logRecord          root logrecord
     * @param childrenLogRecords map of children log records
     * @param executionRequestId execution request id
     * @return logrecord node
     */
    private LogRecordTreeNode getLogRecordNode(LogRecord logRecord, Map<UUID, List<LogRecord>> childrenLogRecords,
                                               UUID executionRequestId) {
        final LogRecordTreeNode logRecordTreeNode = new LogRecordTreeNode(logRecord, executionRequestId);
        List<LogRecord> logRecordChildren = childrenLogRecords.get(logRecord.getUuid());
        if (!isEmpty(logRecordChildren)) {
            List<TreeNode> logRecordChildrenNodes = logRecordChildren
                    .stream()
                    .map(childLogRecord -> getLogRecordNode(childLogRecord, childrenLogRecords, executionRequestId))
                    .collect(Collectors.toList());
            logRecordTreeNode.setChildren(logRecordChildrenNodes);
        } else {
            logRecordTreeNode.setLeaf(true);
        }
        return logRecordTreeNode;
    }

    /**
     * Check filter request fiels.
     *
     * @param filteringRequest request
     * @return filling fiedls and existing filter request
     */
    private boolean isFiltersSet(LogRecordFilteringRequest filteringRequest) {
        return nonNull(filteringRequest) && (filteringRequest.isShowNotAnalyzedItemsOnly()
                || !isEmpty(filteringRequest.getStatuses())
                || !isEmpty(filteringRequest.getTypes()));
    }

    /**
     * Finds Log Record Tree Nodes by parent Test Run Id and part of Log Record's name.
     *
     * @param testRun     parent test run
     * @param searchValue part of log record name to search
     */
    private List<TreeNode> getLogRecordNodesByName(TestRun testRun, String searchValue) {
        Stream<LogRecord> matchedLogRecords =
                logRecordService.getAllMatchesLogRecordsByTestRunIdCaseInsensitive(testRun.getUuid(), searchValue);
        return matchedLogRecords
                .map(logRecord -> new LogRecordTreeNode(logRecord, testRun.getExecutionRequestId()))
                .collect(Collectors.toList());
    }

    /**
     * Filter LogRecordNode by filteringRequest.
     *
     * @param logRecordNode    LogRecordNode
     * @param filteringRequest Request to perform filtering based on it`s parameters
     * @return true if logRecordNode meets the requirements otherwise false
     */
    private boolean filterLogRecordNodeByRequest(LogRecordTreeNode logRecordNode,
                                                 LogRecordFilteringRequest filteringRequest) {
        if (filteringRequest == null) {
            return true;
        }
        return (filteringRequest.getStatuses() == null || filteringRequest.getStatuses().stream()
                .anyMatch(status ->
                        status.compareToIgnoreCase(logRecordNode.getTestingStatus().name()) == 0))
                && (filteringRequest.getTypes() == null || filteringRequest.getTypes().stream()
                .anyMatch(type ->
                        type.compareToIgnoreCase(logRecordNode.getActionType().name()) == 0))
                && (!filteringRequest.isShowNotAnalyzedItemsOnly() || !logRecordNode.isRootCauseAvailable());
    }

    /**
     * Get execution request test run log records nodes.
     *
     * @param testRunId        testrun id
     * @param filteringRequest filters for log records
     * @return testrun node
     */
    public TreeNode getExecutionRequestTestRunLogRecordsTree(UUID testRunId,
                                                             LogRecordFilteringRequest filteringRequest) {
        TestRun testRun = testRunService.getTestRunForNodeTree(testRunId);
        return getTestRunNode(testRun, true, filteringRequest);
    }

    /**
     * Get execution request test runs tree.
     *
     * @param executionRequestId execution request id
     * @return testrun nodes
     */
    public TreeNode getExecutionRequestTestRunTree(UUID executionRequestId) {
        return getExecutionRequestTree(executionRequestId, true, false);
    }

    /**
     * Get execution request widget test runs tree.
     *
     * @param executionRequestId   execution request id
     * @param widgetId             widget id
     * @param labelTemplateId      label template id
     * @param validationTemplateId validation template id
     * @param skipOverride         skip override label template id
     * @param refresh              is true after refresh Summary Statistic table and change Label template
     * @return testrun nodes
     */
    public TreeNode getExecutionRequestWidgetTree(UUID executionRequestId, UUID widgetId,
                                                  UUID labelTemplateId, UUID validationTemplateId,
                                                  boolean skipOverride, boolean refresh) {
        final ExecutionRequest executionRequest = executionRequestService.findById(executionRequestId);
        return getExecutionRequestWidgetTree(executionRequest, widgetId, labelTemplateId, validationTemplateId,
                skipOverride, refresh);
    }

    /**
     * Get execution request widget test runs tree.
     *
     * @param executionRequest     execution request
     * @param widgetId             widget id
     * @param labelTemplateId      label template id
     * @param validationTemplateId validation template id
     * @param skipOverride         skip override label template id
     * @param refresh              is true after refresh Summary Statistic table and change Label template
     * @return testrun nodes
     */
    private TreeNode getExecutionRequestWidgetTree(ExecutionRequest executionRequest, UUID widgetId,
                                                   UUID labelTemplateId, UUID validationTemplateId,
                                                   boolean skipOverride, boolean refresh) {
        log.debug("Get execution request '{}' widget '{}' tree with label template '{}' and validation template '{}'",
                executionRequest.getUuid(), widgetId, labelTemplateId, validationTemplateId);
        final ExecutionRequestWidgetConfigTemplateResponse confResponse =
                widgetConfigTemplateService.getWidgetConfigTemplateForEr(executionRequest);
        final WidgetConfigTemplate widgetConfigTemplate = confResponse.getTemplate();
        ValidationLabelConfigTemplate validationTemplate = null;
        WidgetConfigTemplate.WidgetConfig widgetConfig = null;
        UUID widgetValidationTemplateId = null;
        if (nonNull(widgetConfigTemplate)) {
            log.debug("Found widget config template: {}", widgetConfigTemplate);
            widgetConfig = widgetConfigTemplate.getWidgetConfig(widgetId);
            widgetValidationTemplateId = widgetConfig.getValidationTemplateId();
        }
        validationTemplateId = isNull(validationTemplateId) ? widgetValidationTemplateId : validationTemplateId;
        if (nonNull(validationTemplateId)) {
            validationTemplate = validationLabelConfigTemplateService.get(validationTemplateId);
            log.debug("Found validation template: {}", validationTemplate);
        }
        if (ExecutionRequestWidgets.SUMMARY_STATISTIC.getWidgetId().equals(widgetId)
                || ExecutionRequestWidgets.SUMMARY_STATISTIC_FOR_USAGES.getWidgetId().equals(widgetId)
                || ExecutionRequestWidgets.SUMMARY_STATISTIC_SCENARIO_TYPE.getWidgetId().equals(widgetId)) {
            final UUID erLabelTemplateId = executionRequest.getLabelTemplateId();
            final boolean defaultLabelTemplateChanged = confResponse.isDefaultLabelTemplateChanged();
            if (!(skipOverride)) {
                if (nonNull(erLabelTemplateId)) {
                    labelTemplateId = erLabelTemplateId;
                }
                if (defaultLabelTemplateChanged && nonNull(widgetConfig)) {
                    labelTemplateId = widgetConfig.getLabelTemplateId();
                }
            }
            return getExecutionRequestTree(executionRequest, labelTemplateId, validationTemplate, true, false);
        }
        if (isNull(labelTemplateId) && nonNull(widgetConfig)) {
            labelTemplateId = widgetConfig.getLabelTemplateId();
        }
        log.debug("Found label validationTemplate with id '{}' from widget config", labelTemplateId);
        return getExecutionRequestTree(executionRequest, labelTemplateId, validationTemplate, true, false);
    }

    /**
     * Get execution request widget test runs tree.
     *
     * @param executionRequestId   execution request id
     * @param widgetId             widget id
     * @param labelTemplateId      label template id
     * @param validationTemplateId validation template id
     * @param skipOverride         skip override label template id
     * @param fields               fields filter
     * @param countLr              for cache
     * @return testrun nodes
     * @throws JsonProcessingException possible json process exception
     */
    @Caching(
            evict = {
                    @CacheEvict(value = CacheConstants.ATP_RAM_REPORTS,
                            key = "{#executionRequestId, #countLr, #widgetId}",
                            condition = "#refresh || #skipOverride", beforeInvocation = true)
            },
            cacheable = {
                    @Cacheable(value = CacheConstants.ATP_RAM_REPORTS,
                            key = "{#executionRequestId, #countLr, #widgetId}")
            }
    )
    public String getSerializableExecutionRequestWidgetTree(UUID executionRequestId, UUID widgetId,
                                                            UUID labelTemplateId, UUID validationTemplateId,
                                                            boolean skipOverride, String[] fields,
                                                            Long countLr, boolean refresh)
            throws JsonProcessingException {
        final ExecutionRequest executionRequest = executionRequestService.findById(executionRequestId);
        TreeNode treeNode = getExecutionRequestWidgetTree(executionRequest, widgetId, labelTemplateId,
                validationTemplateId, skipOverride, refresh);
        executionRequest.setCountLogRecords(countLr);
        executionRequestService.save(executionRequest);
        return Utils.filterAllExceptFields(treeNode, fields, objectMapper, TreeNode.TREE_NODE_JSON_FILTER_NAME);
    }

    private void processValidationLabelTemplate(LogRecord logRecord,
                                                ValidationTableLine step,
                                                Map<String, TestingReportLabelParam> resultMap,
                                                TestingStatuses status,
                                                ValidationLabelConfigTemplate template) {
        TreeSet<ValidationLabelConfigTemplate.LabelConfig> labels = template.getLabels();
        Set<String> stepLabels = step.getValidationLabels();
        Set<String> logRecordValidationLabels = logRecord.getValidationLabels();
        Set<String> allLabels = new HashSet<>(CollectionUtils.union(logRecordValidationLabels, stepLabels));
        if (!isEmpty(labels)) {
            for (ValidationLabelConfigTemplate.LabelConfig labelConfig : labels) {
                final Set<String> templateLabels = labelConfig.getLabelNames();
                String columnName = labelConfig.getColumnName();
                if (StringUtils.isEmpty(columnName)) {
                    columnName = String.join(",", templateLabels);
                }
                boolean displayErAr = labelConfig.isDisplayErAr();
                boolean isContains = displayErAr ? allLabels.containsAll(templateLabels) :
                        stepLabels.containsAll(templateLabels);
                TestingReportLabelParam param = displayErAr ? new TestingReportLabelParam(columnName, status, step) :
                        new TestingReportLabelParam(columnName, status);
                if (isContains) {
                    resultMap.merge(columnName, param, statusMergeFunc);
                }
            }
        }
    }

    private void processValidationLabelTemplate(Map<String, TestingReportLabelParam> resultMap,
                                                Set<String> validationLabels,
                                                TestingStatuses status,
                                                ValidationLabelConfigTemplate template) {
        log.debug("Process validation label template '{}' with input labels [{}] and status '{}'",
                template.getUuid(), validationLabels, status);
        TreeSet<ValidationLabelConfigTemplate.LabelConfig> labels = template.getLabels();
        if (!isEmpty(labels)) {
            for (ValidationLabelConfigTemplate.LabelConfig labelConfig : labels) {
                final Set<String> templateLabels = labelConfig.getLabelNames();
                boolean isContains = validationLabels.containsAll(templateLabels);
                if (isContains) {
                    String columnName = labelConfig.getColumnName();
                    if (StringUtils.isEmpty(columnName)) {
                        columnName = String.join(",", templateLabels);
                    }
                    log.debug("Put validation template label: {} = {}", columnName, status);
                    resultMap.merge(columnName, new TestingReportLabelParam(columnName, status), statusMergeFunc);
                }
            }
        }
    }

    /**
     * Get log records nodes for parent LR.
     *
     * @param parentLogRecordId log record id
     * @param filteringRequest  filters for log records
     * @return log record node
     */
    public TreeNode getLogRecordsTreeForLogRecordParent(UUID parentLogRecordId,
                                                        LogRecordFilteringRequest filteringRequest) {
        final LogRecord logRecordParent = logRecordService.findLogRecordForTreeByUuid(parentLogRecordId);
        final UUID executionRequestId =
                testRunService.findTestRunExecReqIdByUuid(logRecordParent.getTestRunId()).getExecutionRequestId();
        final LogRecordTreeNode logRecordTreeNode = new LogRecordTreeNode(logRecordParent, executionRequestId);
        List<LogRecord> childLogRecords =
                logRecordService.findByTestRunIdAndParentUuid(logRecordParent.getTestRunId(), parentLogRecordId,
                        filteringRequest);
        List<TreeNode> topLevelLogRecordNodes = childLogRecords.stream()
                .map(logRecord -> getLogRecordNode(logRecord, executionRequestId, filteringRequest))
                .filter(node -> !node.isLeaf() || filterLogRecordNodeByRequest(node, filteringRequest))
                .collect(Collectors.toList());
        logRecordTreeNode.setChildren(topLevelLogRecordNodes);
        return logRecordTreeNode;
    }
}
