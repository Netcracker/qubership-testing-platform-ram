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
import org.qubership.atp.ram.controllers.api.dto.history.ReportTemplateHistoryChangeDto;
import org.qubership.atp.ram.converters.history.ReportTemplateVersioningMapper;
import org.qubership.atp.ram.models.ReportTemplate;
import org.qubership.atp.ram.service.history.impl.ReportTemplateVersioningHistoryService;

public class ReportTemplateVersioningHistoryServiceTest extends AbstractVersioningHistoryServiceTest<ReportTemplate, ReportTemplateHistoryChangeDto> {


    @BeforeEach
    public void setUp() {
        id = UUID.randomUUID();
        version = "1";
        entity = new ReportTemplate() {};
        shadow = mock(Shadow.class);
        dto = new ReportTemplateHistoryChangeDto();
        ((ReportTemplateHistoryChangeDto)dto).setActive(false);
        responseDto = new CompareEntityResponseDto();
        responseDto.setCompareEntity(dto);
        responseDto.setRevision(version);
        service = new ReportTemplateVersioningHistoryService(javers, new ReportTemplateVersioningMapper(new MvcConfig().modelMapper()));
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
