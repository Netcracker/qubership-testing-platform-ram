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

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.qubership.atp.ram.dto.response.LabelNodeReportResponse;
import org.qubership.atp.ram.entities.treenodes.labelparams.TestingReportLabelParam;
import org.qubership.atp.ram.models.Label;
import org.qubership.atp.ram.service.template.TestingStatusColor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Builder
@Getter
public class TestRunNodeAdapter {

    private LabelNodeReportResponse.TestRunNodeResponse testRunNodeResponse;
    private TestingStatusColor testingStatus;
    private TestingStatusColor firstStatus;
    private TestingStatusColor finalStatus;
    private List<LogRecordAdapter> failedStep;
    private String dataSetUrl;
    private String dataSetName;
    private String url;
    private Integer passedRate;
    private List<TestingReportLabelParam> labelParams;
    private String jiraTicket;
    private Set<String> issues;
    private String comment;
    private String finalRunUrl;

    public TestRunNodeAdapter(LabelNodeReportResponse.TestRunNodeResponse testRunNodeResponse) {
        this.testRunNodeResponse = testRunNodeResponse;
    }

    public String getName() {
        return testRunNodeResponse.getName();
    }

    public UUID getUuid() {
        return testRunNodeResponse.getUuid();
    }

    /**
     * Returns duration as a formatted string with pattern HH:mm:ss.
     * @return duration string value
     */
    public String getDuration() {
        return DurationFormatUtils.formatDuration(
                testRunNodeResponse.getDuration() * 1000,
                "HH:mm:ss",
                true);
    }

    public String getFailureReason() {
        return testRunNodeResponse.getFailureReason();
    }

    public List<Label> getLabels() {
        return testRunNodeResponse.getLabels();
    }

    public String getDataSetUrl() {
        return dataSetUrl;
    }

    public String getDataSetName() {
        return dataSetName;
    }

    public UUID getTestCaseId() {
        return testRunNodeResponse.getTestCaseId();
    }

    public List<LogRecordAdapter> getFailedStep() {
        return failedStep;
    }

    public String getDatasetUrl() {
        return testRunNodeResponse.getDataSetUrl();
    }

    public List<TestingReportLabelParam> getLabelParams() {
        return labelParams;
    }
}
