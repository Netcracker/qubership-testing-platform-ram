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

package org.qubership.atp.ram.service.template;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.qubership.atp.ram.dto.response.ExecutionRequestWidgetConfigTemplateResponse;
import org.qubership.atp.ram.dto.response.FailPatternResponse;
import org.qubership.atp.ram.dto.response.IssueResponse;
import org.qubership.atp.ram.dto.response.IssueResponsesModel;
import org.qubership.atp.ram.dto.response.IssueTestRunResponse;
import org.qubership.atp.ram.dto.response.RamObjectResponse;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.WidgetConfigTemplate;
import org.qubership.atp.ram.models.WidgetType;
import org.qubership.atp.ram.service.mail.ReportParams;
import org.qubership.atp.ram.service.template.impl.IssueResponseAdapter;
import org.qubership.atp.ram.service.template.impl.TopIssuesWidgetModelBuilder;
import org.qubership.atp.ram.services.ExecutionRequestService;
import org.qubership.atp.ram.services.IssueService;
import org.qubership.atp.ram.services.WidgetConfigTemplateService;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.Ordering;

@ExtendWith(SpringExtension.class)
public class TopIssuesWidgetModelBuilderTest {

    @Mock
    private IssueService issueService;

    @Mock
    private ExecutionRequestService executionRequestService;

    @Mock
    private WidgetConfigTemplateService widgetConfigTemplateService;

    @InjectMocks
    private TopIssuesWidgetModelBuilder builder;

    private ReportParams reportParams;

    private IssueResponsesModel issues;

    @BeforeEach
    public void setUp(){
        reportParams = createReportParams();
        issues = createIssues(TopIssuesWidgetModelBuilder.TOP_ISSUES_DEFAULT_SIZE_LIMIT + 2);
        when(issueService.getAllIssuesByExecutionRequestId(
                any(UUID.class), anyInt(), anyInt(), anyString(), anyString())).thenReturn(issues);
        when(executionRequestService.get(any())).thenReturn(createExecutionRequest());
        when(widgetConfigTemplateService.getWidgetConfigTemplateForEr(reportParams.getExecutionRequestUuid()))
                .thenReturn(new ExecutionRequestWidgetConfigTemplateResponse(new WidgetConfigTemplate(), false));
    }

    private ExecutionRequest createExecutionRequest() {
        ExecutionRequest executionRequest = new ExecutionRequest();
        executionRequest.setProjectId(UUID.randomUUID());
        return executionRequest;
    }

    private IssueResponsesModel createIssues( int numberOfIssues) {
        List<IssueResponse> testRuns = IntStream
                .range(0, numberOfIssues)
                .mapToObj(this::createIssueResponse)
                .collect(Collectors.toList());
        return new IssueResponsesModel(numberOfIssues, testRuns);
    }

    private IssueResponse createIssueResponse(Integer index) {
        IssueResponse issueResponse = new IssueResponse();
        issueResponse.setUuid(UUID.randomUUID());
        issueResponse.setMessage("Error Message " + index);
        issueResponse.setTestRuns(new ArrayList<IssueTestRunResponse>(){{
            IssueTestRunResponse issueTestRunResponse = new IssueTestRunResponse();
            issueTestRunResponse.setUuid(UUID.randomUUID());
            issueTestRunResponse.setName("Test Run #" + index);
            add(issueTestRunResponse);
        }});
        issueResponse.setJiraTickets(Collections.singletonList("https://service-address/browse/PRJ-98765"));
        FailPatternResponse failPattern = new FailPatternResponse();
        failPattern.setName("FailPatternName");
        failPattern.setUuid(UUID.randomUUID());
        issueResponse.setFailPattern(failPattern);
        RamObjectResponse failReason = new RamObjectResponse();
        failReason.setName("Big Error");
        failReason.setUuid(UUID.randomUUID());
        issueResponse.setFailReason(failReason);
        return issueResponse;
    }

    private ReportParams createReportParams() {
        ReportParams reportParams = new ReportParams();
        reportParams.setExecutionRequestUuid(UUID.randomUUID());
        reportParams.setRecipients("example@example.com");
        reportParams.setSubject("[E2E017] Top Issues widget");
        reportParams.setDescriptions(new HashMap<String, String>(){{
            put(WidgetType.TOP_ISSUES.toString(), "Test description for top issues");
        }});

        return reportParams;
    }

    @Test
    public void onServerSummaryWidgetModelBuilder_whenGetModel_AllDataStructureAdded(){
        Map<String, Object> model = builder.getModel(reportParams);

        Assertions.assertNotNull(model);
        List<IssueResponseAdapter> topIssues = (List<IssueResponseAdapter>) model.get("topIssues");
        Assertions.assertNotNull(topIssues);
        Assertions.assertEquals("FailPatternName", topIssues.get(0).getFailPattern());
        Assertions.assertEquals("Error Message 0", topIssues.get(0).getErrorMessage());
        Assertions.assertEquals("Big Error", topIssues.get(0).getFailReason());
        Assertions.assertEquals("PRJ-98765", topIssues.get(0).getTickets().get(0).getName());
    }

    @Test
    public void onServerSummaryWidgetModelBuilder_whenGetModel_IssuesSortedAndLimited(){
        Map<String, Object> model = builder.getModel(reportParams);
        List<IssueResponseAdapter> topIssues = (List<IssueResponseAdapter>)model.get("topIssues");

        List<Integer> runCounts = topIssues.stream().map(IssueResponseAdapter::getTestRunsCount).collect(Collectors.toList());
        Assertions.assertTrue(Ordering.natural().reverse().isOrdered(runCounts));
    }

    @Test
    public void onServerSummaryWidgetModelBuilder_whenGetModelWithNullFailPattern_AllDataStructureAdded(){
        IssueResponsesModel responseWithNullFailPattern = createIssuesWithNullFailPattern();
        when(issueService.getAllIssuesByExecutionRequestId(
                any(UUID.class), anyInt(), anyInt(), anyString(), anyString())).thenReturn(responseWithNullFailPattern);
        Map<String, Object> model = builder.getModel(reportParams);

        Assertions.assertNotNull(model);
        List<IssueResponseAdapter> topIssues = (List<IssueResponseAdapter>) model.get("topIssues");
        Assertions.assertNotNull(topIssues);
        Assertions.assertEquals("", topIssues.get(0).getFailPattern());
        Assertions.assertEquals("Error Message 0", topIssues.get(0).getErrorMessage());
        Assertions.assertEquals("Big Error", topIssues.get(0).getFailReason());
        Assertions.assertEquals("PRJ-98765", topIssues.get(0).getTickets().get(0).getName());
    }

    private IssueResponsesModel createIssuesWithNullFailPattern() {
        List<IssueResponse> testRuns = new ArrayList<>();
        IssueResponse issueResponseWithNullFailPattern = createIssueResponse(0);
        issueResponseWithNullFailPattern.setFailPattern(null);
        testRuns.add(issueResponseWithNullFailPattern);
        return new IssueResponsesModel(1, testRuns);
    }

    @Test
    public void onServerSummaryWidgetModelBuilder_whenGetModelWithNullFailReason_AllDataStructureAdded(){
        IssueResponsesModel responseWithNullFailReason = createIssuesWithNullFailReason();

        when(issueService.getAllIssuesByExecutionRequestId(
                any(UUID.class), anyInt(), anyInt(), anyString(), anyString())).thenReturn(responseWithNullFailReason);
        Map<String, Object> model = builder.getModel(reportParams);

        Assertions.assertNotNull(model);
        List<IssueResponseAdapter> topIssues = (List<IssueResponseAdapter>) model.get("topIssues");
        Assertions.assertNotNull(topIssues);
        Assertions.assertEquals("FailPatternName", topIssues.get(0).getFailPattern());
        Assertions.assertEquals("Error Message 0", topIssues.get(0).getErrorMessage());
        Assertions.assertEquals("", topIssues.get(0).getFailReason());
        Assertions.assertEquals("PRJ-98765", topIssues.get(0).getTickets().get(0).getName());
    }

    private IssueResponsesModel createIssuesWithNullFailReason() {
        List<IssueResponse> testRuns = new ArrayList<>();
        IssueResponse issueResponseWithNullFailReason = createIssueResponse(0);
        issueResponseWithNullFailReason.setFailReason(null);
        testRuns.add(issueResponseWithNullFailReason);
        return new IssueResponsesModel(1, testRuns);
    }
}
