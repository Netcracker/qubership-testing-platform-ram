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

package org.qubership.atp.ram.repository;

import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.qubership.atp.ram.model.IssueDto;
import org.qubership.atp.ram.models.DefectPriority;
import org.qubership.atp.ram.models.FailPattern;
import org.qubership.atp.ram.models.Issue;
import org.qubership.atp.ram.models.RootCause;
import org.qubership.atp.ram.pojo.IssueFilteringParams;
import org.qubership.atp.ram.repositories.impl.CustomIssueRepositoryImpl;
import org.qubership.atp.ram.repositories.impl.FieldConstants;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class CustomIssueRepositoryImplTest {
    private CustomIssueRepositoryImpl customIssueRepositoryImpl;
    private MongoTemplate mongoTemplate;

    private static final String FAIL_REASON_COLLECTION_NAME =
            RootCause.class.getAnnotation(org.springframework.data.mongodb.core.mapping.Document.class).collection();
    private static final String FAIL_PATTERN_COLLECTION_NAME =
            FailPattern.class.getAnnotation(org.springframework.data.mongodb.core.mapping.Document.class).collection();
    private static final String REGEX = ".*?";

    @BeforeEach
    public void init() {
        mongoTemplate = mock(MongoTemplate.class);
        customIssueRepositoryImpl = new CustomIssueRepositoryImpl(mongoTemplate);
    }

    @Test
    public void getSortedAndPaginatedIssuesByFilters_fullyFilledFilters_returnsOneTopIssue() {
        ArgumentCaptor<Aggregation> captor = ArgumentCaptor.forClass(Aggregation.class);

        IssueFilteringParams issueFilteringParams = new IssueFilteringParams();
        issueFilteringParams.setExecutionRequestId(randomUUID());
        issueFilteringParams.setFailPattern("url");
        issueFilteringParams.setFailReason("Environmental");
        issueFilteringParams.setJiraTicket("ATPII-3");
        issueFilteringParams.setMessage("execution failed with");
        issueFilteringParams.setLogRecordIds(Collections.singletonList(randomUUID()));
        issueFilteringParams.setPriority(DefectPriority.NORMAL);

        Issue issue = new Issue();
        List<Issue> data = new ArrayList<>();
        data.add(issue);

        IssueDto.MetaData metaData = new IssueDto.MetaData();
        List<IssueDto.MetaData> metadataList = new ArrayList<>();
        metadataList.add(metaData);

        IssueDto issueDtoAggregated = new IssueDto(data, metadataList);
        List<IssueDto> issueDtoAggregatedList = new ArrayList<>();
        issueDtoAggregatedList.add(issueDtoAggregated);

        AggregationResults aggregationResults = new AggregationResults<>(issueDtoAggregatedList, new org.bson.Document());
        when(mongoTemplate.aggregate(captor.capture(), anyString(), any())).thenReturn(aggregationResults);

        customIssueRepositoryImpl.getSortedAndPaginatedIssuesByFilters(issueFilteringParams,
                "columnType", "sortType", 0, 0);

        Mockito.verify(mongoTemplate, times(1)).aggregate(captor.capture(), anyString(), any());

        Aggregation resultOutput = captor.getValue();

        List<AggregationOperation> aggregationOperations = new ArrayList<>();
        aggregationOperations.add(
                Aggregation.match(where(Issue.EXECUTION_REQUEST_ID_FIELD)
                .is(issueFilteringParams.getExecutionRequestId())));
        aggregationOperations.add(
                Aggregation.match(where(Issue.LOG_RECORD_IDS_FIELD)
                .in(issueFilteringParams.getLogRecordIds())));
        aggregationOperations.add(
                Aggregation.match(where(Issue.PRIORITY_FIELD)
                .is(issueFilteringParams.getPriority())));
        aggregationOperations.add(
                Aggregation.match(where(Issue.MESSAGE_FIELD)
                .regex(REGEX + issueFilteringParams.getMessage() + REGEX)));
        aggregationOperations.add(
                Aggregation.match(where(Issue.JIRA_TICKETS_FIELD)
                .regex(REGEX + issueFilteringParams.getJiraTicket() + REGEX)));
        aggregationOperations.add(
                Aggregation.lookup(FAIL_PATTERN_COLLECTION_NAME, Issue.FAIL_PATTERN_ID_FIELD,
                        FieldConstants._ID, FieldConstants.FAIL_PATTERN));
        aggregationOperations.add(
                Aggregation.unwind(FieldConstants.FAIL_PATTERN, true));
        aggregationOperations.add(
                Aggregation.lookup(FAIL_REASON_COLLECTION_NAME, Issue.FAIL_REASON_ID_FIELD, FieldConstants._ID, FieldConstants.FAIL_REASON));
        aggregationOperations.add(
                Aggregation.unwind(FieldConstants.FAIL_REASON, true));
        aggregationOperations.add(
                Aggregation.match(where(FieldConstants.FAIL_PATTERN_NAME_FIELD)
                        .regex(REGEX + issueFilteringParams.getFailPattern() + REGEX)));
        aggregationOperations.add(
                Aggregation.match(where(FieldConstants.FAIL_REASON_NAME_FIELD)
                .regex(REGEX + issueFilteringParams.getFailReason() + REGEX)));
        //aggregationOperations.add(
         //       Aggregation.addFields().addField(FieldConstants.FAIL_TEST_RUNS_COUNT_FIELD)
        //        .withValue(ArrayOperators.Size.lengthOfArray(Issue.FAILED_TEST_RUNS_IDS_FIELD)).build());

        for (int i = 0; i < 11; i++) {
            Assertions.assertEquals(resultOutput.getPipeline().getOperations().get(i).getOperator(),
                    aggregationOperations.get(i).getOperator());
        }
    }

}
