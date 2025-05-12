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

package org.qubership.atp.ram.kafka.listeners.test.plan;

import static org.qubership.atp.ram.config.KafkaTestPlanConfiguration.KAFKA_TEST_PLAN_CONTEINER_FACTORY_NAME;

import org.qubership.atp.integration.configuration.mdc.MdcField;
import org.qubership.atp.integration.configuration.mdc.MdcUtils;
import org.qubership.atp.ram.models.TestPlan;
import org.qubership.atp.ram.services.TestPlansService;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Component
@ConditionalOnProperty(
        value = "kafka.test.plan.event.enable"
)
@Slf4j
public class TestPlanEventListener {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String KAFKA_LISTENER_ID = "ramTestPlan";
    private final TestPlansService testPlansService;

    @Autowired
    public TestPlanEventListener(TestPlansService testPlansService) {
        this.testPlansService = testPlansService;
    }

    /**
     * Kafka listener.
     */
    @KafkaListener(
            id = KAFKA_LISTENER_ID,
            topics = "${kafka.test.plan.consumer.topic.name}",
            containerFactory = KAFKA_TEST_PLAN_CONTEINER_FACTORY_NAME)
    public void listener(@Payload String eventTestPlan) throws JsonProcessingException {
        MDC.clear();
        TestPlanResponse testPlanResponse = objectMapper.readValue(eventTestPlan, TestPlanResponse.class);
        TestPlan repoTestPlan = testPlansService.findByTestPlanUuid(testPlanResponse.getUuid());
        MdcUtils.put(MdcField.PROJECT_ID.toString(), repoTestPlan.getProjectId());
        log.info("reception data test plan from kafka topic : {} and save test plan with change name : {}",
                eventTestPlan, repoTestPlan);
        repoTestPlan.setName(testPlanResponse.getName());
        testPlansService.save(repoTestPlan);
    }
}
