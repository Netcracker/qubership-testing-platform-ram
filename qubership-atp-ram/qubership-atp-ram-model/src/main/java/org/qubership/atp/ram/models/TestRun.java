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

import static java.util.Objects.isNull;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.enums.Flags;
import org.qubership.atp.ram.enums.TestScopeSections;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Document(collection = "testrun")
@CompoundIndexes({
        @CompoundIndex(name = "_id_executionRequestId",
                def = "{'_id': 1, 'executionRequestId': 1}"),
        @CompoundIndex(name = "_id_parentTestRunId",
                def = "{'_id': 1, 'parentTestRunId': 1}"),
        @CompoundIndex(name = "testCaseId_startDateDesc",
                def = "{ 'testCaseId': 1, 'startDate': -1 }")
})
public class TestRun extends RamObject {

    public static final String TEST_RUN_JSON_FILTER_NAME = "Test run Json Filter";

    private UUID parentTestRunId;
    private boolean isGroupedTestRun;
    @Indexed
    private UUID executionRequestId;
    @Indexed(background = true)
    private UUID testCaseId;
    private String testCaseName;
    private ExecutionStatuses executionStatus;
    private TestingStatuses testingStatus = TestingStatuses.UNKNOWN;//moved from constructor to be able to deserialize
    // null values
    private Timestamp startDate;
    private Timestamp finishDate;
    @CreatedDate
    private Timestamp createdDate;
    private long duration;
    private String executor;
    private String jiraTicket;
    private List<String> taHost = new ArrayList<>();
    private List<String> qaHost = new ArrayList<>();
    private List<String> solutionBuild = new ArrayList<>();
    private UUID rootCauseId;
    private String dataSetUrl;
    private List<Flags> flags;
    private String dataSetListUrl;
    private String logCollectorData;
    private boolean fdrWasSent;
    private String fdrLink;
    private int numberOfScreens;
    private Set<String> urlToBrowserOrLogs;
    private String urlToBrowserSession;
    private int passedRate;
    private int warningRate;
    private int failedRate;
    private Comment comment;

    private MetaInfo metaInfo;
    private TestRunStatistic statistic;
    private TestScopeSections testScopeSection;
    private int order;
    private Set<UUID> labelIds;
    private List<String> browserNames;

    private boolean isFinalTestRun;
    private UUID initialTestRunId;

    public TestRun() {
    }

    public boolean isFinalTestRun() {
        return this.isFinalTestRun;
    }

    public Boolean getIsFinalTestRun() {
        return isFinalTestRun;
    }

    /**
     * Updates testing status according to current status and the new status priorities.
     * If current status is null, new value is set.
     */
    public void updateTestingStatus(TestingStatuses testingStatus) {
        if (this.testingStatus == null) {
            this.testingStatus = testingStatus;
            return;
        }
        if (Objects.nonNull(testingStatus)) {
            this.testingStatus = TestingStatuses.compareAndGetPriority(this.testingStatus, testingStatus);
        }
    }

    /**
     * Set the new status.
     */
    public void setTestingStatus(TestingStatuses testingStatus) {
        this.testingStatus = testingStatus;
    }

    public void addNumberOfScreens() {
        this.numberOfScreens = this.numberOfScreens + 1;
    }

    /**
     * Add url to set urls.
     *
     * @param urlToBrowserOrLogs url to browser.
     */
    public void addUrlToBrowserOrLogs(String urlToBrowserOrLogs) {
        if (this.urlToBrowserOrLogs == null) {
            this.urlToBrowserOrLogs = new HashSet<>();
        }
        this.urlToBrowserOrLogs.add(urlToBrowserOrLogs);
    }

    public Set<UUID> getLabelIds() {
        return isNull(labelIds) ? new HashSet<>() : labelIds;
    }

    public TestRun isGroupedTestRun(Boolean isGroupedTestRun) {
        this.isGroupedTestRun = isGroupedTestRun;
        return this;
    }

    public boolean isGroupedTestRun() {
        return this.isGroupedTestRun;
    }

    public Boolean getIsGroupedTestRun() {
        return isGroupedTestRun;
    }
}
