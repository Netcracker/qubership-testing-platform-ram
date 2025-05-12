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

package org.qubership.atp.ram.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.qubership.atp.ram.ExecutionRequestsMock;
import org.qubership.atp.ram.dto.response.LabelNodeReportResponse;
import org.qubership.atp.ram.entities.treenodes.labelparams.TestingReportLabelParam;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.logrecords.parts.ValidationTableLine;
import org.springframework.mock.web.MockHttpServletResponse;

public class ReportExportServiceTest {
    private ReportExportService reportExportService;
    @Mock
    private ReportService reportService;
    @Mock
    private ExecutionRequestService executionRequestService;
    @Mock
    private WidgetConfigTemplateService widgetConfigTemplateService;

    private final static UUID executionRequestId = UUID.randomUUID();
    private final static UUID labelTemplateId = UUID.randomUUID();
    private final static UUID validationTemplateId = UUID.randomUUID();

    @BeforeEach
    public void init() {
        MockitoAnnotations.initMocks(this);
        reportExportService = spy(new ReportExportService(reportService, executionRequestService,
                widgetConfigTemplateService));
    }

    @Test
    public void exportTestCasesWidgetIntoCsv_WithoutLabels_CalledMethodToPrintTestRunNode() throws IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();

        LabelNodeReportResponse.TestRunNodeResponse testRunResponse = new LabelNodeReportResponse.TestRunNodeResponse();
        testRunResponse.setName("testRunResponse");
        ValidationTableLine step = new ValidationTableLine();
        step.setActualResult("ar");
        TestingReportLabelParam param = new TestingReportLabelParam("name", TestingStatuses.PASSED, step);
        testRunResponse.setLabelParams(Collections.singletonList(param));
        LabelNodeReportResponse nodes = new LabelNodeReportResponse();
        nodes.setTestRuns(Collections.singletonList(testRunResponse));

        ExecutionRequest executionRequest = ExecutionRequestsMock.generateRequestById(executionRequestId);

        when(reportService.getTestCasesForExecutionRequest(any(), any(), any(), anyBoolean(), any())).thenReturn(nodes);
        when(executionRequestService.get(any())).thenReturn(executionRequest);

        reportExportService.exportTestCasesWidgetIntoCsv(executionRequestId, labelTemplateId, validationTemplateId, false, response, null);

        verify(reportExportService, times(1)).printTestRunNode(any(), any(), any(), any(), any());

        assertTrue(response.isCommitted());
        assertNotNull(response.getHeader(ACCESS_CONTROL_EXPOSE_HEADERS));
        assertEquals(CONTENT_DISPOSITION, response.getHeader(ACCESS_CONTROL_EXPOSE_HEADERS));
        assertNotNull(response.getHeader(CONTENT_DISPOSITION));
        assertTrue(response.getHeader(CONTENT_DISPOSITION).contains(executionRequest.getName()));
    }

    @Test
    public void exportTestCasesWidgetIntoCsv_WithLabels_CalledMethodToPrintTestRunNode() throws IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        LabelNodeReportResponse.TestRunNodeResponse testRunResponse = new LabelNodeReportResponse.TestRunNodeResponse();
        testRunResponse.setName("testRunResponse");
        ValidationTableLine step = new ValidationTableLine();
        step.setActualResult("ar");
        TestingReportLabelParam param = new TestingReportLabelParam("name", TestingStatuses.PASSED, step);
        testRunResponse.setLabelParams(Collections.singletonList(param));

        LabelNodeReportResponse childReportResponse = new LabelNodeReportResponse();
        childReportResponse.setLabelName("label");
        childReportResponse.setTestRuns(Collections.singletonList(testRunResponse));
        childReportResponse.setLabelParams(Collections.singletonList(param));

        LabelNodeReportResponse nodes = new LabelNodeReportResponse();
        nodes.setChildren(Collections.singletonList(childReportResponse));

        ExecutionRequest executionRequest = ExecutionRequestsMock.generateRequestById(executionRequestId);

        when(reportService.getTestCasesForExecutionRequest(any(), any(), any(), anyBoolean(), any())).thenReturn(nodes);
        when(executionRequestService.get(any())).thenReturn(executionRequest);

        reportExportService.exportTestCasesWidgetIntoCsv(executionRequestId, labelTemplateId, validationTemplateId,
                false, response, null);

        verify(reportExportService, times(1)).printLabelNode(any(), any(), any(), any(), any());
        verify(reportExportService, times(1)).printTestRunNode(any(), any(), any(), any(), any());

        assertTrue(response.isCommitted());
        assertNotNull(response.getHeader(ACCESS_CONTROL_EXPOSE_HEADERS));
        assertEquals(CONTENT_DISPOSITION, response.getHeader(ACCESS_CONTROL_EXPOSE_HEADERS));
        assertNotNull(response.getHeader(CONTENT_DISPOSITION));
        assertTrue(response.getHeader(CONTENT_DISPOSITION).contains(executionRequest.getName()));
    }
}
