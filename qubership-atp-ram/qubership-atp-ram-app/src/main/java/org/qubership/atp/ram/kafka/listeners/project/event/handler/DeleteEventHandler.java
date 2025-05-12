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

import static org.qubership.atp.ram.constants.CacheConstants.PROJECT_CACHE;

import org.qubership.atp.ram.kafka.listeners.project.event.EventType;
import org.qubership.atp.ram.kafka.listeners.project.event.ProjectEvent;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;

@Component
public class DeleteEventHandler extends ProjectEventHandler {

    @Override
    @CacheEvict(value = PROJECT_CACHE, key = "#projectEvent.getProjectId()")
    public void handleEvent(ProjectEvent projectEvent) {
        projectService.deleteByUuid(projectEvent.getProjectId());
    }

    @Override
    public EventType getType() {
        return EventType.DELETE;
    }
}
