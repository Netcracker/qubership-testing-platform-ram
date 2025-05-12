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

package org.qubership.atp.ram.config;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.qubership.atp.ram.clients.api.dto.catalogue.FieldsDto;
import org.qubership.atp.ram.converter.OffsetDateTime2TimestampConverter;
import org.qubership.atp.ram.converter.Timestamp2OffsetDateTimeConverter;
import org.qubership.atp.ram.deserializers.DateDeserializer;
import org.qubership.atp.ram.deserializers.TimestampDeserializer;
import org.qubership.atp.ram.model.jira.Fields;
import org.qubership.atp.ram.models.usersettings.TableColumnVisibilityUserSetting;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

@Configuration
public class MvcConfig {

    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    /**
     * Object mapper config.
     * see how spring does it in class: org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
     * method: jacksonObjectMapper(org.springframework.http.converter.json.Jackson2ObjectMapperBuilder)
     *
     * @return object mapper
     */
    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper mapper = builder
                .createXmlMapper(false)
                .dateFormat(new SimpleDateFormat(DATE_FORMAT))
                .build();

        SimpleModule module = new SimpleModule();
        module.addDeserializer(Timestamp.class, new TimestampDeserializer());
        module.addDeserializer(Date.class, new DateDeserializer());
        mapper.registerModule(module);
        return mapper;
    }

    /**
     * Create {@link ModelMapper} bean and set configuration.
     *
     * @return configured mapper
     */
    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setSkipNullEnabled(true);
        modelMapper.getConfiguration().setAmbiguityIgnored(true);
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STANDARD);

        modelMapper.typeMap(Fields.class, FieldsDto.class)
                .addMappings(m -> m.map(Fields::getAtpLink, FieldsDto::setCustomfield17400))
                .addMappings(m -> m.<String>map(
                        src -> src.getFoundIn().getValue(),
                        (dest, v) -> dest.getCustomfield10014().setValue(v)));

        modelMapper.getConfiguration().getConverters().add(new Timestamp2OffsetDateTimeConverter());
        modelMapper.getConfiguration().getConverters().add(new OffsetDateTime2TimestampConverter());

        modelMapper.createTypeMap(TableColumnVisibilityUserSetting.class, TableColumnVisibilityUserSetting.class)
                .addMappings(map -> map.using(mappingContext -> {
                    List<String> source = (List<String>) mappingContext.getSource();
                    if (source != null) {
                        return source
                                .stream()
                                .map(String::new)
                                .collect(Collectors.toList());
                    }
                    return new ArrayList<>();
                }).map(TableColumnVisibilityUserSetting::getVisibleColumns,
                        TableColumnVisibilityUserSetting::setVisibleColumns));


        return modelMapper;
    }

}
