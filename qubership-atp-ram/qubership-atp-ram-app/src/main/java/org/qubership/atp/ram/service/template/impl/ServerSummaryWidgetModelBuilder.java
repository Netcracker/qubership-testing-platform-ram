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

import org.qubership.atp.ram.dto.response.ServerSummaryResponse;
import org.qubership.atp.ram.models.WidgetType;
import org.qubership.atp.ram.service.mail.ReportParams;
import org.qubership.atp.ram.services.ReportService;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ServerSummaryWidgetModelBuilder extends AbstractWidgetModelBuilder {

    private ReportService reportService;

    public ServerSummaryWidgetModelBuilder(ReportService reportService) {
        this.reportService = reportService;
    }

    @Override
    protected Map<String, Object> buildModel(ReportParams reportParams) {
        List<ServerSummaryResponse> serverSummary =
                reportService.getServerSummaryForExecutionRequest(reportParams.getExecutionRequestUuid());

        log.debug("ServerSummary = {} found for reportParams = {}", serverSummary, reportParams);

        return new HashMap<String, Object>() {
            {
                put("serverSummary", serverSummary);
            }
        };
    }

    @Override
    public WidgetType getType() {
        return WidgetType.SERVER_SUMMARY;
    }
}
