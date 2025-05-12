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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.atp.auth.springbootstarter.exceptions.AtpEntityNotFoundException;
import org.qubership.atp.ram.logging.entities.requests.CreatedTestRunWithParentsRequest;
import org.qubership.atp.ram.models.Project;
import org.qubership.atp.ram.repositories.ProjectsRepository;

public class ProjectLoggingServiceTest {
    private ProjectsRepository projectsRepository;
    private ProjectLoggingService projectLoggingService;

    @BeforeEach
    public void setUp() {
        projectsRepository = mock(ProjectsRepository.class);
        projectLoggingService = spy(new ProjectLoggingService(projectsRepository));
    }

    @Test
    public void findOrCreateProjectByUuid_SetExistedProjectId_ShouldReturnExistedProject() {
        UUID uuid = UUID.randomUUID();
        String name = "name1";
        Project expProject = new Project();
        expProject.setUuid(uuid);
        expProject.setName(name);

        when(projectsRepository.findByUuid(uuid)).thenReturn(expProject);
        Project actualProject = projectLoggingService.findOrCreateProjectByUuid(uuid, name);

        Assertions.assertEquals(uuid, actualProject.getUuid(),"UUID should be equals");
        Assertions.assertEquals(name, actualProject.getName(), "Names should be equals");
    }

    @Test
    public void findByUuidNameOrCreateNew_SetProjectIdInRequest_ShouldBeValid() {
        CreatedTestRunWithParentsRequest request = new CreatedTestRunWithParentsRequest();
        request.setProjectId(UUID.randomUUID());
        request.setProjectName("name");
        projectLoggingService.findByUuidNameOrCreateNew(request);
        verify(projectLoggingService).findOrCreateProjectByUuid(any(), any());
    }

    @Test
    public void findByUuidNameOrCreateNew_NotSetProjectIdInRequest_ShouldBeValid() {
        CreatedTestRunWithParentsRequest request = new CreatedTestRunWithParentsRequest();
        request.setProjectName("name");
        Project mockProject = new Project();
        mockProject.setName("name");

        when(projectsRepository.findByName("name")).thenReturn(mockProject);

        projectLoggingService.findByUuidNameOrCreateNew(request);
        verify(projectLoggingService).findProjectByName(any());
    }

    @Test
    public void findByUuidNameOrCreateNew_NotSetProjectIdInRequest_ReturnException() {
        CreatedTestRunWithParentsRequest request = new CreatedTestRunWithParentsRequest();
        String projectName = "name";
        request.setProjectName(projectName);

        when(projectsRepository.findByName(projectName)).thenReturn(null);

        String errorMessage = String.format(AtpEntityNotFoundException.DEFAULT_REF_ID_MESSAGE, "Project", "name", projectName);
        try {
            projectLoggingService.findByUuidNameOrCreateNew(request);
        } catch (Exception e) {
            Assertions.assertEquals(errorMessage, e.getMessage());
        }
    }
}
