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

package org.qubership.atp.ram.service.emailsubjectmacros.impl;

import static org.springframework.util.CollectionUtils.isEmpty;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.qubership.atp.ram.dto.response.ExecutionSummaryResponse;
import org.qubership.atp.ram.dto.response.TestCaseLabelResponse;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.RamObject;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.service.emailsubjectmacros.ResolvableEmailSubjectMacros;
import org.qubership.atp.ram.services.CatalogueService;
import org.qubership.atp.ram.services.TestRunService;
import org.qubership.atp.ram.utils.StreamUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class GroupLabelsEmailSubjectMacros implements ResolvableEmailSubjectMacros {

    @Autowired
    private CatalogueService catalogueService;

    @Autowired
    private TestRunService testRunService;

    @Override
    public String resolve(ExecutionRequest executionRequest, ExecutionSummaryResponse executionSummaryResponse) {
        UUID executionRequestId = executionRequest.getUuid();
        log.debug("Start resolving macros 'GROUP_LABELS' for execution request with id: {}", executionRequestId);

        List<TestRun> testRuns = testRunService.findAllByExecutionRequestId(executionRequestId);
        log.debug("Found test runs: {}", StreamUtils.extractIds(testRuns));

        List<TestCaseLabelResponse> testCaseLabelResponses = catalogueService.getTestCaseLabelsByIds(testRuns);
        log.debug("Found test cases from catalogue: {}",
                StreamUtils.extractFields(testCaseLabelResponses, TestCaseLabelResponse::getUuid));

        String groupLabelsValue = testCaseLabelResponses.stream()
                .filter(response -> !isEmpty(response.getLabels()))
                .flatMap(response -> response.getLabels().stream())
                .map(RamObject::getName)
                .distinct()
                .collect(Collectors.joining(", "));

        log.debug("Resolved value: {}", groupLabelsValue);

        return groupLabelsValue;
    }
}
