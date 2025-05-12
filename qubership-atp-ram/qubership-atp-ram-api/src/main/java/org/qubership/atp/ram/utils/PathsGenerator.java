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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.services.LogRecordService;

public class PathsGenerator {

    private List<UUID> pathToStep;
    private LogRecordService logRecordService;

    /**
     * Generator paths to found objects.
     *
     * @param logRecordService service for find log record
     */
    public PathsGenerator(LogRecordService logRecordService) {
        this.logRecordService = logRecordService;
    }

    /**
     * Find test runs with names, which contain searchValue.
     *
     * @param testRuns original list with test runs
     * @return generated paths to found test runs
     */
    public StepPath generatePathToFoundTestRuns(List<TestRun> testRuns) {
        List<UUID> foundTestRunsUuid = testRuns.stream()
                .map(TestRun::getUuid)
                .collect(Collectors.toList());
        StepPath stepPath = new StepPath();
        stepPath.setUuidSteps(foundTestRunsUuid);
        return stepPath;
    }

    /**
     * Find log records with names, which contain searchValue.
     *
     * @param logRecords original list with log records
     * @return generated paths to found log records
     */
    public List<StepPath> generatePathToFoundLogRecords(List<LogRecord> logRecords) {
        List<StepPath> result = new LinkedList<>();
        logRecords.forEach(logRecord -> {
            pathToStep = new LinkedList<>();
            pathToStep.add(logRecord.getUuid());
            generateRecursivePath(logRecord);
            if (!pathToStep.isEmpty()) {
                StepPath stepPath = new StepPath();
                Collections.reverse(pathToStep);
                stepPath.setUuidSteps(pathToStep);
                result.add(stepPath);
            }
        });
        return result;
    }

    private void generateRecursivePath(LogRecord logRecord) {
        UUID parentUuid = logRecord.getParentRecordId();
        while (Objects.nonNull(parentUuid)) {
            pathToStep.add(parentUuid);
            parentUuid = logRecordService.findById(parentUuid).getParentRecordId();
        }
        pathToStep.add(logRecord.getTestRunId());
    }

    /**
     * Find log record by uuid.
     *
     * @param uuid of log record
     * @return path to LR
     */
    public StepPath generatePathToFoundLogRecord(UUID uuid) {
        pathToStep = new LinkedList<>();
        LogRecord logRecord = logRecordService.findById(uuid);
        generateRecursivePath(logRecord);
        StepPath stepPath = new StepPath();
        Collections.reverse(pathToStep);
        stepPath.setUuidSteps(pathToStep);
        return stepPath;
    }
}
