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

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.atp.ram.config.ApiPath;
import org.qubership.atp.ram.entities.treenodes.TreeNode;
import org.qubership.atp.ram.model.ArchiveData;
import org.qubership.atp.ram.model.LogRecordFilteringRequest;
import org.qubership.atp.ram.models.PotsStatisticsPerAction;
import org.qubership.atp.ram.models.PotsStatisticsPerTestCase;
import org.qubership.atp.ram.services.FileResponseEntityService;
import org.qubership.atp.ram.services.PotService;
import org.qubership.atp.ram.services.TestRunService;
import org.qubership.atp.ram.services.TreeNodeService;
import org.qubership.atp.ram.utils.Utils;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@CrossOrigin
@RequestMapping("/api/tree")
@RestController()
@RequiredArgsConstructor
@Slf4j
public class TreeController /*implements TreeControllerApi*/ {

    private final TreeNodeService treeNodeService;
    private final ObjectMapper objectMapper;
    private final PotService potService;
    private final FileResponseEntityService fileResponseEntityService;
    @Lazy
    private final TestRunService testRunService;

    /**
     * Get LogRecords Tree for parent LR.
     *
     * @param logRecordUuid log record id
     * @param statuses      statuses for filters
     * @param types         statuses for filters
     * @param fields        statuses for filters
     * @return Tree LRs for parent LR
     * @throws JsonProcessingException If Json processing failed
     */
    @GetMapping(value = "/{logRecordUuid}/logRecordChildNodes", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@entityAccess.checkAccess("
            + "@logRecordService.getProjectIdByLogRecordId(#logRecordUuid),'READ')")
    @AuditAction(auditAction = "Get filtered log record child nodes tree for log record '{{#logRecordUuid}}'")
    public String getLogRecordNodesByParentLogRecord(
            @PathVariable("logRecordUuid") UUID logRecordUuid,
            @RequestParam(value = "statuses", required = false) List<String> statuses,
            @RequestParam(value = "types", required = false) List<String> types,
            @RequestParam(value = "showNotAnalyzedItemsOnly", required = false) boolean showNotAnalyzedItemsOnly,
            @RequestParam(value = "fields", required = false) String[] fields) throws JsonProcessingException {
        LogRecordFilteringRequest filteringRequest =
                new LogRecordFilteringRequest(statuses, types, showNotAnalyzedItemsOnly);
        TreeNode resultTree = treeNodeService.getLogRecordsTreeForLogRecordParent(logRecordUuid, filteringRequest);
        return Utils.filterAllExceptFields(resultTree, fields, objectMapper, TreeNode.TREE_NODE_JSON_FILTER_NAME);
    }

    @GetMapping(value = "/executionrequests" + ApiPath.EXECUTION_REQUEST_ID_PATH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "@treeNodeService.getProjectIdByExecutionRequestId(#executionRequestId),'READ')")
    @AuditAction(auditAction = "Get filtered tree node for execution request '{{#executionRequestId}}'")
    public String getExecutionRequestTreeNode(
            @PathVariable(ApiPath.EXECUTION_REQUEST_ID) UUID executionRequestId,
            @RequestParam(value = "labelTemplateId", required = false) UUID labelTemplateId,
            @RequestParam(value = "fields", required = false) String[] fields,
            @RequestParam(value = "includeAll", required = false, defaultValue = "false") boolean includeAll
    ) throws JsonProcessingException {
        TreeNode resultTree = treeNodeService.getExecutionRequestTree(executionRequestId, labelTemplateId, includeAll);
        return Utils.filterAllExceptFields(resultTree, fields, objectMapper, TreeNode.TREE_NODE_JSON_FILTER_NAME);
    }

    /**
     * Get execution request widget test runs tree.
     *
     * @param executionRequestId execution request id
     * @param widgetId           widget id
     * @param labelTemplateId    label template id.
     * @param validationTemplateId validation template id
     * @param fields             fields filter
     * @param skipOverride skip override label template id
     * @return result tree
     * @throws JsonProcessingException possible json process exception
     */
    @GetMapping(value = "/executionrequests/{executionRequestId}/widgets/{widgetId}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "@treeNodeService.getProjectIdByExecutionRequestId(#executionRequestId),'READ')")
    public String getExecutionRequestTreeNode(
            @PathVariable("executionRequestId") UUID executionRequestId,
            @PathVariable("widgetId") UUID widgetId,
            @RequestParam(value = "labelTemplateId", required = false) UUID labelTemplateId,
            @RequestParam(value = "validationTemplateId", required = false) UUID validationTemplateId,
            @RequestParam(value = "fields", required = false) String[] fields,
            @RequestParam(value = "skipOverride", required = false) boolean skipOverride,
            @RequestParam(value = "refresh", required = false) boolean refresh
    ) throws JsonProcessingException {
        Long countLr = testRunService.getCountLrsForCurrentEr(executionRequestId);
        return treeNodeService.getSerializableExecutionRequestWidgetTree(
                executionRequestId, widgetId, labelTemplateId, validationTemplateId, skipOverride, fields,
                countLr, refresh);
    }

    @Deprecated
    @GetMapping(value = "/executionrequests/{executionRequestId}/testruns", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@treeNodeService.getProjectIdByExecutionRequestId(#executionRequestId),'READ')")
    public String getExecutionRequestTestRunTree(
            @PathVariable("executionRequestId") UUID executionRequestId,
            @RequestParam(value = "fields", required = false) String[] fields) throws JsonProcessingException {
        TreeNode resultTree = treeNodeService.getExecutionRequestTestRunTree(executionRequestId);
        return Utils.filterAllExceptFields(resultTree, fields, objectMapper, TreeNode.TREE_NODE_JSON_FILTER_NAME);
    }

    /**
     * Get LogRecords Tree.
     *
     * @param testRunId Test Run ID
     * @param statuses  Statuses
     * @param types     Types
     * @param fields    Fields
     * @return Tree
     * @throws JsonProcessingException If Json processing failed
     */
    @GetMapping(value = "/testruns/{testRunId}/logrecords", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@treeNodeService.getProjectIdByTestRunId(#testRunId),'READ')")
    @AuditAction(auditAction = "Get filtered log records tree for test run '{{#testRunId}}'")
    public String getExecutionRequestTestRunLogRecordsTree(
            @PathVariable("testRunId") UUID testRunId,
            @RequestParam(value = "statuses", required = false) List<String> statuses,
            @RequestParam(value = "types", required = false) List<String> types,
            @RequestParam(value = "showNotAnalyzedItemsOnly", required = false) boolean showNotAnalyzedItemsOnly,
            @RequestParam(value = "fields", required = false) String[] fields) throws JsonProcessingException {
        LogRecordFilteringRequest filteringRequest =
                new LogRecordFilteringRequest(statuses, types, showNotAnalyzedItemsOnly);
        TreeNode resultTree = treeNodeService.getExecutionRequestTestRunLogRecordsTree(testRunId, filteringRequest);
        return Utils.filterAllExceptFields(resultTree, fields, objectMapper, TreeNode.TREE_NODE_JSON_FILTER_NAME);
    }

    @GetMapping(value = "/executionrequests" + ApiPath.EXECUTION_REQUEST_ID_PATH + "/search/{nodeName}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "@treeNodeService.getProjectIdByExecutionRequestId(#executionRequestId),'READ')")
    @AuditAction(auditAction = "Get execution request tree node for execution request '{{#executionRequestId}}' by "
            + "node name = {{#nodeName}}")
    public String searchExecutionRequestTreeNode(
            @PathVariable(ApiPath.EXECUTION_REQUEST_ID) UUID executionRequestId,
            @PathVariable("nodeName") String nodeName,
            @RequestParam(value = "fields", required = false) String[] fields
    ) throws JsonProcessingException {
        Set<TreeNode> resultTree = treeNodeService.getExecutionRequestTreeNodesByName(executionRequestId, nodeName);
        return Utils.filterAllExceptFields(resultTree, fields, objectMapper, TreeNode.TREE_NODE_JSON_FILTER_NAME);
    }


    /**
     * Get pot files statistics for execution request.
     *
     * @param executionRequestId execution request identifier
     * @return list of statistics per test cases
     */
    @GetMapping(value = "/executionrequests/{executionRequestId}/potfiles")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "@treeNodeService.getProjectIdByExecutionRequestId(#executionRequestId),'READ')")
    @AuditAction(auditAction = "Get pot files statistics for execution request '{{#executionRequestId}}'")
    public ResponseEntity<List<PotsStatisticsPerTestCase>> getPotStatistics(
            @PathVariable("executionRequestId") UUID executionRequestId) {
        log.info("Request to get POTs statistic for run execution request with id = '{}'", executionRequestId);
        return ResponseEntity.ok(potService.collectStatisticForExecutionRequest(executionRequestId));
    }

    /**
     * Download archive with pots files in execution request with specified id.
     *
     * @param executionRequestId executionRequestId
     * @return file
     */
    @GetMapping(value = "/executionrequests/{executionRequestId}/potfilesArchive")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).EXECUTION_REQUEST.getName(),"
            + "@treeNodeService.getProjectIdByExecutionRequestId(#executionRequestId),'READ')")
    @AuditAction(auditAction = "Download archive with pots files in "
            + "execution request with id = {{#executionRequestId}}")
    public ResponseEntity<Resource> downloadArchiveWithPotFilesInExecutionRequest(
            @PathVariable("executionRequestId") UUID executionRequestId) {
        log.info("Request to download archive with pots files in execution request with id = '{}'", executionRequestId);
        ArchiveData archiveData = potService.getArchiveWithPotsFiles(executionRequestId);
        return fileResponseEntityService.buildOctetStreamResponseEntity(archiveData);
    }


    /**
     * Get pot files statistics for execution request.
     *
     * @param executionRequestId execution request identifier
     * @return list of statistics per test cases
     */
    @GetMapping(value = "/executionrequests/{executionRequestId}/potfiles/{testRunId}")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).TEST_RUN.getName(),"
            + "@treeNodeService.getProjectIdByExecutionRequestId(#executionRequestId),'READ')")
    @AuditAction(auditAction = "Get pot files statistics for test run '{{#testRunId}}' under execution request "
            + "'{{#executionRequestId}}'")
    public ResponseEntity<List<PotsStatisticsPerAction>> getPotStatisticsPerTestRun(
            @PathVariable("executionRequestId") UUID executionRequestId,
            @PathVariable("testRunId") UUID testRunId) {
        log.info("Request to get POTs statistic for run execution request with id = '{}', test run ID = '{}'",
                executionRequestId, testRunId);
        return ResponseEntity.ok(potService.collectStatisticForTestRun(testRunId));
    }
}
