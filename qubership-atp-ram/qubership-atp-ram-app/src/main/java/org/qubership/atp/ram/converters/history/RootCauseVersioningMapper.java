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
import org.qubership.atp.ram.controllers.api.dto.history.RootCauseHistoryChangeDto;
import org.qubership.atp.ram.models.RamObject;
import org.qubership.atp.ram.models.RootCause;
import org.qubership.atp.ram.services.RootCauseService;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RootCauseVersioningMapper extends AbstractVersioningMapper<RootCause,
        RootCauseHistoryChangeDto> {

    private final RootCauseService rootCauseService;

    public RootCauseVersioningMapper(ModelMapper mapper, RootCauseService rootCauseService) {
        super(RootCause.class, RootCauseHistoryChangeDto.class, mapper);
        this.rootCauseService = rootCauseService;
    }

    @Override
    public void mapSpecificFields(RootCause source, RootCauseHistoryChangeDto destination) {
        super.mapSpecificFields(source, destination);
        destination.setChildren(rootCauseService
                .getRootCausesByParentId(source.getUuid())
                .stream()
                .map(RamObject::getName)
                .collect(Collectors.toList())
        );
        destination.setParent(rootCauseService.getRootCauseNameById(source.getParentId()));
        destination.setStatus(source.isDisabled() ? "Disabled" : "Enabled");
    }
}
