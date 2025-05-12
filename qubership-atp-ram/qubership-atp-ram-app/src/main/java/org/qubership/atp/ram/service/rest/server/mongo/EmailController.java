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

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.atp.ram.config.EmailConfigurationProvider;
import org.qubership.atp.ram.dto.request.JointExecutionRequestMailSendRequest;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.service.mail.MailService;
import org.qubership.atp.ram.service.rest.dto.EmailProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@CrossOrigin
@RestController
@RequestMapping("api/email")
@RequiredArgsConstructor
@Slf4j
public class EmailController /*implements EmailControllerApi*/ {

    private final EmailConfigurationProvider configurationProvider;

    @Autowired
    private MailService mailService;

    @GetMapping(produces = MediaType.TEXT_HTML_VALUE, value = "/{executionRequestId}")
    @PreAuthorize("@entityAccess.checkAccess(@executionRequestService.getProjectIdByExecutionRequestId("
            + "#executionRequestId),'READ')")
    public String emailBody(@PathVariable("executionRequestId") UUID executionRequestId) {
        return configurationProvider.provideEmailConfiguration().buildEmailBody(executionRequestId);
    }

    /**
     * Send email to recipients.
     *
     * @param executionRequestId {@link UUID} of {@link ExecutionRequest}
     * @param properties         {@link EmailProperties}
     * @param userToken          current user auth token
     */
    @PostMapping(value = "/{executionRequestId}")
    @PreAuthorize("@entityAccess.checkAccess(@executionRequestService.getProjectIdByExecutionRequestId("
            + "#executionRequestId),'READ')")
    @AuditAction(auditAction = "Send email related with execution request '{{#executionRequestId}}' to recipients "
            + "provided in request body")
    public void send(@PathVariable UUID executionRequestId, @RequestBody EmailProperties properties,
                     @RequestHeader(value = HttpHeaders.AUTHORIZATION) String userToken) {
        log.info("Request to send email report for execution request '{}' with params '{}'",
                executionRequestId, properties);
        mailService.send(executionRequestId, properties, userToken);
    }

    /**
     * Send joint execution request report.
     */
    @PreAuthorize("@entityAccess.checkAccess(T(org.qubership.atp.ram.enums.UserManagementEntities)"
            + ".EXECUTION_REQUEST.getName(), #request.getProjectId(),'EXECUTE')")
    @PostMapping(value = "/jointExecutionRequest/send")
    @AuditAction(auditAction = "Send joint report for execution request '{{#params.executionRequestUuid}}' to "
            + "recipients provided in request body")
    public void sendJointExecutionRequestReport(@RequestBody JointExecutionRequestMailSendRequest request,
                                                @RequestHeader(value = HttpHeaders.AUTHORIZATION) String userToken) {
        log.info("Request to send joint execution request email report with request: '{}'", request);

        mailService.sendJointExecutionRequestReport(request, userToken);
    }
}
