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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.qubership.atp.ram.exceptions.potfiles.RamPotFilesArchiveDataCreationException;
import org.qubership.atp.ram.model.ArchiveData;
import org.qubership.atp.ram.model.ExtendedFileData;
import org.qubership.atp.ram.model.LogRecordWithParentResponse;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.PotsStatisticsPerAction;
import org.qubership.atp.ram.models.PotsStatisticsPerTestCase;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.models.logrecords.parts.FileMetadata;
import org.qubership.atp.ram.models.logrecords.parts.FileType;
import org.qubership.atp.ram.repositories.CustomLogRecordRepository;
import org.qubership.atp.ram.repositories.LogRecordRepository;
import org.qubership.atp.ram.repositories.TestRunRepository;
import org.qubership.atp.ram.utils.StreamUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PotService {

    private final LogRecordRepository logRecordRepository;
    private final TestRunRepository testRunRepository;
    private final GridFsService gridFsService;
    private final ArchiveService archiveService;
    private final FileNamesService fileNamesService;
    private final ObjectMapper objectMapper;
    private final CustomLogRecordRepository customLogRecordRepository;

    /**
     * Collection all POTs files in ER with specified id.
     *
     * @param executionRequestId id of ER
     * @return return statistics of POTs files per ER
     */
    public List<PotsStatisticsPerTestCase> collectStatisticForExecutionRequest(UUID executionRequestId) {
        log.info("PotService request collectStatisticForExecutionRequest with id '{}'", executionRequestId);
        List<TestRun> testRuns = testRunRepository
                .findTestRunsByExecutionRequestAndHasLogRecordsWithFile(executionRequestId, FileType.POT);

        return testRuns.stream()
                .map(this::buildPotStatisticPerTestCase)
                .collect(Collectors.toList());
    }

    Map<UUID, List<LogRecord>> collectTrIdsToListOfReportRecordsMap(List<LogRecord> logRecords) {
        return logRecords
                .stream()
                .collect(Collectors.groupingBy(LogRecord::getTestRunId));
    }

    void logPotsRecordsIfNecessary(Map<UUID, List<LogRecord>> testRunIdsToPotsLogRecordsMap) {
        if (log.isDebugEnabled()) {
            try {
                log.debug("testRunIdsToPotsLogRecordsMap: {}", objectMapper
                        .writeValueAsString(testRunIdsToPotsLogRecordsMap));
            } catch (JsonProcessingException e) {
                log.error("Unable to write testRunIdsToPotsLogRecordsMap json: ", e);
            }
        }
    }

    PotsStatisticsPerTestCase buildPotStatisticPerTestCase(TestRun testRun) {
        return new PotsStatisticsPerTestCase(
                testRun.getTestingStatus(), testRun.getName(), testRun.getUuid());
    }

    PotsStatisticsPerAction buildPotStatisticPerAction(LogRecord logRecordPot,
                                                       LogRecord parentLogRecord) {
        return new PotsStatisticsPerAction(
                parentLogRecord.getName(),
                parentLogRecord.getTestingStatus(),
                logRecordPot.getFileMetadata().stream().filter(file -> FileType.POT.equals(file.getType())).findFirst()
                        .orElse(new FileMetadata()).getFileName(),
                logRecordPot.getUuid().toString()
        );
    }

    LogRecord findParentSectionRecord(LogRecord logRecord, Map<UUID, LogRecord> logRecordMap) {
        LogRecord iterator = logRecord;
        while (!iterator.isSection()) {
            iterator = logRecordMap.get(iterator.getParentRecordId());
        }
        return iterator;
    }

    boolean recordHasPotFileMetadata(LogRecord logRecord) {
        return logRecord.getFileMetadata() != null
                && logRecord.getFileMetadata().stream().anyMatch(file -> FileType.POT.equals(file.getType()));
    }

    /**
     * Create temp archive with pot files, store it's content to heap and delete temp archive.
     *
     * @param executionRequestId id of ER
     * @return archive data
     */
    public ArchiveData getArchiveWithPotsFiles(UUID executionRequestId) {
        List<TestRun> testRuns = testRunRepository.findTestRunsIdNameByExecutionRequestId(executionRequestId);
        List<UUID> testRunsIds = StreamUtils.extractIdsToList(testRuns);
        List<LogRecord> logRecords =
                logRecordRepository.findLogRecordsForArchivePotFileByTestRunIdIn(testRunsIds, FileType.POT);
        Map<UUID, List<LogRecord>> trIdToListOfPotRecordsMap = collectTrIdsToListOfReportRecordsMap(logRecords);
        List<LogRecord> potRecords = trIdToListOfPotRecordsMap.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        Map<UUID, TestRun> testRunMap = StreamUtils.toIdEntityMap(testRuns);
        Map<UUID, LogRecord> logRecordMap = StreamUtils.toIdEntityMap(logRecords);

        List<ExtendedFileData> files = gridFsService.downloadFilesByLogRecords(potRecords);
        fileNamesService.renameFilesInPlaceIfCollisionsOccurs(files, logRecordMap, testRunMap);
        File archive;
        byte[] archiveContent;
        String archiveName;
        try {
            archive = archiveService.writeFileDataToArchive(files, executionRequestId);

            archiveName = archive.getName();

            archiveContent = FileCopyUtils.copyToByteArray(archive);
            Files.delete(Paths.get(archive.getAbsolutePath()));
        } catch (IOException exception) {
            log.error("Failed to create POT files archive data", exception);
            throw new RamPotFilesArchiveDataCreationException();
        }

        return new ArchiveData(archiveContent, archiveName);
    }

    /**
     * Get log records pot statistics.
     *
     * @param testRunId ID of TR
     * @return pot statistics with parent and pot LR-s
     */
    public List<PotsStatisticsPerAction> collectStatisticForTestRun(UUID testRunId) {
        List<LogRecordWithParentResponse> parentChildLogRecords = customLogRecordRepository
                .getTopLogRecordsIdAndChildLogRecordsByFileTypeFilterLookup(testRunId, FileType.POT);

        List<PotsStatisticsPerAction> potsStatisticsPerActions = new ArrayList<>();
        parentChildLogRecords.forEach(logRecordLookupResponse -> {
            potsStatisticsPerActions.add(buildPotStatisticPerAction(logRecordLookupResponse.getFileLogRecord(),
                    logRecordLookupResponse.getParent()));
        });
        return potsStatisticsPerActions;
    }
}
