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

package org.qubership.atp.ram;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.models.ExecutionRequest;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ExecutionRequestsMock {
    public List<ExecutionRequest> generateRequestsList() {
        List<ExecutionRequest> requestsList = new ArrayList<>();
        UUID uuidScope = UUID.randomUUID();
        UUID uuidEnvironment = UUID.randomUUID();

        ExecutionRequest request = generateRequest();
        request.setTestScopeId(uuidScope);
        request.setEnvironmentId(uuidEnvironment);
        requestsList.add(request);

        ExecutionRequest request1 = generateRequest();
        request1.setTestScopeId(uuidScope);
        request1.setEnvironmentId(uuidEnvironment);
        requestsList.add(request1);


        return requestsList;
    }

    public ExecutionRequest generateRequest() {
        ExecutionRequest executionRequest = new ExecutionRequest();
        executionRequest.setName("ER");
        executionRequest.setUuid(UUID.randomUUID());
        executionRequest.setStartDate(new Timestamp(System.currentTimeMillis()));
        executionRequest.setFinishDate(new Timestamp(System.currentTimeMillis()));
        executionRequest.setDuration(10);
        executionRequest.setPassedRate(50);
        executionRequest.setWarningRate(17);
        executionRequest.setFailedRate(33);
        executionRequest.setEnvironmentId(UUID.randomUUID());
        executionRequest.setThreads(5);
        executionRequest.setInitialExecutionRequestId(UUID.fromString("b90dfeda-1fac-4e10-bef9-d852a23d2003"));
        return executionRequest;
    }

    public ExecutionRequest generateRequestById(UUID requestUuid) {
        ExecutionRequest executionRequest = new ExecutionRequest();
        executionRequest.setName("ER");
        executionRequest.setUuid(requestUuid);
        executionRequest.setStartDate(new Timestamp(System.currentTimeMillis()));
        executionRequest.setFinishDate(new Timestamp(System.currentTimeMillis()));
        executionRequest.setDuration(10);
        executionRequest.setPassedRate(50);
        executionRequest.setWarningRate(17);
        executionRequest.setFailedRate(33);
        executionRequest.setEnvironmentId(UUID.randomUUID());
        executionRequest.setThreads(5);
        executionRequest.setProjectId(UUID.randomUUID());
        return executionRequest;
    }

    public List<ExecutionRequest> generateFinishedRequestsList() {
        List<ExecutionRequest> requestsList = new ArrayList<>();
        UUID uuidScope = UUID.randomUUID();
        UUID uuidEnvironment = UUID.randomUUID();

        ExecutionRequest request = generateRequest();
        request.setTestScopeId(uuidScope);
        request.setEnvironmentId(uuidEnvironment);
        request.setExecutionStatus(ExecutionStatuses.FINISHED);
        requestsList.add(request);

        ExecutionRequest request1 = generateRequest();
        request1.setTestScopeId(uuidScope);
        request1.setEnvironmentId(uuidEnvironment);
        request1.setExecutionStatus(ExecutionStatuses.TERMINATED_BY_TIMEOUT);
        requestsList.add(request1);

        ExecutionRequest request2 = generateRequest();
        request2.setTestScopeId(uuidScope);
        request2.setEnvironmentId(uuidEnvironment);
        request2.setExecutionStatus(ExecutionStatuses.TERMINATED);
        requestsList.add(request2);


        return requestsList;
    }

    public ExecutionRequest generateExecutionRequestWithPrevId(UUID currentId, UUID previousId) {
        ExecutionRequest executionRequest = new ExecutionRequest();
        executionRequest.setStartDate(new Timestamp(System.currentTimeMillis()));
        executionRequest.setName("test er");
        executionRequest.setUuid(currentId);
        executionRequest.setPreviousExecutionRequestId(previousId);
        return executionRequest;
    }

    public List<ExecutionRequest> generateRequestsList(int count) {
        List<ExecutionRequest> executionRequests = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            executionRequests.add(generateRequest());
        }
        return executionRequests;
    }
}
