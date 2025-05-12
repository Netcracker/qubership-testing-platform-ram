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

package org.qubership.atp.ram.logging.controllers;

import static org.qubership.atp.ram.logging.constants.ApiPathLogging.API_PATH;
import static org.qubership.atp.ram.logging.constants.ApiPathLogging.LOGGING_PATH;
import static org.qubership.atp.ram.logging.constants.ApiPathLogging.LOG_RECORDS_PATH;

import java.io.InputStream;
import java.util.UUID;

import org.bson.types.ObjectId;
import org.qubership.atp.ram.logging.constants.ApiPathLogging;
import org.qubership.atp.ram.logging.entities.requests.CreatedLogRecordRequest;
import org.qubership.atp.ram.logging.entities.requests.UpdateLogRecordStatusAndResponseRequest;
import org.qubership.atp.ram.logging.entities.responses.CreatedLogRecordResponse;
import org.qubership.atp.ram.logging.entities.responses.UploadScreenshotResponse;
import org.qubership.atp.ram.logging.services.LogRecordLoggingService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RequestMapping(API_PATH + LOGGING_PATH + LOG_RECORDS_PATH)
@RestController()
@RequiredArgsConstructor
public class LogRecordLoggingController {
    private final LogRecordLoggingService logRecordLoggingService;

    /**
     * Upload file for log record.
     *
     * @param id             of log record
     * @param contentType    type of content
     * @param fileName       file name
     * @param snapshotSource source
     * @param inputStream    file
     * @return {@link ObjectId} of uploaded file
     */
    @PostMapping(ApiPathLogging.UPLOAD_PATH + ApiPathLogging.UUID_PATH + ApiPathLogging.STREAM_PATH)
    public UploadScreenshotResponse upload(@PathVariable(ApiPathLogging.UUID) UUID id,
                                           @RequestParam(ApiPathLogging.CONTENT_TYPE_PARAM) String contentType,
                                           @RequestParam(ApiPathLogging.FILE_NAME_PARAM) String fileName,
                                           @RequestParam(ApiPathLogging.SNAPSHOT_SOURCE_PARAM) String snapshotSource,
                                           @RequestBody InputStream inputStream) {
        return logRecordLoggingService.upload(contentType, id, inputStream, fileName, snapshotSource);
    }

    /**
     * Update Log Records (REST, MIA, ITF) status, message, request and response.
     * @param logRecordId id of log record
     * @param request     content
     * @return            id of updated log record
     */
    @PostMapping(ApiPathLogging.UUID_PATH + ApiPathLogging.UPDATE_PATH)
    public CreatedLogRecordResponse updateTestingStatusMessageAndRequestResponse(
            @PathVariable(ApiPathLogging.UUID) UUID logRecordId,
            @RequestBody UpdateLogRecordStatusAndResponseRequest request) {
        return logRecordLoggingService.updateTestingStatusMessageAndRequestResponse(logRecordId, request);
    }

    /**
     * Find existed log record or created new by info from request.
     * Then update execution status of test run and execution request:
     * 1) Execution status set to 'In Progress' if test run isn't terminated
     * 2) Or do nothing, if test run is terminated
     *
     * @param createdLogRecordRequest info for created new log record
     * @return ID of log record
     */
    @PostMapping(ApiPathLogging.FIND_OR_CREATE_PATH)
    public CreatedLogRecordResponse findOrCreate(@RequestBody CreatedLogRecordRequest createdLogRecordRequest) {
        return logRecordLoggingService.findOrCreate(createdLogRecordRequest);
    }
}
