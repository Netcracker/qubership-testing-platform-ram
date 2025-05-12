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

package org.qubership.atp.ram.model.jira;

import java.util.Collection;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.qubership.atp.ram.models.JiraComponent;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Fields {

    @NotEmpty
    private String summary;
    private String description;
    private Project project;
    @NotNull
    private Priority priority;
    private Collection<String> labels;
    private IssueType issuetype;
    @NotEmpty
    private Collection<JiraComponent> components;
    @JsonProperty("customfield_17400")
    private String atpLink;
    @JsonProperty("customfield_10014")
    private FoundIn foundIn;
    private Status status;
    private String environment;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FoundIn {
        @NotEmpty
        private String value;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Project {
        private String key;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Priority {
        private String name;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class IssueType {
        private String name;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Status {
        private String name;
    }
}
