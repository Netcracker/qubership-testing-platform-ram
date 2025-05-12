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

import static java.util.Arrays.asList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.qubership.atp.ram.testdata.LabelServiceMock.newLabel;
import static org.qubership.atp.ram.testdata.TestCaseLabelResponseServiceMock.newTestCaseLabelResponse;
import static org.qubership.atp.ram.testdata.TestRunServiceMock.newTestRun;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.qubership.atp.ram.dto.response.ExecutionSummaryResponse;
import org.qubership.atp.ram.dto.response.TestCaseLabelResponse;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.Label;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.service.emailsubjectmacros.impl.GroupLabelsEmailSubjectMacros;
import org.qubership.atp.ram.services.CatalogueService;
import org.qubership.atp.ram.services.TestRunService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = GroupLabelsEmailSubjectMacros.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"spring.cloud.consul.config.enabled=false"})
public class GroupLabelsEmailSubjectMacrosIT {

    @Autowired
    private GroupLabelsEmailSubjectMacros macros;

    @MockBean
    private CatalogueService catalogueService;

    @MockBean
    private TestRunService testRunService;

    @Test
    public void resolve_testExecutionRequestGroupLabelsResolve_expectedSuccessfulResolve() {
        // given
        final UUID executionRequestId = UUID.randomUUID();

        final List<TestRun> testRuns = asList(newTestRun(), newTestRun(), newTestRun());

        final Label label1 = newLabel("Regression");
        final Label label2 = newLabel("Smoke");
        final Label label3 = newLabel("High Priority");

        final List<TestCaseLabelResponse> testCaseLabelResponses = asList(
                newTestCaseLabelResponse(label1, label2),
                newTestCaseLabelResponse(label1, label2, label3),
                newTestCaseLabelResponse(label3)
        );

        ExecutionRequest executionRequest = new ExecutionRequest();
        executionRequest.setUuid(executionRequestId);
        final ExecutionSummaryResponse executionSummaryResponse = new ExecutionSummaryResponse();
        when(testRunService.findAllByExecutionRequestId(executionRequestId)).thenReturn(testRuns);
        when(catalogueService.getTestCaseLabelsByIds(any())).thenReturn(testCaseLabelResponses);

        // when
        final String resolvedValue = macros.resolve(executionRequest, executionSummaryResponse);

        // then
        final String commaSeparator = ", ";
        final String expectedResolvedValue = label1.getName() + commaSeparator +
                label2.getName() + commaSeparator + label3.getName();

        assertThat(resolvedValue)
                .isNotNull()
                .isEqualTo(expectedResolvedValue);
    }
}
