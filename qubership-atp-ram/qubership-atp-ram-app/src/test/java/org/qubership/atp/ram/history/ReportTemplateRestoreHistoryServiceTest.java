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

package org.qubership.atp.ram.history;

import static org.mockito.Mockito.mock;

import org.javers.core.Javers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.atp.ram.models.ReportTemplate;
import org.qubership.atp.ram.service.history.impl.ReportTemplateRestoreHistoryService;
import org.qubership.atp.ram.service.history.impl.ValidateReferenceExistsService;
import org.qubership.atp.ram.services.ReportTemplatesService;

public class ReportTemplateRestoreHistoryServiceTest extends AbstractRestoreHistoryServiceTest<ReportTemplate> {


    @BeforeEach
    @Override
    public void setUp() {
        javers = mock(Javers.class);
        entityService = mock(ReportTemplatesService.class);
        validateReferenceExistsService = mock(ValidateReferenceExistsService.class);
        restoreHistoryService = new ReportTemplateRestoreHistoryService(javers, (ReportTemplatesService)entityService,
                validateReferenceExistsService);
        entity = mock(ReportTemplate.class);
    }

    @Test
    @Override
    public void testRestoreToRevision_ShadowNotFound() {
        super.testRestoreToRevision_ShadowNotFound();
    }

    @Test
    @Override
    public void testRestoreToRevision_Success() {
        super.testRestoreToRevision_Success();
    }


}
