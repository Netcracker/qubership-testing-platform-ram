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
import java.util.Set;
import java.util.UUID;

import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.models.EnrichedTestRun;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.models.response.TestRunsRatesResponse;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TestRunRepository extends CustomTestRunRepository, MongoRepository<TestRun, UUID> {
    TestRun findByUuid(UUID uuid);

    TestRun findFirstByTestCaseIdOrderByStartDateDesc(UUID testCaseId);

    List<TestRun> findAllByTestCaseIdOrderByStartDateDesc(UUID testCaseId);

    @Query(fields = "{'uuid': 1, 'finishDate': 1, 'startDate': 1}")
    List<TestRun> findAllByExecutionStatusOrderByStartDateDesc(ExecutionStatuses statuses);

    @Query(fields = "{'uuid': 1, 'finishDate': 1, 'startDate': 1}")
    List<TestRun> findAllByExecutionStatusIn(List<ExecutionStatuses> statuses);

    void deleteByUuid(UUID uuid);

    List<TestRun> findAllByExecutionRequestId(UUID execReqId);

    @Query(fields = "{'uuid': 1, 'rootCauseId': 1}")
    List<TestRun> findAllTestRunRootCausesByExecutionRequestId(UUID execReqId);

    @Query(fields = "{uuid: 1, testCaseId: 1}")
    List<TestRun> findTestCaseIdByExecutionRequestId(UUID execReqId);

    @Query(value = "{executionRequestId: ?0, name: {$ne: ?1}}", fields = "{'rootCauseId': 1, 'testingStatus': 1}")
    List<TestRun> findRootCauseIdTestingStatusByExecutionRequestIdAndNameNot(UUID executionRequestId,
                                                                             String excludeName);

    @Query("{executionRequestId: ?0, name: {$ne: ?1}}")
    List<TestRun> findAllByExecutionRequestIdAndNameNot(UUID execReqId, String excludeName);

    void deleteAllByUuidIn(List<UUID> uuid);

    List<TestRun> findAllByExecutionRequestIdIn(List<UUID> executionRequestId);

    Long countAllByExecutionRequestIdAndExecutionStatusIn(UUID executionRequestId,
                                                          List<ExecutionStatuses> executionStatuses);

    @Query(fields = "{'finishDate': 1}")
    TestRun findFinishDateByExecutionRequestIdAndFinishDateIsNotNullOrderByFinishDateDesc(UUID executionRequestsId);

    @Query(fields = "{'testCaseName': 1}")
    List<TestRun> findAllTestCasesNamesByExecutionRequestIdIn(List<UUID> executionRequestsId);

    TestRun findByExecutionRequestIdAndName(UUID executionRequestsId, String name);

    List<TestRun> findAllByExecutionRequestIdAndTestingStatusIsNotNull(UUID executionRequestId);

    List<TestRun> findAllByExecutionRequestIdAndExecutionStatusIn(UUID executionRequestId,
                                                                  List<ExecutionStatuses> executionStatuses);

    @Query("{executionRequestId: ?0, testingStatus: {$ne: ?1}}")
    List<TestRun> findByExecutionRequestIdAndTestingStatus(UUID requestId, TestingStatuses notEqualStatus);

    List<TestRun> findAllByExecutionRequestIdAndNameContains(UUID requestId, String searchValue);

    @Query(fields = "{'name': 1, 'parentTestRunId': 1}")
    TestRun findNameParentIdByUuid(UUID uuid);

    @Query(fields = "{'uuid': 1, executionRequestId: 1, order: 1}")
    List<TestRun> findAllByUuidInAndExecutionStatusIn(List<UUID> uuid, List<ExecutionStatuses> executionStatus);

    Long countAllByTestingStatusAndParentTestRunId(TestingStatuses testingStatuses, UUID parentId);

    Long countAllByParentTestRunId(UUID parentId);

    List<TestRun> findAllByParentTestRunId(UUID parentTestRunId);

    @Query(fields = "{'uuid': 1, 'name': 1, 'testingStatus': 1}")
    List<TestRun> findAllByExecutionRequestIdAndIsGroupedTestRun(UUID executionRequestId, boolean isGroupedTestRun);

    List<TestRun> findAllByUuidIn(Collection<UUID> ids);

    @Query(fields = "{'uuid': 1, 'name': 1, 'testingStatus': 1, 'executionStatus': 1}")
    List<TestRun> findShortTestRunsByUuidIn(Collection<UUID> ids);

    List<TestRun> findAllByExecutionRequestIdOrderByStartDateAsc(UUID requestId);

    @Query(fields = "{'passedRate': 1, 'warningRate': 1, 'failedRate': 1, 'testingStatus': 1}")
    List<TestRun> findAllRatesByUuidIn(Collection<UUID> testRunsIds);

    Long countAllByExecutionRequestId(UUID executionRequestId);

    @Query(fields = "{'name': 1, 'uuid': 1, 'testingStatus': 1, 'passedRate': 1, 'duration': 1, 'dataSetUrl': 1, "
            + "'rootCauseId': 1, 'testCaseId': 1, 'dataSetListUrl': 1, 'jiraTicket': 1}")
    List<TestRun> findTestRunForReportByUuidIn(Set<UUID> testRunIds);

    @Query(fields = "{'name': 1, 'uuid': 1, 'testingStatus': 1, 'passedRate': 1, 'duration': 1, 'dataSetUrl': 1, "
            + "'dataSetListUrl': 1, 'rootCauseId': 1, 'testCaseId':1, 'dataSetListUrl': 1, 'jiraTicket': 1}")
    List<TestRun> findTestRunForReportByExecutionRequestId(UUID executionRequestId);

    @Query(fields = "{'name': 1, 'uuid': 1, 'testingStatus': 1, 'fdrWasSent': 1, 'executionStatus': 1}")
    List<TestRun> findTestRunsForFdrByExecutionRequestId(UUID executionRequestId);

    @Query(fields = "{'testingStatus': 1, 'urlToBrowserSession': 1, 'executionStatus': 1, 'testCaseId': 1, "
            + "'initialTestRunId': 1, 'labelIds': 1}")
    List<TestRun> findTestRunForExecutionSummaryByExecutionRequestId(UUID executionRequestId);

    @Query(fields = "{'uuid': 1, 'testingStatus': 1}")
    List<TestRun> findTestRunsUuidAndTestingStatusByExecutionRequestId(UUID executionRequestId);

    @Query(fields = "{'uuid': 1}")
    List<TestRun> findTestRunsUuidByExecutionRequestId(UUID executionRequestId);


    @Query(fields = "{'executionStatus': 1, 'name': 1, 'passedRate': 1, 'reportLabelParams': 1, 'testingStatus': 1}")
    TestRun findTestRunForTreeNodeByUuid(UUID testRunId);

    @Query(fields = "{'executionRequestId': 1}")
    TestRun findTestRunExecReqIdByUuid(UUID testRunId);

    List<TestRun> findAllByRootCauseId(UUID rootCauseId);

    @Query(fields = "{'executionRequestId': 1, 'testingStatus': 1}")
    TestRun findTestRunExecReqIdAndTestStatusByUuid(UUID testRunId);

    @Query(fields = "{'uuid': 1}")
    List<TestRun> findTestRunsIdByExecutionRequestIdAndTestingStatus(UUID executionRequestId,
                                                                 TestingStatuses testingStatus);

    @Query(fields = "{'uuid':1}")
    List<TestRun> findTestRunsByExecutionRequestIdAndTestingStatusIn(UUID executionRequestId,
                                                                     List<TestingStatuses> testingStatuses);

    @Query(fields = "{'uuid': 1, 'executionRequestId': 1}")
    List<TestRun> findTestRunsUuidErIdByTestingStatusAndUuidIn(TestingStatuses testingStatuses,
                                                                            List<UUID> testRunIds);

    @Query(fields = "{'uuid': 1, 'executionRequestId': 1}")
    List<TestRun> findTestRunsUuidErIdByTestingStatusInAndUuidIn(List<TestingStatuses> testingStatuses,
                                                                 List<UUID> testRunIds);

    @Query(fields = "{'uuid': 1, 'testingStatus': 1, 'name': 1}")
    List<TestRun> findTestRunsIdNameTestingStatusByExecutionRequestId(UUID executionRequestId);

    @Query(fields = "{'uuid': 1, 'name': 1}")
    List<TestRun> findTestRunsIdNameByExecutionRequestId(UUID executionRequestId);

    List<EnrichedTestRun> findAllEnrichedTestRunsByExecutionRequestId(UUID execReqId);

    List<TestRunsRatesResponse> findTestRunsRatesResponseByExecutionRequestId(UUID execReqId);

    @Query(fields = "{'uuid': 1}")
    List<TestRun> findTestRunsIdByExecutionRequestId(UUID executionRequestId);

    @Query(value = "{'executionRequestId': ?0, 'numberOfScreens': {$exists: true}}")
    List<TestRun> findTestRunsByExecutionRequestId(UUID execReqId);

    @Query(fields = "{'executionRequestId': 1}")
    TestRun findFirstByTestCaseId(UUID testCaseId);

    @Query(fields = "{'executionRequestId': 1}")
    List<TestRun> findErByUuidIn(List<UUID> testRunId);
}
