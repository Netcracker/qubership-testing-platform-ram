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

package org.qubership.atp.ram.config;

import org.qubership.atp.integration.configuration.service.MailSenderService;
import org.qubership.atp.ram.service.mail.ExecutionRequestReport;
import org.qubership.atp.ram.service.mail.MailSenderConfig;
import org.qubership.atp.ram.services.DefectsService;
import org.qubership.atp.ram.services.ExecutionRequestService;
import org.qubership.atp.ram.services.RootCauseService;
import org.qubership.atp.ram.services.TestPlansService;
import org.qubership.atp.ram.services.TestRunService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmailConfigurationProvider {

    @Value("${mail.smtp.host}")
    private String mailSmtpHost;
    @Value("${mail.smtp.port}")
    private String mailSmtpPort;
    @Value("${mail.smtps.auth}")
    private String mailSmtpsAuth;
    @Value("${mail.smtp.ssl.enable}")
    private String mailSmtpSslEnable;
    @Value("${base.url}")
    private String baseUrl;
    private ExecutionRequestService erService;
    private TestRunService trService;
    private TestPlansService testPlansService;
    private DefectsService defectsService;
    private RootCauseService rcService;
    private MailSenderConfig mailSenderConfig;
    private MailSenderService mailSender;

    /**
     * Constructor for email configuration which injects {@link ExecutionRequestService}, {@link
     * TestRunService} and {@link TestPlansService}.
     *
     * @param erService        {@link ExecutionRequestService}
     * @param trService        {@link TestRunService}
     * @param testPlansService {@link TestPlansService}
     */
    @Autowired
    public EmailConfigurationProvider(ExecutionRequestService erService,
                                      TestRunService trService,
                                      TestPlansService testPlansService,
                                      DefectsService defectsService,
                                      RootCauseService rcService,
                                      MailSenderConfig mailSenderConfig,
                                      MailSenderService mailSender) {
        this.erService = erService;
        this.trService = trService;
        this.testPlansService = testPlansService;
        this.defectsService = defectsService;
        this.rcService = rcService;
        this.mailSenderConfig = mailSenderConfig;
        this.mailSender = mailSender;
    }

    /**
     * Return {@link ExecutionRequestReport} by specified parameters.
     *
     * @return {@link ExecutionRequestReport} by specified parameters
     */
    public ExecutionRequestReport provideEmailConfiguration() {
        final ExecutionRequestReport executionRequestReport = new ExecutionRequestReport(erService, trService,
                testPlansService, defectsService, rcService, mailSenderConfig, mailSender);
        executionRequestReport.setProperties(mailSmtpHost, mailSmtpPort, mailSmtpsAuth, mailSmtpSslEnable, baseUrl);
        return executionRequestReport;
    }
}
