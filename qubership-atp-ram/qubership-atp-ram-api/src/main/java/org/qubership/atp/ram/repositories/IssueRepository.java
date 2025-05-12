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

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.qubership.atp.ram.models.Issue;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

@Repository
public interface IssueRepository extends MongoRepository<Issue, UUID> {

    List<Issue> findByExecutionRequestId(UUID executionRequestId);

    @Query(fields = "{'_id': 1, 'logRecordIds': 1}")
    List<Issue> findShortByExecutionRequestId(UUID executionRequestId);

    List<Issue> findByExecutionRequestIdAndFailPatternIdIn(UUID executionRequestId, Collection<UUID> failPatternIds);

    List<Issue> findByExecutionRequestIdAndFailedTestRunIdsIn(UUID executionRequestId, Collection<UUID> testRunIds);

    List<Issue> findByFailPatternIdAndExecutionRequestId(UUID failPatternId, UUID executionRequestId);

    List<Issue> findByFailPatternId(UUID failPatternId);

    @Query("{failPatternId: ?0 }")
    @Update("{ '$set' : { priority: null, failPatternId: null, failReasonId:  null} }")
    void updateByRemovedPatternId(UUID failPatternId);

    long countByExecutionRequestId(UUID executionRequestId);

    long countBylogRecordIds(UUID logRecordId);

    void deleteAllByExecutionRequestIdIn(List<UUID> executionRequestIds);
}
