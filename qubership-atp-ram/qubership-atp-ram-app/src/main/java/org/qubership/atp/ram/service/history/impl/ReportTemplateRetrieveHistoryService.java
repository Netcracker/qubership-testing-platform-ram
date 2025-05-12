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

package org.qubership.atp.ram.service.history.impl;

import org.javers.core.Javers;
import org.qubership.atp.ram.controllers.api.dto.history.HistoryItemTypeDto;
import org.qubership.atp.ram.models.ReportTemplate;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ReportTemplateRetrieveHistoryService extends AbstractRetrieveHistoryService {

    public ReportTemplateRetrieveHistoryService(Javers javers) {
        super(javers);
    }

    @Override
    public HistoryItemTypeDto getItemType() {
        return HistoryItemTypeDto.EMAILTEMPLATE;
    }

    @Override
    public Class<ReportTemplate> getEntityClass() {
        return ReportTemplate.class;
    }
}
