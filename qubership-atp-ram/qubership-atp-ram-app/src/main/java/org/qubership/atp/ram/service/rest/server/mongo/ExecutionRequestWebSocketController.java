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

package org.qubership.atp.ram.service.rest.server.mongo;

import java.util.UUID;

import org.qubership.atp.ram.dto.request.StatusUpdateRequest;
import org.qubership.atp.ram.services.ExecutionRequestNotificationService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@MessageMapping("/executionRequests")
@Slf4j
@RequiredArgsConstructor
public class ExecutionRequestWebSocketController {

    private final ExecutionRequestNotificationService notificationService;

    /**
     * Send execution request status update event.
     *
     * @param executionRequestId execution request id
     * @param request filter request
     */
    @MessageMapping("/{executionRequestId}")
    @PreAuthorize("@entityAccess.checkAccess(@executionRequestService.getProjectIdByExecutionRequestId("
            + "#executionRequestId),'EXECUTE')")
    public void sendStatusUpdate(@DestinationVariable UUID executionRequestId,
                                 @Payload StatusUpdateRequest request) {
        log.info("Request to send status update info for execution request '{}' with filter request: {}",
                executionRequestId, request);
        notificationService.sendStatusUpdate(executionRequestId, request);
    }
}
