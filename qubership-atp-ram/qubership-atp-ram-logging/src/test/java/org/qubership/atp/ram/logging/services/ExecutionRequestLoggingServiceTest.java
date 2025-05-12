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

package org.qubership.atp.ram.logging.services;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.qubership.atp.ram.logging.entities.requests.CreatedTestRunWithParentsRequest;
import org.qubership.atp.ram.logging.services.mocks.ModelMocks;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.TestPlan;
import org.qubership.atp.ram.repositories.ExecutionRequestRepository;
import org.qubership.atp.ram.utils.RateCalculator;

public class ExecutionRequestLoggingServiceTest {
    private ExecutionRequestLoggingService executionRequestLoggingService;
    private ExecutionRequestRepository requestRepository;

    @BeforeEach
    public void setUp() {
        requestRepository = mock(ExecutionRequestRepository.class);
        executionRequestLoggingService = spy(new ExecutionRequestLoggingService(requestRepository, new ModelMapper(),
                mock(RateCalculator.class), mock(LogRecordLoggingService.class), mock(TestRunLoggingService.class)));
    }

    @Test
    public void findOrCreateExecutionRequest_ExecutionRequestIsExist_ShouldReturnFoundRequest() {
        UUID id = ModelMocks.ATP_EXECUTION_REQUEST_ID;
        ExecutionRequest expExecutionRequest = ModelMocks.generateExecutionRequest(id);
        when(requestRepository.findByUuid(id)).thenReturn(expExecutionRequest);

        CreatedTestRunWithParentsRequest request = ModelMocks.generateCreatedTestRunWithParentsRequest();
        TestPlan testPlan = ModelMocks.generateTestPlan();
        ExecutionRequest actExecReq = executionRequestLoggingService.findOrCreateExecutionRequest(request, testPlan);
        verify(requestRepository, times(1)).findByUuid(id);
        Assertions.assertEquals(expExecutionRequest, actExecReq, "Return found execution request");
    }
}
