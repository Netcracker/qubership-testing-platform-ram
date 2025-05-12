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

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.model.LogRecordWithChildrenResponse;
import org.qubership.atp.ram.model.LogRecordWithParentListResponse;
import org.qubership.atp.ram.model.LogRecordWithParentResponse;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.logrecords.parts.FileType;

public interface CustomLogRecordRepository {

    List<LogRecord> getTopLogRecordsByFilterLookup(UUID testRunId,
                                                   List<String> statuses,
                                                   List<String> types,
                                                   boolean showNotAnalyzedItemsOnly);

    List<LogRecord> getAllHierarchicalChildrenLogRecords(UUID parentLogRecord);

    List<LogRecordWithParentResponse> getTopLogRecordsIdAndChildLogRecordsByFileTypeFilterLookup(UUID testRunId,
                                                                                                 FileType fileType);

    LogRecordWithChildrenResponse getLogRecordParentAndChildrenByTestingStatusAndTestRunId(UUID tesRunId,
                                                                                           TestingStatuses
                                                                                                   testingStatus);

    List<LogRecord> findLogRecordsByTestRunIdsAndValidationWithHint(Collection<UUID> testRunIds);


    List<LogRecordWithParentListResponse> findLogRecordsWithParentsByPreviewExists(UUID testRunId);

    LogRecord findLastOrcLogRecordByTestRunAndExecutionStatus(UUID testRunId, ExecutionStatuses status);

    UUID getProjectIdByLogRecordId(UUID logRecordId);
}
