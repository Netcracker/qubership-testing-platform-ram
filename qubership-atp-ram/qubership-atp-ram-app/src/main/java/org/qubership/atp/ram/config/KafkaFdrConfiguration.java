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

import org.qubership.atp.ram.tsg.model.FdrResponse;
import org.qubership.atp.ram.tsg.model.TsgFdr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
@ConditionalOnProperty(
        value = "kafka.fdr.enable",
        matchIfMissing = false
)
public class KafkaFdrConfiguration {

    @Autowired
    KafkaCommonConfiguration kafkaCommonConfiguration;

    public static final String KAFKA_FDR_CONTAINER_FACTORY_NAME = "fdrContainerFactory";

    /**
     * Factory for kafka fdr-links topic listener.
     */
    @Bean(KAFKA_FDR_CONTAINER_FACTORY_NAME)
    public KafkaListenerContainerFactory<?> fdrContainerFactory(ConsumerFactory consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, FdrResponse> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }

    @Bean
    public KafkaTemplate<String, TsgFdr> fdrKafkaTemplate() {
        return new KafkaTemplate<>(kafkaCommonConfiguration.producerFactory());
    }
}
