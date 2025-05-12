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

import static org.springframework.util.CollectionUtils.isEmpty;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.javers.core.metamodel.annotation.DiffInclude;
import org.javers.core.metamodel.annotation.TypeName;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "failPattern")
@TypeName("failPattern")
public class FailPattern extends DateAuditorEntity {

    @DiffInclude
    private DefectPriority priority;
    @Indexed(background = true)
    private UUID projectId;
    @DiffInclude
    private String message;
    @DiffInclude
    private String rule;
    @DiffInclude
    private String patternDescription;
    @DiffInclude
    private List<String> jiraTickets;
    @DiffInclude
    private List<JiraTicket> jiraDefects;
    @DiffInclude
    private UUID failReasonId;

    /**
     * Propagate jira tickets.
     */
    public void propagateJiraTickets() {
        if (!isEmpty(jiraDefects)) {
            jiraTickets = jiraDefects.stream()
                    .map(JiraTicket::getUrl)
                    .collect(Collectors.toList());
        }
    }
}
