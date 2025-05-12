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

package org.qubership.atp.ram.service.rest.server.executor;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.service.rest.server.executor.request.StartRunRequest;
import org.qubership.atp.ram.services.IssueService;
import org.qubership.atp.ram.services.TestRunService;

public class TestRunControllerExecutorTest {
    private TestRunController2 testRunController;

    @BeforeEach
    public void setUp() {
        testRunController = spy(new TestRunController2(mock(TestRunService.class), mock(IssueService.class)));
    }

    @Test
    public void getExecutionRequestByRequest_SetLabelIdAsNull_ReturnValidObject() {
        UUID projectId = UUID.randomUUID();
        UUID testPlanId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        ExecutionRequest expRequest = new ExecutionRequest();
        expRequest.setName("Test");
        expRequest.setProjectId(projectId);
        expRequest.setTestPlanId(testPlanId);
        expRequest.setUuid(requestId);

        StartRunRequest startRunRequest = new StartRunRequest();
        startRunRequest.setProjectId(projectId);
        startRunRequest.setTestPlanId(testPlanId);
        startRunRequest.setExecutionRequestName("Test");
        startRunRequest.setAtpExecutionRequestId(requestId);
        startRunRequest.setLabelTemplateId("null");

        ExecutionRequest actualRequest = testRunController.getExecutionRequestByRequest(startRunRequest);
        Assertions.assertEquals(expRequest, actualRequest, "Execution request should be configured successfully");
    }

    @Test
    public void getExecutionRequestByRequest_SetLabelId_ReturnValidObject() {
        UUID projectId = UUID.randomUUID();
        UUID testPlanId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID labelTemplateId = UUID.randomUUID();
        ExecutionRequest expRequest = new ExecutionRequest();
        expRequest.setName("Test");
        expRequest.setProjectId(projectId);
        expRequest.setTestPlanId(testPlanId);
        expRequest.setUuid(requestId);
        expRequest.setLabelTemplateId(labelTemplateId);

        StartRunRequest startRunRequest = new StartRunRequest();
        startRunRequest.setProjectId(projectId);
        startRunRequest.setTestPlanId(testPlanId);
        startRunRequest.setExecutionRequestName("Test");
        startRunRequest.setAtpExecutionRequestId(requestId);
        startRunRequest.setLabelTemplateId(labelTemplateId.toString());

        ExecutionRequest actualRequest = testRunController.getExecutionRequestByRequest(startRunRequest);
        Assertions.assertEquals(expRequest, actualRequest, "Execution request should be configured successfully");
    }


}
