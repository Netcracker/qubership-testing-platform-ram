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

import java.util.List;
import java.util.UUID;

import org.qubership.atp.ram.models.ExecutionRequestDetails;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ExecutionRequestDetailsRepository extends MongoRepository<ExecutionRequestDetails, UUID> {
    ExecutionRequestDetails findByExecutionRequestId(UUID executionRequestId);

    List<ExecutionRequestDetails> findAllByExecutionRequestId(UUID executionRequestId);

    void deleteAllByExecutionRequestIdIn(List<UUID> executionRequestIds);
}
