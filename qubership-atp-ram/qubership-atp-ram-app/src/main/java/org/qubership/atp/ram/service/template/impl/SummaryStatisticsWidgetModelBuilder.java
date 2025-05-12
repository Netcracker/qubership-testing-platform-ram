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

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.qubership.atp.ram.entities.treenodes.ExecutionRequestTreeNode;
import org.qubership.atp.ram.entities.treenodes.LabelTemplateTreeNode;
import org.qubership.atp.ram.entities.treenodes.TreeNode;
import org.qubership.atp.ram.entities.treenodes.labelparams.CountReportLabelParam;
import org.qubership.atp.ram.enums.ExecutionRequestWidgets;
import org.qubership.atp.ram.models.WidgetType;
import org.qubership.atp.ram.service.mail.ReportParams;
import org.qubership.atp.ram.service.template.impl.generictable.Body;
import org.qubership.atp.ram.service.template.impl.generictable.Column;
import org.qubership.atp.ram.service.template.impl.generictable.Header;
import org.qubership.atp.ram.service.template.impl.generictable.Row;
import org.qubership.atp.ram.service.template.impl.generictable.Table;
import org.qubership.atp.ram.service.template.impl.generictable.columntypes.PercentColumn;
import org.qubership.atp.ram.services.TreeNodeService;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;


@Component
@RequiredArgsConstructor
public class SummaryStatisticsWidgetModelBuilder extends AbstractWidgetModelBuilder {

    private static final String ID = "ID";
    private static final String TC_COUNT = "TC COUNT";
    private static final String GENERAL_PASS_RATE_STATUS = "GENERAL PASS RATE STATUS";

    private final TreeNodeService treeNodeService;

    @Override
    protected Map<String, Object> buildModel(ReportParams reportParams) {
        TreeNode rootTreeNode = treeNodeService.getExecutionRequestWidgetTree(reportParams.getExecutionRequestUuid(),
                ExecutionRequestWidgets.SUMMARY_STATISTIC.getWidgetId(), null, null, false, true);

        final Table table = new Table();

        final Header header = table.getHeader();
        // set default columns
        header.setTextColumns(asList(ID, TC_COUNT, GENERAL_PASS_RATE_STATUS));

        final ExecutionRequestTreeNode erNode = (ExecutionRequestTreeNode) rootTreeNode;

        // set validation labels columns
        final List<String> validationLabels = erNode.getValidationLabelsOrder();
        if (!isEmpty(validationLabels)) {
            List<String> upperCasedValidationLabelNames = validationLabels
                    .stream()
                    .map(String::toUpperCase)
                    .collect(Collectors.toList());
            header.setTextColumns(upperCasedValidationLabelNames);
        }

        Body body = table.getBody();
        List<Row> rows = mapTreeNodes(rootTreeNode.getChildren(), validationLabels);
        body.setRows(rows);

        markEvenAndOddRows(rows);

        final HashMap<String, Object> variables = new HashMap<>();
        variables.put("tableModel", table);
        variables.put("title", "Summary Statistic");

        final Map<String, String> descriptions = reportParams.getDescriptions();
        if (nonNull(descriptions) && descriptions.containsKey(WidgetType.SUMMARY.name())) {
            variables.put("description", descriptions.get(WidgetType.SUMMARY.name()));
        }

        return variables;
    }

    private List<Row> mapTreeNodes(List<TreeNode> rootNodes, List<String> validationLabels) {
        return rootNodes.stream()
                .filter(node -> node instanceof LabelTemplateTreeNode)
                .map(node -> mapTreeNode(node, validationLabels))
                .collect(Collectors.toList());
    }

    private Row mapTreeNode(TreeNode rootNode, List<String> validationLabels) {
        LabelTemplateTreeNode labelNode = (LabelTemplateTreeNode) rootNode;
        Row row = new Row();

        addDefaultColumns(row, labelNode);
        addValidationLabelColumns(row, labelNode, validationLabels);

        List<TreeNode> children = labelNode.getChildren();
        if (!isEmpty(children)) {
            List<Row> childrenRows = mapTreeNodes(children, validationLabels);

            row.setChildren(childrenRows);
        }

        return row;
    }

    private void addDefaultColumns(Row row, LabelTemplateTreeNode labelNode) {
        boolean isNodeEmpty = labelNode.getTestRunCount() == 0;
        String passRateSuffix = format(" (%s of %s)", labelNode.getTestRunPassedCount(), labelNode.getTestRunCount());
        row.addColumns(
                new Column(labelNode.getName(), Column.BOLD),
                new Column(labelNode.getTestRunCount()),
                isNodeEmpty ? new PercentColumn(labelNode.getPassedRate(), passRateSuffix, Column.GRAY)
                        : new PercentColumn(labelNode.getPassedRate(), passRateSuffix)
        );
    }

    private void addValidationLabelColumns(Row row, LabelTemplateTreeNode labelNode, List<String> validationLabels) {
        if (!isEmpty(validationLabels)) {
            Map<String, CountReportLabelParam> reportLabelParams = labelNode.getReportLabelParams()
                    .stream()
                    .collect(Collectors.toMap(CountReportLabelParam::getName, Function.identity()));

            validationLabels.forEach(label -> {
                if (reportLabelParams.containsKey(label)) {
                    CountReportLabelParam countParam = reportLabelParams.get(label);
                    int passedPercent = countParam.getPassed();
                    int passedCount = countParam.getPassedCount();
                    int totalCount = countParam.getTotalCount();
                    String percentSuffix = format(" (%s of %s)", passedCount, totalCount);
                    row.addColumns(new PercentColumn(passedPercent, percentSuffix, Column.BLACK, Column.BOLD));
                } else {
                    row.addColumns(new Column(Column.N_A));
                }
            });
        }
    }

    @Override
    public WidgetType getType() {
        return WidgetType.SUMMARY;
    }
}
