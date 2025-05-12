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

package org.qubership.atp.ram.logging.services;

import java.util.Objects;
import java.util.UUID;

import org.qubership.atp.auth.springbootstarter.exceptions.AtpEntityNotFoundException;
import org.qubership.atp.ram.logging.entities.requests.CreatedTestRunWithParentsRequest;
import org.qubership.atp.ram.models.Project;
import org.qubership.atp.ram.repositories.ProjectsRepository;
import org.springframework.stereotype.Service;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectLoggingService {
    private final ProjectsRepository projectsRepository;

    /**
     * Find existed project by UUID/name or create new.
     *
     * @param request for search project
     * @return existed or created {@link Project}
     */
    public Project findByUuidNameOrCreateNew(CreatedTestRunWithParentsRequest request) {
        if (Objects.nonNull(request.getProjectId())) {
            return findOrCreateProjectByUuid(request.getProjectId(),
                    request.getProjectName());
        } else {
            return findProjectByName(request.getProjectName());
        }
    }

    /**
     * Return existing project or create new.
     *
     * @param projectId   for search
     * @param projectName for set name to new project
     * @return existed or new {@link Project}
     */
    synchronized Project findOrCreateProjectByUuid(UUID projectId, String projectName) {
        Project result = projectsRepository.findByUuid(projectId);
        if (Objects.nonNull(result)) {
            log.debug("Project by ID is exist {}", result.getUuid());
            return result;
        }
        result = new Project();
        result.setName(projectName);
        result.setUuid(projectId);
        projectsRepository.save(result);
        log.debug("Project will be creating {}", projectId);
        return result;
    }

    /**
     * Returns existing project or create new.
     */
    synchronized Project findProjectByName(String projectName) {
        Project result = getProjectByName(projectName);
        if (Objects.isNull(result)) {
            log.error("Failed to find Project by name: {}", projectName);
            throw new AtpEntityNotFoundException("Project", "name", projectName);
        }

        return result;
    }

    private Project getProjectByName(String projectName) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(projectName), "ProjectName is required!");
        return projectsRepository.findByName(projectName);
    }
}
