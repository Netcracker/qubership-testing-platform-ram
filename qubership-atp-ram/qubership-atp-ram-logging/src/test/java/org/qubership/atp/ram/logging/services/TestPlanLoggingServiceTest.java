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
import org.qubership.atp.ram.logging.entities.requests.CreatedTestRunWithParentsRequest;
import org.qubership.atp.ram.models.Project;
import org.qubership.atp.ram.models.TestPlan;
import org.qubership.atp.ram.repositories.TestPlansRepository;

public class TestPlanLoggingServiceTest {
    private TestPlanLoggingService service;
    private TestPlansRepository repository;

    @BeforeEach
    public void setUp() {
        repository = mock(TestPlansRepository.class);
        service = spy(new TestPlanLoggingService(repository));
    }

    @Test
    public void findOrCreateTestPlanByUuid_SetExistedTestPlanId_ShouldReturnExistedTestPlan() {
        UUID uuid = UUID.randomUUID();
        String name = "name1";
        Project project = new Project();
        project.setUuid(UUID.randomUUID());

        TestPlan expTestPlan = new TestPlan();
        expTestPlan.setUuid(uuid);
        expTestPlan.setName(name);
        expTestPlan.setProjectId(project.getUuid());

        when(repository.findByUuid(uuid)).thenReturn(expTestPlan);
        TestPlan actualTestPlan = service.findOrCreateTestPlanByUuid(project.getUuid(), uuid, name);

        Assertions.assertEquals(uuid, actualTestPlan.getUuid(), "UUID should be equals");
        Assertions.assertEquals(name, actualTestPlan.getName(), "Names should be equals");
        Assertions.assertEquals(project.getUuid(), actualTestPlan.getProjectId(), "Project UUID should be equals");
    }

    @Test
    public void findByUuidNameOrCreateNew_SetTestPlanIdInRequest_ShouldBeValid() {
        CreatedTestRunWithParentsRequest request = new CreatedTestRunWithParentsRequest();
        request.setTestPlanId(UUID.randomUUID());
        request.setTestPlanName("name");
        service.findByUuidNameOrCreateNew(request, new Project());
        verify(service).findOrCreateTestPlanByUuid(any(), any(), any());
    }

    @Test
    public void findByUuidNameOrCreateNew_NotSetTestPlanIdInRequest_ShouldBeValid() {
        CreatedTestRunWithParentsRequest request = new CreatedTestRunWithParentsRequest();
        request.setTestPlanName("name");
        Project project = new Project();
        project.setUuid(UUID.randomUUID());
        service.findByUuidNameOrCreateNew(request, project);
        verify(service).findOrCreateTestPlanByProjectUuidAndTestPlanName(any(), any());
    }

}
