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

package org.qubership.atp.ram.repositories;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ExecutionRequestRepository extends MongoRepository<ExecutionRequest, UUID> {

    @Query(fields = "{'uuid': 1}")
    List<ExecutionRequest> findUuidByProjectId(UUID projectId);

    ExecutionRequest findByUuid(UUID uuid);

    List<ExecutionRequest> findAllByInitialExecutionRequestId(UUID uuid);

    void deleteByUuid(UUID uuid);

    ExecutionRequest findByNameAndTestPlanId(String name, UUID testPlanId);

    List<ExecutionRequest> findAllByTestPlanId(UUID testPlanId);

    List<ExecutionRequest> findAllByTestPlanId(UUID testPlanId, Pageable pageable);

    List<ExecutionRequest> findAllByProjectIdAndExecutionStatusNotIn(UUID projectId,
                                                                     List<ExecutionStatuses> statuses);

    List<ExecutionRequest> findLimitRequestsByProjectIdAndExecutionStatusNotIn(UUID projectId,
                                                                               List<ExecutionStatuses> statuses,
                                                                               Pageable page);

    List<ExecutionRequest> findAllByProjectIdAndExecutionStatusNotInAndFinishDateAfter(UUID projectId,
                                                                                       List<ExecutionStatuses>
                                                                                               statuses,
                                                                                       Timestamp timestamp,
                                                                                       Pageable page);

    @Query(value = "{'startDate': { $lte:?0 }, 'projectId': ?1}", fields = "{'_id': 1}")
    List<ExecutionRequest> findAllByArrivedBetweenAndProjectId(Timestamp timestamp, UUID projectId);

    void deleteAllByUuidIn(List<UUID> executionRequestIds);

    List<ExecutionRequest> findAllByTestPlanIdAndStartDateBetween(UUID testPlanId,
                                                                  Timestamp start, Timestamp end,
                                                                  Pageable pageable);

    List<ExecutionRequest> findAllByTestPlanIdAndFinishDateBetween(UUID testPlanId,
                                                                   Timestamp start, Timestamp end,
                                                                   Pageable pageable);

    List<ExecutionRequest> findAllByUuidIn(Collection<UUID> uuids);

    List<ExecutionRequest> findAllByTestPlanIdAndFinishDateBetweenAndAnalyzedByQa(UUID testPlanId,
                                                                                  Timestamp start, Timestamp end,
                                                                                  boolean analyzedByQa,
                                                                                  Pageable page);

    List<ExecutionRequest> findAllByTestPlanIdAndAnalyzedByQaEquals(UUID testPlanId, boolean analyzedByQa,
                                                                    Pageable pageable);

    @Query(fields = "{'uuid': 1}")
    List<ExecutionRequest> findRequestsIdByExecutionStatusIn(List<ExecutionStatuses> executionStatuses);

    List<ExecutionRequest> findAllByTestScopeId(UUID scopeId);

    @Query(fields = "{'name': 1, 'startDate': 1, 'previousExecutionRequestId': 1}")
    ExecutionRequest findNameStartDatePreviousErIdByUuid(UUID executionRequestId);

    @Query(fields = "{'labelTemplateId': 1}")
    ExecutionRequest findLabelTemplateIdByUuid(UUID executionRequestId);

    @Query(fields = "{'projectId': 1}")
    ExecutionRequest findProjectIdByUuid(UUID executionRequestId);

    @Query(fields = "{'projectId': 1}")
    List<ExecutionRequest> findProjectIdByUuidIn(Set<UUID> executionRequestId);

    List<ExecutionRequest> findAllByJointExecutionKey(String jointExecutionKey);

    List<ExecutionRequest> findAllByUuidInOrderByStartDate(Collection<UUID> ids);
}
