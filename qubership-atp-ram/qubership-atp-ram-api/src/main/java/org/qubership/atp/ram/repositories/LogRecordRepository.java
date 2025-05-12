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
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.enums.TypeAction;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.logrecords.parts.FileType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface LogRecordRepository extends MongoRepository<LogRecord, UUID> {
    LogRecord findByUuid(UUID uuid);

    @Query(fields = "{'stepContextVariables': 1}")
    LogRecord findLogRecordWithStepContextByUuid(UUID uuid);

    Stream<LogRecord> findAllByParentRecordIdIsOrderByCreatedDateStampAsc(UUID parentRecordId);

    @Query(fields = "{'name': 1, 'testingStatus': 1, 'executionStatus': 1, 'parentRecordId': 1, 'message': 1, "
            + "'type': 1, 'testRunId':1, 'metaInfo':1}")
    List<LogRecord> findLogRecordsForTreeByTestRunIdAndParentRecordIdOrderByCreatedDateStampAsc(UUID testRunId,
                                                                                                UUID parentRecordId);

    @Query(fields = "{'name': 1, 'testingStatus': 1,'isSection': 1,'isCompaund': 1, 'duration': 1,"
            + " 'executionStatus': 1, 'parentRecordId': 1, 'message': 1, 'type': 1, 'testRunId':1, 'metaInfo':1}")
    List<LogRecord> findLogRecordsByTestRunIdAndParentRecordIdOrderByCreatedDateStampAsc(UUID testRunId,
                                                                                                UUID parentRecordId);

    @Query(fields = "{'name': 1, 'testingStatus': 1, 'executionStatus': 1, 'parentRecordId': 1, 'message': 1, "
            + "'type': 1, 'testRunId':1, 'metaInfo':1}")
    LogRecord findLogRecordForTreeByUuid(UUID logRecordId);

    @Query(fields = "{'uuid': 1, 'testingStatus': 1, 'type': 1, 'testRunId': 1, 'name': 1, 'message': 1, "
            + "'parentRecordId': 1, 'duration': 1, 'isSection': 1, 'isCompaund': 1}")
    List<LogRecord> findLogRecordsWithSpecificFieldsByTestRunIdOrderByStartDateAsc(UUID testRunId);

    @Query(fields = "{'uuid': 1, 'testingStatus': 1, 'type': 1, 'preview': 1}")
    Stream<LogRecord> findLogRecordsWithPreviewByTestRunIdOrderByStartDateAsc(UUID testRunId);

    List<LogRecord> findAllByTestRunIdOrderByStartDateAsc(UUID testRunId);

    @Query(fields = "{'name': 1}")
    List<LogRecord> findAllNameByTestRunId(UUID testRunId);

    @Query(fields = "{'uuid': 1}")
    List<LogRecord> findAllUuidByTestRunId(UUID testRunId);

    @Query(fields = "{'uuid': 1, 'startDate': 1, 'endDate': 1}")
    LogRecord findFirstByTestRunIdAndExecutionStatusOrderByCreatedDateStampDesc(UUID testRunId,
                                                                                 ExecutionStatuses status);

    @Query(fields = "{'testingStatus': 1}")
    List<LogRecord> findAllTestingStatusByTestRunId(UUID testRunId);

    Long countAllByParentRecordIdIs(UUID parentId);

    List<LogRecord> findAllByTestRunIdAndIsSectionAndIsCompaund(UUID testRunId, boolean isSection,
                                                                boolean isCompound);

    List<LogRecord> findAllByTestRunIdAndIsSection(UUID testRunId, boolean isSection);

    List<LogRecord> findAllByTestRunIdAndTestingStatus(UUID testRunId, TestingStatuses testingStatuses);

    @Query(fields = "{'uuid': 1, 'message': 1, 'testRunId': 1}")
    List<LogRecord> findUuidAndMessageByTestRunIdAndTestingStatus(UUID testRunId, TestingStatuses testingStatuses);

    List<LogRecord> findAllByTestRunIdIn(List<UUID> testRunIds);

    List<LogRecord> findAllByTestRunIdAndNameContains(UUID testRunId, String searchValue);

    Stream<LogRecord> findAllByTestRunIdAndNameRegex(UUID testRunId, String searchValue);

    List<LogRecord> findAllByParentRecordIdOrderByStartDateAsc(UUID parentId);

    List<LogRecord> findAllByTestRunIdAndType(UUID testRunId, TypeAction type);

    @Query(fields = "{'uuid': 1, 'name':1}")
    List<LogRecord> findAllByTestingStatusAndTestRunId(TestingStatuses testingStatuses, UUID testRunId);

    List<LogRecord> findAllByParentRecordIdAndNameContains(UUID parentRecordId, String searchValue);

    @Query(fields = "{'uuid': 1, 'testRunId': 1, 'testingStatus': 1, 'executionStatus': 1}")
    List<LogRecord> findAllByLastUpdatedAfterAndTestRunIdIn(Date lastLoaded, List<UUID> testRunIds);

    List<LogRecord> findAllByTestRunId(UUID testRunId);

    @Query(fields = "{'testRunId': 1, 'validationLabels': 1, 'validationTable': 1, 'testingStatus': 1}")
    List<LogRecord> findLogRecordsWithValidationParamsByTestRunId(UUID testRunId);

    @Query(value = "{'testRunId': {$in: ?0}, 'metaInfo': {$exists: true}, 'testingStatus': ?1}",
            fields = "{'uuid': 1, 'name': 1, 'testRunId': 1, 'testingStatus': 1, 'parentRecordId': 1}")
    List<LogRecord> findLogRecordsWithMetaInfoByTestRunIdInAndTestingStatus(Collection<UUID> testRunIds,
                                                                            TestingStatuses status);

    @Query(value = "{'testRunId': {$in: ?0}, "
            + "$or: ["
            +   "{'validationLabels': {$exists: true, $not: {$size: 0}}},"
            +   "{'validationTable.steps.validationLabels': {$exists: true, $not: {$size: 0}}},"
            +   "{'testingStatus': 'FAILED'}"
            + "]}",
            fields = "{'uuid': 1, 'name': 1, 'testRunId': 1, 'validationLabels': 1, "
                    + "'validationTable': 1, 'testingStatus': 1, 'parentRecordId': 1, 'metaInfo': 1}")
    Stream<LogRecord> findLogRecordsWithValidationParamsAndFailureByTestRunIds(Collection<UUID> testRunIds);

    void deleteByUuid(UUID uuid);

    void deleteAllByUuidIn(List<UUID> uuid);

    @Query(fields = "{'fileMetadata': 1, 'uuid': 1, 'testRunId': 1}",
            value = "{testRunId: {$in: ?0}, 'fileMetadata.type': ?1}")
    List<LogRecord> findLogRecordsForArchivePotFileByTestRunIdIn(List<UUID> testRunsId, FileType fileType);

    @Query(fields = "{'uuid': 1, 'testRunId': 1}", value = "{testRunId: {$in: ?0}, 'fileMetadata.type': ?1}")
    List<LogRecord> findFileLogRecordsByTestRunIdInAndFileType(List<UUID> testRunsId, FileType fileType);

    @Query(fields = "{'name': 1, 'testingStatus': 1}")
    List<LogRecord> findLogRecordsByUuidIn(Set<UUID> logRecordsId);

    Long countAllByTestRunIdIn(Set<UUID> testRunIds);

    Long countAllByTestRunIdInAndTestingStatus(Collection<UUID> testRunIds, TestingStatuses testingStatuses);

    Stream<LogRecord> findAllByTestRunIdInAndTestingStatus(Collection<UUID> testRunIds,
                                                           TestingStatuses testingStatuses);

    Stream<LogRecord> findAllByUuidNotInAndTestRunIdInAndTestingStatus(Collection<UUID> logRecordsId,
                                                                       Collection<UUID> testRunIds,
                                                                       TestingStatuses testingStatuses);
}
