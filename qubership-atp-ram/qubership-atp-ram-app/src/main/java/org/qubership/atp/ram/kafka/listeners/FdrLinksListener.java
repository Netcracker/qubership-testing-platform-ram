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

import static org.qubership.atp.ram.config.KafkaFdrConfiguration.KAFKA_FDR_CONTAINER_FACTORY_NAME;

import org.qubership.atp.integration.configuration.mdc.MdcUtils;
import org.qubership.atp.ram.config.KafkaCommonConfiguration;
import org.qubership.atp.ram.mdc.MdcField;
import org.qubership.atp.ram.services.TestRunService;
import org.qubership.atp.ram.tsg.model.FdrResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        value = "kafka.fdr.enable",
        matchIfMissing = false
)
@Import(KafkaCommonConfiguration.class)
public class FdrLinksListener {

    private static final String KAFKA_LISTENER_ID = "fdr";

    private final TestRunService testRunService;

    @Autowired
    public FdrLinksListener(TestRunService testRunService) {
        this.testRunService = testRunService;
    }

    /**
     * Listen fdr-links kafka topic.
     */
    @KafkaListener(id = KAFKA_LISTENER_ID, topics = "${kafka.fdr.consumer.topic.name}",
            containerFactory = KAFKA_FDR_CONTAINER_FACTORY_NAME)
    public void setFdrLink(@Payload FdrResponse fdrLink) {
        MDC.clear();
        MdcUtils.put(MdcField.TEST_RUN_ID.toString(), fdrLink.getTestRunId());
        testRunService.setFdrLink(fdrLink.getTestRunId(), fdrLink.getFdrLink());
    }
}
