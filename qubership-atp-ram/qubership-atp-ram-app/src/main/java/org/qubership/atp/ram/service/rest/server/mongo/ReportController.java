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

package org.qubership.atp.ram.service.rest.server.mongo;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.atp.ram.config.ApiPath;
import org.qubership.atp.ram.dto.response.ExecutionSummaryResponse;
import org.qubership.atp.ram.dto.response.LabelNodeReportResponse;
import org.qubership.atp.ram.dto.response.RootCausesStatisticResponse;
import org.qubership.atp.ram.dto.response.ServerSummaryResponse;
import org.qubership.atp.ram.model.GridFsFileData;
import org.qubership.atp.ram.models.TestCaseWidgetReportRequest;
import org.qubership.atp.ram.services.GridFsService;
import org.qubership.atp.ram.services.ReportExportService;
import org.qubership.atp.ram.services.ReportService;
import org.qubership.atp.ram.utils.FilesDownloadHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@CrossOrigin
@RestController
@RequestMapping(ApiPath.API_PATH + ApiPath.REPORTS_PATH)
@RequiredArgsConstructor
public class ReportController /*implements ReportControllerApi*/ {
    private final ReportService reportService;
    private final ReportExportService csvExportService;
    private final GridFsService gridFsService;

    /**
     * Get {@link ServerSummaryResponse} for ER.
     * Result as
     * [
     *  {
     *      "server": "string",
     *      "build": "list of string"
     *  },
     *  ...
     * ]
     * Or throw {@link AtpEntityNotFoundException} when envInfo doesn't found for ER
     *
     * @param erId for getting server summary
     * @return list of {@link ServerSummaryResponse}
     */
    @GetMapping(value = ApiPath.EXECUTION_REQUEST_ID_PATH + ApiPath.SERVER_SUMMARY_PATH)
    @PreAuthorize("@entityAccess.checkAccess(@executionRequestService.getProjectIdByExecutionRequestId(#erId),'READ')")
    @AuditAction(auditAction = "Get server summary for execution request '{{#erId}}'")
    public List<ServerSummaryResponse> getServerSummaryForExecutionRequest(
            @PathVariable(ApiPath.EXECUTION_REQUEST_ID) UUID erId) {
        return reportService.getServerSummaryForExecutionRequest(erId);
    }

    /**
     * Get {@link RootCausesStatisticResponse} for ER and previous, if exists.
     * Result as:
     * [
     *  {
     *      "startDate": timestamp,
     *      "executionRequestName": string,
     *      "rootCausesGroups": [
     *          {
     *              "rootCauseName": string,
     *              "count": int,
     *              "percent": int
     *          },
     *          ...
     *      ]
     *  },
     *  ...
     * ]
     * Or throw {@link AtpEntityNotFoundException} if ER is null.
     *
     * @param erId for getting root causes statistic
     * @return {@link RootCausesStatisticResponse}
     */
    @GetMapping(value = ApiPath.EXECUTION_REQUEST_ID_PATH + ApiPath.ROOT_CAUSES_STATISTIC_PATH)
    @PreAuthorize("@entityAccess.checkAccess(@executionRequestService.getProjectIdByExecutionRequestId(#erId),'READ')")
    @AuditAction(auditAction = "Get root cause statistic for execution request '{{#erId}}'")
    public List<RootCausesStatisticResponse> getRootCausesStatisticForExecutionRequestAndPrevious(
            @PathVariable(ApiPath.EXECUTION_REQUEST_ID) UUID erId) {
        return reportService.getRootCausesStatisticForExecutionRequestAndPrevious(erId);
    }

    /**
     * Get Test cases for execution request.
     */
    @PostMapping(value = ApiPath.EXECUTION_REQUEST_ID_PATH + ApiPath.TEST_CASES_PATH)
    @PreAuthorize("@entityAccess.checkAccess(@executionRequestService.getProjectIdByExecutionRequestId("
            + "#executionRequestId),'READ')")
    @AuditAction(auditAction = "Get tree of test cases mapped to label template (if configured) for execution request"
            + " '{{#executionRequestId}}'")
    public LabelNodeReportResponse getTestCasesForExecutionRequest(@PathVariable(ApiPath.EXECUTION_REQUEST_ID)
                                                                   UUID executionRequestId,
                                                                   @RequestParam(required = false)
                                                                   UUID labelTemplateId,
                                                                   @RequestParam(required = false)
                                                                   UUID validationTemplateId,
                                                                   @RequestParam(required = false)
                                                                   boolean isExecutionRequestsSummary,
                                                                   @RequestBody
                                                                   TestCaseWidgetReportRequest request) {

        return reportService.getTestCasesForExecutionRequest(executionRequestId, labelTemplateId,
                validationTemplateId, isExecutionRequestsSummary, request);
    }

    @PostMapping(value = ApiPath.EXECUTION_REQUEST_ID_PATH + ApiPath.TEST_CASES_PATH + ApiPath.EXPORT_PATH
            + ApiPath.CSV_PATH)
    @PreAuthorize("@entityAccess.checkAccess(@executionRequestService.getProjectIdByExecutionRequestId("
            + "#executionRequestId),'READ')")
    @AuditAction(auditAction = "Export 'Test cases' widget into csv for execution request '{{#executionRequestId}}'")
    public void exportTestCasesWidgetIntoCsv(@PathVariable(ApiPath.EXECUTION_REQUEST_ID) UUID executionRequestId,
                                             @RequestParam(required = false) UUID labelTemplateId,
                                             @RequestParam(required = false) UUID validationTemplateId,
                                             @RequestParam(required = false) boolean isExecutionRequestsSummary,
                                             @RequestBody TestCaseWidgetReportRequest request,
                                             HttpServletResponse response) {
        csvExportService.exportTestCasesWidgetIntoCsv(executionRequestId, labelTemplateId, validationTemplateId,
                isExecutionRequestsSummary, response, request);
    }

    /**
     * Get {@link ExecutionSummaryResponse}.
     * Result as:
     * {
     *   "name": "Execution request-001",
     *   "startDate": "2020-05-11T11:02:31.964+0000",
     *   "finishDate": "2020-05-11T13:02:31.964+0000",
     *   "testCasesCount": 1183,
     *   "passedRate": 11,
     *   "passedCount": 25,
     *   "failedRate": 35,
     *   "failedCount": 412,
     *   "warningRate": 35,
     *   "warningCount": 512,
     *   "duration": 42,
     *   "browserSessionLink": ["link1.some-domain", "link2.some-domain"]
     *   "environmentLink": "link222.some-domain.com",
     *   "threads": 2
     * }
     *
     * @param erId for found ER and TR-s
     * @return {@link ExecutionSummaryResponse}
     */
    @GetMapping(value = ApiPath.EXECUTION_REQUEST_ID_PATH + ApiPath.EXECUTION_SUMMARY_PATH)
    @PreAuthorize("@entityAccess.checkAccess(@reportService.getProjectIdByExecutionRequestId(#erId),'READ')")
    @AuditAction(auditAction = "Get execution summary for execution request '{{#erId}}'")

    public ExecutionSummaryResponse getExecutionSummary(@PathVariable(ApiPath.EXECUTION_REQUEST_ID) UUID erId,
                                                        @RequestParam(required = false)
                                                        boolean isExecutionSummaryRunsSummary) {
        return reportService.getExecutionSummary(erId, isExecutionSummaryRunsSummary);
    }

    /**
     * Get report by provided identifier.
     *
     * @param reportId report identifier
     * @return report content data
     * @throws IOException possible IO exception
     */
    @GetMapping(value = "/ssmMetricsReport/{reportId}")
    @AuditAction(auditAction = "Get ssm metrics report by reportId = {{#reportId}}")
    public ResponseEntity<Object> getSsmMetricsReport(@PathVariable UUID reportId) throws IOException {
        GridFsFileData fileData = gridFsService.getReportById(reportId);

        return FilesDownloadHelper.getGridFsFileResponseEntity(fileData);
    }
}
