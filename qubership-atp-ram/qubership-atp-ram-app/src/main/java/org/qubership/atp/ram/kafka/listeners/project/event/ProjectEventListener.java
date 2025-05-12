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

package org.qubership.atp.ram.kafka.listeners.project.event;

import static org.qubership.atp.ram.config.KafkaProjectEventConfiguration.KAFKA_PROJECT_EVENT_CONTAINER_FACTORY_NAME;

import java.io.IOException;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        value = "kafka.project.event.enable",
        matchIfMissing = false
)
public class ProjectEventListener {

    private static final String KAFKA_LISTENER_ID = "ramProjectEvent";

    private final ProjectEventResolver projectEventResolver;

    @Autowired
    public ProjectEventListener(ProjectEventResolver projectEventResolver) {
        this.projectEventResolver = projectEventResolver;
    }

    /**
     * Listen project-event kafka topic.
     */
    @KafkaListener(id = KAFKA_LISTENER_ID, topics = "${kafka.project.event.consumer.topic.name}",
            containerFactory = KAFKA_PROJECT_EVENT_CONTAINER_FACTORY_NAME)
    public void listen(@Payload String event) throws IOException {
        MDC.clear();
        projectEventResolver.resolve(event);
    }
}
