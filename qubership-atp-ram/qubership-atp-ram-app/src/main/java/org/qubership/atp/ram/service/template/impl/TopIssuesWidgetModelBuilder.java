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

package org.qubership.atp.ram.service.template.impl;

import static java.util.Objects.nonNull;
import static org.qubership.atp.ram.enums.ExecutionRequestWidgets.TOP_ISSUES;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.ram.config.ApiPath;
import org.qubership.atp.ram.dto.response.IssueResponse;
import org.qubership.atp.ram.dto.response.IssueResponsesModel;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.WidgetConfigTemplate;
import org.qubership.atp.ram.models.WidgetConfigTemplate.WidgetConfig;
import org.qubership.atp.ram.models.WidgetType;
import org.qubership.atp.ram.service.mail.ReportParams;
import org.qubership.atp.ram.services.ExecutionRequestService;
import org.qubership.atp.ram.services.IssueService;
import org.qubership.atp.ram.services.WidgetConfigTemplateService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
public class TopIssuesWidgetModelBuilder extends AbstractWidgetModelBuilder {

    public static final Integer TOP_ISSUES_DEFAULT_SIZE_LIMIT = 5;
    public static final String TOP_ISSUES_TEST_RUNS = "failedCasesCount";
    public static final String TOP_ISSUES_SORT_TYPE = "DESC";
    public static final int MESSAGE_LENGTH_LIMIT = 125;
    public static final String ELONGATED_MESSAGE_SUFFIX = "...";
    public static final String TOP_ISSUES_PATH = "#top-issues";
    public static final String LT_SYMBOL = "<";
    public static final String LT_SYMBOL_ESCAPE = "&lt;";
    public static final String GT_SYMBOL = ">";
    public static final String GT_SYMBOL_ESCAPE = "&gt;";

    @Value("${base.url}")
    private String baseUrl;
    @Value("${catalogue.url}")
    private String catalogueUrl;
    private IssueService issueService;
    private ExecutionRequestService executionRequestService;
    private WidgetConfigTemplateService widgetConfigTemplateService;

    /**
     * TopIssuesWidgetModelBuilder constructor.
     */
    public TopIssuesWidgetModelBuilder(IssueService issueService,
                                       ExecutionRequestService executionRequestService,
                                       WidgetConfigTemplateService widgetConfigTemplateService) {
        this.issueService = issueService;
        this.executionRequestService = executionRequestService;
        this.widgetConfigTemplateService = widgetConfigTemplateService;
    }

    @Override
    protected Map<String, Object> buildModel(ReportParams reportParams) {
        final UUID executionRequestId = reportParams.getExecutionRequestUuid();

        WidgetConfigTemplate widgetConfigTemplate =
                widgetConfigTemplateService.getWidgetConfigTemplateForEr(executionRequestId).getTemplate();

        int topIssuesSizeLimit = TOP_ISSUES_DEFAULT_SIZE_LIMIT;

        if (nonNull(widgetConfigTemplate)) {
            WidgetConfig topIssuesWidgetConfig = widgetConfigTemplate.getWidgetConfig(TOP_ISSUES.getWidgetId());
            Optional<Integer> sizeLimitOptional = Optional.ofNullable(topIssuesWidgetConfig.getSizeLimit());
            topIssuesSizeLimit = sizeLimitOptional.orElse(TOP_ISSUES_DEFAULT_SIZE_LIMIT);
        }

        IssueResponsesModel issues = issueService.getAllIssuesByExecutionRequestId(
                executionRequestId,
                0,
                topIssuesSizeLimit,
                TOP_ISSUES_TEST_RUNS,
                TOP_ISSUES_SORT_TYPE);
        List<IssueResponse> issueResponses = issues.getParams();

        ExecutionRequest executionRequest = executionRequestService.get(reportParams.getExecutionRequestUuid());
        List<IssueResponseAdapter> updatedIssues = updateModel(executionRequest, issueResponses);
        String topIssuesLink = getTopIssuesLink(executionRequest);

        return new HashMap<String, Object>() {
            {
                put("topIssues", updatedIssues);
                put("topIssuesLink", topIssuesLink);
            }
        };
    }

    private List<IssueResponseAdapter> updateModel(ExecutionRequest executionRequest,
                                                   List<IssueResponse> issueResponses) {
        return issueResponses
                .stream()
                .map(issueResponse -> IssueResponseAdapter.builder()
                        .issueResponse(issueResponse)
                        .testRuns(issueResponse.getTestRuns()
                                .stream()
                                .map(testRun -> TestRunAdapter.builder()
                                        .testRunResponse(testRun)
                                        .url(getTestRunUrl(testRun.getUuid(), executionRequest))
                                        .build()
                                )
                                .collect(Collectors.toList())
                        )
                        .failPattern(issueResponse.getFailPattern() == null ? null : FailPatternAdapter.builder()
                                .failPatternResponse(issueResponse.getFailPattern())
                                .url(getFailPatternUrl(executionRequest)).build())
                        .tickets(getTicketAdapters(issueResponse.getJiraTickets()))
                        .message(formatMessage(issueResponse.getMessage()))
                        .build()
                )
                .collect(Collectors.toList());
    }

    private List<JiraTicketAdapter> getTicketAdapters(List<String> jiraTickets) {
        if (jiraTickets != null && !jiraTickets.isEmpty()) {
            return jiraTickets
                    .stream()
                    .map(jiraTicket -> {
                        Pattern pattern = Pattern.compile("[a-zA-Z-.0-9]*$");
                        Matcher matcher = pattern.matcher(jiraTicket);
                        return JiraTicketAdapter
                                .builder().url(jiraTicket)
                                .name(matcher.find() ? matcher.group(0) : "").build();
                    }).collect(Collectors.toList());
        } else {
            return null;
        }
    }

    private String getTestRunUrl(UUID testRunId, ExecutionRequest executionRequest) {
        return baseUrl + ApiPath.PROJECT_PATH + "/" + executionRequest.getProjectId()
                + ApiPath.REPORT_EXECUTION_REQUESTS_PATH + "/" + executionRequest.getUuid() + "?node=" + testRunId;
    }

    private String getFailPatternUrl(ExecutionRequest executionRequest) {
        return catalogueUrl + ApiPath.PROJECT_PATH + "/" + executionRequest.getProjectId()
                + ApiPath.TEST_CASE_MANAGEMENT_PATH + ApiPath.FAIL_PATTERNS_PATH;
    }

    private String getTopIssuesLink(ExecutionRequest executionRequest) {
        return catalogueUrl + ApiPath.PROJECT_PATH + "/" + executionRequest.getProjectId()
                + ApiPath.REPORT_EXECUTION_REQUESTS_PATH + "/" + executionRequest.getUuid() + TOP_ISSUES_PATH;
    }

    private String formatMessage(String message) {
        if (!StringUtils.isBlank(message) && message.length() > MESSAGE_LENGTH_LIMIT) {
            message = message.substring(0, MESSAGE_LENGTH_LIMIT).concat(ELONGATED_MESSAGE_SUFFIX)
                    .replaceAll(LT_SYMBOL, LT_SYMBOL_ESCAPE)
                    .replaceAll(GT_SYMBOL, GT_SYMBOL_ESCAPE);
        }

        return message;
    }

    @Override
    public WidgetType getType() {
        return WidgetType.TOP_ISSUES;
    }
}
