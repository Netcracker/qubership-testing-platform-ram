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

package org.qubership.atp.ram.kafka.listeners.project.event.handler;

import org.qubership.atp.ram.kafka.listeners.project.event.EventType;
import org.qubership.atp.ram.kafka.listeners.project.event.ProjectEvent;
import org.qubership.atp.ram.models.Project;
import org.springframework.stereotype.Component;

@Component
public class CreateEventHandler extends ProjectEventHandler {

    @Override
    public void handleEvent(ProjectEvent projectEvent) {
        Project project = new Project();
        project.setUuid(projectEvent.getProjectId());
        project.setName(projectEvent.getProjectName());
        project.setDateFormat(projectEvent.getDateFormat());
        project.setTimeFormat(projectEvent.getTimeFormat());
        project.setTimeZone(projectEvent.getTimeZone());
        project.setExecutionRequestsExpirationPeriodWeeks(projectEvent.getExecutionRequestsExpirationPeriodWeeks());
        projectService.synchronizeProjectData(project);
        projectService.save(project);
    }

    @Override
    public EventType getType() {
        return EventType.CREATE;
    }
}
