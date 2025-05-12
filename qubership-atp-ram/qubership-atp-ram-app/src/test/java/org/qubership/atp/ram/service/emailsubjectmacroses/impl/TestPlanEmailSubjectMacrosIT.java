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

package org.qubership.atp.ram.service.emailsubjectmacroses.impl;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;
import org.qubership.atp.auth.springbootstarter.exceptions.AtpIllegalNullableArgumentException;
import org.qubership.atp.ram.dto.response.ExecutionSummaryResponse;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.TestPlan;
import org.qubership.atp.ram.service.emailsubjectmacros.impl.TestPlanEmailSubjectMacros;
import org.qubership.atp.ram.services.CatalogueService;
import org.qubership.atp.ram.services.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestPlanEmailSubjectMacros.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"spring.cloud.consul.config.enabled=false"})
@Isolated
public class TestPlanEmailSubjectMacrosIT {

    @Autowired
    private TestPlanEmailSubjectMacros macros;

    @MockBean
    private CatalogueService catalogueService;

    @MockBean
    private ReportService reportServcatalogueServicee;

    @Test
    public void resolve_testExecutionRequestTestPlanResolve_expectedSuccessfulResolve() {
        // given
        final UUID executionRequestTestPlanId = UUID.randomUUID();

        TestPlan testPlan = new TestPlan();
        testPlan.setName("Test Plan 1");

        ExecutionRequest executionRequest = new ExecutionRequest();
        final ExecutionSummaryResponse executionSummaryResponse = new ExecutionSummaryResponse();
        executionRequest.setTestPlanId(executionRequestTestPlanId);

        when(catalogueService.getTestPlan(executionRequestTestPlanId)).thenReturn(testPlan);

        // when
        final String resolvedValue = macros.resolve(executionRequest, executionSummaryResponse);

        // then
        assertThat(resolvedValue)
                .isNotNull()
                .isEqualTo(testPlan.getName());
    }


    @Test
    public void resolve_testWithNullTestPlanReference_expectedException() {
        // given
        final ExecutionRequest executionRequest = new ExecutionRequest();
        final ExecutionSummaryResponse executionSummaryResponse = new ExecutionSummaryResponse();

        // when
        Assertions.assertThrows(AtpIllegalNullableArgumentException.class, () -> {
            macros.resolve(executionRequest, executionSummaryResponse);
        });
    }
}
