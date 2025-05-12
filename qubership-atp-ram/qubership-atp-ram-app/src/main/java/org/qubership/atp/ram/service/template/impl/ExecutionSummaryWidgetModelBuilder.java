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

import static java.util.Objects.isNull;

import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.qubership.atp.ram.config.ApiPath;
import org.qubership.atp.ram.dto.response.ExecutionSummaryResponse;
import org.qubership.atp.ram.dto.response.ProjectDataResponse;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.Project;
import org.qubership.atp.ram.models.WidgetType;
import org.qubership.atp.ram.service.mail.ReportParams;
import org.qubership.atp.ram.services.CatalogueService;
import org.qubership.atp.ram.services.ExecutionRequestService;
import org.qubership.atp.ram.services.ProjectsService;
import org.qubership.atp.ram.services.ReportService;
import org.qubership.atp.ram.utils.TimeUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;


@Component
@Slf4j
public class ExecutionSummaryWidgetModelBuilder extends AbstractWidgetModelBuilder {

    @Value("${catalogue.url}")
    private String baseUrl;

    private ReportService reportService;
    private ProjectsService projectsService;
    private ExecutionRequestService executionRequestService;
    private ObjectMapper objectMapper;
    private CatalogueService catalogueService;

    /**
     * Constructor.
     * @param reportService instance of {@link ReportService}
     * @param executionRequestService instance of {@link ExecutionRequestService}
     * @param objectMapper instance of {@link ObjectMapper}
     */
    public ExecutionSummaryWidgetModelBuilder(ReportService reportService,
                                              ProjectsService projectsService,
                                              ExecutionRequestService executionRequestService,
                                              ObjectMapper objectMapper,
                                              CatalogueService catalogueService) {
        this.reportService = reportService;
        this.projectsService = projectsService;
        this.executionRequestService = executionRequestService;
        this.objectMapper = objectMapper;
        this.catalogueService = catalogueService;
    }

    @Override
    protected Map<String, Object> buildModel(ReportParams reportParams) {
        UUID executionRequestId = reportParams.getExecutionRequestUuid();
        ExecutionRequest executionRequest = executionRequestService.get(executionRequestId);

        ExecutionSummaryResponse executionSummary = reportService
                .getExecutionSummary(executionRequest, reportParams.isExecutionSummaryRunsSummary());

        UUID projectId = executionRequest.getProjectId();
        Project project = projectsService.get(projectId);

        log.debug("executionSummary = {} found for reportParams = {}", executionSummary, reportParams);

        Map<String, Object> model = toMap(executionSummary);

        postProcess(project, model, executionSummary, reportParams);

        return model;
    }

    private void postProcess(Project project,
                             Map<String, Object> model,
                             ExecutionSummaryResponse executionSummary,
                             ReportParams reportParams) {
        String formatedDuration = DurationFormatUtils.formatDuration(
                executionSummary.getDuration() * 1000,
                "HH:mm:ss",
                true);
        model.put("duration", formatedDuration);

        setProjectDataFromCatalogIfDateTimeFormatNull(project);

        String dateTimeFormat = String.format("%s %s", project.getDateFormat(), project.getTimeFormat());
        String timeZone = project.getTimeZone();

        Timestamp startDate = executionSummary.getStartDate();
        model.put("startDate", TimeUtils.formatDateTime(startDate, dateTimeFormat, timeZone));

        Timestamp finishDate = executionSummary.getFinishDate();
        model.put("finishDate",TimeUtils.formatDateTime(finishDate, dateTimeFormat, timeZone));

        String executionRequestLink = getErLink(reportParams);
        model.put("executionRequestLink", executionRequestLink);
    }

    private void setProjectDataFromCatalogIfDateTimeFormatNull(Project project) {
        if (isNull(project.getDateFormat()) || isNull(project.getTimeFormat()) || isNull(project.getTimeZone())) {
            ProjectDataResponse projectData = catalogueService.getProjectData(project.getUuid());
            project.setDateFormat(projectData.getDateFormat());
            project.setTimeFormat(projectData.getTimeFormat());
            project.setTimeZone(projectData.getTimeZone());
            projectsService.save(project);
        }
    }

    private String getErLink(ReportParams reportParams) {
        ExecutionRequest executionRequest = executionRequestService.get(reportParams.getExecutionRequestUuid());
        return baseUrl + ApiPath.PROJECT_PATH + '/' + executionRequest.getProjectId()
                + ApiPath.REPORT_EXECUTION_REQUESTS_PATH + '/' + reportParams.getExecutionRequestUuid();
    }

    @Override
    public WidgetType getType() {
        return WidgetType.EXECUTION_SUMMARY;
    }


    private Map<String, Object> toMap(ExecutionSummaryResponse executionSummaryResponse) {
        return objectMapper.convertValue(executionSummaryResponse, Map.class);
    }
}
