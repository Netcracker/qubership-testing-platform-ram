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
import java.util.UUID;

import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = false)
@Data
@Document(collection = "tools")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolsInfo extends RamObject {

    private List<WdShells> wdShells;
    private String sessionId;
    private String sessionLogsUrl;
    private String selenoid;
    private String selenoidLogsUrl;
    private String dealer;
    private String dealerLogsUrl;
    private String tool;
    private String toolLogsUrl;
    private UUID executionRequestId;
}
