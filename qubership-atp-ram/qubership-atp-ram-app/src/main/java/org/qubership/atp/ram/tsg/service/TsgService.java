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

package org.qubership.atp.ram.tsg.service;

import static org.qubership.atp.ram.enums.DefaultSuiteNames.EXECUTION_REQUESTS_LOGS;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.Project;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.services.ExecutionRequestService;
import org.qubership.atp.ram.services.LogRecordService;
import org.qubership.atp.ram.services.ProjectsService;
import org.qubership.atp.ram.services.TestRunService;
import org.qubership.atp.ram.tsg.model.FdrResponse;
import org.qubership.atp.ram.tsg.model.TsgCheckPoint;
import org.qubership.atp.ram.tsg.model.TsgConfiguration;
import org.qubership.atp.ram.tsg.model.TsgFdr;
import org.qubership.atp.ram.tsg.senders.Sender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

@Service
public class TsgService {

    private static final Logger LOG = LoggerFactory.getLogger(TsgService.class);

    private final ExecutionRequestService executionRequestService;
    private final TestRunService testRunService;
    private final LogRecordService logRecordService;
    private final ProjectsService projectsService;
    private final TsgProjectService tsgProjectService;
    private final TsgErService tsgErService;
    private final TsgConfiguration tsgConfiguration;
    private final Sender<List<UUID>> fdrSender;

    private List<ExecutionRequest> executionRequestQueue;

    @Value("${base.url}")
    private String ramUrl;

    @Value("${catalogue.url}")
    private String catalogueUrl;

    @Value("${grayLog.url | http://graylog-service-address}")
    private String grayLogUrl;

    /**
     * Constructor.
     */
    @Autowired
    public TsgService(ExecutionRequestService executionRequestService,
                      TestRunService testRunService,
                      LogRecordService logRecordService,
                      ProjectsService projectsService,
                      TsgProjectService tsgProjectService,
                      TsgErService tsgErService,
                      TsgConfiguration tsgConfiguration,
                      @Lazy Sender<List<UUID>> fdrSender) {
        this.executionRequestService = executionRequestService;
        this.testRunService = testRunService;
        this.logRecordService = logRecordService;
        this.projectsService = projectsService;
        this.tsgProjectService = tsgProjectService;
        this.tsgErService = tsgErService;
        this.tsgConfiguration = tsgConfiguration;
        this.fdrSender = fdrSender;
        executionRequestQueue = new CopyOnWriteArrayList<>();
    }

    /**
     * Returns List of TSG FDR objects.
     */
    public List<TsgFdr> buildAllFdrsForEr(UUID executionRequestUuid) {
        LOG.info("Start of FDRs building for ER: {}.", executionRequestUuid);
        ExecutionRequest er = executionRequestService.findById(executionRequestUuid);
        if (er == null) {
            LOG.warn("There is no ER with uuid: {}", executionRequestUuid);
            return null;
        }
        List<TsgFdr> tsgFdrs = new ArrayList<>();
        for (TestRun tr : testRunService.findAllByExecutionRequestId(executionRequestUuid)) {
            TsgFdr tsgFdr = buildFdr(tr.getUuid());
            tsgFdrs.add(tsgFdr);
        }
        LOG.info("Finish of FDRs building for ER: {}.", executionRequestUuid);
        return tsgFdrs;
    }

    /**
     * Converts TestRun Results to the TSG FDR object.
     */
    public TsgFdr buildFdr(UUID testRunUuid) {
        TestRun testRun = testRunService.getByUuid(testRunUuid);
        ExecutionRequest executionRequest = executionRequestService.findById(testRun.getExecutionRequestId());
        Project project = projectsService.getProjectById(executionRequest.getProjectId());
        if (Objects.isNull(project) || !project.isTsgIntegration()) {
            LOG.debug("TSG Integration is disabled for TR: {}", testRunUuid);
            return null;
        }
        TsgFdr fdr = new TsgFdr();
        fdr.setProjectName(project.getName());
        fdr.setExecutionRequestId(testRun.getExecutionRequestId());
        fdr.setTestRunId(testRunUuid);
        fdr.setTestRunName(testRun.getName());
        fdr.setTestCaseName(testRun.getTestCaseName());
        fdr.setStatus(testRun.getTestingStatus().getName());
        fdr.setStartDate(testRun.getStartDate().toLocalDateTime().toString());
        fdr.setFinishDate(testRun.getFinishDate().toLocalDateTime().toString());
        fdr.setEnvironment(testRun.getQaHost());
        fdr.setScope("Execution");
        String url = catalogueUrl.length() > 0 ? catalogueUrl : ramUrl;
        fdr.setExecutionLink(url + "/project/" + executionRequest.getProjectId() + "/ram/execution-request/"
                + testRun.getExecutionRequestId() + "/" + testRunUuid);
        fdr.setCheckPoints(fillCheckPointsWithParents(testRunUuid, null, false));
        return fdr;
    }

    /**
     * Set fdrWasSent for test run as true.
     */
    public void setFdrWasSent(UUID testRunUuid) {
        TestRun testRun = testRunService.getByUuid(testRunUuid);
        testRun.setFdrWasSent(true);
        testRunService.save(testRun);
    }

    private List<TsgCheckPoint> fillCheckPointsWithParents(UUID testRunUuid, UUID parentUuid, boolean withParents) {
        List<TsgCheckPoint> checkPoints = new ArrayList<>();
        List<LogRecord> logRecords = withParents
                ? logRecordService.findByTestRunIdAndParentUuid(testRunUuid, parentUuid, null)
                : logRecordService.findLogRecordsWithSpecificFieldsByTestRunIdOrderByStartDateAsc(testRunUuid);
        Map<UUID, TsgCheckPoint> parents = new LinkedHashMap<>();
        for (LogRecord lr : logRecords) {
            if (!withParents && checkLogRecordIsSection(lr)) {
                continue;
            }
            UUID parentRecordUuid = lr.getParentRecordId();
            TsgCheckPoint checkPoint;
            if (Objects.isNull(parentRecordUuid)) {
                checkPoint = createCheckPoint(lr);
                parents.put(lr.getUuid(), checkPoint);
            } else {
                TsgCheckPoint parent = parents.get(parentRecordUuid);
                if (parent == null) {
                    LogRecord parentLr = logRecordService.findById(parentRecordUuid);
                    parent = createCheckPoint(parentLr);
                    parents.put(parentRecordUuid, parent);
                }
                checkPoint = createCheckPoint(lr);
                parent.addCheckPoint(checkPoint);
            }
            if (withParents) {
                checkPoint.setCheckPoints(fillCheckPointsWithParents(testRunUuid, lr.getUuid(), withParents));
            }
            checkPoints.add(checkPoint);
        }
        if (!withParents) {
            checkPoints = new ArrayList<>();
            for (Map.Entry<UUID, TsgCheckPoint> entry : parents.entrySet()) {
                checkPoints.add(entry.getValue());
            }
        }
        return checkPoints;
    }

    private boolean checkLogRecordIsSection(LogRecord lr) {
        long childrenCount = logRecordService.getChildrenCount(lr);
        return lr.isSection() || childrenCount > 0;
    }

    private TsgCheckPoint createCheckPoint(LogRecord lr) {
        TsgCheckPoint checkPoint = new TsgCheckPoint();
        checkPoint.setName(lr.getName());
        checkPoint.setStatus(lr.getTestingStatus().getName());
        if ("FAILED".equalsIgnoreCase(lr.getTestingStatus().getName())) {
            checkPoint.setMessage(lr.getMessage());
        }
        return checkPoint;
    }

    /**
     * Prepare and send list of FDR to TSG Receiver.
     */
    public void sendFdrs(ExecutionRequest executionRequest) {
        LOG.debug("Start building FDRs for ER: {}", executionRequest.getUuid());
        List<TestRun> testRunList = testRunService.findTestRunsForFdrByExecutionRequestId(executionRequest.getUuid());
        List<UUID> testRunsUuids = testRunList.stream()
                .filter(this::isValidTestRun).map(TestRun::getUuid).collect(Collectors.toList());
        LOG.trace("Found TestRun ids to send FDRs {}", testRunsUuids);
        fdrSender.send(testRunsUuids);
        LOG.debug("Sending FDRs for ER: {} was finished", executionRequest.getUuid());
    }

    /**
     * Prepare and send list of FDR to TSG Receiver.
     */
    public void sendFdrs(List<UUID> uuidList) {
        for (UUID testRunUuid : uuidList) {
            TestRun tr = testRunService.getByUuid(testRunUuid);
            ExecutionRequest er = executionRequestService.findById(tr.getExecutionRequestId());
            Project p = projectsService.getProjectById(er.getProjectId());
            // "allpratp"
            String tsgProjectName = p.getTsgProjectName();
            if (!p.isTsgIntegration() || Strings.isNullOrEmpty(tsgProjectName)) {
                LOG.debug("TSG Integration is disabled for: {} - {}",
                        p.getName(), p.getUuid());
                return;
            }
            List<TsgFdr> request = new ArrayList<>();
            LOG.debug("Prepare FDR for TR: {} - {}", tr.getName(), tr.getUuid());
            TsgFdr fdr = buildFdr(testRunUuid);
            request.add(fdr);
            HttpEntity<List<TsgFdr>> entity = new HttpEntity<>(request);
            String tsgFdrEndpoint = tsgConfiguration.getTsgReceiverUrl() + "/putJson/" + tsgProjectName;
            RestTemplate template = new RestTemplate();
            ResponseEntity<List<FdrResponse>> responseEntity = template
                    .exchange(tsgFdrEndpoint, HttpMethod.POST, entity,
                            new ParameterizedTypeReference<List<FdrResponse>>() {
                            });
            if (responseEntity.getStatusCode() == HttpStatus.OK) {
                LOG.trace("FDR for TR: {} was sent to {} "
                                + "\nResponse from TSG Receiver: {}", tr.getUuid(), tsgFdrEndpoint,
                        responseEntity.toString());
                List<FdrResponse> fdrResponseList = responseEntity.getBody();
                if (fdrResponseList != null) {
                    fdrResponseList.forEach(fdrResponse -> {
                        if (tr.getUuid().equals(fdrResponse.getTestRunId())) {
                            LOG.trace("Fdr link for TestRun: {}", tr.getUuid());
                            tr.setFdrLink(fdrResponse.getFdrLink());
                        }
                    });
                }
                tr.setFdrWasSent(true);
                testRunService.save(tr);
            } else {
                LOG.error("FDR for TR: {} was not sent to {} "
                                + "\nResponse from TSG Receiver: {}", tr.getUuid(), tsgFdrEndpoint,
                        responseEntity.toString());
            }
        }
    }

    /**
     * Fill queue with not finished ers.
     */
    public void checkNotFinishedErs() {
        List<Project> projects = tsgProjectService.getAllTsgProjects();
        for (Project project : projects) {
            List<ExecutionRequest> ers = tsgErService.findInProgressErs(project.getUuid());
            executionRequestQueue.addAll(ers);
        }
    }

    /**
     * Proceed queue.
     */
    public void checkFinishedErs() {
        for (int idx = 0; idx < executionRequestQueue.size(); idx++) {
            ExecutionRequest er = executionRequestQueue.get(idx);
            if (er.getExecutionStatus().equals(ExecutionStatuses.FINISHED)) {
                LOG.debug("Prepare FDR for ER: {}", er.getUuid());
                executionRequestQueue.remove(idx);
                LOG.debug("FDR was sent for ER: {}", er.getUuid());
            } else {
                LOG.warn("FDR wasn't sent for ER: {}", er.getUuid());
            }
        }
    }

    /**
     * Returns list not proceed ERs.
     */
    public List<ExecutionRequest> getAllRequestInQueue() {
        return executionRequestQueue;
    }

    private boolean isValidTestRun(TestRun tr) {
        if (tr.isFdrWasSent()) {
            return false;
        }
        if (EXECUTION_REQUESTS_LOGS.getName().equalsIgnoreCase(tr.getName())
                || tr.getTestingStatus() == null) {
            return false;
        }

        if (!tsgConfiguration.getStatuses().getExecutionStatuses().contains(tr.getExecutionStatus())
                || logRecordService.findLogRecordNamesByTestRunId(tr.getUuid()).stream()
                .anyMatch(logRecord -> logRecord.getName().contains("Test Run has been terminated"))) {
            return false;
        }
        return tsgConfiguration.getStatuses().getTestingStatuses().contains(tr.getTestingStatus());
    }

    /**
     * Returns daily integrations info.
     */
    public JsonArray getDailyInfo(int daysFilter) {
        JsonArray result = new JsonArray();
        List<Project> projects = tsgProjectService.getAllTsgProjects();
        for (Project project : projects) {
            boolean projectHasFails = false;
            int ersCount = 0;
            int projectTestRunsCount = 0;
            JsonObject projectInfo = new JsonObject();
            projectInfo.addProperty("projectId", String.valueOf(project.getUuid()));
            projectInfo.addProperty("projectName", project.getName());
            projectInfo.addProperty("tsgProjectName", project.getTsgProjectName());
            Timestamp dateFilter = Timestamp.valueOf(LocalDate.now().minusDays(daysFilter).atStartOfDay());
            List<ExecutionRequest> projectErs = executionRequestService
                    .findFinishedErByProjectAndSortByFinishDate(project.getUuid(), dateFilter);
            JsonArray executionRequestsInfo = new JsonArray();
            for (ExecutionRequest er : projectErs) {
                boolean erHasFails = false;
                int erTestRunsCount = 0;
                ersCount++;
                JsonObject erInfo = new JsonObject();
                erInfo.addProperty("executionRequestId", String.valueOf(er.getUuid()));
                erInfo.addProperty("executionStatus", String.valueOf(er.getExecutionStatus()));
                erInfo.addProperty("finishDate", String.valueOf(er.getFinishDate()));
                List<TestRun> erTrs = testRunService.findAllByExecutionRequestId(er.getUuid());
                JsonArray testRuns = new JsonArray();
                for (TestRun tr : erTrs) {
                    if (EXECUTION_REQUESTS_LOGS.getName().equalsIgnoreCase(tr.getName())
                            || tr.getTestingStatus() == null) {
                        continue;
                    }
                    if (!tsgConfiguration.getStatuses().getExecutionStatuses().contains(tr.getExecutionStatus())) {
                        continue;
                    }
                    if (!tsgConfiguration.getStatuses().getTestingStatuses().contains(tr.getTestingStatus())) {
                        continue;
                    }
                    erTestRunsCount++;
                    projectTestRunsCount++;
                    JsonObject testRunInfo = new JsonObject();
                    testRunInfo.addProperty("testRunId", String.valueOf(tr.getUuid()));
                    testRunInfo.addProperty("testRunName", tr.getName());
                    testRunInfo.addProperty("executionStatus", String.valueOf(tr.getExecutionStatus()));
                    testRunInfo.addProperty("testingStatus", String.valueOf(tr.getTestingStatus()));
                    testRunInfo.addProperty("fdrWasSent", tr.isFdrWasSent());
                    String grayLogLink = "";
                    if (!tr.isFdrWasSent()) {
                        erHasFails = true;
                        projectHasFails = true;
                        grayLogLink = buildGrayLogLink(tr.getStartDate(), tr.getFinishDate(), 5, tr.getUuid());
                    }
                    testRunInfo.addProperty("grayLog", grayLogLink);
                    testRuns.add(testRunInfo);
                }
                erInfo.addProperty("hasFail", erHasFails);
                if (erHasFails) {
                    erInfo.addProperty("grayLog",
                            buildGrayLogLink(er.getStartDate(), er.getFinishDate(), 5, er.getUuid()));
                } else {
                    erInfo.addProperty("grayLog", "");
                }
                erInfo.addProperty("testRunsCount", erTestRunsCount);
                erInfo.add("testRuns", testRuns);
                executionRequestsInfo.add(erInfo);
            }
            projectInfo.addProperty("hasFail", projectHasFails);
            projectInfo.addProperty("projectErsCount", ersCount);
            projectInfo.addProperty("projectTestRunsCount", projectTestRunsCount);
            projectInfo.add("executionRequests", executionRequestsInfo);
            result.add(projectInfo);
        }
        return result;
    }

    private String buildGrayLogLink(Timestamp startDate, Timestamp finishDate, int hours, UUID uuid) {
        if (Strings.isNullOrEmpty(grayLogUrl)) {
            return "";
        }
        String grayLogLink = grayLogUrl
                + "/streams/000000000000000000000001/search?rangetype=absolute&"
                + "fields=message%2Csource&width=1366&highlightMessage=&"
                + "from=${startDate}&"
                + "to=${finishDate}&"
                + "q=%22${testRunId}%22";
        grayLogLink = grayLogLink.replace("${startDate}", startDate.toString());
        finishDate.setTime(finishDate.getTime() + hours * 60 * 60 * 1000);
        grayLogLink = grayLogLink.replace("${finishDate}", finishDate.toString());
        grayLogLink = grayLogLink.replace("${testRunId}", uuid.toString());
        return grayLogLink;
    }

    /**
     * Returns html report.
     */
    public String getHtmlReport(int daysFilter) {
        StringBuilder result = new StringBuilder();
        result.append("<table cellspacing=\"2\" border=\"1\" cellpadding=\"1\">");
        result.append("<thead>");
        result.append("<tr>");
        result.append(writeTh("ProjectId"));
        result.append(writeTh("ProjectName"));
        result.append(writeTh("TsgProjectName"));
        result.append(writeTh("ExecutionRequestId"));
        result.append(writeTh("Has error"));
        result.append(writeTh("GrayLog"));
        result.append(writeTh("TestRunId"));
        result.append(writeTh("TestRunName"));
        result.append(writeTh("ExecutionStatus"));
        result.append(writeTh("TestingStatus"));
        result.append(writeTh("FDR Was Sent"));
        result.append(writeTh("GrayLog"));
        result.append("</tr>");
        result.append("</thead>");
        result.append("<tbody>");
        JsonArray projects = getDailyInfo(daysFilter);
        for (int projectIdx = 0; projectIdx < projects.size(); projectIdx++) {
            JsonObject project = projects.get(projectIdx).getAsJsonObject();
            JsonArray ers = project.getAsJsonArray("executionRequests");
            for (int erIdx = 0; erIdx < ers.size(); erIdx++) {
                JsonObject er = ers.get(erIdx).getAsJsonObject();
                JsonArray testRuns = er.getAsJsonArray("testRuns");
                for (int testRunIdx = 0; testRunIdx < testRuns.size(); testRunIdx++) {
                    JsonObject testRun = testRuns.get(testRunIdx).getAsJsonObject();
                    result.append("<tr>");
                    result.append(writeTd(project.get("projectId").getAsString()));
                    result.append(writeTd(project.get("projectName").getAsString()));
                    result.append(writeTd(project.get("tsgProjectName").getAsString()));
                    result.append(writeTd(er.get("executionRequestId").getAsString()));
                    result.append(writeColorCell(er.get("hasFail").getAsString(),
                            er.get("hasFail").getAsBoolean() ? "red" : ""));
                    result.append(writeTd(Strings.isNullOrEmpty(er.get("grayLog").getAsString()) ? "" :
                            "<a href='" + er.get("grayLog") + "'>Search in GrayLog</a>"));
                    result.append(writeTd(testRun.get("testRunId").getAsString()));
                    result.append(writeTd(testRun.get("testRunName").getAsString()));
                    result.append(writeTd(testRun.get("executionStatus").getAsString()));
                    result.append(writeTd(testRun.get("testingStatus").getAsString()));
                    result.append(writeColorCell(testRun.get("fdrWasSent").getAsString(),
                            testRun.get("fdrWasSent").getAsBoolean() ? "" : "red"));
                    result.append(writeTd(Strings.isNullOrEmpty(testRun.get("grayLog").getAsString()) ? "" :
                            "<a href='" + testRun.get("grayLog") + "'>Search in GrayLog</a>"));
                    result.append("</tr>");
                }
            }
        }
        result.append("</tbody>");
        result.append("</table>");
        return result.toString();
    }

    private String writeTd(String text) {
        return writeCell("td", text);
    }

    private String writeTh(String text) {
        return writeCell("th", text);
    }

    private String writeColorCell(String text, String color) {
        return "<td bgcolor='" + color + "'>" + text + "</td>";
    }

    private String writeCell(String htmlTag, String text) {
        return "<" + htmlTag + ">" + text + "</" + htmlTag + ">";
    }
}
