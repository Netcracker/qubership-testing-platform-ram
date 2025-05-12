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
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import org.qubership.atp.ram.entities.treenodes.labelparams.TestingReportLabelParam;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.models.Comment;
import org.qubership.atp.ram.models.FinalRunData;
import org.qubership.atp.ram.models.Label;
import org.qubership.atp.ram.models.TestRun;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LabelNodeReportResponse {

    private String labelName;
    private TestingStatuses status;
    private long duration;
    private int passedRate;
    private UUID labelTemplateId;
    private String labelTemplateName;
    private List<LabelNodeReportResponse> children;
    private List<TestRunNodeResponse> testRuns;
    private List<TestingReportLabelParam> labelParams = new ArrayList<>();
    private List<String> validationLabelsOrder = new ArrayList<>();
    @JsonIgnore
    private Supplier<List<TestRun>> testRunsFilterFunc;
    private boolean isGroupedNode;

    public LabelNodeReportResponse(String labelName) {
        this.labelName = labelName;
    }

    /**
     * LabelNodeReportResponse constructor.
     */
    public LabelNodeReportResponse(String labelName, boolean isGroupedNode,
                                   Supplier<List<TestRun>> testRunsFilterFunc) {
        this.labelName = labelName;
        this.testRunsFilterFunc = testRunsFilterFunc;
        this.isGroupedNode = isGroupedNode;
    }

    /**
     * Getter for test runs.
     *
     * @return list of current test runs, or empty list, if not exists
     */
    public List<TestRunNodeResponse> getTestRuns() {
        if (Objects.isNull(testRuns)) {
            return new ArrayList<>();
        }
        return testRuns;
    }

    /**
     * Getter for children nodes.
     *
     * @return list of current children nodes, or empty list, if not exists
     */
    public List<LabelNodeReportResponse> getChildren() {
        if (Objects.nonNull(children)) {
            return children;
        }
        return new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TestRunNodeResponse {

        private String name;
        private UUID uuid;
        private TestingStatuses testingStatus;
        private TestingStatuses firstStatus;
        private TestingStatuses finalStatus;
        private int passedRate;
        private long duration;
        private String failureReason;
        private String dataSetUrl;
        private UUID testCaseId;
        private UUID parentRecordId;
        private List<FailedLogRecordNodeResponse> failedStep;
        private String dataSetListUrl;
        private String dataSetName;
        private List<TestingReportLabelParam> labelParams = new ArrayList<>();
        private String jiraTicket;
        private Set<String> issues;
        private Comment comment;
        private List<Label> labels;
        private FinalRunData finalRun;

        public TestRunNodeResponse(String name) {
            this.name = name;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailedLogRecordNodeResponse {

        private UUID uuid;
        private String name;
    }
}
