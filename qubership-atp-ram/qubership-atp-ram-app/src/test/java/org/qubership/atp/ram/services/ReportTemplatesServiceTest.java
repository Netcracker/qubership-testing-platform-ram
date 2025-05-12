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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.qubership.atp.ram.models.ReportTemplate;
import org.qubership.atp.ram.models.WidgetType;
import org.qubership.atp.ram.repositories.ReportTemplatesRepository;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class ReportTemplatesServiceTest {
    @InjectMocks
    private ReportTemplatesService reportTemplatesService;

    private ReportTemplatesService reportTemplatesServiceSpy;

    @Mock
    private  ReportTemplatesRepository repository;

    @BeforeEach
    public void setUp(){
        reportTemplatesServiceSpy = spy(reportTemplatesService);
    }

    @Test
    public void onReportTemplateService_whenGetActiveTemplateByProject_ActiveTemplateReturned(){
        when(repository.findAllByProjectId(any())).thenReturn(generateListOFReportTemplatesWithOneActive());
        ReportTemplate activeTemplate = reportTemplatesService.getActiveTemplateByProjectId(UUID.randomUUID());
        Assertions.assertNotNull(activeTemplate);
        Assertions.assertTrue(activeTemplate.isActive());
    }

    @Test
    public void onReportTemplateService_whenGetActiveTemplateForProjectWithoutActiveOnes_DefaultTemplateReturned(){
       when(repository.findAllByProjectId(any())).thenReturn(generateListOFReportTemplatesWithNoActive());
       doReturn(generateDefaultTemplate()).when(reportTemplatesServiceSpy).getDefaultTemplate();

        ReportTemplate activeTemplate = reportTemplatesServiceSpy.getActiveTemplateByProjectId(UUID.randomUUID());

        Assertions.assertNotNull(activeTemplate);
        Assertions.assertEquals(UUID.fromString(ReportTemplatesService.SYSTEM_DEFAULT_TEMPLATE_UUID), activeTemplate.getUuid());
    }


    @Test
    public void onReportTemplateService_whenGetActiveTemplateForProjectWithoutTemplates_DefaultTemplateReturned(){
        when(repository.findAllByProjectId(any())).thenReturn(generateEmptyListOFReportTemplates());
        doReturn(generateDefaultTemplate()).when(reportTemplatesServiceSpy).getDefaultTemplate();

        ReportTemplate activeTemplate = reportTemplatesServiceSpy.getActiveTemplateByProjectId(UUID.randomUUID());

        Assertions.assertNotNull(activeTemplate);
        Assertions.assertEquals(UUID.fromString(ReportTemplatesService.SYSTEM_DEFAULT_TEMPLATE_UUID), activeTemplate.getUuid());
    }

    private ReportTemplate generateDefaultTemplate() {
        ReportTemplate reportTemplate = generateReportTemplate(true);
        reportTemplate.setUuid(UUID.fromString(ReportTemplatesService.SYSTEM_DEFAULT_TEMPLATE_UUID));
        return reportTemplate;
    }

    private List<ReportTemplate> generateListOFReportTemplatesWithOneActive() {
        return new ArrayList<ReportTemplate>(){{
            add(generateReportTemplate(true));
            add(generateReportTemplate(false));
            add(generateReportTemplate(false));
        }};
    }

    private List<ReportTemplate> generateListOFReportTemplatesWithNoActive() {
        return new ArrayList<ReportTemplate>(){{
            add(generateReportTemplate(false));
            add(generateReportTemplate(false));
            add(generateReportTemplate(false));
        }};
    }

    private List<ReportTemplate> generateEmptyListOFReportTemplates() {
        return new ArrayList<>();
    }

    private ReportTemplate generateReportTemplate(Boolean isActive) {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setWidgets(Collections.singletonList(WidgetType.ENVIRONMENTS_INFO));
        reportTemplate.setActive(isActive);
        reportTemplate.setUuid(UUID.fromString("a617c4df-6fc4-40cf-9a49-3b25447e60ca"));
        reportTemplate.setName("Test Report Template");
        return reportTemplate;
    }
}
