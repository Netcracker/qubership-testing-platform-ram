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

import java.util.UUID;

import org.qubership.atp.ram.dto.event.ExecutionRequestEvent;
import org.qubership.atp.ram.dto.event.WebSocketEventType;
import org.qubership.atp.ram.dto.request.StatusUpdateRequest;
import org.qubership.atp.ram.dto.response.StatusUpdateResponse;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@Slf4j
public class ExecutionRequestNotificationService {

    private final WebSocketNotificationService notificationService;
    private final TestRunService testRunService;

    /**
     * Send execution request status update event.
     *
     * @param executionRequestId execution request id
     * @param request filter request
     */
    public void sendStatusUpdate(UUID executionRequestId, StatusUpdateRequest request) {
        log.info("Send status update event for execution request '{}' with filter: {}", executionRequestId, request);
        StatusUpdateResponse statusUpdateResponse = testRunService.getStatusUpdate(request);
        ExecutionRequestEvent<StatusUpdateResponse> event = new ExecutionRequestEvent<>(
                WebSocketEventType.EXECUTION_REQUEST_STATUS_UPDATE, executionRequestId, statusUpdateResponse);
        log.debug("Event: {}", event);

        notificationService.sendEvent(event);
    }
}
