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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.javers.core.Javers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.atp.ram.models.FailPattern;
import org.qubership.atp.ram.service.history.impl.FailPatternRestoreHistoryService;
import org.qubership.atp.ram.service.history.impl.ValidateReferenceExistsService;
import org.qubership.atp.ram.services.FailPatternService;
import org.qubership.atp.ram.services.RootCauseService;

public class FailPatternRestoreHistoryServiceTest extends AbstractRestoreHistoryServiceTest<FailPattern> {


    @BeforeEach
    @Override
    public void setUp() {
        javers = mock(Javers.class);
        entityService = mock(FailPatternService.class);
        validateReferenceExistsService = mock(ValidateReferenceExistsService.class);
        restoreHistoryService = new FailPatternRestoreHistoryService(javers, (FailPatternService)entityService,
                validateReferenceExistsService, mock(RootCauseService.class));
        when(entityService.get(any())).thenReturn(new FailPattern());
        entity = mock(FailPattern.class);
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
