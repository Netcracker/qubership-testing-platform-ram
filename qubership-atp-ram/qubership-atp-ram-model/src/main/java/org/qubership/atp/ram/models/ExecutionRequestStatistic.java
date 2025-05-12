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

import java.util.UUID;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "executionRequestStatistics")
public class ExecutionRequestStatistic extends RamObject {
    private UUID executionRequestId;
    private UUID testRunId;
    private UUID parentId;
    private Integer tcCount;
    private Integer passedTcCount;
    private Integer passedRate;
    private Integer bpp;
    private Integer revenue;
    private Integer executed;
    private StatisticDataModelType type;

    private enum StatisticDataModelType {
        SUMMARY_STATISTIC,
        SUMMARY_STATISTIC_FOR_USAGES,
        SUMMARY_STATISTIC_SCENARIO_TYPE
    }
}
