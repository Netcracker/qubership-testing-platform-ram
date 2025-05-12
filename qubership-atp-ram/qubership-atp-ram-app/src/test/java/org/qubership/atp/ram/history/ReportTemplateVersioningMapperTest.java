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

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.qubership.atp.auth.springbootstarter.entities.UserInfo;
import org.qubership.atp.ram.config.MvcConfig;
import org.qubership.atp.ram.controllers.api.dto.history.ReportTemplateHistoryChangeDto;
import org.qubership.atp.ram.converters.history.ReportTemplateVersioningMapper;
import org.qubership.atp.ram.models.ReportTemplate;
import org.qubership.atp.ram.models.WidgetType;
import org.qubership.atp.ram.services.ReportTemplatesService;

@ExtendWith(MockitoExtension.class)
public class ReportTemplateVersioningMapperTest {

    private ModelMapper modelMapper;

    private ReportTemplateVersioningMapper reportTemplateVersioningMapper;

    @Mock
    ReportTemplatesService reportTemplatesService;

    List<String> recipients = Collections.singletonList("test@some-domain");

    List<WidgetType> sections = Collections.singletonList(WidgetType.ENVIRONMENTS_INFO);

    private ReportTemplate source;

    UserInfo userInfo;


    @BeforeEach
    public void setUp() {
        modelMapper = new MvcConfig().modelMapper();
        userInfo = new UserInfo();
        userInfo.setFirstName("Firstname");
        userInfo.setLastName("LastName");
        userInfo.setUsername("username");
        source = new ReportTemplate();
        source.setActive(true);
        source.setRecipients(recipients);
        source.setUuid(UUID.randomUUID());
        source.setProjectId(UUID.randomUUID());
        source.setModifiedBy(userInfo);
        source.setCreatedBy(userInfo);
        source.setWidgets(sections);

        reportTemplateVersioningMapper = new ReportTemplateVersioningMapper(modelMapper);
        reportTemplateVersioningMapper.setMapper(modelMapper);
        reportTemplateVersioningMapper.setupMapper();
    }

    @Test
    public void convertToReportTemplateHistoryChangeEntityExists_allFieldsCorrectlyMapped() {

        ReportTemplateHistoryChangeDto reportTemplateHistoryChangeDto
                = reportTemplateVersioningMapper.map(source);

        assertEquals(reportTemplateHistoryChangeDto.getName(), source.getName());
        assertEquals(reportTemplateHistoryChangeDto.getModifiedBy(), userInfo.getFullName());
        assertEquals(reportTemplateHistoryChangeDto.getCreatedBy(), userInfo.getFullName());
        assertEquals(reportTemplateHistoryChangeDto.getSections(), sections.stream().map(Enum::toString).collect(Collectors.toList()));
    }
}
