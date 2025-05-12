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

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

import org.qubership.atp.integration.configuration.mdc.MdcField;
import org.qubership.atp.integration.configuration.mdc.MdcUtils;
import org.qubership.atp.ram.kafka.listeners.project.event.handler.ProjectEventHandler;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class ProjectEventResolver {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private HashMap<EventType, ProjectEventHandler> map;

    /**
     * Constructor.
     *
     * @param projectEventHandlers projectEventHandlers
     */
    @Autowired
    public ProjectEventResolver(Set<ProjectEventHandler> projectEventHandlers) {
        this.map = new HashMap<>();
        projectEventHandlers.forEach(x -> map.put(x.getType(), x));
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    /**
     * Reads project event from kafka event and resolve it.
     */
    public void resolve(String event) throws IOException {
        ProjectEvent projectEvent = objectMapper.readValue(event, ProjectEvent.class);
        MDC.clear();
        MdcUtils.put(MdcField.PROJECT_ID.toString(), projectEvent.getProjectId());
        map.get(projectEvent.getType()).handleEvent(projectEvent);
    }
}
