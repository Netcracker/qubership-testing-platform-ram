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

import java.util.List;
import java.util.UUID;

import org.qubership.atp.ram.client.EnvironmentFeignClient;
import org.qubership.atp.ram.clients.api.dto.environments.environment.BaseSearchRequestDto;
import org.qubership.atp.ram.clients.api.dto.environments.environment.SystemFullVer1ViewDto;
import org.qubership.atp.ram.converter.DtoConvertService;
import org.qubership.atp.ram.dto.response.Environment;
import org.qubership.atp.ram.model.BaseSearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class EnvironmentsService {

    private final EnvironmentFeignClient environmentsFeignClient;
    private final DtoConvertService dtoConvertService;
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Search environments.
     *
     * @param searchRequest search request
     * @return list of environments
     */
    public List<Environment> searchEnvironments(BaseSearchRequest searchRequest) {
        List<Environment> environments = dtoConvertService.convertList(
                environmentsFeignClient.findBySearchRequest(
                                objectMapper.convertValue(searchRequest, BaseSearchRequestDto.class),
                                false) // TODO: Check if this hardcoded 2nd parameter is Okay
                        .getBody(), Environment.class);
        return environments;
    }

    /**
     * Get environment by id.
     *
     * @param environmentId id of environmemnt
     * @return environment
     */
    public Environment getEnvironmentById(UUID environmentId) {
        Environment environment = dtoConvertService.convert(
                environmentsFeignClient.getEnvironment(environmentId, true).getBody(), Environment.class);
        return environment;
    }

    /**
     * Get environment's name.
     *
     * @param environmentId id of environmemnt
     * @return name
     */
    public String getEnvironmentNameById(UUID environmentId) {
        if (environmentId != null) {
            return environmentsFeignClient.getEnvironmentNameById(environmentId).getBody();
        } else {
            return null;
        }
    }

    public List<SystemFullVer1ViewDto> getEnvironmentSystems(UUID environmentId) {
        return environmentsFeignClient.getEnvironmentSystems(environmentId, null, false).getBody();
    }
}
