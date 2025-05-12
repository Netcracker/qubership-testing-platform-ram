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

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.qubership.atp.ram.enums.FileContentType;
import org.qubership.atp.ram.exceptions.internal.RamGridFsFileNotFoundException;
import org.qubership.atp.ram.exceptions.logrecords.RamLogRecordFileAsStringException;
import org.qubership.atp.ram.model.ExtendedFileData;
import org.qubership.atp.ram.model.FileData;
import org.qubership.atp.ram.model.GridFsFileData;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.repositories.GridFsRepository;
import org.qubership.atp.ram.repositories.LogRecordRepository;
import org.qubership.atp.ram.utils.FileContentTypeDetector;
import org.qubership.atp.ram.utils.SourceShot;
import org.qubership.atp.ram.utils.StreamUtils;
import org.qubership.atp.ram.utils.UrlParamsUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GridFsService {

    private final GridFsRepository repository;
    private final LogRecordRepository lrRepository;

    @Value("${files.expiration.days.interval}")
    private Integer filesExpirationDaysInterval;

    @Autowired
    public GridFsService(GridFsRepository repository, LogRecordRepository lrRepository) {
        this.repository = repository;
        this.lrRepository = lrRepository;
    }

    /**
     * Method gets screenshot or snapshot from GridFs, thru {@link GridFsRepository#getFileData(UUID)}
     * ScreenShot as Base64 or Snapshot in case it present in database.
     *
     * @param logRecordId of step, which for you would like get ScreenShot
     * @return empty string if ScreenShot/Snapshot isn't present in db. Else - ScreenShot as Base64 or Snapshot
     */
    public SourceShot getScreenShot(UUID logRecordId) {
        Optional<FileData> screenShot = repository.getFileData(logRecordId);
        LogRecord logRecord = lrRepository.findByUuid(logRecordId);
        if (logRecord == null) {
            log.debug("LogRecord wasn't found by id {}.", logRecordId);
            return new SourceShot();
        }
        if (!screenShot.isPresent() && !Strings.isNullOrEmpty(logRecord.getSnapshotId())) {
            FileData newScreen = new FileData();
            newScreen.setContentType("message");
            newScreen.setContent("Screenshot was deleted because the storage time is over.".getBytes());
            log.debug("Screenshot with id {} was deleted because the storage time is over, Log Record id {}.",
                    logRecord.getSnapshotId(), logRecord.getUuid());
            screenShot = Optional.of(newScreen);
        }
        final StringBuilder result = new StringBuilder();
        SourceShot sourceShot = new SourceShot();
        screenShot.ifPresent(image -> {
            sourceShot.setStepName(logRecord.getName());
            sourceShot.setMessage(logRecord.getMessage());
            sourceShot.setStatus(logRecord.getTestingStatus().getName());
            sourceShot.setType(image.getContentType());
            sourceShot.setSnapshotSource(image.getSource());
            if (image.getContentType().equalsIgnoreCase("image/png")) {
                result.append("data:").append(image.getContentType()).append(";base64,");
                result.append(Base64.getEncoder().encodeToString(image.getContent()));
            } else {
                result.append(new String(image.getContent()));
            }
        });
        log.trace("Get SourceShot for Log Record: {}:\n{}", logRecordId, result.toString());
        sourceShot.setContent(result.toString());
        return sourceShot;
    }

    /**
     * Find file in storage and return its data.
     *
     * @param logRecordId if of logRecord
     * @return data of the file
     */
    public FileData downloadFile(UUID logRecordId) {
        return composeFileDataWithLogRecord(lrRepository.findByUuid(logRecordId));
    }

    /**
     * Find file in storage and return its data as string.
     * @param logRecordId related with file
     * @return data of the file as string
     */
    public String downloadFileIntoString(UUID logRecordId) {
        FileData fileData = downloadFile(logRecordId);
        if (FileContentType.BIN.equals(FileContentTypeDetector.detect(fileData.getContent()))) {
            log.error("Can't represent file as string: logRecordId={}", logRecordId);
            throw new RamLogRecordFileAsStringException("", logRecordId);
        }
        return new String(fileData.getContent(), StandardCharsets.UTF_8);
    }

    /**
     * Find file in storage and return its data.
     *
     * @param logRecordId if of logRecord
     * @return data of the file
     */
    public FileData downloadFileByName(UUID logRecordId, String filename) {
        String encodeFilename = UrlParamsUtils.decodeUrlPath(filename);
        return composeFileDataWithLogRecordByFileName(lrRepository.findByUuid(logRecordId), encodeFilename);
    }

    /**
     * Find file in storage and return its data as string.
     *
     * @param logRecordId if of logRecord
     * @return data of the file as string
     */
    public String downloadFileIntoStringByName(UUID logRecordId, String filename) {
        FileData fileData = downloadFileByName(logRecordId, filename);
        if (FileContentType.BIN.equals(FileContentTypeDetector.detect(fileData.getContent()))) {
            log.error("Can't represent file as string: logRecordId={}, filename={}", logRecordId, filename);
            throw new RamLogRecordFileAsStringException(filename, logRecordId);
        }
        return new String(fileData.getContent(), StandardCharsets.UTF_8);
    }

    private FileData composeFileDataWithLogRecordByFileName(LogRecord logRecord, String filename) {
        Optional<FileData> file = repository.getFileDataByFileName(logRecord.getUuid(), filename);
        return file.orElseThrow(() -> {
            log.error("File with id {} was deleted because the storage time is over, LogRecord id {}.",
                    logRecord.getSnapshotId(), logRecord.getUuid());
            return new RamGridFsFileNotFoundException();
        });
    }

    private FileData composeFileDataWithLogRecord(LogRecord logRecord) {
        Optional<FileData> file = repository.getFileData(logRecord.getUuid());
        return file.orElseThrow(() -> {
            log.error("File with id {} was deleted because the storage time is over, LogRecord id {}.",
                    logRecord.getSnapshotId(), logRecord.getUuid());
            return new RamGridFsFileNotFoundException();
        });
    }

    public List<ExtendedFileData> downloadFilesByLogRecords(List<LogRecord> logRecords) {
        List<UUID> logRecordsIds = StreamUtils.extractIdsToList(logRecords);
        return repository.getAllFilesWhereMetadataLogRecordIdInList(logRecordsIds);
    }

    public void removeFilesByRecordUuidList(List<UUID> logRecordUuidList) {
        repository.removeAttachment(logRecordUuidList);
    }

    public String save(String image, String creationTime, String contentType,
                       UUID logRecordId, InputStream inputStream, String fileName, String snapshotSource) {
        return repository.save(image, creationTime, contentType, logRecordId, inputStream, fileName, snapshotSource);
    }

    /**
     * Store string content in gridFs repository.
     *
     * @param type         Source type.
     * @param creationTime Save date.
     * @param contentType  Source content type.
     * @param logRecordId  LogRecord id.
     * @param content      Stored content..
     * @param fileName     File name.
     * @return File id.
     */
    public String save(String type, String creationTime, String contentType,
                       UUID logRecordId, String content, String fileName) {
        ByteArrayInputStream bis = new ByteArrayInputStream(content.getBytes());
        log.debug("Store file {} for Log Record {}.", fileName, logRecordId);
        return repository.save(type, creationTime, contentType, logRecordId, bis, fileName, null);
    }

    /**
     * Store string content in gridFs repository.
     *
     * @param type         Source type
     * @param creationTime Save date
     * @param contentType  Source content type
     * @param logRecordId  LogRecord id
     * @param testRunId    TestRun id
     * @param content      Stored content
     * @param fileName     File name
     * @return {@link ObjectId} of created file
     */
    public String save(String type, String creationTime, String contentType,
                       UUID logRecordId, UUID testRunId,
                       String content, String fileName) {
        ByteArrayInputStream bis = new ByteArrayInputStream(content.getBytes());
        log.info("Save request/response for LR {}", logRecordId);

        return repository.save(type, creationTime, contentType, logRecordId,
                testRunId, bis, fileName, null);
    }

    public GridFsFileData getReportById(UUID reportId) throws FileNotFoundException {
        log.info("Get report by id '{}'", reportId);
        return repository.getReportById(reportId);
    }

    /**
     * Save mandatory checks report.
     *
     * @param executionRequestId execution request identifier
     * @param fileInputStream file input stream
     * @param fileName file name
     */
    public void saveMandatoryChecksReport(UUID reportId, UUID executionRequestId,
                                          InputStream fileInputStream, String fileName) {
        log.info("Save mandatory checks report for execution request with id '{}' with id '{}'",
                executionRequestId, reportId);
        repository.saveMandatoryChecksReport(reportId, executionRequestId, fileInputStream, fileName);
    }

    /**
     * Get count of screenshots for TR.
     *
     * @param logRecords all LogRecords from TR.
     * @return count of screenshots.
     */
    public int getCountScreen(List<LogRecord> logRecords) {
        return repository.getCountScreen(logRecords);
    }


    /**
     * Delete files which created earlier than expired period.
     */
    public void deleteExpiredFiles() {
        LocalDate checkedDate = LocalDate.now().minusDays(filesExpirationDaysInterval);
        log.debug("deleteExpiredFiles older then {}", checkedDate);
        repository.deleteByUploadDate(checkedDate);
    }

    /**
     * Upload SSM metrics report file into GridFS storage.
     */
    public UUID saveSsmMetricReport(String fileName, String type, String contentType, InputStream inputStream,
                                    UUID executionRequestId, UUID logRecordId) {
        final UUID reportId = UUID.randomUUID();
        log.debug("Generated report id: {}", reportId);

        final Map<String, Object> metadata = new HashMap<>();
        metadata.put("reportId", reportId);

        if (nonNull(executionRequestId)) {
            metadata.put("executionRequestId", executionRequestId);
        }

        if (nonNull(logRecordId)) {
            metadata.put("logRecordUuid", logRecordId);
        }

        if (isNull(type)) {
            log.debug("Set default 'json' type");
            type = "json";
        }

        if (isNull(contentType)) {
            log.debug("Set default 'application/json' content type");
            contentType = MediaType.APPLICATION_JSON_VALUE;
        }
        log.debug("Metadata: {}", metadata);

        repository.save(fileName, type, contentType, inputStream, metadata);
        log.debug("Report file has been successfully uploaded");

        return reportId;
    }
}
