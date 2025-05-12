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

package org.qubership.atp.ram.service.rest.dto;

import java.util.List;
import java.util.UUID;

import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.services.LogRecordService;
import org.qubership.atp.ram.services.TestRunService;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;
import lombok.Data;

@Data
public class TestRunDto {

    @JsonIgnore
    private final TestRunService testRunService;
    @JsonIgnore
    private final LogRecordService logRecordService;
    private String caption;
    private UUID id;
    private UUID executionRequest;
    private List<LogRecordDto> children = Lists.newLinkedList();
    private String testingStatus;

    /**
     * DataTrasferObject for {@link TestRun} which has enough info for TreeView.
     *
     * @param service          {@link TestRunService} for loading {@link TestRun} by {@link java.util.UUID}
     * @param logRecordService {@link LogRecordService} for loading top level {@link LogRecord}
     * @param uuid             {@link java.util.UUID} of {@link TestRun}
     */
    public TestRunDto(TestRunService service, LogRecordService logRecordService, UUID uuid) {
        this.id = uuid;
        this.logRecordService = logRecordService;
        this.testRunService = service;
    }

    /**
     * Build tree for whole TR.
     *
     * @return tree for TestRun Compare.
     */
    public TestRunDto build() {
        TestRun testRun = testRunService.getByUuid(this.id);
        setCaption(testRun.getName());
        setTestingStatus(testRun.getTestingStatus().name());
        setExecutionRequest(testRun.getExecutionRequestId());
        buildLogRecords();
        return this;
    }

    private void buildLogRecords() {
        List<LogRecord> records = testRunService.getTopLevelLogRecords(this.id, null);
        records.forEach(record -> children.add(new LogRecordDto(record, logRecordService)));
    }
}
