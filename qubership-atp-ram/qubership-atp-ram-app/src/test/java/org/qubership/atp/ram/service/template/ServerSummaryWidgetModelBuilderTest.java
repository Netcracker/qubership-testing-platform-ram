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
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.qubership.atp.ram.dto.response.ServerSummaryResponse;
import org.qubership.atp.ram.models.WidgetType;
import org.qubership.atp.ram.service.mail.ReportParams;
import org.qubership.atp.ram.service.template.impl.ServerSummaryWidgetModelBuilder;
import org.qubership.atp.ram.services.ReportService;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class ServerSummaryWidgetModelBuilderTest {

    @Mock
    private ReportService reportService;
    @InjectMocks
    private ServerSummaryWidgetModelBuilder builder;

    private ReportParams reportParams;
    private List<ServerSummaryResponse> serverSummary;

    @BeforeEach
    public void setUp() {
        reportParams = createReportParams();
        serverSummary = createServerSummary();
        when(reportService.getServerSummaryForExecutionRequest(any())).thenReturn(serverSummary);
    }

    private List<ServerSummaryResponse> createServerSummary() {
        ServerSummaryResponse serverSummaryResponse = new ServerSummaryResponse();
        serverSummaryResponse.setServer("server");
        serverSummaryResponse.setBuild(Collections.singletonList("build v1"));
        return Collections.singletonList(serverSummaryResponse);
    }

    private ReportParams createReportParams() {
        ReportParams reportParams = new ReportParams();
        reportParams.setExecutionRequestUuid(UUID.randomUUID());
        reportParams.setRecipients("example@example.com");
        reportParams.setSubject("Test Subject");
        reportParams.setDescriptions(new HashMap<String, String>(){{
            put(WidgetType.SERVER_SUMMARY.toString(), "Test description");
        }});

        return reportParams;
    }

    @Test
    public void onServerSummaryWidgetModelBuilder_whenGetModel_AllDataStructureAdded(){
        Map<String, Object> model = builder.getModel(reportParams);

        Assertions.assertNotNull(model);
        Assertions.assertNotNull(model.get("serverSummary"));
    }
}
