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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.qubership.atp.ram.dto.response.LabelNodeReportResponse;
import org.qubership.atp.ram.entities.treenodes.labelparams.TestingReportLabelParam;
import org.qubership.atp.ram.service.template.TestingStatusColor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Builder
public class LabelNodeReportAdapter {

    private LabelNodeReportResponse labelNodeReportResponse;
    private List<TestRunNodeAdapter> testRuns;
    private List<LabelNodeReportAdapter> children;
    private TestingStatusColor status;
    private List<String> validationLabelsOrder;
    private List<TestingReportLabelParam> labelParams;
    @Getter
    private boolean isGroupNode;

    public LabelNodeReportAdapter(LabelNodeReportResponse labelNodeReportResponse) {
        this.labelNodeReportResponse = labelNodeReportResponse;
    }

    public String getLabelName() {
        return labelNodeReportResponse.getLabelName();
    }

    public TestingStatusColor getStatus() {
        return status;
    }

    /**
     * Returns duration as a formatted string with pattern HH:mm:ss.
     * @return duration string value
     */
    public String getDuration() {
        return DurationFormatUtils.formatDuration(
                labelNodeReportResponse.getDuration() * 1000,
                "HH:mm:ss",
                true);
    }

    public int getPassedRate() {
        return labelNodeReportResponse.getPassedRate();
    }

    /**
     * Getter for test runs.
     *
     * @return list of current test runs, or empty list, if not exists
     */
    public List<TestRunNodeAdapter> getTestRuns() {
        if (Objects.nonNull(testRuns)) {
            return testRuns;
        }
        return new ArrayList<>();
    }

    /**
     * Getter for children nodes.
     *
     * @return list of current children nodes, or empty list, if not exists
     */
    public List<LabelNodeReportAdapter> getChildren() {
        if (Objects.nonNull(children)) {
            return children;
        }
        return new ArrayList<>();
    }

    public List<TestingReportLabelParam> getLabelParams() {
        return labelParams;
    }
}
