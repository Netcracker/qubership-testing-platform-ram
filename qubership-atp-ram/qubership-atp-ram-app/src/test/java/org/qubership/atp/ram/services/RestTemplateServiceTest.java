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
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.atp.ram.models.ReportTemplate;
import org.qubership.atp.ram.models.WidgetType;
import org.qubership.atp.ram.repositories.ReportTemplatesRepository;

public class RestTemplateServiceTest {

    ReportTemplatesRepository repositoryMock;
    ReportTemplatesService reportTemplatesService;

    @BeforeEach
    public void setUp()
    {
        repositoryMock = mock(ReportTemplatesRepository.class);
        reportTemplatesService = new ReportTemplatesService(repositoryMock);
        when(repositoryMock.findByUuid(UUID.fromString(ReportTemplatesService.SYSTEM_DEFAULT_TEMPLATE_UUID)))
                .thenReturn(generateDefaultTemplate());
    }

    @Test
    public void onRestTemplateService_whenGetDefaultTemplate_templateReturnedWithNullUuid(){
        ReportTemplate defaultTemplate = reportTemplatesService.getDefaultTemplate();
        Assertions.assertNotNull(defaultTemplate);
        Assertions.assertNull(defaultTemplate.getUuid(), "UUID must be unavailable for Default Repost Template.");
    }

    private ReportTemplate generateDefaultTemplate(){
        ReportTemplate template = new ReportTemplate();
        template.setUuid(UUID.fromString(ReportTemplatesService.SYSTEM_DEFAULT_TEMPLATE_UUID));
        template.setName("System Default Report Template");
        template.setActive(true);
        template.setWidgets(Collections.singletonList(WidgetType.EXECUTION_SUMMARY));
        return template;
    }
}
