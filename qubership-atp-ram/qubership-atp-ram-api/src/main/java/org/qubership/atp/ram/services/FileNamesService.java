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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.qubership.atp.ram.model.ExtendedFileData;
import org.qubership.atp.ram.model.FileData;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.TestRun;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileNamesService {

    private final ObjectMapper objectMapper;

    /**
     * If any 2 files have name collisions they should be renamed to allow saving them to the same directory.
     *
     * @param files     list of files
     * @param testRunMap   map testRunId to it's instance
     * @param logRecordMap map logRecordId to it's instance
     */
    public void renameFilesInPlaceIfCollisionsOccurs(List<ExtendedFileData> files,
                                                     Map<UUID, LogRecord> logRecordMap,
                                                     Map<UUID, TestRun> testRunMap) {
        logInputParametersIfNecessary(files, logRecordMap, testRunMap);

        prependFileNamesWithTestCaseNameIfCollisionInFileNamesOccurs(files, logRecordMap, testRunMap);
        normalizeFilesNames(files);
        appendFileNamesWithIndexesIfNameCollisionOccurs(files);
    }

    public List<String> getFileNamesFromFilesList(List<ExtendedFileData> files) {
        return files.stream().map(file -> file.getFileData().getSource()).collect(Collectors.toList());
    }

    private void logInputParametersIfNecessary(List<ExtendedFileData> files,
                                               Map<UUID, LogRecord> logRecordMap,
                                               Map<UUID, TestRun> testRunMap) {
        if (log.isDebugEnabled()) {
            logInputParameters(files, logRecordMap, testRunMap);
        }
    }

    private void logInputParameters(List<ExtendedFileData> files,
                                    Map<UUID, LogRecord> logRecordMap,
                                    Map<UUID, TestRun> testRunMap) {
        try {
            List<String> fileNames = getFileNamesFromFilesList(files);
            log.debug("renameFilesInPlaceIfCollisionsOccurs files: '{}' logRecordMap: '{}', testRunMap: '{}'",
                    objectMapper.writeValueAsString(logRecordMap),
                    objectMapper.writeValueAsString(testRunMap),
                    objectMapper.writeValueAsString(fileNames));
        } catch (JsonProcessingException exception) {
            log.error(exception.getMessage());
        }
    }

    private void normalizeFilesNames(List<ExtendedFileData> potFiles) {
        for (ExtendedFileData potFile : potFiles) {
            FileData file = potFile.getFileData();
            file.setSource(normalizeName(file.getSource()));
        }
    }

    private String normalizeName(String name) {
        return name
                .replaceAll("[!@#^$&~%(*){}'\":;><`]", "")
                .replace(" ", "_")
                .replace("/", "_");
    }

    private void appendFileNamesWithIndexesIfNameCollisionOccurs(List<ExtendedFileData> potFiles) {
        HashMap<String, Integer> nameIndexerMap = new HashMap<>();
        for (ExtendedFileData extendedFileData : potFiles) {
            FileData file = extendedFileData.getFileData();
            if (nameIndexerMap.containsKey(file.getSource())) {
                Integer index = nameIndexerMap.get(file.getSource());
                index++;
                nameIndexerMap.put(file.getSource(), index);
                appendFileNameWithIndex(file, index);
            } else {
                nameIndexerMap.put(file.getSource(), 0);
            }
        }
    }

    private void appendFileNameWithIndex(FileData file, Integer index) {
        file.setSource(FilenameUtils.getBaseName(file.getSource())
                + "_" + index + "." + FilenameUtils.getExtension(file.getSource()));
    }

    private void prependFileNamesWithTestCaseNameIfCollisionInFileNamesOccurs(List<ExtendedFileData> potFiles,
                                                                              Map<UUID, LogRecord> logRecordMap,
                                                                              Map<UUID, TestRun> testRunMap) {
        Set<String> duplicateNames = findDuplicateNames(potFiles);

        for (ExtendedFileData extendedFileData : potFiles) {
            FileData file = extendedFileData.getFileData();
            if (duplicateNames.contains(file.getSource())) {
                prependTestRunNameToFileSourceName(logRecordMap, testRunMap, extendedFileData);
            }
        }
    }

    private void prependTestRunNameToFileSourceName(Map<UUID, LogRecord> logRecordMap,
                                                    Map<UUID, TestRun> testRunMap,
                                                    ExtendedFileData extendedFileData) {
        FileData fileData = extendedFileData.getFileData();
        String testRunName =
                testRunMap.get(logRecordMap.get(extendedFileData.getLogRecordId()).getTestRunId()).getName();
        fileData.setSource(testRunName + "_" + fileData.getSource());
    }

    private Set<String> findDuplicateNames(List<ExtendedFileData> potFiles) {
        Set<String> namesAlreadyMet = new HashSet<>();
        Set<String> duplicateNames = new HashSet<>();
        for (ExtendedFileData potFile : potFiles) {
            FileData file = potFile.getFileData();
            if (namesAlreadyMet.contains(file.getSource())) {
                duplicateNames.add(file.getSource());
            }
            namesAlreadyMet.add(file.getSource());
        }
        return duplicateNames;
    }

}
