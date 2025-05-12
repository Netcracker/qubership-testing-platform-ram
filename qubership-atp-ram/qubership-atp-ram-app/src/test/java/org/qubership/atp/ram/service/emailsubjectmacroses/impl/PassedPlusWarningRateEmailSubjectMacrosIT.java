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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.qubership.atp.ram.dto.response.ExecutionSummaryResponse;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.service.emailsubjectmacros.impl.PassedPlusWarningRateEmailSubjectMacros;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = PassedPlusWarningRateEmailSubjectMacros.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"spring.cloud.consul.config.enabled=false"})
public class PassedPlusWarningRateEmailSubjectMacrosIT {

    @Autowired
    private PassedPlusWarningRateEmailSubjectMacros macros;

    @Test
    public void resolve_testExecutionRequestPassedPlusWarningRateResolve_expectedSuccessfulResolve() {
        // given
        final float executionRequestPassedRate = 33.6F;
        final float executionRequestWarningRate = 23;
        final ExecutionRequest executionRequest = new ExecutionRequest();
        final ExecutionSummaryResponse executionSummaryResponse = new ExecutionSummaryResponse();
        executionSummaryResponse.setWarningRate(executionRequestWarningRate);
        executionSummaryResponse.setPassedRate(executionRequestPassedRate);

        // when
        final String resolvedValue = macros.resolve(executionRequest, executionSummaryResponse);

        // then
        assertThat(resolvedValue)
                .isNotNull()
                .isEqualTo(String.valueOf(executionRequestPassedRate + executionRequestWarningRate));
    }
}
