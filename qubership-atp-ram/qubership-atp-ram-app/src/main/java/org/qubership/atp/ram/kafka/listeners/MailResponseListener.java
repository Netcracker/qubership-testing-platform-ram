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

package org.qubership.atp.ram.kafka.listeners;

import static org.qubership.atp.ram.config.KafkaMailsResponsesConfiguration.KAFKA_MAILS_RESPONSES_CONTAINER_FACTORY_NAME;

import java.io.IOException;

import org.qubership.atp.ram.config.KafkaCommonConfiguration;
import org.qubership.atp.ram.services.ExecutionRequestDetailsService;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(
        value = "kafka.mails.responses.enable",
        matchIfMissing = false
)
@Import(KafkaCommonConfiguration.class)
public class MailResponseListener {

    private static final String KAFKA_LISTENER_ID = "mail";

    private final ExecutionRequestDetailsService service;

    @Autowired
    public MailResponseListener(ExecutionRequestDetailsService service) {
        this.service = service;
    }

    /**
     * Listen mail responses kafka topic.
     */
    @KafkaListener(id = KAFKA_LISTENER_ID, topics = "${kafka.mails.responses.topic.name}",
            containerFactory = KAFKA_MAILS_RESPONSES_CONTAINER_FACTORY_NAME,
            groupId = "${kafka.mails.responses.group.id}")
    public void setMailResponseDetails(@Payload String response) throws IOException {
        MDC.clear();
        service.addMailResponseDetails(response);
    }
}
