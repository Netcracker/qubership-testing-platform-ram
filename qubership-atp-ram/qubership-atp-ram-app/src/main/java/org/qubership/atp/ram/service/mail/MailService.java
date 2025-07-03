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

import static java.util.Collections.singleton;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import org.apache.commons.lang.StringUtils;
import org.qubership.atp.integration.configuration.model.MailRequest;
import org.qubership.atp.integration.configuration.model.MailResponse;
import org.qubership.atp.integration.configuration.model.notification.Notification;
import org.qubership.atp.integration.configuration.service.MailSenderService;
import org.qubership.atp.integration.configuration.service.NotificationService;
import org.qubership.atp.ram.RamConstants;
import org.qubership.atp.ram.config.EmailConfigurationProvider;
import org.qubership.atp.ram.dto.request.JointExecutionRequestMailSendRequest;
import org.qubership.atp.ram.entities.MailRecipients;
import org.qubership.atp.ram.enums.MailMetadata;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.exceptions.jointexecutionrequests.RamJointExecutionRequestReportSendException;
import org.qubership.atp.ram.exceptions.testplans.RamTestPlanRecipientsNotFoundException;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.JointExecutionRequest;
import org.qubership.atp.ram.models.ReportTemplate;
import org.qubership.atp.ram.models.TestPlan;
import org.qubership.atp.ram.models.UserInfo;
import org.qubership.atp.ram.models.WidgetType;
import org.qubership.atp.ram.service.emailsubjectmacros.EmailSubjectMacrosService;
import org.qubership.atp.ram.service.rest.dto.EmailProperties;
import org.qubership.atp.ram.service.template.TemplateRenderService;
import org.qubership.atp.ram.service.template.WidgetModelFactory;
import org.qubership.atp.ram.services.ExecutionRequestDetailsService;
import org.qubership.atp.ram.services.ExecutionRequestReportingService;
import org.qubership.atp.ram.services.ExecutionRequestService;
import org.qubership.atp.ram.services.IssueService;
import org.qubership.atp.ram.services.JointExecutionRequestService;
import org.qubership.atp.ram.services.JointExecutionRequestsReportDataModel;
import org.qubership.atp.ram.services.ReportTemplatesService;
import org.qubership.atp.ram.services.TestPlansService;
import org.qubership.atp.ram.services.UserService;
import org.qubership.atp.ram.utils.TimeUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import freemarker.template.Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class MailService {

    @Value("${kafka.mails.enable}")
    private boolean kafkaMailsEnable;

    @Value("classpath:data/joint-execution-requests/ram-joint-execution-requests.template.ftl")
    private Resource jointExecutionRequestsTemplate;

    @Value("${kafka.mails.responses.enable}")
    private boolean kafkaMailsResponsesEnable;

    private static final int MAIL_SEND_SUCCESS_STATUS = 200;
    private static final int MAIL_SEND_FAILED_STATUS = 500;

    private final ExecutionRequestService erService;
    private final ExecutionRequestReportingService erReportingService;
    private final ExecutionRequestDetailsService erDetailsService;
    private final MailSenderService mailSender;
    private final EmailConfigurationProvider emailConfigurationProvider;
    private final ReportTemplatesService reportTemplatesService;
    private final WidgetModelFactory widgetModelFactory;
    private final TemplateRenderService templateRenderService;
    private final TestPlansService testPlansService;
    private final EmailSubjectMacrosService emailSubjectMacrosService;
    private final UserService userService;
    private final IssueService issueService;
    private final JointExecutionRequestService jointExecutionRequestService;
    private final NotificationService notificationService;
    private final ExecutionRequestService executionRequestService;

    /**
     * Method send email to recipients provided in json erProps.
     */
    public void send(ReportParams erProps) {
        ExecutionRequestReport configuration = emailConfigurationProvider.provideEmailConfiguration();
        configuration.setParams(erProps);
        configuration.sendReport();
    }

    /**
     * Method send email to recipients provided in json erProps.
     *
     * @deprecated use new implementation in {@link #sendFromTemplate(ReportParams)}.
     */
    @Deprecated
    public void send(ReportMailParams erProps) {
        ExecutionRequestReport configuration = emailConfigurationProvider.provideEmailConfiguration();
        ReportParams reportParams = new ReportParams();
        reportParams.setExecutionRequestUuid(erProps.getExecutionRequestUuid());
        reportParams.setRecipients(String.join(",", erProps.getRecipients()));
        configuration.setParams(reportParams);
        configuration.sendReport();
    }

    /**
     * Send email reprot.
     *
     * @param executionRequestId execution request identifier
     * @param properties email properties
     * @param userToken user token
     */
    public void send(UUID executionRequestId, EmailProperties properties, String userToken) {
        boolean isJointExecutionRequest = jointExecutionRequestService.isJointExecutionRequest(executionRequestId);
        issueService.recalculateTopIssues(executionRequestId);
        if (isJointExecutionRequest) {
            log.debug("Selected joint execution report type");
            final JointExecutionRequest activeJointExecutionRequest =
                    jointExecutionRequestService.getActiveJointExecutionRequest(executionRequestId);

            final boolean isReadyToSend =
                    jointExecutionRequestService.isJointExecutionRequestReady(activeJointExecutionRequest);
            log.debug("Is ready to send: {} for joint ER with id: {}", isReadyToSend, executionRequestId);

            if (isReadyToSend) {
                final List<UUID> executionRequestIds = activeJointExecutionRequest.getCompletedExecutionRequestIds();
                final Set<String> recipients = erReportingService.getEmailRecipients(executionRequestIds);
                final ReportParams params = new ReportParams();
                params.setRecipients(String.join(",", recipients));
                params.setSubject(activeJointExecutionRequest.getName());

                try {
                    log.debug("Calling send function...");
                    send(singleton(executionRequestId), params,
                            reportParams -> sendJointExecutionRequestReport(activeJointExecutionRequest, reportParams));

                    jointExecutionRequestService.completeJointExecutionRequest(activeJointExecutionRequest);
                } catch (Exception e) {
                    log.error("Failed to send joint execution request report", e);
                    jointExecutionRequestService.completeFailedJointExecutionRequest(activeJointExecutionRequest, e);
                }
            }
        } else {
            log.debug("Selected default report type");

            final ReportParams params = new ReportParams();
            params.setRecipients(properties.getRecipients());
            params.setExecutionRequestUuid(executionRequestId);
            params.setTemplateId(properties.getTemplateId());
            params.setDescriptions(properties.getDescriptions());
            params.setSubject(properties.getSubject());
            params.setExecutionRequestsSummary(Boolean.parseBoolean(properties.getIsExecutionRequestsSummary()));
            params.setExecutionSummaryRunsSummary(Boolean.parseBoolean(properties.getIsExecutionSummaryRunsSummary()));
            params.setUserToken(userToken);
            log.debug("Report params: {}", params);

            send(singleton(executionRequestId), params, this::sendFromTemplate);
        }
    }

    /**
     * Send email for execution request with specified params.
     *
     * @param executionRequestIds execution request identifiers
     * @param params              report params
     * @param sendFunc            email send function
     */
    public void send(Collection<UUID> executionRequestIds, ReportParams params, Consumer<ReportParams> sendFunc) {
        executionRequestIds.forEach(executionRequestId -> {
            final String subject = params.getSubject();
            final String recipients = params.getRecipients();

            erReportingService.updateReportingInfo(executionRequestId, subject, recipients);
        });

        log.debug("Email params before send: {}", params);
        try {
            // send function
            sendFunc.accept(params);

            // when we use REST, the interaction is synchronous
            // if there were no errors we update status to passed
            if (!kafkaMailsEnable) {
                executionRequestIds.forEach(executionRequestId -> {
                    final TestingStatuses passedStatus = TestingStatuses.PASSED;

                    erReportingService.updateReportingStatus(executionRequestId, passedStatus);
                    erDetailsService.createDetails(executionRequestId, passedStatus, "mail sent successfully");
                });
            }
        } catch (RamJointExecutionRequestReportSendException e) {
            try {
                String errorMessage = e.toString();
                String errorMessageBody = errorMessage.substring(errorMessage.indexOf("{"));
                if (!StringUtils.isEmpty(errorMessageBody)) {
                    MailResponse mailResponse = RamConstants.OBJECT_MAPPER
                            .readValue(errorMessageBody, MailResponse.class);
                    executionRequestIds.forEach(erId ->
                            erDetailsService.saveMessageResponseDetails(mailResponse, erId));
                }
                executionRequestIds.forEach(erId ->
                        erReportingService.updateReportingStatus(erId, TestingStatuses.FAILED));
            } catch (Exception ex) {
                executionRequestIds.forEach(executionRequestId -> updateStatusesToFailed(executionRequestId, e));
            }
        } catch (Exception e) {
            log.error("Error during mail send processes", e);
            executionRequestIds.forEach(executionRequestId -> updateStatusesToFailed(executionRequestId, e));
        }
    }

    private void updateStatusesToFailed(UUID executionRequestId, Exception e) {
        erDetailsService.createFailedDetails(executionRequestId, e);
        erReportingService.updateReportingStatus(executionRequestId, TestingStatuses.FAILED);
    }

    /**
     * Sends email based on UI Template.
     *
     * @param reportParams report params
     */
    public void sendFromTemplate(ReportParams reportParams) {
        log.debug("Send from template with report params: {}", reportParams);
        final UUID executionRequestId = reportParams.getExecutionRequestUuid();
        final ExecutionRequest executionRequest = erService.get(executionRequestId);

        sendFromTemplate(reportParams, executionRequest);
        erService.updateAnalyzedByQa(singleton(executionRequestId), true);
    }

    /**
     * Sends email based on UI Template.
     *
     * @param reportParams     report params
     * @param executionRequest Execution request
     */
    public void sendFromTemplate(ReportParams reportParams, ExecutionRequest executionRequest) {
        log.info("Start sending Report Template Email for Execution Request : " + reportParams);
        final UUID templateId = reportParams.getTemplateId();

        ReportTemplate template;
        if (templateId != null) {
            log.debug("Getting provided report template with id: {}", templateId);
            template = reportTemplatesService.get(templateId);
        } else {
            final UUID projectId = executionRequest.getProjectId();
            log.debug("Getting active report template for project with id: {}", projectId);
            template = reportTemplatesService.getActiveTemplateByProjectId(projectId);
        }
        log.info("Report template : {}", template.getName());

        final List<WidgetType> widgets = template.getWidgets();
        log.debug("Report template widgets: {}", template);

        final Map<WidgetType, Map<String, Object>> model = widgetModelFactory.generateModel(reportParams, widgets);
        log.debug("Report template model: {}", model);

        final String htmlBody = templateRenderService.render(template, model);
        log.info("Rendered report html body: {}", model);

        final MailRequest mailRequest = buildRequest(htmlBody, reportParams, executionRequest);
        log.debug("Mail request: {}", mailRequest);

        MailResponse mailResponse = mailSender.send(mailRequest);
        if (needToSaveMessageResponse(mailResponse)) {
            erDetailsService.saveMessageResponseDetails(mailResponse, executionRequest.getUuid());
        }
    }

    private boolean needToSaveMessageResponse(MailResponse mailResponse) {
        return !kafkaMailsResponsesEnable || mailResponse.getStatus() >= 300;
    }

    /**
     * Send joint execution request report.
     *
     * @param jointExecutionRequest joint execution request
     * @param reportParams report params
     */
    public void sendJointExecutionRequestReport(JointExecutionRequest jointExecutionRequest,
                                                ReportParams reportParams) {
        final List<UUID> completedExecutionRequestIds = jointExecutionRequest.getCompletedExecutionRequestIds();
        log.info("Send completed joint execution request '{}' with report params: {}",
                completedExecutionRequestIds, reportParams);
        sendJointExecutionRequestReport(completedExecutionRequestIds, reportParams);
    }

    /**
     * Send joint execution request report.
     *
     * @param request mail request
     */
    public void sendJointExecutionRequestReport(JointExecutionRequestMailSendRequest request, String userToken) {
        final String key = request.getKey();
        final JointExecutionRequest jointExecutionRequest = jointExecutionRequestService.getJointExecutionRequest(key);

        final ReportParams reportParams = new ReportParams();
        reportParams.setUserToken(userToken);

        final String recipients = String.join(",", request.getRecipients());
        reportParams.setRecipients(recipients);

        final String subject = request.getSubject();
        reportParams.setSubject(subject);

        log.info("Prepare to send joint execution request with params: {} and by key: {}",
                request, jointExecutionRequest);
        sendJointExecutionRequestReport(jointExecutionRequest, reportParams);
    }

    /**
     * Send joint execution request report.
     *
     * @param jointExecutionRequest joint execution request
     */
    public void sendJointExecutionRequestReport(JointExecutionRequest jointExecutionRequest) {
        final List<UUID> completedExecutionRequestIds = jointExecutionRequest.getCompletedExecutionRequestIds();
        log.info("Truing to send a report for completed joint execution request with id: {} for joint ER {}",
                completedExecutionRequestIds, jointExecutionRequest.getUuid());
        final Set<String> recipients = erReportingService.getEmailRecipients(completedExecutionRequestIds);
        log.debug("Recipients: {}", recipients);

        if (!CollectionUtils.isEmpty(recipients)) {
            final ReportParams params = new ReportParams();
            params.setRecipients(String.join(",", recipients));
            params.setSubject(jointExecutionRequest.getName());

            sendJointExecutionRequestReport(completedExecutionRequestIds, params);
        } else {
            log.warn("No recipients found for joint execution request '{}'. Mail sending was skipped.",
                    completedExecutionRequestIds);
            String errLog = "Report recipients were not found";
            jointExecutionRequestService.completeFailedJointExecutionRequest(jointExecutionRequest, errLog);
        }
    }

    private MailResponse sendJointExecutionRequestReport(List<UUID> executionRequestIds, ReportParams reportParams) {
        log.info("Sending joint execution report with ids: {}", executionRequestIds);
        try {
            final InputStream inputStream = jointExecutionRequestsTemplate.getInputStream();
            final Template template = new Template("template", new InputStreamReader(inputStream), null);
            final JointExecutionRequestsReportDataModel model =
                    jointExecutionRequestService.getJointExecutionRequestsReportDataModel(executionRequestIds);
            log.debug("Report model: {} for joint execution report with ids: {}", model, executionRequestIds);

            final String htmlBody = templateRenderService.render(template, model);
            log.debug("Report body: {}  for joint execution report with ids: {}", htmlBody, executionRequestIds);
            erService.updateAnalyzedByQa(executionRequestIds, true);

            log.debug("Sending joint execution report for joint execution report with ids: {}", executionRequestIds);
            final MailResponse mailResponse = mailSender.send(buildRequest(htmlBody, reportParams));

            final int status = mailResponse.getStatus();

            sendMailSendNotification(status, reportParams, executionRequestIds);

            return mailResponse;
        } catch (Exception e) {
            log.error("Error during sending joint execution report", e);
            sendMailSendNotification(MAIL_SEND_FAILED_STATUS, reportParams, executionRequestIds);
            throw new RamJointExecutionRequestReportSendException();
        }
    }

    private void sendMailSendNotification(int status, ReportParams reportParams, List<UUID> executionRequestIds) {
        log.info("Sending joint execution request notification with status {} and parameters: {} with ids: {}",
                status, reportParams, executionRequestIds);
        final Notification notification = buildMailSendNotification(status, reportParams);
        notificationService.sendNotification(notification);
        log.info("Joint execution request notification has been sent: {} with status {} and ids: {}",
                notification, status, executionRequestIds);
    }

    private Notification buildMailSendNotification(int status, ReportParams reportParams) {
        final String userToken = reportParams.getUserToken();
        final UUID userId = userService.getUserIdFromToken(userToken);

        final boolean isSendSuccessful = status == MAIL_SEND_SUCCESS_STATUS;

        final String message = isSendSuccessful ? "<b>Successful</b><br><br>Join Execution Request report is sent"
                : "Sending Join Execution Request<br>Report is failed";

        final Notification.Type type = isSendSuccessful ? Notification.Type.SUCCESS : Notification.Type.ERROR;

        return new Notification(message, type, userId);
    }

    private MailRequest buildRequest(String content, ReportParams reportParams) {
        MailRequest request = new MailRequest();
        request.setSubject(reportParams.getSubject());
        request.setTo(reportParams.getRecipients());
        request.setContent(content);
        request.setService(MailMetadata.ATP_RAM.getValue());

        if (Objects.nonNull(reportParams.getExecutionRequestUuid())) {
            UUID projectId = executionRequestService.getProjectIdByExecutionRequestId(
                    reportParams.getExecutionRequestUuid());
            if (Objects.nonNull(projectId)) {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put(MailMetadata.PROJECT_ID.getValue(), projectId);
                request.setMetadata(metadata);
            }
        }
        return request;
    }

    private MailRequest buildRequest(String content, ReportParams reportParams, ExecutionRequest executionRequest) {
        MailRequest request = new MailRequest();
        String subject = reportParams.getSubject() == null
                ? getDefaultSubject(executionRequest)
                : reportParams.getSubject();

        String userToken = reportParams.getUserToken();
        UserInfo userInfo = userService.getUserInfoFromToken(userToken);

        String resolvedMacrosSubject =
                emailSubjectMacrosService.resolveSubjectMacros(executionRequest.getUuid(), subject, userInfo);
        request.setSubject(resolvedMacrosSubject);
        request.setTo(getRecipients(reportParams));
        request.setContent(content);
        request.setService(MailMetadata.ATP_RAM.getValue());
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(MailMetadata.EXECUTION_REQUEST_ID.getValue(), reportParams.getExecutionRequestUuid());
        metadata.put(MailMetadata.PROJECT_ID.getValue(), executionRequest.getProjectId());
        request.setMetadata(metadata);
        return request;
    }

    private String getDefaultSubject(ExecutionRequest executionRequest) {
        return String.format("%s [%s]",
                executionRequest.getName(),
                TimeUtils.formatDateTime(executionRequest.getStartDate(), TimeUtils.DEFAULT_DATE_TIME_PATTERN));
    }

    private String getRecipients(ReportParams reportParams) {
        final String recipientsFromAdapter = reportParams.getRecipients();
        final UUID executionRequestId = reportParams.getExecutionRequestUuid();

        if (StringUtils.isNotBlank(recipientsFromAdapter)) {
            log.trace("Recipients for ER: {} from adapter: {}.", executionRequestId, recipientsFromAdapter);
            return recipientsFromAdapter;
        }

        final ExecutionRequest executionRequest = erService.get(executionRequestId);
        final UUID testPlanId = executionRequest.getTestPlanId();
        final TestPlan testPlan = testPlansService.findByTestPlanUuid(testPlanId);

        if (Objects.nonNull(testPlan)) {
            final MailRecipients recipients = testPlan.getRecipients();

            if (Objects.nonNull(recipients)) {
                String tpRecipients = recipients.recipientsAsString();
                log.debug("Recipients for ER: {} from TestPlan: {}", executionRequestId, testPlan);

                return tpRecipients;
            }
        }
        log.error("Failed to found recipients for the execution request: {}", executionRequest);
        throw new RamTestPlanRecipientsNotFoundException();
    }
}
