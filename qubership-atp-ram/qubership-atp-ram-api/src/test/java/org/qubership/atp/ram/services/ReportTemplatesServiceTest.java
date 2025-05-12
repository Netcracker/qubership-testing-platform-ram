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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.Rule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.qubership.atp.ram.exceptions.reporttemplates.RamReportTemplateAlreadyExistsException;
import org.qubership.atp.ram.models.ReportTemplate;
import org.qubership.atp.ram.repositories.ReportTemplatesRepository;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class ReportTemplatesServiceTest {

    @InjectMocks
    private ReportTemplatesService service;

    @Mock
    private ReportTemplatesRepository repository;

    @Captor
    private ArgumentCaptor<ReportTemplate> reportTemplateArgumentCaptor;

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    private UUID projectId;
    private String reportTemplateName;
    private ReportTemplate reportTemplate;

    @BeforeEach
    public void setUp() {
        reportTemplateName = "CPQ report template";
        projectId = UUID.randomUUID();

        reportTemplate = new ReportTemplate();
        reportTemplate.setProjectId(projectId);
        reportTemplate.setName(reportTemplateName);
    }

    @Test
    public void save_withValidData_shouldSuccessFullySave() {
        // given
        reportTemplate.setActive(false);

        when(repository.existsByNameAndProjectId(reportTemplateName, projectId)).thenReturn(false);

        // when
        service.save(reportTemplate);

        // then
        verify(repository).save(reportTemplateArgumentCaptor.capture());

        ReportTemplate result = reportTemplateArgumentCaptor.getValue();

        assertNotNull(result);
        assertNotNull(result.getName());
        assertEquals(reportTemplateName, result.getName());
        assertNotNull(result.getProjectId());
        assertEquals(projectId, result.getProjectId());
    }

    @Test
    public void save_withExistedName_exceptionExpected() {
        // given
        reportTemplate.setActive(false);

        when(repository.existsByNameAndProjectId(reportTemplateName, projectId)).thenReturn(true);

        // when
        Throwable e = Assertions.assertThrows(RamReportTemplateAlreadyExistsException.class, () -> {
            service.save(reportTemplate);
        });
        Assertions.assertEquals("Report template with provided name 'CPQ report template' already exists in the project", e.getMessage());
    }
}
