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

package org.qubership.atp.ram.service.rest.server.executor;

import static org.qubership.atp.ram.config.ApiPath.API_PATH;
import static org.qubership.atp.ram.config.ApiPath.CONFIG_INFO_PATH;
import static org.qubership.atp.ram.config.ApiPath.EXECUTOR_PATH;

import java.util.Set;
import java.util.UUID;

import org.qubership.atp.ram.services.ToolConfigInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.google.gson.JsonObject;

@RequestMapping(API_PATH + EXECUTOR_PATH + CONFIG_INFO_PATH)
@Deprecated
public class ToolConfigInfoController2 {
    private static final Logger log = LoggerFactory.getLogger(ToolConfigInfoController2.class);
    private final ToolConfigInfoService service;

    @Autowired
    public ToolConfigInfoController2(ToolConfigInfoService service) {
        this.service = service;
    }

    /**
     * Save configuration files.
     *
     * @param request Request with configFiles.
     * @return Set configs id's.
     */
    @PostMapping("/saveConfigInfo")
    public Set<UUID> saveConfigs(JsonObject request) {
        return service.saveConfigs(request);
    }
}
