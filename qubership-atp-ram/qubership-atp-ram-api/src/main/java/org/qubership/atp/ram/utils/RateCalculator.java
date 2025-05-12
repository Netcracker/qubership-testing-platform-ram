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

package org.qubership.atp.ram.utils;

import static java.util.Objects.isNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.enums.Flags;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.exceptions.testruns.RamTestRunIllegalNullableExecutionStatusException;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.Scope;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.services.CatalogueService;
import org.qubership.atp.ram.services.TestRunService;
import org.springframework.stereotype.Service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@Service
public class RateCalculator {

    private final TestRunService testRunService;
    private final CatalogueService catalogueService;

    /**
     * Calculate rates for stopped ER.
     */
    public void calculateRates(ExecutionRequest executionRequest, List<TestRun> allTestRuns) {
        if (allTestRuns.isEmpty()) {
            log.debug("ER hasn't test runs, uuid = {}", executionRequest.getUuid());
            return;
        }
        calculateTrRates(allTestRuns);
        calculateErRates(executionRequest, allTestRuns);
    }

    private void calculateTrRates(List<TestRun> allTestRuns) {
        log.debug("Calculate TR-s rates");
        allTestRuns.forEach(testRun -> {
            try {
                List<LogRecord> logRecords = testRunService.getAllTestingStatusLogRecordsByTestRunId(testRun.getUuid());
                int passed = 0;
                int warning = 0;
                int failed = 0;
                int others = 0;
                int logRecordsCount = logRecords.size();
                for (LogRecord logRecord : logRecords) {
                    switch (logRecord.getTestingStatus()) {
                        case PASSED: {
                            passed++;
                            break;
                        }
                        case WARNING: {
                            warning++;
                            break;
                        }
                        case FAILED: {
                            failed++;
                            break;
                        }
                        case SKIPPED: {
                            break;
                        }
                        default: {
                            others++;
                            log.debug("Log record {} has other status {}",
                                    logRecord.getUuid(), logRecord.getTestingStatus());
                            break;
                        }
                    }
                }
                int actualRunLogRecords = logRecordsCount - others;
                testRun.setWarningRate((int) (Math.rint(warning * 1000d / actualRunLogRecords) / 10));
                testRun.setFailedRate((int) (Math.rint(failed * 1000d / actualRunLogRecords) / 10));
                testRun.setPassedRate((int) (Math.rint(passed * 1000d / actualRunLogRecords) / 10));
            } catch (Exception e) {
                log.error("Error in calculating rates for Test Run {}.", testRun.getUuid(), e);
            }
        });
        testRunService.saveAll(allTestRuns);
    }

    /**
     * Checks if result of test run should be counted.
     *
     * @param testRun            checked test run
     * @param flagIds            flags of the execution request
     * @param prerequisitesCases uuids of prerequisite cases
     * @param validationCases    uuids of validation cases
     * @return true if test run should not be counted due to set of ignore flag
     */
    public boolean isTestRunIgnoredByFlag(TestRun testRun, Set<UUID> flagIds,
                                          List<UUID> prerequisitesCases, List<UUID> validationCases) {
        final UUID testCaseId = testRun.getTestCaseId();

        if (flagIds == null) {
            return false;
        }

        if (flagIds.contains(Flags.IGNORE_PREREQUISITE_IN_PASS_RATE.getId()) && prerequisitesCases != null
                && prerequisitesCases.contains(testCaseId)) {
            return true;
        }

        if (flagIds.contains(Flags.IGNORE_VALIDATION_IN_PASS_RATE.getId()) && validationCases != null
                && validationCases.contains(testCaseId)) {
            return true;
        }

        return false;
    }

    /**
     * Calculate rates for stopped ER.
     *
     * @param executionRequest execution request
     * @param allTestRuns      all test runs
     */
    public void calculateErRates(ExecutionRequest executionRequest, List<TestRun> allTestRuns) {
        int passed = 0;
        int warning = 0;
        int failed = 0;
        int others = 0;
        int skippedCount = 0;
        int notCounted = 0;

        final Scope testScope = catalogueService.getTestScope(executionRequest.getTestScopeId());
        final Set<UUID> flagIds = executionRequest.getFlagIds();
        final List<UUID> prerequisitesCases = testScope != null ? testScope.getPrerequisitesCases() : null;
        final List<UUID> validationCases = testScope != null ? testScope.getValidationCases() : null;

        List<TestRun> finishedTestRuns = allTestRuns.stream()
                .filter(testRun -> !ExecutionStatuses.IN_PROGRESS.equals(testRun.getExecutionStatus()))
                .collect(Collectors.toList());

        for (TestRun testRun : finishedTestRuns) {
            if (isTestRunIgnoredByFlag(testRun, flagIds, prerequisitesCases, validationCases)) {
                notCounted++;
                continue;
            }
            switch (testRun.getTestingStatus()) {
                case PASSED: {
                    passed++;
                    break;
                }
                case WARNING: {
                    warning++;
                    break;
                }
                case FAILED: {
                    failed++;
                    break;
                }
                case SKIPPED: {
                    skippedCount++;
                    break;
                }
                default: {
                    others++;
                    break;
                }
            }
        }
        int trsCount = allTestRuns.size();
        int actualRunTrs = trsCount - skippedCount - notCounted;
        executionRequest.setCountOfTestRuns(actualRunTrs);
        executionRequest.setWarningRate(calculateRateInt(warning, actualRunTrs));
        executionRequest.setFailedRate(calculateRateInt(failed, actualRunTrs));
        executionRequest.setPassedRate(calculateRateInt(passed, actualRunTrs));
        log.debug("Rate for ER {} Passed: {} Warning: {} Failed: {} Other: {}.",
                executionRequest.getUuid(), passed, warning,
                failed, others);
    }

    /**
     * Calculates what percent 'part' is of 'total'. Returns float with 1 number after point.
     *
     * @param part  count
     * @param total count
     * @return percent part of total.
     */
    public static float calculateRateFloat(int part, int total) {
        if (total == 0) {
            return total;
        }
        final DecimalFormat decimalFormat = new DecimalFormat("#.##",
                DecimalFormatSymbols.getInstance(Locale.US));
        decimalFormat.setRoundingMode(RoundingMode.DOWN);
        String value = decimalFormat.format(part * 100d / total);
        return new BigDecimal(value).setScale(1, RoundingMode.HALF_DOWN).floatValue();
    }

    /**
     * Calculates what percent 'part' is of 'total'. Returns int number.
     *
     * @param part  count
     * @param total count
     * @return percent part of total.
     */
    public static int calculateRateInt(int part, int total) {
        return (int) calculateRateFloat(part, total);
    }

    /**
     * Calculate tets runs testing status stats.
     *
     * @param testRuns test runs
     * @return result stats
     */
    public Map<TestingStatuses, TestingStatusesStat> calculateTestRunsTestingStatusStats(
            ExecutionRequest executionRequest,
            List<TestRun> testRuns) {
        final Scope testScope = catalogueService.getTestScope(executionRequest.getTestScopeId());
        final Set<UUID> flagIds = executionRequest.getFlagIds();
        final List<UUID> prerequisitesCases = testScope != null ? testScope.getPrerequisitesCases() : null;
        final List<UUID> validationCases = testScope != null ? testScope.getValidationCases() : null;
        Map<TestingStatuses, TestingStatusesStat> statusMap = Arrays.stream(TestingStatuses.values())
                .collect(Collectors.toMap(Function.identity(), TestingStatusesStat::new));
        List<TestRun> testRunsWithNullExecutionStatus = testRuns.stream()
                .filter(testRun -> isNull(testRun.getExecutionStatus()))
                .collect(Collectors.toList());
        if (!testRunsWithNullExecutionStatus.isEmpty()) {
            log.error("Can't calculate ER rates, found test run with null execution status: {}",
                    StreamUtils.extractIds(testRunsWithNullExecutionStatus));
            throw new RamTestRunIllegalNullableExecutionStatusException();
        }
        int notCountedTestRuns = 0;
        for (TestRun testRun : testRuns) {
            if (isTestRunIgnoredByFlag(testRun, flagIds, prerequisitesCases, validationCases)) {
                notCountedTestRuns++;
                continue;
            }
            TestingStatuses testingStatus = testRun.getTestingStatus();
            TestingStatusesStat stat = statusMap.get(testingStatus);
            stat.incrCount();
        }
        TestingStatusesStat skippedStatusStat = statusMap.get(TestingStatuses.SKIPPED);
        int skippedCount = isNull(skippedStatusStat) ? 0 : skippedStatusStat.getCount();
        int actualTotalTestRuns = testRuns.size() - skippedCount - notCountedTestRuns;
        statusMap.values()
                .stream()
                .filter(stat -> TestingStatuses.SKIPPED != stat.getStatus())
                .forEach(stat -> stat.setRate(calculateRateFloat(stat.getCount(), actualTotalTestRuns)));
        return statusMap;
    }

    @Data
    public static class TestingStatusesStat {

        private TestingStatuses status;
        private int count;
        private float rate;

        public TestingStatusesStat(TestingStatuses status) {
            this.status = status;
        }

        public void incrCount() {
            this.count = ++count;
        }
    }
}
