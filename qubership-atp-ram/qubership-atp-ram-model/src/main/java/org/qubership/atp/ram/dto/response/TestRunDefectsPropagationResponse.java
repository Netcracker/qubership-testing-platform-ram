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

package org.qubership.atp.ram.dto.response;

import java.util.ArrayList;
import java.util.List;

import org.qubership.atp.ram.models.TestRun;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class TestRunDefectsPropagationResponse {

    private List<Item> successTestRuns;
    private List<Item> failedTestRuns;

    /**
     * Add success case to response.
     */
    public void addSuccess(TestRun testRun) {
        if (successTestRuns == null) {
            successTestRuns = new ArrayList<>();
        }
        successTestRuns.add(new Item(testRun.getName()));
    }

    /**
     * Add failed case to response.
     */
    public void addFailed(TestRun testRun) {
        if (failedTestRuns == null) {
            failedTestRuns = new ArrayList<>();
        }
        failedTestRuns.add(new Item(testRun.getName()));
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Item {
        private String name;
    }
}
