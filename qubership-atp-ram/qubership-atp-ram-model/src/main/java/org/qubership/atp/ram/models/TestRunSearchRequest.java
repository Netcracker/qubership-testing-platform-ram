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

import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.validation.constraints.NotNull;

import org.qubership.atp.ram.enums.TestingStatuses;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

/**
 * Filter for test runs for analyze.
 */
@Data
public class TestRunSearchRequest {

    @NotNull
    private UUID executionRequestId;

    private String testCaseName;

    private String nameContains;

    private Set<UUID> testRunIds;

    private Set<TestingStatuses> inTestingStatuses;

    private Set<TestingStatuses> notInTestingStatuses;

    private Set<UUID> failureReasons;

    private Comment comment;

    private List<String> labelNames;
    private List<String> labelNameContains;
    @JsonIgnore
    private Set<UUID> labelIds;
}
