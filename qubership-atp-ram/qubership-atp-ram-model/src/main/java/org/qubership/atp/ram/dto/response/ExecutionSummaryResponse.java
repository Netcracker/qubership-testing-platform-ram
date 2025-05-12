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

package org.qubership.atp.ram.dto.response;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.qubership.atp.ram.models.Label;
import org.qubership.atp.ram.models.UserInfo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class ExecutionSummaryResponse {
    private UUID uuid;
    private String name;
    private Timestamp startDate;
    private Timestamp finishDate;
    private int testCasesCount;
    private float passedRate;
    private Integer passedCount = 0;
    private float warningRate;
    private Integer warningCount = 0;
    private float failedRate;
    private Integer failedCount = 0;
    private Integer skippedCount = 0;
    private float notStartedRate;
    private Integer notStartedCount = 0;
    private float stoppedRate;
    private Integer stoppedCount = 0;
    private float blockedRate;
    private Integer blockedCount = 0;
    private Integer inProgressCount = 0;
    private long duration;
    private List<String> browserSessionLink;
    private String environmentLink;
    private String environmentName;
    private int threads;
    private UUID environmentId;
    @JsonIgnore
    private UserInfo userInfo;
    private List<Label> labels;
    private String scopeName;
    private String scopeLink;

    /**
     * Getter browser session links.
     *
     * @return list of links, or empty list, if not exists
     */
    public List<String> getBrowserSessionLink() {
        if (Objects.nonNull(browserSessionLink)) {
            return browserSessionLink;
        }
        return new ArrayList<>();
    }
}
