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

package org.qubership.atp.ram.service.rest.server.charts;

import java.util.List;
import java.util.UUID;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.atp.ram.exceptions.charts.RamChartsExecutionInfoFilterOptionsException;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.service.charts.ChartsService;
import org.qubership.atp.ram.service.rest.dto.DurationsByStatuses;
import org.qubership.atp.ram.service.rest.dto.ExecutionInfoOptions;
import org.qubership.atp.ram.service.rest.dto.Sector;
import org.qubership.atp.ram.service.rest.dto.StatisticTrByRc;
import org.qubership.atp.ram.service.rest.dto.StatisticTrByStatuses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("api/charts")
@Slf4j
public class ChartsController /*implements ChartsControllerApi */ {

    public static final Integer MAX_NUMBER_OF_ERS = 20;
    public static final Integer MIN_NUMBER_OF_ERS = 1;
    private final ChartsService chartsService;

    @Autowired
    public ChartsController(ChartsService chartsService) {
        this.chartsService = chartsService;
    }

    /**
     * Ping.
     */
    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }

    /**
     * Returns ChartsData.
     */
    @GetMapping("/{executionRequestId}")
    public JsonObject getChartsData(@PathVariable("executionRequestId") UUID executionRequestId) {
        return chartsService.getChartsData(executionRequestId);
    }

    /**
     * Returns chart data for last 20 runs.
     */
    @GetMapping("/lastruns/{testPlanUuid}")
    public JsonObject getLast20LanchesStatistic(@PathVariable("testPlanUuid") UUID testPlanUuid) {
        return chartsService.getLast20LanchesStatistic(testPlanUuid);
    }

    /**
     * Returns graph options.
     */
    @GetMapping("/options")
    public List<String> getGraphOptions() {
        return chartsService.getGraphOptions();
    }

    /**
     * Returns statistics on the specified parameters.
     * @param options JSONArray with options for statistic.
     *                Example: {graphOptions: [{
     *                      testPlan: "TestPlanId",
     *                      option: "Statuses or Root causes",
     *                      period: ["from Date", "to Date"] or Empty,
     *                      numberOfEr: "number or Empty"
     *                }]}
     * @return JSONArray with statistics.
     */
    @PostMapping("/statistics")
    public JsonArray getLastStatistic(@RequestBody JsonObject options) {
        return chartsService.getStatistics(options.get("graphOptions").getAsJsonArray());
    }

    /**
     * Returns the list of root causes and number of test
     * runs with the given root causes on corresponding date.
     *
     * @param options example1:
     *                Returns information about last 20 ERs, sorted by
     *                {@link ExecutionRequest#finishDate}
     *                with specified test plan id:
     *                {
     *                  "testPlan": "TestPlanId",
     *                }
     *
     *                example 2: several execution requests ids are defined
     *
     *                {
     *                  "executionRequestIds": [
     *                  "first selected ER ID",
     *                  "second selected ER ID"
     *                  ]
     *                }
     *
     *                example 3: filters can be specified in addition to test plan id
     *                {
     *                 "testPlan": "TestPlanId",
     *                 "filterOptions": {
     *                   "analyzedByQa": false,
     *                   "erFinishDateFrom": "YYYY-MM-DDThh:mm:ss.209Z",
     *                   "erFinishDateTo": "2019-09-25T14:07:20.209Z",
     *                   "numberOfEr": 5
     *                 }
     *                }
     * @return List of {@link StatisticTrByRc}
     */
    @PostMapping("/trByRootCauses")
    @AuditAction(auditAction = "Get test runs by root causes per day for test plan '{{#options.testPlan}}'")
    public List<StatisticTrByRc> getTestRunsByRootCausesPerDay(@RequestBody ExecutionInfoOptions options) {
        checkOptions(options);
        return chartsService.getTestRunsByRootCausesPerDay(options);
    }

    /**
     * Returns the list of statuses and number of test
     * runs with the given status for each ER.
     @param options example1:
     *                Returns information about last 20 ERs, sorted by
     *                {@link ExecutionRequest#finishDate}
     *                with specified test plan id:
     *                {
     *                  "testPlan": "TestPlanId",
     *                }
     *
     *                example 2: several execution requests ids are defined
     *
     *                {
     *                  "executionRequestIds": [
     *                  "first selected ER ID",
     *                  "second selected ER ID"
     *                  ]
     *                }
     *
     *                example 3: filters can be specified in addition to test plan id
     *                {
     *                 "testPlan": "TestPlanId",
     *                 "filterOptions": {
     *                   "analyzedByQa": false,
     *                   "erFinishDateFrom": "YYYY-MM-DDThh:mm:ss.209Z",
     *                   "erFinishDateTo": "2019-09-25T14:07:20.209Z",
     *                   "numberOfEr": 5
     *                 }
     *                }
     * @return List of {@link StatisticTrByStatuses}
     */
    @PostMapping("/trByStatuses")
    @AuditAction(auditAction = "Get test runs by statuses per execution request for test plan '{{#options.testPlan}}'")
    public List<StatisticTrByStatuses> getTestRunsByRootCausesPerEr(@RequestBody ExecutionInfoOptions options) {
        checkOptions(options);
        return chartsService.getTestRunsByStatusesPerEr(options);
    }

    /**
     * Returns the list of root causes and number of
     * test runs with specified root cause for all
     * given ERs or all ERs in specified test plan.
     *
     @param options example1:
     *                Returns information about last 20 ERs, sorted by
     *                {@link ExecutionRequest#finishDate}
     *                with specified test plan id:
     *                {
     *                  "testPlan": "TestPlanId",
     *                }
     *
     *                example 2: several execution requests ids are defined
     *
     *                {
     *                  "executionRequestIds": [
     *                  "first selected ER ID",
     *                  "second selected ER ID"
     *                  ]
     *                }
     *
     *                example 3: filters can be specified in addition to test plan id
     *                {
     *                 "testPlan": "TestPlanId",
     *                 "filterOptions": {
     *                 "analyzedByQa": false,
     *                   "erFinishDateFrom": "YYYY-MM-DDThh:mm:ss.209Z",
     *                   "erFinishDateTo": "2019-09-25T14:07:20.209Z",
     *                   "numberOfEr": 5
     *                 }
     *                }
     */
    @PostMapping("/pie/trByRootCauses")
    @AuditAction(auditAction = "Get test runs by root causes for pie chart in test plan '{{#options.testPlan}}'")
    public List<Sector> getTestRunsByRootCauses(@RequestBody ExecutionInfoOptions options) {
        checkOptions(options);
        return chartsService.getTestRunsByRootCauses(options);
    }

    /**
     * Returns the list of statuses and number of
     * test runs with specified status for all
     * given ERs or all ERs in specified test plan.
     *
     * @param options example1:
     *               Returns information about last 20 ERs, sorted by
     *                {@link ExecutionRequest#finishDate}
     *                with specified test plan id:
     *                {
     *                  "testPlan": "TestPlanId",
     *               }
     *
     *                example 2: several execution requests ids are defined
     *
     *                {
     *                 "executionRequestIds": [
     *                  "first selected ER ID",
     *                  "second selected ER ID"
     *                  ]
     *                }
     *
     *                example 3: filters can be specified in addition to test plan id
     *                {
     *                 "testPlan": "TestPlanId",
     *                 "filterOptions": {
     *                 "analyzedByQa": false,
     *                   "erFinishDateFrom": "YYYY-MM-DDThh:mm:ss.209Z",
     *                   "erFinishDateTo": "2019-09-25T14:07:20.209Z",
     *                   "numberOfEr": 5
     *                 }
     *                }
     */
    @PostMapping("/pie/trByStatuses")
    @AuditAction(auditAction = "Get test runs by statuses for pie chart in test plan '{{#options.testPlan}}'")
    public List<Sector> getTestRunsByStatusForPieChart(@RequestBody ExecutionInfoOptions options) {
        checkOptions(options);
        return chartsService.getTestRunsByStatuses(options);
    }

    @PostMapping("/trDurations")
    @AuditAction(auditAction = "Get test runs durations by statuses for test plan '{{#options.testPlan}}'")
    public List<DurationsByStatuses> getTestRunsDurationsByStatuses(@RequestBody ExecutionInfoOptions options) {
        checkOptions(options);
        return chartsService.getTestRunDurationsByStatuses(options);
    }

    private void checkOptions(ExecutionInfoOptions options) {
        ExecutionInfoOptions.FilterOptions filterOptions = options.getFilterOptions();
        if (filterOptions != null) {
            Integer numberOfErs = filterOptions.getNumberOfEr();
            if (numberOfErs != null && numberOfErs < MIN_NUMBER_OF_ERS || numberOfErs > MAX_NUMBER_OF_ERS) {
                log.error("Invalid number of execution requests: {}. Value Should be greater than 1 and less than 21",
                        numberOfErs);
                throw new RamChartsExecutionInfoFilterOptionsException(numberOfErs);
            }
        }
    }
}
