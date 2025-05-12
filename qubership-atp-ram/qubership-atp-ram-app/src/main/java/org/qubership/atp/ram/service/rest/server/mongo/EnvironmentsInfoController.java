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
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import org.modelmapper.ModelMapper;
import org.qubership.atp.auth.springbootstarter.exceptions.AtpEntityNotFoundException;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.atp.ram.model.GridFsFileData;
import org.qubership.atp.ram.models.EnvironmentsInfo;
import org.qubership.atp.ram.models.SsmMetricReports;
import org.qubership.atp.ram.services.EnvironmentsInfoService;
import org.qubership.atp.ram.utils.FilesDownloadHelper;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RequestMapping("api/environmentsInfo")
@RestController
@RequiredArgsConstructor
public class EnvironmentsInfoController /*implements EnvironmentsInfoControllerApi*/ {

    private final EnvironmentsInfoService environmentsInfoService;
    private final ModelMapper modelMapper;

    /**
     * Get EnvironmentsInfo by executionRequestId.
     *
     * @param uuid executionRequestId
     */
    @GetMapping(value = "/executionRequest/{executionRequestId}")
    @PreAuthorize("@entityAccess.checkAccess(@executionRequestService.getProjectIdByExecutionRequestId(#uuid),'READ')")
    @AuditAction(auditAction = "Get environments info for execution request with id = {{#uuid}}")
    public EnvironmentsInfo getByExecutionRequestUuid(@PathVariable("executionRequestId") UUID uuid,
                                                      final HttpServletResponse response) throws IOException {
        EnvironmentsInfo envInfo = null;
        try {
            envInfo = environmentsInfoService.findByExecutionRequestId(uuid);
        } catch (AtpEntityNotFoundException e) {
            response.sendError(HttpStatus.NOT_FOUND.value(), e.getMessage());
        }
        return envInfo;
    }

    /**
     * Get EnvironmentsInfo by a list of executionRequestId.
     *
     * @param requestIds list of executionRequestId
     */
    @GetMapping
    @PreAuthorize("@entityAccess.checkAccess(@executionRequestService.getProjectIdByExecutionRequestId("
            + "#requestIds.get(0)),'READ')")
    public List<EnvironmentsInfo> getByExecutionRequests(@RequestParam(value = "requestIds") List<UUID> requestIds) {
        return environmentsInfoService.findByRequestIds(requestIds);
    }

    /**
     * Get mandatory checks report by provided report id.
     *
     * @param reportId report identifier
     * @throws IOException possible IO exception
     */
    @GetMapping(value = "/mandatoryChecksReport/{reportId}")
    @AuditAction(auditAction = "Get mandatory checks report by report id = {{#reportId}}")
    public ResponseEntity<Object> getMandatoryChecksReportByReportId(@PathVariable UUID reportId) throws IOException {
        GridFsFileData fileData = environmentsInfoService.getReportById(reportId);

        return FilesDownloadHelper.getGridFsFileResponseEntity(fileData);
    }

    /**
     * Upload mandatory checks report.
     *
     * @param file               report file
     * @param fileName           fine name
     * @param executionRequestId execution request identifier
     * @return response
     * @throws IOException possible IO exception
     */
    @PostMapping(value = "/mandatoryChecksReport",
            produces = { "application/json" },
            consumes = { "application/octet-stream" })
    @PreAuthorize("@entityAccess.checkAccess(@executionRequestService.getProjectIdByExecutionRequestId("
            + "#executionRequestId),'EXECUTE')")
    public ResponseEntity<UUID> uploadMandatoryChecksReport(
            @RequestBody Resource file,
            @RequestParam(value = "fileName") String fileName,
            @RequestParam(value = "executionRequestId") UUID executionRequestId) throws IOException {
        InputStream inputStream = file.getInputStream();
        UUID reportId = environmentsInfoService.saveMandatoryChecksReport(executionRequestId, inputStream, fileName);

        return ResponseEntity.ok(reportId);
    }

    /**
     * Upload SSM metrics report file into GridFS storage.
     *
     * @param file               file content
     * @param fileName           file name
     * @param type               file type
     * @param contentType        file content type
     * @param executionRequestId execution request identifier
     * @return uploaded report file identifier
     * @throws IOException possible IO exception
     */
    @PostMapping(value = "/ssmMetricsReport", consumes = {"application/octet-stream"})
    public ResponseEntity<UUID> uploadSsmMetricsReport(@RequestBody Resource file,
                                                       @RequestParam String fileName,
                                                       @RequestParam(required = false) String type,
                                                       @RequestParam(required = false) String contentType,
                                                       @RequestParam UUID executionRequestId)
            throws IOException {
        return ResponseEntity.ok(environmentsInfoService.saveSsmMetricsReport(fileName, type, contentType,
                file.getInputStream(), executionRequestId, null));
    }

    /**
     * Update SSM metric reports data.
     *
     * @param executionRequestId execution request identifier
     * @param data               SSM metric reports data
     */
    @PatchMapping("/executionRequest/{executionRequestId}/ssmMetricsReports")
    public void updateSsmMetricReportsData(@PathVariable("executionRequestId") UUID executionRequestId,
                                           @RequestBody SsmMetricReports data) {
        environmentsInfoService.updateSsmMetricReportsData(executionRequestId, data);
    }
}
