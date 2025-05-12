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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.qubership.atp.ram.dto.response.RootCausesStatisticResponse;
import org.qubership.atp.ram.models.Project;
import org.qubership.atp.ram.models.WidgetType;
import org.qubership.atp.ram.service.mail.ReportParams;
import org.qubership.atp.ram.services.ExecutionRequestService;
import org.qubership.atp.ram.services.ProjectsService;
import org.qubership.atp.ram.services.ReportService;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RootCauseWidgetModelBuilder extends AbstractWidgetModelBuilder {

    public static final String ROOT_CAUSE_STATISCTICS_MODEL_KEY = "statistics";

    private final ReportService reportService;
    private final ExecutionRequestService executionRequestService;
    private final ProjectsService projectsService;

    @Override
    protected Map<String, Object> buildModel(ReportParams reportParams) {
        UUID executionRequestId = reportParams.getExecutionRequestUuid();
        Project project = executionRequestService.getProjectId(executionRequestId);

        List<RootCausesStatisticResponse> rootCausesStatistic =
                reportService.getRootCausesStatisticForExecutionRequestAndPrevious(executionRequestId);

        List<RootCausesStatisticResponseAdapter> model =
                rootCausesStatistic
                        .stream()
                        .map(rootCausesStatisticResponse ->
                                new RootCausesStatisticResponseAdapter(rootCausesStatisticResponse, project))
                        .collect(Collectors.toList());

        return new HashMap<String, Object>() {
            {
                put(ROOT_CAUSE_STATISCTICS_MODEL_KEY, model);
            }
        };
    }

    @Override
    public WidgetType getType() {
        return WidgetType.ROOT_CAUSES_STATISTIC;
    }
}
