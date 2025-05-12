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

import static java.util.Arrays.asList;

import java.util.UUID;

import org.qubership.atp.ram.dto.response.TestCaseLabelResponse;
import org.qubership.atp.ram.models.Label;

public class TestCaseLabelResponseServiceMock {

    public static TestCaseLabelResponse newTestCaseLabelResponse() {
        TestCaseLabelResponse testCaseLabelResponse = new TestCaseLabelResponse();
        testCaseLabelResponse.setUuid(UUID.randomUUID());

        return testCaseLabelResponse;
    }

    public static TestCaseLabelResponse newTestCaseLabelResponse(Label... labels) {
        TestCaseLabelResponse testCaseLabelResponse = newTestCaseLabelResponse();
        testCaseLabelResponse.setLabels(asList(labels));

        return testCaseLabelResponse;
    }
}
