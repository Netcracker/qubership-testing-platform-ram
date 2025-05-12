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

package org.qubership.atp.ram.service.template.impl;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.qubership.atp.ram.RamConstants;
import org.qubership.atp.ram.config.ApiPath;
import org.qubership.atp.ram.dto.response.LabelNodeReportResponse;
import org.qubership.atp.ram.entities.treenodes.labelparams.TestingReportLabelParam;
import org.qubership.atp.ram.enums.ExecutionRequestWidgets;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.model.DataSet;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.FinalRunData;
import org.qubership.atp.ram.models.Label;
import org.qubership.atp.ram.models.TestCaseWidgetReportRequest;
import org.qubership.atp.ram.models.TestPlan;
import org.qubership.atp.ram.models.WidgetConfigTemplate;
import org.qubership.atp.ram.models.WidgetType;
import org.qubership.atp.ram.service.mail.ReportParams;
import org.qubership.atp.ram.service.template.TestingStatusColor;
import org.qubership.atp.ram.service.template.impl.generictable.Body;
import org.qubership.atp.ram.service.template.impl.generictable.Column;
import org.qubership.atp.ram.service.template.impl.generictable.Header;
import org.qubership.atp.ram.service.template.impl.generictable.Link;
import org.qubership.atp.ram.service.template.impl.generictable.Row;
import org.qubership.atp.ram.service.template.impl.generictable.Table;
import org.qubership.atp.ram.service.template.impl.generictable.columntypes.ColoredColumn;
import org.qubership.atp.ram.service.template.impl.generictable.columntypes.DashColumn;
import org.qubership.atp.ram.service.template.impl.generictable.columntypes.EmptyColumn;
import org.qubership.atp.ram.service.template.impl.generictable.columntypes.LabelColumn;
import org.qubership.atp.ram.service.template.impl.generictable.columntypes.LinkColumn;
import org.qubership.atp.ram.service.template.impl.generictable.columntypes.PercentColumn;
import org.qubership.atp.ram.service.template.impl.generictable.columntypes.StatusColumn;
import org.qubership.atp.ram.services.CatalogueService;
import org.qubership.atp.ram.services.DataSetService;
import org.qubership.atp.ram.services.ExecutionRequestService;
import org.qubership.atp.ram.services.ReportService;
import org.qubership.atp.ram.services.TreeNodeService;
import org.qubership.atp.ram.services.WidgetConfigTemplateService;
import org.qubership.atp.ram.utils.StreamUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class TestCasesWidgetModelBuilder extends AbstractWidgetModelBuilder {

    private static final String EMPTY = "";
    private static final int MAX_ER_AR_LENGTH = 100;
    private static final String DOTS = "...";
    private static final String LINK = "link";
    public static final List<String> DEFAULT_COLUMN_NAMES = RamConstants.DEFAULT_COLUMN_NAMES;

    @Value("${catalogue.url}")
    private String baseUrl;
    private final CatalogueService catalogueService;
    private final ExecutionRequestService executionRequestService;
    private final DataSetService dataSetService;
    private final ReportService reportService;
    private final WidgetConfigTemplateService widgetConfigTemplateService;

    @Override
    protected Map<String, Object> buildModel(ReportParams reportParams) {
        UUID executionRequestId = reportParams.getExecutionRequestUuid();
        boolean isExecutionRequestsSummary = reportParams.isExecutionRequestsSummary();
        LabelNodeReportResponse testCases = reportService.getTestCasesForExecutionRequest(executionRequestId, null,
                null, isExecutionRequestsSummary, new TestCaseWidgetReportRequest());

        UUID widgetId = ExecutionRequestWidgets.TEST_CASES.getWidgetId();
        Map<String, Boolean> columnVisibilityMap =
                widgetConfigTemplateService.getWidgetColumnVisibilityMap(executionRequestId, widgetId);

        final Table table = new Table();

        final Header header = table.getHeader();
        // set default columns
        setHeaderColumnHeaders(header, DEFAULT_COLUMN_NAMES, columnVisibilityMap);

        final List<String> validationLabels = testCases.getValidationLabelsOrder();
        if (!isEmpty(validationLabels)) {
            List<String> upperCasedValidationLabelNames = validationLabels
                    .stream()
                    .map(String::toUpperCase)
                    .collect(Collectors.toList());
            header.setTextColumns(upperCasedValidationLabelNames);
        }

        Body body = table.getBody();

        final ExecutionRequest executionRequest = executionRequestService.get(executionRequestId);

        LabelNodeReportAdapter model = updateModel(executionRequest, testCases);

        List<Row> labelNodeRows = mapLabelNodes(model.getChildren(), validationLabels, columnVisibilityMap);
        body.setRows(labelNodeRows);

        List<Row> testRunRows = mapTestRuns(model.getTestRuns(), validationLabels, columnVisibilityMap);
        List<Row> rows = body.getRows();
        rows.addAll(testRunRows);

        markEvenAndOddRows(rows);

        final HashMap<String, Object> variables = new HashMap<>();
        variables.put("tableModel", table);
        variables.put("title", "Test Cases");
        variables.put("widgetUrl", generateTestCasesWidgetUrl(executionRequest));

        final Map<String, String> descriptions = reportParams.getDescriptions();
        if (nonNull(descriptions) && descriptions.containsKey(WidgetType.TEST_CASES.name())) {
            variables.put("description", descriptions.get(WidgetType.TEST_CASES.name()));
        }

        WidgetConfigTemplate.Filters filters =
                reportService.resolveWidgetConfigFilters(new TestCaseWidgetReportRequest(), executionRequest);
        variables.put("filtered", nonNull(filters));

        return variables;
    }

    private void setHeaderColumnHeaders(Header header,
                                        List<String> columnNames,
                                        Map<String, Boolean> columnVisibilityMap) {
        columnNames.forEach(columnName -> {
            if (checkVisibility(columnName, columnVisibilityMap)) {
                header.addColumn(new Column(columnName));
            }
        });
    }

    private boolean checkVisibility(String columnName, Map<String, Boolean> columnVisibilityMap) {
        return columnVisibilityMap.getOrDefault(columnName, true);
    }

    private List<Row> mapLabelNodes(List<LabelNodeReportAdapter> children,
                                    List<String> validationLabels,
                                    Map<String, Boolean> columnVisibilityMap) {
        return children.stream()
                .map(labelNode -> mapLabelNode(labelNode, validationLabels, columnVisibilityMap))
                .collect(Collectors.toList());
    }

    private Row mapLabelNode(LabelNodeReportAdapter labelNode,
                             List<String> validationLabels,
                             Map<String, Boolean> columnVisibilityMap) {
        Row row = new Row();

        addLabelNodeDefaultColumns(row, labelNode, columnVisibilityMap);
        addValidationLabelColumns(row, labelNode.getLabelParams(), validationLabels);

        List<LabelNodeReportAdapter> childrenLabelNodes = labelNode.getChildren();
        if (!isEmpty(childrenLabelNodes)) {
            List<Row> childrenRows = mapLabelNodes(childrenLabelNodes, validationLabels, columnVisibilityMap);

            row.getChildren().addAll(childrenRows);
        }

        List<TestRunNodeAdapter> testRuns = labelNode.getTestRuns();
        if (!isEmpty(testRuns)) {
            List<Row> testRunRows = mapTestRuns(testRuns, validationLabels, columnVisibilityMap);

            row.getChildren().addAll(testRunRows);
        }

        return row;

    }

    private List<Row> mapTestRuns(List<TestRunNodeAdapter> testRuns,
                                  List<String> validationLabels,
                                  Map<String, Boolean> columnVisibilityMap) {
        return testRuns.stream()
                .map(testRun -> mapTestRunNode(testRun, validationLabels, columnVisibilityMap))
                .collect(Collectors.toList());
    }

    private Row mapTestRunNode(TestRunNodeAdapter testRunNode, List<String> validationLabels,
                               Map<String, Boolean> columnVisibilityMap) {
        Row row = new Row();

        addTestRunDefaultColumns(row, testRunNode, columnVisibilityMap);
        addValidationLabelColumns(row, testRunNode.getLabelParams(), validationLabels);

        return row;
    }

    private void addTestRunDefaultColumns(Row row,
                                          TestRunNodeAdapter testRunNode,
                                          Map<String, Boolean> columnVisibilityMap) {
        final Integer passedRate = testRunNode.getPassedRate();
        final Set<String> issues = testRunNode.getIssues();
        final String failureReason = testRunNode.getFailureReason();
        final List<LogRecordAdapter> failedStep = testRunNode.getFailedStep();
        final List<Label> labels = testRunNode.getLabels();
        final String comment = testRunNode.getComment();
        final String jiraTicket = testRunNode.getJiraTicket();

        boolean isPassedRateUnknown = isNull(passedRate);
        boolean isIssuesUnknown = isEmpty(issues);
        boolean isFailureReasonUnknown = StringUtils.isEmpty(failureReason);
        boolean isFailedStepsPresent = !isEmpty(failedStep);
        boolean isLabelsPresent = !isEmpty(labels);
        boolean isJiraTicketPresent = !isNull(jiraTicket);
        boolean isCommentPresent = !StringUtils.isEmpty(comment);
        boolean isTestingStatusUnknown = TestingStatusColor.UNKNOWN.equals(testRunNode.getTestingStatus());
        boolean isFirstStatusUnknown = TestingStatusColor.UNKNOWN.equals(testRunNode.getFirstStatus());
        boolean isFinalStatusUnknown = TestingStatusColor.UNKNOWN.equals(testRunNode.getFinalStatus());

        List<Link> failedStepLinks = null;
        if (isFailedStepsPresent) {
            failedStepLinks = failedStep
                    .stream()
                    .map(step -> new Link(step.getName(), step.getLink()))
                    .collect(Collectors.toList());
        }

        List<Link> issueLinks = null;
        if (!isIssuesUnknown) {
            issueLinks = issues
                    .stream()
                    .map(this::parseIssueToLink)
                    .collect(Collectors.toList());
        }

        final Column dash = new DashColumn();
        final Column nameColumn = new LinkColumn(testRunNode.getName(), testRunNode.getUrl());
        final Column statusColumn = isTestingStatusUnknown ? dash : new StatusColumn(testRunNode.getTestingStatus());
        final Column firstStatus = isFirstStatusUnknown ? dash : new StatusColumn(testRunNode.getFirstStatus());
        final Column finalStatus = isFinalStatusUnknown ? dash : new StatusColumn(testRunNode.getFinalStatus());
        final Column percentColumn = isPassedRateUnknown ? dash : new PercentColumn(passedRate);
        final Column issueColumn = isIssuesUnknown ? dash : new LinkColumn(issueLinks);
        final Column jiraTicketColumn = isJiraTicketPresent ? new LinkColumn(parseIssueToLink(jiraTicket)) : dash;
        final Column durationColumn = new Column(testRunNode.getDuration());
        final Column failureReasonColumn = isFailureReasonUnknown ? dash : new Column(failureReason);
        final Column failedStepColumn = isFailedStepsPresent ? new LinkColumn(failedStepLinks) : dash;
        final Column finalRun = new LinkColumn(LINK, testRunNode.getFinalRunUrl());
        final Column labelsColumn = isLabelsPresent ? new LabelColumn(labels) : dash;
        final Column datasetColumn = new LinkColumn(testRunNode.getDataSetName(), testRunNode.getDatasetUrl());
        final Column commentColumn = isCommentPresent ? new Column(comment) : dash;

        row.addColumn(nameColumn, checkVisibility(RamConstants.NAME, columnVisibilityMap));
        row.addColumn(statusColumn, checkVisibility(RamConstants.STATUS, columnVisibilityMap));
        row.addColumn(firstStatus, checkVisibility(RamConstants.FIRST_STATUS, columnVisibilityMap));
        row.addColumn(finalStatus, checkVisibility(RamConstants.FINAL_STATUS, columnVisibilityMap));
        row.addColumn(percentColumn, checkVisibility(RamConstants.PASSED_RATE, columnVisibilityMap));
        row.addColumn(issueColumn, checkVisibility(RamConstants.ISSUE, columnVisibilityMap));
        row.addColumn(durationColumn, checkVisibility(RamConstants.DURATION, columnVisibilityMap));
        row.addColumn(failureReasonColumn, checkVisibility(RamConstants.FAILURE_REASON, columnVisibilityMap));
        row.addColumn(failedStepColumn, checkVisibility(RamConstants.FAILED_STEP, columnVisibilityMap));
        row.addColumn(finalRun, checkVisibility(RamConstants.FINAL_RUN, columnVisibilityMap));
        row.addColumn(labelsColumn, checkVisibility(RamConstants.LABELS, columnVisibilityMap));
        row.addColumn(datasetColumn, checkVisibility(RamConstants.DATA_SET, columnVisibilityMap));
        row.addColumn(jiraTicketColumn, checkVisibility(RamConstants.JIRA_TICKET, columnVisibilityMap));
        row.addColumn(commentColumn, checkVisibility(RamConstants.COMMENT, columnVisibilityMap));
    }

    private void addLabelNodeDefaultColumns(Row row,
                                            LabelNodeReportAdapter labelNode,
                                            Map<String, Boolean> columnVisibilityMap) {
        boolean isNodeEmpty = labelNode.getTestRuns().isEmpty() && labelNode.getChildren().isEmpty();
        TestingStatusColor status = labelNode.getStatus();
        final int passedRate = labelNode.getPassedRate();

        final Column nameColumn = new Column(labelNode.getLabelName(), Column.BOLD);
        row.addColumn(nameColumn, checkVisibility(RamConstants.NAME, columnVisibilityMap));

        final long allColumnsSize = columnVisibilityMap.entrySet()
                .stream()
                .filter(Map.Entry::getValue)
                .count() - 1; // minus name column

        if (!labelNode.isGroupNode()) {
            final Column statusColumn = isNodeEmpty ? new DashColumn() : new StatusColumn(status);
            final Column percentColumn = isNodeEmpty ? new PercentColumn(passedRate, Column.EMPTY, Column.GRAY)
                    : new PercentColumn(passedRate);
            final Column issueColumn = new DashColumn();
            final Column durationColumn = new Column(labelNode.getDuration());
            final Column failureReasonColumn = new DashColumn();
            final Column failedStepColumn = new DashColumn();
            final Column labelsStepColumn = new DashColumn();
            final Column datasetColumn = new DashColumn();
            final Column jiraTicketColumn = new DashColumn();
            final Column commentColumn = new DashColumn();

            row.addColumn(statusColumn, checkVisibility(RamConstants.STATUS, columnVisibilityMap));
            row.addColumn(percentColumn, checkVisibility(RamConstants.PASSED_RATE, columnVisibilityMap));
            row.addColumn(issueColumn, checkVisibility(RamConstants.ISSUE, columnVisibilityMap));
            row.addColumn(durationColumn, checkVisibility(RamConstants.DURATION, columnVisibilityMap));
            row.addColumn(failureReasonColumn, checkVisibility(RamConstants.FAILURE_REASON, columnVisibilityMap));
            row.addColumn(failedStepColumn, checkVisibility(RamConstants.FAILED_STEP, columnVisibilityMap));
            row.addColumn(labelsStepColumn, checkVisibility(RamConstants.LABELS, columnVisibilityMap));
            row.addColumn(datasetColumn, checkVisibility(RamConstants.DATA_SET, columnVisibilityMap));
            row.addColumn(jiraTicketColumn, checkVisibility(RamConstants.JIRA_TICKET, columnVisibilityMap));
            row.addColumn(commentColumn, checkVisibility(RamConstants.COMMENT, columnVisibilityMap));
        } else {
            Stream.generate(EmptyColumn::new)
                    .limit(allColumnsSize)
                    .forEach(column -> row.addColumn(column, true));
        }
    }

    private void addValidationLabelColumns(Row row,
                                           List<TestingReportLabelParam> params,
                                           List<String> validationLabels) {
        if (!isEmpty(validationLabels)) {
            final Map<String, TestingReportLabelParam> reportLabelParams =
                    StreamUtils.toEntityMap(params, TestingReportLabelParam::getName);
            final Set<String> ignoredLabels = new HashSet<>();

            validationLabels.forEach(label -> {
                if (reportLabelParams.containsKey(label)) {
                    final TestingReportLabelParam param = reportLabelParams.get(label);
                    final TestingStatuses status = param.getStatus();
                    row.addColumns(new StatusColumn(status));

                    addErArColumns(row, param, ignoredLabels);
                } else if (!ignoredLabels.contains(label)) {
                    row.addColumns(new Column(Column.DASH));
                }
            });
        }
    }

    private void addErArColumns(Row row, TestingReportLabelParam param, Set<String> ignoredLabels) {
        final TestingStatuses status = param.getStatus();
        final String expectedResult = param.getExpectedResult();
        final String actualResult = param.getActualResult();
        final boolean isExpectedResultPresent = nonNull(expectedResult);
        final boolean isActualResultPresent = nonNull(actualResult);

        if (isExpectedResultPresent || isActualResultPresent) {
            addErArColumn(row, expectedResult, status);
            ignoredLabels.add(param.getName() + TreeNodeService.ER_SUFFIX);
            addErArColumn(row, actualResult, status);
            ignoredLabels.add(param.getName() + TreeNodeService.AR_SUFFIX);
        }
    }

    private void addErArColumn(Row row, String value, TestingStatuses status) {
        final boolean isValuePresent = nonNull(value);
        final boolean isFailed = TestingStatuses.FAILED.equals(status);
        final Column column;

        if (value.length() > MAX_ER_AR_LENGTH) {
            value = value.substring(0, MAX_ER_AR_LENGTH).concat(DOTS);
        }

        if (isValuePresent && isFailed) {
            column = new ColoredColumn(value, Column.TRANSPARENT_DARK_RED, Column.TRANSPARENT_LIGHT_RED);
        } else if (isValuePresent) {
            column = new Column(value);
        } else {
            column = new DashColumn();
        }
        row.addColumns(column);
    }

    private LabelNodeReportAdapter updateModel(ExecutionRequest executionRequest,
                                               LabelNodeReportResponse labelNode) {
        return LabelNodeReportAdapter.builder()
                .labelNodeReportResponse(labelNode)
                .status(TestingStatusColor.of(labelNode.getStatus()))
                .children(getChildren(executionRequest, labelNode.getChildren()))
                .testRuns(getTestRuns(executionRequest, labelNode.getTestRuns()))
                .validationLabelsOrder(labelNode.getValidationLabelsOrder())
                .labelParams(labelNode.getLabelParams())
                .isGroupNode(labelNode.isGroupedNode())
                .build();
    }

    private List<TestRunNodeAdapter> getTestRuns(ExecutionRequest executionRequest,
                                                 List<LabelNodeReportResponse.TestRunNodeResponse> testRuns) {
        final UUID testPlanId = executionRequest.getTestPlanId();
        final TestPlan testPlan = catalogueService.getTestPlan(testPlanId);
        final TestPlan.BugTrackingSystemSynchronization bugTrackingSystem = testPlan.getSynchronization();
        String jiraTicketUrl = nonNull(bugTrackingSystem) ? bugTrackingSystem.getSystemUrl() + "/browse/" : EMPTY;
        Map<UUID, String> mapDatasetIdName = getMapWithDataSetIdName(testRuns);

        return testRuns
                .stream()
                .map(testRun -> TestRunNodeAdapter
                        .builder()
                        .testingStatus(TestingStatusColor.of(testRun.getTestingStatus()))
                        .firstStatus(TestingStatusColor.of(testRun.getFirstStatus()))
                        .finalStatus(TestingStatusColor.of(testRun.getFinalStatus()))
                        .testRunNodeResponse(testRun)
                        .failedStep(getFailedSteps(executionRequest, testRun))
                        .dataSetUrl(generateDataSetUrlLink(executionRequest, testRun))
                        .dataSetName(getDatasetNameFromMap(mapDatasetIdName, testRun.getDataSetUrl()))
                        .url(generateTestRunUrl(executionRequest, testRun))
                        .passedRate(null)
                        .jiraTicket(nonNull(testRun.getJiraTicket()) ? jiraTicketUrl + testRun.getJiraTicket() : null)
                        .issues(testRun.getIssues())
                        .comment(nonNull(testRun.getComment()) ? testRun.getComment().getHtml() : EMPTY)
                        .labelParams(testRun.getLabelParams())
                        .finalRunUrl(generateFinalRunUrl(executionRequest, testRun.getFinalRun()))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Get DS name from map.
     *
     * @param mapDatasetIdName map witd datasets id and name
     * @param dataSetId ID for get DS name from map
     * @return name of DS or null
     */
    private String getDatasetNameFromMap(Map<UUID, String> mapDatasetIdName, String dataSetId) {
        if (!StringUtils.isEmpty(dataSetId) && !"null".equals(dataSetId)) {
            UUID key = UUID.fromString(dataSetId);
            if (mapDatasetIdName.containsKey(key)) {
                return mapDatasetIdName.get(key);
            }
        }
        return null;
    }

    private String generateTestCasesWidgetUrl(ExecutionRequest executionRequest) {
        return baseUrl + ApiPath.PROJECT_PATH + "/" + executionRequest.getProjectId()
                + ApiPath.REPORT_EXECUTION_REQUESTS_PATH + "/" + executionRequest.getUuid() + "#test-cases";
    }

    private String generateTestRunUrl(ExecutionRequest executionRequest,
                                      LabelNodeReportResponse.TestRunNodeResponse testRun) {
        return baseUrl + ApiPath.PROJECT_PATH + "/" + executionRequest.getProjectId()
                + ApiPath.REPORT_EXECUTION_REQUESTS_PATH + "/" + executionRequest.getUuid()
                + "?node=" + testRun.getUuid();
    }

    private String generateFinalRunUrl(ExecutionRequest executionRequest,
                                       FinalRunData finalRun) {
        UUID executionRequestId = finalRun.getExecutionRequestId();
        UUID testRunId = finalRun.getTestRunId();
        return baseUrl + ApiPath.PROJECT_PATH + "/" + executionRequest.getProjectId()
                + ApiPath.REPORT_EXECUTION_REQUESTS_PATH + "/" + executionRequestId
                + "?node=" + testRunId;
    }

    /**
     * Get map with ID and name of DS-s.
     *
     * @param testRuns for collect DS id from TR
     * @return map with ID and name of DS-s
     */
    private Map<UUID, String> getMapWithDataSetIdName(List<LabelNodeReportResponse.TestRunNodeResponse> testRuns) {
        List<UUID> dataSetsIds = testRuns.stream()
                .map(testRunNodeResponse -> {
                    String dataSetId = testRunNodeResponse.getDataSetUrl();
                    if (!StringUtils.isEmpty(dataSetId) && !"null".equals(dataSetId)) {
                        return UUID.fromString(dataSetId);
                    }
                    return null;
                })
                .collect(Collectors.toList());
        if (!isEmpty(dataSetsIds)) {
            try {
                List<DataSet> dataSets = dataSetService.getDataSetsByIds(dataSetsIds);
                return dataSets.stream().collect(Collectors.toMap(DataSet::getId, DataSet::getName));
            } catch (Exception e) {
                log.error("Can not get Data sets with id-s {}",  dataSetsIds, e);
                return new HashMap<>();
            }
        }
        return new HashMap<>();
    }

    private String generateDataSetUrlLink(ExecutionRequest executionRequest,
                                          LabelNodeReportResponse.TestRunNodeResponse testRun) {
        String dataSetId = testRun.getDataSetUrl();
        if (dataSetId != null) {
            return baseUrl + ApiPath.PROJECT_PATH + "/" + executionRequest.getProjectId()
                    + "/data-sets/dsl/" + testRun.getDataSetListUrl()
                    + "/data-set/" + dataSetId;
        }
        return null;
    }

    private Link parseIssueToLink(String issueUrl) {
        String issueText = issueUrl.substring(issueUrl.lastIndexOf("/") + 1);

        return new Link(issueText, issueUrl);
    }

    private List<LogRecordAdapter> getFailedSteps(ExecutionRequest executionRequest,
                                                  LabelNodeReportResponse.TestRunNodeResponse testRun) {
        final List<LabelNodeReportResponse.FailedLogRecordNodeResponse> testRunFailedSteps = testRun.getFailedStep();

        return isEmpty(testRunFailedSteps) ? Collections.emptyList() : testRunFailedSteps
                .stream()
                .limit(1)
                .map(logRecord -> LogRecordAdapter
                        .builder()
                        .logRecord(logRecord)
                        .link(generateLogRecordUrlLink(executionRequest, logRecord, testRun.getUuid()))
                        .build())
                .collect(Collectors.toList());
    }

    private String generateLogRecordUrlLink(ExecutionRequest executionRequest,
                                            LabelNodeReportResponse.FailedLogRecordNodeResponse logRecord,
                                            UUID testRunId) {
        return baseUrl + ApiPath.PROJECT_PATH + "/" + executionRequest.getProjectId()
                + ApiPath.REPORT_EXECUTION_REQUESTS_PATH + "/" + executionRequest.getUuid()
                + "?node=" + testRunId
                + "&logRecordId=" + logRecord.getUuid();
    }

    private List<LabelNodeReportAdapter> getChildren(ExecutionRequest executionRequest,
                                                     List<LabelNodeReportResponse> children) {
        return children
                .stream()
                .map(node -> updateModel(executionRequest, node))
                .collect(Collectors.toList());
    }

    @Override
    public WidgetType getType() {
        return WidgetType.TEST_CASES;
    }
}
