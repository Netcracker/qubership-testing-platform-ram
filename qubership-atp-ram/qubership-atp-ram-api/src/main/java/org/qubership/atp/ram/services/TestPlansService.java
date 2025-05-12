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

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.qubership.atp.ram.dto.request.TestPlansSearchRequest;
import org.qubership.atp.ram.entities.MailRecipients;
import org.qubership.atp.ram.models.Project;
import org.qubership.atp.ram.models.TestPlan;
import org.qubership.atp.ram.repositories.TestPlansRepository;
import org.qubership.atp.ram.repositories.impl.CustomTestPlanRepository;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class TestPlansService extends CrudService<TestPlan> {

    private final TestPlansRepository repository;
    private final CustomTestPlanRepository customRepository;

    @Override
    protected MongoRepository<TestPlan, UUID> repository() {
        return repository;
    }

    /**
     * Returns list of TestPlans by Project.
     */
    public List<TestPlan> getTestPlansByProjectUuid(UUID projectUuid) {
        return repository.findAllByProjectId(projectUuid);
    }

    /**
     * Save TestPlan.
     */
    @Override
    public TestPlan save(TestPlan testPlan) {
        Preconditions.checkArgument(Objects.nonNull(testPlan.getProjectId()),
                "ProjectUuid is required");
        Preconditions.checkArgument(Objects.nonNull(testPlan.getUuid()),
                "TestPlanUuid is required");
        return repository.save(testPlan);
    }

    /**
     * Create new test plan.
     * Project ID cannot be null
     *
     * @param testPlan for creating
     * @return creating new test plan
     */
    @Deprecated
    public TestPlan create(TestPlan testPlan) {
        Preconditions.checkNotNull(testPlan.getProjectId(),
                "Project ID is required");
        TestPlan existTestPlan = findTestPlanByProjectUuidAndTestPlanName(testPlan.getProjectId(), testPlan.getName());
        if (Objects.nonNull(existTestPlan)) {
            log.warn("Test plan with name {} already exist for project {}",
                    testPlan.getName(), testPlan.getProjectId());
            return repository.save(existTestPlan);
        } else {
            return repository.save(testPlan);
        }
    }

    /**
     * Returns testPlan by ProjectUuid and TestPlan's name.
     */
    @Deprecated
    public TestPlan findTestPlanByProjectUuidAndTestPlanName(UUID projectUuid, String testPlanName) {
        Preconditions.checkArgument(Objects.nonNull(projectUuid),
                "ProjectUuid is required!");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(testPlanName),
                "TestPlanName is required!");
        return repository.findByProjectIdAndName(projectUuid, testPlanName);
    }

    /**
     * Returns existing TestPlan for provided Project or create new.
     */
    @Deprecated
    public synchronized TestPlan findOrCreateTestPlanByProjectUuidAndTestPlanName(
            UUID projectUuid, String testPlanName) {
        TestPlan result = findTestPlanByProjectUuidAndTestPlanName(projectUuid, testPlanName);
        if (Objects.nonNull(result)) {
            log.debug("TP exists {} by name + project ID", result.getUuid());
            return result;
        }
        result = new TestPlan();
        result.setProjectId(projectUuid);
        result.setName(testPlanName);
        repository.save(result);
        log.debug("TP create {} by name + project ID", result.getUuid());
        return result;
    }

    /**
     * Return existed test plan or create new.
     *
     * @param projectId    for set project UUID
     * @param testPlanId   for search
     * @param testPlanName for set test plan name
     * @return existed or new {@link TestPlan}
     */
    @Deprecated
    public synchronized TestPlan findOrCreateTestPlanByUuid(UUID projectId, UUID testPlanId,
                                                            String testPlanName) {
        TestPlan result = repository.findByUuid(testPlanId);
        if (Objects.nonNull(result)) {
            log.debug("TP exists {} by ID", result.getUuid());
            return result;
        }
        result = new TestPlan();
        result.setProjectId(projectId);
        result.setName(testPlanName);
        result.setUuid(testPlanId);
        repository.save(result);
        log.debug("TP created {} by ID", result.getUuid());
        return result;
    }

    /**
     * Returns TestPlan by uuid.
     */
    public TestPlan findByTestPlanUuid(UUID testPlanUuid) {
        Preconditions.checkArgument(Objects.nonNull(testPlanUuid),
                "TestPlanUuid is required!");
        return repository.findByUuid(testPlanUuid);
    }

    public MailRecipients getRecipients(UUID testPlanUuid) {
        return repository.findByUuid(testPlanUuid).getRecipients();
    }

    /**
     * Save recipients for TestPlane.
     *
     * @param testPlanUuid {@link UUID} of {@link TestPlan}
     * @param recipients   {@link MailRecipients} which has list of email addresses.
     */
    public void saveRecipients(UUID testPlanUuid, MailRecipients recipients) {
        TestPlan testPlan = findByTestPlanUuid(testPlanUuid);
        testPlan.setRecipients(recipients);
        save(testPlan);
    }

    public String getTestPlanName(UUID testPlanUuid) {
        return repository.findNameAndUuidAndProjectUuidByUuid(testPlanUuid).getName();
    }

    public TestPlan getTestPlanForNavigationPath(UUID testPlanUuid) {
        return repository.findNameAndUuidAndProjectUuidByUuid(testPlanUuid);
    }

    public UUID getProjectIdByTestPlanId(UUID id) {
        return repository.findNameAndUuidAndProjectUuidByUuid(id).getProjectId();
    }

    /**
     * Find existed test plan by UUID/name or create new.
     *
     * @param testPlan request for search test plan
     * @param project  for search test plan
     * @return existed or created {@link TestPlan}
     */
    @Deprecated
    public TestPlan findByUuidNameOrCreateNew(TestPlan testPlan, Project project) {
        if (testPlan.getUuid() != null) {
            return findOrCreateTestPlanByUuid(project.getUuid(), testPlan.getUuid(), testPlan.getName());
        } else {
            return findOrCreateTestPlanByProjectUuidAndTestPlanName(
                    project.getUuid(), testPlan.getName());
        }
    }

    /**
     * Search test plans.
     */
    public List<TestPlan> search(TestPlansSearchRequest request) {
        return customRepository.search(request);
    }
}
