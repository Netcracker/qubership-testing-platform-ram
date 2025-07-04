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
import org.qubership.atp.ram.services.ProjectsService;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class ProjectEventHandler {

    @Autowired
    protected ProjectsService projectService;

    /**
     * Handles project event.
     */
    public abstract void handleEvent(ProjectEvent projectEvent);

    /**
     * Gets type of project event.
     */
    public abstract EventType getType();
}
