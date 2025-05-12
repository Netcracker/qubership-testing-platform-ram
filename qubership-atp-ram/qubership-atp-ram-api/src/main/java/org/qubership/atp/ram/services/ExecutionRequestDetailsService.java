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
import static org.qubership.atp.ram.enums.MailMetadata.ATP_RAM;
import static org.qubership.atp.ram.enums.MailMetadata.EXECUTION_REQUEST_ID;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.qubership.atp.integration.configuration.model.KafkaMailResponse;
import org.qubership.atp.integration.configuration.model.KafkaMailResponseStatus;
import org.qubership.atp.integration.configuration.model.MailResponse;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.models.ExecutionRequestDetails;
import org.qubership.atp.ram.repositories.ExecutionRequestDetailsRepository;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExecutionRequestDetailsService extends CrudService<ExecutionRequestDetails> {

    private static final String DETAILS_SECTION_REPORTING = "Details";

    private final ObjectMapper objectMapper;

    private final ExecutionRequestDetailsRepository detailsRepository;
    private final ExecutionRequestReportingService reportingService;

    @Override
    protected MongoRepository<ExecutionRequestDetails, UUID> repository() {
        return detailsRepository;
    }

    /**
     * Create details section.
     *
     * @param executionRequestId ID of execution request
     * @param details            creating data
     * @return created objects
     */
    public ExecutionRequestDetails createDetails(UUID executionRequestId, ExecutionRequestDetails details) {
        log.debug("Create details for execution request with id '{}'", executionRequestId);
        details.setUuid(UUID.randomUUID());
        details.setName(DETAILS_SECTION_REPORTING);
        details.setExecutionRequestId(executionRequestId);
        ExecutionRequestDetails savedDetails = detailsRepository.save(details);
        log.debug("Details for execution request successfully created");
        return savedDetails;
    }

    /**
     * Create details section.
     *
     * @param executionRequestId ID of execution request
     * @param testingStatuses    testing status
     * @param msg                details message
     * @return created objects
     */
    public ExecutionRequestDetails createDetails(UUID executionRequestId, TestingStatuses testingStatuses, String msg) {
        log.debug("Create details for execution request with id '{}'", executionRequestId);
        ExecutionRequestDetails details = new ExecutionRequestDetails();
        details.setUuid(UUID.randomUUID());
        details.setName(DETAILS_SECTION_REPORTING);
        details.setExecutionRequestId(executionRequestId);
        details.setStatus(testingStatuses);
        details.setMessage(msg);
        details.setDate(new Date());
        ExecutionRequestDetails savedDetails = detailsRepository.save(details);
        log.debug("Details for execution request successfully created");
        return savedDetails;
    }

    /**
     * Create details section with failed status.
     *
     * @param executionRequestId ID of execution request
     * @param e                  occurred exception which used to get message
     * @return created objects
     */
    public ExecutionRequestDetails createFailedDetails(UUID executionRequestId, Exception e) {
        log.debug("Create details for execution request with id '{}'", executionRequestId);
        ExecutionRequestDetails details = new ExecutionRequestDetails();
        details.setUuid(UUID.randomUUID());
        details.setName(DETAILS_SECTION_REPORTING);
        details.setExecutionRequestId(executionRequestId);
        details.setStatus(TestingStatuses.FAILED);
        details.setMessage(generateErrorMsg(e.getMessage(), Throwables.getStackTraceAsString(e)));
        details.setDate(new Date());
        ExecutionRequestDetails savedDetails = detailsRepository.save(details);
        log.debug("Details for execution request successfully created");
        return savedDetails;
    }

    public List<ExecutionRequestDetails> getDetails(UUID executionRequestId) {
        return detailsRepository.findAllByExecutionRequestId(executionRequestId);
    }

    /**
     * Update details section.
     *
     * @param executionRequestId ID of execution request
     * @param newDataDetails     new data
     * @return updated object or null
     */
    public ExecutionRequestDetails updateDetailsStatus(UUID executionRequestId,
                                                       ExecutionRequestDetails newDataDetails) {
        ExecutionRequestDetails details =
                detailsRepository.findByExecutionRequestId(executionRequestId);
        if (nonNull(details)) {
            log.debug("Update details section for ER {}, new data {}", executionRequestId, newDataDetails);
            details.setStatus(newDataDetails.getStatus());
            details.setMessage(newDataDetails.getMessage());
            return detailsRepository.save(details);
        } else {
            log.warn("Details section for ER {} is null", executionRequestId);
            return null;
        }
    }

    /**
     * Add male responses details.
     *
     * @param response response entity captured from kafka logs
     */
    public void addMailResponseDetails(String response) throws IOException {
        KafkaMailResponse mailResponse = objectMapper.readValue(response, KafkaMailResponse.class);
        saveMessageResponseDetails(mailResponse);
    }

    /**
     * Save message response details to the Data Base.
     *
     * @param mailResponse mail response entity
     */
    private void saveMessageResponseDetails(KafkaMailResponse mailResponse) {
        if (ATP_RAM.getValue().equals(mailResponse.getService())) {
            Map<String, Object> metadata = mailResponse.getMetadata();
            if (nonNull(metadata) && !metadata.isEmpty()) {
                String executionRequestIdValue = String.valueOf(metadata.get(EXECUTION_REQUEST_ID.getValue()));
                if (!Strings.isNullOrEmpty(executionRequestIdValue)) {
                    UUID executionRequestId = UUID.fromString(executionRequestIdValue);
                    log.debug("Create details for mail response for id '{}'", executionRequestId);
                    ExecutionRequestDetails details = new ExecutionRequestDetails();
                    details.setUuid(UUID.randomUUID());
                    details.setName(DETAILS_SECTION_REPORTING);
                    details.setExecutionRequestId(executionRequestId);
                    TestingStatuses testingStatuses = mailResponse.getStatus() == KafkaMailResponseStatus.ERROR
                            ? TestingStatuses.FAILED : TestingStatuses.PASSED;
                    details.setStatus(testingStatuses);
                    reportingService.updateReportingStatus(executionRequestId, testingStatuses);
                    String message = mailResponse.getMessage();
                    String stackTrace = mailResponse.getStacktrace();
                    message = setMailResponseMessage(message, stackTrace);
                    details.setMessage(message);
                    details.setDate(new Date());
                    detailsRepository.save(details);
                    log.debug("Details for mail response successfully created");
                }
            }
        }
    }

    /**
     * Save message response details to the Data Base.
     *
     * @param mailResponse mail response entity
     */
    public void saveMessageResponseDetails(MailResponse mailResponse, UUID executionRequestId) {
        log.debug("Create details for mail response for id '{}'", executionRequestId);
        ExecutionRequestDetails details = new ExecutionRequestDetails();
        details.setUuid(UUID.randomUUID());
        details.setName(DETAILS_SECTION_REPORTING);
        details.setExecutionRequestId(executionRequestId);
        int httpStatus = mailResponse.getStatus();
        TestingStatuses testingStatuses = httpStatus >= 300 ? TestingStatuses.FAILED : TestingStatuses.PASSED;
        details.setStatus(testingStatuses);
        String message = mailResponse.getMessage();
        String stackTrace = mailResponse.getTrace();
        message = setMailResponseMessage(message, stackTrace);
        details.setMessage(message);
        details.setDate(new Date());
        detailsRepository.save(details);
        log.debug("Details for mail response successfully created");
    }

    /**
     * Deleted ExecutionRequestConfig.
     */
    public void deleteAllByExecutionRequestDetailsIdIn(List<UUID> executionRequestIds) {
        detailsRepository.deleteAllByExecutionRequestIdIn(executionRequestIds);
    }

    private String setMailResponseMessage(String message, String stackTrace) {
        if (nonNull(stackTrace) && !stackTrace.isEmpty()) {
            return generateErrorMsg(message, stackTrace);
        }
        return message;
    }

    private String generateErrorMsg(String message, String stackTrace) {
        return String.format("%s\n\nRoot Cause of the Error:\n%s", message, stackTrace);
    }
}
