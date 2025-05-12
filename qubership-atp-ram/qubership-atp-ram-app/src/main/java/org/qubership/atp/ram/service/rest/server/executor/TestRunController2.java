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

package org.qubership.atp.ram.service.rest.server.executor;

import static org.qubership.atp.ram.config.ApiPath.API_PATH;
import static org.qubership.atp.ram.config.ApiPath.BULK_PATH;
import static org.qubership.atp.ram.config.ApiPath.CREATE_PATH;
import static org.qubership.atp.ram.config.ApiPath.DELAYED_PATH;
import static org.qubership.atp.ram.config.ApiPath.EXECUTOR_PATH;
import static org.qubership.atp.ram.config.ApiPath.FINISH_PATH;
import static org.qubership.atp.ram.config.ApiPath.PATCH_PATH;
import static org.qubership.atp.ram.config.ApiPath.STOP_PATH;
import static org.qubership.atp.ram.config.ApiPath.STOP_TEST_RUNS_PATH;
import static org.qubership.atp.ram.config.ApiPath.TEST_RUNS_PATH;
import static org.qubership.atp.ram.config.ApiPath.UPDATE_OR_CREATE_PATH;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang.StringUtils;
import org.qubership.atp.ram.enums.RootCauseEnum;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.Project;
import org.qubership.atp.ram.models.TestPlan;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.service.rest.server.executor.request.StartRunRequest;
import org.qubership.atp.ram.service.rest.server.executor.request.StartRunResponse;
import org.qubership.atp.ram.services.GridFsService;
import org.qubership.atp.ram.services.IssueService;
import org.qubership.atp.ram.services.TestRunService;
import org.qubership.atp.ram.utils.JsonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;

/**
 * CRUD Test run.
 *
 * @deprecated use TestRunLoggingController instead of this.
 */
@SuppressWarnings("PMD")
@Deprecated
@RestController("ExecutorTestRunController")
@RequestMapping(API_PATH + EXECUTOR_PATH + TEST_RUNS_PATH)
@Validated
public class TestRunController2 /*implements TestRunControllerApi*/ {

    private static final Logger log = LoggerFactory.getLogger(GridFsService.class);

    private final TestRunService testRunService;
    private final IssueService issueService;

    /**
     * Inject services.
     */
    @Autowired
    public TestRunController2(TestRunService testRunService, IssueService issueService) {
        this.testRunService = testRunService;
        this.issueService = issueService;
    }

    /**
     * Find existing or create new TestRun.
     *
     * @deprecated use method from TestRunLoggingController
     */
    @Deprecated
    @PostMapping(value = CREATE_PATH)
    public StartRunResponse create(@RequestBody StartRunRequest request) {
        log.info("Request to create ER: {}", request);
        TestRun newTestRun = getTestRunByRequest(request);
        ExecutionRequest newExecutionRequest = getExecutionRequestByRequest(request);
        TestPlan newTestPlan = getTestPlanByRequest(request);
        Project newProject = getProjectByRequest(request);

        TestRun testRun = testRunService.create(newProject, newTestPlan, newTestRun, newExecutionRequest);

        StartRunResponse response = new StartRunResponse();
        response.setTestRunId(testRun.getUuid());
        response.setExecutionRequestId(testRun.getExecutionRequestId());
        log.debug("Test Run: {} was created for ER: {}.", testRun.getUuid(), testRun.getExecutionRequestId());
        return response;
    }

    /**
     * For all testRuns with provided UUID's recalculates issues
     * and updates status.
     */
    @PutMapping(PATCH_PATH)
    public TestRun patch(@NotNull @RequestBody TestRun testRun) {
        log.info("Patching test run {}", testRun.getUuid());
        log.debug("Test run patch provided: {}", testRun);
        return testRunService.patch(testRun);
    }

    private Project getProjectByRequest(StartRunRequest request) {
        Project project = new Project();
        project.setName(request.getProjectName());
        project.setUuid(request.getProjectId());
        log.debug("Prepare project {}", project);
        return project;
    }

    private TestPlan getTestPlanByRequest(StartRunRequest request) {
        TestPlan testPlan = new TestPlan();
        testPlan.setName(request.getTestPlanName());
        testPlan.setProjectId(request.getProjectId());
        testPlan.setUuid(request.getTestPlanId());
        log.debug("Prepare TP {}", testPlan);
        return testPlan;
    }

    ExecutionRequest getExecutionRequestByRequest(StartRunRequest request) {
        ExecutionRequest executionRequest = new ExecutionRequest();
        executionRequest.setName(request.getExecutionRequestName());
        executionRequest.setProjectId(request.getProjectId());
        executionRequest.setTestPlanId(request.getTestPlanId());
        executionRequest.setLegacyMailRecipients(request.getMailList());
        executionRequest.setTestScopeId(request.getTestScopeId());
        executionRequest.setEnvironmentId(request.getEnvironmentId());
        executionRequest.setUuid(request.getAtpExecutionRequestId());
        executionRequest.setThreads(request.getThreads());
        if (Objects.nonNull(request.getLabelTemplateId()) && !"null".equalsIgnoreCase(request.getLabelTemplateId())) {
            executionRequest.setLabelTemplateId(UUID.fromString(request.getLabelTemplateId()));
        }
        final String widgetConfigTemplateId = request.getWidgetConfigTemplateId();
        if (Objects.nonNull(widgetConfigTemplateId) && !"null".equalsIgnoreCase(widgetConfigTemplateId)) {
            executionRequest.setWidgetConfigTemplateId(UUID.fromString(widgetConfigTemplateId));
        }
        executionRequest.setExecutorId(request.getExecutorId());

        executionRequest.setAutoSyncCasesWithJira(request.isAutoSyncCasesWithJira());
        executionRequest.setAutoSyncRunsWithJira(request.isAutoSyncRunsWithJira());
        executionRequest.setFlagIds(request.getFlagIds());
        log.debug("Prepare ER {}", executionRequest);
        return executionRequest;
    }

    private TestRun getTestRunByRequest(StartRunRequest request) {
        TestRun testRun = new TestRun();
        testRun.setTestCaseName(request.getTestCaseName());
        testRun.setTestCaseId(request.getTestCaseId());
        testRun.setName(request.getTestRunName());
        testRun.setStartDate(request.getStartDate());
        List<String> taHosts = convertToList(request.getTaHost(), ";");
        testRun.setTaHost(taHosts);
        List<String> qaHosts = convertToList(request.getQaHost(), ";");
        testRun.setQaHost(qaHosts);
        testRun.setExecutor(request.getExecutor());
        testRun.setMetaInfo(request.getMetaInfo());
        testRun.setDataSetListUrl(request.getDataSetListId());
        testRun.setDataSetUrl(request.getDataSetId());
        if (!Strings.isNullOrEmpty(request.getTestRunId())) {
            testRun.setUuid(UUID.fromString(request.getTestRunId()));
            testRun.setCreatedDate(new Timestamp(System.currentTimeMillis()));
        }
        testRun.setTestScopeSection(request.getTestScopeSection());
        testRun.setOrder(request.getOrder());
        testRun.setLabelIds(request.getLabelIds());
        testRun.setRootCauseId(RootCauseEnum.NOT_ANALYZED.getRootCause().getUuid());
        testRun.setFinalTestRun(request.isFinalTestRun());
        testRun.setInitialTestRunId(request.getInitialTestRunId());
        return testRun;
    }

    private List<String> convertToList(String text, String s) {
        return Arrays.asList(StringUtils.split(StringUtils.defaultIfEmpty(text, ""), s));
    }

    /**
     * Stop testRun.
     *
     * @deprecated use method from TestRunLoggingController
     */
    @Deprecated
    @PostMapping(STOP_PATH)
    public String stopTestRun(@RequestBody JsonObject request) {
        log.debug("Request to stop TR: {}", request);
        final String status = testRunService.stopTestRun(request);
        issueService.mapTestRunsAndRecalculateIssues(
                Collections.singletonList(UUID.fromString(JsonHelper.getStringValue(request, "testRunId"))));
        JsonObject response = new JsonObject();
        response.addProperty("executionStatus", status);

        return response.toString();
    }

    /**
     * Stop all testRuns with provided UUID's.
     */
    @PostMapping(STOP_TEST_RUNS_PATH)
    public List<UUID> stopTestRuns(@RequestBody List<UUID> testRunUuids) {
        issueService.mapTestRunsAndRecalculateIssues(testRunUuids);
        return testRunService.stopTestRuns(testRunUuids);
    }

    /**
     * For all testRuns with provided UUID's recalculates issues
     * and updates status.
     */
    @PostMapping(BULK_PATH + FINISH_PATH + DELAYED_PATH)
    public List<UUID> finishTestRunsDelayed(@RequestBody List<UUID> testRunUuids) {
        log.info("Test runs {} received to delayed finish.", testRunUuids);
        issueService.mapTestRunsAndRecalculateIssues(testRunUuids);
        return testRunService.finishTestRuns(testRunUuids, true);
    }

    /**
     * Create or update {@link TestRun} by JSON request. For integration with ATP1.0
     *
     * @param request with info for creating/update
     * @return JSON with uuid of {@link TestRun}
     * @deprecated use method from TestRunLoggingController
     */
    @PostMapping(UPDATE_OR_CREATE_PATH)
    public JsonObject updateOrCreate(@RequestBody JsonObject request) {
        final UUID testRunId = testRunService.updateOrCreate(request);

        JsonObject result = new JsonObject();
        result.addProperty("testRunId", String.valueOf(testRunId));
        log.debug("Test Run: {} was created.", testRunId);
        return result;
    }
}
