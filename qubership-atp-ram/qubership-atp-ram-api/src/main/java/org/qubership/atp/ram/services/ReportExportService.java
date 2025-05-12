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

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletResponse;

import org.qubership.atp.ram.RamConstants;
import org.qubership.atp.ram.dto.response.LabelNodeReportResponse;
import org.qubership.atp.ram.dto.response.LabelNodeReportResponse.TestRunNodeResponse;
import org.qubership.atp.ram.enums.ExecutionRequestWidgets;
import org.qubership.atp.ram.model.TestCaseWidgetCsvExportLine;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.TestCaseWidgetReportRequest;
import org.springframework.stereotype.Service;

import com.opencsv.CSVWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportExportService {

    private final ReportService reportService;
    private final ExecutionRequestService executionRequestService;
    private final WidgetConfigTemplateService widgetConfigTemplateService;

    private static final String TEST_CASES_CSV_FILE_NAME_SUFFIX = "Test_Cases.";
    private static final String CSV_EXT = ".csv";
    private static final String UNDERSCORE = "_";
    private static final String SPACE = " ";
    private static final String ATTACHMENT_HEADER_NAME = "attachment; filename=\"%s\"";
    private static final char CSV_EXPORT_SEPARATOR = '|';

    /**
     * Export 'Test cases' widget into CSV file.
     */
    public void exportTestCasesWidgetIntoCsv(UUID executionRequestId, UUID labelTemplateId,
                                             UUID validationTemplateId, boolean isExecutionRequestsSummary,
                                             HttpServletResponse response, TestCaseWidgetReportRequest request) {
        final LabelNodeReportResponse nodes = reportService.getTestCasesForExecutionRequest(executionRequestId,
                labelTemplateId, validationTemplateId, isExecutionRequestsSummary, request);
        final UUID widgetId = ExecutionRequestWidgets.TEST_CASES.getWidgetId();
        final Map<String, Boolean> columnVisibilityMap =
                widgetConfigTemplateService.getWidgetColumnVisibilityMap(executionRequestId, widgetId);

        final ExecutionRequest executionRequest = executionRequestService.get(executionRequestId);
        final String executionRequestName = executionRequest.getName().replaceAll(SPACE, UNDERSCORE);
        final String fileName = TEST_CASES_CSV_FILE_NAME_SUFFIX + executionRequestName + CSV_EXT;

        response.setHeader(CONTENT_DISPOSITION, String.format(ATTACHMENT_HEADER_NAME, fileName));
        response.setHeader(ACCESS_CONTROL_EXPOSE_HEADERS, CONTENT_DISPOSITION);

        try (CSVWriter writer = new CSVWriter(response.getWriter(), CSV_EXPORT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END)) {
            final List<String> validationLabels = nodes.getValidationLabelsOrder();
            setHeaders(writer, validationLabels, columnVisibilityMap);
            final AtomicInteger depth = new AtomicInteger(0);
            final List<LabelNodeReportResponse> children = nodes.getChildren();

            if (isNotEmpty(children)) {
                children.forEach(node -> printLabelNode(node, writer, validationLabels, columnVisibilityMap, depth));
            } else {
                final List<TestRunNodeResponse> testRunNodes = nodes.getTestRuns();
                if (isNotEmpty(testRunNodes)) {
                    testRunNodes.forEach(testRunNode ->
                            printTestRunNode(testRunNode, writer, validationLabels, depth, columnVisibilityMap));
                }
            }

            response.flushBuffer();
        } catch (Exception e) {
            log.error("Failed to export csv file for Test Case widget for ER with id '{}'", executionRequestId);
        }
    }

    private void setHeaders(CSVWriter writer, List<String> validationLabels, Map<String, Boolean> columnVisibilityMap) {
        final List<String> headerLine = new ArrayList<>();

        RamConstants.DEFAULT_COLUMN_NAMES.forEach(columnName -> {
            if (isColumnVisible(columnName, columnVisibilityMap)) {
                headerLine.add(columnName);
            }
        });
        headerLine.addAll(validationLabels);

        writer.writeNext(headerLine.toArray(new String[0]));
    }

    private boolean isColumnVisible(String columnName, Map<String, Boolean> columnVisibilityMap) {
        return columnVisibilityMap.getOrDefault(columnName, true);
    }

    /**
     * Write Label nodes to a file.
     */
    public void printLabelNode(LabelNodeReportResponse node, CSVWriter writer,
                                List<String> validationLabels, Map<String, Boolean> columnVisibilityMap,
                                AtomicInteger depth) {
        final TestCaseWidgetCsvExportLine line =
                new TestCaseWidgetCsvExportLine(node, validationLabels, columnVisibilityMap, depth.get());

        writer.writeNext(line.getContent());

        final List<LabelNodeReportResponse> childrenLabelNodes = node.getChildren();
        final int nextDepthValue = depth.incrementAndGet();
        final AtomicInteger nextLevelDepth = new AtomicInteger(nextDepthValue);

        if (isNotEmpty(childrenLabelNodes)) {
            childrenLabelNodes.forEach(childLabelNode ->
                    printLabelNode(childLabelNode, writer, validationLabels, columnVisibilityMap, nextLevelDepth));
        }

        final List<TestRunNodeResponse> testRunNodes = node.getTestRuns();
        if (isNotEmpty(testRunNodes)) {
            testRunNodes.forEach(testRunNode ->
                    printTestRunNode(testRunNode, writer, validationLabels, depth, columnVisibilityMap));
        }
    }

    /**
     * Write Test Run nodes to a file.
     */
    public void printTestRunNode(TestRunNodeResponse node, CSVWriter writer, List<String> validationLabels,
                                  AtomicInteger depth, Map<String, Boolean> columnVisibilityMap) {
        TestCaseWidgetCsvExportLine line =
                new TestCaseWidgetCsvExportLine(node, validationLabels, columnVisibilityMap, depth.get());
        writer.writeNext(line.getContent());
    }
}
