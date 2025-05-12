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

import org.qubership.atp.ram.kafka.listeners.test.plan.TestPlanResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

@Configuration
@ConditionalOnProperty(
        value = "kafka.test.plan.event.enable"
)
public class KafkaTestPlanConfiguration {
    @Autowired
    KafkaCommonConfiguration kafkaCommonConfiguration;

    public static final String KAFKA_TEST_PLAN_CONTEINER_FACTORY_NAME = "testPlanContainerFactory";

    /**
     * Factory kafka topic listener for test plans.
     */
    @Bean(KAFKA_TEST_PLAN_CONTEINER_FACTORY_NAME)
    public KafkaListenerContainerFactory<?> testPlanContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<UUID, TestPlanResponse> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setErrorHandler((e, consumerRecord) -> {
            throw new RuntimeException("Error during event processing.", e);
        });
        return factory;
    }

    public ConsumerFactory<UUID, TestPlanResponse> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(kafkaCommonConfiguration.consumerConfigs());
    }
}
