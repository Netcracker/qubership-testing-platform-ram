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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.qubership.atp.auth.springbootstarter.exceptions.AtpEntityNotFoundException;
import org.qubership.atp.ram.EnvironmentsInfoMock;
import org.qubership.atp.ram.enums.SystemStatus;
import org.qubership.atp.ram.models.SystemInfo;
import org.qubership.atp.ram.repositories.EnvironmentsInfoRepository;
import org.qubership.atp.ram.repositories.ToolsInfoRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class EnvironmentsInfoServiceTest {

    private EnvironmentsInfoService environmentsInfoService;
    private EnvironmentsInfoRepository environmentsInfoRepository;

    @BeforeEach
    public void setUp() {
        environmentsInfoRepository = mock(EnvironmentsInfoRepository.class);
        environmentsInfoService = spy(new EnvironmentsInfoService(
                environmentsInfoRepository,
                mock(ToolsInfoRepository.class),
                mock(GridFsService.class),
                mock(MongoTemplate.class)));
    }

    @Test
    public void calculateStatus_SystemHasFailStatus_ReturnFailAsCommonStatus() {
        SystemInfo failSystem = EnvironmentsInfoMock.generateSystemInfoWithStatus(SystemStatus.FAIL);
        SystemInfo warnSystem = EnvironmentsInfoMock.generateSystemInfoWithStatus(SystemStatus.WARN);
        SystemInfo nothingSystem = EnvironmentsInfoMock.generateSystemInfoWithStatus(SystemStatus.NOTHING);
        SystemInfo passSystem = EnvironmentsInfoMock.generateSystemInfoWithStatus(SystemStatus.PASS);

        SystemStatus result = environmentsInfoService.calculateStatus(Arrays.asList(failSystem, warnSystem,
                nothingSystem, passSystem));
        Assertions.assertEquals(SystemStatus.FAIL, result, "Status should be failed for Environments Info section");
    }

    @Test
    public void calculateStatus_SystemHasWarnStatusAndHasNotFailStatus_ReturnWarnAsCommonStatus() {
        SystemInfo nothingSystemOne = EnvironmentsInfoMock.generateSystemInfoWithStatus(SystemStatus.NOTHING);
        SystemInfo warnSystem = EnvironmentsInfoMock.generateSystemInfoWithStatus(SystemStatus.WARN);
        SystemInfo nothingSystem = EnvironmentsInfoMock.generateSystemInfoWithStatus(SystemStatus.NOTHING);
        SystemInfo passSystem = EnvironmentsInfoMock.generateSystemInfoWithStatus(SystemStatus.PASS);

        SystemStatus result = environmentsInfoService.calculateStatus(Arrays.asList(nothingSystemOne, warnSystem,
                nothingSystem, passSystem));
        Assertions.assertEquals(SystemStatus.WARN, result, "Status should be warning for Environments Info section");
    }

    @Test
    public void calculateStatus_SystemHasHasNotFailStatus_ReturnPassAsCommonStatus() {
        SystemInfo nothingSystemOne = EnvironmentsInfoMock.generateSystemInfoWithStatus(SystemStatus.NOTHING);
        SystemInfo nothingSystem = EnvironmentsInfoMock.generateSystemInfoWithStatus(SystemStatus.NOTHING);
        SystemInfo passSystem = EnvironmentsInfoMock.generateSystemInfoWithStatus(SystemStatus.PASS);

        SystemStatus result = environmentsInfoService.calculateStatus(Arrays.asList(nothingSystemOne,
                nothingSystem, passSystem));
        Assertions.assertEquals(SystemStatus.PASS, result, "Status should be passed for Environments Info section");
    }

    @Test
    public void findByExecutionRequestUuid_RepositoryGetNull_ReturnException() {
        final UUID executionRequestId = UUID.randomUUID();
        when(environmentsInfoRepository.findByExecutionRequestId(executionRequestId)).thenReturn(null);
        Assertions.assertThrows(AtpEntityNotFoundException.class, () -> {
            environmentsInfoService.findByExecutionRequestId(executionRequestId);
        });
    }
}
