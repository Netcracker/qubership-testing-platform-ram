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

package org.qubership.atp.ram.mapper;

import java.util.Objects;

import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Abstract custom mapper with possibility to customize mapping of necessary fields.
 *
 * @param <E> entity object type
 * @param <D> dto object type
 */
public abstract class AbstractMapper<E, D> implements Mapper<E, D> {

    @Autowired
    ModelMapper mapper;

    private Class<E> entityClass;
    private Class<D> dtoClass;

    AbstractMapper(Class<E> entityClass, Class<D> dtoClass) {
        this.entityClass = entityClass;
        this.dtoClass = dtoClass;
    }

    /**
     * Map entity object to destination object.
     *
     * @param entity entity object
     * @return dto object
     */
    @Override
    public D entityToDto(E entity) {
        return Objects.isNull(entity)
                ? null
                : mapper.map(entity, dtoClass);
    }

    /**
     * Need to apply to mapper if custom {@link AbstractMapper#mapSpecificFields(E, D)} is implemented.
     *
     * @return custom converter
     */
    Converter<E, D> mapConverter() {
        return context -> {
            E entity = context.getSource();
            D dto = context.getDestination();
            mapSpecificFields(entity, dto);
            return context.getDestination();
        };
    }

    /**
     * Need to implement to customize mapper's logic.
     *
     * @param entity entity object
     * @param dto    dto object
     */
    void mapSpecificFields(E entity, D dto) {
    }
}
