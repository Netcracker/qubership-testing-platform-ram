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

package org.qubership.atp.ram.service.rest.gracefulshutdown;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.GracefulShutdownHandler;

public class GracefulUndertowShutdown
        implements ApplicationListener<ContextClosedEvent>, HandlerWrapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(GracefulUndertowShutdown.class);
    private final int waitTime = 30000;
    private volatile GracefulShutdownHandler handler;

    @Override
    public HttpHandler wrap(HttpHandler handler) {
        this.handler = new GracefulShutdownHandler(handler);
        return this.handler;
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        try {
            this.handler.shutdown();
            this.handler.awaitShutdown(waitTime);
        } catch (InterruptedException ex) {
            LOGGER.error(ex.getMessage());
        }
    }

}