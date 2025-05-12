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

package org.qubership.atp.ram.service;

import static java.util.Objects.nonNull;
import static org.qubership.atp.ram.utils.StreamUtils.extractFields;
import static org.qubership.atp.ram.utils.StreamUtils.extractFlatEntities;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.qubership.atp.ram.dto.request.DefectCreateRequest;
import org.qubership.atp.ram.dto.response.DefectPredefineResponse;
import org.qubership.atp.ram.dto.response.TestCaseLabelResponse;
import org.qubership.atp.ram.exceptions.defects.RamDefectBugTrackingSystemConfigurationException;
import org.qubership.atp.ram.exceptions.defects.RamDefectDescriptionRenderException;
import org.qubership.atp.ram.exceptions.defects.RamDefectEnvironmentDescriptionRenderException;
import org.qubership.atp.ram.exceptions.defects.RamDefectPostCreationReporterUpdateException;
import org.qubership.atp.ram.model.DefectDescriptionRenderModel;
import org.qubership.atp.ram.model.DefectDescriptionRenderModel.Link;
import org.qubership.atp.ram.model.jira.JiraIssueCreateRequest;
import org.qubership.atp.ram.model.jira.JiraIssueCreateResponse;
import org.qubership.atp.ram.model.jira.JiraIssueResponse;
import org.qubership.atp.ram.models.EnvironmentsInfo;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.FailPattern;
import org.qubership.atp.ram.models.Issue;
import org.qubership.atp.ram.models.JiraComponent;
import org.qubership.atp.ram.models.JiraTicket;
import org.qubership.atp.ram.models.Label;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.PotsStatisticsPerAction;
import org.qubership.atp.ram.models.RootCause;
import org.qubership.atp.ram.models.SsmMetricReports;
import org.qubership.atp.ram.models.TestPlan;
import org.qubership.atp.ram.models.TestPlan.BugTrackingSystemSynchronization;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.service.template.TemplateRenderService;
import org.qubership.atp.ram.services.CatalogueService;
import org.qubership.atp.ram.services.EnvironmentsInfoService;
import org.qubership.atp.ram.services.EnvironmentsService;
import org.qubership.atp.ram.services.ExecutionRequestService;
import org.qubership.atp.ram.services.FailPatternService;
import org.qubership.atp.ram.services.IssueService;
import org.qubership.atp.ram.services.LogRecordService;
import org.qubership.atp.ram.services.PotService;
import org.qubership.atp.ram.services.RootCauseService;
import org.qubership.atp.ram.services.TestCaseService;
import org.qubership.atp.ram.services.TestRunService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import freemarker.template.Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefectPredefineService {

    private static final String ISSUE_DEFECT_TYPE = "Defect";
    private static final int DEFECT_SUMMARY_MAX_LENGTH = 252;
    private static final String JIRA_TICKET_CLOSED_STATUS = "Closed";
    private static final Integer JIRA_ISSUE_DEFECTS_MAX_SIZE = 10;

    @Value("${catalogue.url}")
    String catalogueUrl;

    @Value("classpath:data/defect-description/defect-description.template.ftl")
    Resource descriptionTemplate;
    @Value("classpath:data/defect-description/defect-environment.template.ftl")
    Resource environmentTemplate;

    private final IssueService issueService;
    private final TemplateRenderService templateRenderService;
    private final FailPatternService failPatternService;
    private final RootCauseService rootCauseService;
    private final ExecutionRequestService executionRequestService;
    private final TestRunService testRunService;
    private final EnvironmentsInfoService environmentsInfoService;
    private final EnvironmentsService environmentsService;
    private final PotService potService;
    private final LogRecordService logRecordService;
    private final TestCaseService testCaseService;
    private final CatalogueService catalogueService;

    /**
     * Predefine Defect data.
     */
    public DefectPredefineResponse predefine(UUID issueId) throws Exception {
        final DefectPredefineResponse response = new DefectPredefineResponse();
        final Issue issue = issueService.get(issueId);
        final List<UUID> failedTestRunIds = issue.getFailedTestRunIds();

        if (!isEmpty(failedTestRunIds)) {
            final List<TestRun> failedTestRuns = testRunService.getByIds(failedTestRunIds);
            final List<TestCaseLabelResponse> labelResponse = testCaseService.getTestCaseLabelsByIds(failedTestRuns);

            if (nonNull(labelResponse) && !labelResponse.isEmpty()) {
                final List<Label> labels = extractFlatEntities(labelResponse, TestCaseLabelResponse::getLabels);
                response.setLabels(labels);

                final UUID executionRequestId = issue.getExecutionRequestId();
                final ExecutionRequest executionRequest = executionRequestService.get(executionRequestId);
                final UUID testPlanId = executionRequest.getTestPlanId();
                final List<JiraComponent> allComponents = catalogueService.getTestPlanJiraComponents(testPlanId);
                response.setComponents(allComponents);

                final List<JiraComponent> selectedComponents = labelResponse.stream()
                        .flatMap(testCaseLabelResponse -> testCaseLabelResponse.getComponents().stream())
                        .distinct()
                        .collect(Collectors.toList());
                response.setSelectedComponents(selectedComponents);
            }
        }

        final DefectDescriptionRenderModel descriptionRenderModel = getDescriptionRenderModel(issue);
        final String executionRequestLink = descriptionRenderModel.getLinkToEr();
        response.setAtpLink(executionRequestLink);

        String message = descriptionRenderModel.getMessage();
        response.setSummary(message);

        String description;
        try {
            final InputStream inputStream = descriptionTemplate.getInputStream();
            final Template descTemplate = new Template("description",
                    new InputStreamReader(inputStream), null);
            description = templateRenderService.render(descTemplate, descriptionRenderModel);
        } catch (Exception e) {
            log.error("Failed to render defect description for issue: {}", issueId, e);
            throw new RamDefectDescriptionRenderException();
        }
        response.setDescription(description);

        try {
            final Template envTemplate = new Template("environment",
                    new InputStreamReader(environmentTemplate.getInputStream()), null);
            response.setEnvironment(templateRenderService.render(envTemplate, descriptionRenderModel));
        } catch (Exception e) {
            log.error("Failed to render defect environment description for issue: {}", issueId, e);
            throw new RamDefectEnvironmentDescriptionRenderException();
        }

        return response;
    }

    /**
     * Create data for Defect description markdown rendering.
     */
    private DefectDescriptionRenderModel getDescriptionRenderModel(Issue issue) {
        final DefectDescriptionRenderModel renderModel = new DefectDescriptionRenderModel();
        final UUID executionRequestId = issue.getExecutionRequestId();
        final ExecutionRequest executionRequest = executionRequestService.get(executionRequestId);
        final UUID projectId = executionRequest.getProjectId();

        // Message
        String message = issue.getMessage();
        renderModel.setMessage(message);

        // Log Record
        final List<UUID> logRecordIds = issue.getLogRecordIds();
        LogRecord logRecord = null;
        if (!isEmpty(logRecordIds)) {
            final UUID firstLogRecordId = logRecordIds.get(0);
            logRecord = logRecordService.get(firstLogRecordId);
            final String logRecordUrl = getLogRecordUrl(projectId, executionRequestId, logRecord);
            final UUID testRunId = logRecord.getTestRunId();
            final TestRun testRun = testRunService.get(testRunId);

            renderModel.setLinkToLc(testRun.getLogCollectorData());
            renderModel.setLogRecordLink(new Link(logRecord.getName(), logRecordUrl));
            renderModel.setSvpLinks(getSvpLinksFromTestRunId(testRun.getUuid()));
        }

        // Environment
        final EnvironmentsInfo environmentsInfo = environmentsInfoService.findByExecutionRequestId(executionRequestId);
        final UUID environmentId = environmentsInfo.getEnvironmentId();
        final String environmentName = environmentsService.getEnvironmentNameById(environmentId);
        final String environmentLink = getEnvironmentLink(projectId, environmentId);
        renderModel.setEnvironmentLink(new Link(environmentName, environmentLink));
        renderModel.setQaSystems(environmentsInfo.getQaSystemInfoList());

        // Fail Pattern
        final UUID failPatternId = issue.getFailPatternId();
        if (nonNull(failPatternId)) {
            final FailPattern failPattern = failPatternService.get(failPatternId);
            renderModel.setFailPattern(failPattern.getName());
        }

        // Failure Reason
        final UUID failReasonId = issue.getFailReasonId();
        if (nonNull(failReasonId)) {
            final RootCause rootCause = rootCauseService.get(failReasonId);
            renderModel.setFailReason(rootCause.getName());
        }

        // Link to ER
        final String executionRequestLink = getExecutionRequestLink(projectId, executionRequestId);
        renderModel.setLinkToEr(executionRequestLink);

        // Links to POT
        if (nonNull(logRecord)) {
            final UUID testRunId = logRecord.getTestRunId();
            final List<PotsStatisticsPerAction> pots = potService.collectStatisticForTestRun(testRunId);
            final List<Link> potFileLinks = pots.stream()
                    .map(pot -> new Link(pot.getPotFileName(), getPotFileLink(projectId, executionRequestId)))
                    .collect(Collectors.toList());
            renderModel.setPotLinks(potFileLinks);
        }

        // Blocks
        final List<UUID> failedTestRunIds = issue.getFailedTestRunIds();
        if (!isEmpty(failedTestRunIds)) {
            final List<TestRun> failedTestRuns = testRunService.getByIds(failedTestRunIds);
            final List<Link> blockLinks = failedTestRuns.stream()
                    .map(testRun -> new Link(testRun.getName(), getTestRunUrl(projectId, testRun)))
                    .collect(Collectors.toList());
            renderModel.setBlockLinks(blockLinks);
        }

        // SSM Metrics
        if (nonNull(logRecord)) {
            SsmMetricReports ssmMetricReports = logRecord.getSsmMetricReports();

            if (Objects.isNull(ssmMetricReports)) {
                UUID parentRecordId = logRecord.getParentRecordId();
                if (nonNull(parentRecordId)) {
                    ssmMetricReports = logRecordService.get(parentRecordId).getSsmMetricReports();
                }
            }

            if (nonNull(ssmMetricReports)) {
                final List<Link> ssmMetricLinks = new ArrayList<>();

                final UUID microservicesReportId = ssmMetricReports.getMicroservicesReportId();
                if (nonNull(microservicesReportId)) {
                    final String microservicesReportLink = getSsmMetricsReportLink(microservicesReportId);
                    ssmMetricLinks.add(new Link("Microservices.json", microservicesReportLink));
                }

                final UUID problemContextReportId = ssmMetricReports.getProblemContextReportId();
                if (nonNull(problemContextReportId)) {
                    final String problemContextReportLink = getSsmMetricsReportLink(problemContextReportId);
                    ssmMetricLinks.add(new Link("Problem context.json", problemContextReportLink));
                }
                renderModel.setSsmMetricsLinks(ssmMetricLinks);
            }
        }

        return renderModel;
    }

    private List<Link> getSvpLinksFromTestRunId(UUID testRunId) {
        return logRecordService
                .getAllLogRecordsByTestRunIds(Collections.singletonList(testRunId))
                .stream()
                .filter(logRecord -> !StringUtils.isEmpty(logRecord.getLinkToSvp()))
                .map(logRecord -> new Link("Link to SVP", logRecord.getLinkToSvp()))
                .collect(Collectors.toList());
    }

    /**
     * Create defect.
     */
    public JiraIssueCreateResponse createDefect(UUID testPlanId, UUID issueId,
                                                DefectCreateRequest request) throws Exception {
        TestPlan testPlan = catalogueService.getTestPlan(testPlanId);
        BugTrackingSystemSynchronization synchronization = testPlan.getSynchronization();

        if (Objects.isNull(synchronization)) {
            log.error("Failed to create defect. BTS is not configured for the test plan with id: {}", testPlan);
            throw new RamDefectBugTrackingSystemConfigurationException();
        }

        String summary = validateSummary(request.getSummary());

        JiraIssueCreateRequest issueCreateRequest = new JiraIssueCreateRequest(
                synchronization.getProjectKey(),
                summary,
                request.getDescription(),
                request.getPriority().getName(),
                extractFields(request.getLabels(), Label::getName),
                ISSUE_DEFECT_TYPE,
                request.getComponents(),
                request.getAtpLink(),
                request.getFoundIn(),
                request.getEnvironment()
        );

        JiraIssueCreateResponse response = catalogueService.createJiraTicket(testPlanId, issueCreateRequest);
        String systemUrl = synchronization.getSystemUrl();

        final JiraTicket newTicket = new JiraTicket(systemUrl + "/browse/" + response.getKey());

        final Issue issue = issueService.get(issueId);

        recalculateJiraTickets(issue, newTicket, testPlanId);

        String errorMessage = response.getErrorMessage();
        if (!StringUtils.isEmpty(errorMessage)) {
            final String newTicketKey = newTicket.getKey();
            log.error("Jira issue created with key '{}', but the reporter's change failed. Reason: {}",
                    newTicketKey, errorMessage);
            throw new RamDefectPostCreationReporterUpdateException(newTicketKey);
        }

        return response;
    }

    private String validateSummary(String summary) {
        if (!StringUtils.isEmpty(summary) && summary.length() > DEFECT_SUMMARY_MAX_LENGTH) {
            return summary
                    .replaceAll("\n", " ")
                    .replaceAll("\t", " ")
                    .substring(0, DEFECT_SUMMARY_MAX_LENGTH)
                    .concat("...");
        }

        return summary;
    }

    private void recalculateJiraTickets(Issue issue, JiraTicket newJiraTicket, UUID testPlanId) {
        final UUID failPatternId = issue.getFailPatternId();
        final FailPattern failPattern = failPatternService.get(failPatternId);

        List<JiraTicket> jiraDefects = failPattern.getJiraDefects();
        if (isEmpty(jiraDefects)) {
            jiraDefects = new ArrayList<>();
            failPattern.setJiraDefects(jiraDefects);
        }
        jiraDefects.add(newJiraTicket);

        if (jiraDefects.size() > JIRA_ISSUE_DEFECTS_MAX_SIZE) {
            final Map<String, String> issuesStatusMap = jiraDefects.stream()
                    .map(jiraDefect -> catalogueService.getJiraTicket(testPlanId, jiraDefect.getKey()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(JiraIssueResponse::getKey,
                            resp -> resp.getFields().getStatus().getName()));

            final Timestamp currentDate = Timestamp.valueOf(LocalDateTime.now());
            final List<JiraTicket> updJiraDefects = jiraDefects.stream()
                    .peek(jiraTicket -> {
                        final String key = jiraTicket.getKey();
                        final Timestamp createdDate = jiraTicket.getCreatedDate();
                        if (issuesStatusMap.containsKey(key)) {
                            if (Objects.isNull(createdDate)) {
                                jiraTicket.setCreatedDate(currentDate);
                            }
                            String status = issuesStatusMap.get(key);
                            jiraTicket.setResolved(JIRA_TICKET_CLOSED_STATUS.equals(status));
                        }
                    })
                    .sorted()
                    .limit(JIRA_ISSUE_DEFECTS_MAX_SIZE)
                    .collect(Collectors.toList());

            failPattern.setJiraDefects(updJiraDefects);

            issue.setJiraDefects(updJiraDefects);
            issue.propagateJiraTickets();
            issueService.save(issue);
        }

        failPattern.propagateJiraTickets();
        failPatternService.save(failPattern);
    }

    private String getLogRecordUrl(UUID projectId, UUID executionRequestId, LogRecord logRecord) {
        final UUID id = logRecord.getUuid();
        final UUID testRunId = logRecord.getTestRunId();

        return catalogueUrl + "/project/" + projectId + "/ram/execution-request/" + executionRequestId + "?node="
                + testRunId + "&logRecordId=" + id;
    }

    private String getTestRunUrl(UUID projectId, TestRun testRun) {
        final UUID executionRequestId = testRun.getExecutionRequestId();
        final UUID id = testRun.getUuid();

        return catalogueUrl + "/project/" + projectId + "/ram/execution-request/" + executionRequestId + "?node=" + id;
    }

    private String getPotFileLink(UUID projectId, UUID ertId) {
        return catalogueUrl + "/project/" + projectId + "/ram/execution-request/" + ertId + "#pots-statistic";
    }

    private String getEnvironmentLink(UUID projectId, UUID environmentId) {
        return catalogueUrl + "/project/" + projectId + "/environments/environment/" + environmentId;
    }

    private String getExecutionRequestLink(UUID projectId, UUID executionRequestId) {
        return catalogueUrl + "/project/" + projectId + "/ram/execution-request/" + executionRequestId;
    }

    private String getSsmMetricsReportLink(UUID reportId) {
        return catalogueUrl + "/redirect-uri/api/atp-ram/v1/api/environmentsInfo/mandatoryChecksReport/" + reportId;
    }
}
