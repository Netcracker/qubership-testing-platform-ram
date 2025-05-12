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

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.atp.ram.TestRunsMock;
import org.qubership.atp.ram.enums.Flags;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.Scope;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.services.CatalogueService;
import org.qubership.atp.ram.services.TestRunService;

public class RateCalculatorTest {

    private TestRunService testRunService;
    private RateCalculator rateCalculator;
    private CatalogueService catalogueService;

    @BeforeEach
    public void init() {
        testRunService = mock(TestRunService.class);
        catalogueService = mock(CatalogueService.class);
        rateCalculator = new RateCalculator(testRunService, catalogueService);
    }

    @Test
    public void calculateErRates() {
        ExecutionRequest executionRequest = new ExecutionRequest();
        List<TestRun> allTestRuns = createTestRuns();
        rateCalculator.calculateErRates(executionRequest, allTestRuns);
        assertNotNull(executionRequest);
        assertEquals(33, executionRequest.getPassedRate());
        assertEquals(50, executionRequest.getFailedRate());
        assertEquals(0, executionRequest.getWarningRate());
        assertEquals(6, executionRequest.getCountOfTestRuns());
    }

    @Test
    public void calculateErRates_withFlagsOnExecutionRequest_onlyExecutionCasesCounted() {
        ExecutionRequest executionRequest = new ExecutionRequest();
        List<TestRun> executionScopeTestRuns = createTestRuns();
        List<TestRun> allTestRuns = new ArrayList<>(executionScopeTestRuns);
        TestRun tr8 = TestRunsMock.generateTestRun("tr8", TestingStatuses.FAILED);
        TestRun tr9 = TestRunsMock.generateTestRun("tr9", TestingStatuses.PASSED);
        allTestRuns.add(tr8);
        allTestRuns.add(tr9);
        Scope scope = new Scope();
        scope.setUuid(UUID.fromString("ae178548-e7ae-485f-9a9b-93b415f2c69d"));
        scope.setExecutionCases(executionScopeTestRuns.stream().map(TestRun::getTestCaseId).collect(Collectors.toList()));
        scope.setPrerequisitesCases(asList(tr8.getTestCaseId()));
        scope.setValidationCases(asList(tr9.getTestCaseId()));
        executionRequest.setTestScopeId(scope.getUuid());
        executionRequest.setFlagIds(
                new HashSet<>(Arrays.asList(
                        Flags.IGNORE_PREREQUISITE_IN_PASS_RATE.getId(),
                        Flags.IGNORE_VALIDATION_IN_PASS_RATE.getId())));
        when(catalogueService.getTestScope(executionRequest.getTestScopeId())).thenReturn(scope);
        rateCalculator.calculateErRates(executionRequest, allTestRuns);
        assertNotNull(executionRequest);
        assertEquals(33, executionRequest.getPassedRate());
        assertEquals(50, executionRequest.getFailedRate());
        assertEquals(0, executionRequest.getWarningRate());
        assertEquals(6, executionRequest.getCountOfTestRuns());
    }

    @Test
    public void calculateErRates_withoutFlagsOnExecutionRequest_allCasesCounted() {
        ExecutionRequest executionRequest = new ExecutionRequest();
        List<TestRun> allTestRuns = createTestRuns();
        List<TestRun> executionScopeTestRuns = allTestRuns.subList(0, 5);
        Scope scope = new Scope();
        scope.setUuid(UUID.fromString("ae178548-e7ae-485f-9a9b-93b415f2c69d"));
        scope.setExecutionCases(executionScopeTestRuns.stream()
                .map(TestRun::getTestCaseId).collect(Collectors.toList()));
        scope.setPrerequisitesCases(asList(allTestRuns.get(5).getTestCaseId()));
        scope.setValidationCases(asList(allTestRuns.get(6).getTestCaseId()));
        executionRequest.setTestScopeId(scope.getUuid());
        when(catalogueService.getTestScope(executionRequest.getTestScopeId())).thenReturn(scope);
        rateCalculator.calculateErRates(executionRequest, allTestRuns);
        assertNotNull(executionRequest);
        assertEquals(33, executionRequest.getPassedRate());
        assertEquals(50, executionRequest.getFailedRate());
        assertEquals(0, executionRequest.getWarningRate());
        assertEquals(6, executionRequest.getCountOfTestRuns());
    }

    public List<TestRun> createTestRuns() {
        TestRun tr1 = TestRunsMock.generateTestRun("tr1", TestingStatuses.PASSED);
        TestRun tr2 = TestRunsMock.generateTestRun("tr2", TestingStatuses.PASSED);
        TestRun tr3 = TestRunsMock.generateTestRun("tr3", TestingStatuses.FAILED);
        TestRun tr4 = TestRunsMock.generateTestRun("tr3", TestingStatuses.FAILED);
        TestRun tr5 = TestRunsMock.generateTestRun("tr5", TestingStatuses.SKIPPED);
        TestRun tr6 = TestRunsMock.generateTestRun("tr6", TestingStatuses.STOPPED);
        TestRun tr7 = TestRunsMock.generateTestRun("tr7", TestingStatuses.FAILED);
        return asList(tr1, tr2, tr3, tr4, tr5, tr6, tr7);
    }
}
