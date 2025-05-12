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

import org.qubership.atp.integration.configuration.model.KafkaMailResponse;
import org.qubership.atp.ram.kafka.listeners.project.event.filter.MailsResponsesFilterStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;

@Configuration
@ConditionalOnProperty(
        value = "kafka.mails.responses.enable",
        matchIfMissing = false
)
public class KafkaMailsResponsesConfiguration {

    @Autowired
    KafkaCommonConfiguration kafkaCommonConfiguration;

    public static final String KAFKA_MAILS_RESPONSES_CONTAINER_FACTORY_NAME = "mailsResponsesContainerFactory";

    /**
     * Factory for kafka mails responses topic listener.
     */
    @Bean(KAFKA_MAILS_RESPONSES_CONTAINER_FACTORY_NAME)
    public KafkaListenerContainerFactory<?> mailsResponsesContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, KafkaMailResponse> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(mailsResponseConsumerFactory());
        factory.setRecordFilterStrategy((RecordFilterStrategy) new MailsResponsesFilterStrategy());
        return factory;
    }

    public ConsumerFactory<String, KafkaMailResponse> mailsResponseConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(kafkaCommonConfiguration.consumerConfigs());
    }
}
