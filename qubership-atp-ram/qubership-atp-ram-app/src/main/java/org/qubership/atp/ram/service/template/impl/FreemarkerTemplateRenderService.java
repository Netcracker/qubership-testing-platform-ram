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

package org.qubership.atp.ram.service.template.impl;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.StringJoiner;

import org.qubership.atp.ram.models.ReportTemplate;
import org.qubership.atp.ram.models.WidgetType;
import org.qubership.atp.ram.service.template.TemplateRenderService;
import org.qubership.atp.ram.services.ReportTemplatesService;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import com.google.common.base.Preconditions;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FreemarkerTemplateRenderService implements TemplateRenderService {

    private static final String HTML_WIDGET_DELIMETER = "<br/>";
    private static final String HTML_ROOT_PREFIX = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n"
            + "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n"
            + "<head>\n"
            + "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n"
            + "<title>Ram Email Report</title>\n"
            + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"/>\n"
            + "<style>\n"
            + "table, td, th {border-spacing: 0; border-collapse: collapse; padding: 0;} \n"
            + "a {text-decoration: none; color: #0068FF}\n"
            + "</style>\n"
            + "</head>\n"
            + " <body>";
    private static final String HTML_ROOT_SUFFIX = "</body></html>";
    private Configuration freemarkerConfiguration;
    private ReportTemplatesService reportTemplatesService;

    public FreemarkerTemplateRenderService(Configuration freemarkerConfiguration,
                                           ReportTemplatesService reportTemplatesService) {
        this.freemarkerConfiguration = freemarkerConfiguration;
        this.reportTemplatesService = reportTemplatesService;
    }

    @Override
    public String render(WidgetType widget, Map<String, Object> model) throws IOException, TemplateException {
        String widgetTemplateContent = reportTemplatesService.getReportTemplateWidgetTemplate(widget);
        Template template = new Template(widget.toString(), new StringReader(widgetTemplateContent),
                freemarkerConfiguration);

        return FreeMarkerTemplateUtils.processTemplateIntoString(template, model);
    }

    @Override
    public String render(ReportTemplate reportTemplate, Map<WidgetType, Map<String, Object>> model) {

        log.info(String.format("Start generating html report for the report template = %s and model = %s",
                reportTemplate.getName(), model));

        Preconditions.checkNotNull(reportTemplate, "Report Template can not be null.");
        Preconditions.checkNotNull(model, "Template Model can not be null.");

        StringJoiner joiner = new StringJoiner(HTML_WIDGET_DELIMETER, HTML_ROOT_PREFIX, HTML_ROOT_SUFFIX);
        reportTemplate.getWidgets().forEach(widget -> {
                    try {
                        joiner.add(render(widget, model.get(widget)));
                    } catch (Exception e) {
                        log.error(String.format("Error occurred while rendering report templateId = %s, widget = %s",
                                reportTemplate.getUuid(), widget.toString()), e);
                    }
                }
        );

        log.info(String.format("Html report generated for the report template = %s ,html = %s",
                reportTemplate.getName(), joiner.toString()));

        return joiner.toString();
    }

    @Override
    public String render(Template template, Object model) throws Exception {
        return FreeMarkerTemplateUtils.processTemplateIntoString(template, model);
    }
}
