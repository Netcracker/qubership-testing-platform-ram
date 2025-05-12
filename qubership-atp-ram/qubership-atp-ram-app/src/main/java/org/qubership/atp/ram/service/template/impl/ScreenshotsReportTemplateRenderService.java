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
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.qubership.atp.auth.springbootstarter.utils.ExceptionUtils;
import org.qubership.atp.ram.constants.ScreenshotsConstants;
import org.qubership.atp.ram.dto.request.ExecutionRequestsForCompareScreenshotsRequest;
import org.qubership.atp.ram.exceptions.screenshots.RamScreenshotsExportReportTemplateNotFoundException;
import org.qubership.atp.ram.model.ExecutionRequestsCompareScreenshotResponse;
import org.qubership.atp.ram.services.ExecutionRequestCompareService;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.util.FileCopyUtils;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@AllArgsConstructor
public class ScreenshotsReportTemplateRenderService {

    private final String reportName = "screenshots-export-report";
    private final String screenshotsExportReportTemplateLocation =
            "classpath:data/screenshots-templates/ram-screenshots-export-report.template.ftl";

    private ExecutionRequestCompareService executionRequestCompareService;
    private Configuration freemarkerConfiguration;

    /**
     * Render screenshots report template.
     * @param compareRequest compare execution requests request.
     * @return string template with resolved variables.
     * @throws IOException exception during template creation
     * @throws TemplateException exception during template processing
     */
    public String render(ExecutionRequestsForCompareScreenshotsRequest compareRequest) throws IOException,
            TemplateException {
        log.debug("Start calculating html report with screenshots for ERs: {}",
                compareRequest.getExecutionRequestIds());
        ExecutionRequestsCompareScreenshotResponse model =
                executionRequestCompareService.getCompareScreenshotsExecutionRequests(
                        compareRequest.getExecutionRequestIds(), true);
        log.debug("Get variables '{}' and '{}' for template render",
                ScreenshotsConstants.EXECUTION_REQUESTS_TEMPLATE_VARIABLE,
                ScreenshotsConstants.TREE_TEMPLATE_VARIABLE);
        Map<String, Object> variables = new HashMap<>();
        variables.put(ScreenshotsConstants.EXECUTION_REQUESTS_TEMPLATE_VARIABLE, model.getExecutionRequests());
        variables.put(ScreenshotsConstants.TREE_TEMPLATE_VARIABLE, model.getTree());
        String screenshotsExportReportTemplateContent = getScreenshotsExportReportTemplateContent();
        Template template = new Template(reportName, new StringReader(screenshotsExportReportTemplateContent),
                freemarkerConfiguration);

        log.debug("Process template {} into string", template.getSourceName());
        return FreeMarkerTemplateUtils.processTemplateIntoString(template, variables);
    }

    /**
     * Get screenshots export report template content.
     *
     * @return screenshots export report template content
     * @throws IOException if cannot find template resource file
     */
    private String getScreenshotsExportReportTemplateContent() throws IOException {
        log.debug("Load screenshots export report template");
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        Resource resource = resourceLoader.getResource(screenshotsExportReportTemplateLocation);

        if (!resource.exists()) {
            ExceptionUtils.throwWithLog(log,
                    new RamScreenshotsExportReportTemplateNotFoundException(screenshotsExportReportTemplateLocation));
        }

        Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
        String content = FileCopyUtils.copyToString(reader);
        reader.close();
        log.debug("Found screenshots export report template content");
        return content;
    }
}
