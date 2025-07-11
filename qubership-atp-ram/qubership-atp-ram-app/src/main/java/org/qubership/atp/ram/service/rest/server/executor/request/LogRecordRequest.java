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

package org.qubership.atp.ram.service.rest.server.executor.request;

import java.sql.Timestamp;
import java.util.Set;
import java.util.UUID;

import org.qubership.atp.ram.models.MetaInfo;

import lombok.Data;

@Data
public class LogRecordRequest {
    private UUID testRunId;
    private UUID logRecordId;
    private UUID parentRecordId;
    private String name;
    private String message;

    private Boolean isSection;
    private Boolean isCompound;
    private String testingStatus;

    private Timestamp startDate;
    private Timestamp finishDate;

    private MetaInfo metaInfo;
    private Set<String> configInfo;

}
