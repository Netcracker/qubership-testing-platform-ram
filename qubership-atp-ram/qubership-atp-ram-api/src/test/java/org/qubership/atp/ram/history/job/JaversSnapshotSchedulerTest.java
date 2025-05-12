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

package org.qubership.atp.ram.history.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.atp.ram.model.JaversCountResponse;
import org.qubership.atp.ram.model.JaversIdsResponse;
import org.qubership.atp.ram.model.JaversVersionsResponse;
import org.qubership.atp.ram.repositories.JaversSnapshotRepository;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class JaversSnapshotSchedulerTest {

    @Mock
    private JaversSnapshotRepository repository;

    @InjectMocks
    private JaversSnapshotScheduler service;

    private List<String> listOfCdoId;
    private JaversCountResponse javersCountResponse;
    private JaversVersionsResponse javersVersionsResponse;

    @BeforeEach
    void setUp() {
        listOfCdoId = Arrays.asList("cdo1", "cdo2");
        javersCountResponse = new JaversCountResponse();
        javersCountResponse.setCdoId("cdo1");
        javersCountResponse.setVersions(Arrays.asList(1L, 2L, 3L));
        javersVersionsResponse = new JaversVersionsResponse();
        javersVersionsResponse.setCdoId("cdo1");
        javersVersionsResponse.setVersion(1L);
    }

    @Test
    void testFindCdoIdAndCount() {
        when(repository.findCdoIdAndCount(listOfCdoId)).thenReturn(Collections.singletonList(javersCountResponse));

        List<JaversCountResponse> result = service.findCdoIdAndCount(listOfCdoId);

        assertEquals(1, result.size());
        assertEquals("cdo1", result.get(0).getCdoId());
        verify(repository, times(1)).findCdoIdAndCount(listOfCdoId);
    }

    @Test
    void testFindOldObjects() {
        int maxCount = 2;
        ReflectionTestUtils.setField(service, "maxCount", maxCount);
        List<JaversVersionsResponse> result = service.findOldObjects(javersCountResponse);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getVersion());
        assertEquals("cdo1", result.get(0).getCdoId());
    }

    @Test
    void testFindObjectWithMinVersion() {
        List<JaversVersionsResponse> oldObjects = Collections.singletonList(javersVersionsResponse);

        Long minVersion = service.findObjectWithMinVersion(javersCountResponse, oldObjects);

        assertNotNull(minVersion);
        assertEquals(2L, minVersion);
    }

    @Test
    void testDeleteByCdoIdAndVersions() {
        List<Long> versions = Arrays.asList(1L, 2L, 3L);
        ReflectionTestUtils.setField(service, "bulkDeleteCount", 500);
        service.deleteByCdoIdAndVersions("cdo1", versions);

        verify(repository, times(1)).deleteByCdoIdAndVersions("cdo1", versions);
    }

    @Test
    void testUpdateObjectAsInitial() {
        service.updateObjectAsInitial("cdo1", 1L);

        verify(repository, times(1)).updateAsInitial("cdo1", 1L);
    }

    @Test
    void testFindTerminatedCdoId() {
        JaversIdsResponse response = new JaversIdsResponse();
        response.setCdoId("cdo1");
        List<JaversIdsResponse> snapshots = Collections.singletonList(response);
        when(repository.findTerminatedSnapshots()).thenReturn(snapshots);

        List<String> terminatedCdoId = service.findTerminatedCdoId();

        assertEquals(1, terminatedCdoId.size());
        assertEquals("cdo1", terminatedCdoId.get(0));
    }

    @Test
    void testDeleteTerminatedSnapshots() {
        JaversIdsResponse response = new JaversIdsResponse();
        response.setCdoId("cdo1");
        List<JaversIdsResponse> terminatedCdo = Collections.singletonList(response);
        List<String> terminatedCdoId = Collections.singletonList("cdo1");
        doReturn(terminatedCdo).when(repository).findTerminatedSnapshots();
        ReflectionTestUtils.setField(service, "bulkDeleteCount", 500);

        service.deleteTerminatedSnapshots();

        verify(repository, times(1)).deleteByCdoIds(terminatedCdoId);
    }

}
