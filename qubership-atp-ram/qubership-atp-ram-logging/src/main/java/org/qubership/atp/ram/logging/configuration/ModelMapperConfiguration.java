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

package org.qubership.atp.ram.logging.configuration;

import java.sql.Timestamp;

import org.modelmapper.AbstractConverter;
import org.modelmapper.AbstractProvider;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.modelmapper.Provider;
import org.qubership.atp.ram.logging.constants.ApiPathLogging;
import org.qubership.atp.ram.logging.entities.requests.CreatedLogRecordRequest;
import org.qubership.atp.ram.models.LogRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfiguration {

    /**
     * Initialize model mapper bean.
     */
    @Bean(name = ApiPathLogging.MAPPER_FOR_LOGGING_BEAN_NAME)
    public ModelMapper initModelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setSkipNullEnabled(true);
        addDateConverted(modelMapper);
        addPropertyMap(modelMapper);
        return modelMapper;
    }

    private void addDateConverted(ModelMapper modelMapper) {
        Provider<Timestamp> localDateProvider = new AbstractProvider<Timestamp>() {
            @Override
            public Timestamp get() {
                return new Timestamp(System.currentTimeMillis());
            }
        };

        Converter<String, Timestamp> toStringDate = new AbstractConverter<String, Timestamp>() {
            @Override
            protected Timestamp convert(String source) {
                return Timestamp.valueOf(source);
            }
        };

        modelMapper.createTypeMap(String.class, Timestamp.class);
        modelMapper.addConverter(toStringDate);
        modelMapper.getTypeMap(String.class, Timestamp.class).setProvider(localDateProvider);
    }

    private void addPropertyMap(ModelMapper modelMapper) {
        PropertyMap<CreatedLogRecordRequest, LogRecord> propertyMap
                = new PropertyMap<CreatedLogRecordRequest, LogRecord>() {
            protected void configure() {
                map().setUuid(source.getLogRecordUuid());
                map().setParentRecordId(source.getParentRecordUuid());
                map().setCompaund(source.isCompaund());
            }
        };

        modelMapper.addMappings(propertyMap);
    }
}
