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

package org.qubership.atp.ram.services;

import static java.util.Objects.nonNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.models.ExecutionRequestReporting;
import org.qubership.atp.ram.repositories.ExecutionRequestReportingRepository;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExecutionRequestReportingService extends CrudService<ExecutionRequestReporting> {

    private static final String E_MAIL_REPORTING = "E-mail Reporting";

    private final ExecutionRequestReportingRepository reportingRepository;

    @Override
    protected MongoRepository<ExecutionRequestReporting, UUID> repository() {
        return reportingRepository;
    }

    /**
     * Create execution request reporting.
     *
     * @param executionRequestId execution request identifier
     * @param reporting          created reporting
     * @return reporting
     */
    public ExecutionRequestReporting createReporting(UUID executionRequestId, ExecutionRequestReporting reporting) {
        log.debug("Create email reporting for execution request with id '{}'", reporting.getExecutionRequestId());
        reporting.setUuid(UUID.randomUUID());
        reporting.setName(E_MAIL_REPORTING);
        reporting.setExecutionRequestId(executionRequestId);
        ExecutionRequestReporting savedReporting = reportingRepository.save(reporting);
        log.debug("Email reporting for execution request successfully created");
        return savedReporting;
    }

    /**
     * Get execution request reporting.
     *
     * @param executionRequestId execution request identifier
     * @return reporting
     */
    public ExecutionRequestReporting getReporting(UUID executionRequestId) {
        return reportingRepository.findByExecutionRequestId(executionRequestId);
    }

    /**
     * Get execution request reportings.
     *
     * @param executionRequestIds execution request identifiers
     * @return reportings
     */
    public List<ExecutionRequestReporting> getEmailReportings(Collection<UUID> executionRequestIds) {
        return reportingRepository.findByExecutionRequestIdIn(executionRequestIds);
    }

    /**
     * Get overall email recipients for provided execution requests.
     *
     * @param executionRequestIds execution request identifiers
     * @return set of emails
     */
    public Set<String> getEmailRecipients(Collection<UUID> executionRequestIds) {
        final List<ExecutionRequestReporting> reportings = getEmailReportings(executionRequestIds);
        if (!CollectionUtils.isEmpty(reportings)) {
            return reportings.stream()
                    .flatMap(emailReporting -> emailReporting.getRecipients().stream())
                    .collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    /**
     * Update testing status of {@link ExecutionRequestReporting} and return updated object
     * or return null if objects wasn't found.
     *
     * @param executionRequestId for find {@link ExecutionRequestReporting}
     * @param testingStatuses    new value of status
     * @return updated object or null
     */
    public ExecutionRequestReporting updateReportingStatus(UUID executionRequestId, TestingStatuses testingStatuses) {
        ExecutionRequestReporting executionRequestReporting =
                reportingRepository.findByExecutionRequestId(executionRequestId);
        if (nonNull(executionRequestReporting)) {
            log.debug("Update reporting section for ER {}, new status {}", executionRequestId, testingStatuses);
            executionRequestReporting.setStatus(testingStatuses);
            return reportingRepository.save(executionRequestReporting);
        } else {
            log.warn("Reporting section for ER {} is null", executionRequestId);
            return null;
        }
    }

    private List<String> getRecipientsFromString(String recipients) {
        return Arrays.asList(recipients.split(","));
    }

    /**
     * Update reporting information and return updated object
     * or return null if objects wasn't found.
     *
     * @param executionRequestId for find {@link ExecutionRequestReporting}
     * @param subject            email subject
     * @param recipients         email recipients
     * @return updated object or null
     */
    public ExecutionRequestReporting updateReportingInfo(UUID executionRequestId,
                                                         String subject,
                                                         String recipients) {
        ExecutionRequestReporting executionRequestReporting =
                reportingRepository.findByExecutionRequestId(executionRequestId);
        if (nonNull(executionRequestReporting)) {
            executionRequestReporting.setSubject(subject);
            executionRequestReporting.setRecipients(getRecipientsFromString(recipients));
            log.debug("Update reporting section for ER {}, new subject {}, new recipients {}", executionRequestId,
                    subject, recipients);
            return reportingRepository.save(executionRequestReporting);
        } else {
            log.warn("Reporting section for ER {} is null", executionRequestId);
            return null;
        }
    }

    /**
     * Deleted ExecutionRequestReporting.
     */
    public void deleteAllByExecutionRequestDetailsIdIn(List<UUID> executionRequestIds) {
        reportingRepository.deleteAllByExecutionRequestIdIn(executionRequestIds);
    }
}
