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

package org.qubership.atp.ram.service.rest.server.executor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.BooleanUtils;
import org.qubership.atp.ram.config.ApiPath;
import org.qubership.atp.ram.dto.request.UpdateLogRecordFields;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.enums.TypeAction;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.logrecords.RestLogRecord;
import org.qubership.atp.ram.service.rest.dto.UploadFileResponse;
import org.qubership.atp.ram.service.rest.server.executor.request.LogRecordRequest;
import org.qubership.atp.ram.service.rest.server.executor.request.LogRecordResponse;
import org.qubership.atp.ram.services.ExecutionRequestService;
import org.qubership.atp.ram.services.GridFsService;
import org.qubership.atp.ram.services.LogRecordService;
import org.qubership.atp.ram.utils.ImageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * CRUD Log Records.
 *
 * @deprecated use LogRecordLoggingController instead of this.
 */
@RestController("ExecutorLogRecordsController")
@RequestMapping(ApiPath.API_PATH + ApiPath.EXECUTOR_PATH + ApiPath.LOG_RECORDS_PATH)
@Deprecated
public class LogRecordsController /*implements LogRecordsControllerApi*/ {

    private static final Logger log = LoggerFactory.getLogger(LogRecordsController.class);
    private static final String IMAGE_PNG_CONTENT_TYPE = "image/png";

    private final GridFsService gridFsService;
    private final LogRecordService logRecordService;
    private final ExecutionRequestService executionRequestService;

    /**
     * Inject services.
     */
    @Autowired
    public LogRecordsController(GridFsService gridFsService,
                                LogRecordService logRecordService,
                                ExecutionRequestService executionRequestService) {
        this.gridFsService = gridFsService;
        this.logRecordService = logRecordService;
        this.executionRequestService = executionRequestService;
    }

    /**
     * Find or create logRecord.
     *
     * @deprecated use LogRecordLoggingController instead of this.
     */
    @PostMapping(ApiPath.FIND_OR_CREATE_PATH)
    @Deprecated
    public LogRecordResponse findOrCreate(@RequestBody LogRecordRequest request) {
        LogRecord lr = getLogRecordByRequest(request);
        LogRecord logRecord = logRecordService.findOrCreate(lr);
        LogRecordResponse response = new LogRecordResponse();
        response.setLogRecordId(logRecord.getUuid());
        return response;
    }

    private LogRecord getLogRecordByRequest(LogRecordRequest request) {
        LogRecord logRecord = new LogRecord();
        logRecord.setTestRunId(request.getTestRunId());
        logRecord.setUuid(request.getLogRecordId());
        logRecord.setTestingStatus(TestingStatuses.findByValue(request.getTestingStatus()));
        logRecord.setParentRecordId(request.getParentRecordId());
        logRecord.setName(request.getName());
        logRecord.setMessage(request.getMessage());
        logRecord.setSection(BooleanUtils.toBoolean(request.getIsSection()));
        logRecord.setCompaund(BooleanUtils.toBoolean(request.getIsCompound()));
        logRecord.setStartDate(request.getStartDate());
        logRecord.setEndDate(request.getFinishDate());
        logRecord.setMetaInfo(request.getMetaInfo());
        logRecord.setConfigInfoId(request.getConfigInfo());
        return logRecord;
    }

    /**
     * Store attachment to GridFs.
     *
     * @deprecated use LogRecordLoggingController instead of this.
     */
    @Deprecated
    @PostMapping(ApiPath.UPLOAD_PATH + "/{logRecordUuid}" + ApiPath.STREAM_PATH)
    public UploadFileResponse upload(@PathVariable("logRecordUuid") UUID logRecordUuid,
                                     @RequestParam("contentType") String contentType,
                                     @RequestParam("fileName") String fileName,
                                     @RequestParam("snapshotSource") String snapshotSource,
                                     InputStream inputStream) {
        log.debug("Upload file {} for Log Record {} with content type {}, snapshot source {}.",
                fileName, logRecordUuid, contentType, snapshotSource);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            IOUtils.copy(inputStream, byteArrayOutputStream);
            byte[] bytesImage = byteArrayOutputStream.toByteArray();
            String fileId = gridFsService.save(contentType, LocalDateTime.now().toString(), contentType,
                    logRecordUuid, new ByteArrayInputStream(bytesImage), fileName, snapshotSource);
            String base64Preview = null;
            if (Objects.isNull(contentType) || contentType.equals(IMAGE_PNG_CONTENT_TYPE)) {
                base64Preview = ImageUtils.scaleAndConvertImageToBase64(new ByteArrayInputStream(bytesImage),
                        logRecordUuid);
            }
            return new UploadFileResponse(fileId, base64Preview);
        } catch (IOException ex) {
            log.error("Failed to save file for Log Record: [{}]", logRecordUuid, ex);
            return new UploadFileResponse();
        }
    }

    /**
     * Update Log Records (REST, MIA, ITF) status, message, request and response.
     *
     * @param logRecordId id of log record
     * @param request     content
     * @return id of updated log record
     */
    @PostMapping(ApiPath.UUID_PATH + ApiPath.UPDATE_PATH)
    public LogRecordResponse updateTestingStatusMessageAndRequestResponse(@PathVariable(ApiPath.UUID) UUID logRecordId,
                                                                          @RequestBody UpdateLogRecordFields request) {
        log.debug("Start update LR {}", logRecordId);
        LogRecord logRecord = logRecordService.get(logRecordId);
        logRecord.setMessage(request.getMessage());
        logRecord.setTestingStatus(request.getTestingStatus());
        TypeAction type = logRecord.getType();
        if (request.getFiles() != null && !request.getFiles().isEmpty()) {
            logRecord.setFileMetadata(request.getFiles());
        } else if ((Objects.nonNull(request.getRequest()) || Objects.nonNull(request.getResponse()))
                && (type.equals(TypeAction.MIA) || type.equals(TypeAction.ITF) || type.equals(TypeAction.REST))) {
            ((RestLogRecord) logRecord).setRequest(request.getRequest());
            ((RestLogRecord) logRecord).setResponse(request.getResponse());
        }
        logRecordService.save(logRecord);
        log.debug("LR {} updated. Testing status [{}], message [{}].", logRecordId, request.getTestingStatus(),
                request.getMessage());
        return new LogRecordResponse(logRecord.getUuid());
    }

}
