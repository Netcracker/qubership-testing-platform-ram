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

package org.qubership.atp.ram.model;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.qubership.atp.ram.RamConstants;
import org.qubership.atp.ram.dto.response.LabelNodeReportResponse;
import org.qubership.atp.ram.dto.response.LabelNodeReportResponse.FailedLogRecordNodeResponse;
import org.qubership.atp.ram.dto.response.LabelNodeReportResponse.TestRunNodeResponse;
import org.qubership.atp.ram.entities.treenodes.labelparams.TestingReportLabelParam;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.models.Comment;
import org.qubership.atp.ram.models.Label;
import org.qubership.atp.ram.services.TreeNodeService;
import org.qubership.atp.ram.utils.StreamUtils;
import org.qubership.atp.ram.utils.Utils;

public class TestCaseWidgetCsvExportLine {

    public static final String DASH = "-";

    private List<String> cells = new ArrayList<>();

    /**
     * TestCaseWidgetCsvExportLine constructor.
     */
    public TestCaseWidgetCsvExportLine(LabelNodeReportResponse node, List<String> validationLabels,
                                       Map<String, Boolean> columnVisibilityMap, int depth) {
        add(RamConstants.NAME, name(node, LabelNodeReportResponse::getLabelName, depth), columnVisibilityMap);
        add(RamConstants.STATUS, getStatus(node::getStatus), columnVisibilityMap);
        add(RamConstants.PASSED_RATE, DASH, columnVisibilityMap);
        add(RamConstants.ISSUE, DASH, columnVisibilityMap);
        add(RamConstants.DURATION, getDuration(node::getDuration), columnVisibilityMap);
        add(RamConstants.FAILURE_REASON, DASH, columnVisibilityMap);
        add(RamConstants.FAILED_STEP, DASH, columnVisibilityMap);
        add(RamConstants.LABELS, DASH, columnVisibilityMap);
        add(RamConstants.DATA_SET, DASH, columnVisibilityMap);
        add(RamConstants.JIRA_TICKET, DASH, columnVisibilityMap);
        add(RamConstants.COMMENT, DASH, columnVisibilityMap);

        this.setValidationLabels(node.getLabelParams(), validationLabels);
    }

    /**
     * TestCaseWidgetCsvExportLine constructor.
     */
    public TestCaseWidgetCsvExportLine(TestRunNodeResponse node, List<String> validationLabels,
                                       Map<String, Boolean> columnVisibilityMap, int depth) {
        add(RamConstants.NAME, name(node, TestRunNodeResponse::getName, depth), columnVisibilityMap);
        add(RamConstants.STATUS, getStatus(node::getTestingStatus), columnVisibilityMap);
        add(RamConstants.PASSED_RATE, percent(node.getPassedRate()), columnVisibilityMap);
        add(RamConstants.ISSUE, join(node.getIssues()), columnVisibilityMap);
        add(RamConstants.DURATION, getDuration(node::getDuration), columnVisibilityMap);
        add(RamConstants.FAILURE_REASON, node.getFailureReason(), columnVisibilityMap);
        add(RamConstants.FAILED_STEP, join(node.getFailedStep(), FailedLogRecordNodeResponse::getName),
                columnVisibilityMap);
        add(RamConstants.LABELS, join(node.getLabels(), Label::getName), columnVisibilityMap);
        add(RamConstants.DATA_SET, node.getDataSetName(), columnVisibilityMap);
        add(RamConstants.JIRA_TICKET, node.getJiraTicket(), columnVisibilityMap);
        add(RamConstants.COMMENT, get(node.getComment(), Comment::getText), columnVisibilityMap);

        this.setValidationLabels(node.getLabelParams(), validationLabels);
    }

    private void add(String columnName, String cellValue, Map<String, Boolean> columnVisibilityMap) {
        if (isColumnVisible(columnName, columnVisibilityMap)) {
            cells.add(cellValue);
        }
    }

    private boolean isColumnVisible(String columnName, Map<String, Boolean> columnVisibilityMap) {
        return columnVisibilityMap.getOrDefault(columnName, true);
    }

    private <T> String name(T node, Function<T, String> func, int depth) {
        return IntStream.range(0, depth)
                .mapToObj(num -> "    ")
                .collect(Collectors.joining("", "", func.apply(node)));
    }

    private <T> String get(T entity, Function<T, String> func) {
        if (nonNull(entity)) {
            return func.apply(entity);
        }

        return DASH;
    }

    private String percent(int value) {
        return String.valueOf(value).concat("%");
    }

    private String join(Collection<String> entities) {
        if (CollectionUtils.isNotEmpty(entities)) {
            return String.join(", ", entities);
        }

        return DASH;
    }

    private <T> String join(Collection<T> entities, Function<T, String> func) {
        if (CollectionUtils.isNotEmpty(entities)) {
            return entities.stream()
                    .map(func)
                    .collect(Collectors.joining(", "));
        }

        return DASH;
    }

    private String getStatus(Supplier<TestingStatuses> statusSupplier) {
        final TestingStatuses status = statusSupplier.get();

        return isNull(status) ? DASH : status.getName();
    }

    private String getDuration(Supplier<Long> durationSupplier) {
        final long duration = durationSupplier.get();

        if (duration != 0) {
            return DurationFormatUtils.formatDuration(duration * 1000, "HH:mm:ss", true);
        } else {
            return DASH;
        }
    }

    public String[] getContent() {
        return cells.toArray(new String[0]);
    }

    /**
     * Set validation label cells.
     */
    public void setValidationLabels(List<TestingReportLabelParam> labelParams, List<String> validationLabels) {
        Map<String, TestingReportLabelParam> paramsMap =
                StreamUtils.toEntityMap(labelParams, TestingReportLabelParam::getName);

        Set<String> ignoredLabels = new HashSet<>();
        validationLabels.forEach(validationLabel -> {
            if (paramsMap.containsKey(validationLabel)) {
                TestingReportLabelParam labelParam = paramsMap.get(validationLabel);
                cells.add(labelParam.getStatus().getName());

                addErArColumns(labelParam, ignoredLabels);
            } else if (!ignoredLabels.contains(validationLabel)) {
                cells.add(DASH);
            }
        });
    }

    private void addErArColumns(TestingReportLabelParam param, Set<String> ignoredLabels) {
        final String expectedResult = Utils.cleanXmlTags(param.getExpectedResult());
        final String actualResult = Utils.cleanXmlTags(param.getActualResult());
        final boolean isExpectedResultPresent = nonNull(expectedResult);
        final boolean isActualResultPresent = nonNull(actualResult);

        if (isExpectedResultPresent || isActualResultPresent) {
            cells.add(nonNull(expectedResult) ? expectedResult : DASH);
            ignoredLabels.add(param.getName() + TreeNodeService.ER_SUFFIX);

            cells.add(nonNull(actualResult) ? actualResult : DASH);
            ignoredLabels.add(param.getName() + TreeNodeService.AR_SUFFIX);
        }
    }
}
