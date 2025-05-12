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

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.qubership.atp.ram.dto.response.BaseEntityResponse;
import org.qubership.atp.ram.dto.response.CompareTreeTestRunResponse;
import org.qubership.atp.ram.dto.response.PaginationResponse;
import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.models.AnalyzedTestRunSortedColumns;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.models.TestRunSearchRequest;
import org.qubership.atp.ram.models.logrecords.parts.FileType;
import org.springframework.data.domain.Sort;

/**
 * Custom repo for test runs.
 */
public interface CustomTestRunRepository {

    /**
     * Find paged test runs by filter.
     *
     * @param page       page num
     * @param size       size num
     * @param columnType column for sorting
     * @param sortType   sorting type
     * @param filter     filter
     * @return filtered test runs
     */
    PaginationResponse<TestRun> findAllByFilter(int page,
                                                int size,
                                                AnalyzedTestRunSortedColumns columnType,
                                                Sort.Direction sortType,
                                                TestRunSearchRequest filter);

    List<CompareTreeTestRunResponse> compareByExecutionRequestIds(List<UUID> executionRequestIds);

    List<BaseEntityResponse> getTestRunsNotInExecutionRequestCompareTable(
            List<UUID> executionRequestIds);

    void updateStatusesAndFinishDateByTestRunId(UUID testRunId, ExecutionStatuses executionStatus,
                                                TestingStatuses testingStatus, Timestamp finishDate, long duration);

    List<TestRun> findTestRunsByExecutionRequestAndHasLogRecordsWithFile(UUID executionRequestId, FileType fileType);

    List<TestRun> findTestRunsByExecutionRequestIdAndNamesAndLabelIds(UUID executionRequestId,
                                                                      List<String> testRunNames, List<UUID> labelIds);

    List<TestRun> findTestRunsIdNameByExecutionRequestIdAndLabelIds(UUID executionRequestId, List<UUID> labelIds);

    UUID findProjectIdByTestRunId(UUID testRunId);

    UUID findProjectIdByTestCaseId(UUID testCaseId);

    void updateAnyFieldsRamObjectByIdDocument(UUID testRunId, Map<String, Object> fieldsToUpdate, Class<?> entityClass);
}
