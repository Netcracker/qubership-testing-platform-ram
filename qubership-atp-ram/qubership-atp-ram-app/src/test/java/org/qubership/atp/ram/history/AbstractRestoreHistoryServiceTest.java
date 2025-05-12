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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.javers.core.Javers;
import org.javers.repository.jql.JqlQuery;
import org.javers.shadow.Shadow;
import org.qubership.atp.ram.exceptions.history.RamHistoryRevisionRestoreException;
import org.qubership.atp.ram.models.DateAuditorEntity;
import org.qubership.atp.ram.service.history.impl.AbstractRestoreHistoryService;
import org.qubership.atp.ram.service.history.impl.ValidateReferenceExistsService;
import org.qubership.atp.ram.services.CrudService;

public abstract class  AbstractRestoreHistoryServiceTest <T extends DateAuditorEntity> {

    protected AbstractRestoreHistoryService<T> restoreHistoryService;
    protected Javers javers;
    protected CrudService<T> entityService;
    protected ValidateReferenceExistsService<T> validateReferenceExistsService;
    protected T entity;

    public abstract void setUp();

    public void testRestoreToRevision_ShadowNotFound() {
        UUID id = UUID.randomUUID();
        long revisionId = 1L;

        when(javers.findShadows(any(JqlQuery.class))).thenReturn(new ArrayList<>());

        assertThrows(RamHistoryRevisionRestoreException.class, () -> restoreHistoryService.restoreToRevision(id, revisionId));
    }

    public void testRestoreToRevision_Success() {
        UUID id = UUID.randomUUID();
        long revisionId = 1L;

        when(entityService.get(id)).thenReturn(entity);

        Shadow<Object> shadow = mock(Shadow.class);
        when(shadow.get()).thenReturn(entity);

        List<Shadow<Object>> shadows = new ArrayList<>();
        shadows.add(shadow);

        when(javers.findShadows(any(JqlQuery.class))).thenReturn(shadows);

        restoreHistoryService.restoreToRevision(id, revisionId);

        verify(validateReferenceExistsService, times(1)).validateEntity(entity);
        verify(entityService, times(1)).save(entity);
    }


}
