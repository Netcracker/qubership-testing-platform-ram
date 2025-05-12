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
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.qubership.atp.ram.service.charts.ChartsService;
import org.qubership.atp.ram.testdata.ExecutionRequestServiceMock;
import org.qubership.atp.ram.testdata.RootCauseServiceMock;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ChartsServicesTest {
    private static final String request = "[{"
            + "testPlan: \"1\","
            + "option: \"Statuses\","
            + "period: [],"
            + "numberOfEr: \"10\""
            + "}]";

    private ExecutionRequestServiceMock erMock = new ExecutionRequestServiceMock();
    private RootCauseServiceMock rcMock = new RootCauseServiceMock();
    private ChartsService chartsService;

    @BeforeEach
    public void before() {
        ExecutionRequestService executionRequestService = mock(ExecutionRequestService.class);
        RootCauseService rootCauseService = mock(RootCauseService.class);
        chartsService = new ChartsService(executionRequestService, rootCauseService);

        when(executionRequestService.findPageByTestPlanUuidAndSort(any(), anyInt(), anyInt(), anyString(),
                anyString())).thenReturn(erMock.findPageByTestPlanUuidAndSort());
        when(executionRequestService.getAllTestRuns(any())).thenReturn(erMock.getAllTestRuns());
        when(rootCauseService.getAllRootCauses()).thenReturn(rcMock.getAllRootCauses());
    }

    @Disabled
    @Test
    public void getStatistics_ForTestRunStatus_SizeForFirstErShouldEqualsFour() {
        int expectedSizeForRootCause1 = 4;
        JsonElement element = JsonParser.parseString(request);
        JsonArray requestArray = element.getAsJsonArray();
        JsonArray result = chartsService.getStatistics(requestArray);
        int resultSizeForRootCause1 = 0;
        JsonArray sample = result.get(0).getAsJsonObject().getAsJsonArray("sample");
        int size = sample.size();
        for (int i = 0; i < size; i++) {
            if (sample.get(i).getAsJsonObject().get("name").equals("Passed")) {
                resultSizeForRootCause1 =
                        sample.get(i).getAsJsonObject().get("data").getAsJsonArray().get(0).getAsInt();
            }
        }
        Assertions.assertEquals(expectedSizeForRootCause1, resultSizeForRootCause1);
    }

    @Test
    @Disabled
    public void calculateRootCauses_TwoTestRunWithRootCause1ForEverEr_SizeForDataShouldEqualsTwo() {
        int expectedSizeForRootCause1 = 2;
        JsonArray result = chartsService.calculateRootCauses(erMock.findPageByTestPlanUuidAndSort());
        int resultSizeForRootCause1 = ((JsonObject) result.get(1)).get("data").getAsJsonArray().size();
        Assertions.assertEquals(expectedSizeForRootCause1, resultSizeForRootCause1);
    }

}
