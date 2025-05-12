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

package org.qubership.atp.ram.tsg.service;

import java.util.List;
import java.util.UUID;

import org.qubership.atp.ram.models.Project;
import org.qubership.atp.ram.repositories.ProjectsRepository;
import org.springframework.stereotype.Service;

import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class TsgProjectService {

    private final ProjectsRepository projectsRepository;

    public List<Project> getAllTsgProjects() {
        log.debug("Start search for all TSG Projects.");
        return projectsRepository.findAllByTsgProjectNameExists();
    }

    /**
     * Updates tsgParameters on RAM Project.
     */
    public Project updateTsgParameters(UUID projectUuid, JsonObject tsgParameters) {
        Project project = projectsRepository.findByUuid(projectUuid);

        Preconditions.checkNotNull(project, "There is no project with UUID: %s", projectUuid);

        if (tsgParameters.has("enable")) {
            boolean tsgIntegrationEnable = tsgParameters.get("enable").getAsBoolean();
            project.setTsgIntegration(tsgIntegrationEnable);
        }
        if (tsgParameters.has("tsgProjectName")) {
            String tsgProjectName = tsgParameters.get("tsgProjectName").getAsString();
            project.setTsgProjectName(tsgProjectName);
        }

        projectsRepository.save(project);
        log.debug("Project: {} was updated with tsgProjectName: {} Integration enable: {}", project.getName(),
                project.getTsgProjectName(), project.isTsgIntegration());
        return project;
    }
}
