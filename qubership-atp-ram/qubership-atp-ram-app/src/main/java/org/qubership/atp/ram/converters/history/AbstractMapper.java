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

import java.util.Objects;

import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;

import lombok.Setter;

public abstract class AbstractMapper<S, D> implements Mapper<S, D> {

    @Setter
    protected ModelMapper mapper;

    protected Class<S> sourceClass;
    protected Class<D> destinationClass;

    /**
     * Abstract constructor for mapper.
     */
    public AbstractMapper(Class<S> sourceClass, Class<D> destinationClass, ModelMapper mapper) {
        this.sourceClass = sourceClass;
        this.destinationClass = destinationClass;
        this.mapper = mapper;
    }

    /**
     * Map source object to destination object.
     *
     * @param source source object
     * @return destination object
     */
    @Override
    public D map(S source) {
        return Objects.isNull(source)
                ? null
                : mapper.map(source, destinationClass);
    }

    /**
     * Need to apply to mapper if custom {@link AbstractMapper#mapSpecificFields(S, D)} is implemented.
     *
     * @return custom converter
     */
    public Converter<S, D> mapConverter() {
        return context -> {
            S source = context.getSource();
            D destination = context.getDestination();
            mapSpecificFields(source, destination);
            return context.getDestination();
        };
    }

    /**
     * Need to implement to customize mapper's logic.
     *
     * @param source      source object
     * @param destination destination object
     */
    public void mapSpecificFields(S source, D destination) {
    }
}
