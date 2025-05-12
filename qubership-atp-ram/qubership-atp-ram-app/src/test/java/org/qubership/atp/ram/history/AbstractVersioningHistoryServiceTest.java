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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.javers.core.Javers;
import org.javers.repository.jql.JqlQuery;
import org.javers.shadow.Shadow;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.atp.auth.springbootstarter.exceptions.AtpEntityNotFoundException;
import org.qubership.atp.ram.controllers.api.dto.history.AbstractCompareEntityDto;
import org.qubership.atp.ram.controllers.api.dto.history.CompareEntityResponseDto;
import org.qubership.atp.ram.models.DateAuditorEntity;
import org.qubership.atp.ram.service.history.impl.AbstractVersioningHistoryService;

@ExtendWith(MockitoExtension.class)
public abstract class AbstractVersioningHistoryServiceTest <T extends DateAuditorEntity,
        E extends AbstractCompareEntityDto> {

    @Mock
    protected Javers javers;

    protected AbstractVersioningHistoryService<T, E> service;

    protected UUID id;
    protected String version;
    protected DateAuditorEntity entity;
    protected Shadow shadow;
    protected AbstractCompareEntityDto dto;
    protected CompareEntityResponseDto responseDto;

    protected abstract void setUp();

    protected void testGetEntitiesByVersions() {
        when(shadow.get()).thenReturn(entity);
        when(javers.findShadows(any(JqlQuery.class))).thenReturn(Arrays.asList(shadow));
        List<CompareEntityResponseDto> results = service.getEntitiesByVersions(id, Arrays.asList(version));

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(responseDto, results.get(0));
        verify(javers, times(1)).findShadows(any(JqlQuery.class));
    }

    protected void testGetEntityByVersion_EntityFound() {
        when(shadow.get()).thenReturn(entity);
        when(javers.findShadows(any(JqlQuery.class))).thenReturn(Arrays.asList(shadow));
        CompareEntityResponseDto result = service.getEntityByVersion(version, id);

        assertNotNull(result);
        assertEquals(responseDto, result);
        verify(javers, times(1)).findShadows(any(JqlQuery.class));
    }

    protected void testGetEntityByVersion_EntityNotFound() {
        when(javers.findShadows(any(JqlQuery.class))).thenReturn(Arrays.asList());
        assertThrows(AtpEntityNotFoundException.class, () -> service.getEntityByVersion(version, id));
        verify(javers, times(1)).findShadows(any(JqlQuery.class));
    }
}
