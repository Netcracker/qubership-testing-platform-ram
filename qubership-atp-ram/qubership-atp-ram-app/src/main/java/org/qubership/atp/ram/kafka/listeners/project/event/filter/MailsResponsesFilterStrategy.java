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

package org.qubership.atp.ram.kafka.listeners.project.event.filter;

import static org.qubership.atp.ram.RamConstants.OBJECT_MAPPER;
import static org.qubership.atp.ram.enums.MailMetadata.ATP_RAM;

import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.qubership.atp.integration.configuration.model.KafkaMailResponse;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MailsResponsesFilterStrategy implements RecordFilterStrategy<UUID, String> {

    @Override
    public boolean filter(ConsumerRecord<UUID, String> consumerRecord) {
        KafkaMailResponse mailResponse = getMailResponseEntity(consumerRecord);
        if (mailResponse == null) {
            log.debug("filter: Null was captured converting object to the MailResponse type");
            return true;
        }

        log.debug("filter: Check service name for log message {}", mailResponse);
        if (ATP_RAM.getValue().equals(mailResponse.getService())) {
            log.debug("filter: Service name AR: {} is the same as expected ER: {}",
                    ATP_RAM.getValue(), mailResponse.getService());
            return false;
        } else {
            throw new RuntimeException(String.format(
                    "filter: Service name AR: {%s} is differ from the expected one ER: {%s}",
                    ATP_RAM.getValue(), mailResponse.getService()));
        }
    }

    private KafkaMailResponse getMailResponseEntity(ConsumerRecord<UUID, String> consumerRecord) {
        try {
            return OBJECT_MAPPER.readValue(consumerRecord.value(), KafkaMailResponse.class);
        } catch (JsonProcessingException e) {
            log.error("filter: Cannot parse consumerRecord to MailResponse when consume value: {} because of an "
                    + "exception", consumerRecord.value(), e);
            return null;
        }
    }
}
