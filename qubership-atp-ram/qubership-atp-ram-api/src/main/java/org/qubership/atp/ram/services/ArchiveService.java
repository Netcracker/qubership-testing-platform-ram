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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.qubership.atp.ram.model.ExtendedFileData;
import org.qubership.atp.ram.model.FileData;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ArchiveService {

    private static final String STORAGE_PATH = "./tmp/archives";
    private final ObjectMapper objectMapper;
    private final FileNamesService fileNamesService;

    /**
     * Create zip archive with specified files in it.
     *
     * @param files              list of FileData to store to archive
     * @param executionRequestId id of execution request to form proper archive name
     * @return path to created archive
     * @throws IOException if something is wrong with writing to archive
     */
    public File writeFileDataToArchive(List<ExtendedFileData> files, UUID executionRequestId) throws IOException {
        logFileNamesAndExecutionRequestIdIfNecessary(files, executionRequestId);
        createFolderForTempArchivesIfNecessary();

        String archivePath = STORAGE_PATH + "/all_" + executionRequestId.toString() + ".zip";
        FileOutputStream fos = getArchiveFileOutputStream(archivePath);
        writeEachFileDataToArchive(files, fos);

        return new File(archivePath);
    }

    private void writeEachFileDataToArchive(List<ExtendedFileData> extendedFilesList,
                                            FileOutputStream fos) throws IOException {
        try (ZipArchiveOutputStream zipOutputStream = new ZipArchiveOutputStream(fos)) {
            for (ExtendedFileData extendedFile : extendedFilesList) {
                writeExtendedFileToArchive(zipOutputStream, extendedFile);
            }
        }
    }

    private void writeExtendedFileToArchive(ZipArchiveOutputStream zipOutputStream,
                                            ExtendedFileData potFile) {
        FileData file = potFile.getFileData();
        writeToZipFile(file.getContent(), zipOutputStream, file.getSource());
    }

    private FileOutputStream getArchiveFileOutputStream(String archivePath) throws IOException {
        FileOutputStream fos;
        File archive = new File(archivePath);
        if (archive.createNewFile()) {
            fos = new FileOutputStream(archivePath);
        } else {
            log.error("Cannot create archive to store files at path: {}", archivePath);
            throw new IOException("Cannot create archive to store files at path: " + archivePath);
        }
        return fos;
    }

    private void createFolderForTempArchivesIfNecessary() throws IOException {
        if (Files.notExists(Paths.get(STORAGE_PATH)) && !new File(STORAGE_PATH).mkdirs()) {
            log.error("Cannot create folder for temp archives at path: {}", STORAGE_PATH);
            throw new IOException("Cannot create folder structure to path: " + STORAGE_PATH);
        }
    }

    private void logFileNamesAndExecutionRequestIdIfNecessary(List<ExtendedFileData> files, UUID executionRequestId) {
        if (log.isInfoEnabled()) {
            logFileNamesAndExecutionRequestId(files, executionRequestId);
        }
    }

    private void logFileNamesAndExecutionRequestId(List<ExtendedFileData> files, UUID executionRequestId) {
        try {
            List<String> fileNames = fileNamesService.getFileNamesFromFilesList(files);
            log.info("writeFileDataToArchive Writing files: '{}', executionRequestId: '{}'",
                    objectMapper.writeValueAsString(fileNames),
                    executionRequestId.toString());
        } catch (JsonProcessingException exception) {
            log.error(exception.getMessage());
        }

    }

    private void writeToZipFile(byte[] content, ZipArchiveOutputStream zipStream, String entryName) {
        try {
            ZipArchiveEntry zipEntry = new ZipArchiveEntry(entryName);
            zipStream.putArchiveEntry(zipEntry);
            zipStream.write(content, 0, content.length);

            zipStream.closeArchiveEntry();
        } catch (IOException e) {
            log.error("Error while writing file {} to zip. Exception: {}", entryName, e);
        }
    }
}
