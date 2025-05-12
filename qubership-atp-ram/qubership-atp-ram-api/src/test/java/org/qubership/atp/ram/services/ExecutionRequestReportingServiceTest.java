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

package org.qubership.atp.ram.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.qubership.atp.ram.models.ExecutionRequestReporting;
import org.qubership.atp.ram.repositories.ExecutionRequestReportingRepository;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class ExecutionRequestReportingServiceTest {

    private static ExecutionRequestReportingService executionRequestReportingService;

    private static ExecutionRequestReportingRepository reportingRepository;

    private static UUID executionRequestId;

    @BeforeAll
    public static void setUp() throws Exception {
        reportingRepository = mock(ExecutionRequestReportingRepository.class);
        executionRequestReportingService = new ExecutionRequestReportingService(reportingRepository);
        executionRequestId = UUID.randomUUID();
    }

    @Test
    public void createDetailsTest_ExecutionRequestDetailsConfigured_successfullyCreated() {
        // given
        ExecutionRequestReporting reporting = new ExecutionRequestReporting();
        // when
        when(reportingRepository.save(any())).thenReturn(reporting);
        // then
        ExecutionRequestReporting actualReporting = executionRequestReportingService.createReporting(executionRequestId,
                reporting);
        assertEquals(executionRequestId, actualReporting.getExecutionRequestId());
    }
}
