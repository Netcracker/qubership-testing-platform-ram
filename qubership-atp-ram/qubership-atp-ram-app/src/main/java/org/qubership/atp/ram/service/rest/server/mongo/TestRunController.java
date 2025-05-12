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

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.atp.integration.configuration.mdc.MdcUtils;
import org.qubership.atp.ram.config.ApiPath;
import org.qubership.atp.ram.dto.request.AnalyzedTestRunRequest;
import org.qubership.atp.ram.dto.request.JiraTicketUpdateRequest;
import org.qubership.atp.ram.dto.request.LabelsPathSearchRequest;
import org.qubership.atp.ram.dto.request.StatusUpdateRequest;
import org.qubership.atp.ram.dto.request.TestRunDefectsPropagationRequest;
import org.qubership.atp.ram.dto.request.TestingStatusUpdateRequest;
import org.qubership.atp.ram.dto.request.UpdateTestRunsRootCause;
import org.qubership.atp.ram.dto.response.AnalyzedTestRunResponse;
import org.qubership.atp.ram.dto.response.BaseEntityResponse;
import org.qubership.atp.ram.dto.response.LogRecordPreviewResponse;
import org.qubership.atp.ram.dto.response.NonGroupedTestRunResponse;
import org.qubership.atp.ram.dto.response.PaginationResponse;
import org.qubership.atp.ram.dto.response.StatusUpdateResponse;
import org.qubership.atp.ram.dto.response.TestRunDefectsPropagationResponse;
import org.qubership.atp.ram.dto.response.TestRunResponse;
import org.qubership.atp.ram.dto.response.TestRunTreeResponse;
import org.qubership.atp.ram.enums.DefaultRootCauseType;
import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.mdc.MdcField;
import org.qubership.atp.ram.model.LogRecordFilteringRequest;
import org.qubership.atp.ram.models.AnalyzedTestRunSortedColumns;
import org.qubership.atp.ram.models.EnrichedTestRun;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.SetBulkFinalTestRuns;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.models.TestRunSearchRequest;
import org.qubership.atp.ram.models.TestRunStatistic.ReportLabelParameterData;
import org.qubership.atp.ram.models.TestRunsCommentSetBulkRequest;
import org.qubership.atp.ram.models.TestRunsFailureReasonSetBulkRequest;
import org.qubership.atp.ram.models.logrecords.parts.ContextVariable;
import org.qubership.atp.ram.services.GridFsService;
import org.qubership.atp.ram.services.OrchestratorService;
import org.qubership.atp.ram.services.TestRunService;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping(ApiPath.API_PATH + ApiPath.TEST_RUNS_PATH)
@RequiredArgsConstructor
@Slf4j
public class TestRunController /*implements TestRunControllerApi*/ {

    private final TestRunService service;
    private final OrchestratorService orchestratorService;
    private final GridFsService gridFsService;

    @GetMapping(value = ApiPath.UUID_PATH)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@testRunService.getProjectIdByTestRunId(#uuid),'READ')")
    @AuditAction(auditAction = "Get test run by id = {{#uuid}}")
    public TestRun getById(@PathVariable(ApiPath.UUID) UUID uuid) {
        return service.getByUuid(uuid);
    }

    @GetMapping(value = ApiPath.TEST_CASE_PATH + ApiPath.UUID_PATH)
    @PostAuthorize("returnObject == null ? true : @entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@testRunService.getProjectIdByTestRunId(returnObject.getUuid()), 'READ')")
    @AuditAction(auditAction = "Get latest test run by test case id = {{#testCaseId}}")
    public TestRun getByTestCase(@PathVariable(ApiPath.UUID) UUID testCaseId) {
        return service.getByTestCase(testCaseId);
    }

    /**
     * Get all log records (with child) by {@link TestRun} id.
     *
     * @param id link on {@link TestRun}
     * @return all log records by {@link TestRun} id
     */
    @GetMapping(value = ApiPath.TEST_RUN_ID_PATH + ApiPath.LOG_RECORDS_PATH)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@testRunService.getProjectIdByTestRunId(#id),'READ')")
    public List<LogRecord> getAllLogRecords(@PathVariable(ApiPath.TEST_RUN_ID) UUID id) {
        return service.getAllLogRecordsByTestRunId(id);
    }

    /**
     * Get all log records (with child) by {@link TestRun} id.
     *
     * @param id link on {@link TestRun}
     * @return all log records by {@link TestRun} id
     */
    @PostMapping(value = ApiPath.TEST_RUN_ID_PATH + ApiPath.LOG_RECORDS_PATH)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@testRunService.getProjectIdByTestRunId(#id),'READ')")
    public List<LogRecord> getAllFilteredLogRecords(@PathVariable(ApiPath.TEST_RUN_ID) UUID id,
                                                    @RequestBody(required = false) LogRecordFilteringRequest filter) {
        return service.getAllFilteredLogRecordsByTestRunId(id, filter);
    }

    @DeleteMapping(value = ApiPath.DELETE_PATH + ApiPath.TEST_RUN_ID_PATH)
    @PreAuthorize("@entityAccess.checkAccess(T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@testRunService.getProjectIdByTestRunId(#uuid),'DELETE')")
    public void delete(@PathVariable(ApiPath.TEST_RUN_ID) UUID uuid) {
        service.deleteByUuid(uuid);
    }

    /**
     * Create test run.
     *
     * @param testRun for creating
     * @return created test run
     * @deprecated use TestRunLoggingController
     */
    @PostMapping(value = ApiPath.CREATE_PATH)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@testRunService.getProjectIdByTestRun(#testRun),'CREATE')")
    @Deprecated
    public ResponseEntity create(@RequestBody TestRun testRun) {
        return new ResponseEntity<>(service.save(testRun), HttpStatus.CREATED);
    }

    @Deprecated
    @PutMapping(value = ApiPath.SAVE_PATH)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@testRunService.getProjectIdByTestRun(#testRun),'UPDATE')")
    public TestRun save(@RequestBody TestRun testRun) {
        MdcUtils.put(MdcField.TEST_RUN_ID.toString(), testRun.getUuid());
        return service.save(testRun);
    }

    // This method for new RCA with tree. Saves id of RootCause item
    @PutMapping(value = ApiPath.TEST_RUN_ID_PATH + ApiPath.SAVE_ROOT_CASE_PATH + ApiPath.ROOT_CAUSE_ID_PATH)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@testRunService.getProjectIdByTestRunId(#uuid),'UPDATE')")
    public TestRun saveRootCause(@PathVariable(ApiPath.TEST_RUN_ID) UUID uuid,
                                 @PathVariable(ApiPath.ROOT_CAUSE_ID) UUID rootCauseId) {
        return service.saveRootCause(uuid, rootCauseId);
    }


    @PutMapping(value = ApiPath.SAVE_ROOT_CASE_PATH)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@testRunService.getProjectIdByTestRunId(#body.getTestRunIds().get(0)),'UPDATE')")
    public List<TestRun> saveRootCausesForOfListTestRuns(@RequestBody UpdateTestRunsRootCause body) {
        return service.saveRootCausesForListOfTestRuns(body.getTestRunIds(), body.getRootCauseId());
    }

    /**
     * Get log records (top level without child) for test run.
     *
     * @param uuid of test run
     * @return list of log record
     */
    @GetMapping(value = ApiPath.TEST_RUN_ID_PATH + ApiPath.TOP_LEVEL_LOG_RECORDS_PATH)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@testRunService.getProjectIdByTestRunId(#uuid),"
            + "'READ')")
    @AuditAction(auditAction = "Get top level log records by testRunId = {{#uuid}}")
    public List<LogRecord> getTopLevelLogRecords(
            @PathVariable(ApiPath.TEST_RUN_ID) UUID uuid,
            @RequestParam(value = "statuses", required = false) List<String> statuses,
            @RequestParam(value = "types", required = false) List<String> types,
            @RequestParam(value = "showNotAnalyzedItemsOnly", required = false) boolean showNotAnalyzedItemsOnly) {
        LogRecordFilteringRequest filteringRequest =
                new LogRecordFilteringRequest(statuses, types, showNotAnalyzedItemsOnly);
        return service.getTopLevelLogRecords(uuid, filteringRequest);
    }

    @GetMapping(value = ApiPath.PROJECT_PATH + "/{projectId}")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(), #projectUuid, 'READ')")
    public List<TestRun> getTestRunsForProject(@PathVariable("projectId") UUID projectUuid) {
        return service.getTestRunsForProject(projectUuid);
    }

    @GetMapping(value = ApiPath.PROJECT_PATH + "/{projectId}" + ApiPath.TEST_CASES_NAMES_PATH)
    @PreAuthorize("@entityAccess.checkAccess(#projectId, 'READ')")
    public List<String> getTestCasesList(@PathVariable("projectId") UUID projectId) {
        return service.getTestCasesNamesForProject(projectId).stream()
                .map(TestRun::getTestCaseName).distinct().collect(Collectors.toList());
    }

    /**
     * Change execution status of Test run.
     *
     * @param uuid              of TR
     * @param executionStatuses is new execution status
     * @return updated {@link TestRun}
     */
    @PutMapping(value = ApiPath.TEST_RUN_ID_PATH + ApiPath.UPD_EXECUTION_STATUS_PATH + ApiPath.EXECUTION_STATUS_PATH)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@testRunService.getProjectIdByTestRunId(#uuid),'UPDATE')")
    public TestRun updExecutionStatus(@PathVariable(ApiPath.TEST_RUN_ID) UUID uuid,
                                      @PathVariable(ApiPath.EXECUTION_STATUS) ExecutionStatuses executionStatuses) {
        TestRun testRun = getById(uuid);
        testRun.setExecutionStatus(executionStatuses);
        return service.save(testRun);
    }

    /**
     * Change testing status of Test run log records.
     *
     * @param uuid            of TR
     * @param testingStatuses is new testing status
     * @return updated {@link TestRun}
     */
    @PutMapping(value =
            ApiPath.TEST_RUN_ID_PATH + ApiPath.LOG_RECORDS_PATH + ApiPath.UPD_TESTING_STATUS_PATH
                    + ApiPath.TESTING_STATUS_PATH)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@testRunService.getProjectIdByTestRunId(#uuid),'UPDATE')")
    public List<LogRecord> updNotStartedLogRecordsTestingStatus(@PathVariable(ApiPath.TEST_RUN_ID)
                                                                        UUID uuid,
                                                                @PathVariable(ApiPath.TESTING_STATUS)
                                                                        TestingStatuses testingStatuses) {
        return service.updateLogRecordsTestingStatus(uuid, testingStatuses);
    }

    /**
     * Get all context variable of logrecord.
     *
     * @param testRunId uuid of {@link TestRun}
     * @return all children log records
     */
    @GetMapping(value = "/{testRunId}/contextVariables/all")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@testRunService.getProjectIdByTestRunId(#testRunId),'READ')")
    public ResponseEntity<List<ContextVariable>> getAllContextVariables(
            @PathVariable(ApiPath.TEST_RUN_ID) UUID testRunId) {
        return new ResponseEntity<>(service.getAllContextVariables(testRunId),
                HttpStatus.OK);
    }

    /**
     * Start test run.
     *
     * @param uuid the id of test run
     * @return the test run
     */
    @PutMapping(value = ApiPath.TEST_RUN_ID_PATH + ApiPath.START_PATH)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@testRunService.getProjectIdByTestRunId(#uuid),'UPDATE')")
    public TestRun startTestRun(@PathVariable(ApiPath.TEST_RUN_ID) UUID uuid) {
        log.debug("start startTestRun(id: {})", uuid);
        TestRun testRun = getById(uuid);
        testRun.setExecutionStatus(ExecutionStatuses.IN_PROGRESS);
        testRun.setStartDate(new Timestamp(System.currentTimeMillis()));
        return service.save(testRun);
    }

    /**
     * Finish test run.
     *
     * @param uuid the id of test run
     * @return the test run
     */
    @PutMapping(value = ApiPath.TEST_RUN_ID_PATH + ApiPath.FINISH_PATH)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@testRunService.getProjectIdByTestRunId(#uuid),'UPDATE')")
    public TestRun finishTestRun(@PathVariable(ApiPath.TEST_RUN_ID) UUID uuid) {
        log.debug("start finishTestRun(id: {})", uuid);
        TestRun testRun = getById(uuid);
        service.finishTestRun(testRun, false);
        log.info("finishTestRun: id {} testing status {}", uuid, testRun.getTestingStatus());
        return service.save(testRun);
    }

    /**
     * Change testing status of Test run.
     *
     * @param uuid            of TR
     * @param testingStatuses is new testing status
     * @return updated {@link TestRun}
     */
    @PutMapping(value = ApiPath.TEST_RUN_ID_PATH + ApiPath.UPD_TESTING_STATUS_PATH + ApiPath.TESTING_STATUS_PATH)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@testRunService.getProjectIdByTestRunId(#uuid),'UPDATE')")
    public TestRun updTestingStatus(@PathVariable(ApiPath.TEST_RUN_ID) UUID uuid,
                                    @PathVariable(ApiPath.TESTING_STATUS) TestingStatuses testingStatuses) {
        TestRun testRun = getById(uuid);
        log.debug("updTestingStatus: new testing status = {} for test run {}. ", testingStatuses, testRun);
        testRun.updateTestingStatus(testingStatuses);
        return service.save(testRun);
    }

    /**
     * Force upd testing status test run by top log records.
     *
     * @param testRunId the Test Run Id
     * @return the test run
     */
    @PutMapping(value = ApiPath.TEST_RUN_ID_PATH + ApiPath.UPD_TESTING_STATUS_PATH)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@testRunService.getProjectIdByTestRunId(#testRunId),'UPDATE')")
    public TestRun updTestingStatus(@PathVariable(ApiPath.TEST_RUN_ID) UUID testRunId) {
        log.debug("start updTestingStatus(testRunId: {})", testRunId);
        return service.updTestingStatus(testRunId, false);
    }

    /**
     * Force upd testing status test run by top log records. Replace TestRun status without priority.
     *
     * @param testRunId the Test Run Id
     * @return the test run
     */
    @PutMapping(value = ApiPath.TEST_RUN_ID_PATH + ApiPath.UPD_TESTING_STATUS_HARD_PATH)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@testRunService.getProjectIdByTestRunId(#testRunId),'UPDATE')")
    public TestRun updTestingStatusWithTestRunStatusHardReplace(@PathVariable(ApiPath.TEST_RUN_ID) UUID testRunId) {
        log.debug("start updTestingStatus(testRunId: {})", testRunId);
        return service.updTestingStatus(testRunId, true);
    }


    /**
     * Change testing status of Test run without compare.
     *
     * @param uuid            of TR
     * @param testingStatuses is new testing status
     * @return updated {@link TestRun}
     */
    @PutMapping(value = ApiPath.TEST_RUN_ID_PATH + ApiPath.UPD_TESTING_STATUS_PATH
            + ApiPath.TESTING_STATUS_PATH + ApiPath.HARD_PATH)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@testRunService.getProjectIdByTestRunId(#uuid),'UPDATE')")
    @AuditAction(auditAction = "Set testing status = '{{#testingStatuses.name}}' for test run '{{#uuid}}' "
            + "without compare")
    public TestRun updTestingStatusHard(@PathVariable(ApiPath.TEST_RUN_ID) UUID uuid,
                                        @PathVariable(ApiPath.TESTING_STATUS) TestingStatuses testingStatuses) {
        return service.updTestingStatusHard(uuid, testingStatuses);
    }

    /**
     * Terminate set of test runs.
     *
     * @param uuidList set of test runs that needs termination.
     */
    @PostMapping(value = ApiPath.TERMINATE_PATH)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@testRunService.getProjectIdByTestRunId(#uuidList.get(0)),'UPDATE')")
    @AuditAction(auditAction = "Terminate provided set of test runs")
    public void terminate(@RequestBody List<UUID> uuidList) {
        try {
            List<UUID> testRunIds = service.getTestRunsForStoppingOrTerminating(uuidList);
            orchestratorService.terminateTestRun(testRunIds);
        } catch (Exception e) {
            log.error("Unable terminate TR-s: {}", uuidList, e);
        }
    }

    /**
     * Change testing status and browser names of Test run.
     *
     * @param uuid            of TR
     * @param testingStatuses is new testing status
     * @param browserNames is new browser names
     * @return updated {@link TestRun}
     */
    @PutMapping(value = ApiPath.TEST_RUN_ID_PATH + ApiPath.UPD_TESTING_STATUS_PATH + ApiPath.TESTING_STATUS_PATH
            + ApiPath.UPD_BROWSER_NAMES_PATH)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@testRunService.getProjectIdByTestRunId(#uuid),"
            + "'UPDATE')")
    public TestRun updTestingStatusHardAndBrowserNames(@PathVariable(ApiPath.TEST_RUN_ID) UUID uuid,
                                                   @PathVariable(ApiPath.TESTING_STATUS)
                                                           TestingStatuses testingStatuses,
                                                   @RequestBody List<String> browserNames) {
        TestRun testRun = getById(uuid);
        log.debug("updTestingStatus: new testing status = {}, setBrowserNames: browser names = {} for test run {}.",
                testingStatuses, browserNames, testRun);
        return service.updTestingStatusHardAndBrowserNames(testRun, testingStatuses, browserNames);
    }

    /**
     * Stop or Resume test runs according to ExecutionStatuses.
     *
     * @param uuidList set of test runs that needs stopping or resuming.
     */
    @PostMapping(value = ApiPath.STOP_RESUME_PATH)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@testRunService.getProjectIdByTestRunId(#uuidList.get(0)),'UPDATE')")
    @AuditAction(auditAction = "Stop or Resume provided test runs according to ExecutionStatuses")
    public void stopResume(@RequestBody List<UUID> uuidList) {
        List<UUID> requestsForStopping = service.getTestRunsForStoppingOrTerminating(uuidList);
        List<UUID> requestsForResuming = service.getTestRunsForResuming(uuidList);
        TestRun firstTr = service.get(uuidList.get(0));
        orchestratorService.stopTestRun(requestsForStopping, firstTr.getExecutionRequestId());
        orchestratorService.resumeTestRun(requestsForResuming, firstTr.getExecutionRequestId());
    }

    /**
     * Terminate not finished test runs for current ER.
     *
     * @param executionRequestId for found test runs
     */
    @PutMapping(value = ApiPath.EXECUTION_REQUEST_ID_PATH + ApiPath.STATUS_TO_TERMINATED_PATH)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@executionRequestService.getProjectIdByExecutionRequestId(#executionRequestId), 'UPDATE')")
    public void updateTestRunsStatusToTerminatedByErId(@PathVariable(ApiPath.EXECUTION_REQUEST_ID)
                                                               UUID executionRequestId) {
        service.updateTestRunsStatusToTerminatedByErId(executionRequestId);
    }

    /**
     * Find current test run and return info about this with parent, if exists.
     *
     * @param uuid for found test run
     * @return current test run with parent if exists
     */
    @GetMapping(value = ApiPath.TEST_RUN_ID_PATH + ApiPath.PARENT_PATH)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@testRunService.getProjectIdByTestRunId(#uuid),"
            + "'READ')")
    @AuditAction(auditAction = "Get test run with parent (if exists) by id = {{#uuid}}")
    public TestRunTreeResponse getTestRunByIdWithParent(@PathVariable(ApiPath.TEST_RUN_ID) UUID uuid) {
        return service.getTestRunByIdWithParent(uuid);
    }

    /**
     * Find all entities which meets the filtering criterion (testRunId/statuses/types/showNotAnalyzedItemsOnly).
     *
     * @param testRunId                ID of test run
     * @param statuses                 list of statuses
     * @param types                    list of types
     * @param showNotAnalyzedItemsOnly flag
     * @return list of entities which meet the filtering criterion
     */
    @GetMapping(value = ApiPath.TEST_RUN_ID_PATH + "/logrecords/previews")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@testRunService.getProjectIdByTestRunId(#testRunId),"
            + "'READ')")
    @AuditAction(auditAction = "Find all screenshot previews for carousel for test run '{{#testRunId}}' "
            + "and filtering criterion")
    public List<LogRecordPreviewResponse> getAllLogRecordPreviews(
            @PathVariable(ApiPath.TEST_RUN_ID) UUID testRunId,
            @RequestParam(value = "statuses", required = false) List<String> statuses,
            @RequestParam(value = "types", required = false) List<String> types,
            @RequestParam(value = "showNotAnalyzedItemsOnly", required = false) boolean showNotAnalyzedItemsOnly) {
        LogRecordFilteringRequest filteringRequest =
                new LogRecordFilteringRequest(statuses, types, showNotAnalyzedItemsOnly);
        return service.getAllLogRecordPreviews(testRunId, filteringRequest);
    }

    /**
     * Get test run test case.
     *
     * @param testRunId test run id
     * @return test case info
     */
    @GetMapping(value = ApiPath.TEST_RUN_ID_PATH + "/testcase")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@testRunService.getProjectIdByTestRunId(#testRunId),"
            + "'READ')")
    @AuditAction(auditAction = "Get test case for test run '{{#testRunId}}'")
    public BaseEntityResponse getTestRunTestCase(@PathVariable(ApiPath.TEST_RUN_ID) UUID testRunId) {
        return service.getTestRunTestCase(testRunId);
    }

    /**
     * Get non-grouped test runs by execution request.
     *
     * @param executionRequestId execution request id
     * @return list of test runs
     */
    @GetMapping(value = "/nongrouped")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@executionRequestService.getProjectIdByExecutionRequestId(#executionRequestId),"
            + "'READ')")
    @AuditAction(auditAction = "Get non-grouped test runs by executionRequestId = {{#executionRequestId}}")
    public List<NonGroupedTestRunResponse> getNonGroupedTestRuns(@RequestParam("executionRequestId")
                                                                         UUID executionRequestId) {
        return service.getNonGroupedTestRuns(executionRequestId);
    }

    /**
     * Create or update test run statistic report label param.
     *
     * @param testRunId test run id
     * @param paramData param data
     */
    @PostMapping(value = ApiPath.TEST_RUN_ID_PATH + ApiPath.STATISTIC_PATH + ApiPath.REPORT_LABELS_PARAMS_PATH
            + ApiPath.NAME_PATH)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@testRunService.getProjectIdByTestRunId(#testRunId),'UPDATE')")
    public void upsertTestRunStatisticReportLabelParam(@PathVariable(ApiPath.TEST_RUN_ID) UUID testRunId,
                                                       @PathVariable(ApiPath.NAME) String paramName,
                                                       @RequestBody ReportLabelParameterData paramData) {
        log.info("Request to upsert test run '{}' statistic report label param '{}' with data: {}",
                testRunId, paramName, paramData);
        service.upsertTestRunStatisticReportLabelParam(testRunId, paramName, paramData);
    }

    /**
     * Get test runs for analyze with filtration.
     *
     * @param page          start index
     * @param size          end index
     * @param sortColumn    column for sorting
     * @param sortType      sorting type
     * @param searchRequest filter
     * @return filtered test runs for analyze
     */
    @PostMapping(ApiPath.ANALYZED_PATH)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@executionRequestService.getProjectIdByExecutionRequestId(#searchRequest.getExecutionRequestId()),"
            + "'READ')")
    @AuditAction(auditAction = "Get test runs for analyze with filtration "
            + "for execution request '{{#searchRequest.executionRequestId}}'")
    public ResponseEntity<AnalyzedTestRunResponse> getAnalyzedTestRuns(
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @RequestParam("sortColumn") AnalyzedTestRunSortedColumns sortColumn,
            @RequestParam("sortType") Sort.Direction sortType,
            @RequestBody @Valid TestRunSearchRequest searchRequest) {
        MdcUtils.put(MdcField.EXECUTION_REQUEST_ID.toString(), searchRequest.getExecutionRequestId());
        AnalyzedTestRunResponse analyzedTestRunResponse =
                service.getAnalyzedTestRuns(page, size, sortColumn, sortType, searchRequest);
        return ResponseEntity.ok(analyzedTestRunResponse);
    }

    /**
     * Update test run from analyze tab.
     *
     * @param testRunId       test run id
     * @param analyzedTestRun info to update
     */
    @PostMapping(ApiPath.ANALYZED_PATH + ApiPath.TEST_RUN_ID_PATH + "/update")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@testRunService.getProjectIdByTestRunId(#testRunId),"
            + "'UPDATE')")
    @AuditAction(auditAction = "Update test run '{{#testRunId}}' from analyze tab")
    public void updateAnalyzedTestRun(@PathVariable(ApiPath.TEST_RUN_ID) UUID testRunId,
                                      @RequestBody AnalyzedTestRunRequest analyzedTestRun) {
        service.updateAnalyzedTestRun(testRunId, analyzedTestRun);
    }

    /**
     * Get classifier of testing statuses.
     *
     * @return classifier of testing statuses
     */
    @GetMapping(ApiPath.TESTING_STATUSES_PATH)
    public Map<TestingStatuses, String> getTestingStatuses() {
        return service.getTestStatuses();
    }

    /**
     * Get classifier of failure reasons.
     *
     * @return classifier of failure reasons
     */
    @GetMapping(ApiPath.FAILURE_REASONS_PATH)
    public Map<DefaultRootCauseType, String> getFailureReasons() {
        return service.getFailureReasons();
    }


    /**
     * Get test runs status update.
     *
     * @param request status update request
     * @return status update response
     */
    @PostMapping(value = "/statusUpdate")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@testRunService.getProjectIdByTestRunId(#request.getTestRunsIds().get(0)),"
            + "'EXECUTE')")
    @AuditAction(auditAction = "Update provided test runs statuses")
    public StatusUpdateResponse getStatusUpdate(@RequestBody StatusUpdateRequest request) {
        return service.getStatusUpdate(request);
    }

    /**
     * Search test runs by filter request.
     *
     * @param page          page number
     * @param size          page size
     * @param searchRequest search request
     * @return founded test runs
     */
    @PostMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    @PostAuthorize("returnObject.getEntities().size() == 0 ? true : @entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@executionRequestService.getProjectIdByExecutionRequestId("
            + "returnObject.getEntities().get(0).getExecutionRequestId()),"
            + "'READ')")
    public PaginationResponse<TestRun> search(@RequestParam(value = "page", required = false) int page,
                                              @RequestParam(value = "size", required = false) int size,
                                              @RequestBody TestRunSearchRequest searchRequest) {
        MdcUtils.put(MdcField.EXECUTION_REQUEST_ID.toString(), searchRequest.getExecutionRequestId());
        return service.search(searchRequest, page, size);
    }

    /**
     * Search test runs by filter request.
     *
     * @param page          page number
     * @param size          page size
     * @param searchRequest search request
     * @return founded test runs
     */
    @PostMapping(value = "/enriched/search", produces = MediaType.APPLICATION_JSON_VALUE)
    @PostAuthorize("returnObject.getEntities().size() == 0 ? true : @entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@executionRequestService.getProjectIdByExecutionRequestId("
            + "returnObject.getEntities().get(0).getExecutionRequestId()),"
            + "'READ')")
    @AuditAction(auditAction = "Get enriched test runs for execution request '{{#searchRequest.executionRequestId}}' "
            + "and provided filters")
    public PaginationResponse<EnrichedTestRun> searchEnriched(@RequestParam(value = "page", required = false) int page,
                                                              @RequestParam(value = "size", required = false) int size,
                                                              @RequestBody TestRunSearchRequest searchRequest) {
        MdcUtils.put(MdcField.EXECUTION_REQUEST_ID.toString(), searchRequest.getExecutionRequestId());
        return service.searchEnriched(searchRequest, page, size);
    }

    @PatchMapping(value = "/failurereason/bulk")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@testRunService.getProjectIdByTestRunId(#request.getTestRunIds().stream().findAny().get()),'UPDATE')")
    @AuditAction(auditAction = "Set failure reason '{{#reqiest.failureReasonId}}' for provided test runs")
    public List<TestRun> setFailureReasonToTestRuns(@RequestBody TestRunsFailureReasonSetBulkRequest request) {
        return service.setFailureReasonToTestRuns(request);
    }


    /**
     * Update test runs with jira tickets keys.
     *
     * @param jiraTicketUpdateRequests - jira ticket update request
     */
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PutMapping(value = ApiPath.PROPAGATE_JIRA_TICKETS_PATH)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@testRunService.getProjectIdByTestRunId(#jiraTicketUpdateRequests.get(0).getTestRunId()),'UPDATE')")
    public void updateTestRunsWithJiraTicket(@RequestBody List<JiraTicketUpdateRequest> jiraTicketUpdateRequests) {
        log.info("Request to update test runs with jira ticket key [jiraTicketUpdateRequests={}]",
                jiraTicketUpdateRequests);
        service.updateTestRunsWithJiraTickets(jiraTicketUpdateRequests);
    }

    /**
     * Bulk update test runs with testing status hard.
     *
     * @param testingStatusUpdateRequests - test run testing status update request
     */
    @ResponseStatus(HttpStatus.OK)
    @PutMapping(value = ApiPath.TESTING_STATUSES_PATH)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@testRunService.getProjectIdByTestRunId(#testingStatusUpdateRequests.get(0).getTestRunId()),'UPDATE')")
    public void updateTestRunsTestingStatus(@RequestBody List<TestingStatusUpdateRequest> testingStatusUpdateRequests) {
        log.info("Request to update test runs with testing status hard [testingStatusUpdateRequests={}]",
                testingStatusUpdateRequests);
        service.updateTestRunsTestingStatus(testingStatusUpdateRequests);
    }

    /**
     * Bulk update test runs with comment.
     */
    @PostMapping(value = "/comment")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@testRunService.getProjectIdByTestRunId(#request.getTestRunIds().get(0)), 'UPDATE')")
    @AuditAction(auditAction = "Bulk update provided test runs with comment")
    public void updateTestRunsWithComment(@RequestBody TestRunsCommentSetBulkRequest request) {
        service.setCommentToTestRuns(request);
    }

    /**
     * Search test runs by execution request id and labels.
     * @param searchRequest search request
     * @return founded test runs with ids and names
     */
    @PostMapping(value = "/labelspath/search", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@executionRequestService.getProjectIdByExecutionRequestId(#searchRequest.getExecutionRequestId()),"
            + "'READ')")
    @AuditAction(auditAction = "Get test runs by execution request id = {{#searchRequest.executionRequestId}} and "
            + "provided labels")
    public List<TestRunResponse> labelsPathSearch(@RequestBody LabelsPathSearchRequest searchRequest) {
        MdcUtils.put(MdcField.EXECUTION_REQUEST_ID.toString(), searchRequest.getExecutionRequestId());
        return service.labelsPathSearch(searchRequest);
    }

    /**
     * Bulk update test runs with final.
     */
    @PostMapping(value = "/setFinal")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@testRunService.getProjectIdByTestRunId(#request.getTestRunIds().get(0)), 'UPDATE')")
    @AuditAction(auditAction =
            "Bulk mark provided test runs as final for execution request '{{#request.executionRequestId}}'")
    public void updateTestRunsWithFinalStatus(@RequestBody SetBulkFinalTestRuns request) {
        MdcUtils.put(MdcField.EXECUTION_REQUEST_ID.toString(), request.getExecutionRequestId());
        service.setFinalTestRuns(request);
    }

    /**
     * Bulk update test runs with final.
     */
    @PostMapping(value = "/propagateDefectsToComments")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "#request.getProjectId(), 'UPDATE')")
    public TestRunDefectsPropagationResponse propagateDefectsToComments(@RequestBody
                                                                        TestRunDefectsPropagationRequest request) {
        return service.propagateDefectsToComments(request);
    }
}
