/*
 * # Copyright 2024-2026 NetCracker Technology Corporation
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
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;

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

        // Create error handler with default configuration.
        //  The 2nd (optional) parameter is to set retry config,
        //  For example:
        //      - new FixedBackOff(1000L, 3L) // Timeout 1 second, 3 attempts max.
        CommonErrorHandler commonErrorHandler = new DefaultErrorHandler(
                // recoverer - What to do after all retries are over
                (record, exception) -> {
                    throw new RuntimeException("Error during event processing.", exception);
                });
        factory.setCommonErrorHandler(commonErrorHandler);
        return factory;
    }

    public ConsumerFactory<UUID, TestPlanResponse> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(kafkaCommonConfiguration.consumerConfigs());
    }
}
