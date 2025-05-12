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

import static org.qubership.atp.ram.dto.response.ExecutionRequestMainInfoResponse.Executor;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.validation.Valid;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.atp.integration.configuration.mdc.MdcUtils;
import org.qubership.atp.orchestrator.clients.dto.TerminateRequestDto;
import org.qubership.atp.ram.constants.ScreenshotsConstants;
import org.qubership.atp.ram.dto.request.ExecutionRequestConfigUpdateRequest;
import org.qubership.atp.ram.dto.request.ExecutionRequestSearchRequest;
import org.qubership.atp.ram.dto.request.ExecutionRequestsForCompareScreenshotsRequest;
import org.qubership.atp.ram.dto.request.JointExecutionRequestSearchRequest;
import org.qubership.atp.ram.dto.request.LogRecordRegexSearchRequest;
import org.qubership.atp.ram.dto.request.TestRunsByValidationLabelsRequest;
import org.qubership.atp.ram.dto.response.Environment;
import org.qubership.atp.ram.dto.response.ExecutionRequestMainInfoResponse;
import org.qubership.atp.ram.dto.response.ExecutionRequestWidgetConfigTemplateResponse;
import org.qubership.atp.ram.dto.response.IssueResponsesModel;
import org.qubership.atp.ram.dto.response.JointExecutionRequestSearchResponse;
import org.qubership.atp.ram.dto.response.LogRecordRegexSearchResponse;
import org.qubership.atp.ram.dto.response.TestRunsByValidationLabelsResponse;
import org.qubership.atp.ram.entities.ComparisonExecutionRequest;
import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.mdc.MdcField;
import org.qubership.atp.ram.model.ExecutionRequestTestResult;
import org.qubership.atp.ram.model.ExecutionRequestsCompareScreenshotResponse;
import org.qubership.atp.ram.model.TestResult;
import org.qubership.atp.ram.model.request.EnvironmentsCompareRequest;
import org.qubership.atp.ram.model.request.LogRecordCompareRequest;
import org.qubership.atp.ram.model.request.LogRecordCompareRequestItem;
import org.qubership.atp.ram.model.request.RowScreenshotRequest;
import org.qubership.atp.ram.models.EnrichedTestRun;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.ExecutionRequestConfig;
import org.qubership.atp.ram.models.ExecutionRequestDetails;
import org.qubership.atp.ram.models.ExecutionRequestRatesResponse;
import org.qubership.atp.ram.models.ExecutionRequestReporting;
import org.qubership.atp.ram.models.FailPattern;
import org.qubership.atp.ram.models.RootCause;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.models.response.ExecutionRequestResponse;
import org.qubership.atp.ram.models.response.PaginatedResponse;
import org.qubership.atp.ram.models.response.TestRunsRatesResponse;
import org.qubership.atp.ram.pojo.IssueFilteringParams;
import org.qubership.atp.ram.service.template.impl.ScreenshotsReportTemplateRenderService;
import org.qubership.atp.ram.services.ExecutionRequestCompareService;
import org.qubership.atp.ram.services.ExecutionRequestDetailsService;
import org.qubership.atp.ram.services.ExecutionRequestReportingService;
import org.qubership.atp.ram.services.ExecutionRequestService;
import org.qubership.atp.ram.services.IssueService;
import org.qubership.atp.ram.services.JointExecutionRequestService;
import org.qubership.atp.ram.services.OrchestratorService;
import org.qubership.atp.ram.services.TestRunService;
import org.qubership.atp.ram.services.WidgetConfigTemplateService;
import org.qubership.atp.ram.utils.FilesDownloadHelper;
import org.qubership.atp.ram.utils.StepPath;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Splitter;
import freemarker.template.TemplateException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequestMapping("/api/executionrequests")
@RestController()
@CrossOrigin
@RequiredArgsConstructor
@Slf4j
public class ExecutionRequestController /*implements ExecutionRequestControllerApi*/ {

    private final ExecutionRequestService service;
    private final ExecutionRequestReportingService executionRequestReportingService;
    private final ExecutionRequestDetailsService executionRequestDetailsService;
    private final IssueService issueService;
    private final OrchestratorService orchestratorService;
    private final WidgetConfigTemplateService widgetConfigTemplateService;
    private final TestRunService testRunService;
    private final ExecutionRequestCompareService executionRequestCompareService;
    private final ScreenshotsReportTemplateRenderService screenshotsReportTemplateRenderService;
    private final JointExecutionRequestService jointExecutionRequestService;

    /**
     * Returns list of ERs by TestPlanUuid.
     */
    @GetMapping(value = "/testplan/{testPlanUuid}")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "@testPlansService.getProjectIdByTestPlanId(#testPlanUuid),'READ')")
    @AuditAction(auditAction = "Get all execution requests by testPlanUuid = {{#testPlanUuid}}")
    public List<ExecutionRequestResponse> getErsByTestPlanUuid(@PathVariable("testPlanUuid") UUID testPlanUuid) {
        return service.findByTestPlanUuid(testPlanUuid);
    }

    /**
     * Returns list of ERs with qaHosts and taHosts by ProjectUuid.
     */
    @GetMapping(value = "/project/{projectId}/testresults")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "#projectUuid,'READ')")
    @AuditAction(auditAction = "Get execution requests with qaHosts and taHosts by projectId = {{#projectUuid}}")
    public List<ExecutionRequestTestResult> getLastErsByProjectUuidWithTestRuns(
            @PathVariable("projectId") UUID projectUuid) {
        return service.getLastErsByProjectUuidWithTestRuns(projectUuid);
    }

    /**
     * Return list of ERs with qaHosts and taHosts by TestPlanUuid.
     *
     * @param testPlanUuid for found ERs
     * @return list of {@link TestResult}
     */
    @GetMapping(value = "/testplan/{testplanUuid}/testresults")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "@testPlansService.getProjectIdByTestPlanId(#testPlanUuid),'READ')")
    @AuditAction(auditAction = "Get execution requests with qaHosts and taHosts by testPlanId = {{#testPlanUuid}}")
    public List<TestResult> getLastErsByTestplanUuidWithTestRuns(@PathVariable("testplanUuid") UUID testPlanUuid) {
        return service.getLastErsByTestplanUuidWithQaHostsAndTaHosts(testPlanUuid);
    }

    /**
     * Returns list of ERs by list uuid for compare.
     */
    @GetMapping(value = "/compare")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "@executionRequestService.getProjectIdByExecutionRequestId(#uuids.get(0)),'READ')")
    @AuditAction(auditAction = "Get execution requests by uuids = {{#uuid}} for further compare")
    public List<ComparisonExecutionRequest> getErsByUuids(@RequestParam("uuids") List<UUID> uuids) {
        return service.getComparisonExecutionRequest(uuids);
    }

    /**
     * Returns list of ERs with steps  by list uuid for compare.
     */
    @GetMapping(value = "/compareWithSteps")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "@executionRequestService.getProjectIdByExecutionRequestId(#uuids.get(0)),'READ')")
    public List<ComparisonExecutionRequest> getErsWithStepsByUuids(@RequestParam("uuids") List<UUID> uuids) {
        return service.getComparisonExecutionRequestWithSteps(uuids);
    }

    /**
     * Returns sorted list of ERs on page by TestPlanUuid.
     */
    @GetMapping(value = "/testplan/{testPlanUuid}/startIndex/{startIndex}/endIndex/{endIndex}")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "@testPlansService.getProjectIdByTestPlanId(#testPlanUuid),'READ')")
    public List<ExecutionRequestResponse> getErsPageByTestPlanUuidWithSort(
            @PathVariable("testPlanUuid") UUID testPlanUuid,
            @PathVariable("startIndex") int startIndex,
            @PathVariable("endIndex") int endIndex,
            @RequestParam("columnType") String columnType,
            @RequestParam("sortType") String sortType) {
        return service.findResponsePageByTestPlanUuidAndSort(testPlanUuid, startIndex, endIndex, columnType, sortType);
    }

    /**
     * Returns sorted and filtered list of ERs on page by TestPlanUuid.
     */
    @Deprecated
    @GetMapping(value = "/testplan/{testPlanUuid}/startIndex/{startIndex}/endIndex/{endIndex}/filter")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "@testPlansService.getProjectIdByTestPlanId(#testPlanUuid),'READ')")
    public List<ExecutionRequestResponse> getErsPageByTestPlanUuidWithSortAndFilters(
            @PathVariable("testPlanUuid") UUID testPlanUuid,
            @PathVariable("startIndex") int startIndex,
            @PathVariable("endIndex") int endIndex,
            @RequestParam("columnType") String columnType,
            @RequestParam("sortType") String sortType) {
        //Parse Filter list with different classes (Jackson cannot parse it like RequestParam)
        return service.findResponsePageByTestPlanUuidAndSort(
                testPlanUuid, startIndex, endIndex, columnType, sortType);
    }

    @PostMapping(value = "/testplan/{testPlanId}/search")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "@testPlansService.getProjectIdByTestPlanId(#testPlanId),'READ')")
    @AuditAction(auditAction = "Get filtered and sorted execution requests by testPlanId = {{#testPlanId}}")
    public PaginatedResponse<ExecutionRequestResponse> getExecutionRequests(
            @PathVariable("testPlanId") UUID testPlanId,
            @RequestBody ExecutionRequestSearchRequest body) {
        return service.getExecutionRequestsResponse(testPlanId, body);
    }

    @GetMapping(value = "/environments")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "@testPlansService.getProjectIdByTestPlanId(#testPlanId),'READ')")
    @AuditAction(auditAction = "Get execution request's environments by testPlanId = {{#testPlanId}}")
    public List<Environment> getExecutionRequestEnvironments(@RequestParam("testPlanId") UUID testPlanId) {
        return service.getExecutionRequestEnvironments(testPlanId);
    }

    @GetMapping(value = "/executors")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "@testPlansService.getProjectIdByTestPlanId(#testPlanId),'READ')")
    @AuditAction(auditAction = "Get execution request's executors by testPlanId = {{#testPlanId}}")
    public List<Executor> getExecutionRequestExecutors(@RequestParam("testPlanId") UUID testPlanId) {
        return service.getExecutionRequestExecutors(testPlanId);
    }

    /**
     * Returns an ER by UUID.
     */
    @GetMapping(value = "/{executionRequestId}")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "@executionRequestService.getProjectIdByExecutionRequestId(#uuid),'READ')")
    @AuditAction(auditAction = "Get execution request by id = {{#uuid}}")
    public ExecutionRequest getByUuid(@PathVariable("executionRequestId") UUID uuid) {
        return service.findById(uuid);
    }

    /**
     * Returns an ER main info by id.
     */
    @GetMapping(value = "/{executionRequestId}/mainInfo")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "@executionRequestService.getProjectIdByExecutionRequestId(#id),'READ')")
    @AuditAction(auditAction = "Get main info by executionRequestId = {{#id}}")
    public ExecutionRequestMainInfoResponse getByIdMainInfo(@PathVariable("executionRequestId") UUID id) {
        return service.findByIdMainInfo(id);
    }

    /**
     * Returns a comparison table for test runs.
     *
     * @param ids executionRequestIds
     * @return Test Runs comparison table
     */
    @PostMapping(value = "/compare/testrun")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@executionRequestService.getProjectIdByExecutionRequestId(#ids.get(0)),'EXECUTE')")
    @AuditAction(auditAction = "Get comparison table for test runs in execution requests from request body")
    public ResponseEntity<Object> getTestRunCompareInfo(@RequestBody List<UUID> ids) {
        if (ids.size() > 4) {
            return ResponseEntity.badRequest().body("Number of execution requests should not exceed 4");
        }
        if (!executionRequestCompareService.validateExecutionRequestsDuplicates(ids)) {
            return ResponseEntity.badRequest().body("Execution requests must be unique");
        }
        return ResponseEntity.ok(executionRequestCompareService.getTestRunDetailsCompareResponses(ids));
    }

    /**
     * Returns a comparison table for environments.
     *
     * @param request contains environmentIds and executionRequestIds
     * @return EnvironmentsComparisonResponse
     */
    @PostMapping(value = "/compare/environment")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "@executionRequestService.getProjectIdByExecutionRequestId(#request.executionRequestIds.get(0)),"
            + "'EXECUTE')")
    @AuditAction(auditAction = "Get comparison table for execution requests' environments with "
            + "ids = {{#request.environmentIds}}")
    public ResponseEntity<Object> getEnvironmentsCompareInfo(@RequestBody EnvironmentsCompareRequest request) {
        if (request.getExecutionRequestIds().size() > 4) {
            return ResponseEntity.badRequest().body("Number of execution requests should not exceed 4");
        }
        if (!executionRequestCompareService.validateExecutionRequestsDuplicates(request.getExecutionRequestIds())) {
            return ResponseEntity.badRequest().body("Execution requests must be unique");
        }
        return ResponseEntity.ok(executionRequestCompareService.getEnvironmentDetailsCompareResponse(request));
    }

    /**
     * Returns a defect statistic for execution requests.
     *
     * @param ids executionRequestIds
     * @return Defect Statistic Response
     */
    @PostMapping(value = "/compare/defect")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "@executionRequestService.getProjectIdByExecutionRequestId(#ids.get(0)),'EXECUTE')")
    @AuditAction(auditAction = "Get defects statistics for execution requests from request body")
    public ResponseEntity<Object> getDefectStatisticInfo(@RequestBody List<UUID> ids) {
        if (ids.size() > 4) {
            return ResponseEntity.badRequest().body("Number of execution requests should not exceed 4");
        }
        if (!executionRequestCompareService.validateExecutionRequestsDuplicates(ids)) {
            return ResponseEntity.badRequest().body("Execution requests must be unique");
        }
        return ResponseEntity.ok(executionRequestCompareService.getDefectStatisticResponse(ids));
    }

    /**
     * Returns a comparison table for logRecords by parent logRecordId on request.
     *
     * @param request object with compare type and list of
     *                {@link LogRecordCompareRequestItem}
     * @return Log records comparison table
     */
    @PostMapping(value = "/compare/logrecord")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "@executionRequestService.getProjectIdByExecutionRequestId("
            + "#request.logRecordCompareRequestItems.get(0).getExecutionRequestId()),'EXECUTE')")
    @AuditAction(auditAction = "Get comparison table for execution requests' log records by parent logRecordId on "
            + "request")
    public ResponseEntity<Object> getCompareInfo(@RequestBody LogRecordCompareRequest request) {
        if (request.getLogRecordCompareRequestItems().size() > 4) {
            return ResponseEntity.badRequest().body("Number of execution requests should not exceed 4");
        }
        return ResponseEntity.ok(executionRequestCompareService.getLogRecordCompareResponse(request));
    }


    /**
     * Returns an Test Plan id by Execution Request Id.
     *
     * @param id Execution Request identifier
     * @return Test Plan identifier
     */
    @GetMapping(value = "/{executionRequestId}/testplan/id")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "@executionRequestService.getProjectIdByExecutionRequestId(#id),'READ')")
    @AuditAction(auditAction = "Get test plan id by executionRequestId = {{#id}}")
    public UUID getTestPlanIdByUuid(@PathVariable("executionRequestId") UUID id) {
        return service.getTestPlanIdByExecutionRequestId(id);
    }

    /**
     * Get all enriched test runs.
     *
     * @return list of enriched test runs
     */
    @GetMapping(value = "/{executionRequestId}/testruns")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@executionRequestService.getProjectIdByExecutionRequestId(#id),'READ')")
    @AuditAction(auditAction = "Get all enriched test runs for execution request with id = {{#id}}")
    public List<EnrichedTestRun> getAllEnrichedTestRuns(@PathVariable("executionRequestId") UUID id) {
        return service.getAllEnrichedTestRuns(id);
    }

    /**
     * Get all test run ids by executionRequestId.
     *
     * @return list of test run ids
     */
    @GetMapping(value = "/{executionRequestId}/testruns/ids")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@executionRequestService.getProjectIdByExecutionRequestId(#id),'READ')")
    public List<UUID> getAllTestRunIds(@PathVariable("executionRequestId") UUID id) {
        return service.getAllTestRunUuidsByExecutionRequestId(id);
    }

    /**
     * Stop execution request.
     *
     * @deprecated Use ExecutionRequestLoggingController#stop(id) instead of this
     */
    @Deprecated
    @GetMapping(value = "/{executionRequestId}/stop")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "@executionRequestService.getProjectIdByExecutionRequestId(#id),'EXECUTE')")
    public void stopExecutionRequestsById(@PathVariable("executionRequestId") UUID id) {
        ExecutionRequest executionRequest = service.get(id);
        service.stopExecutionRequest(executionRequest, System.currentTimeMillis());
    }

    /**
     * Get all failed test runs by some execution requests.
     *
     * @return list of test runs
     */
    @GetMapping(value = "/{executionRequestId}/failedtestruns")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@executionRequestService.getProjectIdByExecutionRequestId(#id),'READ')")
    public List<TestRun> getAllFailedTestRuns(@PathVariable("executionRequestId") UUID id) {
        return service.getAllFailedTestRuns(id);
    }

    /**
     * Get all test runs by some execution requests.
     *
     * @return list of test test runs
     */
    @GetMapping(value = "/{ids}/testRunsBySomeExecutionRequests")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@executionRequestService.getProjectIdsByExecutionRequestIds(#ids),'READ')")
    public Map<UUID, List<TestRun>> getAllTestRunsBySomeExecutionRequests(@PathVariable("ids") String ids) {
        Map<UUID, List<TestRun>> res = new LinkedHashMap<>();
        Splitter.on(',').trimResults().splitToList(ids).forEach(id -> {
            MdcUtils.put(MdcField.EXECUTION_REQUEST_ID.toString(), id);
            res.put(UUID.fromString(id),
                    service.getAllTestRuns(UUID.fromString(id)));
        });
        return res;
    }

    /**
     * Get all execution requests.
     *
     * @return all execution requests
     */
    @Deprecated
    @GetMapping
    @PreAuthorize("@entityAccess.isAdmin()")
    public List<ExecutionRequest> getAllSortedExecutionRequests() {
        return service.getAllSortedExecutionRequests();
    }

    /**
     * Remove execution requests with test runs, log records and attachments.
     *
     * @param uuidList uuid of execution requests for remove
     */
    @PostMapping(value = "/recursiveDelete")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "@executionRequestService.getProjectIdByExecutionRequestId(#uuidList.get(0)),'DELETE')")
    @AuditAction(auditAction = "Remove execution requests with all test runs and log records "
            + "by executionRequestIds = {{#uuidList}}")
    public void recursiveDelete(@RequestBody List<UUID> uuidList) {
        service.recursiveDeleteListRequests(uuidList);
    }

    /**
     * Create new {@link ExecutionRequest}.
     *
     * @return created {@link ExecutionRequest}
     * @deprecated use ExecutionRequestLoggingController instead of this
     */
    @PostMapping(value = "/create")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "#executionRequest.getProjectId(),'EXECUTE')")
    public ResponseEntity create(@RequestBody ExecutionRequest executionRequest) {
        ExecutionRequest createdExecutionRequest = service.save(executionRequest);
        MdcUtils.put(MdcField.EXECUTION_REQUEST_ID.toString(), executionRequest.getUuid());
        if (jointExecutionRequestService.isJointExecutionRequest(executionRequest)) {
            jointExecutionRequestService.updateActiveJointExecutionRequest(executionRequest);
        }
        return new ResponseEntity<>(createdExecutionRequest, HttpStatus.CREATED);
    }

    /**
     * Old endpoint for analyze ER (create/update AKB records).
     *
     * @param executionRequestUuid ID of ER
     * @deprecated because AKB Records are old entities. Should this be replaced to recalculate top issues?
     */
    @Deprecated
    @GetMapping(value = "/{executionRequestId}/analyze", produces = TEXT_PLAIN_VALUE)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "@executionRequestService.getProjectIdByExecutionRequestId(#executionRequestUuid),'EXECUTE')")
    public void analyzeExecutionRequest(@PathVariable("executionRequestId") UUID executionRequestUuid) {
    }

    @Deprecated
    @PutMapping(value = "/save")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "#executionRequest.getProjectId(),'UPDATE')")
    public ExecutionRequest save(@RequestBody ExecutionRequest executionRequest) {
        MdcUtils.put(MdcField.EXECUTION_REQUEST_ID.toString(), executionRequest.getUuid());
        return service.save(executionRequest);
    }

    @PutMapping(value = "/{executionRequestId}/setLabel")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "@executionRequestService.getProjectIdByExecutionRequestId(#uuidRequest),'UPDATE')")
    public ExecutionRequest setLabel(@PathVariable("executionRequestId") UUID uuidRequest,
                                     @RequestBody List<UUID> uuidLabels) {
        return service.setLabel(uuidRequest, uuidLabels);
    }

    @PutMapping(value = "/{executionRequestId}/removeLabel")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "@executionRequestService.getProjectIdByExecutionRequestId(#uuidRequest),'UPDATE')")
    public ExecutionRequest removeLabel(@PathVariable("executionRequestId") UUID uuidRequest,
                                        @RequestBody List<UUID> uuidLabels) {
        return service.removeLabel(uuidRequest, uuidLabels);
    }

    @GetMapping(value = "/{executionRequestId}/findByStepName/{searchValue}")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "@executionRequestService.getProjectIdByExecutionRequestId(#uuidRequest),'READ')")
    public List<StepPath> findStepBySearchValue(@PathVariable("executionRequestId") UUID uuidRequest,
                                                @PathVariable("searchValue") String searchValue) {
        return service.findStepBySearchValue(uuidRequest, searchValue);
    }

    /**
     * Old endpoint for reanalyze TR-s (create/update AKB records).
     *
     * @param uuidList list of TR-s
     * @deprecated because AKB Records are old entities. Should this be replaced to recalculate top issues?
     */
    @Deprecated
    @PostMapping(value = "/reanalyze")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@executionRequestService.getProjectIdByExecutionRequestId(#uuidList.get(0)),'EXECUTE')")
    public void reanalyzeTestRuns(@RequestBody List<UUID> uuidList) {

    }

    /**
     * Update execution status of ER.
     *
     * @param requestUuid     of ER
     * @param executionStatus is new Execution status
     * @return update {@link ExecutionRequest}
     */
    @PutMapping(value = "/{executionRequestId}/updExecutionStatus/{executionStatus}")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "@executionRequestService.getProjectIdByExecutionRequestId(#requestUuid),'UPDATE')")
    public ExecutionRequest updateExecutionStatus(@PathVariable("executionRequestId") UUID requestUuid,
                                                  @PathVariable("executionStatus") ExecutionStatuses executionStatus) {
        ExecutionRequest executionRequest = service.updateExecutionStatus(requestUuid, executionStatus);
        jointExecutionRequestService.updateJointExecutionRequestRunStatus(executionRequest);

        return executionRequest;
    }

    /**
     * Returns list of ERs with TestRuns by TestScopeUuid.
     */
    @Deprecated
    @GetMapping(value = "/testScope/{testScopeUuid}")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "@executionRequestService.getProjectIdByTestScopeId(#testScopeUuid),'READ')")
    public List<TestResult> getByRestScopeUuid(@PathVariable("testScopeUuid") UUID testScopeUuid) {
        return service.getTestResultsByRestScopeUuid(testScopeUuid);
    }

    /**
     * Terminate set of ExecutionRequests.
     *
     * @param terminateRequestDto set of ExecutionRequests that needs termination.
     */
    @PostMapping(value = "/terminate")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "@executionRequestService.getProjectIdByExecutionRequestId"
            + "(#terminateRequestDto.getExecutionRequestIds().get(0)),'UPDATE')")
    @AuditAction(auditAction = "Terminate over orchestrator set of execution requests with ids = "
            + "{{#terminateRequestDto.executionRequestIds}}")
    public void terminateExecutionRequests(@RequestBody TerminateRequestDto terminateRequestDto) {

        List<UUID> requestsForStoppingOrTerminating = service
                .getRequestsForStoppingOrTerminating(terminateRequestDto.getExecutionRequestIds());
        terminateRequestDto.setExecutionRequestIds(requestsForStoppingOrTerminating);
        orchestratorService.terminate(terminateRequestDto);
    }

    /**
     * Stop or Resume ExecutionRequests according to ExecutionStatuses.
     *
     * @param uuidList set of ExecutionRequests that needs stopping or resuming.
     */
    @PostMapping(value = "/stopresume")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "@executionRequestService.getProjectIdByExecutionRequestId(#uuidList.get(0)),'UPDATE')")
    @AuditAction(auditAction = "Stop (if status is \"In Progress\") or resume (if status is \"Suspended\") execution "
            + "requests with ids from request body")
    public void stopResumeExecutionRequests(@RequestBody List<UUID> uuidList) {
        List<UUID> requestsForStopping = service.getRequestsForStoppingOrTerminating(uuidList);
        List<UUID> requestsForResuming = service.getRequestsForResuming(uuidList);
        orchestratorService.stop(requestsForStopping);
        orchestratorService.resume(requestsForResuming);
    }

    /**
     * Create email reporting.
     *
     * @param id        execution request identifier
     * @param reporting reporting data
     */
    @PostMapping(value = "/{executionRequestId}/emailReporting", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "#reporting.getProjectId(), 'CREATE')")
    public ExecutionRequestReporting createReporting(@PathVariable("executionRequestId") UUID id,
                                                     @RequestBody ExecutionRequestReporting reporting) {
        return executionRequestReportingService.createReporting(id, reporting);
    }

    /**
     * Update status of email reporting.
     * Return updated object or null.
     *
     * @param executionRequestId execution request identifier
     * @param testingStatuses    new status
     */
    @PutMapping(value = "/{executionRequestId}/emailReporting/updateStatus/{status}")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "@executionRequestService.getProjectIdByExecutionRequestId(#executionRequestId),'UPDATE')")
    public ExecutionRequestReporting updateReportingStatus(@PathVariable("executionRequestId") UUID executionRequestId,
                                                           @PathVariable("status") TestingStatuses testingStatuses) {
        return executionRequestReportingService.updateReportingStatus(executionRequestId, testingStatuses);
    }

    /**
     * Get email reporting.
     *
     * @param executionRequestId execution request identifier
     * @return reporting data
     */
    @GetMapping(value = "/{executionRequestId}/emailReporting")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "@executionRequestService.getProjectIdByExecutionRequestId(#executionRequestId),'READ')")
    @AuditAction(auditAction = "Get email reporting for execution reques with id = {{#executionRequestId}}")
    public ExecutionRequestReporting getReporting(@PathVariable("executionRequestId") UUID executionRequestId) {
        return executionRequestReportingService.getReporting(executionRequestId);
    }

    /**
     * Get execution request widget config template.
     *
     * @param executionRequestId execution request identifier
     * @return widget config
     */
    @GetMapping(value = "/{executionRequestId}/config/widgettemplate")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).WIDGET_CONFIGURATION.getName(),"
            + "@executionRequestService.getProjectIdByExecutionRequestId(#executionRequestId),'READ')")
    @AuditAction(auditAction = "Get active widget config template by execution request id = {{#executionRequestId}}")
    public ExecutionRequestWidgetConfigTemplateResponse getWidgetConfigTemplate(
            @PathVariable("executionRequestId") UUID executionRequestId) {
        log.info("Request to get widget config template for execution request '{}'", executionRequestId);

        return widgetConfigTemplateService.getWidgetConfigTemplateForEr(executionRequestId);
    }

    /**
     * Get execution request widget config template.
     *
     * @param executionRequestId execution request identifier
     * @return widget config
     */
    @GetMapping(value = "/{executionRequestId}/config")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "@executionRequestService.getProjectIdByExecutionRequestId(#executionRequestId),'READ')")
    public ExecutionRequestConfig getConfig(@PathVariable("executionRequestId") UUID executionRequestId) {
        log.info("Request to get widget config template for execution request '{}'", executionRequestId);

        return service.getExecutionRequestConfig(executionRequestId);
    }

    /**
     * Get execution request widget config template.
     *
     * @param executionRequestId execution request identifier
     */
    @PatchMapping(value = "/{executionRequestId}/config")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "@executionRequestService.getProjectIdByExecutionRequestId(#executionRequestId),'UPDATE')")
    @AuditAction(auditAction = "Update execution request's '{{#executionRequestId}}' "
            + "widget config template '{{#request.widgetConfigTemplateId}}'")
    public void updateConfig(@PathVariable("executionRequestId") UUID executionRequestId,
                             @RequestBody ExecutionRequestConfigUpdateRequest request) {
        log.info("Request to update config for execution request '{}' with new data: {}", executionRequestId, request);

        service.updateExecutionRequestConfig(executionRequestId, request);
    }

    /**
     * Get all issues present in an execution request. Paginated
     *
     * @param executionRequestId execution request identifier
     * @param startIndex         number of start element
     * @param endIndex           number of finish element
     * @return issues
     */
    @GetMapping(value = "/{executionRequestId}/issues")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "@executionRequestService.getProjectIdByExecutionRequestId(#executionRequestId),'READ')")
    @AuditAction(auditAction = "Get all issues present in the execution request with id = {{#executionRequestId}}")
    public IssueResponsesModel getIssuedById(@PathVariable("executionRequestId") UUID executionRequestId,
                                             @RequestParam("startIndex") int startIndex,
                                             @RequestParam("endIndex") int endIndex,
                                             @RequestParam(required = false) String columnType,
                                             @RequestParam(required = false) String sortType,
                                             IssueFilteringParams issueFilteringParams) {
        log.info("Request to get issues for execution request '{}'", executionRequestId);
        issueService.recalculateTopIssues(executionRequestId);
        return issueService.getAllIssuesByExecutionRequestId(issueFilteringParams, startIndex, endIndex, columnType,
                sortType);
    }


    /**
     * Get all issues present in an execution request.
     *
     * @param executionRequestId execution request identifier
     * @return issues
     */
    @GetMapping(value = "/{executionRequestId}/issues/all")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "@executionRequestService.getProjectIdByExecutionRequestId(#executionRequestId),'READ')")
    public IssueResponsesModel getIssuedById(@PathVariable("executionRequestId") UUID executionRequestId) {
        log.info("Request to get issues for execution request '{}'", executionRequestId);
        return issueService.getAllIssuesByExecutionRequestId(
                executionRequestId);
    }

    /**
     * Executes recalculating of top issues for execution request id.
     *
     * @param executionRequestId id of execution request
     * @return 204 no content
     */
    @PostMapping(value = "/{executionRequestId}/recalculateIssues")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "@executionRequestService.getProjectIdByExecutionRequestId(#executionRequestId),'EXECUTE')")
    @AuditAction(auditAction = "Recalculate top issues for execution request '{{#executionRequestId}}'")
    public ResponseEntity<Void> recalculateIssues(
            @PathVariable("executionRequestId") UUID executionRequestId) {
        log.info("Request to recalculate issues for execution request '{}'", executionRequestId);
        issueService.recalculateIssuesForExecution(executionRequestId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Create details section.
     *
     * @param id        execution request identifier
     * @param reporting details data
     */
    @PostMapping(value = "/{executionRequestId}/details",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "@executionRequestService.getProjectId(#reporting.getProjectId(), "
            + "#reporting.getExecutionRequestId())," + "'EXECUTE')")
    public ExecutionRequestDetails createDetails(@PathVariable("executionRequestId") UUID id,
                                                 @RequestBody ExecutionRequestDetails reporting) {
        return executionRequestDetailsService.createDetails(id, reporting);
    }

    /**
     * Update status of details.
     * Return updated object or null.
     *
     * @param executionRequestId execution request identifier
     * @param details            new status
     */
    @PutMapping(value = "/{executionRequestId}/details/update")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "@executionRequestService.getProjectIdByExecutionRequestId(#executionRequestId),'UPDATE')")
    public ExecutionRequestDetails updateDetails(@PathVariable("executionRequestId") UUID executionRequestId,
                                                 @RequestBody ExecutionRequestDetails details) {
        return executionRequestDetailsService.updateDetailsStatus(executionRequestId, details);
    }

    /**
     * Get details.
     *
     * @param executionRequestId execution request identifier
     * @return details
     */
    @GetMapping(value = "/{executionRequestId}/details")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "@executionRequestService.getProjectIdByExecutionRequestId(#executionRequestId),'READ')")
    @AuditAction(auditAction = "Get execution request details by executionRequestId = {{#executionRequestId}}")
    public List<ExecutionRequestDetails> getDetails(@PathVariable("executionRequestId") UUID executionRequestId) {
        return executionRequestDetailsService.getDetails(executionRequestId);
    }

    /**
     * Get execution request rates info.
     *
     * @param executionRequestId execution request id
     * @return rates response
     */
    @GetMapping(value = "/{executionRequestId}/rates")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "@executionRequestService.getProjectIdByExecutionRequestId(#executionRequestId),'READ')")
    public ExecutionRequestRatesResponse getRates(@PathVariable("executionRequestId") UUID executionRequestId) {
        return service.getRates(executionRequestId);
    }

    /**
     * create a fail pattern.
     * Return created object or null.
     *
     * @param executionRequestId execution request identifier
     * @param failPattern        new fail pattern
     */
    @PostMapping(value = "/{executionRequestId}/failpatterns")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "@executionRequestService.getProjectIdByExecutionRequestId(#executionRequestId),'UPDATE')")
    public FailPattern createFailPattern(@PathVariable("executionRequestId") UUID executionRequestId,
                                         @RequestBody FailPattern failPattern) {
        return issueService.saveFailPattern(failPattern, executionRequestId);
    }

    /**
     * Update a fail pattern.
     * Return updated object or null.
     *
     * @param executionRequestId execution request identifier
     * @param failPattern        new fail pattern
     */
    @PutMapping(value = "/{executionRequestId}/failpatterns")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "@executionRequestService.getProjectIdByExecutionRequestId(#executionRequestId),'UPDATE')")
    public FailPattern updateFailPattern(@PathVariable("executionRequestId") UUID executionRequestId,
                                         @RequestBody FailPattern failPattern) {
        return issueService.saveFailPattern(failPattern, executionRequestId);
    }

    /**
     * delete a fail pattern.
     *
     * @param executionRequestId execution request identifier
     * @param failPatternId      fail pattern
     */
    @DeleteMapping(value = "/{executionRequestId}/failpatterns/{failPatternId}")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "@executionRequestService.getProjectIdByExecutionRequestId(#executionRequestId),'UPDATE')")
    public void deleteFailPattern(@PathVariable("executionRequestId") UUID executionRequestId,
                                  @PathVariable("failPatternId") UUID failPatternId) {
        issueService.deleteFailPattern(failPatternId, executionRequestId);
    }

    @PostMapping(value = "/{executionRequestId}/failedLogRecords/search")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "@executionRequestService.getProjectIdByExecutionRequestId(#executionRequestId),'READ')")
    @AuditAction(auditAction = "Get log records by execution request id = {{#executionRequestId}} and regexp")
    public LogRecordRegexSearchResponse searchLogRecords(@PathVariable("executionRequestId") UUID executionRequestId,
                                                         @RequestBody LogRecordRegexSearchRequest request) {
        return service.searchFailedLogRecords(executionRequestId, request);
    }

    /**
     * Get TR-s rates info.
     *
     * @param executionRequestId execution request id
     * @return rates response
     */
    @GetMapping(value = "/{executionRequestId}/testruns/rates")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@executionRequestService.getProjectIdByExecutionRequestId(#executionRequestId),'READ')")
    public List<TestRunsRatesResponse> getTestRunsRates(@PathVariable("executionRequestId") UUID executionRequestId) {
        return testRunService.getTestRunsRatesWithFailedLr(executionRequestId);
    }

    /**
     * Compare ER-s with child and return tree hierarchy with child.
     *
     * @param compareRequest screenshots compare request
     * @return ER-s with child comparing
     */
    @PostMapping(value = "/compare/screenshots/executionrequest")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "#compareRequest.getProjectId(),'READ')")
    @AuditAction(auditAction = "Compare execution requests' screenshots and return tree heirarchy with child by "
            + "projectId = {{#compareRequest.projectId}} and "
            + "executionRequests = {{#compareRequest.executionRequestIds}}")
    public ResponseEntity<ExecutionRequestsCompareScreenshotResponse> getCompareScreenshotsExecutionRequests(
            @RequestBody @Valid ExecutionRequestsForCompareScreenshotsRequest compareRequest) {
        return ResponseEntity.ok(executionRequestCompareService
                .getCompareScreenshotsExecutionRequests(compareRequest.getExecutionRequestIds(), false));
    }

    /**
     * Get screenshots for subSteps of comparing ER-s.
     *
     * @param rows ID-s of LR-s
     * @return screenshots for subSteps
     */
    @PostMapping(value = "/compare/screenshots/logrecord")
    @AuditAction(auditAction = "Get screenshots for sub steps of comparing execution requests by log record ids from "
            + "request body")
    public ResponseEntity<Object> getCompareScreenshotsSubSteps(
            @RequestBody List<RowScreenshotRequest> rows) {
        return ResponseEntity.ok(executionRequestCompareService.getCompareScreenshotsSubSteps(rows));
    }

    @PostMapping(value = "/{executionRequestId}/testruns/validationlabels/search")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@executionRequestService.getProjectIdByExecutionRequestId(#id),'READ')")
    @AuditAction(auditAction = "Get test runs by validation labels for execution request '{{#id}}'")
    public ResponseEntity<TestRunsByValidationLabelsResponse> searchTestRunsByValidationLabels(
            @PathVariable("executionRequestId") UUID id,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestBody TestRunsByValidationLabelsRequest testRunsRequest) {
        return ResponseEntity.ok(service.searchTestRunsByValidationLabels(id, testRunsRequest, page, size));
    }

    /**
     * Get screenshots for subSteps of comparing ER-s.
     *
     * @param compareRequest compare screenshots request
     * @return screenshots
     */
    @PostMapping(value = "/screenshots/compare-report/export")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "#compareRequest.getProjectId(),'READ')")
    @AuditAction(auditAction = "Get screenshots comparing report for subSteps of comparing execution requests by "
            + "projectId = {{#compareRequest.projectId}} and "
            + "executionRequestIds = {{#compareRequest.executionRequestIds}}")
    public ResponseEntity<String> getScreenshotsHtmlReport(
            @RequestBody @Valid ExecutionRequestsForCompareScreenshotsRequest compareRequest)
            throws TemplateException, IOException {
        String processedTemplate = screenshotsReportTemplateRenderService.render(compareRequest);
        HttpHeaders responseHeaders = FilesDownloadHelper.addDownloadToFileSystemHeaders(
                ScreenshotsConstants.SCREENSHOTS_COMPARE_REPORT_NAME);
        return ResponseEntity.ok().headers(responseHeaders).body(processedTemplate);
    }

    /**
     * Search joint execution requests.
     *
     * @param request search request
     * @return found execution requests
     */
    @PreAuthorize("@entityAccess.checkAccess(T(org.qubership.atp.ram.enums.UserManagementEntities)"
            + ".EXECUTION_REQUEST.getName(), #request.getProjectId(),'READ')")
    @PostMapping(value = "/joint/search")
    @AuditAction(auditAction = "Get joint execution requests by projectId = {{#request.projectId}} and key = "
            + "{{#request.key}}")
    public List<JointExecutionRequestSearchResponse> searchJointExecutionRequests(
            @RequestBody JointExecutionRequestSearchRequest request) {
        return jointExecutionRequestService.search(request);
    }

    /**
     * Get execution request failure reasons.
     *
     * @param executionRequestId execution request identifier
     * @return failure reasons list
     */
    @GetMapping(value = "/{executionRequestId}/failureReasons")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "@executionRequestService.getProjectIdByExecutionRequestId(#executionRequestId),'READ')")
    public Collection<RootCause> getFailureReasons(@PathVariable("executionRequestId") UUID executionRequestId) {
        log.info("Request to get execution request '{}' failure reasons", executionRequestId);

        return service.getFailureReasons(executionRequestId);
    }
}
