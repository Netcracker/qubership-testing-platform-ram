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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.atp.ram.dto.response.ExecutionSummaryResponse;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.Project;
import org.qubership.atp.ram.service.mail.ReportParams;
import org.qubership.atp.ram.service.template.impl.AbstractWidgetModelBuilder;
import org.qubership.atp.ram.service.template.impl.ExecutionSummaryWidgetModelBuilder;
import org.qubership.atp.ram.services.CatalogueService;
import org.qubership.atp.ram.services.ExecutionRequestService;
import org.qubership.atp.ram.services.ProjectsService;
import org.qubership.atp.ram.services.ReportService;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ExecutionSummaryWidgetModelBuilderTest {

    ObjectMapper objectMapper = new ObjectMapper();

    private ReportService reportService = mock(ReportService.class);
    private ProjectsService projectsService = mock(ProjectsService.class);
    private CatalogueService catalogueService = mock(CatalogueService.class);
    private ExecutionRequestService executionRequestService = mock(ExecutionRequestService.class);
    private ReportParams reportParams;


    AbstractWidgetModelBuilder builder = new ExecutionSummaryWidgetModelBuilder(reportService, projectsService,
            executionRequestService, objectMapper, catalogueService);

    @BeforeEach
    public void setUp(){
        Project project = new Project();
        project.setTimeFormat("hh:mm");
        project.setDateFormat("d MMM yyyy");
        project.setTimeZone("GMT+03:00");
        when(reportService
                .getExecutionSummary(any(ExecutionRequest.class), anyBoolean()))
                .thenReturn(createFullExecutionSummary());
        when(executionRequestService.get(any())).thenReturn(createExecutionRequest());
        when(projectsService.get(any())).thenReturn(project);
        reportParams = new ReportParams();
        reportParams.setExecutionRequestUuid(UUID.randomUUID());
    }

    private ExecutionRequest createExecutionRequest() {
        ExecutionRequest executionRequest = new ExecutionRequest();
        executionRequest.setProjectId(UUID.randomUUID());
        return executionRequest;
    }

    private ExecutionSummaryResponse createFullExecutionSummary() {
        ExecutionSummaryResponse response = new ExecutionSummaryResponse();
        response.setName("Execution Request #1");
        response.setStartDate(Timestamp.from(Instant.now().minusSeconds(3600)));
        response.setFinishDate(Timestamp.from(Instant.now()));
        response.setDuration(3600);
        response.setThreads(16);
        response.setTestCasesCount(100);
        response.setPassedRate(45);
        response.setFailedRate(34);
        response.setWarningRate(12);
        response.setPassedCount(75);
        response.setFailedCount(20);
        response.setWarningCount(5);
        response.setEnvironmentLink("http://some-domain.com");
        response.setBrowserSessionLink(new ArrayList<String>(){{
            add("http://some-domain.com");
        }});

        return response;
    }

    @Test
    public void onExecutionSummaryWidgetModelBuilder_whenGetModel_AllDataStructureAdded(){
        Map<String, Object> model = builder.getModel(reportParams);

        Assertions.assertNotNull(model);
        Assertions.assertNotNull(model.get("name"));
        Assertions.assertNotNull(model.get("executionRequestLink"));
        Assertions.assertNotNull(model.get("startDate"));
        Assertions.assertNotNull(model.get("finishDate"));
        Assertions.assertNotNull(model.get("testCasesCount"));
        Assertions.assertNotNull(model.get("passedRate"));
        Assertions.assertNotNull(model.get("passedCount"));
        Assertions.assertNotNull(model.get("warningRate"));
        Assertions.assertNotNull(model.get("warningCount"));
        Assertions.assertNotNull(model.get("failedRate"));
        Assertions.assertNotNull(model.get("failedCount"));
        Assertions.assertNotNull(model.get("duration"));
        Assertions.assertNotNull(model.get("browserSessionLink"));
        Assertions.assertNotNull(model.get("environmentLink"));
        Assertions.assertNotNull(model.get("threads"));

    }

}
