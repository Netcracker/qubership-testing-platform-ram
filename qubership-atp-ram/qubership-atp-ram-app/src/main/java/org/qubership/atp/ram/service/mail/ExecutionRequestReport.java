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

import static org.qubership.atp.ram.enums.DefaultSuiteNames.EXECUTION_REQUESTS_LOGS;
import static org.qubership.atp.ram.enums.MailMetadata.ATP_RAM;
import static org.qubership.atp.ram.enums.MailMetadata.EXECUTION_REQUEST_ID;
import static org.qubership.atp.ram.enums.MailMetadata.PROJECT_ID;

import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.qubership.atp.integration.configuration.model.MailRequest;
import org.qubership.atp.integration.configuration.service.MailSenderService;
import org.qubership.atp.ram.config.ApiPath;
import org.qubership.atp.ram.enums.DefaultRootCauseType;
import org.qubership.atp.ram.enums.DefaultSuiteNames;
import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.exceptions.executionrequests.RamExecutionRequestReportSendException;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.TestPlan;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.services.DefectsService;
import org.qubership.atp.ram.services.ExecutionRequestService;
import org.qubership.atp.ram.services.RootCauseService;
import org.qubership.atp.ram.services.TestPlansService;
import org.qubership.atp.ram.services.TestRunService;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExecutionRequestReport {

    private final ExecutionRequestService erService;
    private final TestRunService trService;
    private final TestPlansService testPlansService;
    private final DefectsService defectsService;
    private final RootCauseService rcService;
    private final MailSenderConfig mailSenderConfig;
    private final MailSenderService mailSender;
    private ReportParams reportParams;
    private String template;
    private String mailSmtpHost;
    private String mailSmtpPort;
    private String mailSmtpsAuth;
    private String mailSmtpSslEnable;
    private String baseUrl;
    private ExecutionRequest executionRequest;

    /**
     * Constructor.
     */
    @Autowired
    public ExecutionRequestReport(ExecutionRequestService erService,
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

    private static List<String> checkHosts(List<String> hosts) {
        if (hosts == null) {
            return Collections.emptyList();
        }
        if (hosts.size() == 1) {
            return new ArrayList<>(Arrays.asList(hosts.get(0).split(";\\s?")));
        }
        return hosts;
    }

    /**
     * Set erReport parameters.
     */
    public void setParams(ReportParams params) {
        this.reportParams = params;
    }

    /**
     * Set mail parameters and baseUrl.
     */
    public void setProperties(String mailSmtpHost, String mailSmtpPort, String mailSmtpsAuth, String mailSmtpSslEnable,
                              String baseUrl) {
        this.mailSmtpHost = mailSmtpHost;
        this.mailSmtpPort = mailSmtpPort;
        this.mailSmtpsAuth = mailSmtpsAuth;
        this.mailSmtpSslEnable = mailSmtpSslEnable;
        this.baseUrl = baseUrl;
    }

    /**
     * Send current report to recipients.
     */
    public void sendReport() {
        final UUID executionRequestId = reportParams.getExecutionRequestUuid();
        log.info("Sending report for ER {}.", executionRequestId);
        if (Strings.isNullOrEmpty(reportParams.getRecipients())) {
            log.warn("List of recipients is null. Report parameters: {}", reportParams);
            return;
        }
        template = getTemplate();
        executionRequest = erService.findById(executionRequestId);
        String content = buildEmailBody(executionRequestId);
        if (content == null) {
            log.error("The execution request '{}' report wasn't sent because of email empty content body",
                    executionRequestId);
            throw new RamExecutionRequestReportSendException();
        }

        mailSender.send(buildRequest(content));
    }

    /**
     * Builds HTML body for email.
     *
     * @param executionRequestId {@link java.util.UUID} of {@link ExecutionRequest}
     * @return html body as {@link String}
     */
    @Nullable
    public String buildEmailBody(UUID executionRequestId) {
        log.debug("Start building email body for ER {}.", executionRequestId);
        Statistics statistics = new Statistics();
        if (executionRequest == null) {
            executionRequest = erService.findById(executionRequestId);
        }
        statistics.setRootCauseAndCount(trService.getTestRunsGroupedByRootCauses(executionRequestId));
        UUID previousErId = executionRequest.getPreviousExecutionRequestId();
        if (Objects.nonNull(previousErId)) {
            log.trace("For ER id:{} - Previous ER id: {}", executionRequest.getUuid(), previousErId);
            ExecutionRequest prevEr = erService.findById(previousErId);
            if (prevEr != null) {
                statistics
                        .setRootCauseAndCountPrevEr(trService.getTestRunsGroupedByRootCauses(prevEr.getUuid()));
                statistics.countAllTestRunsForPrevEr();
                statistics.setParamsForPrevEr(prevEr);
            } else {
                log.trace("Previous ER {} for ER {} not found.", previousErId, executionRequest.getUuid());
            }
        } else {
            log.trace("There is no previous ER for ER id {}.", executionRequest.getUuid());
        }
        equalizeMaps(statistics);
        // TODO: 12/20/2018 this need for 'send run report' from ATP ui. as WA
        if (ExecutionStatuses.IN_PROGRESS.equals(executionRequest.getExecutionStatus())) {
            return null;
        }
        setStatisticsForEr(executionRequest, statistics);
        if (template == null) {
            template = getTemplate();
        }
        List<TestRun> tmp = trService.findTestRunsWithFillStatusByRequestId(executionRequestId);
        List<TestRun> testRuns = new ArrayList<>();
        tmp.forEach(testRun -> {
            if (!EXECUTION_REQUESTS_LOGS.getName().equalsIgnoreCase(testRun.getName())) {
                testRuns.add(testRun);
            }
        });

        if (testRuns.isEmpty()) {
            log.warn("ER {} hasn't test runs.", executionRequest.getUuid());
            return null;
        }
        statistics.setAllTr(testRuns.size());
        Map<String, StringBuilder> suiteParts = new LinkedHashMap<>();
        Set<String> qaHosts = new HashSet<>(testRuns.size());
        Set<String> solutionBuilds = new HashSet<>(testRuns.size());
        for (TestRun tr : testRuns) {
            qaHosts.addAll(checkHosts(tr.getQaHost()));
            solutionBuilds.addAll(tr.getSolutionBuild() == null ? Collections.emptyList() : tr.getSolutionBuild());
            setStatisticsForLr(tr.getUuid(), statistics);
            setStatisticsForTr(tr, statistics);
            StringBuilder rows = suiteParts.get(DefaultSuiteNames.SINGLE_TEST_RUNS.getName());
            if (rows == null) {
                rows = new StringBuilder();
            }
            String failureReason = tr.getRootCauseId() == null
                    ? DefaultRootCauseType.NOT_ANALYZED.getName()
                    : rcService.getById(tr.getRootCauseId()).getName();

            rows.append("<tr>")
                    .append("<td style=\"padding: 2px; background:").append(statistics.getColor())
                    .append("; color:").append(statistics.getFontColor())
                    .append("\">").append(tr.getTestingStatus().getName()).append("</td>")
                    .append("<td style=\"padding: 2px\">").append(printTestRunLink(tr)).append("</td>")
                    .append("<td style=\"padding: 2px\">").append(convertSecondToHhMmSsString(tr.getDuration()))
                    .append("</td>")
                    .append("<td style=\"padding: 2px\">").append(failureReason).append("</td>")
                    .append("<td style=\"padding: 2px\">").append(printFailedLogRecords(tr))
                    .append("</td>")
                    .append("<td style=\"padding: 2px\">").append(printFdrLink(tr)).append("</td>")
                    .append("</tr>");
            suiteParts.put(DefaultSuiteNames.SINGLE_TEST_RUNS.getName(), rows);
        }
        StringBuilder testRunsTable = prepareTestRunsTable(suiteParts);
        StringBuilder qaHost = createTableOfItems(qaHosts);
        StringBuilder builds = createTableOfItems(solutionBuilds);
        String report = replaceTemplatePlaceHolders(testRunsTable, qaHost.toString(), statistics, builds.toString());
        log.debug("Finish building email body for ER {}.", executionRequestId);
        return report;
    }

    private String printFdrLink(TestRun tr) {
        if (Strings.isNullOrEmpty(tr.getFdrLink())) {
            return "";
        }
        return "<a href='"
                + tr.getFdrLink()
                + "'>Navigate to TSG</a>";
    }

    private void equalizeMaps(Statistics stat) {
        Map<String, Integer> rootCauseAndCountPrevEr = stat.getRootCauseAndCountPrevEr();
        Map<String, Integer> rootCauseAndCount = stat.getRootCauseAndCount();
        Set<String> listNames = new HashSet<>();
        try {
            Preconditions.checkNotNull(rootCauseAndCount, "Map with Root Cause statistic fo current ER is null");
            listNames.addAll(rootCauseAndCount.keySet());
            Preconditions.checkNotNull(rootCauseAndCountPrevEr,
                    "Map with Root Cause statistic fo previous ER is null");
            listNames.addAll(rootCauseAndCountPrevEr.keySet());
        } catch (NullPointerException e) {
            log.warn(e.getMessage());
        } finally {
            stat.setRootCausesNames(listNames);
        }
    }

    private String printHeader(Statistics stat) {
        StringBuilder headers = new StringBuilder();
        StringBuilder countAndRate = new StringBuilder();
        countAndRate.append("<tr>");
        Set<String> listNames = stat.getRootCausesNames();
        if (!listNames.isEmpty()) {
            for (String name : listNames) {
                headers.append("<td colspan='2' align='center'><b>").append(name).append("</b></td>");
                countAndRate.append("<td><b>Count</b></td><td><b>%</b></td>");
            }
        }
        headers.append("</tr>");
        headers.append(countAndRate);
        return headers.toString();
    }

    private String printBody(boolean currentEr, Map<String, Integer> rootCausesAndCount, Statistics stat) {
        StringBuilder builder = new StringBuilder();
        Set<String> listNames = stat.getRootCausesNames();
        if (rootCausesAndCount != null && !rootCausesAndCount.isEmpty() && !listNames.isEmpty()) {
            if (!currentEr) {
                builder.append("<td style='width:15px'>2</td>\n")
                        .append("<td style='width:auto'>").append(stat.getPrevErStartDate()).append("</td>\n")
                        .append("<td style='width:auto'>").append(stat.getPrevErName()).append("</td>\n");
            }
            for (String name : listNames) {
                Integer count = rootCausesAndCount.get(name) == null ? 0 : rootCausesAndCount.get(name);
                builder.append("<td>").append(count).append("</td><td>")
                        .append(String.format(Locale.ROOT, "%.1f",
                                ((double) count / (currentEr ? stat.getAllTr() : stat.getAllPrevTr())) * 100))
                        .append("</td>");
            }
            return builder.toString();
        }
        return "";
    }

    private void setStatisticsForTr(TestRun testRun, Statistics statistics) {
        switch (testRun.getTestingStatus()) {
            case FAILED:
                statistics.setColor("red");
                statistics.setFontColor("black");
                statistics.addTrFailed();
                break;
            case WARNING:
                statistics.setColor("yellow");
                statistics.setFontColor("black");
                statistics.addTrWarning();
                break;
            case PASSED:
                statistics.setColor("limegreen");
                statistics.setFontColor("black");
                statistics.addTrPassed();
                break;
            case BLOCKED:
            case STOPPED:
                statistics.setColor("#ff5218");
                statistics.setFontColor("black");
                statistics.addTrError();
                break;
            case SKIPPED:
                statistics.setColor("#ffaa32");
                statistics.setFontColor("black");
                break;
            default:
                statistics.setColor("white");
                statistics.setFontColor("black");
                break;
        }
    }

    private void setStatisticsForEr(ExecutionRequest er, Statistics statistics) {
        switch (er.getExecutionStatus()) {
            case FINISHED:
                statistics.setErStatusBgrColor("#71FF35");
                break;
            case TERMINATED:
                statistics.setErStatusBgrColor("lightgrey");
                break;
            default:
                statistics.setErStatusBgrColor("white");
                break;
        }
    }

    private void setStatisticsForLr(UUID testRunUuid, Statistics statistics) {
        List<LogRecord> logRecords = Collections.emptyList();
        try {
            logRecords = trService.getAllSectionNotCompoundLogRecords(testRunUuid);
        } catch (Exception e) {
            log.error("Unable get LRs", e);
        }
        if (!logRecords.isEmpty()) {
            statistics.setAllLr(statistics.getAllLr() + logRecords.size());
            for (LogRecord logRecord : logRecords) {
                switch (logRecord.getTestingStatus()) {
                    case FAILED:
                        statistics.addLrFailed();
                        break;
                    case PASSED:
                        statistics.addLrPassed();
                        break;
                    case WARNING:
                        statistics.addLrWarning();
                        break;
                    case BLOCKED:
                    case STOPPED:
                        statistics.addLrError();
                        break;
                    default:
                        break;
                }
            }
        } else {
            log.warn("Log Records were not found for Test Run {}, ER {}.", testRunUuid, executionRequest.getUuid());
        }
    }

    private StringBuilder prepareTestRunsTable(Map<String, StringBuilder> suiteParts) {
        StringBuilder res = new StringBuilder();
        suiteParts.forEach((suite, testruns) ->
                res.append("<tr><td colspan='6' style=\"padding: 2px\"><h5>Suite Name: ").append(suite)
                        .append("</h5></td></tr>")
                        .append(testruns));
        return res;
    }

    private StringBuilder createTableOfItems(Collection<String> items) {
        final StringBuilder builder = new StringBuilder();
        builder.append("<table>");
        items.forEach(item -> builder.append("<tr>").append("<td>").append(item).append("</td>").append("</tr>"));
        builder.append("</table>");
        return builder;
    }

    private MimeMessage createMimeMessage(String content) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = new MimeMessage(getSession());
        message.setFrom(new InternetAddress("atpnotification@some-domain",
                "RAMNotifications"));
        message.setRecipients(
                Message.RecipientType.TO, InternetAddress.parse(getRecipients()));
        String executionRequestName = executionRequest.getName();
        String mailSubject = reportParams.getSubject();
        String subject = mailSubject == null ? "ER: " + executionRequestName : mailSubject;
        message.setSubject(subject);
        MimeBodyPart mimeBodyPart = new MimeBodyPart();
        mimeBodyPart.setContent(content, "text/html");
        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(mimeBodyPart);
        message.setContent(multipart);
        return message;
    }

    private MailRequest buildRequest(String content) {
        MailRequest request = new MailRequest();
        String mailSubject = reportParams.getSubject();
        String executionRequestName = executionRequest.getName();
        String subject = mailSubject == null ? "ER: " + executionRequestName : mailSubject;
        request.setSubject(subject);
        request.setTo(getRecipients());
        request.setContent(content);
        request.setService(ATP_RAM.getValue());
        Map<String, Object> metadata = new HashMap<>();
        UUID executionRequestId = Objects.isNull(reportParams.getExecutionRequestUuid())
                ? executionRequest.getUuid() : reportParams.getExecutionRequestUuid();
        metadata.put(EXECUTION_REQUEST_ID.getValue(), executionRequestId);
        metadata.put(PROJECT_ID.getValue(), executionRequest.getProjectId());
        request.setMetadata(metadata);
        return request;
    }

    private String replaceTemplatePlaceHolders(StringBuilder msg, String qaHost, Statistics statistics,
                                               String solutionBuilds) {
        return template.replace("${RAM_ER_LINK}", printErLink(executionRequest))
                //.replace("${COMPARE_LINK}", printCompareLink(er))
                .replace("${CI_JOB_URL}", printCiJobUrl(executionRequest))
                .replace("${EXECUTION_RESULT_COLOR}", statistics.getErStatusBgrColor())
                .replace("${EXECUTION_RESULT}", executionRequest.getExecutionStatus().getName())
                .replace("${HEADER_ROOT_CAUSE}", printHeader(statistics))
                .replace("${TD_WITH_COUNT_AND_RATE}", printBody(true, statistics.getRootCauseAndCount(),
                        statistics))
                .replace("${TD_WITH_PREV_COUNT_AND_RATE}",
                        printBody(false, statistics.getRootCauseAndCountPrevEr(), statistics))
                .replace("${ER_NAME}", executionRequest.getName())
                .replace("${START_DATE}", convertTimeStampToDateString(executionRequest.getStartDate()))
                .replace("${FINISH_DATE}", convertTimeStampToDateString(executionRequest.getFinishDate()))
                .replace("${TR_COUNT}", String.valueOf(statistics.getAllTr()))
                .replace("${ACTIONS_COUNT}", String.valueOf(statistics.getAllLr()))
                .replace("${QA_HOST}", qaHost)
                .replace("${SOLUTION_BUILD}", solutionBuilds)
                .replace("${DURATION}", convertSecondToHhMmSsString(executionRequest.getDuration()))
                .replace("${TR_PASSED}", String.valueOf(statistics.getTrPassed()))
                .replace("${TR_FAILED}", String.valueOf(statistics.getTrFailed()))
                .replace("${TR_WARNING}", String.valueOf(statistics.getTrWarning()))
                .replace("${TR_ERROR}", String.valueOf(statistics.getTrError()))
                .replace("${ACTION_PASSED}", String.valueOf(statistics.getLrPassed()))
                .replace("${ACTION_FAILED}", String.valueOf(statistics.getLrFailed()))
                .replace("${ACTION_WARNING}", String.valueOf(statistics.getLrWarning()))
                .replace("${ACTION_ERROR}", String.valueOf(statistics.getLrError()))
                .replace("${FAIL_RATE_FORMULA}",
                        printRateFormula(statistics.getTrFailed(), statistics.getAllTr()))
                .replace("${PASS_RATE_FORMULA}",
                        printRateFormula(statistics.getTrPassed(), statistics.getAllTr()))
                .replace("${WARN_RATE_FORMULA}",
                        printRateFormula(statistics.getTrWarning(), statistics.getAllTr()))
                .replace("${ERROR_RATE_FORMULA}",
                        printRateFormula(statistics.getTrError(), statistics.getAllTr()))
                .replace("${ACTION_FAIL_RATE}",
                        printRateFormula(statistics.getLrFailed(), statistics.getAllLr()))
                .replace("${TEST_RUNS_ROWS}", msg.toString());
    }

    private String getRecipients() {
        TestPlan tp = testPlansService.findByTestPlanUuid(executionRequest.getTestPlanId());
        if (Objects.nonNull(tp) && Objects.nonNull(tp.getRecipients())) {
            String tpRecipients = tp.getRecipients().recipientsAsString();
            log.trace("Recipients for ER: {} from TestPlan: {}.", executionRequest.getUuid(), tpRecipients);
            return tpRecipients;
        }
        String recipientsFromAdapter = reportParams.getRecipients();
        log.trace("Recipients for ER: {} from adapter: {}.", executionRequest.getUuid(), recipientsFromAdapter);
        return recipientsFromAdapter;
    }


    private Session getSession() {
        Properties props = System.getProperties();
        props.setProperty("mail.smtp.host", mailSmtpHost);
        props.setProperty("mail.smtp.port", mailSmtpPort);
        props.setProperty("mail.smtps.auth", mailSmtpsAuth);
        props.setProperty("mail.smtp.ssl.enable", mailSmtpSslEnable);
        return Session.getDefaultInstance(props, null);
    }

    private String printErLink(ExecutionRequest er) {
        return "<a href='" + baseUrl + ApiPath.PROJECT_PATH + '/' + er.getProjectId()
                + ApiPath.REPORT_EXECUTION_REQUESTS_PATH + '/' + er.getUuid() + "'>" + er.getName() + "</a>";
    }

    /* Added link when it will be implemented on the UI

    private String printCompareLink(ExecutionRequests er) {
        return !Strings.isNullOrEmpty(er.getPreviousExecutionRequestId())
                ? "<a href='" + baseUrl + "/execution-requests/" + er.getTestPlanUuid() + "/compare?uuids="
                + er.getUuid() + ',' + er.getPreviousExecutionRequestId() + "'>" + er.getName() + "</a>"
                : "Previous Execution Request doesn't exist.";
    }*/

    private String printCiJobUrl(ExecutionRequest er) {
        String ciJob = !Strings.isNullOrEmpty(er.getCiJobUrl())
                ? er.getCiJobUrl().replaceFirst("\\[", "").replaceAll("\\]", "")
                .replaceAll("\"", "")
                : "";
        return !ciJob.isEmpty()
                ? "<h4><b>Ci job url: </b><a href='" + ciJob + "'>" + ciJob + "</a><br></h4>\n"
                : "";
    }

    private String printRateFormula(int value, int total) {
        return String.format(Locale.ROOT, "(" + value + "/" + total + ") * 100%% =  %.1f%%",
                ((double) value / total) * 100);
    }

    private String convertTimeStampToDateString(Timestamp date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
        if (date == null) {
            date = new Timestamp(System.currentTimeMillis());
        }
        return date.toLocalDateTime().format(formatter);
    }

    private String printTestRunLink(TestRun tr) {
        return "<a href='" + baseUrl + ApiPath.PROJECT_PATH + "/" + executionRequest.getProjectId()
                + ApiPath.REPORT_EXECUTION_REQUESTS_PATH + "/" + executionRequest.getUuid() + "?node=" + tr.getUuid()
                + "'>" + tr.getName() + "</a>";
    }

    private String printFailedLogRecords(TestRun testRun) {
        StringBuilder builder = new StringBuilder();
        List<LogRecord> allFailedLogRecords = trService.getAllFailedLogRecords(testRun.getUuid());
        if (!allFailedLogRecords.isEmpty()) {
            allFailedLogRecords.stream()
                    .filter(logRecord ->
                            logRecord.getParentRecordId() == null)
                    .forEach(logRecord -> {
                                builder.append("<b>").append("Step: ").append("</b>")
                                        .append("<a href='").append(baseUrl).append(ApiPath.PROJECT_PATH)
                                        .append("/").append(executionRequest.getProjectId())
                                        .append(ApiPath.REPORT_EXECUTION_REQUESTS_PATH).append("/")
                                        .append(executionRequest.getUuid()).append("?node=")
                                        .append(testRun.getUuid())
                                        .append("'>")
                                        .append(logRecord.getName())
                                        .append("</a>")
                                        .append("<br>");
                            }
                    );
        }
        return builder.toString();
    }

    private String convertSecondToHhMmSsString(long timeSeconds) {
        return LocalTime.MIN.plusSeconds(timeSeconds).toString();
    }

    private String getTemplate() {
        return MailConstants.HEADER_ER_REPORT
                + MailConstants.SUMMARY_TABLE_TEMPLATE
                + MailConstants.ROOT_CAUSES_STATISTICS
                + MailConstants.TR_TABLE_TEMPLATE;
    }

    private class Statistics {

        private String color;
        private String erStatusBgrColor;
        private String fontColor;
        private int allTr = 0;
        private int allPrevTr = 0;
        private String prevErName = "";
        private String prevErStartDate = "";
        private int allLr = 0;
        private int trPassed = 0;
        private int trFailed = 0;
        private int trWarning = 0;
        private int trError = 0;
        private int lrPassed = 0;
        private int lrFailed = 0;
        private int lrWarning = 0;
        private int lrError = 0;
        private Map<String, Integer> rootCauseAndCountPrevEr;
        private Map<String, Integer> rootCauseAndCount;
        private Set<String> rootCausesNames;

        String getColor() {
            return color;
        }

        void setColor(String color) {
            this.color = color;
        }

        String getErStatusBgrColor() {
            return erStatusBgrColor;
        }

        void setErStatusBgrColor(String erStatusBgrColor) {
            this.erStatusBgrColor = erStatusBgrColor;
        }

        String getFontColor() {
            return fontColor;
        }

        void setFontColor(String fontColor) {
            this.fontColor = fontColor;
        }

        int getAllTr() {
            return allTr;
        }

        void setAllTr(int allTr) {
            this.allTr = allTr;
        }

        int getAllLr() {
            return allLr;
        }

        void setAllLr(int allLr) {
            this.allLr = allLr;
        }

        int getTrPassed() {
            return trPassed;
        }

        void addTrPassed() {
            this.trPassed++;
        }

        int getTrFailed() {
            return trFailed;
        }

        void addTrFailed() {
            this.trFailed++;
        }

        int getTrWarning() {
            return trWarning;
        }

        void addTrWarning() {
            this.trWarning++;
        }

        int getTrError() {
            return trError;
        }

        void addTrError() {
            this.trError++;
        }

        int getLrPassed() {
            return lrPassed;
        }

        void addLrPassed() {
            this.lrPassed++;
        }

        int getLrFailed() {
            return lrFailed;
        }

        void addLrFailed() {
            this.lrFailed++;
        }

        int getLrWarning() {
            return lrWarning;
        }

        void addLrWarning() {
            this.lrWarning++;
        }

        int getLrError() {
            return lrError;
        }

        void addLrError() {
            this.lrError++;
        }

        Map<String, Integer> getRootCauseAndCountPrevEr() {
            return rootCauseAndCountPrevEr;
        }

        void setRootCauseAndCountPrevEr(Map<String, Integer> rootCauseAndCountPrevEr) {
            this.rootCauseAndCountPrevEr = rootCauseAndCountPrevEr;
        }

        Map<String, Integer> getRootCauseAndCount() {
            return rootCauseAndCount;
        }

        void setRootCauseAndCount(Map<String, Integer> rootCauseAndCount) {
            this.rootCauseAndCount = rootCauseAndCount;
        }

        int getAllPrevTr() {
            return allPrevTr;
        }

        void addAllPrevTr(int allPrevTr) {
            this.allPrevTr += allPrevTr;
        }

        void countAllTestRunsForPrevEr() {
            if (this.rootCauseAndCountPrevEr != null && !this.rootCauseAndCountPrevEr.isEmpty()) {
                this.rootCauseAndCountPrevEr.forEach((key, value) -> addAllPrevTr(value));
            }
        }

        void setParamsForPrevEr(ExecutionRequest prevEr) {
            if (prevEr != null) {
                this.prevErStartDate = convertTimeStampToDateString(prevEr.getStartDate());
                this.prevErName = prevEr.getName();
            }
        }

        String getPrevErName() {
            return prevErName;
        }

        String getPrevErStartDate() {
            return prevErStartDate;
        }

        Set<String> getRootCausesNames() {
            return rootCausesNames;
        }

        void setRootCausesNames(Set<String> rootCausesNames) {
            this.rootCausesNames = rootCausesNames;
        }
    }
}
