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

package org.qubership.atp.ram.history;

import static org.mockito.Mockito.mock;

import java.util.UUID;

import org.javers.shadow.Shadow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.atp.ram.config.MvcConfig;
import org.qubership.atp.ram.controllers.api.dto.history.CompareEntityResponseDto;
import org.qubership.atp.ram.controllers.api.dto.history.FailPatternHistoryChangeDto;
import org.qubership.atp.ram.converters.history.FailPatternVersioningMapper;
import org.qubership.atp.ram.models.FailPattern;
import org.qubership.atp.ram.service.history.impl.FailPatternVersioningHistoryService;
import org.qubership.atp.ram.services.RootCauseService;

public class FailPatternVersioningHistoryServiceTest extends AbstractVersioningHistoryServiceTest<FailPattern, FailPatternHistoryChangeDto> {


    @BeforeEach
    public void setUp() {
        id = UUID.randomUUID();
        version = "1";
        entity = new FailPattern() {};
        shadow = mock(Shadow.class);
        dto = new FailPatternHistoryChangeDto();
        responseDto = new CompareEntityResponseDto();
        responseDto.setCompareEntity(dto);
        responseDto.setRevision(version);
        RootCauseService rootCauseService = mock(RootCauseService.class);
        service = new FailPatternVersioningHistoryService(javers,
                new FailPatternVersioningMapper(new MvcConfig().modelMapper(), rootCauseService));
    }

    @Test
    public void testGetEntitiesByVersions() {
        super.testGetEntitiesByVersions();
    }

    @Test
    public void testGetEntityByVersion_EntityFound() {
        super.testGetEntityByVersion_EntityFound();
    }

    @Test
    public void testGetEntityByVersion_EntityNotFound() {
        super.testGetEntityByVersion_EntityNotFound();
    }
}
