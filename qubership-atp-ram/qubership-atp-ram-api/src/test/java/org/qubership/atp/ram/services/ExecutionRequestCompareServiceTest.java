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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.Mockito;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.enums.TypeAction;
import org.qubership.atp.ram.model.CompareItem;
import org.qubership.atp.ram.model.CompareTestRunsDetailsRow;
import org.qubership.atp.ram.model.LogRecordCompareScreenshotResponse;
import org.qubership.atp.ram.model.LogRecordWithParentListResponse;
import org.qubership.atp.ram.model.ShortExecutionRequest;
import org.qubership.atp.ram.model.SubstepScreenshotResponse;
import org.qubership.atp.ram.model.TestRunDetailsCompareResponse;
import org.qubership.atp.ram.model.request.LogRecordCompareRequest;
import org.qubership.atp.ram.model.request.LogRecordCompareRequestItem;
import org.qubership.atp.ram.model.request.RowScreenshotRequest;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.MetaInfo;
import org.qubership.atp.ram.models.RamObject;
import org.qubership.atp.ram.models.TestPlan;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.testdata.CompareEntitiesMock;
import org.qubership.atp.ram.testdata.TestRunServiceMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = ExecutionRequestCompareService.class,
        properties = {"spring.cloud.consul.config.enabled=false"})
@MockBeans({
        @MockBean(LogRecordService.class),
        @MockBean(ExecutionRequestService.class),
        @MockBean(EnvironmentsService.class),
        @MockBean(RootCauseService.class),
})
@Isolated
public class ExecutionRequestCompareServiceTest {

    @Autowired
    private ExecutionRequestCompareService executionRequestCompareService;
    @MockBean
    private ExecutionRequestService executionRequestService;
    @MockBean
    private LogRecordService logRecordService;
    @MockBean
    private TestPlansService testPlansService;

    private ExecutionRequest executionRequest1;
    private ExecutionRequest executionRequest2;

    private List<UUID> executionRequestIds;
    private TestRun testRun1;
    private TestRun testRun2;
    private TestRun testRun3;
    private TestRun testRun4;
    private Map<UUID, List<TestRun>> entitiesMap;
    private final UUID definitionIdForMessageCompound = UUID.randomUUID();

    @BeforeEach
    public void setUp() {
        TestRunServiceMock testRunServiceMock = new TestRunServiceMock();

        executionRequest1 = new ExecutionRequest();
        executionRequest1.setUuid(UUID.randomUUID());
        executionRequest1.setName("ER_1");

        executionRequest2 = new ExecutionRequest();
        executionRequest2.setUuid(UUID.randomUUID());
        executionRequest2.setName("ER_2");

        executionRequestIds = new ArrayList<>();
        executionRequestIds.add(executionRequest1.getUuid());
        executionRequestIds.add(executionRequest2.getUuid());

        testRun1 = testRunServiceMock.generateTestRun("TestRun test1",
                executionRequest1.getUuid(),
                1, 10L, TestingStatuses.FAILED);
        testRun2 = testRunServiceMock.generateTestRun("TestRun test2",
                executionRequest1.getUuid(),
                2, 15L, TestingStatuses.PASSED);

        testRun3 = testRunServiceMock.generateTestRun("TestRun test1",
                executionRequest2.getUuid(),
                1, 20L, TestingStatuses.WARNING);
        testRun4 = testRunServiceMock.generateTestRun("TestRun test2",
                executionRequest2.getUuid(),
                2, 30L, TestingStatuses.FAILED);

        List<ExecutionRequest> executionRequests = new ArrayList<>();
        executionRequests.add(executionRequest1);
        executionRequests.add(executionRequest2);

        List<TestRun> testRuns1 = new ArrayList<>();
        testRuns1.add(testRun1);
        testRuns1.add(testRun2);

        List<TestRun> testRuns2 = new ArrayList<>();
        testRuns2.add(testRun3);
        testRuns2.add(testRun4);

        entitiesMap = new HashMap<>();
        entitiesMap.put(executionRequest1.getUuid(), testRuns1);
        entitiesMap.put(executionRequest2.getUuid(), testRuns2);

        when(executionRequestService
                .getExecutionRequestsByIds(executionRequestIds))
                .thenReturn(executionRequests);
        when(executionRequestService
                .getMapTestRunsForExecutionRequests(executionRequestIds))
                .thenReturn(entitiesMap);

        when(executionRequestService.getAllTestRuns(executionRequest1.getUuid()))
                .thenReturn(testRuns1);
        when(executionRequestService.getAllTestRuns(executionRequest2.getUuid()))
                .thenReturn(testRuns2);
    }

    private LogRecord getLogrecord(String name, String hashSum) {
        LogRecord logRecord = new LogRecord();
        logRecord.setName(name);
        logRecord.setType(TypeAction.UI);
        MetaInfo metaInfo = new MetaInfo(UUID.randomUUID(), 2, hashSum, UUID.randomUUID(), false);
        logRecord.setMetaInfo(metaInfo);
        return logRecord;
    }

    @Test
    public void getLogRecordCompareResponse() {
        List<LogRecordCompareRequestItem> list = new ArrayList<>();
        LogRecordCompareRequestItem logRecordCompareRequestItem = new LogRecordCompareRequestItem();
        logRecordCompareRequestItem.setItemId(UUID.randomUUID());
        logRecordCompareRequestItem.setExecutionRequestId(UUID.randomUUID());
        list.add(logRecordCompareRequestItem);

        LogRecordCompareRequest request = new LogRecordCompareRequest();
        request.setLogRecordCompareRequestItems(list);
        request.setCompareType("TEST_RUN");


        List<LogRecord> recordList = new ArrayList<>();
        recordList.add(getLogrecord("Login", "15de90e9-fe48-444d-ba0b-06dbd9fe4252"));
        recordList.add(getLogrecord("Login", "15de90e9-fe48-444d-ba0b-06dbd9fe4252"));
        recordList.add(getLogrecord("Open", "7e9494ae-c77d-4409-b1c7-5b0b338adf4f"));
        recordList.add(getLogrecord("Open", "7e9494ae-c77d-4409-b1c7-5b0b338adf4f"));

        Mockito.when(logRecordService.findTopLogRecordsOnTestRun(any()))
                .thenReturn(recordList);


        TestRunDetailsCompareResponse testRunDetails = executionRequestCompareService.getLogRecordCompareResponse(request);
        Assertions.assertEquals(testRunDetails.getRowList().get(0).getName(), "Login");

    }

    @Test
    public void getCompareScreenshotsSubSteps_whenHaveListScreenShot() {
        RowScreenshotRequest rowScreenshotRequest = getRowScreenshotRequest();
        List<RowScreenshotRequest> row = new ArrayList<>();
        row.add(rowScreenshotRequest);

        SubstepScreenshotResponse response = new SubstepScreenshotResponse();
        response.setId(UUID.fromString("5c9cfcf9-9f1e-4693-b2f7-492ff17a2ee6"));
        SubstepScreenshotResponse response2 = new SubstepScreenshotResponse();
        response2.setId(UUID.fromString("7e9494ae-c77d-4409-b1c7-5b0b338adf4f"));
        List<SubstepScreenshotResponse> screenshotResponseList = new ArrayList<>();
        screenshotResponseList.add(response);
        screenshotResponseList.add(response2);

        Mockito.when(logRecordService.getSubstepScreenshots(any())).thenReturn(screenshotResponseList);

        List<SubstepScreenshotResponse> substepScreenshotResponseList = executionRequestCompareService.getCompareScreenshotsSubSteps(row);

        Assertions.assertEquals(UUID.fromString("5c9cfcf9-9f1e-4693-b2f7-492ff17a2ee6"), substepScreenshotResponseList.get(0).getId());
        Assertions.assertEquals(UUID.fromString("7e9494ae-c77d-4409-b1c7-5b0b338adf4f"), substepScreenshotResponseList.get(1).getId());
    }

    @Test
    public void getCompareScreenshotsSubSteps_whenRowScreenshotRequestIsEmpry() {
        List<RowScreenshotRequest> row = new ArrayList<>();

        List<SubstepScreenshotResponse> substepScreenshotResponseList = executionRequestCompareService.getCompareScreenshotsSubSteps(row);

        Assertions.assertEquals(new ArrayList<>(), substepScreenshotResponseList);
    }

    @Test
    public void getTestRunDetailsCompareResponses_WhenTestPlansAreIdentical_ShouldValidComparingByTestCasesId() {
        UUID testPlanId = UUID.randomUUID();
        executionRequest1.setTestPlanId(testPlanId);
        executionRequest2.setTestPlanId(testPlanId);
        UUID testCaseId1 = UUID.randomUUID();
        UUID testCaseId2 = UUID.randomUUID();
        testRun1.setTestCaseId(testCaseId1);
        testRun2.setTestCaseId(testCaseId2);
        testRun3.setTestCaseId(testCaseId1);
        testRun4.setTestCaseId(testCaseId2);

        TestRunDetailsCompareResponse expectedResponse = generateExpectedResult();
        TestRunDetailsCompareResponse actualResponse =
                executionRequestCompareService.getTestRunDetailsCompareResponses(executionRequestIds);
        Assertions.assertEquals(expectedResponse, actualResponse);
    }

    @Test
    public void getTestRunDetailsCompareResponses_WhenTestPlansAreDifferent_ShouldValidComparingByTestRunsName() {
        executionRequest1.setTestPlanId(UUID.randomUUID());
        executionRequest2.setTestPlanId(UUID.randomUUID());
        testRun1.setTestCaseId(UUID.randomUUID());
        testRun2.setTestCaseId(UUID.randomUUID());
        testRun3.setTestCaseId(UUID.randomUUID());
        testRun4.setTestCaseId(UUID.randomUUID());

        TestRunDetailsCompareResponse expectedResponse = generateExpectedResult();
        TestRunDetailsCompareResponse actualResponse =
                executionRequestCompareService.getTestRunDetailsCompareResponses(executionRequestIds);
        Assertions.assertEquals(expectedResponse, actualResponse);
    }

    private TestRunDetailsCompareResponse generateExpectedResult() {
        CompareEntitiesMock mock = new CompareEntitiesMock();
        TestRunDetailsCompareResponse testRunDetailsCompareResponse = new TestRunDetailsCompareResponse();

        ShortExecutionRequest shortExecutionRequest1 =
                new ShortExecutionRequest(executionRequest1.getUuid(),
                        executionRequest1.getName());
        ShortExecutionRequest shortExecutionRequest2 =
                new ShortExecutionRequest(executionRequest2.getUuid(),
                        executionRequest2.getName());
        testRunDetailsCompareResponse
                .setExecutionRequests(Arrays.asList(shortExecutionRequest1, shortExecutionRequest2));

        CompareTestRunsDetailsRow row1 = mock.generateRow(testRun1, testRun3);
        CompareTestRunsDetailsRow row2 = mock.generateRow(testRun2, testRun4);

        testRunDetailsCompareResponse.setRowList(Arrays.asList(row1, row2));
        return testRunDetailsCompareResponse;
    }

    @Test
    public void createComparedMatrix_FirstTrHasSameNumberOfLogRecordsButDifferentOrder_ComparedMatrixHasThreeRows() {
        LogRecordWithParentListResponse login = generateOneLogRecordWithParents(
                "Login", "parent1", "parent2", "ad40234c3d778b9b30988a25b43d5ac5");
        LogRecordWithParentListResponse open = generateOneLogRecordWithParents(
                "Open", "parent1", "parent2", "36efd40c1e75dcc63fd85c9d4cc44ab2");
        LogRecordWithParentListResponse navigate = generateOneLogRecordWithParents(
                "Navigate", "parent1", "parent2", "3f063032731fff4a14523df97c1383e3");

        List<LogRecordWithParentListResponse> logRecords1 = new LinkedList<>();
        logRecords1.add(login);
        logRecords1.add(open);
        logRecords1.add(navigate);

        List<LogRecordWithParentListResponse> logRecords2 = new LinkedList<>();
        logRecords2.add(open);
        logRecords2.add(login);
        logRecords2.add(navigate);

        when(logRecordService.findLogRecordsWithParentsByPreviewExists(eq(testRun1.getUuid()))).thenReturn(logRecords1);
        when(logRecordService.findLogRecordsWithParentsByPreviewExists(eq(testRun3.getUuid()))).thenReturn(logRecords2);
        LinkedHashMap<UUID, TestRun> map = new LinkedHashMap<>();
        map.put(executionRequest1.getUuid(), testRun1);
        map.put(executionRequest2.getUuid(), testRun3);

        List<List<CompareItem>> comparedItemsMatrix = executionRequestCompareService.createComparedMatrix(map);

        Assertions.assertEquals(4, comparedItemsMatrix.size());
        List<CompareItem> item1 = comparedItemsMatrix.get(0);
        Assertions.assertEquals("Login", item1.get(0).getItemValue().getName());

        List<CompareItem> item2 = comparedItemsMatrix.get(1);
        Assertions.assertEquals("Open", item2.get(0).getItemValue().getName());
        Assertions.assertEquals("Open", item2.get(1).getItemValue().getName());

        List<CompareItem> item3 = comparedItemsMatrix.get(2);
        Assertions.assertEquals("Login", item3.get(1).getItemValue().getName());

        List<CompareItem> item4 = comparedItemsMatrix.get(3);
        Assertions.assertEquals("Navigate", item4.get(0).getItemValue().getName());
        Assertions.assertEquals("Navigate", item4.get(1).getItemValue().getName());
    }

    @Test
    public void createComparedMatrix_FirstTrHasMoreLogRecords_ComparedMatrixHasThreeRows() {
        LogRecordWithParentListResponse login = generateOneLogRecordWithParents("Login", "parent1", "parent2", "ad40234c3d778b9b30988a25b43d5ac5");
        LogRecordWithParentListResponse open = generateOneLogRecordWithParents("Open", "parent1", "parent2", "36efd40c1e75dcc63fd85c9d4cc44ab2");
        LogRecordWithParentListResponse navigate = generateOneLogRecordWithParents("Navigate", "parent1", "parent2", "3f063032731fff4a14523df97c1383e3");

        List<LogRecordWithParentListResponse> logRecords1 = new LinkedList<>();
        logRecords1.add(login);
        logRecords1.add(open);
        logRecords1.add(navigate);

        List<LogRecordWithParentListResponse> logRecords2 = new LinkedList<>();
        logRecords2.add(open);
        logRecords2.add(navigate);

        when(logRecordService.findLogRecordsWithParentsByPreviewExists(eq(testRun1.getUuid()))).thenReturn(logRecords1);
        when(logRecordService.findLogRecordsWithParentsByPreviewExists(eq(testRun3.getUuid()))).thenReturn(logRecords2);
        LinkedHashMap<UUID, TestRun> map = new LinkedHashMap<>();
        map.put(executionRequest1.getUuid(), testRun1);
        map.put(executionRequest2.getUuid(), testRun3);

        List<List<CompareItem>> comparedItemsMatrix = executionRequestCompareService.createComparedMatrix(map);

        Assertions.assertEquals(3, comparedItemsMatrix.size());
        List<CompareItem> item1 = comparedItemsMatrix.get(0);
        Assertions.assertEquals("Login", item1.get(0).getItemValue().getName());
        Assertions.assertTrue(Objects.isNull(item1.get(1).getItemValue()));

        List<CompareItem> item2 = comparedItemsMatrix.get(1);
        Assertions.assertEquals("Open", item2.get(0).getItemValue().getName());
        Assertions.assertEquals("Open", item2.get(1).getItemValue().getName());

        List<CompareItem> item3 = comparedItemsMatrix.get(2);
        Assertions.assertEquals("Navigate", item3.get(0).getItemValue().getName());
        Assertions.assertEquals("Navigate", item3.get(1).getItemValue().getName());
    }

    @Test
    public void createComparedMatrix_FirstTrHasSameNumberOfLogRecordsButDifferentOrder_ComparedMatrixHasSevenRows() {
        LogRecordWithParentListResponse login = generateOneLogRecordWithParents("Login", "parent1", "parent2", "ad40234c3d778b9b30988a25b43d5ac5");
        LogRecordWithParentListResponse open = generateOneLogRecordWithParents("Open", "parent1", "parent2", "36efd40c1e75dcc63fd85c9d4cc44ab2");
        LogRecordWithParentListResponse navigate = generateOneLogRecordWithParents("Navigate", "parent1", "parent2", "3f063032731fff4a14523df97c1383e3");
        LogRecordWithParentListResponse error = generateOneLogRecordWithParents("error", "parent1", "parent2", "");

        List<LogRecordWithParentListResponse> logRecords1 = new LinkedList<>();
        logRecords1.add(login);
        logRecords1.add(open);
        logRecords1.add(navigate);
        logRecords1.add(open);
        logRecords1.add(open);

        List<LogRecordWithParentListResponse> logRecords2 = new LinkedList<>();
        logRecords2.add(open);
        logRecords2.add(error);
        logRecords2.add(login);
        logRecords2.add(open);

        when(logRecordService.findLogRecordsWithParentsByPreviewExists(eq(testRun1.getUuid()))).thenReturn(logRecords1);
        when(logRecordService.findLogRecordsWithParentsByPreviewExists(eq(testRun3.getUuid()))).thenReturn(logRecords2);
        LinkedHashMap<UUID, TestRun> map = new LinkedHashMap<>();
        map.put(executionRequest1.getUuid(), testRun1);
        map.put(executionRequest2.getUuid(), testRun3);

        List<List<CompareItem>> comparedItemsMatrix = executionRequestCompareService.createComparedMatrix(map);

        Assertions.assertEquals(7, comparedItemsMatrix.size());
        Assertions.assertEquals("Login", comparedItemsMatrix.get(0).get(0).getItemValue().getName());

        Assertions.assertEquals("Open", comparedItemsMatrix.get(1).get(0).getItemValue().getName());
        Assertions.assertEquals("Open", comparedItemsMatrix.get(1).get(1).getItemValue().getName());

        Assertions.assertEquals("error", comparedItemsMatrix.get(2).get(1).getItemValue().getName());

        Assertions.assertEquals("Navigate", comparedItemsMatrix.get(3).get(0).getItemValue().getName());

        Assertions.assertEquals("Login", comparedItemsMatrix.get(4).get(1).getItemValue().getName());

        Assertions.assertEquals("Open", comparedItemsMatrix.get(5).get(0).getItemValue().getName());
        Assertions.assertEquals("Open", comparedItemsMatrix.get(5).get(1).getItemValue().getName());

        Assertions.assertEquals("Open", comparedItemsMatrix.get(6).get(0).getItemValue().getName());
    }

    @Test
    public void createComparedMatrix_FirstTrHasFewerLogRecords_ComparedMatrixHasThreeRows() {
        LogRecordWithParentListResponse login = generateOneLogRecordWithParents("Login", "parent1", "parent2", "ad40234c3d778b9b30988a25b43d5ac5");
        LogRecordWithParentListResponse open = generateOneLogRecordWithParents("Open", "parent1", "parent2", "36efd40c1e75dcc63fd85c9d4cc44ab2");
        LogRecordWithParentListResponse navigate = generateOneLogRecordWithParents("Navigate", "parent1", "parent2", "3f063032731fff4a14523df97c1383e3");

        List<LogRecordWithParentListResponse> logRecords1 = new LinkedList<>();
        logRecords1.add(open);
        logRecords1.add(navigate);

        List<LogRecordWithParentListResponse> logRecords2 = new LinkedList<>();
        logRecords2.add(login);
        logRecords2.add(open);
        logRecords2.add(navigate);

        when(logRecordService.findLogRecordsWithParentsByPreviewExists(eq(testRun1.getUuid()))).thenReturn(logRecords1);
        when(logRecordService.findLogRecordsWithParentsByPreviewExists(eq(testRun3.getUuid()))).thenReturn(logRecords2);
        LinkedHashMap<UUID, TestRun> map = new LinkedHashMap<>();
        map.put(executionRequest1.getUuid(), testRun1);
        map.put(executionRequest2.getUuid(), testRun3);

        List<List<CompareItem>> comparedItemsMatrix = executionRequestCompareService.createComparedMatrix(map);

        Assertions.assertEquals(3, comparedItemsMatrix.size());
        List<CompareItem> item1 = comparedItemsMatrix.get(0);
        Assertions.assertTrue(Objects.isNull(item1.get(0).getItemValue()));
        Assertions.assertEquals("Login", item1.get(1).getItemValue().getName());

        List<CompareItem> item2 = comparedItemsMatrix.get(1);
        Assertions.assertEquals("Open", item2.get(0).getItemValue().getName());
        Assertions.assertEquals("Open", item2.get(1).getItemValue().getName());

        List<CompareItem> item3 = comparedItemsMatrix.get(2);
        Assertions.assertEquals("Navigate", item3.get(0).getItemValue().getName());
        Assertions.assertEquals("Navigate", item3.get(1).getItemValue().getName());
    }

    @Test
    public void createComparedMatrix_StepsForComparingWithSameParentsButDifferentNames_FirstStepsNotEqualAndAreInDifferentRowsOfMatrix() {
        List<LogRecordWithParentListResponse> logRecords1 =
                generateTwoLogRecord("Login", "Open",
                        "ParentLogin1", "ParentLogin2",
                        "ParentLogin1123", "ParentLogin1123", definitionIdForMessageCompound.toString());
        List<LogRecordWithParentListResponse> logRecords2 =
                generateTwoLogRecord("Login123", "Open",
                        "ParentLogin1", "ParentLogin2",
                        "ParentLogin1123", "ParentLogin1123", definitionIdForMessageCompound.toString());
        when(logRecordService.findLogRecordsWithParentsByPreviewExists(eq(testRun1.getUuid()))).thenReturn(logRecords1);
        when(logRecordService.findLogRecordsWithParentsByPreviewExists(eq(testRun3.getUuid()))).thenReturn(logRecords2);
        LinkedHashMap<UUID, TestRun> map = new LinkedHashMap<>();
        map.put(executionRequest1.getUuid(), testRun1);
        map.put(executionRequest2.getUuid(), testRun3);

        List<List<CompareItem>> comparedItemsMatrix = executionRequestCompareService.createComparedMatrix(map);

        Assertions.assertEquals(2, comparedItemsMatrix.size());

        CompareItem item1 = comparedItemsMatrix.get(0).get(0);
        String path = definitionIdForMessageCompound + " / ";
        checkCompareItem(executionRequest1.getUuid(), path, "Login", item1);
        Assertions.assertFalse(Objects.isNull(item1.getHashSumForCompare()));

        CompareItem item2 = comparedItemsMatrix.get(0).get(1);
        checkCompareItem(executionRequest2.getUuid(), path, "Login123", item2);
        Assertions.assertFalse(Objects.isNull(item2.getHashSumForCompare()));

        CompareItem item3 = comparedItemsMatrix.get(1).get(0);
        checkCompareItem(executionRequest1.getUuid(), path, "Open", item3);

        CompareItem item4 = comparedItemsMatrix.get(1).get(1);
        checkCompareItem(executionRequest2.getUuid(), path, "Open", item4);
    }

    @Test
    public void createComparedMatrix_StepsForComparingWithDifferentParentsAndSameNames_FirstStepsNotEqualAndAreInDifferentRowsOfMatrix() {
        List<LogRecordWithParentListResponse> logRecords1 =
                generateTwoLogRecord("Login", "Open",
                        "ParentLogin1", "ParentLogin2",
                        "ParentLogin1123", "ParentLogin1123", definitionIdForMessageCompound.toString());
        List<LogRecordWithParentListResponse> logRecords2 =
                generateTwoLogRecord("Login", "Open",
                        "ParentLogin3", "ParentLogin4",
                        "ParentLogin1123", "ParentLogin1123", definitionIdForMessageCompound.toString());
        when(logRecordService.findLogRecordsWithParentsByPreviewExists(eq(testRun1.getUuid()))).thenReturn(logRecords1);
        when(logRecordService.findLogRecordsWithParentsByPreviewExists(eq(testRun3.getUuid()))).thenReturn(logRecords2);
        LinkedHashMap<UUID, TestRun> map = new LinkedHashMap<>();
        map.put(executionRequest1.getUuid(), testRun1);
        map.put(executionRequest2.getUuid(), testRun3);

        List<List<CompareItem>> comparedItemsMatrix = executionRequestCompareService.createComparedMatrix(map);

        Assertions.assertEquals(2, comparedItemsMatrix.size());

        CompareItem item1 = comparedItemsMatrix.get(0).get(0);
        String path = definitionIdForMessageCompound + " / ";
        checkCompareItem(executionRequest1.getUuid(), path, "Login", item1);

        CompareItem item2 = comparedItemsMatrix.get(1).get(1);
        checkCompareItem(executionRequest2.getUuid(), path, "Open", item2);
    }

    @Test
    public void createComparedMatrix_StepsForComparingWithSameParentsAndNames_FirstStepsNotEqualAndAreInDifferentRowsOfMatrix() {
        List<LogRecordWithParentListResponse> logRecords1 =
                generateTwoLogRecord("Login", "Open",
                        "ParentLogin1", "ParentLogin2",
                        "ParentLogin1123", "ParentLogin1123", definitionIdForMessageCompound.toString());
        List<LogRecordWithParentListResponse> logRecords2 =
                generateTwoLogRecord("Login", "Open",
                        "ParentLogin1", "ParentLogin2",
                        "ParentLogin1123", "ParentLogin1123", definitionIdForMessageCompound.toString());
        when(logRecordService.findLogRecordsWithParentsByPreviewExists(eq(testRun1.getUuid()))).thenReturn(logRecords1);
        when(logRecordService.findLogRecordsWithParentsByPreviewExists(eq(testRun3.getUuid()))).thenReturn(logRecords2);
        LinkedHashMap<UUID, TestRun> map = new LinkedHashMap<>();
        map.put(executionRequest1.getUuid(), testRun1);
        map.put(executionRequest2.getUuid(), testRun3);

        List<List<CompareItem>> comparedItemsMatrix = executionRequestCompareService.createComparedMatrix(map);

        Assertions.assertEquals(2, comparedItemsMatrix.size());

        CompareItem item11 = comparedItemsMatrix.get(0).get(0);
        String path = definitionIdForMessageCompound + " / ";
        checkCompareItem(executionRequest1.getUuid(), path, "Login", item11);

        CompareItem item12 = comparedItemsMatrix.get(0).get(1);
        checkCompareItem(executionRequest2.getUuid(), path, "Login", item12);

        CompareItem item21 = comparedItemsMatrix.get(1).get(0);
        checkCompareItem(executionRequest1.getUuid(), path, "Open", item21);

        CompareItem item22 = comparedItemsMatrix.get(1).get(1);
        checkCompareItem(executionRequest2.getUuid(), path, "Open", item22);
    }

    @Test
    public void createLogRecordsTree_ComparedMatrixWithSameSteps_ReturnValidLogRecordsTree() {
        LinkedList<List<CompareItem>> comparedMatrix = generateComparedLogRecordsMatrix();
        List<RamObject> testPlanInfoList = generateTestPlanInfoList();
        LinkedList<LogRecordCompareScreenshotResponse> topLogRecordsList = new LinkedList<>();

        executionRequestCompareService.createLogRecordsTree(comparedMatrix, testPlanInfoList, topLogRecordsList, false);

        Assertions.assertEquals(3, topLogRecordsList.size());

        LogRecordCompareScreenshotResponse topLogRecord1 = topLogRecordsList.get(0);
        Assertions.assertEquals("message", topLogRecord1.getName());
        Assertions.assertEquals(1, topLogRecord1.getChild().size());
        Assertions.assertEquals("Login / Login", topLogRecord1.getChild().get(0).getName());
        Assertions.assertEquals(1, topLogRecord1.getChild().get(0).getChild().size());
        Assertions.assertEquals("Successful login", topLogRecord1.getChild().get(0).getChild().get(0).getSubStepName());
        Assertions.assertEquals("ACTION", topLogRecord1.getChild().get(0).getChild().get(0).getType());

        LogRecordCompareScreenshotResponse topLogRecord2 = topLogRecordsList.get(1);
        Assertions.assertEquals("message", topLogRecord2.getName());
        Assertions.assertEquals(1, topLogRecord2.getChild().size());
        Assertions.assertEquals("ACTION", topLogRecord2.getChild().get(0).getType());
    }

    @Test
    public void createLogRecordsTreeWithScreenshotsContent_ReturnValidLogRecordsTreeWithScreenshots() {
        LinkedList<List<CompareItem>> comparedMatrix = generateComparedLogRecordsMatrix();
        List<RamObject> testPlanInfoList = generateTestPlanInfoList();
        LinkedList<LogRecordCompareScreenshotResponse> topLogRecordsList = new LinkedList<>();

        SubstepScreenshotResponse response = new SubstepScreenshotResponse();
        response.setId(UUID.fromString("5c9cfcf9-9f1e-4693-b2f7-492ff17a2ee6"));
        String screenshotContent = "screenshot";
        response.setScreenshot(Base64.encodeBase64String(screenshotContent.getBytes()));
        Mockito.when(logRecordService.getSubstepScreenshots(any())).thenReturn(Collections.singletonList(response));

        executionRequestCompareService.createLogRecordsTree(comparedMatrix, testPlanInfoList, topLogRecordsList, true);

        Assertions.assertEquals(3, topLogRecordsList.size());

        LogRecordCompareScreenshotResponse topLogRecord1 = topLogRecordsList.get(0);
        Assertions.assertEquals("message", topLogRecord1.getName());
        Assertions.assertEquals(1, topLogRecord1.getChild().size());
        Assertions.assertEquals("Login / Login", topLogRecord1.getChild().get(0).getName());
        Assertions.assertEquals(1, topLogRecord1.getChild().get(0).getChild().size());
        Assertions.assertEquals("Successful login", topLogRecord1.getChild().get(0).getChild().get(0).getSubStepName());

        // screenshots content comparison
        Assertions.assertEquals(screenshotContent,
                new String(Base64.decodeBase64(topLogRecord1
                        .getChild().get(0).getChild().get(0).getRow().get(0).getScreenshot()), StandardCharsets.UTF_8));
        Assertions.assertEquals(screenshotContent,
                new String(Base64.decodeBase64(topLogRecord1
                        .getChild().get(0).getChild().get(0).getRow().get(1).getScreenshot()), StandardCharsets.UTF_8));

        LogRecordCompareScreenshotResponse topLogRecord2 = topLogRecordsList.get(1);
        Assertions.assertEquals("message", topLogRecord2.getName());
        Assertions.assertEquals(1, topLogRecord2.getChild().size());
        Assertions.assertEquals("ACTION", topLogRecord2.getChild().get(0).getType());

        // screenshots content comparison
        Assertions.assertEquals(screenshotContent,
                new String(Base64.decodeBase64(topLogRecord2
                        .getChild().get(0).getChild().get(0).getRow().get(0).getScreenshot()), StandardCharsets.UTF_8));
        Assertions.assertEquals(screenshotContent,
                new String(Base64.decodeBase64(topLogRecord2
                        .getChild().get(0).getChild().get(0).getRow().get(1).getScreenshot()), StandardCharsets.UTF_8));
    }

    @Test
    public void createLogRecordsTree_ComparedMatrixWithDifferentSteps_ReturnValidLogRecordsTree() {
        LinkedList<List<CompareItem>> comparedMatrix = generateComparedDifferentLogRecordsMatrix();
        List<RamObject> testPlanInfoList = generateTestPlanInfoList();
        LinkedList<LogRecordCompareScreenshotResponse> topLogRecordsList = new LinkedList<>();

        executionRequestCompareService.createLogRecordsTree(comparedMatrix, testPlanInfoList, topLogRecordsList, false);

        Assertions.assertEquals(4, topLogRecordsList.size());

        LogRecordCompareScreenshotResponse topLogRecord1 = topLogRecordsList.get(0);
        Assertions.assertEquals("message", topLogRecord1.getName());
        Assertions.assertEquals(1, topLogRecord1.getChild().size());
        Assertions.assertEquals("Login / Login", topLogRecord1.getChild().get(0).getName());
        Assertions.assertEquals(1, topLogRecord1.getChild().get(0).getChild().size());

        LogRecordCompareScreenshotResponse subStep1 = topLogRecord1.getChild().get(0).getChild().get(0);
        Assertions.assertEquals("Successful login", subStep1.getSubStepName());
        Assertions.assertEquals(2, subStep1.getRow().size());
        Assertions.assertTrue(Objects.isNull(subStep1.getRow().get(1).getId()));

        LogRecordCompareScreenshotResponse topLogRecord2 = topLogRecordsList.get(1);
        Assertions.assertEquals("message", topLogRecord2.getName());
        Assertions.assertEquals(1, topLogRecord2.getChild().size());
        Assertions.assertEquals("ACTION", topLogRecord2.getChild().get(0).getType());
        Assertions.assertEquals("Login / Login", topLogRecord2.getChild().get(0).getName());
    }

    @Test
    public void createLogRecordsTree_ComparedMatrixWithStepWithThreeParents_ReturnValidLogRecordsTree() {
        LinkedList<List<CompareItem>> comparedMatrix =
                generateComparedLogRecordsMatrixForOneLogRecordWithThreeParents();
        List<RamObject> testPlanInfoList = generateTestPlanInfoList();
        LinkedList<LogRecordCompareScreenshotResponse> topLogRecordsList = new LinkedList<>();

        executionRequestCompareService.createLogRecordsTree(comparedMatrix, testPlanInfoList, topLogRecordsList, false);

        Assertions.assertEquals(1, topLogRecordsList.size());

        LogRecordCompareScreenshotResponse topLogRecord1 = topLogRecordsList.get(0);
        Assertions.assertEquals("Compound 2", topLogRecord1.getName());
        Assertions.assertEquals(1, topLogRecord1.getChild().size());
        Assertions.assertEquals("message / Login / Login", topLogRecord1.getChild().get(0).getName());
        Assertions.assertEquals(1, topLogRecord1.getChild().get(0).getChild().size());
        Assertions.assertEquals("Successful login", topLogRecord1.getChild().get(0).getChild().get(0).getSubStepName());
    }

    private List<RamObject> generateTestPlanInfoList() {
        List<RamObject> testPlanInfoList = new LinkedList<>();
        RamObject testPlanMap1 = new TestPlan();
        testPlanMap1.setName("Test Plan 1");
        testPlanMap1.setUuid(UUID.randomUUID());
        testPlanInfoList.add(testPlanMap1);
        RamObject testPlanMap2 = new TestPlan();
        testPlanMap2.setName("Test Plan 2");
        testPlanMap2.setUuid(UUID.randomUUID());
        testPlanInfoList.add(testPlanMap2);

        return testPlanInfoList;
    }

    private LinkedList<List<CompareItem>> generateComparedLogRecordsMatrix() {
        LinkedList<List<CompareItem>> comparedMatrix = new LinkedList<>();
        List<LogRecordWithParentListResponse> logRecords = generateThreeLogRecord("Successful login",
                "Message", "Url was opened", "Login",
                "Compound", "ad40234c3d778b9b30988a25b43d5ac5");

        LinkedList<CompareItem> firstStep = new LinkedList<>();
        CompareItem item1 = new CompareItem(executionRequest1.getUuid(), "parent 2 / parent 1", 0, 0,
                logRecords.get(0), true, "");
        firstStep.add(item1);
        CompareItem item2 = new CompareItem(executionRequest2.getUuid(), "parent 2 / parent 1", 0, 0,
                logRecords.get(0), true, "");
        firstStep.add(item2);
        comparedMatrix.add(firstStep);

        LinkedList<CompareItem> secondStep = new LinkedList<>();
        CompareItem item3 = new CompareItem(executionRequest1.getUuid(), "parent 2 / parent 1", 1, 1,
                logRecords.get(1), true, "");
        secondStep.add(item3);
        CompareItem item4 = new CompareItem(executionRequest2.getUuid(), "parent 2 / parent 1", 1, 1,
                logRecords.get(1), true, "");
        secondStep.add(item4);
        comparedMatrix.add(secondStep);

        LinkedList<CompareItem> thirdStep = new LinkedList<>();
        CompareItem item5 = new CompareItem(executionRequest1.getUuid(), null, 2, 2,
                logRecords.get(2), true, "");
        thirdStep.add(item5);
        CompareItem item6 = new CompareItem(executionRequest2.getUuid(), null, 2, 2,
                logRecords.get(2), true, "");
        thirdStep.add(item6);
        comparedMatrix.add(thirdStep);

        return comparedMatrix;
    }

    private LinkedList<List<CompareItem>> generateComparedDifferentLogRecordsMatrix() {
        LinkedList<List<CompareItem>> comparedMatrix = new LinkedList<>();
        List<LogRecordWithParentListResponse> logRecords = generateThreeLogRecord("Successful login",
                "Message", "Url was opened", "Login",
                "Compound", "ad40234c3d778b9b30988a25b43d5ac5");

        LinkedList<CompareItem> firstStep = new LinkedList<>();
        CompareItem item1 = new CompareItem(executionRequest1.getUuid(), "parent 2 / parent 1", 0, 0,
                logRecords.get(0), true, "");
        firstStep.add(item1);
        firstStep.add(new CompareItem());
        comparedMatrix.add(firstStep);

        LinkedList<CompareItem> firstStep2 = new LinkedList<>();
        CompareItem item2 = new CompareItem(executionRequest2.getUuid(), "parent 2 / parent 1", 0, 0,
                generateOneLogRecordWithParents("Successful login 2", "Login", "Compound", "ad40234c3d778b9b30988a25b43d5ac5"),
                true, "");
        firstStep2.add(new CompareItem());
        firstStep2.add(item2);
        comparedMatrix.add(firstStep2);

        LinkedList<CompareItem> secondStep = new LinkedList<>();
        CompareItem item3 = new CompareItem(executionRequest1.getUuid(), "parent 2 / parent 1", 1, 1,
                logRecords.get(1), true, "");
        secondStep.add(item3);
        CompareItem item4 = new CompareItem(executionRequest2.getUuid(), "parent 2 / parent 1", 1, 1,
                logRecords.get(1), true, "");
        secondStep.add(item4);
        comparedMatrix.add(secondStep);

        LinkedList<CompareItem> thirdStep = new LinkedList<>();
        CompareItem item5 = new CompareItem(executionRequest1.getUuid(), null, 2, 2,
                logRecords.get(2), true, "");
        thirdStep.add(item5);
        CompareItem item6 = new CompareItem(executionRequest2.getUuid(), null, 2, 2,
                logRecords.get(2), true, "");
        thirdStep.add(item6);
        comparedMatrix.add(thirdStep);

        return comparedMatrix;
    }

    private LinkedList<List<CompareItem>> generateComparedLogRecordsMatrixForOneLogRecordWithThreeParents() {
        LinkedList<List<CompareItem>> comparedMatrix = new LinkedList<>();
        LogRecordWithParentListResponse logRecord = generateOneLogRecordWithParents("Successful login", "Login",
                "Compound 1", "ad40234c3d778b9b30988a25b43d5ac5");

        LogRecordWithParentListResponse.LogRecordParent logRecordParent =
                new LogRecordWithParentListResponse.LogRecordParent();
        logRecordParent.setUuid(UUID.randomUUID());
        logRecordParent.setName("Compound 2");
        logRecordParent.setType(TypeAction.COMPOUND);
        logRecordParent.setDepth(2);

        List<LogRecordWithParentListResponse.LogRecordParent> parents = logRecord.getParent();
        parents.add(logRecordParent);
        logRecord.setParent(parents);

        LinkedList<CompareItem> firstStep = new LinkedList<>();
        CompareItem item1 = new CompareItem(executionRequest1.getUuid(), "Compound 2 / Compound 1 / Login", 0, 0,
                logRecord, true, "");
        firstStep.add(item1);
        CompareItem item2 = new CompareItem(executionRequest2.getUuid(), "Compound 2 / Compound 1 / Login", 0, 0,
                logRecord, true, "");
        firstStep.add(item2);
        comparedMatrix.add(firstStep);

        return comparedMatrix;
    }

    private void checkCompareItem(UUID expectedErId, String expectedPath, String expectedName, CompareItem item) {
        Assertions.assertEquals(expectedErId, item.getExecutionRequestId());
        Assertions.assertEquals(expectedPath, item.getPath());
        Assertions.assertEquals(expectedName, item.getItemValue().getName());
    }

    private List<LogRecordWithParentListResponse> generateTwoLogRecord(String firstStepName, String secondStepName,
                                                                       String parentNameForFirstStep1,
                                                                       String parentNameForFirstStep2,
                                                                       String parentNameForSecondStep1,
                                                                       String parentNameForSecondStep2,
                                                                       String hashSum) {
        LogRecordWithParentListResponse logRecord = generateOneLogRecordWithParents(firstStepName,
                parentNameForFirstStep1, parentNameForFirstStep2, hashSum);

        LogRecordWithParentListResponse logRecord2 = generateOneLogRecordWithParents(secondStepName,
                parentNameForSecondStep1, parentNameForSecondStep2, hashSum);

        return Arrays.asList(logRecord, logRecord2);
    }

    private List<LogRecordWithParentListResponse> generateThreeLogRecord(String firstStepName, String secondStepName,
                                                                         String thirdStepName,
                                                                         String parentNameForFirstStep1,
                                                                         String parentNameForFirstStep2,
                                                                         String hashSum) {
        LogRecordWithParentListResponse subStep1 = generateOneLogRecordWithParents(firstStepName,
                parentNameForFirstStep1, parentNameForFirstStep2, hashSum);

        LogRecordWithParentListResponse subStep2 = generateOneLogRecordWithParents(secondStepName,
                parentNameForFirstStep1, parentNameForFirstStep2, hashSum);

        LogRecordWithParentListResponse subStep3 = generateOneLogRecordWithoutParents(thirdStepName, hashSum);
        LogRecordWithParentListResponse.LogRecordParent step =
                new LogRecordWithParentListResponse.LogRecordParent();
        step.setUuid(UUID.randomUUID());
        step.setName("Open");
        step.setType(TypeAction.UI);
        step.setDepth(0);
        List<LogRecordWithParentListResponse.LogRecordParent> parents = new ArrayList<>();
        parents.add(step);
        subStep3.setParent(parents);

        return Arrays.asList(subStep1, subStep2, subStep3);
    }

    private LogRecordWithParentListResponse generateOneLogRecordWithParents(String stepName,
                                                                            String parentName1,
                                                                            String parentName2,
                                                                            String hashSum) {
        UUID defUUID = UUID.randomUUID();
        LogRecordWithParentListResponse logRecord = new LogRecordWithParentListResponse();
        logRecord.setUuid(UUID.randomUUID());
        logRecord.setName(stepName);
        logRecord.setTestingStatus(TestingStatuses.PASSED);
        logRecord.setMessage("Message");

        LogRecordWithParentListResponse.LogRecordParent logRecordParen1 =
                new LogRecordWithParentListResponse.LogRecordParent();
        logRecordParen1.setUuid(UUID.randomUUID());
        logRecordParen1.setName(parentName1);
        logRecordParen1.setType(TypeAction.UI);
        logRecordParen1.setDepth(0);
        logRecordParen1.setMetaInfo(new MetaInfo(null, null, hashSum, defUUID, false));

        LogRecordWithParentListResponse.LogRecordParent logRecordParen2 =
                new LogRecordWithParentListResponse.LogRecordParent();
        logRecordParen2.setUuid(UUID.randomUUID());
        logRecordParen2.setName(parentName2);
        logRecordParen2.setName("message");
        logRecordParen2.setType(TypeAction.COMPOUND);
        logRecordParen2.setDepth(1);
        logRecordParen2.setMetaInfo(new MetaInfo(null, null, hashSum, definitionIdForMessageCompound, false));
        List<LogRecordWithParentListResponse.LogRecordParent> parents = new LinkedList<>();
        parents.add(logRecordParen1);
        parents.add(logRecordParen2);
        parents.add(logRecordParen1);
        logRecord.setParent(parents);

        return logRecord;
    }

    private LogRecordWithParentListResponse generateOneLogRecordWithoutParents(String stepName, String hashSum) {
        LogRecordWithParentListResponse logRecord = new LogRecordWithParentListResponse();
        UUID defUUID = UUID.randomUUID();
        logRecord.setUuid(UUID.randomUUID());
        logRecord.setName(stepName);
        logRecord.setTestingStatus(TestingStatuses.PASSED);
        logRecord.setMetaInfo(new MetaInfo(null, null, hashSum, defUUID, false));
        return logRecord;
    }

    private RowScreenshotRequest getRowScreenshotRequest() {
        UUID uuid1 = UUID.fromString("5c9cfcf9-9f1e-4693-b2f7-492ff17a2ee6");
        UUID uuid2 = UUID.fromString("7e9494ae-c77d-4409-b1c7-5b0b338adf4f");
        List<UUID> uuidList1 = new ArrayList<>();
        uuidList1.add(uuid1);
        uuidList1.add(uuid2);
        return new RowScreenshotRequest(uuidList1);
    }
}
