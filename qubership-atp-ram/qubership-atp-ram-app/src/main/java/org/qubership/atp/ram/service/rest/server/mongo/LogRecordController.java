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

import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.apache.commons.lang.StringUtils;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.atp.ram.config.ApiPath;
import org.qubership.atp.ram.dto.request.LogRecordQuantityMatchPatternRequest;
import org.qubership.atp.ram.dto.request.SearchContextVariablesRequest;
import org.qubership.atp.ram.dto.request.UpdateLogRecordContextVariablesRequest;
import org.qubership.atp.ram.dto.request.UpdateLogRecordExecutionStatusRequest;
import org.qubership.atp.ram.dto.request.UpdateLogRecordMessageParametersRequest;
import org.qubership.atp.ram.dto.response.ContextVariablesResponse;
import org.qubership.atp.ram.dto.response.IssueResponsesModel;
import org.qubership.atp.ram.dto.response.LocationInEditorResponse;
import org.qubership.atp.ram.dto.response.LogRecordFileContentResponse;
import org.qubership.atp.ram.dto.response.LogRecordQuantityResponse;
import org.qubership.atp.ram.dto.response.LogRecordResponse;
import org.qubership.atp.ram.dto.response.LogRecordScreenshotResponse;
import org.qubership.atp.ram.dto.response.MessageParameter;
import org.qubership.atp.ram.dto.response.PaginationResponse;
import org.qubership.atp.ram.dto.response.logrecord.LogRecordContentResponse;
import org.qubership.atp.ram.entities.ErrorMappingItem;
import org.qubership.atp.ram.enums.ContextVariablesActiveTab;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.model.FileData;
import org.qubership.atp.ram.models.BrowserConsoleLogsTable;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.ScriptConsoleLog;
import org.qubership.atp.ram.models.SsmMetricReports;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.models.logrecords.parts.ContextVariable;
import org.qubership.atp.ram.models.response.ScriptResponse;
import org.qubership.atp.ram.pojo.IssueFilteringParams;
import org.qubership.atp.ram.services.ExecutionRequestService;
import org.qubership.atp.ram.services.FileResponseEntityService;
import org.qubership.atp.ram.services.GridFsService;
import org.qubership.atp.ram.services.IssueService;
import org.qubership.atp.ram.services.LogRecordService;
import org.qubership.atp.ram.services.ScriptReportService;
import org.qubership.atp.ram.services.TestRunService;
import org.qubership.atp.ram.utils.SourceShot;
import org.qubership.atp.ram.utils.StepPath;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/logrecords")
public class LogRecordController /*implements LogRecordControllerApi*/ {

    private final LogRecordService logRecordService;
    private final GridFsService gridFsService;
    private final ExecutionRequestService executionRequestService;
    private final TestRunService testRunService;
    private final IssueService issueService;
    private final FileResponseEntityService fileResponseEntityService;
    private final ScriptReportService scriptReportService;

    @GetMapping(value = "/{uuid}")
    @PreAuthorize("@entityAccess.checkAccess(@logRecordService.getProjectIdByLogRecordId(#uuid),'READ')")
    @AuditAction(auditAction = "Get log record by id = {{#uuid}}")
    public LogRecord getByUuid(@PathVariable("uuid") UUID uuid) {
        return logRecordService.findById(uuid);
    }

    /**
     * Get all children log records.
     *
     * @param uuid parent id of {@link LogRecord}
     * @return all children log records
     */
    @GetMapping(value = "/{uuid}/logrecords")
    @PreAuthorize("@entityAccess.checkAccess(@logRecordService.getProjectIdByLogRecordId(#uuid),'READ')")
    public List<LogRecord> getAllLogRecordsForLr(@PathVariable("uuid") UUID uuid) {
        return logRecordService.getLogRecordChildren(uuid).collect(Collectors.toList());
    }


    /**
     * Get all children log records or parent log records.
     *
     * @param id parent id of {@link LogRecord}
     * @return all children log records or parent
     */
    @GetMapping(value = "/{uuid}/logrecordsOrParent")
    @PreAuthorize("@entityAccess.checkAccess(@logRecordService.getProjectIdByLogRecordId(#id),'READ')")
    @AuditAction(auditAction = "Get all children log records or parent log records for logRecordId = {{#id}}")
    public List<LogRecord> geChildLogRecordsOrParent(@PathVariable("uuid") UUID id) {
        return logRecordService.geChildLogRecordsOrParent(id);
    }

    /**
     * Get context variable of logrecord.
     *
     * @param logRecordId uuid of {@link LogRecord}
     * @param page        number of page with results
     * @param size        size of 1 page
     * @return all children log records
     */
    @GetMapping(value = "/{id}/contextVariables")
    @PreAuthorize("@entityAccess.checkAccess(@logRecordService.getProjectIdByLogRecordId(#logRecordId),'READ')")
    @AuditAction(auditAction = "Get all context variables for log record with id = {{#logRecordId}}")
    public ResponseEntity<Object> getContextVariables(@PathVariable("id") UUID logRecordId,
                                                      @RequestParam(required = false) Integer page,
                                                      @RequestParam(required = false) Integer size,
                                                      @RequestParam(required = false)
                                                              List<ContextVariablesActiveTab> activeTabs) {
        return new ResponseEntity<>(logRecordService.getContextVariables(logRecordId, page, size, activeTabs),
                HttpStatus.OK);
    }

    /**
     * Get all context variable of logrecord.
     *
     * @param logRecordId uuid of {@link LogRecord}
     * @return all children log records
     */
    @GetMapping(value = "/{id}/contextVariables/all")
    @PreAuthorize("@entityAccess.checkAccess(@logRecordService.getProjectIdByLogRecordId(#logRecordId),'READ')")
    public ResponseEntity<List<ContextVariable>> getAllContextVariables(@PathVariable("id") UUID logRecordId) {
        return new ResponseEntity<>(logRecordService.getAllContextVariables(logRecordId),
                HttpStatus.OK);
    }

    /**
     * Get all step context variable of logrecord.
     *
     * @param logRecordId id of {@link LogRecord}
     * @param page        number of page with results
     * @param size        size of 1 page
     * @return all children log records
     */
    @GetMapping(value = "/{id}/stepContextVariables")
    @PreAuthorize("@entityAccess.checkAccess(@logRecordService.getProjectIdByLogRecordId(#logRecordId),'READ')")
    @AuditAction(auditAction = "Get all step context variables for log record with id = {{#logRecordId}}")
    public ResponseEntity<Object> getStepContextVariables(@PathVariable("id") UUID logRecordId,
                                                          @RequestParam(required = false) Integer page,
                                                          @RequestParam(required = false) Integer size,
                                                          @RequestParam(required = false)
                                                                  List<ContextVariablesActiveTab> activeTabs) {
        return new ResponseEntity<>(logRecordService.getStepContextVariables(logRecordId, page, size, activeTabs),
                HttpStatus.OK);
    }

    /**
     * Get all context variable of logrecord.
     *
     * @param logRecordId id of {@link LogRecord}
     * @return list of context variables
     */
    @PostMapping(value = "/{id}/contextVariables/search")
    @PreAuthorize("@entityAccess.checkAccess(@logRecordService.getProjectIdByLogRecordId(#logRecordId),'READ')")
    @AuditAction(auditAction = "Get all context variables for log record with id = {{#logRecordId}} and filter "
            + "from request body")
    public ContextVariablesResponse searchContextVariables(@PathVariable("id") UUID logRecordId,
                                                           @RequestBody SearchContextVariablesRequest request) {
        return logRecordService.filterContextVariables(
                logRecordId,
                request.getParameters(),
                request.getBeforeValue(),
                request.getAfterValue());
    }

    /**
     * Get all step context variable of logrecord.
     *
     * @param logRecordId id of {@link LogRecord}
     * @return list of context variables
     */
    @PostMapping(value = "/{id}/stepContextVariables/search")
    @PreAuthorize("@entityAccess.checkAccess(@logRecordService.getProjectIdByLogRecordId(#logRecordId),'READ')")
    @AuditAction(auditAction = "Get all step context variables for log record with id = {{#logRecordId}} and filter "
            + "from request body")
    public ContextVariablesResponse searchStepContextVariables(@PathVariable("id") UUID logRecordId,
                                                               @RequestBody SearchContextVariablesRequest request) {
        return logRecordService.filterStepContextVariables(
                logRecordId,
                request.getParameters(),
                request.getBeforeValue(),
                request.getAfterValue());
    }

    /**
     * Get all step context variable parameters of logrecord.
     *
     * @param logRecordId id of {@link LogRecord}
     * @return list of parameters
     */
    @GetMapping(value = "/{id}/contextVariables/parameters")
    @PreAuthorize("@entityAccess.checkAccess(@logRecordService.getProjectIdByLogRecordId(#logRecordId),'READ')")
    @AuditAction(auditAction = "Search context variable parameters for log record '{{#logRecordId}}' "
            + "by name = {{#name}}")
    public List<String> searchContextVariableParameters(@PathVariable("id") UUID logRecordId,
                                                        @RequestParam String name) {
        return logRecordService.searchContextVariableParameters(logRecordId, name);
    }

    /**
     * Get all step context variable parameters of logrecord.
     *
     * @param logRecordId id of {@link LogRecord}
     * @return list of parameters
     */
    @GetMapping(value = "/{id}/stepContextVariables/parameters")
    @PreAuthorize("@entityAccess.checkAccess(@logRecordService.getProjectIdByLogRecordId(#logRecordId),'READ')")
    @AuditAction(auditAction = "Search step context variable parameters for log record '{{#logRecordId}}' "
            + "by name = {{#name}}")
    public List<String> searchStepContextVariableParameters(@PathVariable("id") UUID logRecordId,
                                                            @RequestParam String name) {
        return logRecordService.searchStepContextVariableParameters(logRecordId, name);
    }

    @DeleteMapping(value = "/delete/{id}")
    @PreAuthorize("@entityAccess.checkAccess(@logRecordService.getProjectIdByLogRecordId(#id),'DELETE')")
    public void delete(@PathVariable("id") UUID id) {
        logRecordService.delete(id);
    }

    /**
     * Get screenshot/snapshot by LogRecordUuid from GridFS.
     */
    @GetMapping(value = "/screenshot/{logRecordUuid}")
    @PreAuthorize("@entityAccess.checkAccess(@logRecordService.getProjectIdByLogRecordId(#logRecordUuid),'READ')")
    @AuditAction(auditAction = "Get screenshot/snapshot by logRecordUuid = {{#logRecordUuid}} from GridFS")
    public SourceShot getScreenShot(@PathVariable("logRecordUuid") UUID logRecordUuid) {
        return gridFsService.getScreenShot(logRecordUuid);
    }

    /**
     * Get BrowserConsoleLogs by logRecordUUID.
     *
     * @param logRecordUuid {@link LogRecord}
     * @param pageable size (default 5), page (starts from 0), sort (default by timestamp,desc)
     * @return list of logs on page as entities and totalCount
     */
    @GetMapping(value = "/{logRecordUuid}/browserConsoleLog")
    @PreAuthorize("@entityAccess.checkAccess(@logRecordService.getProjectIdByLogRecordId(#logRecordUuid),'READ')")
    @AuditAction(auditAction = "Get BrowserConsoleLogs by logRecordId = {{#logRecordUuid}}")
    public PaginationResponse<BrowserConsoleLogsTable> getBrowserConsoleLogWithPagination(
            @PathVariable("logRecordUuid") UUID logRecordUuid,
            @PageableDefault(sort = {"timestamp"}, size = 5, direction = Sort.Direction.DESC) Pageable pageable) {
        return logRecordService.getBrowserConsoleLogsTable(logRecordUuid, pageable);
    }

    /**
     * Create BrowserConsoleLog.
     *
     * @param logRecordUuid LogRecordId
     * @param logs logs to save
     */
    @PostMapping(value = "/{logRecordUuid}/createBrowserConsoleLog")
    @PreAuthorize("@entityAccess.checkAccess(@logRecordService.getProjectIdByLogRecordId(#logRecordUuid),'CREATE')")
    public void createBrowserConsoleLog(@PathVariable("logRecordUuid") UUID logRecordUuid,
                                           @RequestBody List<BrowserConsoleLogsTable> logs) {
        logRecordService.createBrowserConsoleLog(logRecordUuid, logs);
    }

    /**
     * Add AkbRecord to LogRecord by Uuid.
     */
    @PutMapping(value = "/{logRecordUuid}/addAkb/{akbRecordUuid}", produces = TEXT_PLAIN_VALUE)
    @PreAuthorize("@entityAccess.checkAccess(@logRecordService.getProjectIdByLogRecordId(#logRecordUuid),'UPDATE')")
    public String addLogRecordAkb(@PathVariable("logRecordUuid") UUID logRecordUuid,
                                  @PathVariable("akbRecordUuid") UUID akbRecordUuid) {
        return logRecordService.addLogRecordAkb(logRecordUuid, akbRecordUuid);
    }

    /**
     * Add some AkbRecords to LogRecord by Uuid.
     */
    @PutMapping(value = "/{lr}/akb")
    @PreAuthorize("@entityAccess.checkAccess(@logRecordService.getProjectIdByLogRecordId(#logRecordUuid),'UPDATE')")
    @AuditAction(auditAction = "Put AkbRecords to log record with id = {{#logRecordUuid}}")
    public LogRecord addLogRecordAkbRecords(@PathVariable("lr") UUID logRecordUuid,
                                            @RequestBody List<UUID> akbRecordsUuid) {
        return logRecordService.addLogRecordAkbRecords(logRecordUuid, akbRecordsUuid);
    }

    /**
     * Return mapping of LogRecordId against LogRecordStatus.
     * [ {id:uuidId, level:PASSED}, {id:uuidId, level:FAILED}]
     *
     * @param parentUuid id of {@link TestRun} or
     *                   {@link ExecutionRequest}
     * @return list of {@link ErrorMappingItem}
     */
    @GetMapping(value = "/{uuid}/error/mapping")
    @PreAuthorize("@entityAccess.checkAccess(@logRecordService.getProjectIdByParentId(#parentUuid, #source),'READ')")
    public List<ErrorMappingItem> getErrorMapping(
            @PathVariable("uuid") UUID parentUuid, @RequestParam("source") String source) {
        return logRecordService.getErrorMapping(executionRequestService, source, parentUuid);
    }

    @GetMapping(value = "/{uuid}/tree/path")
    @PreAuthorize("@entityAccess.checkAccess(@logRecordService.getProjectIdByLogRecordId(#uuid),'READ')")
    @AuditAction(auditAction = "Get tree path for log record with id = {{#uuid}}")
    public StepPath getLogRecordPath(@PathVariable("uuid") UUID uuid) {
        return logRecordService.getLogRecordPath(uuid);
    }

    /**
     * Change testing status of Log record.
     *
     * @param uuid            of LR
     * @param testingStatuses is new testing status
     * @return updated {@link LogRecord}
     */
    @PutMapping(value = "/{uuid}/updTestingStatus/{testingStatus}")
    @PreAuthorize("@entityAccess.checkAccess(@logRecordService.getProjectIdByLogRecordId(#uuid),'UPDATE')")
    public LogRecord updTestingStatus(@PathVariable("uuid") UUID uuid,
                                      @PathVariable("testingStatus") TestingStatuses testingStatuses) {
        LogRecord logRecord = getByUuid(uuid);
        logRecord.setTestingStatus(testingStatuses);
        return logRecordService.save(logRecord);
    }

    /**
     * Revert testing status for log record log record.
     *
     * @param logRecordId the log record id
     * @return the log record
     */
    @PutMapping(value = "/{id}/revertTestingStatus",
    produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@entityAccess.checkAccess(@logRecordService.getProjectIdByLogRecordId(#logRecordId),'UPDATE')")
    public ResponseEntity<LogRecord> revertTestingStatusForLogRecord(@PathVariable("id") UUID logRecordId) {
        LogRecord logRecord = testRunService.revertTestingStatusForLogRecord(logRecordId);
        return ResponseEntity.ok(logRecord);
    }

    /**
     * Change testing status of Log record.
     *
     * @param uuid            of LR
     * @param testingStatuses is new testing status
     * @return updated {@link LogRecord}
     */
    @PutMapping(value = "/{uuid}/updTestingStatus/{testingStatus}/hard")
    @PreAuthorize("@entityAccess.checkAccess(@logRecordService.getProjectIdByLogRecordId(#uuid),'UPDATE')")
    @AuditAction(auditAction = "Set testing status \"{{#testingStatuses.name}}\" for log record {{#uuid}}")
    public LogRecord updTestingStatusHard(@PathVariable("uuid") UUID uuid,
                                          @PathVariable("testingStatus") TestingStatuses testingStatuses) {
        LogRecord logRecord = getByUuid(uuid);
        logRecord.setTestingStatusHard(testingStatuses);
        return logRecordService.save(logRecord);
    }

    /**
     * Update log record execution status.
     *
     * @param logRecordId log record id
     * @param request     update request
     */
    @PutMapping(value = "/{id}/updateExecutionStatus")
    @PreAuthorize("@entityAccess.checkAccess(@logRecordService.getProjectIdByLogRecordId(#logRecordId),'UPDATE')")
    public void updateExecutionStatus(@PathVariable("id") UUID logRecordId,
                                      @RequestBody UpdateLogRecordExecutionStatusRequest request) {
        logRecordService.updateExecutionStatus(logRecordId, request);
    }

    /**
     * Update log record step context variables.
     *
     * @param logRecordId log record id
     * @param request     update request
     */
    @PostMapping(value = "/{id}/updateStepContextVariables")
    @PreAuthorize("@entityAccess.checkAccess(@logRecordService.getProjectIdByLogRecordId(#logRecordId),'UPDATE')")
    public void updateStepContextVariables(@PathVariable("id") UUID logRecordId,
                                           @RequestBody UpdateLogRecordContextVariablesRequest request) {
        logRecordService.updateStepContextVariables(logRecordId, request);
    }

    /**
     * Update log record message parameters.
     *
     * @param logRecordId log record id
     * @param request     update request
     */
    @PostMapping(value = "/{id}/updateMessageParameters")
    @PreAuthorize("@entityAccess.checkAccess(@logRecordService.getProjectIdByLogRecordId(#logRecordId),'UPDATE')")
    public void updateMessageParameters(@PathVariable("id") UUID logRecordId,
                                        @RequestBody UpdateLogRecordMessageParametersRequest request) {
        logRecordService.updateMessageParameters(logRecordId, request);
    }

    /**
     * Update log record context variables.
     *
     * @param logRecordId log record id
     * @param request     update request
     */
    @PostMapping(value = "/{id}/updateContextVariables")
    @PreAuthorize("@entityAccess.checkAccess(@logRecordService.getProjectIdByLogRecordId(#logRecordId),'UPDATE')")
    public void updateContextVariables(@PathVariable("id") UUID logRecordId,
                                       @RequestBody UpdateLogRecordContextVariablesRequest request) {
        logRecordService.updateContextVariables(logRecordId, request);
    }

    /**
     * Get all children log records and parent log records.
     *
     * @param uuid parent id of {@link LogRecord}
     * @return all children log records or parent
     */
    @Deprecated
    @GetMapping(value = "/{uuid}/logrecordsAndParent")
    @PreAuthorize("@entityAccess.checkAccess(@logRecordService.getProjectIdByLogRecordId(#uuid),'READ')")
    public List<LogRecord> getChildLogRecordsAndParent(@PathVariable("uuid") UUID uuid) {
        LogRecord parent = getByUuid(uuid);
        List<LogRecord> result = logRecordService.getLogRecordChildren(uuid).collect(Collectors.toList());
        result.add(0, parent);
        return result;
    }

    /**
     * Create log record.
     *
     * @param logRecord log record for creating
     * @return created log record
     * @deprecated use LogRecordLoggingController
     */
    @Deprecated
    @PostMapping(value = "/create")
    @PreAuthorize("@entityAccess.checkAccess(@testRunService.getProjectIdByTestRunId(#logRecord.testRunId),'CREATE')")
    public ResponseEntity create(@RequestBody LogRecord logRecord) {
        return new ResponseEntity<>(logRecordService.save(logRecord), HttpStatus.CREATED);
    }

    /**
     * Find current log record and return info about this with list of children, if exists.
     *
     * @param uuid for found log record
     * @return current log record with list of children if exists
     */
    @GetMapping(value = ApiPath.UUID_PATH + ApiPath.LOG_RECORD_WITH_CHILDREN_PATH)
    @PreAuthorize("@entityAccess.checkAccess(@logRecordService.getProjectIdByLogRecordId(#uuid),'READ')")
    public LogRecordResponse getLogRecordByIdWithChildren(@PathVariable(ApiPath.UUID) UUID uuid) {
        return logRecordService.getLogRecordByIdWithChildren(uuid);
    }

    @GetMapping(value = "/{id}/location-in-editor")
    @PreAuthorize("@entityAccess.checkAccess(@logRecordService.getProjectIdByLogRecordId(#id),'READ')")
    @AuditAction(auditAction = "Get location in editor for log record with id = {{#id}}")
    public LocationInEditorResponse getLocationInEditor(@PathVariable("id") UUID id) {
        return logRecordService.getLocationInEditor(id);
    }

    /**
     * Return common information of log record for any type.
     */
    @GetMapping(value = ApiPath.UUID_PATH + "/content")
    @PreAuthorize("@entityAccess.checkAccess(@logRecordService.getProjectIdByLogRecordId(#id),'READ')")
    public LogRecordContentResponse getLogRecordContent(@PathVariable(ApiPath.UUID) UUID id) {
        return logRecordService.getLogRecordContent(id);
    }

    /**
     * Get all log record children previews.
     *
     * @param logRecordId log record id
     * @return list of previews
     */
    @GetMapping(value = "/{id}/screenshot/previews")
    @PreAuthorize("@entityAccess.checkAccess(@logRecordService.getProjectIdByLogRecordId(#logRecordId),'READ')")
    @AuditAction(auditAction = "Get all screenshots previews for log record with id = {{#logRecordId}}")
    public LogRecordScreenshotResponse getAllLogRecordScreenshotPreviews(@PathVariable("id") UUID logRecordId) {
        return logRecordService.getAllLogRecordScreenshotPreviews(logRecordId);
    }

    /**
     * Get all issues in a log record.
     *
     * @param logRecordId log record id
     * @return list of issues
     */
    @GetMapping(value = "/{id}/issues")
    @PreAuthorize("@entityAccess.checkAccess(@executionRequestService.getProjectIdByExecutionRequestId("
            + "#issueFilteringParams.getExecutionRequestId()),'READ')")
    @AuditAction(auditAction = "Get all issues in log record '{{#logRecordId}}'")
    public IssueResponsesModel getAllIssuesByLogRecordId(@PathVariable("id") UUID logRecordId,
                                                         @RequestParam("startIndex") int startIndex,
                                                         @RequestParam("endIndex") int endIndex,
                                                         @RequestParam(required = false) String columnType,
                                                         @RequestParam(required = false) String sortType,
                                                         IssueFilteringParams issueFilteringParams) {
        issueFilteringParams.setLogRecordIds(Collections.singletonList(logRecordId));
        return issueService.getAllIssuesByLogRecordId(issueFilteringParams,
                startIndex, endIndex,
                columnType, sortType);
    }

    /**
     * Get the statistics for new REGEXP in scope of the execution.
     *
     * @param request object which contains information for calculation.
     *                {@link LogRecordQuantityMatchPatternRequest}
     * @return quantity of all Log Records which match to current and new patterns.
     */
    @ApiResponses(value = {
            @ApiResponse(responseCode  = "200", description = "OK"),
            @ApiResponse(responseCode  = "401", description = "Unauthorized"),
            @ApiResponse(responseCode  = "403", description = "Forbidden")
    })
    @RequestMapping(value = "/getQuantity/matchPattern",
            method = RequestMethod.POST)
    @PreAuthorize("@entityAccess.checkAccess(@executionRequestService.getProjectIdByExecutionRequestId("
            + "#request.getExecutionRequestId()),'READ')")
    @AuditAction(auditAction = "Get statistics for new REGEXP in scope of execution request "
            + "'{{#request.executionRequestId}}'")
    public LogRecordQuantityResponse getQuantityLogRecordsWhichMatchToPatterns(
            @Parameter(name = "request", required = true)
            @Valid @RequestBody LogRecordQuantityMatchPatternRequest request) {

        return logRecordService.getQuantityLogRecordsWhichMatchToPatterns(request);
    }

    /**
     * Get log record message parameters.
     *
     * @param id log record identifier
     * @return message parameters
     */
    @GetMapping(value = "/{id}/messageParameters")
    @PreAuthorize("@entityAccess.checkAccess(@logRecordService.getProjectIdByLogRecordId(#id),'READ')")
    @AuditAction(auditAction = "Get message parameters for log record with id = {{#id}}")
    public List<MessageParameter> getLogRecordMessageParameters(@PathVariable UUID id) {
        return logRecordService.getLogRecordMessageParameters(id);
    }

    /**
     * Download pot file by logRecordId.
     *
     * @param logRecordId logRecordId
     * @return file
     * @deprecated use downloadFileByLogRecordId
     */
    @Deprecated
    @GetMapping(value = "/pot/{logRecordId}")
    @PreAuthorize("@entityAccess.checkAccess(@logRecordService.getProjectIdByLogRecordId(#logRecordId),'READ')")
    public ResponseEntity<Resource> downloadPotFileByLogRecordId(@PathVariable("logRecordId") UUID logRecordId) {
        FileData fileData = gridFsService.downloadFile(logRecordId);
        return fileResponseEntityService.buildOctetStreamResponseEntity(fileData);
    }

    /**
     * Download file by logRecordId.
     *
     * @param logRecordId logRecordId
     * @return file
     */
    @GetMapping(value = "/file/{logRecordId}")
    @PreAuthorize("@entityAccess.checkAccess(@logRecordService.getProjectIdByLogRecordId(#logRecordId),'READ')")
    @AuditAction(auditAction = "Generate file '{{#filename}}' download link for log record '{{#logRecordId}}'")
    public ResponseEntity<Resource> downloadFileByLogRecordId(@PathVariable("logRecordId") UUID logRecordId,
                                                              @RequestParam(value = "filename", required = false)
                                                              String filename) {
        FileData fileData;
        if (StringUtils.isNotEmpty(filename)) {
            fileData = gridFsService.downloadFileByName(logRecordId, filename);
        } else {
            log.warn("File name is empty. The file will be found by Log Record id {}", logRecordId);
            fileData = gridFsService.downloadFile(logRecordId);
        }
        return fileResponseEntityService.buildOctetStreamResponseEntity(fileData);
    }

    /**
     * Download file as string by logRecordId and filename (optional parameter).
     *
     * @param logRecordId logRecordId
     * @param filename filename
     * @return file as string
     */
    @GetMapping(value = "/file/{logRecordId}/downloadAsString")
    @PreAuthorize("@entityAccess.checkAccess(@logRecordService.getProjectIdByLogRecordId(#logRecordId),'READ')")
    @AuditAction(auditAction = "Get file '{{#filename}}' as string for log record '{{#logRecordId}}'")
    public ResponseEntity<LogRecordFileContentResponse> downloadFileAsStringByLogRecordIdAndFilename(
            @PathVariable("logRecordId") UUID logRecordId,
            @RequestParam(value = "filename", required = false) String filename) {
        LogRecordFileContentResponse fileContent = new LogRecordFileContentResponse();
        if (StringUtils.isNotEmpty(filename)) {
            fileContent.setContent(gridFsService.downloadFileIntoStringByName(logRecordId, filename));
        } else {
            log.warn("File name is empty. The file will be found by Log Record id {}", logRecordId);
            fileContent.setContent(gridFsService.downloadFileIntoString(logRecordId));
        }
        return ResponseEntity.ok(fileContent);
    }

    /**
     * Upload SSM metrics report.
     *
     * @param file file content
     * @param fileName file name
     * @param type file type
     * @param contentType file content type
     * @param logRecordId log record identifier
     * @return uploaded report file identifier
     * @throws IOException possible IO exception
     */
    @PostMapping(value = "/{id}/ssmMetricsReport", consumes = { "application/octet-stream" })
    public ResponseEntity<UUID> uploadSsmMetricsReport(@PathVariable("id") UUID logRecordId,
                                                       @RequestBody Resource file,
                                                       @RequestParam String fileName,
                                                       @RequestParam(required = false) String type,
                                                       @RequestParam(required = false) String contentType)
            throws IOException {
        return ResponseEntity.ok(gridFsService.saveSsmMetricReport(fileName, type, contentType, file.getInputStream(),
                null, logRecordId));
    }

    /**
     * Update SSM metric reports data.
     *
     * @param logRecordId log record identifier
     * @param data SSM metric reports data
     */
    @PatchMapping("/{id}/ssmMetricsReports")
    public void updateSsmMetricReportsData(@PathVariable("id") UUID logRecordId,
                                           @RequestBody SsmMetricReports data) {
        logRecordService.updateSsmMetricReportsData(logRecordId, data);
    }

    /**
     * Download file by logRecordId.
     *
     * @param logRecordId logRecordId
     * @return file
     */
    @GetMapping(value = "/{logRecordId}/script/preScript")
    @PreAuthorize("@entityAccess.checkAccess(@logRecordService.getProjectIdByLogRecordId(#logRecordId),'READ')")
    public ResponseEntity<ScriptResponse> getPreScriptByLogRecordUuid(@PathVariable("logRecordId") UUID logRecordId) {
        return ResponseEntity.ok(new ScriptResponse(scriptReportService.getPreScript(logRecordId)));
    }

    /**
     * Download file by logRecordId.
     *
     * @param logRecordId logRecordId
     * @return file
     */
    @GetMapping(value = "/{logRecordId}/script/postScript")
    @PreAuthorize("@entityAccess.checkAccess(@logRecordService.getProjectIdByLogRecordId(#logRecordId),'READ')")
    public ResponseEntity<ScriptResponse> getPostScriptByLogRecordUuid(@PathVariable("logRecordId") UUID logRecordId) {
        return ResponseEntity.ok(new ScriptResponse(scriptReportService.getPostScript(logRecordId)));
    }

    /**
     * Download file by logRecordId.
     *
     * @param logRecordId logRecordId
     * @return file
     */
    @GetMapping(value = "/{logRecordId}/script/consoleLogs")
    @PreAuthorize("@entityAccess.checkAccess(@logRecordService.getProjectIdByLogRecordId(#logRecordId),'READ')")
    public ResponseEntity<List<ScriptConsoleLog>> getScriptConsoleLogsByLogRecordUuid(
            @PathVariable("logRecordId") UUID logRecordId) {
        return ResponseEntity.ok(scriptReportService.getScriptConsoleLogs(logRecordId));
    }
}
