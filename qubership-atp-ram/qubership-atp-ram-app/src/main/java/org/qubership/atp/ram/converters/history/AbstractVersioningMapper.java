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

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.qubership.atp.ram.controllers.api.dto.history.AbstractCompareEntityDto;
import org.qubership.atp.ram.models.DateAuditorEntity;

public abstract class AbstractVersioningMapper<S extends DateAuditorEntity, D extends AbstractCompareEntityDto>
        extends AbstractMapper<S, D> {

    AbstractVersioningMapper(Class<S> sourceClass,
                             Class<D> destinationClass,
                             ModelMapper mapper) {
        super(sourceClass, destinationClass, mapper);
    }

    @PostConstruct
    public void setupMapper() {
        mapper.createTypeMap(sourceClass, destinationClass).setPostConverter(mapConverter());
    }

    @Override
    public void mapSpecificFields(S source, D destination) {
        if (source != null) {
            if (source.getModifiedBy() != null) {
                String fullName = source.getModifiedBy().getFullName();
                destination.setModifiedBy(StringUtils.isEmpty(fullName)
                        ? source.getModifiedBy().getUsername()
                        : fullName);
            }
            if (source.getCreatedBy() != null) {
                String fullName = source.getCreatedBy().getFullName();
                destination.setCreatedBy(StringUtils.isEmpty(fullName)
                        ? source.getCreatedBy().getUsername()
                        : fullName);
            }
        }
    }
}
