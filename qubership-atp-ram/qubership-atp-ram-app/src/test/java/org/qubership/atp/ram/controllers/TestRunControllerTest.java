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

package org.qubership.atp.ram.controllers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.service.rest.server.mongo.TestRunController;

public class TestRunControllerTest {
    private final UUID uuid = UUID.randomUUID();
    private final ExecutionStatuses executionStatuses = ExecutionStatuses.IN_PROGRESS;
    private final TestingStatuses testingStatuses = TestingStatuses.PASSED;
    private TestRunController controller;

    @BeforeEach
    public void setUp() throws Exception {
        controller = mock(TestRunController.class);

        TestRun newTr = new TestRun();
        newTr.setUuid(uuid);
        newTr.setExecutionStatus(executionStatuses);
        newTr.updateTestingStatus(testingStatuses);

        when(controller.updExecutionStatus(uuid, executionStatuses)).thenReturn(newTr);
        when(controller.updTestingStatus(uuid, testingStatuses)).thenReturn(newTr);
    }

    @Test
    public void updateStatusShouldBeSuccess() {
        TestRun updTr = controller.updExecutionStatus(uuid, executionStatuses);
        Assertions.assertEquals(uuid, updTr.getUuid());
        Assertions.assertEquals(executionStatuses, updTr.getExecutionStatus());

        updTr = controller.updTestingStatus(uuid, testingStatuses);
        Assertions.assertEquals(uuid, updTr.getUuid());
        Assertions.assertEquals(testingStatuses, updTr.getTestingStatus());

    }
}
