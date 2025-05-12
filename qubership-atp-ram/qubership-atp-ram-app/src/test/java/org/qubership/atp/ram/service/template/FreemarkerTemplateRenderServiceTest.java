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

package org.qubership.atp.ram.service.template;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.atp.ram.models.ReportTemplate;
import org.qubership.atp.ram.models.Widget;
import org.qubership.atp.ram.models.WidgetType;
import org.qubership.atp.ram.service.template.impl.FreemarkerTemplateRenderService;
import org.qubership.atp.ram.services.ReportTemplatesService;

import freemarker.template.Configuration;
import lombok.SneakyThrows;

public class FreemarkerTemplateRenderServiceTest {

    TemplateRenderService templateRenderService;
    private ReportTemplate validReportTemplate;
    private ReportTemplate invalidReportTemplate;
    private Map<WidgetType, Map<String, Object>> model;

    @BeforeEach
    @SneakyThrows
    public void setUp(){

        Configuration freeMarkerConfiguration = mock(Configuration.class);
        ReportTemplatesService reportTemplatesService = mock(ReportTemplatesService.class);
        templateRenderService = spy(new FreemarkerTemplateRenderService(freeMarkerConfiguration, reportTemplatesService));

        validReportTemplate = generateValidReportTemplate();
        invalidReportTemplate = generateInvalidReportTemplate();

        model = new HashMap<WidgetType, Map<String, Object>>(){{
            put(WidgetType.SUMMARY,
                    new HashMap<String, Object>(){{
                        put("text", "widget content");
                    }});
        }};
        doReturn("<p>widget content</p>")
                .when(templateRenderService)
                .render(generateValidWidget(), model.get(WidgetType.SUMMARY));
        doThrow(new IllegalStateException())
                .when(templateRenderService)
                .render(generateInvalidWidget(), model.get(WidgetType.SUMMARY));
    }


    @Test
    public void onFreemarkerTemplateService_generateHtmlBodyFromTemplate_htmlGenerated(){
        String renderResult = templateRenderService.render(validReportTemplate, model);
        assertNotNull(renderResult);
        assertTrue(renderResult.startsWith("<!DOCTYPE html"));
        assertTrue(renderResult.endsWith("</html>"));
    }

    @Test
    public void onFreemarkerTemplateService_generateHtmlBodyFromInvalidInput_doesNotThrowException(){
        Assertions.assertThatCode(() -> templateRenderService.render(invalidReportTemplate, model))
                .doesNotThrowAnyException();
    }

    private ReportTemplate generateValidReportTemplate() {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setWidgets(Collections.singletonList(
                generateValidWidget()));
        reportTemplate.setActive(true);
        reportTemplate.setUuid(UUID.fromString("a617c4df-6fc4-40cf-9a49-3b25447e60ca"));
        reportTemplate.setName("Test Report Template");
        return reportTemplate;
    }

    private ReportTemplate generateInvalidReportTemplate() {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setWidgets(Collections.singletonList(
                generateInvalidWidget()));
        reportTemplate.setActive(true);
        reportTemplate.setUuid(UUID.fromString("a617c4df-6fc4-40cf-9a49-3b25447e60ca"));
        reportTemplate.setName("Test Report Template");
        return reportTemplate;
    }

    private WidgetType generateValidWidget() {
        return WidgetType.SUMMARY;
    }

    private WidgetType generateInvalidWidget() {
        return WidgetType.ENVIRONMENTS_INFO;
    }

    private Widget generateWidget(WidgetType type, String template){
        Widget widget = new Widget();
        widget.setType(type);
        widget.setRenderTemplate(template);
        return widget;
    }
}
