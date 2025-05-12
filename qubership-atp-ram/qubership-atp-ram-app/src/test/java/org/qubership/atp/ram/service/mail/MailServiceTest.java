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

package org.qubership.atp.ram.service.mail;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.qubership.atp.integration.configuration.model.MailRequest;
import org.qubership.atp.integration.configuration.service.MailSenderService;
import org.qubership.atp.ram.exceptions.testplans.RamTestPlanRecipientsNotFoundException;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.ReportTemplate;
import org.qubership.atp.ram.models.TestPlan;
import org.qubership.atp.ram.models.UserInfo;
import org.qubership.atp.ram.models.WidgetType;
import org.qubership.atp.ram.service.emailsubjectmacros.EmailSubjectMacrosService;
import org.qubership.atp.ram.service.template.TemplateRenderService;
import org.qubership.atp.ram.service.template.WidgetModelFactory;
import org.qubership.atp.ram.services.ExecutionRequestDetailsService;
import org.qubership.atp.ram.services.ExecutionRequestService;
import org.qubership.atp.ram.services.ReportTemplatesService;
import org.qubership.atp.ram.services.TestPlansService;
import org.qubership.atp.ram.services.UserService;
import org.qubership.atp.ram.utils.TimeUtils;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class MailServiceTest {

    @Mock
    private ExecutionRequestService erService;
    @Mock
    private MailSenderService mailSender;
    @Mock
    private ExecutionRequestDetailsService erDetailsService;
    @Mock
    private ReportTemplatesService reportTemplatesService;
    @Mock
    private WidgetModelFactory widgetModelFactory;
    @Mock
    private TemplateRenderService templateRenderService;
    @Mock
    private TestPlansService testPlansService;
    @Mock
    private EmailSubjectMacrosService emailSubjectMacrosService;
    @Mock
    private UserService userService;

    @InjectMocks
    private MailService mailService;

    private ReportParams noRecipientsReportParams;

    private String executionRequestName = "Execution Request";
    private Timestamp executionRequestStartDate = Timestamp.from(Instant.now());
    private String defaultEmailSubject;

    private String username = "username";

    @BeforeEach
    public void setUp() {
        noRecipientsReportParams = createReportParamsWithNoRecipients();
        when(erService.get(any())).thenReturn(generateEr());
        when(reportTemplatesService.getActiveTemplateByProjectId(any())).thenReturn(new ReportTemplate());
        when(testPlansService.findByTestPlanUuid(any())).thenReturn(new TestPlan());
        when(userService.getUserInfoFromToken(any())).thenReturn(generateUserInfo());
        defaultEmailSubject = String.format("%s [%s]",
                executionRequestName,
                TimeUtils.formatDateTime(executionRequestStartDate, TimeUtils.DEFAULT_DATE_TIME_PATTERN));
    }

    private ExecutionRequest generateEr() {
        ExecutionRequest executionRequest = new ExecutionRequest();
        executionRequest.setName(executionRequestName);
        executionRequest.setStartDate(executionRequestStartDate);
        return executionRequest;
    }

    private UserInfo generateUserInfo() {
        UserInfo userInfo = new UserInfo();
        userInfo.setUsername(username);
        return userInfo;
    }

    private ReportParams createReportParamsWithNoRecipients() {
        ReportParams params = new ReportParams();
        params.setExecutionRequestUuid(UUID.randomUUID());
        params.setSubject("Test Subject");
        params.setDescriptions(new HashMap<String, String>() {{
            put(WidgetType.ENVIRONMENTS_INFO.toString(), "description");
        }});
        params.setUserToken("user token");
        return params;
    }

    private ReportParams createReportParamsWithNullSubject() {
        ReportParams params = new ReportParams();
        params.setExecutionRequestUuid(UUID.randomUUID());
        params.setRecipients("qstp@some-domain.com");
        params.setDescriptions(new HashMap<String, String>() {{
            put(WidgetType.ENVIRONMENTS_INFO.toString(), "description");
        }});
        params.setUserToken("user token");
        return params;
    }

    @Test
    public void onMailService_whenSendEmailWithNoRecipients_thenThrowException() {
        when(emailSubjectMacrosService
                .resolveSubjectMacros(any(), any(), any())).thenReturn(noRecipientsReportParams.getSubject());

        Assertions.assertThrows(RamTestPlanRecipientsNotFoundException.class, () -> {
            mailService.sendFromTemplate(noRecipientsReportParams);
        });
    }

    @Test
    public void onMailService_whenSendEmailWithNullSubject_thenDefaultSubjectSent() {
        when(emailSubjectMacrosService
                .resolveSubjectMacros(any(), any(), any())).thenReturn(defaultEmailSubject);
        mailService.sendFromTemplate(createReportParamsWithNullSubject());
        ArgumentCaptor<MailRequest> captor = ArgumentCaptor.forClass(MailRequest.class);
        Mockito.verify(mailSender).send(captor.capture());
        MailRequest mailRequest = captor.getValue();
        Assertions.assertNotNull(mailRequest);
        Assertions.assertEquals(defaultEmailSubject, mailRequest.getSubject());
    }
}
