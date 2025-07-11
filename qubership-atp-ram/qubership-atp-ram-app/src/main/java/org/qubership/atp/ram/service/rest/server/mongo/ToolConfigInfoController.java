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

import org.qubership.atp.ram.config.ApiPath;
import org.qubership.atp.ram.models.ToolConfigInfo;
import org.qubership.atp.ram.services.ToolConfigInfoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RequestMapping(ApiPath.API_PATH + ApiPath.CONFIG_INFO_PATH)
@RestController
@RequiredArgsConstructor
public class ToolConfigInfoController /*implements ToolConfigInfoControllerApi*/ {
    private final ToolConfigInfoService service;

    @GetMapping(value = "/{uuid}")
    public ToolConfigInfo getByUuid(@PathVariable("uuid") UUID uuid) {
        return service.getByUuid(uuid);
    }
}
