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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.qubership.atp.ram.dto.response.RootCausesStatisticResponse;
import org.qubership.atp.ram.models.Project;
import org.qubership.atp.ram.service.mail.ReportParams;
import org.qubership.atp.ram.service.template.impl.RootCauseWidgetModelBuilder;
import org.qubership.atp.ram.services.ExecutionRequestService;
import org.qubership.atp.ram.services.ReportService;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class RootCauseWidgetModelBuilderTest {
    @Mock
    private ReportService reportService;

    @Mock
    private ExecutionRequestService executionRequestService;

    @InjectMocks
    private RootCauseWidgetModelBuilder builder;

    @BeforeEach
    public void setUp(){
        Project project = new Project();
        project.setTimeFormat("hh:mm");
        project.setDateFormat("d MMM yyyy");
        project.setTimeZone("GMT+03:00");
        when(executionRequestService.getProjectId(any())).thenReturn(project);
        when(reportService.getRootCausesStatisticForExecutionRequestAndPrevious(any())).thenReturn(createRootCauses());
    }

    private List<RootCausesStatisticResponse> createRootCauses() {
        RootCausesStatisticResponse response = new RootCausesStatisticResponse();
        response.setExecutionRequestName("Execution Report #1");
        response.setStartDate(Timestamp.from(Instant.now()));
        RootCausesStatisticResponse.RootCausesGroup group = new RootCausesStatisticResponse.RootCausesGroup();
        group.setRootCauseName("ISSUES");
        group.setCount(5);
        group.setPercent(40);
        response.setRootCausesGroups(new ArrayList<RootCausesStatisticResponse.RootCausesGroup>(){{
            add(group);
        }});

        return new ArrayList<RootCausesStatisticResponse>(){{
            add(response);
        }};
    }

    @Test
    public void onRootCauseWidgetBuilder_whenGetModel_allDataStructureAdded(){
        ReportParams reportParams = new ReportParams();
        reportParams.setExecutionRequestUuid(UUID.randomUUID());
        Map<String, Object> model = builder.getModel(reportParams);

        Assertions.assertNotNull(model);
        Assertions.assertNotNull(model.get(RootCauseWidgetModelBuilder.ROOT_CAUSE_STATISCTICS_MODEL_KEY));
    }
}
