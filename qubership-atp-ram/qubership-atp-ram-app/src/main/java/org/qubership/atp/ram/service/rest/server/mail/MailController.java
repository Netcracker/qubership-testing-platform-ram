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

package org.qubership.atp.ram.service.rest.server.mail;

import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.atp.ram.service.mail.MailSenderConfig;
import org.qubership.atp.ram.service.mail.MailService;
import org.qubership.atp.ram.service.mail.ReportMailParams;
import org.qubership.atp.ram.service.mail.ReportParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("api/mail")
@RestController()
public class MailController /*implements MailControllerApi*/ {
    private static final Logger log = LoggerFactory.getLogger(MailController.class);
    private MailService service;
    private MailSenderConfig mailSenderConfig;

    /**
     * Constructor.
     */
    @Autowired
    public MailController(MailService service, MailSenderConfig mailSenderConfig) {
        this.service = service;
        this.mailSenderConfig = mailSenderConfig;
    }

    @GetMapping(value = "/ping", produces = TEXT_PLAIN_VALUE)
    public String ping() {
        return "pong";
    }

    /**
     * Send ER report to recipients provided in params.
     */
    @Deprecated
    @PostMapping(value = "/send/er/report", produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> sendExecutionRequestReport(@RequestBody ReportParams params) {
        try {
            service.send(params);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error during sending report: ", e);
            return ResponseEntity
                    .badRequest()
                    .body("Error during sending report: " + e.getMessage());
        }
    }

    /**
     * Send ER report to recipients provided in params.
     */
    @PostMapping(value = "/send/er/report/v2", produces = TEXT_PLAIN_VALUE)
    @AuditAction(auditAction = "Send execution request '{{#params.executionRequestUuid}}' report to recipients "
            + "provided in request body")
    public ResponseEntity<String> sendExecutionRequestReportV2(@RequestBody ReportMailParams params) {
        try {
            service.send(params);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error during sending report: ", e);
            return ResponseEntity
                    .badRequest()
                    .body("Error during sending report: " + e.getMessage());
        }
    }

    /**
     * Update MailSender config.
     */
    @PutMapping("/config/put")
    public MailSenderConfig setConfig(@RequestBody MailSenderConfig config) {
        this.mailSenderConfig.setMailSenderUrl(config.getMailSenderUrl());
        return mailSenderConfig;
    }

    /**
     * Get current MailSender config.
     */
    @GetMapping("/config/get")
    public MailSenderConfig getConfig() {
        return mailSenderConfig;
    }
}
