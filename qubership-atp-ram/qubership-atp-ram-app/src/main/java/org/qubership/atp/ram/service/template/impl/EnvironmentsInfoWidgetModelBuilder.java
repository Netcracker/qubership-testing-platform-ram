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

import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;

import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.qubership.atp.auth.springbootstarter.exceptions.AtpEntityNotFoundException;
import org.qubership.atp.ram.dto.response.Environment;
import org.qubership.atp.ram.model.BaseSearchRequest;
import org.qubership.atp.ram.model.GridFsFileData;
import org.qubership.atp.ram.models.EnvironmentsInfo;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.SsmMetricReports;
import org.qubership.atp.ram.models.SystemInfo;
import org.qubership.atp.ram.models.ToolsInfo;
import org.qubership.atp.ram.models.WdShells;
import org.qubership.atp.ram.models.WidgetType;
import org.qubership.atp.ram.service.mail.ReportParams;
import org.qubership.atp.ram.service.template.SystemInfoAdapter;
import org.qubership.atp.ram.service.template.SystemStatusColor;
import org.qubership.atp.ram.services.EnvironmentsInfoService;
import org.qubership.atp.ram.services.EnvironmentsService;
import org.qubership.atp.ram.services.ExecutionRequestService;
import org.qubership.atp.ram.utils.StreamUtils;
import org.qubership.atp.ram.utils.TimeUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EnvironmentsInfoWidgetModelBuilder extends AbstractWidgetModelBuilder {

    private static final int COLUMNS_IN_ROW = 3;
    private final EnvironmentsInfoService environmentsInfoService;
    private final ExecutionRequestService executionRequestService;
    private final EnvironmentsService environmentsService;
    private final ObjectMapper objectMapper;

    @Value("${catalogue.url}")
    private String catalogueUrl;

    @Override
    public Map<String, Object> buildModel(ReportParams reportParams) {
        Map<String, Object> model;
        try {
            EnvironmentsInfo environmentsInfo =
                    environmentsInfoService.findByExecutionRequestId(reportParams.getExecutionRequestUuid());

            model = toMap(environmentsInfo);
            postProcess(model, environmentsInfo);
        } catch (AtpEntityNotFoundException e) {
            model = Collections.EMPTY_MAP;
        }
        return model;
    }

    private void postProcess(Map<String, Object> model, EnvironmentsInfo environmentsInfo) {
        String formatedDuration = DurationFormatUtils.formatDuration(
                environmentsInfo.getDuration(),
                "HH:mm:ss",
                true);
        model.put("duration", formatedDuration);
        model.put("startDate", TimeUtils.formatDateTime(environmentsInfo.getStartDate(),
                TimeUtils.DEFAULT_DATE_TIME_PATTERN));
        model.put("endDate", TimeUtils.formatDateTime(environmentsInfo.getEndDate(),
                TimeUtils.DEFAULT_DATE_TIME_PATTERN));

        String status = environmentsInfo.getStatus();
        model.put("statusBgColor", SystemStatusColor.valueOf(status).getHtmlColor());

        final UUID executionRequestId = environmentsInfo.getExecutionRequestId();
        final ExecutionRequest executionRequest = executionRequestService.get(executionRequestId);
        final UUID projectId = executionRequest.getProjectId();
        final String catalogEnvLink = catalogueUrl + "/project/" + projectId + "/environments";

        final UUID environmentId = executionRequest.getEnvironmentId();
        final UUID taToolsGroupId = executionRequest.getTaToolsGroupId();

        BaseSearchRequest searchRequest = BaseSearchRequest.builder()
                .ids(asList(environmentId, taToolsGroupId))
                .build();
        List<Environment> environments = environmentsService.searchEnvironments(searchRequest);
        Map<UUID, Environment> environmentMap = StreamUtils.toIdEntityMap(environments, Environment::getId);

        Environment environment = environmentMap.get(environmentId);
        if (nonNull(environment)) {
            String environmentLink = catalogEnvLink + "/environment/" + environmentId;
            model.put("environmentLink", environmentLink);
            model.put("environmentName", environment.getName());
        }

        Environment taToolsGroup = environmentMap.get(taToolsGroupId);
        if (nonNull(taToolsGroup)) {
            String toolGroupLink = catalogEnvLink + "/tool/" + taToolsGroupId;
            model.put("toolGroupLink", toolGroupLink);
            model.put("toolGroupName", taToolsGroup.getName());
        }

        UUID mandatoryChecksReportId = environmentsInfo.getMandatoryChecksReportId();
        String mandatoryChecksReportName;
        try {
            GridFsFileData report = environmentsInfoService.getReportById(mandatoryChecksReportId);
            mandatoryChecksReportName = report.getFileName();
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Failed to load mandatory checks report file");
        }
        if (nonNull(mandatoryChecksReportId) && nonNull(mandatoryChecksReportName)) {
            String reportLink = catalogueUrl + "/redirect-uri/api/atp-ram/v1/api/environmentsInfo"
                    + "/mandatoryChecksReport/" + mandatoryChecksReportId;
            model.put("mandatoryChecksReportLink", reportLink);
            model.put("mandatoryChecksReportName", mandatoryChecksReportName);
        }

        final SsmMetricReports ssmMetricReports = environmentsInfo.getSsmMetricReports();

        if (nonNull(ssmMetricReports)) {
            final UUID microservicesReportId = ssmMetricReports.getMicroservicesReportId();
            if (nonNull(microservicesReportId)) {
                String reportLink = catalogueUrl + "/redirect-uri/api/atp-ram/v1/api/environmentsInfo"
                        + "/mandatoryChecksReport/" + microservicesReportId;
                model.put("ssmMetricsMicroservicesReportLink", reportLink);
                model.put("ssmMetricsMicroservicesReportName", "Microservices.json");
            }

            final UUID problemContextReportId = ssmMetricReports.getProblemContextReportId();
            if (nonNull(problemContextReportId)) {
                String reportLink = catalogueUrl + "/redirect-uri/api/atp-ram/v1/api/environmentsInfo"
                        + "/mandatoryChecksReport/" + problemContextReportId;
                model.put("ssmMetricsProblemContextReportLink", reportLink);
                model.put("ssmMetricsProblemContextReportName", "Problem context.json");
            }

            buildQaSystemInfo(model, environmentsInfo);
            buildTaSystemInfo(model, environmentsInfo);
            buildWdhellsModel(model, environmentsInfo);
        }
    }

    private void buildQaSystemInfo(Map<String, Object> model, EnvironmentsInfo environmentsInfo) {
        List<SystemInfo> qaSystemInfoList = environmentsInfo.getQaSystemInfoList();
        if (qaSystemInfoList != null) {
            model.put("qaSystemInfoList", toListOfSystemInfoAdapters(qaSystemInfoList));
        }
    }

    private void buildTaSystemInfo(Map<String, Object> model, EnvironmentsInfo environmentsInfo) {
        List<SystemInfo> taSystemInfoList = environmentsInfo.getTaSystemInfoList();
        if (taSystemInfoList != null) {
            model.put("taSystemInfoList", toListOfSystemInfoAdapters(taSystemInfoList));
        }
    }

    private void buildWdhellsModel(Map<String, Object> model, EnvironmentsInfo environmentsInfo) {
        ToolsInfo toolsInfo = environmentsInfo.getToolsInfo();
        if (toolsInfo != null && toolsInfo.getWdShells() != null) {
            List<List<WdShells>> partitions = Lists.partition(toolsInfo.getWdShells(), COLUMNS_IN_ROW);
            List<WdShellsTableAdapter> partitionedWdShells = partitions
                    .stream()
                    .map(WdShellsTableAdapter::new)
                    .collect(Collectors.toList());
            model.put("wdShellTables", partitionedWdShells);
        }
    }

    private List<SystemInfoAdapter> toListOfSystemInfoAdapters(List<SystemInfo> systemInfos) {
        return systemInfos.stream()
                .map(SystemInfoAdapter::new)
                .collect(Collectors.toList());
    }

    @Override
    public WidgetType getType() {
        return WidgetType.ENVIRONMENTS_INFO;
    }

    private Map<String, Object> toMap(EnvironmentsInfo environmentsInfo) {
        return objectMapper.convertValue(environmentsInfo, Map.class);
    }
}
