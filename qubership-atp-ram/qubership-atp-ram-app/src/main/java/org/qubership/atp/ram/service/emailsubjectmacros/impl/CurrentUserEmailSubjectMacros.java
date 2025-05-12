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

package org.qubership.atp.ram.service.emailsubjectmacros.impl;

import static java.util.Objects.isNull;

import java.util.StringJoiner;
import java.util.UUID;

import org.qubership.atp.auth.springbootstarter.exceptions.AtpEntityNotFoundException;
import org.qubership.atp.ram.dto.response.ExecutionSummaryResponse;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.UserInfo;
import org.qubership.atp.ram.service.emailsubjectmacros.ResolvableEmailSubjectMacros;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CurrentUserEmailSubjectMacros implements ResolvableEmailSubjectMacros {

    @Override
    public String resolve(ExecutionRequest executionRequest, ExecutionSummaryResponse executionSummaryResponse) {
        UUID executionRequestId = executionRequest.getUuid();
        log.debug("Start resolving macros 'CURRENT_USER' for execution request with id: {}",
                executionRequestId);

        UUID executionSummaryId = executionSummaryResponse.getUuid();
        log.debug("Execution summary response id: {}", executionSummaryId);

        UserInfo userInfo = executionSummaryResponse.getUserInfo();

        if (isNull(userInfo)) {
            log.error("Failed to find User Info by execution summary response id: {}", executionSummaryId);
            throw new AtpEntityNotFoundException("User Info", "execution summary response id", executionSummaryId);
        }
        log.debug("Execution summary response user info id: {}", userInfo.getId());

        String currentUserUsername = userInfo.getUsername();
        String currentUserFirstName = userInfo.getFirstName();
        String currentUserLastName = userInfo.getLastName();

        StringJoiner username = new StringJoiner(" ");

        if (!Strings.isNullOrEmpty(currentUserFirstName)) {
            username.add(currentUserFirstName);
        }
        if (!Strings.isNullOrEmpty(currentUserLastName)) {
            username.add(currentUserLastName);
        }
        if (username.length() == 0) {
            username.add(currentUserUsername);
        }
        log.debug("Resolved value: {}", username);

        return username.toString();
    }
}
