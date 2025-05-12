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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.qubership.atp.auth.springbootstarter.entities.UserInfo;
import org.qubership.atp.ram.config.MvcConfig;
import org.qubership.atp.ram.controllers.api.dto.history.RootCauseHistoryChangeDto;
import org.qubership.atp.ram.converters.history.RootCauseVersioningMapper;
import org.qubership.atp.ram.models.RootCause;
import org.qubership.atp.ram.services.RootCauseService;

@ExtendWith(MockitoExtension.class)
public class RootCauseVersioningMapperTest {

    private ModelMapper modelMapper;

    private RootCauseVersioningMapper rootCauseVersioningMapper;

    @Mock
    RootCauseService rootCauseService;

    String childRootCauseName = "Child Root cause";

    String parentRootCauseName = "Parent Root cause";

    private RootCause source;

    UserInfo userInfo;


    @BeforeEach
    public void setUp() {
        modelMapper = new MvcConfig().modelMapper();
        userInfo = new UserInfo();
        userInfo.setFirstName("Firstname");
        userInfo.setLastName("LastName");
        userInfo.setUsername("username");
        source = new RootCause();
        source.setParentId(UUID.randomUUID());
        source.setDisabled(true);
        source.setUuid(UUID.randomUUID());
        source.setProjectId(UUID.randomUUID());source.setModifiedBy(userInfo);
        source.setCreatedBy(userInfo);
        RootCause childRootCause = new RootCause();
        childRootCause.setName(childRootCauseName);

        when(rootCauseService.getRootCauseNameById(source.getParentId())).thenReturn(parentRootCauseName);
        when(rootCauseService.getRootCausesByParentId(source.getUuid())).thenReturn(Collections.singletonList(childRootCause));

        rootCauseVersioningMapper = new RootCauseVersioningMapper(modelMapper, rootCauseService);
        rootCauseVersioningMapper.setMapper(modelMapper);
        rootCauseVersioningMapper.setupMapper();
    }

    @Test
    public void convertToRootCauseHistoryChangeEntityExists_allFieldsCorrectlyMapped() {

        RootCauseHistoryChangeDto rootCauseHistoryChangeDto
                = rootCauseVersioningMapper.map(source);

        assertEquals(rootCauseHistoryChangeDto.getName(), source.getName());
        assertEquals(rootCauseHistoryChangeDto.getModifiedBy(), userInfo.getFullName());
        assertEquals(rootCauseHistoryChangeDto.getCreatedBy(), userInfo.getFullName());
        assertEquals(rootCauseHistoryChangeDto.getChildren().get(0), childRootCauseName);
        assertEquals(rootCauseHistoryChangeDto.getParent(), parentRootCauseName);
    }
}
