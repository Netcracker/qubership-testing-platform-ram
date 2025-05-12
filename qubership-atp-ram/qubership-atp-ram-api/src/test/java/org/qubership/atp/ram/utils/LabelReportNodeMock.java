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

package org.qubership.atp.ram.utils;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.qubership.atp.ram.LogRecordMock;
import org.qubership.atp.ram.dto.response.LabelNodeReportResponse;
import org.qubership.atp.ram.dto.response.LabelNodeReportResponse.FailedLogRecordNodeResponse;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.models.LabelTemplate;
import org.qubership.atp.ram.models.LogRecord;

import com.google.common.collect.Sets;
import lombok.experimental.UtilityClass;

@UtilityClass
public class LabelReportNodeMock {

    private static final ModelMapper modelMapper = new ModelMapper();

    public LabelNodeReportResponse.TestRunNodeResponse generateFailedTestRunNodeWithZeroDuration(String name,
                                                                                                 boolean stepsAreExists) {
        return generateTestRunNode(name, stepsAreExists, TestingStatuses.FAILED, 0);
    }

    public LabelNodeReportResponse.TestRunNodeResponse generateTestRunNode(String name, boolean stepsAreExists,
                                                                           TestingStatuses testingStatus,
                                                                           long duration) {
        LabelNodeReportResponse.TestRunNodeResponse testRunNodeResponse =
                new LabelNodeReportResponse.TestRunNodeResponse();
        testRunNodeResponse.setName(name);
        testRunNodeResponse.setFailureReason("RC");
        testRunNodeResponse.setTestingStatus(testingStatus);
        testRunNodeResponse.setDuration(duration);

        if (stepsAreExists) {
            List<LogRecord> logRecords = LogRecordMock.findLogRecordsByTestRunId();
            testRunNodeResponse.setFailedStep(toFailedLogRecordNodeResponses(logRecords));
        }
        return testRunNodeResponse;
    }

    public static List<FailedLogRecordNodeResponse> toFailedLogRecordNodeResponses(List<LogRecord> logRecords) {

        return logRecords.stream()
                .map(logRecord -> modelMapper.map(logRecord, FailedLogRecordNodeResponse.class))
                .collect(Collectors.toList());
    }

    public LabelTemplate.LabelTemplateNode generateNode(String name, Set<UUID> testRunIds,
                                                        List<LabelTemplate.LabelTemplateNode> children) {
        LabelTemplate.LabelTemplateNode node = new LabelTemplate.LabelTemplateNode();
        node.setTestRunIds(Sets.newHashSet(testRunIds));
        node.setChildren(children);
        node.setLabelName(name);
        return node;
    }

    public LabelNodeReportResponse generateFailedLabelNodeReportWithZeroDuration(String name,
                                                                                 List<LabelNodeReportResponse.TestRunNodeResponse> testRuns,
                                                                                 List<LabelNodeReportResponse> children) {
        return generateLabelNodeReport(name, testRuns, children, TestingStatuses.FAILED, 0);
    }

    public LabelNodeReportResponse generateLabelNodeReport(String name,
                                                           List<LabelNodeReportResponse.TestRunNodeResponse> testRuns,
                                                           List<LabelNodeReportResponse> children,
                                                           TestingStatuses testingStatus,
                                                           long duration) {
        LabelNodeReportResponse nodeReportResponse = new LabelNodeReportResponse();
        nodeReportResponse.setTestRuns(testRuns);
        nodeReportResponse.setLabelName(name);
        nodeReportResponse.setChildren(children);
        nodeReportResponse.setStatus(testingStatus);
        nodeReportResponse.setDuration(duration);
        return nodeReportResponse;
    }
}
