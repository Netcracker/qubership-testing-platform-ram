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

package org.qubership.atp.ram.logging.services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.modelmapper.ModelMapper;
import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.enums.TypeAction;
import org.qubership.atp.ram.logging.constants.ApiPathLogging;
import org.qubership.atp.ram.logging.entities.requests.CreatedLogRecordRequest;
import org.qubership.atp.ram.logging.entities.requests.UpdateLogRecordStatusAndResponseRequest;
import org.qubership.atp.ram.logging.entities.responses.CreatedLogRecordResponse;
import org.qubership.atp.ram.logging.entities.responses.UploadScreenshotResponse;
import org.qubership.atp.ram.logging.utils.ImageUtils;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.models.logrecords.RestLogRecord;
import org.qubership.atp.ram.repositories.ExecutionRequestRepository;
import org.qubership.atp.ram.repositories.LogRecordRepository;
import org.qubership.atp.ram.repositories.TestRunRepository;
import org.qubership.atp.ram.services.GridFsService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogRecordLoggingService {
    private static final String CONTENT_TYPE = "image/png";
    private final GridFsService gridFsService;
    private final LogRecordRepository logRecordRepository;
    private final TestRunRepository testRunRepository;
    private final ExecutionRequestRepository executionRequestRepository;
    @Qualifier(ApiPathLogging.MAPPER_FOR_LOGGING_BEAN_NAME)
    private final ModelMapper modelMapper;

    /**
     * Upload screenshot.
     */
    public UploadScreenshotResponse upload(String contentType, UUID id, InputStream inputStream,
                                           String fileName, String snapshotSource) {
        log.trace("Start upload file for LR {}", id);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            IOUtils.copy(inputStream, byteArrayOutputStream);
            byte[] bytesImage = byteArrayOutputStream.toByteArray();
            String screenId = gridFsService.save(ApiPathLogging.CONTENT_TYPE_IMAGE, LocalDateTime.now().toString(),
                    contentType, id, new ByteArrayInputStream(bytesImage), fileName, snapshotSource);
            String base64Preview = null;
            if (Objects.isNull(contentType) || contentType.equals(CONTENT_TYPE)) {
                base64Preview = ImageUtils.scaleAndConvertImageToBase64(new ByteArrayInputStream(bytesImage), id);
            }
            return new UploadScreenshotResponse(screenId, base64Preview);
        } catch (IOException ex) {
            log.error("Failed to save file for Log Record: [{}]", id, ex);
            return new UploadScreenshotResponse();
        }
    }

    /**
     * Updates testing status message,request and response.
     */
    public CreatedLogRecordResponse updateTestingStatusMessageAndRequestResponse(UUID logRecordId,
                                                               UpdateLogRecordStatusAndResponseRequest request) {
        log.debug("Start update LR {}", logRecordId);
        LogRecord logRecord = logRecordRepository.findByUuid(logRecordId);
        logRecord.setMessage(request.getMessage());
        logRecord.setTestingStatus(request.getTestingStatus());
        TypeAction type = logRecord.getType();
        if (type.equals(TypeAction.MIA) || type.equals(TypeAction.ITF) || type.equals(TypeAction.REST)) {
            ((RestLogRecord) logRecord).setRequest(request.getRequest());
            ((RestLogRecord) logRecord).setResponse(request.getResponse());
        }
        logRecordRepository.save(logRecord);
        log.debug("LR {} updated. Testing status [{}], message [{}].", logRecordId, request.getTestingStatus(),
                request.getMessage());
        return new CreatedLogRecordResponse(logRecord.getUuid());
    }

    void saveCountScreenshots(UUID executionRequestUuid, List<TestRun> testRuns) {
        if (!testRuns.isEmpty()) {
            int numberOfScreen = 0;
            Map<UUID, String> trIdAndNumberOfScreens = new HashMap<>();
            for (TestRun testRun : testRuns) {
                UUID uuid = testRun.getUuid();
                try {
                    List<LogRecord> logRecords = logRecordRepository.findAllUuidByTestRunId(uuid);
                    if (!logRecords.isEmpty()) {
                        numberOfScreen = gridFsService.getCountScreen(logRecords);
                    }
                    trIdAndNumberOfScreens.put(uuid, String.valueOf(numberOfScreen));
                    testRun.setNumberOfScreens(numberOfScreen);
                } catch (Exception e) {
                    log.error("Error in calculating screenshots count for Test Run {}.", uuid, e);
                }
            }
            testRunRepository.saveAll(testRuns);
            log.debug("Number of screens for ER {}: {}", executionRequestUuid, trIdAndNumberOfScreens);
        }
    }

    /**
     * Find or create log record.
     */
    public CreatedLogRecordResponse findOrCreate(CreatedLogRecordRequest createdLogRecordRequest) {
        log.trace("Start of search (or creating - if the Log Record was not found) by request:\n{}",
                createdLogRecordRequest);
        LogRecord logRecord = findByRequestOrCreate(createdLogRecordRequest);
        TestRun testRun = testRunRepository.findByUuid(logRecord.getTestRunId());

        updateExecutionStatusOfTestAndExecutionRequest(testRun);
        log.trace("Finish updating Log Record {} for TR {}, ER {}", logRecord.getUuid(),
                testRun.getUuid(), testRun.getExecutionRequestId());
        CreatedLogRecordResponse createdLogRecordResponse = new CreatedLogRecordResponse();
        createdLogRecordResponse.setLogRecordUuid(logRecord.getUuid());

        return createdLogRecordResponse;
    }

    private void updateExecutionStatusOfTestAndExecutionRequest(TestRun testRun) {
        if (ExecutionStatuses.TERMINATED.equals(testRun.getExecutionStatus())) {
            log.warn("TestRun [{}] is already terminated", testRun.getUuid());
        } else {
            testRun.setExecutionStatus(ExecutionStatuses.IN_PROGRESS);
            testRunRepository.save(testRun);

            ExecutionRequest executionRequest =
                    executionRequestRepository.findByUuid(testRun.getExecutionRequestId());
            executionRequest.setExecutionStatus(ExecutionStatuses.IN_PROGRESS);
            executionRequestRepository.save(executionRequest);

            log.debug("Set In Progress status for TR {} and ER {}.", testRun.getUuid(),
                    testRun.getExecutionRequestId());
        }
    }

    LogRecord findByRequestOrCreate(CreatedLogRecordRequest createdLogRecordRequest) {
        LogRecord logRecord = logRecordRepository.findByUuid(createdLogRecordRequest.getLogRecordUuid());
        if (Objects.isNull(logRecord)) {
            LogRecord logRecordRequest = modelMapper.map(createdLogRecordRequest, LogRecord.class);
            logRecordRequest.setCreatedDate(new Timestamp(System.currentTimeMillis()));
            return logRecordRepository.save(logRecordRequest);
        } else {
            return logRecord;
        }
    }


}
