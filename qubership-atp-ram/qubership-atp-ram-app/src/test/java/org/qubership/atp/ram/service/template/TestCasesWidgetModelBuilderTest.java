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

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.atp.auth.springbootstarter.feign.exception.FeignClientException;
import org.qubership.atp.ram.dto.response.ExecutionRequestWidgetConfigTemplateResponse;
import org.qubership.atp.ram.dto.response.LabelNodeReportResponse;
import org.qubership.atp.ram.dto.response.LabelNodeReportResponse.FailedLogRecordNodeResponse;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.model.DataSet;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.FinalRunData;
import org.qubership.atp.ram.models.TestPlan;
import org.qubership.atp.ram.models.WidgetConfigTemplate;
import org.qubership.atp.ram.models.WidgetType;
import org.qubership.atp.ram.service.mail.ReportParams;
import org.qubership.atp.ram.service.template.impl.AbstractWidgetModelBuilder;
import org.qubership.atp.ram.service.template.impl.TestCasesWidgetModelBuilder;
import org.qubership.atp.ram.services.CatalogueService;
import org.qubership.atp.ram.services.DataSetService;
import org.qubership.atp.ram.services.ExecutionRequestService;
import org.qubership.atp.ram.services.ReportService;
import org.qubership.atp.ram.services.WidgetConfigTemplateService;

public class TestCasesWidgetModelBuilderTest {

    private ReportParams reportParams;
    private final ReportService reportService = mock(ReportService.class);
    private final ExecutionRequestService executionRequestService = mock(ExecutionRequestService.class);
    private final DataSetService dataSetService = mock(DataSetService.class);
    private final WidgetConfigTemplateService widgetConfigTemplateService = mock(WidgetConfigTemplateService.class);
    private final CatalogueService catalogueService = mock(CatalogueService.class);

    private final AbstractWidgetModelBuilder builder = new TestCasesWidgetModelBuilder(
            catalogueService, executionRequestService, dataSetService, reportService, widgetConfigTemplateService);

    @BeforeEach
    public void setUp() {
        reportParams = createReportParams();
        when(reportService.getTestCasesForExecutionRequest(any(), any(), any(), anyBoolean(), any()))
                .thenReturn(createTestCases(true));
        when(executionRequestService.get(any())).thenReturn(createExecutionRequest());
        when(widgetConfigTemplateService.getWidgetConfigTemplateForEr(reportParams.getExecutionRequestUuid())).thenReturn(new ExecutionRequestWidgetConfigTemplateResponse(new WidgetConfigTemplate(), false));
        when(catalogueService.getTestPlan(any())).thenReturn(new TestPlan());
    }

    private List<DataSet> createDataSetResponse() {
        return Collections.singletonList(new DataSet(UUID.randomUUID(), "Data Set #1"));
    }

    private LabelNodeReportResponse createTestCases(boolean setChildren) {
        LabelNodeReportResponse labelNodeReportResponse = new LabelNodeReportResponse();
        labelNodeReportResponse.setStatus(TestingStatuses.PASSED);
        labelNodeReportResponse.setDuration(36000);
        labelNodeReportResponse.setLabelName("Test Lable Name");
        labelNodeReportResponse.setPassedRate(56);
        labelNodeReportResponse.setTestRuns(getTestRuns());

        if (setChildren) {
            labelNodeReportResponse.setChildren(getChildren());
        }
        return labelNodeReportResponse;
    }

    private List<LabelNodeReportResponse> getChildren() {
        LabelNodeReportResponse children = createTestCases(false);
        return Collections.singletonList(children);
    }

    private List<LabelNodeReportResponse.TestRunNodeResponse> getTestRuns() {
        LabelNodeReportResponse.TestRunNodeResponse testRunNodeResponse = new LabelNodeReportResponse.TestRunNodeResponse();
        testRunNodeResponse.setUuid(UUID.randomUUID());
        testRunNodeResponse.setName("Test Run");
        testRunNodeResponse.setDuration(3600);
        testRunNodeResponse.setTestingStatus(TestingStatuses.PASSED);
        testRunNodeResponse.setTestCaseId(UUID.randomUUID());
        testRunNodeResponse.setDataSetUrl("c87c5dec-e25e-4c11-a2d4-7d6b84dda5ef");
        testRunNodeResponse.setPassedRate(32);
        testRunNodeResponse.setFailureReason("Internal Server Error");
        testRunNodeResponse.setFailedStep(new ArrayList<FailedLogRecordNodeResponse>() {{
            FailedLogRecordNodeResponse logRecord = new FailedLogRecordNodeResponse();
            logRecord.setName("Failed Test Run");

            add(logRecord);
        }});
        testRunNodeResponse.setFinalRun(createFinalRunData());
        return Collections.singletonList(testRunNodeResponse);
    }

    private ExecutionRequest createExecutionRequest() {
        ExecutionRequest executionRequest = new ExecutionRequest();
        executionRequest.setProjectId(UUID.randomUUID());
        return executionRequest;
    }

    private ReportParams createReportParams() {
        ReportParams reportParams = new ReportParams();
        reportParams.setExecutionRequestUuid(UUID.randomUUID());
        reportParams.setRecipients("example@example.com");
        reportParams.setSubject("Test Subject");
        reportParams.setDescriptions(new HashMap<String, String>() {{
            put(WidgetType.SERVER_SUMMARY.toString(), "Test description");
        }});

        return reportParams;
    }

    private FinalRunData createFinalRunData() {
        FinalRunData finalRunData = new FinalRunData();
        finalRunData.setTestRunId(UUID.randomUUID());
        finalRunData.setExecutionRequestId(UUID.randomUUID());
        return finalRunData;
    }

    @Test
    public void onTestCasesWidgetModelBuilder_whenGetModel_AllDataStructureAdded() {
        Map<String, Object> model = builder.getModel(reportParams);

        when(dataSetService.getDataSetsByIds(any())).thenReturn(createDataSetResponse());

        Assertions.assertNotNull(model);
        Assertions.assertNotNull(model.get("tableModel"));
    }

    @Test
    public void onTestCasesWidgetModelBuilder_whenRemovedDataSetAndHaveThrowFeignException_correctlyCollectReportWithOutDataSet() {

        when(dataSetService.getDataSetsByIds(any())).thenThrow(FeignClientException.class);

        Map<String, Object> model = builder.getModel(reportParams);
        Assertions.assertEquals("Test Cases", model.get("title"));
    }

}
