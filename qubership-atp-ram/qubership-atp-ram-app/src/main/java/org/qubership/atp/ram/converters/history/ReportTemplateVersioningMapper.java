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

package org.qubership.atp.ram.converters.history;

import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.qubership.atp.ram.controllers.api.dto.history.ReportTemplateHistoryChangeDto;
import org.qubership.atp.ram.models.ReportTemplate;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ReportTemplateVersioningMapper extends AbstractVersioningMapper<ReportTemplate,
        ReportTemplateHistoryChangeDto> {

    public ReportTemplateVersioningMapper(ModelMapper mapper) {
        super(ReportTemplate.class, ReportTemplateHistoryChangeDto.class, mapper);
    }

    @Override
    public void mapSpecificFields(ReportTemplate source, ReportTemplateHistoryChangeDto destination) {
        super.mapSpecificFields(source, destination);
        destination.setSections(source.getWidgets()
                .stream()
                .map(Enum::toString)
                .collect(Collectors.toList()));

    }
}
