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

import static java.util.Objects.nonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.qubership.atp.ram.dto.response.Environment;
import org.qubership.atp.ram.enums.TestScopeSections;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.enums.TypeAction;
import org.qubership.atp.ram.model.BaseSearchRequest;
import org.qubership.atp.ram.model.CompareItem;
import org.qubership.atp.ram.model.CompareTestRunsDetailsCell;
import org.qubership.atp.ram.model.CompareTestRunsDetailsRow;
import org.qubership.atp.ram.model.DefectStatisticExecutionRequestResponse;
import org.qubership.atp.ram.model.DefectStatisticResponse;
import org.qubership.atp.ram.model.EnvironmentDetailsCompareResponse;
import org.qubership.atp.ram.model.ExecutionRequestsCompareScreenshotResponse;
import org.qubership.atp.ram.model.LogRecordCompareScreenshotResponse;
import org.qubership.atp.ram.model.LogRecordWithParentListResponse;
import org.qubership.atp.ram.model.RootCauseStatisticResponse;
import org.qubership.atp.ram.model.ShortExecutionRequest;
import org.qubership.atp.ram.model.SubstepScreenshotResponse;
import org.qubership.atp.ram.model.TestRunDetailsCompareResponse;
import org.qubership.atp.ram.model.request.EnvironmentsCompareRequest;
import org.qubership.atp.ram.model.request.LogRecordCompareRequest;
import org.qubership.atp.ram.model.request.LogRecordCompareRequestItem;
import org.qubership.atp.ram.model.request.RowScreenshotRequest;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.MetaInfo;
import org.qubership.atp.ram.models.RamObject;
import org.qubership.atp.ram.models.TestPlan;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.utils.StreamUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExecutionRequestCompareService {

    private static final String PARENT_DELIMITER = " / ";

    @Value("${catalogue.url}")
    String catalogueUrl;

    private final LogRecordService logRecordService;
    private final ExecutionRequestService executionRequestService;
    private final EnvironmentsService environmentsService;
    private final RootCauseService rootCauseService;
    private final TestPlansService testPlansService;
    private final String deltaPostfix = "s";

    /**
     * Validate request.
     */
    public boolean validateExecutionRequestsDuplicates(List<UUID> executionRequestIds) {
        for (UUID id : executionRequestIds) {
            if (Collections.frequency(executionRequestIds, id) > 1) {
                return false;
            }
        }
        return true;
    }

    /**
     * Comparison response for environments.
     */
    public List<EnvironmentDetailsCompareResponse> getEnvironmentDetailsCompareResponse(
            EnvironmentsCompareRequest environmentsCompareRequest) {
        List<ExecutionRequest> executionRequests =
                executionRequestService.getExecutionRequestsByIds(environmentsCompareRequest.getExecutionRequestIds());
        BaseSearchRequest searchRequest = new BaseSearchRequest();
        searchRequest.setIds(new ArrayList<>());
        if (CollectionUtils.isEmpty(environmentsCompareRequest.getEnvironmentIds())) {
            executionRequests.forEach(executionRequest -> searchRequest.getIds()
                    .add(executionRequest.getEnvironmentId()));
        } else {
            searchRequest.getIds().addAll(environmentsCompareRequest.getEnvironmentIds());
        }
        List<Environment> environments = environmentsService.searchEnvironments(searchRequest);
        List<EnvironmentDetailsCompareResponse> environmentDetailsCompareResponses = new ArrayList<>();
        for (ExecutionRequest request : executionRequests) {
            List<Environment> foundEnvironmentList = environments.stream()
                    .filter(env -> request.getEnvironmentId().equals(env.getId())).collect(Collectors.toList());
            if (foundEnvironmentList.isEmpty()
                    && !CollectionUtils.isEmpty(environmentsCompareRequest.getEnvironmentIds())) {
                continue;
            }
            String environmentName = foundEnvironmentList.isEmpty() ? "-" : foundEnvironmentList.get(0).getName();
            String environmentLink = foundEnvironmentList.isEmpty() ? null : generateEnvironmentLink(request);
            environmentDetailsCompareResponses.add(new EnvironmentDetailsCompareResponse(
                    request.getUuid(),
                    request.getName(),
                    environmentName,
                    environmentLink,
                    request.getEnvironmentId()));
        }
        return environmentDetailsCompareResponses;
    }

    private String generateEnvironmentLink(ExecutionRequest executionRequest) {
        return catalogueUrl + "/project/" + executionRequest.getProjectId() + "/environments"
                + "/environment/" + executionRequest.getEnvironmentId();
    }

    /**
     * Comparison response for defect statistic.
     */
    public DefectStatisticResponse getDefectStatisticResponse(List<UUID> executionRequestIds) {
        DefectStatisticResponse defectStatisticResponse = new DefectStatisticResponse();
        Map<UUID, List<TestRun>> testRunMatrix = new HashMap<>();
        for (UUID executionRequestId : executionRequestIds) {
            testRunMatrix.put(executionRequestId, executionRequestService.getAllTestRuns(executionRequestId));
        }
        List<UUID> rootCauseIds = new ArrayList<>();
        for (Map.Entry<UUID, List<TestRun>> entry : testRunMatrix.entrySet()) {
            rootCauseIds.addAll(entry.getValue().stream()
                    .filter(testRun -> testRun.getRootCauseId() != null)
                    .map(TestRun::getRootCauseId).collect(Collectors.toList()));
        }
        Map<UUID, String> rootCauses =
                rootCauseService.getByIds(rootCauseIds).stream().collect(Collectors.toMap(RamObject::getUuid,
                        RamObject::getName));
        List<ExecutionRequest> executionRequests =
                executionRequestService.getExecutionRequestsByIds(executionRequestIds);
        for (ExecutionRequest executionRequest : executionRequests) {
            DefectStatisticExecutionRequestResponse defectStatisticExecutionRequestResponse =
                    new DefectStatisticExecutionRequestResponse();
            defectStatisticExecutionRequestResponse.setExecutionRequestName(executionRequest.getName());
            List<TestRun> testRuns = testRunMatrix.get(executionRequest.getUuid());
            defectStatisticExecutionRequestResponse
                    .setRootCauseStatisticResponseList(generateRootCauseStatisticResponses(testRuns, rootCauses));
            defectStatisticResponse.addDefectStatisticExecutionRequestResponse(defectStatisticExecutionRequestResponse);
        }
        return defectStatisticResponse;
    }

    private List<RootCauseStatisticResponse> generateRootCauseStatisticResponses(List<TestRun> testRuns, Map<UUID,
            String> rootCauses) {
        int testRunsCount = testRuns.size();
        if (testRunsCount == 0) {
            return Collections.emptyList();
        }
        Map<String, Integer> defectStatisticStatusMap = new HashMap<>();
        for (TestRun testRun : testRuns) {
            if (testRun.getRootCauseId() != null) {
                String rootCauseName = rootCauses.get(testRun.getRootCauseId());
                defectStatisticStatusMap.merge(rootCauseName, 1, Integer::sum);
            } else if (testRun.getTestingStatus() == TestingStatuses.PASSED) {
                defectStatisticStatusMap.merge(DefectStatisticResponse.DefaultDefectStatisticStatus.PASSED, 1,
                        Integer::sum);
            } else {
                defectStatisticStatusMap.merge(DefectStatisticResponse.DefaultDefectStatisticStatus.NOT_ANALYZED, 1,
                        Integer::sum);
            }
        }
        return defectStatisticStatusMap.entrySet().stream()
                .map(defectStatisticEntry -> new RootCauseStatisticResponse(
                        defectStatisticEntry.getKey(),
                        defectStatisticEntry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * Comparison table for test runs.
     */
    public TestRunDetailsCompareResponse getTestRunDetailsCompareResponses(List<UUID> executionRequestIds) {
        List<ExecutionRequest> executionRequests = getSortedExecutionRequests(executionRequestIds);
        Map<UUID, List<TestRun>> notSortedMap =
                executionRequestService.getMapTestRunsForExecutionRequests(executionRequestIds);
        Map<UUID, List<TestRun>> sortedCompareMap =
                generateOrderedMapWithExecutionRequestsAndTestRuns(executionRequests, notSortedMap);


        List<TestRun> firstSortedTestRuns = sortedCompareMap.get(executionRequestIds.get(0)).stream()
                .sorted(Comparator.comparingInt(TestRun::getOrder))
                .collect(Collectors.toList());
        TestRunDetailsCompareResponse testRunDetailsCompareResponse = new TestRunDetailsCompareResponse();
        testRunDetailsCompareResponse.setExecutionRequests(executionRequests
                .stream()
                .map(er -> new ShortExecutionRequest(er.getUuid(), er.getName()))
                .collect(Collectors.toList()));

        ExecutionRequest firstExecutionRequest = executionRequests.get(0);
        executionRequests.remove(0);
        for (TestRun testRun : firstSortedTestRuns) {
            CompareTestRunsDetailsRow row = new CompareTestRunsDetailsRow();
            row.setRowType(CompareTestRunsDetailsRow.CompareTestRunsRowType.TEST_RUN);
            row.setName(testRun.getName());
            addTestRunCell(row, testRun);
            for (ExecutionRequest executionRequest : executionRequests) {
                UUID executionRequestId = executionRequest.getUuid();
                boolean testPlansAreEquals =
                        checkTestPlansAreEquals(Arrays.asList(firstExecutionRequest, executionRequest));
                TestRun foundTestRun = testPlansAreEquals
                        ? getTestRunByTestCaseId(sortedCompareMap.get(executionRequestId), testRun.getTestCaseId())
                        : getTestRunByTestRunName(sortedCompareMap.get(executionRequestId), testRun.getName());
                if (foundTestRun == null) {
                    addEmptyTestRunCell(row, executionRequestId);
                } else {
                    addTestRunCell(row, foundTestRun);
                    sortedCompareMap.get(executionRequestId).remove(foundTestRun);
                }
            }
            setDeltaValues(row);
            testRunDetailsCompareResponse.getRowList().add(row);
        }
        return testRunDetailsCompareResponse;
    }

    private LinkedList<ExecutionRequest> getSortedExecutionRequests(List<UUID> executionRequestIds) {
        return executionRequestService.getExecutionRequestsByIds(executionRequestIds)
                .stream()
                .sorted(Comparator.comparing(er -> executionRequestIds.indexOf(er.getUuid())))
                .collect(Collectors.toCollection(LinkedList::new));
    }

    private Map<UUID, List<TestRun>> generateOrderedMapWithExecutionRequestsAndTestRuns(
            List<ExecutionRequest> executionRequests, Map<UUID, List<TestRun>> notSortedMap) {
        Map<UUID, List<TestRun>> compareMap = new LinkedHashMap<>();
        for (ExecutionRequest executionRequest : executionRequests) {
            List<TestRun> testRuns = notSortedMap.get(executionRequest.getUuid());
            if (executionRequest.getTestScopeId() != null) {
                reorderTestRunByScope(testRuns);
            }
            compareMap.put(executionRequest.getUuid(), testRuns);
        }
        return compareMap;
    }

    private void setDeltaValues(CompareTestRunsDetailsRow row) {
        if (row.getCellList().size() >= 2) {
            Iterator<CompareTestRunsDetailsCell> cellIterator = row.getCellList().iterator();
            CompareTestRunsDetailsCell firstCell = cellIterator.next();
            CompareTestRunsDetailsCell secondCell = cellIterator.next();
            setDeltaValue(firstCell, secondCell);
            while (cellIterator.hasNext()) {
                secondCell = cellIterator.next();
                setDeltaValue(firstCell, secondCell);
            }
        }
    }

    private void setDeltaValue(CompareTestRunsDetailsCell firstCell,
                               CompareTestRunsDetailsCell currentCell) {
        currentCell.setDelta(currentCell.getDuration() - firstCell.getDuration());
    }


    private void addTestRunCell(CompareTestRunsDetailsRow row, TestRun testRun) {
        row.addCell(new CompareTestRunsDetailsCell(testRun.getTestingStatus().toString().toUpperCase(),
                testRun.getDuration(),
                false,
                testRun.getExecutionRequestId(),
                testRun.getUuid(), null));
    }

    private void addEmptyTestRunCell(CompareTestRunsDetailsRow row, UUID executionRequestId) {
        row.addCell(new CompareTestRunsDetailsCell(null,
                0,
                true,
                executionRequestId,
                null, null));
    }

    /**
     * Getting test run by test case id.
     */
    public TestRun getTestRunByTestCaseId(List<TestRun> testRuns, UUID testCaseId) {
        if (!testRuns.isEmpty()) {
            for (TestRun testRun : testRuns) {
                if (testRun.getTestCaseId().equals(testCaseId)) {
                    return testRun;
                }
            }
        }
        return null;
    }

    /**
     * Getting test run by test run name.
     */
    public TestRun getTestRunByTestRunName(List<TestRun> testRuns, String testRunName) {
        if (!testRuns.isEmpty()) {
            for (TestRun testRun : testRuns) {
                if (testRun.getName().equals(testRunName)) {
                    return testRun;
                }
            }
        }
        return null;
    }

    /**
     * Calculate new orders for test runs in the scope.
     */
    public void reorderTestRunByScope(List<TestRun> testRuns) {
        List<TestRun> prerequisitesTestRuns = getFilteredTestRunBySection(testRuns, TestScopeSections.PREREQUISITES);
        int maxPrerequisitesOrder = prerequisitesTestRuns.isEmpty()
                ? 0
                : prerequisitesTestRuns.stream().max(Comparator.comparingInt(TestRun::getOrder)).get().getOrder();
        reorderTestRunsBySection(testRuns, TestScopeSections.EXECUTION, maxPrerequisitesOrder);
        reorderTestRunsBySection(testRuns, TestScopeSections.VALIDATION, maxPrerequisitesOrder);
    }

    /**
     * Calculate new orders for test runs in the section.
     */
    public void reorderTestRunsBySection(List<TestRun> testRuns, TestScopeSections section, int previousMaxOrder) {
        List<TestRun> sectionTestRuns = getFilteredTestRunBySection(testRuns, section);
        int currentMaxOrder = previousMaxOrder + 1;
        for (int index = 0; index < sectionTestRuns.size(); index++, currentMaxOrder++) {
            sectionTestRuns.get(index).setOrder(currentMaxOrder);
        }
    }

    /**
     * Getting test runs by section in the scope.
     */
    public List<TestRun> getFilteredTestRunBySection(List<TestRun> testRuns, TestScopeSections section) {
        return StreamUtils.filterByTestScopeSection(testRuns,
                        TestRun::getTestScopeSection,
                        section).stream()
                .sorted(Comparator.comparingInt(TestRun::getOrder)).collect(Collectors.toList());
    }

    /**
     * Getting log record on another execution request with the same definition id or name.
     */
    public CompareItem getReferenceToRecord(List<CompareItem> compareItems, CompareItem item,
                                            boolean compareWithFirst) {
        int index = 0;
        if (compareWithFirst) {
            index = compareItems.stream().filter(CompareItem::isCompared).mapToInt(CompareItem::getRowNumberInCase)
                    .max().orElse(0);
        }
        for (; index < compareItems.size(); index++) {
            CompareItem compareItem = compareItems.get(index);
            if (!compareItem.isCompared()
                    && checkHashSum(compareItem, item)
                    && recordsPathsAreNullOrHaveSamePaths(compareItem, item)) {
                compareItem.setCompared(true);
                return compareItem;
            }
        }
        return new CompareItem();
    }

    /**
     * Returns true if records don't have paths or have same paths.
     */
    public boolean recordsPathsAreNullOrHaveSamePaths(CompareItem firstItem, CompareItem secondItem) {
        return Objects.isNull(firstItem.getPath()) && Objects.isNull(secondItem.getPath())
                || Strings.nullToEmpty(firstItem.getPath()).equals(secondItem.getPath());
    }

    /**
     * Returns true if records has same name.
     */
    public boolean recordsHaveSameNames(CompareItem firstItem, CompareItem secondItem) {
        return firstItem.getItemValue().getName().equals(secondItem.getItemValue().getName());
    }

    /**
     * Returns true if records has same definition id.
     */
    public boolean recordsHaveSameDefinitionIds(CompareItem firstItem, CompareItem secondItem) {
        return firstItem.getItemValue().getMetaInfo() != null
                && secondItem.getItemValue().getMetaInfo() != null
                && firstItem.getItemValue().getMetaInfo().getDefinitionId()
                .equals(secondItem.getItemValue().getMetaInfo().getDefinitionId());
    }

    /**
     * Returns a row with empty cells for only one log record.
     */
    public List<CompareItem> getEmptyCompareList(int listSize, CompareItem lastItem) {
        List<CompareItem> returnedList = new ArrayList<>();
        for (int index = 0; index < listSize - 1; index++) {
            returnedList.add(new CompareItem());
        }
        returnedList.add(lastItem);
        return returnedList;
    }

    /**
     *  Returns maximal row.
     */
    public int getMaximalRowInCompareItems(List<CompareItem> compareItems) {
        return compareItems.isEmpty() ? 0 :
                compareItems.stream()
                        .max(Comparator.comparingInt(CompareItem::getRowNumberInCase))
                        .get().getRowNumberInCase();
    }

    /**
     * Comparison table for log records.
     */
    public TestRunDetailsCompareResponse getLogRecordCompareResponse(LogRecordCompareRequest request) {
        if (request.getLogRecordCompareRequestItems().isEmpty()) {
            return new TestRunDetailsCompareResponse();
        }
        String compareType = request.getCompareType();
        List<List<CompareItem>> compareItemsMatrix = new ArrayList<>();
        int erCount = request.getLogRecordCompareRequestItems().size();
        request.getLogRecordCompareRequestItems().forEach(requestItem -> {
            List<CompareItem> compareItems = new ArrayList<>();
            if (requestItem.getItemId() != null) {
                List<LogRecord> logRecordList;
                if (compareType.equals("TEST_RUN")) {
                    logRecordList = logRecordService
                            .findTopLogRecordsOnTestRun(requestItem.getItemId());
                } else {
                    logRecordList = logRecordService
                            .getOrderedChildrenLogRecordsForParentLogRecord(requestItem.getItemId());

                }
                for (int index = 0; index < logRecordList.size(); index++) {
                    LogRecord logRecord = logRecordList.get(index);
                    if (!nonNull(logRecord.getMetaInfo())
                            || !nonNull(logRecord.getMetaInfo().getScenarioHashSum())) {
                        MetaInfo metaInfo = new MetaInfo();
                        metaInfo.setScenarioHashSum(logRecord.getName());
                        logRecord.setMetaInfo(metaInfo);
                    }
                    compareItems.add(new CompareItem(requestItem.getExecutionRequestId(),
                            null, index, index,
                            logRecord, false,
                            logRecord.getMetaInfo().getScenarioHashSum()));
                }
            }
            compareItemsMatrix.add(compareItems);
        });

        List<LinkedList<CompareItem>> comparedItemMatrix = compareLogRecords(compareItemsMatrix, erCount);

        TestRunDetailsCompareResponse testRunDetailsCompareResponse = new TestRunDetailsCompareResponse();
        comparedItemMatrix = comparedItemMatrix.stream().sorted(Comparator
                .comparingInt(this::getMaximalRowInCompareItems)).collect(Collectors.toList());
        List<UUID> executionRequestIds = request.getLogRecordCompareRequestItems()
                .stream()
                .map(LogRecordCompareRequestItem::getExecutionRequestId)
                .collect(Collectors.toList());
        comparedItemMatrix.forEach(compareItems -> {
            CompareTestRunsDetailsRow row = new CompareTestRunsDetailsRow();
            CompareItem notEmptyItem = compareItems.stream().filter(compareItem -> compareItem.getItemValue() != null)
                    .collect(Collectors.toList()).get(0);
            if (notEmptyItem.getItemValue().isCompaund() || notEmptyItem.getItemValue().isSection()) {
                row.setRowType("COMPOUND");
            } else {
                row.setRowType(notEmptyItem.getItemValue().getType().toString());
            }

            row.setName(notEmptyItem.getItemValue().getName());
            executionRequestIds.forEach(executionRequestId -> {
                CompareTestRunsDetailsCell cell = new CompareTestRunsDetailsCell();
                List<CompareItem> filterItems = compareItems
                        .stream()
                        .filter(item -> executionRequestId.equals(item.getExecutionRequestId()))
                        .collect(Collectors.toList());
                if (filterItems.isEmpty()) {
                    cell.setCellStatus(null);
                    cell.setExecutionRequestId(executionRequestId);
                    cell.setEmpty(true);
                } else {
                    CompareItem compareItem = filterItems.get(0);
                    cell.setItemId(compareItem.getItemValue().getUuid());
                    cell.setExecutionRequestId(executionRequestId);
                    cell.setCellStatus(compareItem.getItemValue().getTestingStatus().getName().toUpperCase());
                    cell.setDuration(compareItem.getItemValue().getDuration());
                    cell.setEmpty(false);
                }
                row.addCell(cell);
            });
            setDeltaValues(row);
            testRunDetailsCompareResponse.getRowList().add(row);
        });
        return testRunDetailsCompareResponse;
    }

    private void fillEmptyCells(LinkedList<CompareItem> items, int currentIndex) {
        while (items.size() <= currentIndex) {
            items.add(new CompareItem());
        }
    }

    private List<LinkedList<CompareItem>> compareLogRecords(List<List<CompareItem>> compareItemsMatrix, int erCount) {
        List<LinkedList<CompareItem>> comparedItemMatrix = new LinkedList<>();
        for (CompareItem compareItem : compareItemsMatrix.get(0)) {
            LinkedList<CompareItem> comparedItemList = new LinkedList<>();
            comparedItemList.add(compareItem);
            comparedItemMatrix.add(comparedItemList);
        }
        for (int index = 1; index < erCount; index++) {
            List<CompareItem> firstErItems = new LinkedList<>(compareItemsMatrix.get(0));
            for (CompareItem compareItem : compareItemsMatrix.get(index)) {
                if (!compareItem.isCompared()) {
                    CompareItem secondItem = getReferenceToRecord(firstErItems, compareItem, true);

                    if (!secondItem.isCompared()) {
                        for (int compareIndex = 1; compareIndex < index; compareIndex++) {
                            secondItem =
                                    getReferenceToRecord(compareItemsMatrix.get(compareIndex), compareItem, false);
                            if (secondItem.isCompared()) {
                                break;
                            }
                        }
                        if (!secondItem.isCompared()) {
                            LinkedList<CompareItem> comparedItemsRow = new LinkedList<>();
                            for (int i = 0; i < index; i++) {
                                comparedItemsRow.add(new CompareItem());
                            }
                            compareItem.setRowNumberInMatrix(comparedItemMatrix.size());
                            comparedItemsRow.add(compareItem);
                            comparedItemMatrix.add(comparedItemsRow);
                        } else {
                            List<CompareItem> comparedItemRow = comparedItemMatrix
                                    .get(secondItem.getRowNumberInMatrix());
                            compareItem.setCompared(true);
                            compareItem.setRowNumberInMatrix(secondItem.getRowNumberInMatrix());
                            comparedItemRow.add(compareItem);
                        }
                    } else {
                        List<CompareItem> comparedItemRow = comparedItemMatrix.get(secondItem.getRowNumberInMatrix());
                        comparedItemRow.add(compareItem);
                    }
                }
            }
            for (LinkedList<CompareItem> comparedItemRow : comparedItemMatrix) {
                fillEmptyCells(comparedItemRow, index);
            }
            firstErItems.forEach(item -> item.setCompared(false));
        }
        return comparedItemMatrix;
    }

    /**
     * compare HashSun in MetaInfo in two CompareItems.
     */
    public boolean checkHashSum(CompareItem item, CompareItem itemCompare) {
        return item.getHashSumForCompare().equals(itemCompare.getHashSumForCompare());
    }

    /**
     * Check ER-s from one test plan.
     *
     * @param executionRequests list of ER
     * @return true, if ER-s from one test plan
     */
    public boolean checkTestPlansAreEquals(List<ExecutionRequest> executionRequests) {
        Set<UUID> testPlansId = StreamUtils.extractIds(executionRequests, ExecutionRequest::getTestPlanId);
        return testPlansId.size() <= 1;
    }

    /**
     * Get screenshots.
     */
    public List<SubstepScreenshotResponse> getCompareScreenshotsSubSteps(List<RowScreenshotRequest> rows) {
        if (CollectionUtils.isEmpty(rows)) {
            return new ArrayList<>();
        }
        List<UUID> logRecordIds = new ArrayList<>();
        rows.forEach(rowScreenshotRequest -> logRecordIds.addAll(rowScreenshotRequest.getRow()));
        return logRecordService.getSubstepScreenshots(logRecordIds);
    }

    /**
     * Compares Execution Requests with screenshots.
     *
     * @param executionRequestIds ER Id's.
     * @param logsWithScreenshotsContent compareScreenshots boolean
     * @return Tree with compared ER's, TR's and LR's.
     */
    public ExecutionRequestsCompareScreenshotResponse getCompareScreenshotsExecutionRequests(
            List<UUID> executionRequestIds, boolean logsWithScreenshotsContent) {
        log.debug("Start comparing ER's with screenshots: {}", executionRequestIds);
        ExecutionRequestsCompareScreenshotResponse response = new ExecutionRequestsCompareScreenshotResponse();
        LinkedList<ExecutionRequest> executionRequests = getSortedExecutionRequests(executionRequestIds);
        fillExecutionRequestsSection(response, executionRequests);

        Map<UUID, List<TestRun>> notSortedMap =
                executionRequestService.getMapTestRunsWithScreenShotsForExecutionRequests(executionRequestIds);
        Map<UUID, List<TestRun>> compareMap =
                generateOrderedMapWithExecutionRequestsAndTestRuns(executionRequests, notSortedMap);

        List<RamObject> testPlanInfoList = new LinkedList<>();
        executionRequests.forEach(executionRequest -> {
            RamObject testPlan = new TestPlan();
            UUID testPlanId = executionRequest.getTestPlanId();
            testPlan.setName(testPlansService.getTestPlanName(testPlanId));
            testPlan.setUuid(testPlanId);
            testPlanInfoList.add(testPlan);
        });

        List<TestRun> firstSortedTestRuns = compareMap.get(executionRequestIds.get(0)).stream()
                .sorted(Comparator.comparingInt(TestRun::getOrder))
                .collect(Collectors.toList());
        LinkedList<ExecutionRequestsCompareScreenshotResponse.TestRunCompareScreenshotResponse> testRuns =
                new LinkedList<>();

        for (TestRun leftTestRun : firstSortedTestRuns) {
            UUID executionRequestId = executionRequestIds.get(1);
            TestRun rightTestRun = checkTestPlansAreEquals(executionRequests)
                    ? getTestRunByTestCaseId(compareMap.get(executionRequestId), leftTestRun.getTestCaseId())
                    : getTestRunByTestRunName(compareMap.get(executionRequestId), leftTestRun.getName());
            if (rightTestRun != null) {
                ExecutionRequestsCompareScreenshotResponse.TestRunCompareScreenshotResponse
                        testRunResponse =
                        new ExecutionRequestsCompareScreenshotResponse.TestRunCompareScreenshotResponse();
                testRunResponse.setTestRunName(leftTestRun.getName());
                testRunResponse.setType(ExecutionRequestsCompareScreenshotResponse.Type.TESTRUN);

                Map<UUID, TestRun> compareTestRunMap = new LinkedHashMap<>();
                compareTestRunMap.put(leftTestRun.getExecutionRequestId(), leftTestRun);
                compareTestRunMap.put(executionRequestId, rightTestRun);
                LinkedList<List<CompareItem>> comparedLogRecordsMatrix = createComparedMatrix(compareTestRunMap);

                LinkedList<LogRecordCompareScreenshotResponse> topLogRecordsList = new LinkedList<>();
                createLogRecordsTree(comparedLogRecordsMatrix, testPlanInfoList, topLogRecordsList,
                        logsWithScreenshotsContent);
                testRunResponse.setChild(topLogRecordsList);

                testRuns.add(testRunResponse);
            }
        }
        response.setTree(testRuns);
        log.debug("End comparing ER's with screenshots: {}", executionRequestIds);
        return response;
    }

    /**
     * Compare Log Records and create matrix.
     *
     * @param compareMap Map with Test Runs for comparison.
     * @return Compared matrix.
     */
    public LinkedList<List<CompareItem>> createComparedMatrix(Map<UUID, TestRun> compareMap) {
        List<UUID> testRunIds = compareMap.values().stream().map(TestRun::getUuid).collect(Collectors.toList());
        log.debug("Start comparing LR's with screenshots for ER's {} and TR's {}", compareMap.keySet(), testRunIds);
        List<List<CompareItem>> compareItemsMatrix = new ArrayList<>();
        compareMap.forEach((UUID erId, TestRun testRun) -> {
            List<CompareItem> compareItems = new ArrayList<>();
            List<LogRecordWithParentListResponse> logRecordList =
                    logRecordService.findLogRecordsWithParentsByPreviewExists(testRun.getUuid());
            for (int index = 0; index < logRecordList.size(); index++) {
                LogRecordWithParentListResponse logRecord = logRecordList.get(index);
                List<LogRecordWithParentListResponse.LogRecordParent> logRecordParentList =
                        logRecord.getParent().stream()
                                .sorted(Comparator
                                        .comparingInt(LogRecordWithParentListResponse.LogRecordParent::getDepth)
                                        .reversed())
                                .collect(Collectors.toList());

                StringBuilder path = new StringBuilder();
                if (logRecordParentList.size() == 1) {
                    logRecord.setMetaInfo(logRecordParentList.get(0).getMetaInfo());
                } else if (logRecordParentList.size() > 1) {
                    for (LogRecordWithParentListResponse.LogRecordParent parent : logRecordParentList) {
                        if (nonNull(parent.getMetaInfo()) && !parent.getType().equals(TypeAction.COMPOUND)) {
                            logRecord.setMetaInfo(parent.getMetaInfo());
                            break;
                        } else {
                            path.append(parent.getMetaInfo().getScenarioHashSum()).append(PARENT_DELIMITER);
                        }
                    }
                }
                compareItems.add(new CompareItem(erId, path.toString(), index,
                        index, logRecord, false, logRecord.getMetaInfo().getScenarioHashSum()));
            }
            compareItemsMatrix.add(compareItems);
        });
        List<LinkedList<CompareItem>> comparedItemMatrix = new ArrayList<>();
        try {
            comparedItemMatrix = compareLogRecords(compareItemsMatrix, 2);
        } catch (NullPointerException e) {
            log.trace("Can not compared Log Records for screenshots : {}", compareItemsMatrix, e);
        }
        log.trace("Matrix of compared Log Records: {}", comparedItemMatrix);

        log.debug("End comparing LR's with screenshots for ER's {} and TR's {}", compareMap.keySet(), testRunIds);
        return comparedItemMatrix.stream().sorted(Comparator
                .comparingInt(this::getMaximalRowInCompareItems)).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Create log records with screenshots content.
     * @param comparedLogRecordsMatrix Matrix with compared Log Records.
     * @param testPlanInfoList Info about Test Plans.
     * @param topLogRecordsList Log Records with parent is a Test Run.
     * @param logsWithScreenshotsContent boolean value to check if it needs to add screenshots content.
     */
    public void createLogRecordsTree(LinkedList<List<CompareItem>> comparedLogRecordsMatrix,
                                     List<RamObject> testPlanInfoList,
                                     LinkedList<LogRecordCompareScreenshotResponse> topLogRecordsList,
                                     boolean logsWithScreenshotsContent) {
        log.debug("Start creating Log Record tree.");
        comparedLogRecordsMatrix.forEach(items -> {
            LogRecordCompareScreenshotResponse action = new LogRecordCompareScreenshotResponse();
            action.setType(ExecutionRequestsCompareScreenshotResponse.Type.ACTION.name());
            LogRecordWithParentListResponse logRecord =
                    createSubSteps(items, action, testPlanInfoList, logsWithScreenshotsContent);

            if (nonNull(logRecord)) {
                List<LogRecordWithParentListResponse.LogRecordParent> parent = logRecord.getParent().stream()
                        .sorted(Comparator
                                .comparingInt(LogRecordWithParentListResponse.LogRecordParent::getDepth)
                                .reversed()).collect(Collectors.toList());
                if (!parent.isEmpty()) {
                    LogRecordWithParentListResponse.LogRecordParent topParent = parent.get(0);
                    LogRecordCompareScreenshotResponse topLogRecord = createParentLogRecord(
                            topParent.getName(),
                            TypeAction.COMPOUND.equals(topParent.getType())
                                    ? ExecutionRequestsCompareScreenshotResponse.Type.COMPOUND.name()
                                    : ExecutionRequestsCompareScreenshotResponse.Type.ACTION.name());

                    parent.remove(0);
                    String path = parent.stream().map(RamObject::getName).collect(Collectors.joining(PARENT_DELIMITER));
                    List<LogRecordCompareScreenshotResponse> children = topLogRecord.getChild();

                    if (!path.isEmpty()) {
                        LogRecordCompareScreenshotResponse pathLogRecord = createParentLogRecord(path,
                                ExecutionRequestsCompareScreenshotResponse.Type.ACTION.name());

                        List<LogRecordCompareScreenshotResponse> childLogRecords = pathLogRecord.getChild();
                        childLogRecords.add(action);
                        pathLogRecord.setChild(childLogRecords);

                        children.add(pathLogRecord);
                    } else {
                        children.add(action);
                    }
                    topLogRecord.setChild(children);
                    topLogRecordsList.add(topLogRecord);
                } else {
                    LogRecordCompareScreenshotResponse topLogRecord = new LogRecordCompareScreenshotResponse();
                    topLogRecord.setType(ExecutionRequestsCompareScreenshotResponse.Type.ACTION.name());
                    topLogRecord.setName(StringUtils.EMPTY);
                    topLogRecord.setChild(Collections.singletonList(action));
                    topLogRecordsList.add(topLogRecord);
                }
            }
        });
        log.debug("End creating Log Record tree.");
    }

    private LogRecordCompareScreenshotResponse createParentLogRecord(String name, String type) {
        LogRecordCompareScreenshotResponse topLogRecord = new LogRecordCompareScreenshotResponse();
        topLogRecord.setType(type);
        topLogRecord.setName(name);
        return topLogRecord;
    }

    private LogRecordWithParentListResponse createSubSteps(List<CompareItem> items,
                                                           LogRecordCompareScreenshotResponse action,
                                                           List<RamObject> testPlanInfoList,
                                                           boolean logsWithScreenshotsContent) {
        LogRecordWithParentListResponse logRecord = null;
        LinkedList<LogRecordCompareScreenshotResponse.SubStepCompareScreenshotResponse> subSteps =
                new LinkedList<>();
        for (int index = 0; index < items.size(); index++) {
            CompareItem item = items.get(index);
            LogRecordCompareScreenshotResponse.SubStepCompareScreenshotResponse subStep;
            if (nonNull(item.getItemValue())) {
                logRecord = (LogRecordWithParentListResponse) item.getItemValue();
                action.setSubStepName(logRecord.getName());

                RamObject testPlanInfo = testPlanInfoList.get(index);
                if (logsWithScreenshotsContent) {
                    String screenshot = "";
                    List<SubstepScreenshotResponse> screenshotResponses =
                            logRecordService.getSubstepScreenshots(Collections.singletonList(logRecord.getUuid()));
                    if (!CollectionUtils.isEmpty(screenshotResponses)) {
                        screenshot = screenshotResponses.get(0).getScreenshot();
                    }
                    subStep =
                            new LogRecordCompareScreenshotResponse.SubStepCompareScreenshotResponse(logRecord.getUuid(),
                                    logRecord.getTestingStatus(), testPlanInfo.getUuid(),
                                    testPlanInfo.getName(), screenshot);
                } else {
                    subStep =
                            new LogRecordCompareScreenshotResponse.SubStepCompareScreenshotResponse(logRecord.getUuid(),
                                    logRecord.getTestingStatus(), testPlanInfo.getUuid(), testPlanInfo.getName(), "");
                }
            } else {
                subStep = new LogRecordCompareScreenshotResponse.SubStepCompareScreenshotResponse();
            }
            subSteps.add(subStep);
        }
        action.setRow(subSteps);
        return logRecord;
    }

    private void fillExecutionRequestsSection(ExecutionRequestsCompareScreenshotResponse response,
                                              List<ExecutionRequest> executionRequests) {
        response.setExecutionRequests(executionRequests.stream()
                .map(executionRequest ->
                        new ShortExecutionRequest(executionRequest.getUuid(), executionRequest.getName()))
                .collect(Collectors.toList())
        );
    }
}
