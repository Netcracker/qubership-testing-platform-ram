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

package org.qubership.atp.ram.job;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.qubership.atp.common.lock.LockManager;
import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.Project;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.service.mail.MailService;
import org.qubership.atp.ram.service.mail.ReportParams;
import org.qubership.atp.ram.services.CatalogueService;
import org.qubership.atp.ram.services.EnvironmentsInfoService;
import org.qubership.atp.ram.services.ExecutionRequestDetailsService;
import org.qubership.atp.ram.services.ExecutionRequestReportingService;
import org.qubership.atp.ram.services.ExecutionRequestService;
import org.qubership.atp.ram.services.IssueService;
import org.qubership.atp.ram.services.ProjectsService;
import org.qubership.atp.ram.services.TestRunService;
import org.qubership.atp.ram.services.WidgetConfigTemplateService;
import org.qubership.atp.ram.tsg.service.TsgService;
import org.qubership.atp.ram.utils.StreamUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.gson.JsonArray;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ExecutionRequestScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(ExecutionRequestScheduler.class);
    private final TestRunService testRunService;
    private final TsgService tsgService;
    private final ExecutionRequestService executionRequestService;
    private final MailService mailService;
    private final CatalogueService catalogueService;
    private final LockManager lockManager;
    private final ProjectsService projectsService;
    private final EnvironmentsInfoService environmentsInfoService;
    private final WidgetConfigTemplateService widgetConfigTemplateService;
    private final ExecutionRequestDetailsService executionRequestDetailsService;
    private final ExecutionRequestReportingService executionRequestReportingService;
    private final IssueService issueService;

    @Value("${timeout.after.finish.date.of.last.tr.ms}")
    private long timeoutAfterFinishDateOfLastTrMs;
    @Value("${timeout.after.start.date.of.er.hours}")
    private long timeoutAfterStartDateOfErHours;
    @Value("${atp1.integration.enable}")
    private String atp1IntegrationEnable;
    @Value("${atp.expired.execution.requests.batch.size}")
    private int expiredErsBatchSize;

    /**
     * Constructor of job for stop ER.
     */
    @Autowired
    public ExecutionRequestScheduler(TestRunService testRunService,
                                     ExecutionRequestService executionRequestService,
                                     MailService mailService,
                                     CatalogueService catalogueService,
                                     TsgService tsgService,
                                     LockManager lockManager,
                                     ProjectsService projectsService,
                                     EnvironmentsInfoService environmentsInfoService,
                                     WidgetConfigTemplateService widgetConfigTemplateService,
                                     ExecutionRequestDetailsService executionRequestDetailsService,
                                     ExecutionRequestReportingService executionRequestReportingService,
                                     IssueService issueService) {
        this.testRunService = testRunService;
        this.executionRequestService = executionRequestService;
        this.mailService = mailService;
        this.catalogueService = catalogueService;
        this.tsgService = tsgService;
        this.lockManager = lockManager;
        this.projectsService = projectsService;
        this.environmentsInfoService = environmentsInfoService;
        this.widgetConfigTemplateService = widgetConfigTemplateService;
        this.executionRequestDetailsService = executionRequestDetailsService;
        this.executionRequestReportingService = executionRequestReportingService;
        this.issueService = issueService;
    }

    /**
     * Clean collections:
     * 1. environmnets_info
     * 2. executionRequestConfig
     * 3. executionRequestDetails
     * 4. executionRequestReporting
     * 5. executionRequests
     * 6. issue
     * 7. tools.
     */
    @Scheduled(cron = "${execution.request.cleanup.job.cron}")
    public boolean cleanupExecutionRequestAndData() {
        return lockManager.executeWithLockNoWait("cleanup expired ER",
                () -> {
                    long executionTime = System.currentTimeMillis();
                    log.info("Start cleanup expired ER: {}", new Date(executionTime));
                    AtomicInteger deletedCount = new AtomicInteger();
                    List<Project> listProject = projectsService.getAllProjects();

                    listProject.forEach(project -> {
                        long expirationPeriod = getTimeHowLongByInt(project
                                .getExecutionRequestsExpirationPeriodWeeks());
                        long period = System.currentTimeMillis() - expirationPeriod;
                        Timestamp timestamp = new Timestamp(period);
                        List<ExecutionRequest> expiredExecutionRequests = executionRequestService
                                .findExpireExecutionRequest(timestamp, project.getUuid());
                        if (!expiredExecutionRequests.isEmpty()) {
                            List<UUID> expiredExecutionRequestIds = expiredExecutionRequests.stream()
                                    .map(ExecutionRequest::getUuid).collect(Collectors.toList());
                            deletedCount.addAndGet(expiredExecutionRequestIds.size());
                            log.info("Expired period: {} , by project {}", timestamp, project.getUuid());
                            log.debug("All expired execution requests ids: {} by project {}",
                                    expiredExecutionRequestIds, project.getUuid());
                            Iterable<List<UUID>> partitions = Iterables
                                    .partition(expiredExecutionRequestIds, expiredErsBatchSize);
                            List<UUID> firstBatch = partitions.iterator().next();
                            environmentsInfoService.deleteAllEnvironmentsInfoByExecutionRequestId(firstBatch);
                            widgetConfigTemplateService.deleteAllByExecutionRequestIdIn(firstBatch);
                            executionRequestDetailsService.deleteAllByExecutionRequestDetailsIdIn(firstBatch);
                            executionRequestReportingService.deleteAllByExecutionRequestDetailsIdIn(firstBatch);
                            issueService.deleteAllIssueByExecutionRequestIds(firstBatch);
                            environmentsInfoService.deleteAllToolsByExecutionRequestId(firstBatch);
                            executionRequestService.deleteAllExecutionRequestByExecutionRequestId(firstBatch);
                        }
                    });
                    executionTime = (System.currentTimeMillis() - executionTime) / 60000;

                    log.info("Delete {} counts data of old execution requests. Has deleted by {} minutes",
                            deletedCount, executionTime);
                    return true;
                }, () -> {
                    return false;
                });
    }

    private long getTimeHowLongByInt(int weekPeriod) {
        return 1000L * 60 * 60 * 24 * 7 * weekPeriod;
    }

    /**
     * Update not finished request and send report.
     */
    @Scheduled(fixedDelayString = "${fixedRate.er.in.milliseconds}")
    public void finishedRequestAndSendReport() {
        lockManager.executeWithLock("finishedRequestAndSendReport", () -> {
            //list contains objects with filled only requestUuid field
            LOG.info("Start stopping running requests and sending reports.");
            List<ExecutionRequest> inProgressRequestsIds = executionRequestService.getNotFinishedRequests();
            inProgressRequestsIds.parallelStream().forEach(request -> {
                UUID erId = request.getUuid();
                long countInProgressTestRuns = testRunService.countAllByExecutionRequestIdAndExecutionStatusIn(erId,
                        Collections.singletonList(ExecutionStatuses.IN_PROGRESS));
                long countNotStartedTestRuns = testRunService.countAllByExecutionRequestIdAndExecutionStatusIn(erId,
                        Collections.singletonList(ExecutionStatuses.NOT_STARTED));
                if (countInProgressTestRuns > 0) {
                    LOG.debug("Execution Request:{} has TestRuns with status IN_PROGRESS.", erId);
                } else {
                    List<TestRun> testRuns = testRunService.findTestCaseIdsByExecutionRequestId(erId);
                    Set<UUID> testCases = StreamUtils.extractIds(testRuns, TestRun::getTestCaseId);
                    updateRequestAndSendReport(request.getUuid(),
                            testCases.isEmpty() || countInProgressTestRuns == 0 && countNotStartedTestRuns > 0);
                }
            });

            LOG.info("Finish stopping running requests and sending reports.");
        });
    }

    private void updateRequestAndSendReport(UUID uuid, boolean inProgressTestRunsIsNotExist) {
        LOG.debug("Start update ER {}.", uuid);
        try {
            long lastFinishDate = testRunService.getFinishDateOfLastTestRun(uuid);
            long currentTime = System.currentTimeMillis();
            long differenceBetweenCurrentTimeAndLastFinishDate = currentTime - lastFinishDate;

            ExecutionRequest request = executionRequestService.findById(uuid);

            if (ExecutionStatuses.IN_PROGRESS.equals(request.getExecutionStatus())
                    && differenceBetweenCurrentTimeAndLastFinishDate < timeoutAfterFinishDateOfLastTrMs) {
                LOG.debug("The timeout hasn't come for ER = {}, finish date of last Test Run {}",
                        uuid, lastFinishDate);
                return;
            }

            long startErDate = request.getStartDate().getTime();
            long differenceBetweenCurrentTimeAndStartDate = currentTime - startErDate;
            if (inProgressTestRunsIsNotExist) {
                if (differenceBetweenCurrentTimeAndStartDate
                        > TimeUnit.HOURS.toMillis(timeoutAfterStartDateOfErHours)) {
                    LOG.info("ER = {} with start date = {} was terminated by timeout. Test Runs not found.", uuid,
                            startErDate);
                    request.setExecutionStatus(ExecutionStatuses.TERMINATED_BY_TIMEOUT);
                } else {
                    LOG.info("The timeout hasn't come for ER = {} with start date = {}. Test Runs not found.",
                            uuid, startErDate);
                    return;
                }
            }
            executionRequestService.stopExecutionRequest(request, lastFinishDate == 0 ? currentTime : lastFinishDate);

            tsgService.sendFdrs(request);
            if (!Strings.isNullOrEmpty(request.getLegacyMailRecipients())) {
                ReportParams reportParams = new ReportParams();
                reportParams.setExecutionRequestUuid(request.getUuid());
                reportParams.setRecipients(request.getLegacyMailRecipients());
                mailService.sendFromTemplate(reportParams, request);
            }

            LOG.info("Execution Request {} successfully processed.", uuid);
        } catch (Exception e) {
            LOG.error("Can not complete the update ER {} and sending the report.", uuid, e);
        }
    }

    /**
     * Returns list supplemented by servers from JsonArray.
     *
     * @param hostsOrBuildsExisting List to be added.
     * @param hostsOrBuildsExternal Array derived from json.
     * @return Updated list.
     */
    public static List<String> getHostsOrBuilds(List<String> hostsOrBuildsExisting, JsonArray hostsOrBuildsExternal,
                                                String paramName, String testRunId) {
        final List<String> hostsOrBuildsInTr = hostsOrBuildsExisting == null ? new ArrayList<>() :
                hostsOrBuildsExisting;
        if (hostsOrBuildsExternal != null && !hostsOrBuildsExternal.isEmpty()) {
            hostsOrBuildsExternal.forEach(hostOrBuild -> {
                if (!hostsOrBuildsInTr.contains(hostOrBuild.toString())) {
                    hostsOrBuildsInTr.add(hostOrBuild.toString());
                }
            });
        } else {
            LOG.debug("Parameter {} for Test Run {} is empty.", paramName, testRunId);
        }
        return hostsOrBuildsInTr;
    }
}
