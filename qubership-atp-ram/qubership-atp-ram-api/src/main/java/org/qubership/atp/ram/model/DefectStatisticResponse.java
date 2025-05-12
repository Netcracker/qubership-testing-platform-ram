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

package org.qubership.atp.ram.model;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DefectStatisticResponse {
    List<DefectStatisticExecutionRequestResponse> defectStatisticExecutionRequestResponses;

    /**
     * Initializing responses list and rootCauses list.
     * **/
    public DefectStatisticResponse() {
        this.defectStatisticExecutionRequestResponses = new ArrayList<>();
    }

    public void addDefectStatisticExecutionRequestResponse(
            DefectStatisticExecutionRequestResponse defectStatisticExecutionRequestResponse) {
        this.defectStatisticExecutionRequestResponses.add(defectStatisticExecutionRequestResponse);
    }


    public static class DefaultDefectStatisticStatus {
        public static final String PASSED = "Passed";
        public static final String NOT_ANALYZED = "Not Analyzed";
    }
}
