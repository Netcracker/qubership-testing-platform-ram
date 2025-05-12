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
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.qubership.atp.ram.client.CatalogueTestCaseFeignClient;
import org.qubership.atp.ram.converter.DtoConvertService;
import org.qubership.atp.ram.models.TestRun;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class CatalogueServiceTest {
    @InjectMocks
    private CatalogueService catalogueService;
    @Mock
    private CatalogueTestCaseFeignClient catalogueTestCaseFeignClient;
    @Mock
    private DtoConvertService dtoConvertService;

    @Test
    public void getTestCaseLabelsByIds_WithEmptyTestCaseSet_DoesNotRequestToCatalog() {
        catalogueService.getTestCaseLabelsByIds(new ArrayList<>());

        verify(catalogueTestCaseFeignClient, times(0)).getCaseLabels(any());
    }

    @Test
    public void getTestCaseLabelsByIds_TestRunWithNullTestCaseId_DoesNotRequestToCatalog() {
        TestRun testRun = new TestRun();
        testRun.setUuid(UUID.randomUUID());
        List<TestRun> testRuns = Collections.singletonList(testRun);

        catalogueService.getTestCaseLabelsByIds(testRuns);

        verify(catalogueTestCaseFeignClient, times(0)).getCaseLabels(any());
    }
}
