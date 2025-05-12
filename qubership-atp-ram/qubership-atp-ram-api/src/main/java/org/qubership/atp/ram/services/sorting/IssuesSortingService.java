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

package org.qubership.atp.ram.services.sorting;

import static java.util.Comparator.comparing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.ram.comparators.ComplexListComparator;
import org.qubership.atp.ram.dto.response.FailPatternResponse;
import org.qubership.atp.ram.dto.response.IssueResponse;
import org.qubership.atp.ram.utils.StreamUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class IssuesSortingService {

    private static final ComplexListComparator<String> STRING_LIST_COMPARATOR =
            new ComplexListComparator<>(Comparator.naturalOrder());

    private final Map<String, Comparator<IssueResponse>> fieldToAscComparatorMap =
            ImmutableMap.<String, Comparator<IssueResponse>>builder()
                    .put("message", comparing(IssueResponse::getMessage))
                    .put("priority", comparing(IssueResponse::getPriority))
                    .put("failPattern", comparing(IssuesSortingService::getFailPatternNameForComparator))
                    .put("failReason", comparing(IssuesSortingService::getFailReasonNameForComparator))
                    .put("failedCasesCount", comparing(resp -> resp.getTestRuns().size()))
                    .put("failedTestRuns", (first, second) ->
                            STRING_LIST_COMPARATOR.compare(
                                    StreamUtils.extractNames(first.getTestRuns()),
                                    StreamUtils.extractNames(second.getTestRuns())))
                    .put("jiraTickets", (first, second) ->
                            STRING_LIST_COMPARATOR.compare(first.getJiraTickets(), second.getJiraTickets()))
                    .build();

    private final Map<String, Function<IssueResponse, Object>> fieldNameToFieldMethodMap =
            ImmutableMap.<String, Function<IssueResponse, Object>>builder()
                    .put("message", IssueResponse::getMessage)
                    .put("priority", IssueResponse::getPriority)
                    .put("failPattern", IssueResponse::getFailPattern)
                    .put("failReason", IssueResponse::getFailReason)
                    .put("failedCasesCount", resp -> resp.getTestRuns().size())
                    .put("failedTestRuns", resp ->  StreamUtils.extractNames(resp.getTestRuns()))
                    .put("jiraTickets", IssueResponse::getJiraTickets)
                    .build();


    /**
     * Apply sorting by field to list of responses.
     *
     * @param responses     list with issues
     * @param columnType    field to sort
     * @param sortDirection direction to sort
     */
    public List<IssueResponse> applySorting(List<IssueResponse> responses,
                                            String columnType, Sort.Direction sortDirection) {
        List<IssueResponse> result = responses;
        if (columnType != null) {
            Comparator<IssueResponse> comparator = fieldToAscComparatorMap.get(columnType);

            Map<Boolean, List<IssueResponse>> listByFieldIsNullPartition =
                    partitionListByFieldIsNull(responses, columnType);
            List<IssueResponse> issuesWithSortFieldIsNull = listByFieldIsNullPartition.get(true);
            List<IssueResponse> issuesWithSortFieldIsNotNull = listByFieldIsNullPartition.get(false);

            applySortingToItemsWithNotNullField(issuesWithSortFieldIsNotNull, columnType, sortDirection, comparator);

            result = new ArrayList<>(issuesWithSortFieldIsNotNull);
            result.addAll(issuesWithSortFieldIsNull);
        }
        return result;
    }

    private void applySortingToItemsWithNotNullField(List<IssueResponse> responses,
                                                     String columnType, Sort.Direction sortDirection,
                                                     Comparator<IssueResponse> comparator) {
        if (comparator != null) {
            if (sortDirection == Sort.Direction.DESC) {
                comparator = comparator.reversed();
            }
            responses.sort(comparator);
        } else {
            log.warn("Cannot sort issues by {}, skipping sorting...", columnType);
        }
    }

    private Map<Boolean, List<IssueResponse>> partitionListByFieldIsNull(List<IssueResponse> responses,
                                                                         String columnType) {
        Function<IssueResponse, Object> fieldExtractor = fieldNameToFieldMethodMap.get(columnType);
        return responses.stream().collect(Collectors.partitioningBy(item -> fieldExtractor.apply(item) == null));
    }

    private static String getFailPatternNameForComparator(IssueResponse issueResponse) {
        return issueResponse.getFailPattern() != null
                ? issueResponse.getFailPattern().getName()
                : StringUtils.EMPTY;
    }

    private static String getFailReasonNameForComparator(IssueResponse issueResponse) {
        FailPatternResponse failPattern = issueResponse.getFailPattern();
        boolean failReasonExists = failPattern != null && failPattern.getFailReason() != null;
        return failReasonExists
                ? failPattern.getFailReason().getName()
                : StringUtils.EMPTY;
    }


}
