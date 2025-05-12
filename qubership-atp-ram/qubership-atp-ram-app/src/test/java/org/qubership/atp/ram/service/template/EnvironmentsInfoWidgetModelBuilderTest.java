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


import static java.util.Arrays.asList;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.atp.ram.dto.response.Environment;
import org.qubership.atp.ram.enums.SystemStatus;
import org.qubership.atp.ram.model.GridFsFileData;
import org.qubership.atp.ram.models.EnvironmentsInfo;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.SsmMetricReports;
import org.qubership.atp.ram.models.SystemInfo;
import org.qubership.atp.ram.models.ToolsInfo;
import org.qubership.atp.ram.models.WdShells;
import org.qubership.atp.ram.models.WidgetType;
import org.qubership.atp.ram.service.mail.ReportParams;
import org.qubership.atp.ram.service.template.impl.EnvironmentsInfoWidgetModelBuilder;
import org.qubership.atp.ram.services.EnvironmentsInfoService;
import org.qubership.atp.ram.services.EnvironmentsService;
import org.qubership.atp.ram.services.ExecutionRequestService;

import com.fasterxml.jackson.databind.ObjectMapper;

public class EnvironmentsInfoWidgetModelBuilderTest {

    private static final String DESCRIPTION = "description";
    private final String expectedDescription = "Test Expected Description";
    private EnvironmentsInfoWidgetModelBuilder widgetModelBuilder;
    private ObjectMapper mapper = new ObjectMapper();
    private EnvironmentsInfoService environmentsInfoService;
    private ExecutionRequestService executionRequestService;
    private EnvironmentsService environmentsService;
    private ReportParams reportParams;

    @BeforeEach
    public void setUp() throws Exception {

        environmentsInfoService = mock(EnvironmentsInfoService.class);
        executionRequestService = mock(ExecutionRequestService.class);
        environmentsService = mock(EnvironmentsService.class);
        widgetModelBuilder = new EnvironmentsInfoWidgetModelBuilder(environmentsInfoService, executionRequestService,
                environmentsService, mapper);

        Environment environment = new Environment(UUID.randomUUID(), "Some env");
        Environment taToolGroup = new Environment(UUID.randomUUID(), "Some ta tool");
        ExecutionRequest executionRequest = new ExecutionRequest();
        executionRequest.setUuid(UUID.randomUUID());
        executionRequest.setEnvironmentId(environment.getId());
        executionRequest.setTaToolsGroupId(taToolGroup.getId());

        GridFsFileData gridFsFileData = new GridFsFileData("report.html", null);

        when(environmentsInfoService.findByExecutionRequestId(any())).thenReturn(createEnvInfo());
        when(environmentsInfoService.getReportById(any())).thenReturn(gridFsFileData);
        when(executionRequestService.get(any())).thenReturn(executionRequest);
        when(environmentsService.searchEnvironments(any())).thenReturn(asList(environment, taToolGroup));
        reportParams = new ReportParams();
        reportParams.setExecutionRequestUuid(UUID.randomUUID());
        reportParams.setRecipients("jhon@gmail.com");
        reportParams.setSubject("Test Subject");
        reportParams.setDescriptions(new HashMap<String, String>(){{
            put(WidgetType.ENVIRONMENTS_INFO.toString(), expectedDescription);
            put(WidgetType.SUMMARY.toString(), "Summary Description");
        }});
    }

    private EnvironmentsInfo createEnvInfo() {
        WdShells wdShell = new WdShells();
        wdShell.setName("wdShell");
        wdShell.setVersion("v.1");
        List<WdShells> wdShells = Collections.singletonList(wdShell);

        ToolsInfo toolsInfo = new ToolsInfo();
        toolsInfo.setDealer("dealer");
        toolsInfo.setDealerLogsUrl("www.dealer.com");
        toolsInfo.setSelenoid("solenoid");
        toolsInfo.setSelenoidLogsUrl("www.solenoid.com");
        toolsInfo.setSessionId("sessionId");
        toolsInfo.setSelenoidLogsUrl("www.session.com");
        toolsInfo.setTool("tool");
        toolsInfo.setToolLogsUrl("www.tool.com");
        toolsInfo.setWdShells(wdShells);

        EnvironmentsInfo environmentsInfo = new EnvironmentsInfo();
        environmentsInfo.setExecutionRequestId(UUID.randomUUID());
        environmentsInfo.setDuration(98294187L);
        environmentsInfo.setStartDate(Timestamp.from(Instant.now().minus(23, ChronoUnit.MINUTES)));
        environmentsInfo.setEndDate(Timestamp.from(Instant.now()));
        environmentsInfo.setStatus(SystemStatus.PASS.toString());
        environmentsInfo.setToolsInfo(toolsInfo);
        environmentsInfo.setMandatoryChecksReportId(UUID.randomUUID());
        environmentsInfo.setSsmMetricReports(new SsmMetricReports(UUID.randomUUID(), UUID.randomUUID()));

        SystemInfo systemInfo = new SystemInfo();
        systemInfo.setName("qa system");
        systemInfo.setStatus(SystemStatus.PASS);
        systemInfo.setVersion("v1");
        systemInfo.setUrls(new ArrayList<String>(){{
            add("www.qasystem.com");
            add("www.some-service.com");
        }});
        List<SystemInfo> qaSystemInfoList = Collections.singletonList(systemInfo);
        environmentsInfo.setQaSystemInfoList(qaSystemInfoList);

        SystemInfo taSystemInfo = new SystemInfo();
        taSystemInfo.setName("ta system");
        taSystemInfo.setStatus(SystemStatus.FAIL);
        taSystemInfo.setVersion("v1");
        taSystemInfo.setUrls(Collections.singletonList("www.tasystem.com"));
        List<SystemInfo> taSystemInfoList = Collections.singletonList(taSystemInfo);
        environmentsInfo.setTaSystemInfoList(taSystemInfoList);

        return environmentsInfo;
    }

    @Test
    public void onAbstractWidgetModelBuilder_whenGetModel_DescriptionIsUpdatedFromIncomingParams(){
        Map<String, Object> model = widgetModelBuilder.getModel(reportParams);
        Assertions.assertNotNull(model);
        Assertions.assertEquals(expectedDescription, model.get(DESCRIPTION));
    }

    @Test
    public void onEnvironmentsInfoWidgetModelBuilder_whenGetModel_AllDataStructureAdded(){
        Map<String, Object> model = widgetModelBuilder.getModel(reportParams);
        Assertions.assertNotNull(model);
        Assertions.assertNotNull(model.get("duration"));
        Assertions.assertNotNull(model.get("startDate"));
        Assertions.assertNotNull(model.get("endDate"));
        Assertions.assertNotNull(model.get("statusBgColor"));
        Assertions.assertNotNull(model.get("qaSystemInfoList"));
        Assertions.assertNotNull(model.get("taSystemInfoList"));
        Assertions.assertNotNull(model.get("wdShellTables"));
        Assertions.assertNotNull(model.get("environmentLink"));
        Assertions.assertNotNull(model.get("environmentName"));
        Assertions.assertNotNull(model.get("toolGroupLink"));
        Assertions.assertNotNull(model.get("toolGroupName"));
        Assertions.assertNotNull(model.get("mandatoryChecksReportLink"));
        Assertions.assertNotNull(model.get("mandatoryChecksReportName"));
        Assertions.assertNotNull(model.get("ssmMetricsMicroservicesReportName"));
        Assertions.assertNotNull(model.get("ssmMetricsMicroservicesReportLink"));
        Assertions.assertNotNull(model.get("ssmMetricsProblemContextReportName"));
        Assertions.assertNotNull(model.get("ssmMetricsProblemContextReportLink"));
    }
}
