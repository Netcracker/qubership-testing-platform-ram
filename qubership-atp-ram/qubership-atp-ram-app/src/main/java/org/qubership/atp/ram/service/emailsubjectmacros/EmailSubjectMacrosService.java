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

package org.qubership.atp.ram.service.emailsubjectmacros;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.qubership.atp.ram.dto.response.ExecutionSummaryResponse;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.UserInfo;
import org.qubership.atp.ram.service.rest.dto.EmailSubjectMacrosResponse;
import org.qubership.atp.ram.services.ExecutionRequestService;
import org.qubership.atp.ram.services.ReportService;
import org.qubership.atp.ram.utils.StreamUtils;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailSubjectMacrosService {

    private static final Pattern MACROS_PATTERN = Pattern.compile("\\$\\{.*?}");

    private final EmailSubjectMacrosFactory macrosFactory;
    private final ReportService reportService;
    private final ExecutionRequestService executionRequestService;

    /**
     * Get all email subject macros.
     *
     * @return list of macros
     */
    public List<EmailSubjectMacrosResponse> getAll() {
        log.info("Get all email subject macros");

        List<EmailSubjectMacrosEnum> subjectMacrosEnums = EmailSubjectMacrosEnum.getAll();
        List<EmailSubjectMacrosResponse> emailSubjectMacros =
                StreamUtils.mapToClazz(subjectMacrosEnums, EmailSubjectMacrosResponse.class);

        log.debug("Found macros: {}", emailSubjectMacros);

        return emailSubjectMacros;
    }

    /**
     * Resolve subject macros in context of specified execution request.
     *
     * @param executionRequestId execution request id
     * @param subject email subject
     * @param userInfo current user info
     * @return resolved subject value
     */
    public String resolveSubjectMacros(UUID executionRequestId, String subject, UserInfo userInfo) {
        log.debug("Trying to resolve email subject '{}' for execution request '{}'", subject, executionRequestId);
        ExecutionRequest executionRequest = executionRequestService.get(executionRequestId);
        ExecutionSummaryResponse executionSummaryResponse = reportService
                .getExecutionSummary(executionRequestId, false);
        executionSummaryResponse.setUserInfo(userInfo);

        log.debug("Found execution request '{}'", executionRequest.getName());

        Matcher matcher = MACROS_PATTERN.matcher(subject);

        while (matcher.find()) {
            String macrosName = matcher.group(0);
            log.debug("Resolve subject '{}' macros '{}'", subject, macrosName);

            ResolvableEmailSubjectMacros macros = macrosFactory.getMacros(macrosName);
            String resolvedValue = macros.resolve(executionRequest, executionSummaryResponse);
            log.debug("Resolved value '{}'", resolvedValue);

            subject = subject.replace(macrosName, resolvedValue);
            log.debug("Subject value after '{}' macros resolve: {}", macros, subject);
        }

        return subject;
    }
}
