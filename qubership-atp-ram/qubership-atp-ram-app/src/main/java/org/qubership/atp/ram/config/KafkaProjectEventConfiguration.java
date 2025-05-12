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

import java.util.UUID;

import org.qubership.atp.auth.springbootstarter.exceptions.AtpKafkaListenerContainerFactoryException;
import org.qubership.atp.ram.kafka.listeners.project.event.ProjectEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import lombok.extern.slf4j.Slf4j;

@Configuration
@ConditionalOnProperty(
        value = "kafka.project.event.enable",
        matchIfMissing = false
)
@Slf4j
public class KafkaProjectEventConfiguration {

    public static final String KAFKA_PROJECT_EVENT_CONTAINER_FACTORY_NAME = "projectEventContainerFactory";

    @Autowired
    KafkaCommonConfiguration kafkaCommonConfiguration;

    /**
     * Factory for kafka project event topic listener.
     */
    @Bean(KAFKA_PROJECT_EVENT_CONTAINER_FACTORY_NAME)
    public KafkaListenerContainerFactory<?> projectEventContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<UUID, ProjectEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(projectEventConsumerFactory());
        factory.setErrorHandler((e, consumerRecord) -> {
            log.error("Error during kafka event processing in {}, consumerRecord: {}",
                    KAFKA_PROJECT_EVENT_CONTAINER_FACTORY_NAME, consumerRecord, e);
            throw new AtpKafkaListenerContainerFactoryException();
        });
        return factory;
    }

    /**
     * Custom kafka consumer factory.
     */
    @Bean
    public ConsumerFactory projectEventConsumerFactory() {
        return new DefaultKafkaConsumerFactory(kafkaCommonConfiguration.consumerConfigs());
    }
}
