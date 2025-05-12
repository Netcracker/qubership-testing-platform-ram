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

package org.qubership.atp.ram.testdata;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.model.ExecutionRequestsCompareScreenshotResponse;
import org.qubership.atp.ram.model.LogRecordCompareScreenshotResponse;
import org.qubership.atp.ram.model.ShortExecutionRequest;
import org.qubership.atp.ram.models.JiraComponent;
import org.qubership.atp.ram.models.Label;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EntitiesGenerator {

    public ExecutionRequestsCompareScreenshotResponse generateExecutionRequestsCompareScreenshotResponse() throws IOException {
        ExecutionRequestsCompareScreenshotResponse response = new ExecutionRequestsCompareScreenshotResponse();
        ShortExecutionRequest shortExecutionRequest =
                new ShortExecutionRequest(UUID.randomUUID(), "executionRequestName");
        response.setExecutionRequests(Collections.singletonList(shortExecutionRequest));
        ExecutionRequestsCompareScreenshotResponse.TestRunCompareScreenshotResponse testRunCompareScreenshotResponse
                = new ExecutionRequestsCompareScreenshotResponse.TestRunCompareScreenshotResponse();
        testRunCompareScreenshotResponse.setTestRunName("test");
        testRunCompareScreenshotResponse.setType(ExecutionRequestsCompareScreenshotResponse.Type.ACTION);
        LogRecordCompareScreenshotResponse logRecordCompareScreenshotResponse = new LogRecordCompareScreenshotResponse();
        logRecordCompareScreenshotResponse.setName("testLogRecord");
        logRecordCompareScreenshotResponse.setType("type");
        logRecordCompareScreenshotResponse.setSubStepName("subStepName");

        LogRecordCompareScreenshotResponse.SubStepCompareScreenshotResponse subStepCompareScreenshotResponse =
                new LogRecordCompareScreenshotResponse.SubStepCompareScreenshotResponse();
        subStepCompareScreenshotResponse.setId(UUID.randomUUID());
        subStepCompareScreenshotResponse.setTestingStatus(TestingStatuses.PASSED);
        subStepCompareScreenshotResponse.setTestPlanName("testPlanName");
        subStepCompareScreenshotResponse.setTestPlanId(UUID.randomUUID());


        String screenshotName = "snapshot.png";
        Path screenshotPath = Paths.get("src","test","resources", screenshotName);
        File screenshotFile = new File(String.valueOf(screenshotPath));
        InputStream screenshotInputStream = new FileInputStream(screenshotFile);
        byte[] screenshotBytes = IOUtils.toByteArray(screenshotInputStream);
        subStepCompareScreenshotResponse.setScreenshot(new String(screenshotBytes, StandardCharsets.UTF_8));
        logRecordCompareScreenshotResponse.setRow(Collections.singletonList(subStepCompareScreenshotResponse));
        testRunCompareScreenshotResponse.setChild(Collections.singletonList(logRecordCompareScreenshotResponse));

        response.setTree(Collections.singletonList(testRunCompareScreenshotResponse));
        return response;
    }

    public static Label newLabel(String name) {
        Label label = new Label();
        label.setUuid(UUID.randomUUID());
        label.setName(name);

        return label;
    }

    public static JiraComponent newJiraComponent(String name) {
        JiraComponent component = new JiraComponent();
        component.setId(UUID.randomUUID().toString());
        component.setName(name);

        return component;
    }
}
