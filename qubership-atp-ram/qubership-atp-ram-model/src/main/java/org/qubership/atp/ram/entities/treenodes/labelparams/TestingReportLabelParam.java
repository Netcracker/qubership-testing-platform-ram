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

package org.qubership.atp.ram.entities.treenodes.labelparams;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.models.logrecords.parts.ValidationTableLine;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TestingReportLabelParam extends ReportLabelParam {

    private TestingStatuses status;

    @JsonInclude(NON_NULL)
    private String expectedResult;

    @JsonInclude(NON_NULL)
    private String actualResult;

    public TestingReportLabelParam(String name, TestingStatuses status) {
        super(name);
        this.status = status;
    }

    /**
     * TestingReportLabelParam constructor.
     */
    public TestingReportLabelParam(String name, TestingStatuses status, ValidationTableLine step) {
        super(name);
        this.status = status;
        this.expectedResult = step.getExpectedResult();
        this.actualResult = step.getActualResult();
    }
}
