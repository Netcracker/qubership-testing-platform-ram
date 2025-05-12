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

import org.qubership.atp.ram.dto.event.WebSocketEvent;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketNotificationService {

    private final SimpMessageSendingOperations messagingTemplate;

    /**
     * Send event via web socket.
     * Destination should be provided by {@link WebSocketEvent#getDestination()}.
     *
     * @param event event data
     * @param <T> event type
     */
    public <T extends WebSocketEvent<R>, R> void sendEvent(T event) {
        log.debug("Send event via WS with body: {}", event);
        final String destination = event.getEventType().getDestinationPrefix() + event.getDestination();
        messagingTemplate.convertAndSend(destination, event);
        log.debug("Notification event '{}' has been sent", event);
    }
}
