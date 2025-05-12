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

package org.qubership.atp.ram.converters;

import java.util.List;

import org.qubership.atp.ram.controllers.api.dto.execution.ExecutionRequestDto;
import org.qubership.atp.ram.controllers.api.dto.jira.TestRunForRefreshFromJiraDto;
import org.qubership.atp.ram.controllers.api.dto.jira.TestRunToJiraInfoDto;
import org.qubership.atp.ram.controllers.api.dto.testrun.JiraTicketUpdateRequestDto;
import org.qubership.atp.ram.converter.DtoConvertService;
import org.qubership.atp.ram.dto.request.JiraTicketUpdateRequest;
import org.qubership.atp.ram.model.TestRunForRefreshFromJira;
import org.qubership.atp.ram.model.TestRunToJiraInfo;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@Service
public class ModelConverter {

    private final DtoConvertService dtoConverter;

    public List<TestRunToJiraInfoDto> convertJiraInfoModelToDto(List<TestRunToJiraInfo> result) {
        return dtoConverter.convertList(result, TestRunToJiraInfoDto.class);
    }

    public List<TestRunForRefreshFromJiraDto> convertRefreshFromJiraModelToDto(
            List<TestRunForRefreshFromJira> result) {
        return dtoConverter.convertList(result, TestRunForRefreshFromJiraDto.class);
    }

    public ExecutionRequestDto convertCreatedExecutionRequestToDto(
            ExecutionRequest createdExecutionRequest) {
        return dtoConverter.convert(createdExecutionRequest, ExecutionRequestDto.class);
    }

    public List<JiraTicketUpdateRequest> convertJiraTicketUpdateRequest(
            List<JiraTicketUpdateRequestDto> jiraTicketUpdateRequest) {
        return dtoConverter.convertList(jiraTicketUpdateRequest, JiraTicketUpdateRequest.class);
    }
}
