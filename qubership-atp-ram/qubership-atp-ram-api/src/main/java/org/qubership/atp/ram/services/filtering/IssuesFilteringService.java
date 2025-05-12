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

package org.qubership.atp.ram.services.filtering;

import static org.qubership.atp.ram.models.Issue.EXECUTION_REQUEST_ID_FIELD;
import static org.qubership.atp.ram.models.Issue.LOG_RECORD_IDS_FIELD;
import static org.qubership.atp.ram.models.Issue.PRIORITY_FIELD;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.qubership.atp.ram.dto.response.FailPatternResponse;
import org.qubership.atp.ram.dto.response.IssueResponse;
import org.qubership.atp.ram.pojo.IssueFilteringParams;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.CriteriaDefinition;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IssuesFilteringService {


    /**
     * Build searchCriteria to find necessary Issues by fields.
     *
     * @return search criteria
     */
    public CriteriaDefinition buildSearchCriteria(IssueFilteringParams issueFilteringParams) {
        if (issueFilteringParams == null) {
            return new Criteria();
        }
        List<Criteria> filters = new ArrayList<>();
        if (issueFilteringParams.getExecutionRequestId() != null) {
            filters.add(Criteria.where(EXECUTION_REQUEST_ID_FIELD).is(issueFilteringParams.getExecutionRequestId()));
        }
        if (issueFilteringParams.getLogRecordIds() != null) {
            filters.add(Criteria.where(LOG_RECORD_IDS_FIELD).all(issueFilteringParams.getLogRecordIds()));
        }
        if (issueFilteringParams.getPriority() != null) {
            filters.add(Criteria.where(PRIORITY_FIELD).is(issueFilteringParams.getPriority().name()));
        }
        return new Criteria().andOperator(filters.toArray(new Criteria[0]));
    }

    /** Apply filters to list od IssueResponses and return filtered list.
     * @param responses list of data
     * @param issueFilteringParams filtring data
     * @return filtered list
     */
    public List<IssueResponse> applyAdditionalFiltering(List<IssueResponse> responses,
                                                        IssueFilteringParams issueFilteringParams) {
        if (issueFilteringParams == null) {
            return responses;
        }
        List<Predicate<IssueResponse>> filterPredicates = buildAllFilterPredicates(issueFilteringParams);
        return responses.stream()
                .filter(item -> allPredicatesMatch(filterPredicates, item))
                .collect(Collectors.toList());
    }

    private List<Predicate<IssueResponse>> buildAllFilterPredicates(IssueFilteringParams issueFilteringParams) {
        List<Predicate<IssueResponse>> predicates = new ArrayList<>();

        if (issueFilteringParams.getFailedTestRun() != null) {
            predicates.add(failedTestRunPredicate(issueFilteringParams.getFailedTestRun()));
        }
        if (issueFilteringParams.getFailReason() != null) {
            predicates.add(failReasonPredicate(issueFilteringParams.getFailReason()));
        }
        if (issueFilteringParams.getJiraTicket() != null) {
            predicates.add(jiraTicketPredicate(issueFilteringParams.getJiraTicket()));
        }
        if (issueFilteringParams.getFailPattern() != null) {
            predicates.add(failPatternPredicate(issueFilteringParams.getFailPattern()));
        }
        if (issueFilteringParams.getMessage() != null) {
            predicates.add(failMessagePredicate(issueFilteringParams.getMessage()));
        }
        return predicates;
    }

    private Predicate<IssueResponse> failMessagePredicate(String message) {
        return item -> {
            String issueMessage = item.getMessage();
            Boolean validation = checkMessageNullValidations(message, issueMessage);
            if (validation != null) {
                return validation;
            }
            return issueMessage.toLowerCase().contains(message.toLowerCase());
        };
    }

    private Boolean checkMessageNullValidations(String message, String issueMessage) {
        if (issueMessage == null) {
            return message == null;
        } else {
            if (message == null) {
                return false;
            }
        }
        return null;
    }

    private Predicate<IssueResponse> failPatternPredicate(String failPattern) {
        return item -> {
            FailPatternResponse failPatternResponse = item.getFailPattern();
            if (failPatternResponse == null || failPatternResponse.getName() == null) {
                return false;
            }
            return failPatternResponse.getName().toLowerCase().contains(failPattern.toLowerCase());
        };
    }

    private Predicate<IssueResponse> jiraTicketPredicate(String jiraTicket) {
        return item -> {
            if (CollectionUtils.isEmpty(item.getJiraTickets())) {
                return false;
            }
            return item.getJiraTickets().stream().anyMatch(ticketName ->
                    ticketName != null && ticketName.toLowerCase().contains(jiraTicket.toLowerCase()));
        };
    }

    private Predicate<IssueResponse> failReasonPredicate(String failReason) {
        return item -> {
            if (item.getFailReason() == null || item.getFailReason().getName() == null) {
                return false;
            }
            return item.getFailReason().getName().toLowerCase().contains(failReason.toLowerCase());
        };
    }

    private Predicate<IssueResponse> failedTestRunPredicate(String failedTestRun) {
        return item -> {
            if (CollectionUtils.isEmpty(item.getTestRuns())) {
                return false;
            }
            return item.getTestRuns().stream()
                    .anyMatch(testRun -> failedTestRun.equals(testRun.getName()));
        };
    }

    private boolean allPredicatesMatch(List<Predicate<IssueResponse>> predicates, IssueResponse item) {
        return predicates.stream().allMatch(predicate -> predicate.test(item));
    }

}
