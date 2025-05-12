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

package org.qubership.atp.ram.service.template.impl;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.qubership.atp.ram.dto.response.IssueResponse;
import org.qubership.atp.ram.models.DefectPriority;

import lombok.AllArgsConstructor;
import lombok.Builder;

@AllArgsConstructor
@Builder
public class IssueResponseAdapter {

    private IssueResponse issueResponse;
    private List<TestRunAdapter> testRuns;
    private List<JiraTicketAdapter> tickets;
    private FailPatternAdapter failPattern;
    private String message;

    public IssueResponseAdapter(IssueResponse issueResponse) {
        this.issueResponse = issueResponse;
    }

    public UUID getUuid() {
        return issueResponse.getUuid();
    }

    public String getName() {
        return issueResponse.getName();
    }

    public String getErrorMessage() {
        return issueResponse.getMessage();
    }

    public String getFailPattern() {
        return Objects.isNull(failPattern) ? "" : failPattern.getName();
    }

    public String getFailPatternUrl() {
        return Objects.isNull(failPattern) ? "" : failPattern.getUrl();
    }

    public String getFailReason() {
        return Objects.isNull(issueResponse.getFailReason()) ? "" : issueResponse.getFailReason().getName();
    }

    public List<JiraTicketAdapter> getTickets() {
        return tickets;
    }

    public DefectPriority getPriority() {
        return issueResponse.getPriority();
    }

    public List<TestRunAdapter> getTestRuns() {
        return testRuns;
    }

    public int getTestRunsCount() {
        return issueResponse.getTestRuns().size();
    }

    public String getMessage() {
        return message;
    }
}
