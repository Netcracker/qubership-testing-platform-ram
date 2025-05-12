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

import org.qubership.atp.ram.logging.entities.requests.CreatedTestRunWithParentsRequest;
import org.qubership.atp.ram.models.Project;
import org.qubership.atp.ram.models.TestPlan;
import org.qubership.atp.ram.repositories.TestPlansRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class TestPlanLoggingService {
    private final TestPlansRepository repository;

    /**
     * Find existed test plan by UUID/name or create new.
     *
     * @param request request for search test plan
     * @param project for search test plan
     * @return existed or created {@link TestPlan}
     */
    public TestPlan findByUuidNameOrCreateNew(CreatedTestRunWithParentsRequest request, Project project) {
        return findByUuidNameOrCreateNew(project.getUuid(), request.getTestPlanId(), request.getTestPlanName());
    }

    /**
     * Find existed test plan by UUID/name or create new.
     *
     * @param projectId for search test plan
     * @param testPlanId for search test plan
     * @param testPlanName for search test plan
     * @return existed or created {@link TestPlan}
     */
    public TestPlan findByUuidNameOrCreateNew(UUID projectId, UUID testPlanId, String testPlanName) {
        if (Objects.nonNull(testPlanId)) {
            return findOrCreateTestPlanByUuid(projectId, testPlanId, testPlanName);
        } else {
            return findOrCreateTestPlanByProjectUuidAndTestPlanName(projectId, testPlanName);
        }
    }

    /**
     * Return existed test plan or create new.
     *
     * @param projectId    for set project UUID
     * @param testPlanId   for search
     * @param testPlanName for set test plan name
     * @return existed or new {@link TestPlan}
     */
    synchronized TestPlan findOrCreateTestPlanByUuid(UUID projectId, UUID testPlanId,
                                                     String testPlanName) {
        TestPlan result = repository.findByUuid(testPlanId);
        if (Objects.nonNull(result)) {
            log.debug("Test plan by ID is exist {}", result.getUuid());
            return result;
        }
        result = new TestPlan();
        result.setProjectId(projectId);
        result.setName(testPlanName);
        result.setUuid(testPlanId);
        log.debug("Test plan will be creating for project {} with name {}", projectId, testPlanName);
        repository.save(result);
        return result;
    }

    /**
     * Returns existing TestPlan for provided Project or create new.
     */
    synchronized TestPlan findOrCreateTestPlanByProjectUuidAndTestPlanName(
            UUID projectId, String testPlanName) {
        TestPlan result = repository.findByProjectIdAndName(projectId, testPlanName);
        if (Objects.nonNull(result)) {
            log.debug("Test plan by projectId && name is exist {}", result.getUuid());
            return result;
        }
        result = new TestPlan();
        result.setProjectId(projectId);
        result.setName(testPlanName);
        log.debug("Test plan will be creating for project {} with name {}", projectId, testPlanName);
        repository.save(result);
        return result;
    }
}
