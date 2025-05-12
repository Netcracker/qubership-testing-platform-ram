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

package org.qubership.atp.ram.tsg.senders.fdr;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.qubership.atp.ram.services.ExecutionRequestService;
import org.qubership.atp.ram.services.ProjectsService;
import org.qubership.atp.ram.tsg.model.TsgFdr;
import org.qubership.atp.ram.tsg.senders.Sender;
import org.qubership.atp.ram.tsg.service.TsgService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component("fdrSender")
@ConditionalOnProperty(
        value = "kafka.fdr.enable",
        matchIfMissing = false
)
public class KafkaSender implements Sender<List<UUID>> {

    private static final String PROJECT_HEADER_KEY = "project";

    private final TsgService tsgService;
    private final ProjectsService projectsService;
    private final ExecutionRequestService executionRequestService;
    private final KafkaTemplate<String, TsgFdr> fdrKafkaTemplate;

    @Value("${kafka.fdr.producer.topic.name}")
    private String fdrTopicName;

    /**
     * Constructor.
     */
    @Autowired
    public KafkaSender(KafkaTemplate<String, TsgFdr> fdrKafkaTemplate,
                       TsgService tsgService, ProjectsService projectsService,
                       ExecutionRequestService executionRequestService) {
        this.fdrKafkaTemplate = fdrKafkaTemplate;
        this.tsgService = tsgService;
        this.projectsService = projectsService;
        this.executionRequestService = executionRequestService;
    }

    @Override
    public void send(List<UUID> testRunIds) {
        testRunIds.forEach(testRunId -> {
            TsgFdr tsgFdr = tsgService.buildFdr(testRunId);
            if (Objects.nonNull(tsgFdr)) {

                UUID projectId = executionRequestService
                        .findById(tsgFdr.getExecutionRequestId())
                        .getProjectId();

                String tsgProject = projectsService
                        .getProjectById(projectId)
                        .getTsgProjectName();

                Message<TsgFdr> message = MessageBuilder
                        .withPayload(tsgFdr)
                        .setHeader(KafkaHeaders.TOPIC, fdrTopicName)
                        .setHeader(PROJECT_HEADER_KEY, tsgProject)
                        .build();

                fdrKafkaTemplate.send(message);
                tsgService.setFdrWasSent(testRunId);
            }
        });
    }
}
