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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.qubership.atp.auth.springbootstarter.entities.UserInfo;
import org.qubership.atp.ram.config.MvcConfig;
import org.qubership.atp.ram.controllers.api.dto.history.FailPatternHistoryChangeDto;
import org.qubership.atp.ram.converters.history.FailPatternVersioningMapper;
import org.qubership.atp.ram.models.DefectPriority;
import org.qubership.atp.ram.models.FailPattern;
import org.qubership.atp.ram.services.RootCauseService;

@ExtendWith(MockitoExtension.class)
public class FailPatternVersioningMapperTest {

    private ModelMapper modelMapper;

    private FailPatternVersioningMapper failPatternVersioningMapper;

    @Mock
    RootCauseService rootCauseService;

    String rootCauseName = "Root cause";

    private FailPattern source;

    private List<String> tickets = Collections.singletonList("ticket");

    UserInfo userInfo;


    @BeforeEach
    public void setUp() {
        modelMapper = new MvcConfig().modelMapper();
        when(rootCauseService.getRootCauseNameById(any(UUID.class))).thenReturn(rootCauseName);
        userInfo = new UserInfo();
        userInfo.setFirstName("Firstname");
        userInfo.setLastName("LastName");
        userInfo.setUsername("username");
        source = new FailPattern();
        source.setFailReasonId(UUID.randomUUID());
        source.setJiraTickets(Collections.singletonList("ticket"));
        source.setMessage("message");
        source.setPatternDescription("description");
        source.setRule("rule");
        source.setModifiedBy(userInfo);
        source.setCreatedBy(userInfo);
        source.setPriority(DefectPriority.LOW);
        failPatternVersioningMapper = new FailPatternVersioningMapper(modelMapper, rootCauseService);
        failPatternVersioningMapper.setMapper(modelMapper);
        failPatternVersioningMapper.setupMapper();
    }

    @Test
    public void convertToFailPatternHistoryChangeEntityExists_allFieldsCorrectlyMapped() {

        FailPatternHistoryChangeDto failPatternHistoryChangeDto
                = failPatternVersioningMapper.map(source);

        assertEquals(failPatternHistoryChangeDto.getDescription(), source.getPatternDescription());
        assertEquals(failPatternHistoryChangeDto.getName(), source.getName());
        assertEquals(failPatternHistoryChangeDto.getModifiedBy(), userInfo.getFullName());
        assertEquals(failPatternHistoryChangeDto.getCreatedBy(), userInfo.getFullName());
        assertEquals(failPatternHistoryChangeDto.getDefect(), tickets);
        assertEquals(failPatternHistoryChangeDto.getFailReason(), rootCauseName);
        assertEquals(failPatternHistoryChangeDto.getPriority().toString(), source.getPriority().toString());
        assertEquals(failPatternHistoryChangeDto.getRule(), source.getRule());
        assertEquals(failPatternHistoryChangeDto.getMessage(), source.getMessage());
    }
}
