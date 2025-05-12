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

package org.qubership.atp.ram.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.qubership.atp.ram.TestRunsMock;
import org.qubership.atp.ram.client.CatalogueExecuteRequestFeignClient;
import org.qubership.atp.ram.client.CatalogueIntegrationFeignClient;
import org.qubership.atp.ram.client.CatalogueIssueFeignClient;
import org.qubership.atp.ram.client.CatalogueLabelFeignClient;
import org.qubership.atp.ram.client.CatalogueLabelTemplateFeignClient;
import org.qubership.atp.ram.client.CatalogueProjectFeignClient;
import org.qubership.atp.ram.client.CatalogueTestCaseFeignClient;
import org.qubership.atp.ram.client.CatalogueTestPlanFeignClient;
import org.qubership.atp.ram.client.CatalogueTestScenarioFeignClient;
import org.qubership.atp.ram.client.CatalogueTestScopeFeignClient;
import org.qubership.atp.ram.converter.DtoConvertService;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.models.TestCaseLastStatus;
import org.qubership.atp.ram.models.TestRun;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class TestCaseServiceTest {
    private TestCaseService testCaseService;
    private CatalogueService catalogueService;

    @Mock
    private CatalogueTestScopeFeignClient catalogueTestScopeFeignClient;
    @Mock
    private CatalogueProjectFeignClient catalogueProjectFeignClient;
    @Mock
    private CatalogueIntegrationFeignClient catalogueIntegrationFeignClient;
    @Mock
    private CatalogueTestPlanFeignClient catalogueTestPlanFeignClient;
    @Mock
    private CatalogueTestCaseFeignClient catalogueTestCaseFeignClient;
    @Mock
    private CatalogueTestScenarioFeignClient catalogueTestScenarioFeignClient;
    @Mock
    private CatalogueLabelTemplateFeignClient catalogueLabelTemplateFeignClient;
    @Mock
    private CatalogueLabelFeignClient catalogueLabelFeignClient;
    @Mock
    private CatalogueIssueFeignClient catalogueIssueFeignClient;
    @Mock
    private CatalogueExecuteRequestFeignClient catalogueExecuteRequestFeignClient;
    @Mock
    private DtoConvertService dtoConvertService;

    @Captor
    ArgumentCaptor<List<TestCaseLastStatus>> listTestCaseLastStatusCaptor;

    @BeforeEach
    public void setUp() {
        catalogueService = new CatalogueService(
        catalogueTestScopeFeignClient, catalogueProjectFeignClient, catalogueIntegrationFeignClient,
        catalogueTestPlanFeignClient, catalogueTestCaseFeignClient, catalogueTestScenarioFeignClient,
        catalogueLabelTemplateFeignClient, catalogueLabelFeignClient, catalogueIssueFeignClient,
        catalogueExecuteRequestFeignClient,
        dtoConvertService
        );
        testCaseService = new TestCaseService(catalogueService);
    }

    @Test
    public void updateCaseStatuses_whenHaveEmptyTestRun_CaseStatusesUpdateCorrectly() {
        TestRun testRun = TestRunsMock.generateSimpleTestRun(UUID.randomUUID());
        TestRun emptyTestRun = new TestRun();
        emptyTestRun.setTestCaseId(null);

        List<TestRun> listTestRun = new ArrayList<>();
        listTestRun.add(testRun);
        listTestRun.add(emptyTestRun);

        testCaseService.updateCaseStatuses(listTestRun);

        Mockito.verify(dtoConvertService, times(1)).convertList(listTestCaseLastStatusCaptor.capture(), any());

        Assertions.assertEquals(listTestCaseLastStatusCaptor.getValue().get(0).getStatus(), TestingStatuses.PASSED.getName(),
                "Testing status set correctly");
        Assertions.assertNotNull(listTestCaseLastStatusCaptor.getValue().get(0).getTestCaseId(),
                "TestCaseId is not null");
        Assertions.assertEquals(listTestCaseLastStatusCaptor.getValue().size(), 1, "TestRun with null testCaseID is not add");
    }
}
