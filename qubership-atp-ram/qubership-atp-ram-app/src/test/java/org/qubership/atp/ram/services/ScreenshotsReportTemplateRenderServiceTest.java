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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.qubership.atp.ram.dto.request.ExecutionRequestsForCompareScreenshotsRequest;
import org.qubership.atp.ram.model.ExecutionRequestsCompareScreenshotResponse;
import org.qubership.atp.ram.service.template.impl.ScreenshotsReportTemplateRenderService;
import org.qubership.atp.ram.testdata.EntitiesGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

@SpringBootTest(classes = {ScreenshotsReportTemplateRenderService.class, Configuration.class},
        properties = {"spring.cloud.consul.config.enabled=false"})
public class ScreenshotsReportTemplateRenderServiceTest {

    @MockBean
    private ExecutionRequestCompareService executionRequestCompareService;

    @Autowired
    private ScreenshotsReportTemplateRenderService screenshotsReportTemplateRenderService;

    @Test
    public void renderTest() throws IOException, TemplateException {
        ExecutionRequestsForCompareScreenshotsRequest request = new ExecutionRequestsForCompareScreenshotsRequest();
        ExecutionRequestsCompareScreenshotResponse response = EntitiesGenerator.generateExecutionRequestsCompareScreenshotResponse();
        when(executionRequestCompareService.getCompareScreenshotsExecutionRequests(any(), anyBoolean())).thenReturn(response);
        String processedTemplate = screenshotsReportTemplateRenderService.render(request);
        Assertions.assertNotNull(processedTemplate);
        Assertions.assertFalse(processedTemplate.isEmpty());
    }
}
