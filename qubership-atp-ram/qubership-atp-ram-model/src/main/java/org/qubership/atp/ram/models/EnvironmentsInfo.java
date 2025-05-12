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

package org.qubership.atp.ram.models;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
@Document(collection = "environments_info")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnvironmentsInfo extends RamObject {

    @Indexed
    private UUID executionRequestId;
    private String status;
    private Timestamp startDate;
    private Timestamp endDate;
    private long duration;
    @Transient
    private ToolsInfo toolsInfo;
    @Field("toolsInfo")
    private UUID toolsInfoUuid;
    private List<SystemInfo> qaSystemInfoList;
    private List<SystemInfo> taSystemInfoList;
    private UUID environmentId;
    private UUID taToolsGroupId;
    private UUID mandatoryChecksReportId;
    private SsmMetricReports ssmMetricReports;
}
