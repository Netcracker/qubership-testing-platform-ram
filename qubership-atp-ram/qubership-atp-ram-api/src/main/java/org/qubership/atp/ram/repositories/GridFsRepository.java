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

package org.qubership.atp.ram.repositories;

import static java.util.Objects.isNull;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.qubership.atp.ram.exceptions.internal.RamReportGridFsFileNotFoundException;
import org.qubership.atp.ram.model.ExtendedFileData;
import org.qubership.atp.ram.model.FileData;
import org.qubership.atp.ram.model.GridFsFileData;
import org.qubership.atp.ram.models.LogRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;

import com.google.common.base.Strings;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Repository("ram-gridfs-repository")
@RequiredArgsConstructor
@Slf4j
public class GridFsRepository {
    private static final String LOG_RECORD_UUID = "logRecordUuid";
    private static final String METADATA_LOG_RECORD_UUID = "metadata" + "." + LOG_RECORD_UUID;
    private static final String METADATA_SOURCE = "metadata.snapshotSource";
    private static final String TYPE = "type";
    private static final String UPLOAD_DATE = "uploadDate";
    private static final String CONTENT_TYPE = "contentType";
    private static final String REPORT_ID = "reportId";
    private static final String HTML = "html";
    private static final String EXECUTION_REQUEST_ID = "executionRequestId";
    private static final String METADATA_REPORT_ID = "metadata.reportId";
    private static final String SNAPSHOT_SOURCE = "snapshotSource";
    private static final String TEST_RUN_ID = "testRunId";
    private static final String TEST_RUN_UUID = "testRunUuid";

    private final GridFSBucket gridFsBucket;

    @Value("${gridfs.chunk.size}")
    private Integer chunkSizeBytes;

    /**
     * This method retrieves screen shot fro GridFS system.
     *
     * @param logRecordsIds of steps, which for you would like get List of FileData
     * @return {@link Optional#empty()} if ScreenShot not found for specified logRecordUuid or
     *      {@link FileData} if it present in database.
     */
    public @NotNull List<ExtendedFileData> getAllFilesWhereMetadataLogRecordIdInList(List<UUID> logRecordsIds) {
        Document filter = getMetadataLogRecordIdsInFilter(logRecordsIds);

        List<GridFSFile> gridFsFiles = new ArrayList<>();
        gridFsFiles = gridFsBucket.find(filter).into(gridFsFiles);

        List<ExtendedFileData> result = gridFsFiles.parallelStream().map(file -> {
            log.debug("File was found {}", file.getFilename());
            UUID logRecordId = Objects.requireNonNull(file.getMetadata()).get(LOG_RECORD_UUID, UUID.class);
            return new ExtendedFileData(createFileDataFromFileInDb(file), logRecordId);
        }).collect(Collectors.toList());

        if (result.isEmpty()) {
            log.debug("Cannot find any files");
        }
        return result;
    }

    private FileData createFileDataFromFileInDb(GridFSFile file) {
        ByteArrayOutputStream bos = downloadFileFromDbToByteStream(file);
        return composeFileDataFromGridFsFileAndItsContent(file, bos);
    }

    private ByteArrayOutputStream downloadFileFromDbToByteStream(GridFSFile file) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        gridFsBucket.downloadToStream(file.getObjectId(), bos);
        return bos;
    }

    private FileData composeFileDataFromGridFsFileAndItsContent(GridFSFile file, ByteArrayOutputStream bos) {
        FileData fileData = new FileData();
        fileData.setContent(bos.toByteArray());
        addMetadataToFileDataIfItExists(file, fileData);
        return fileData;
    }

    private void addMetadataToFileDataIfItExists(GridFSFile file, FileData fileData) {
        Document metadata = file.getMetadata();
        if (isNull(metadata)) {
            log.debug("Cannot get metadata for file {}", file);
        } else {
            fileData.setContentType(
                    Optional.ofNullable(metadata.getString("contentType")).orElse("image/png")
            );
            fileData.setSource(Strings.nullToEmpty(metadata.getString("snapshotSource")));
        }
    }

    /**
     * This method retrieves all files from gridFs where metadata.logRecordUuid equals logRecordUuid
     *
     * @param logRecordUuid of step, which for you would like get FileData
     * @return {@link Optional#empty()} if ScreenShot not found for specified logRecordUuid or
     *      {@link FileData} if it present in database.
     */
    @Deprecated
    public Optional<FileData> getFileData(UUID logRecordUuid) {
        Document filter = getFilter(logRecordUuid);
        return getFileDataByFilter(logRecordUuid, filter);
    }

    /**
     * This method retrieves file from gridFs where metadata.logRecordUuid equals logRecordUuid and
     * metadata.snapshotSource equals filename.
     *
     * @param logRecordUuid of step, which for you would like get FileData
     * @param filename      file name
     * @return {@link Optional#empty()} if ScreenShot not found for specified logRecordUuid or
     *      {@link FileData} if it present in database.
     */
    public Optional<FileData> getFileDataByFileName(UUID logRecordUuid, String filename) {
        Document filter = getFilter(logRecordUuid).append(METADATA_SOURCE, filename);
        return getFileDataByFilter(logRecordUuid, filter);
    }

    /**
     * Remove attachment.
     *
     * @param logRecordsUuidList of log record, for which will delete attachment
     */
    public void removeAttachment(List<UUID> logRecordsUuidList) {
        logRecordsUuidList.forEach(uuid -> {
            Document filter = new Document().append(METADATA_LOG_RECORD_UUID, uuid);
            GridFSFile res = gridFsBucket.find(filter).first();
            if (Objects.nonNull(res)) {
                gridFsBucket.delete(res.getObjectId());
            }
        });
    }

    /**
     * Get count of screenshots for TR.
     *
     * @param logRecords all LogRecords from TR.
     * @return count of screenshots.
     */
    public int getCountScreen(List<LogRecord> logRecords) {
        AtomicInteger countOfScreenshots = new AtomicInteger(0);
        Document filter = new Document();
        logRecords.parallelStream().forEach(logRecord -> {
            filter.append(METADATA_LOG_RECORD_UUID, logRecord.getUuid());
            GridFSFile res = findByFilter(filter);
            if (Objects.nonNull(res)) {
                countOfScreenshots.incrementAndGet();
            }
        });
        return countOfScreenshots.get();
    }

    private Optional<FileData> getFileDataByFilter(UUID logRecordUuid, Document filter) {
        GridFSFile res = gridFsBucket.find(filter).first();
        if (isNull(res)) {
            log.debug("Cannot get file for logRecord {}", logRecordUuid);
            return Optional.empty();
        }
        FileData fileData = createFileDataFromFileInDb(res);
        log.debug("File was found {} for LR {}", res.getFilename(), logRecordUuid);

        return Optional.of(fileData);
    }

    private GridFSFile findByFilter(Document filter) {
        return gridFsBucket.find(filter).first();
    }

    private Document getFilter(UUID logRecordUuid) {
        return new Document().append(METADATA_LOG_RECORD_UUID, logRecordUuid);
    }

    private Document getMetadataLogRecordIdsInFilter(List<UUID> logRecordsIds) {
        return new Document().append(METADATA_LOG_RECORD_UUID,
                new Document().append("$in", logRecordsIds));
    }

    /**
     * Save screenshot.
     */
    public String save(String type, String creationTime, String contentType,
                       UUID id, InputStream fileInputStream, String fileName, String snapshotSource) {
        GridFSUploadOptions uploadOptions = new GridFSUploadOptions()
                .chunkSizeBytes(chunkSizeBytes)
                .metadata(new Document(TYPE, type)
                        .append(UPLOAD_DATE, creationTime)
                        .append(CONTENT_TYPE, contentType)
                        .append(LOG_RECORD_UUID, id)
                        .append(SNAPSHOT_SOURCE, snapshotSource));

        return String.valueOf(gridFsBucket.uploadFromStream(fileName, fileInputStream, uploadOptions));
    }

    /**
     * Save file for log record.
     *
     * @param type            type of file
     * @param creationTime    time of creation
     * @param contentType     type of content
     * @param logRecordUuid   id of log record
     * @param testRunId       id of test run
     * @param fileInputStream file
     * @param fileName        name
     * @param snapshotSource  source
     * @return {@link ObjectId} of created file
     */
    public String save(String type, String creationTime, String contentType,
                       UUID logRecordUuid, UUID testRunId,
                       InputStream fileInputStream, String fileName, String snapshotSource) {
        GridFSUploadOptions uploadOptions = new GridFSUploadOptions()
                .chunkSizeBytes(chunkSizeBytes)
                .metadata(new Document(TYPE, type)
                        .append(UPLOAD_DATE, creationTime)
                        .append(CONTENT_TYPE, contentType)
                        .append(LOG_RECORD_UUID, logRecordUuid)
                        .append(TEST_RUN_ID, testRunId)
                        .append(SNAPSHOT_SOURCE, snapshotSource));

        return String.valueOf(gridFsBucket.uploadFromStream(fileName, fileInputStream, uploadOptions));
    }

    /**
     * Upload file with specified filename, type, content type and metadata.
     *
     * @param fileName file name
     * @param type file type
     * @param contentType file content type
     * @param fileInputStream file input steam data
     * @param metadata metadata map
     * @return uploaded file identifier
     */
    public String save(String fileName, String type, String contentType, InputStream fileInputStream,
                       Map<String, Object> metadata) {
        log.debug("Upload file into GridFS with params: fileName='{}', type='{}', contentType='{}', metadata='{}'",
                fileName, type, contentType, metadata);
        Document document = new Document(TYPE, type);

        document.append(UPLOAD_DATE, LocalDateTime.now().toString())
                .append(CONTENT_TYPE, contentType);

        metadata.forEach(document::append);
        log.debug("Metadata: {}", metadata);

        GridFSUploadOptions uploadOptions = new GridFSUploadOptions()
                .chunkSizeBytes(chunkSizeBytes)
                .metadata(document);

        ObjectId uploadedFileId = gridFsBucket.uploadFromStream(fileName, fileInputStream, uploadOptions);
        log.debug("Uploaded file id: {}", uploadedFileId);

        return String.valueOf(uploadedFileId);
    }

    /**
     * Get report by id.
     *
     * @param reportId report identifier
     * @return file data
     */
    public GridFsFileData getReportById(UUID reportId) throws FileNotFoundException {
        Document filter = new Document().append(METADATA_REPORT_ID, reportId);
        GridFSFile reportFile = gridFsBucket.find(filter).first();

        if (isNull(reportFile)) {
            log.error("Failed to find report file in the GridFS by id '{}'", reportId);
            throw new RamReportGridFsFileNotFoundException();
        }
        ByteArrayOutputStream outputStream = downloadFileFromDbToByteStream(reportFile);

        return new GridFsFileData(reportFile.getFilename(), outputStream);
    }

    /**
     * Save mandatory checks report.
     */
    public void saveMandatoryChecksReport(UUID reportId, UUID executionRequestId,
                                          InputStream fileInputStream, String fileName) {
        GridFSUploadOptions uploadOptions = new GridFSUploadOptions()
                .chunkSizeBytes(chunkSizeBytes)
                .metadata(new Document(TYPE, HTML)
                        .append(UPLOAD_DATE, LocalDateTime.now().toString())
                        .append(REPORT_ID, reportId)
                        .append(CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
                        .append(EXECUTION_REQUEST_ID, executionRequestId));

        gridFsBucket.uploadFromStream(fileName, fileInputStream, uploadOptions);
    }

    private void deleteByFilter(Document filter) {
        log.debug("deleteByFilter gridFsFiles {}", filter);
        gridFsBucket.find(filter).forEach((Consumer<? super GridFSFile>) gridFSFile -> {
            try {
                log.debug("delete file gridFSFile.getObjectId()");
                gridFsBucket.delete(gridFSFile.getId());
            } catch (Exception ex) {
                log.error("Cannot delete file", ex);
            }
        });
    }

    /**
     * Delete documents from gridfs by `filter` UPLOAD_DATE.
     */
    public void deleteByUploadDate(LocalDate checkedDate) {
        log.debug("deleteByUploadDate {}", checkedDate);
        Document expiredFilesFilter = new Document(GridFsRepository.UPLOAD_DATE, new Document("$lte", checkedDate));
        deleteByFilter(expiredFilesFilter);
    }
}
