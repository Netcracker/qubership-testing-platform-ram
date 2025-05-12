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

package org.qubership.atp.ram.services;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.qubership.atp.auth.springbootstarter.exceptions.AtpEntityNotFoundException;
import org.qubership.atp.ram.dto.response.ProjectDataResponse;
import org.qubership.atp.ram.models.Project;
import org.qubership.atp.ram.repositories.ProjectsRepository;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectsService extends CrudService<Project> {

    private final ProjectsRepository repository;
    private final CatalogueService catalogueService;

    @Override
    protected MongoRepository<Project, UUID> repository() {
        return repository;
    }

    /**
     * Returns project by projectUuid.
     */
    public Project getProjectById(UUID projectId) {
        return get(projectId);
    }

    /**
     * Returns project by projectUuid.
     */
    public List<Project> getAllProjects() {
        return repository.findAll();
    }

    /**
     * Returns project by projectUuid.
     */
    public List<Project> getProjectsByIds(Collection<UUID> ids) {
        return repository.findByUuidIn(ids);
    }

    /**
     * Returns project by Name.
     */
    private Project getProjectByName(String projectName) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(projectName),
                "ProjectName is required!");
        return repository.findByName(projectName);
    }

    /**
     * Returns existing project or create new.
     */
    @Deprecated
    public synchronized Project findOrCreateProjectByName(String projectName) {
        Project result = getProjectByName(projectName);
        if (Objects.nonNull(result)) {
            log.debug("Project exists {} by name", result.getUuid());
            return result;
        }
        result = new Project();
        result.setName(projectName);
        repository.save(result);
        log.debug("Project create {} by name", result.getUuid());
        return result;
    }

    /**
     * Return existing project or create new.
     *
     * @param projectUuid for search
     * @param projectName for set name to new project
     * @return existed or new {@link Project}
     */
    @Deprecated
    public synchronized Project findOrCreateProjectByUuid(UUID projectUuid, String projectName) {
        Project result = repository.findByUuid(projectUuid);
        if (Objects.nonNull(result)) {
            log.debug("Project exists {} by ID", result.getUuid());
            return result;
        }
        result = new Project();
        result.setName(projectName);
        result.setUuid(projectUuid);
        repository.save(result);
        log.debug("Project created {} by ID", result.getUuid());
        return result;
    }


    public String getProjectName(UUID projectUuid) {
        return repository.findNameByUuid(projectUuid).getName();
    }

    /**
     * Find existed project by UUID/name or create new.
     *
     * @param project for search project
     * @return existed or created {@link Project}
     */
    @Deprecated
    public Project findByUuidNameOrCreateNew(Project project) {
        if (project.getUuid() != null) {
            return findOrCreateProjectByUuid(project.getUuid(), project.getName());
        } else {
            String projectName = project.getName();

            Project result = getProjectByName(projectName);
            if (Objects.isNull(result)) {
                log.error("Failed to find Project by name: {}", projectName);
                throw new AtpEntityNotFoundException("Project", "name", projectName);
            }

            return result;
        }
    }

    public void deleteByUuid(UUID uuid) {
        repository.deleteByUuid(uuid);
    }

    /**
     * Synchronize project data.
     *
     * @param project project
     */
    public void synchronizeProjectData(Project project) {
        try {
            ProjectDataResponse projectData = catalogueService.getProjectData(project.getUuid());
            project.setTroubleShooterUrl(projectData.getTshooterUrl());
            project.setMissionControlToolUrl(projectData.getMissionControlToolUrl());
            project.setMonitoringToolUrl(projectData.getMonitoringToolUrl());
        } catch (Exception e) {
            log.error("Failed to synchronize project data", e);
        }
    }
}
