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

package org.qubership.atp.ram.service.charts;

import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static org.qubership.atp.ram.enums.DefaultSuiteNames.EXECUTION_REQUESTS_LOGS;
import static org.qubership.atp.ram.service.rest.server.charts.ChartsController.MAX_NUMBER_OF_ERS;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.qubership.atp.ram.enums.DefaultRootCauseType;
import org.qubership.atp.ram.enums.DefaultSuiteNames;
import org.qubership.atp.ram.enums.GraphOptions;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.RamObject;
import org.qubership.atp.ram.models.RootCause;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.service.rest.dto.DurationsByStatuses;
import org.qubership.atp.ram.service.rest.dto.ExecutionInfoOptions;
import org.qubership.atp.ram.service.rest.dto.Sector;
import org.qubership.atp.ram.service.rest.dto.StatisticTrByRc;
import org.qubership.atp.ram.service.rest.dto.StatisticTrByStatuses;
import org.qubership.atp.ram.services.ExecutionRequestService;
import org.qubership.atp.ram.services.RootCauseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

@Service
public class ChartsService {

    private static final Integer START_INDEX = 0;
    private static final String FINISH_DATE_FILED_NAME = "finishDate";
    private static final String SORT_DIRECTION = "desc";
    private final ExecutionRequestService erService;
    private final RootCauseService rootCauseService;

    @Autowired
    public ChartsService(ExecutionRequestService erService, RootCauseService rootCauseService) {
        this.erService = erService;
        this.rootCauseService = rootCauseService;
    }

    /**
     * Returns graph options.
     */
    public List<String> getGraphOptions() {
        List<String> options = new ArrayList<>();
        options.add(GraphOptions.STATUSES.getName());
        options.add(GraphOptions.ROOT_CAUSES.getName());
        return options;
    }

    /**
     * Returns statistics on the specified parameters.
     *
     * @param jsonArray JSONArray with options for statistic.
     * @return JSONArray with statistics.
     */
    public JsonArray getStatistics(JsonArray jsonArray) {
        JsonArray result = new JsonArray();
        if (jsonArray.isEmpty()) {
            return getErrorMessage(result, "Request cannot be EMPTY");
        }
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonObject resultStatistic = new JsonObject();
            JsonObject parameters = jsonArray.get(i).getAsJsonObject();
            UUID testPlanId = UUID.fromString(parameters.get(ChartsParameters.REQUEST_PARAM_TEST_PLAN).getAsString());
            String numberOfErs = parameters.get(ChartsParameters.REQUEST_PARAM_NUMBER_OF_ER).getAsString();
            JsonArray period = parameters.getAsJsonArray(ChartsParameters.REQUEST_PARAM_PERIOD);

            List<ExecutionRequest> testPlanErs;
            if (Strings.isNullOrEmpty(numberOfErs) && (period == null || period.isEmpty())) {
                return getErrorMessage(result, "Number of ER's or period must be specified.");
            } else {
                testPlanErs = getExecutionRequests(testPlanId, numberOfErs, period);
            }
            JsonArray categories = calculateCategories(testPlanErs);
            resultStatistic.add(ChartsParameters.RESPONSE_PARAM_CATEGORIES, categories);

            JsonArray sample = new JsonArray();
            String option = parameters.get(ChartsParameters.REQUEST_PARAM_OPTION).getAsString();

            if (option.equals(GraphOptions.STATUSES.getName())) {
                sample = calculateStatusesData(testPlanErs);
            } else if (option.equals(GraphOptions.ROOT_CAUSES.getName())) {
                sample = calculateRootCauses(testPlanErs);
            }
            resultStatistic.add(ChartsParameters.RESPONSE_PARAM_SAMPLE, sample);
            result.add(resultStatistic);
        }
        return result;
    }

    private List<ExecutionRequest> getExecutionRequests(UUID testPlanId, String numberOfErs, JsonArray period) {
        List<ExecutionRequest> testPlanErs;

        if (!Strings.isNullOrEmpty(numberOfErs)) {
            testPlanErs = erService.findPageByTestPlanUuidAndSort(
                    testPlanId, START_INDEX, Integer.parseInt(numberOfErs), "date", "desc");
        } else {
            Timestamp startDate = new Timestamp(period.get(0).getAsLong());
            Timestamp finishDate = new Timestamp(period.get(1).getAsLong());
            testPlanErs = erService.findPageByTestPlanUuidBetweenPeriodAndSort(testPlanId, startDate, finishDate,
                    START_INDEX, Integer.parseInt(numberOfErs), "date", "desc");
        }

        return testPlanErs;
    }

    private List<ExecutionRequest> getExecutionRequests(ExecutionInfoOptions options) {
        if (!CollectionUtils.isEmpty(options.getExecutionRequestIds())) {
            return erService.findAllByUuidIn(options.getExecutionRequestIds());
        } else if (options.getTestPlan() != null) {
            if (options.getFilterOptions() == null) {
                return erService.findPageByTestPlanUuidAndSort(options.getTestPlan(), START_INDEX, MAX_NUMBER_OF_ERS,
                        FINISH_DATE_FILED_NAME, SORT_DIRECTION);
            } else {
                return getExecutionRequestsWithOptions(options);
            }
        }
        return emptyList();
    }

    private JsonArray getErrorMessage(JsonArray result, String message) {
        JsonObject object = new JsonObject();
        object.addProperty(ChartsParameters.RESPONSE_PARAM_SUCCESSFUL, false);
        object.addProperty(ChartsParameters.RESPONSE_PARAM_MESSAGE, message);
        result.add(object);
        return result;
    }

    /**
     * Returns ChartData.
     */
    public JsonObject getChartsData(UUID executionRequestId) {
        JsonObject result = new JsonObject();
        if (Objects.isNull(executionRequestId)) {
            result.addProperty(ChartsParameters.RESPONSE_PARAM_SUCCESSFUL, false);
            result.addProperty(ChartsParameters.RESPONSE_PARAM_MESSAGE, "ER_ID cannot be NULL or EMPTY");
            return result;
        }
        ExecutionRequest er = erService.findById(executionRequestId);
        List<TestRun> testRunList = erService.getAllTestRuns(executionRequestId);
        List<TestRun> tmp = new ArrayList<>();
        testRunList.forEach(testRun -> {
            if (!EXECUTION_REQUESTS_LOGS.getName().equalsIgnoreCase(testRun.getName())) {
                tmp.add(testRun);
            }
        });
        List<RootCause> rootCauses = this.rootCauseService.getAllRootCauses();
        JsonArray failureReasonsData = fillSimpleParams(calculateFailureReasons(tmp, rootCauses));
        result.add(ChartsParameters.RESPONSE_PARAM_FR, failureReasonsData);
        JsonArray testRunsData = calculateTestRunsData(tmp);
        result.add(ChartsParameters.RESPONSE_PARAM_TR, testRunsData);
        JsonArray suitesData = calculateSuitesData(tmp);
        result.add(ChartsParameters.RESPONSE_PARAM_SUITES, suitesData);
        JsonArray durationsData = calculateDurationsData(tmp);
        result.add(ChartsParameters.RESPONSE_PARAM_DURATION, durationsData);
        return result;
    }

    /**
     * Returns chart data for last 20 runs.
     */
    public JsonObject getLast20LanchesStatistic(UUID testPlanId) {
        JsonObject result = new JsonObject();
        List<ExecutionRequest> testPlanErs = erService.findPageByTestPlanUuidAndSort(
                testPlanId, START_INDEX, MAX_NUMBER_OF_ERS, "date", "desc");
        JsonArray categories = calculateCategories(testPlanErs);
        result.add(ChartsParameters.RESPONSE_PARAM_CATEGORIES, categories);
        JsonArray statuses = calculateStatusesData(testPlanErs);
        result.add(ChartsParameters.RESPONSE_PARAM_STATUSES, statuses);
        return result;
    }

    private JsonArray calculateStatusesData(List<ExecutionRequest> requests) {
        JsonArray result = new JsonArray();
        HashMap<TestingStatuses, JsonArray> data = new HashMap<>();
        data.put(TestingStatuses.PASSED, new JsonArray());
        data.put(TestingStatuses.FAILED, new JsonArray());
        data.put(TestingStatuses.WARNING, new JsonArray());
        data.put(TestingStatuses.STOPPED, new JsonArray());
        data.put(TestingStatuses.SKIPPED, new JsonArray());
        data.put(TestingStatuses.UNKNOWN, new JsonArray());
        requests.forEach(request -> {
            AtomicInteger trsPassed = new AtomicInteger(0);
            AtomicInteger trsFailed = new AtomicInteger(0);
            AtomicInteger trsWarning = new AtomicInteger(0);
            AtomicInteger trsStopped = new AtomicInteger(0);
            AtomicInteger trsSkipped = new AtomicInteger(0);
            AtomicInteger trsUnknown = new AtomicInteger(0);
            List<TestRun> testRuns = erService.getAllTestRuns(request.getUuid());
            testRuns.forEach(testRun -> {
                TestingStatuses status = testRun.getTestingStatus();
                //WA to processing internal TRs created by ATP1
                if (status != null) {
                    switch (status) {
                        case PASSED:
                            trsPassed.getAndIncrement();
                            break;
                        case FAILED:
                            trsFailed.getAndIncrement();
                            break;
                        case WARNING:
                            trsWarning.getAndIncrement();
                            break;
                        case STOPPED:
                            trsStopped.getAndIncrement();
                            break;
                        case SKIPPED:
                            trsSkipped.getAndIncrement();
                            break;
                        default:
                            trsUnknown.getAndIncrement();
                            break;
                    }
                }
            });
            data.get(TestingStatuses.PASSED).add(trsPassed.get());
            data.get(TestingStatuses.FAILED).add(trsFailed.get());
            data.get(TestingStatuses.WARNING).add(trsWarning.get());
            data.get(TestingStatuses.STOPPED).add(trsStopped.get());
            data.get(TestingStatuses.SKIPPED).add(trsSkipped.get());
            data.get(TestingStatuses.UNKNOWN).add(trsUnknown.get());
        });
        for (Map.Entry<TestingStatuses, JsonArray> entry : data.entrySet()) {
            JsonObject status = new JsonObject();
            status.addProperty(ChartsParameters.RESPONSE_PARAM_NAME, entry.getKey().getName());
            status.add(ChartsParameters.RESPONSE_PARAM_DATA, entry.getValue());
            status.addProperty(ChartsParameters.RESPONSE_PARAM_COLOR, getStatusColor(entry.getKey()));
            result.add(status);
        }
        return result;
    }

    /**
     * Calculate root causes for ever Execution request.
     *
     * @param requests Execution requests.
     * @return Array with data for graph.
     */
    public JsonArray calculateRootCauses(List<ExecutionRequest> requests) {
        JsonArray result = new JsonArray();
        HashMap<String, JsonArray> data = new HashMap<>();

        List<RootCause> rootCauses = this.rootCauseService.getAllRootCauses();
        data.put(DefaultRootCauseType.NOT_ANALYZED.getName(), new JsonArray());
        if (!rootCauses.isEmpty()) {
            rootCauses.forEach(rootCause -> data.put(rootCause.getName(), new JsonArray()));
        }

        requests.forEach(request -> {
            List<TestRun> testRunList = erService.getAllTestRuns(request.getUuid());
            HashMap<String, Integer> resultForTestRun = calculateFailureReasons(testRunList, rootCauses);
            if (!resultForTestRun.isEmpty()) {
                resultForTestRun.forEach((key, value) -> data.get(key).add(value));
            }
        });
        for (Map.Entry<String, JsonArray> entry : data.entrySet()) {
            JsonObject status = new JsonObject();
            status.addProperty(ChartsParameters.RESPONSE_PARAM_NAME, entry.getKey());
            status.add(ChartsParameters.RESPONSE_PARAM_DATA, entry.getValue());
            result.add(status);
        }
        return result;
    }

    private JsonArray calculateCategories(List<ExecutionRequest> requests) {
        JsonArray result = new JsonArray();
        requests.forEach(request -> result.add(request.getName()));
        return result;
    }

    private JsonArray calculateDurationsData(List<TestRun> testRunList) {
        JsonArray result = new JsonArray();
        AtomicInteger testRunIdx = new AtomicInteger(0);
        testRunList.forEach(testRun -> {
            if (testRun.getStartDate() != null && testRun.getFinishDate() != null) {
                testRunIdx.getAndIncrement();
                JsonObject testRunJson = new JsonObject();
                testRunJson.addProperty(ChartsParameters.RESPONSE_PARAM_NAME, testRun.getName());
                JsonArray data = new JsonArray();
                JsonArray start = new JsonArray();
                start.add(testRun.getStartDate().getTime());
                start.add(testRunIdx.get());
                data.add(start);
                JsonArray finish = new JsonArray();
                finish.add(testRun.getFinishDate().getTime());
                finish.add(testRunIdx.get());
                data.add(finish);
                testRunJson.add(ChartsParameters.RESPONSE_PARAM_DATA, data);
                result.add(testRunJson);
            }
        });
        return result;
    }

    private JsonArray calculateSuitesData(List<TestRun> testRunList) {
        HashMap<String, Integer> suites = new HashMap<>();
        testRunList.forEach(testRun -> {
            String suiteName = DefaultSuiteNames.SINGLE_TEST_RUNS.getName();
            int current = suites.get(suiteName) == null ? 0 : suites.get(suiteName);
            suites.put(suiteName, current + 1);
        });
        return fillSimpleParams(suites);
    }

    private JsonArray calculateTestRunsData(List<TestRun> testRunList) {
        JsonArray result = new JsonArray();
        HashMap<TestingStatuses, Integer> statuses = new HashMap<>();
        testRunList.forEach(testRun -> {
            TestingStatuses status = testRun.getTestingStatus();
            int current = statuses.get(status) == null ? 0 : statuses.get(status);
            statuses.put(status, current + 1);
        });
        for (Map.Entry<TestingStatuses, Integer> entry : statuses.entrySet()) {
            JsonObject status = new JsonObject();
            status.addProperty(ChartsParameters.RESPONSE_PARAM_NAME, entry.getKey().getName());
            status.addProperty(ChartsParameters.RESPONSE_PARAM_Y, entry.getValue());
            status.addProperty(ChartsParameters.RESPONSE_PARAM_COLOR, getStatusColor(entry.getKey()));
            result.add(status);
        }
        return result;
    }

    private String getStatusColor(TestingStatuses key) {
        switch (key) {
            case FAILED:
                return "rgb(255, 100, 100)";
            case PASSED:
                return "rgb(75, 230, 75)";
            case WARNING:
                return "rgb(255, 255, 126)";
            case SKIPPED:
                return "rgb(255, 170, 50)";
            case STOPPED:
                return "rgb(255, 82, 24)";
            default:
                return "";
        }
    }

    private HashMap<String, Integer> calculateFailureReasons(List<TestRun> testRunList, List<RootCause> rootCauses) {
        HashMap<String, Integer> reasons = new HashMap<>();
        testRunList.forEach(testRun -> {
            if (!testRun.getTestingStatus().equals(TestingStatuses.PASSED)) {
                UUID rootCauseId = testRun.getRootCauseId();
                String frName = getTestRunRootCause(testRun, rootCauses);
                int current = reasons.get(frName) == null ? 0 : reasons.get(frName);
                reasons.put(frName, current + 1);
            }
        });
        return reasons;
    }

    private JsonArray fillSimpleParams(Map<String, Integer> values) {
        JsonArray result = new JsonArray();
        for (Map.Entry<String, Integer> entry : values.entrySet()) {
            JsonObject value = new JsonObject();
            value.addProperty(ChartsParameters.RESPONSE_PARAM_NAME, entry.getKey());
            value.addProperty(ChartsParameters.RESPONSE_PARAM_Y, entry.getValue());
            result.add(value);
        }
        return result;
    }

    /**
     * Returns the list of root causes and number of test
     * runs with the given root causes on corresponding date.
     */
    public List<StatisticTrByRc> getTestRunsByRootCausesPerDay(ExecutionInfoOptions options) {
        List<ExecutionRequest> ers = getExecutionRequests(options);
        List<TestRun> testRuns = getTestRuns(ers);
        Map<String, List<TestRun>> trByRootCauses =
                testRuns
                        .stream()
                        .collect(Collectors.groupingBy(
                                tr -> getTestRunRootCause(tr, rootCauseService.getAllRootCauses())
                        ));

        return trByRootCauses.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue()
                                .stream()
                                .filter(tr -> tr.getFinishDate() != null)
                                .collect(Collectors.groupingBy(tr -> extractDay(tr.getFinishDate()).getTime()))
                                .entrySet()
                                .stream()
                                .collect(Collectors.toMap(Map.Entry::getKey, item -> item.getValue().size()))
                ))
                .entrySet()
                .stream()
                .map(entry -> {
                    StatisticTrByRc statistic = new StatisticTrByRc();
                    statistic.setName(entry.getKey());
                    statistic.setData(entry.getValue()
                            .entrySet()
                            .stream()
                            .map(pair -> new Long[]{pair.getKey(), new Long(pair.getValue())})
                            .collect(Collectors.toList()));
                    return statistic;
                })
                .collect(Collectors.toList());
    }

    /**
     * Returns the list of statuses and number of test
     * runs with the given status for each ER.
     */
    public List<StatisticTrByStatuses> getTestRunsByStatusesPerEr(ExecutionInfoOptions options) {
        List<ExecutionRequest> ers = getExecutionRequests(options);
        List<TestRun> testRuns = getTestRuns(ers);
        Map<UUID, ExecutionRequest> executionRequestMap = ers.stream().collect(Collectors.toMap(
                RamObject::getUuid,
                er -> er
        ));
        Map<String, List<TestRun>> trByStatuses = groupTestRunsByStatuses(testRuns);
        return trByStatuses.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue()
                                .stream()
                                .collect(Collectors.groupingBy(TestRun::getExecutionRequestId))
                                .entrySet().stream().collect(Collectors.toMap(
                                        Map.Entry::getKey,/*UUID*/
                                        trList -> trList.getValue().size())
                                )
                ))
                .entrySet()
                .stream()
                .map(entry -> {
                    StatisticTrByStatuses statistic = new StatisticTrByStatuses();
                    statistic.setName(entry.getKey());
                    statistic.setData(entry.getValue()
                            .entrySet()
                            .stream()
                            .map(pair -> {
                                JsonArray array = new JsonArray();
                                array.add(executionRequestMap.get(pair.getKey()).getName());
                                array.add(pair.getValue());
                                return array;
                            })
                            .collect(Collectors.toList()));
                    return statistic;
                })
                .collect(Collectors.toList());
    }

    /**
     * Returns the list of root causes and number of
     * test runs with specified root cause for all
     * given ERs or all ERs in specified test plan.
     */
    public List<Sector> getTestRunsByRootCauses(ExecutionInfoOptions options) {
        List<ExecutionRequest> ers = getExecutionRequests(options);
        List<TestRun> testRuns = getTestRuns(ers);
        return
                testRuns
                        .stream()
                        .collect(Collectors.groupingBy(
                                tr -> getTestRunRootCause(tr, rootCauseService.getAllRootCauses())
                        ))
                        .entrySet().stream().map(entry -> new Sector(
                                entry.getKey(),
                                entry.getValue().size())
                        ).collect(Collectors.toList());
    }

    private String getTestRunRootCause(TestRun tr, List<RootCause> rootCauses) {
        if (tr.getRootCauseId() == null || CollectionUtils.isEmpty(rootCauses)) {
            return DefaultRootCauseType.NOT_ANALYZED.getName();
        } else {
            Optional<RootCause> rootCause = rootCauses.stream()
                    .filter(cause -> cause.getUuid().equals(tr.getRootCauseId()))
                    .findFirst();
            return rootCause.isPresent() ? rootCause.get().getName()
                    : DefaultRootCauseType.NOT_ANALYZED.getName();
        }
    }

    private Timestamp extractDay(Timestamp ts) {
        Timestamp newTs = new Timestamp(ts.getTime());
        newTs.setHours(0);
        newTs.setMinutes(0);
        newTs.setSeconds(0);
        newTs.setNanos(0);
        return newTs;
    }

    /**
     * Returns the list of statuses and number of
     * test runs with specified status for all
     * given ERs or all ERs in specified test plan.
     */
    public List<Sector> getTestRunsByStatuses(ExecutionInfoOptions options) {
        List<ExecutionRequest> ers = getExecutionRequests(options);
        List<TestRun> testRuns = getTestRuns(ers);
        return
                testRuns
                        .stream()
                        .collect(Collectors.groupingBy(
                                tr -> tr.getTestingStatus().getName()))
                        .entrySet().stream().map(entry -> new Sector(
                                entry.getKey(),
                                entry.getValue().size())
                        ).collect(Collectors.toList());
    }

    /**
     * Returns test run durations grouped by statuses.
     */
    public List<DurationsByStatuses> getTestRunDurationsByStatuses(ExecutionInfoOptions options) {
        List<ExecutionRequest> ers = getExecutionRequests(options);
        List<TestRun> testRuns = getTestRuns(ers);
        Map<String, List<TestRun>> trByStatuses = groupTestRunsByStatuses(testRuns);
        AtomicInteger counter = new AtomicInteger();
        return trByStatuses.entrySet()
                .stream()
                .map(statusTrListPair -> {
                    DurationsByStatuses durations = new DurationsByStatuses();
                    durations.setName(statusTrListPair.getKey());
                    statusTrListPair.getValue()
                            .stream()
                            .filter(testRun -> nonNull(testRun.getStartDate()) && nonNull(testRun.getFinishDate()))
                            .forEach(testRun -> {
                                DurationsByStatuses.DataPoint leftPoint = new DurationsByStatuses.DataPoint(
                                        testRun.getStartDate().getTime(), counter.get());
                                DurationsByStatuses.DataPoint rightPoint = new DurationsByStatuses.DataPoint(
                                        testRun.getFinishDate().getTime(), counter.getAndIncrement());
                                durations.getData().add(leftPoint);
                                durations.getData().add(rightPoint);
                            });
                    return durations;
                })
                .collect(Collectors.toList());
    }

    private List<ExecutionRequest> getExecutionRequestsWithOptions(ExecutionInfoOptions options) {
        ExecutionInfoOptions.FilterOptions filterOptions = options.getFilterOptions();
        if (filterOptions.getErFinishDateTo() == null && filterOptions.getErFinishDateFrom() == null) {
            return erService.findPageByTestPlanUuidAndAnalyzedByQa(
                    options.getTestPlan(),
                    filterOptions.getAnalyzedByQa(),
                    START_INDEX,
                    filterOptions.getNumberOfEr() == null ? MAX_NUMBER_OF_ERS : filterOptions.getNumberOfEr()
            );
        } else {
            return erService.findPageByTestPlanUuidBetweenPeriodAndAnalyzedByQa(
                    options.getTestPlan(),
                    filterOptions.getErFinishDateFrom() == null ? new Timestamp(0) :
                            new Timestamp(filterOptions.getErFinishDateFrom().getTime()),
                    filterOptions.getErFinishDateTo() == null ? new Timestamp(System.currentTimeMillis()) :
                            new Timestamp(filterOptions.getErFinishDateTo().getTime()),
                    filterOptions.getAnalyzedByQa(),
                    0,
                    filterOptions.getNumberOfEr() == null ? MAX_NUMBER_OF_ERS : filterOptions.getNumberOfEr()
            );
        }
    }

    private List<TestRun> getTestRuns(List<ExecutionRequest> ers) {
        return ers.stream()
                .flatMap(er -> erService.getAllTestRuns(er.getUuid()).stream())
                .collect(Collectors.toList());
    }

    private Map<String, List<TestRun>> groupTestRunsByStatuses(List<TestRun> testRuns) {
        return testRuns
                .stream()
                .collect(Collectors.groupingBy(
                        tr -> tr.getTestingStatus().getName()
                ));
    }

    private class ChartsParameters {

        private static final String REQUEST_PARAM_TEST_PLAN = "testPlan";
        private static final String REQUEST_PARAM_NUMBER_OF_ER = "numberOfEr";
        private static final String REQUEST_PARAM_PERIOD = "period";
        private static final String REQUEST_PARAM_OPTION = "option";

        private static final String RESPONSE_PARAM_NAME = "name";
        private static final String RESPONSE_PARAM_Y = "y";
        private static final String RESPONSE_PARAM_DATA = "data";
        private static final String RESPONSE_PARAM_CATEGORIES = "categories";
        private static final String RESPONSE_PARAM_SAMPLE = "sample";
        private static final String RESPONSE_PARAM_COLOR = "color";
        private static final String RESPONSE_PARAM_SUCCESSFUL = "successful";
        private static final String RESPONSE_PARAM_MESSAGE = "message";
        private static final String RESPONSE_PARAM_FR = "failureReasonData";
        private static final String RESPONSE_PARAM_TR = "testRunsData";
        private static final String RESPONSE_PARAM_SUITES = "suitesData";
        private static final String RESPONSE_PARAM_DURATION = "durationsData";
        private static final String RESPONSE_PARAM_STATUSES = "statuses";
    }
}
