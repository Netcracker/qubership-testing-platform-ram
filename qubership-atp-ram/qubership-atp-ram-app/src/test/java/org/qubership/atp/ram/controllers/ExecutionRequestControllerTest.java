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

package org.qubership.atp.ram.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;
import org.qubership.atp.ram.config.MvcConfig;
import org.qubership.atp.ram.dto.request.ExecutionRequestsForCompareScreenshotsRequest;
import org.qubership.atp.ram.model.ExecutionRequestsCompareScreenshotResponse;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.service.rest.server.mongo.ExecutionRequestController;
import org.qubership.atp.ram.service.template.impl.ScreenshotsReportTemplateRenderService;
import org.qubership.atp.ram.services.EnvironmentsService;
import org.qubership.atp.ram.services.ExecutionRequestCompareService;
import org.qubership.atp.ram.services.ExecutionRequestDetailsService;
import org.qubership.atp.ram.services.ExecutionRequestReportingService;
import org.qubership.atp.ram.services.ExecutionRequestService;
import org.qubership.atp.ram.services.IssueService;
import org.qubership.atp.ram.services.JointExecutionRequestService;
import org.qubership.atp.ram.services.OrchestratorService;
import org.qubership.atp.ram.services.TestRunService;
import org.qubership.atp.ram.services.WidgetConfigTemplateService;
import org.qubership.atp.ram.testdata.EntitiesGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.template.Configuration;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = ExecutionRequestController.class,
        properties = {"spring.cloud.consul.config.enabled=false"})
@ContextConfiguration(classes = {
        ExecutionRequestControllerTest.TestApp.class,
        ExecutionRequestController.class,
        MvcConfig.class})
@AutoConfigureMockMvc(addFilters = false)
@Isolated
public class ExecutionRequestControllerTest {

    @SpringBootApplication
    public static class TestApp {
    }

    @MockBean
    private ExecutionRequestService service;

    @MockBean
    private ExecutionRequestReportingService executionRequestReportingService;

    @MockBean
    private ExecutionRequestDetailsService executionRequestDetailsService;

    @MockBean
    private IssueService issueService;

    @MockBean
    private OrchestratorService orchestratorService;

    @MockBean
    private WidgetConfigTemplateService widgetConfigTemplateService;

    @MockBean
    private TestRunService testRunService;

    @MockBean
    private ExecutionRequestCompareService executionRequestCompareService;

    @MockBean
    private ScreenshotsReportTemplateRenderService screenshotsReportTemplateRenderService;

    @MockBean
    private JointExecutionRequestService jointExecutionRequestService;

    @MockBean
    private EnvironmentsService environmentsService;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FreeMarkerConfigurer freeMarkerConfigurer;

    @MockBean
    private Configuration freemarkerConfiguration;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setUp() throws Exception {
        ExecutionRequest er = new ExecutionRequest();
        er.setUuid(UUID.randomUUID());
        er.setStartDate(new Timestamp(0));
        when(service.findById(any(UUID.class))).thenReturn(er);
    }

    @Test
    public void getExecutionRequest() throws Exception {
        byte[] result = mockMvc
                .perform(MockMvcRequestBuilders.get("/api/executionrequests/" + UUID.randomUUID()))
                .andReturn().getResponse().getContentAsByteArray();

        Assertions.assertNotNull(result);
        Assertions.assertNotEquals(0, result.length);

        ExecutionRequest executionRequest = objectMapper.readValue(result, ExecutionRequest.class);

        Timestamp expectedStartDate = new Timestamp(0);
        Assertions.assertEquals(expectedStartDate, executionRequest.getStartDate());
    }

    @Test
    public void getScreenshotsHtmlReport() throws Exception {
        ExecutionRequestsCompareScreenshotResponse response = EntitiesGenerator.generateExecutionRequestsCompareScreenshotResponse();
        UUID projectId = UUID.randomUUID();
        List<UUID> executionRequestIds = Arrays.asList(UUID.randomUUID(), UUID.randomUUID());
        ExecutionRequestsForCompareScreenshotsRequest request =
                new ExecutionRequestsForCompareScreenshotsRequest(projectId, executionRequestIds);
        when(executionRequestCompareService.getCompareScreenshotsExecutionRequests(
                executionRequestIds, true)).thenReturn(response);
        String expectedResult = "result";
        when(screenshotsReportTemplateRenderService.render(any())).thenReturn(expectedResult);
        MockHttpServletResponse result = mockMvc
                .perform(MockMvcRequestBuilders.post("/api/executionrequests/screenshots/compare-report/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse();

        Assertions.assertNotNull(result.getContentAsByteArray());
        Assertions.assertNotEquals(0, result.getContentAsByteArray().length);

        Assertions.assertTrue(Objects.requireNonNull(result.getHeader(HttpHeaders.CONTENT_DISPOSITION))
                .contains("attachment; filename="));
        Assertions.assertEquals(result.getHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS), HttpHeaders.CONTENT_DISPOSITION);
        Assertions.assertEquals(expectedResult, new String(result.getContentAsByteArray()));
    }
}
