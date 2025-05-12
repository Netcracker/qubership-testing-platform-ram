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
import java.util.stream.Stream;

import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.services.LogRecordService;

import com.google.common.collect.Lists;
import lombok.Data;

@Data
public class LogRecordDto {

    private String caption;
    private UUID id;
    private List<LogRecordDto> children = Lists.newLinkedList();
    private String testingStatus;

    /**
     * DataTransferObject for {@link LogRecord}, which has enough info for TreeView.
     *
     * @param record           {@link LogRecord} which for need create DTO.
     * @param logRecordService {@link LogRecordService} to get children of {@code record}.
     */
    public LogRecordDto(LogRecord record, LogRecordService logRecordService) {
        this.caption = record.getName();
        this.id = record.getUuid();
        this.testingStatus = record.getTestingStatus().name();
        Stream<LogRecord> records = logRecordService.getLogRecordChildren(this.id);
        records.forEach(logRecord -> children.add(new LogRecordDto(logRecord, logRecordService)));
    }

}
