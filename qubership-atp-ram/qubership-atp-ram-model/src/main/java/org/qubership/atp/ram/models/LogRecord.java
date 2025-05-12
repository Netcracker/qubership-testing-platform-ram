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

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static java.util.Objects.nonNull;
import static org.qubership.atp.ram.enums.ExecutionStatuses.NOT_STARTED;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.qubership.atp.ram.dto.response.MessageParameter;
import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.enums.TypeAction;
import org.qubership.atp.ram.models.logrecords.parts.ContextVariable;
import org.qubership.atp.ram.models.logrecords.parts.FileMetadata;
import org.qubership.atp.ram.models.logrecords.parts.Log.TaToolLog;
import org.qubership.atp.ram.models.logrecords.parts.ValidationTable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonInclude(NON_NULL)
@Data
@Document(collection = "logrecord")
@CompoundIndexes({
        @CompoundIndex(name = "_id_parentRecordId_testRunId",
                def = "{'_id': 1, 'parentRecordId': 1, 'testRunId': 1}"),
        @CompoundIndex(name = "testRunId_startDate",
                def = "{'testRunId': 1, 'startDate': 1}"),
        @CompoundIndex(name = "_testRunId_createdDate",
                def = "{'testRunId': 1, 'createdDate': 1}", background = true),
        @CompoundIndex(name = "_name_testRunId",
                def = "{'name': 'text', 'testRunId': 1}", background = true),
        @CompoundIndex(name = "_testRunId_parentRecordId_createdDateStamp",
                def = "{'testRunId': 1, 'parentRecordId': 1, 'createdDateStamp': 1}", background = true),
        @CompoundIndex(name = "_testRunId_lastUpdated",
                def = "{'testRunId': 1, 'lastUpdated': 1}", background = true),
        @CompoundIndex(name = "_testRunId_fileMetadata.type",
                def = "{'testRunId': 1, 'fileMetadata.type': 1}", background = true),
        @CompoundIndex(name = "_testRunId_testingStatus",
                def = "{'testRunId': 1, 'testingStatus': 1}", background = true)
})
public class LogRecord extends RamObject {

    private String protocolType;
    @LastModifiedDate
    private Date lastUpdated;
    @CreatedDate
    private Timestamp createdDate;
    private long createdDateStamp;
    private String message;
    @Indexed
    private UUID parentRecordId;
    @Indexed
    private UUID testRunId;
    private boolean isSection;
    private TestingStatuses testingStatus;
    private ExecutionStatuses executionStatus = NOT_STARTED;
    private String server;
    @Indexed(background = true)
    private Timestamp startDate;
    private Timestamp endDate;
    private long duration;
    private RootCause rootCause;
    private String stackTrace;
    private boolean isCompaund;
    private String snapshotId;
    private Set<String> configInfoId = new HashSet<>();
    private String urlToLogCollectorData;
    private String linkToSvp;
    private TypeAction type;
    private List<UUID> duplicateId = new ArrayList<>();
    private List<TaToolLog> taToolsLogs;
    private ValidationTable validationTable;
    private Set<String> validationLabels;
    private List<FileMetadata> fileMetadata;
    @Transient
    @JsonIgnore
    private List<MessageParameter> messageParameters;
    @JsonIgnore
    @Transient
    private List<ContextVariable> contextVariables;
    @JsonIgnore
    @Transient
    private List<ContextVariable> stepContextVariables;

    @JsonProperty("isBrowserConsoleLogsPresent")
    private boolean isBrowserConsoleLogsPresent;
    private boolean isMessageParametersPresent;
    private Table table;
    private SsmMetricReports ssmMetricReports;

    /**
     * Encoded in base64.
     */
    private String preview;
    /**
     * Meta info about source action.
     */
    private MetaInfo metaInfo;
    private List<Child> children;

    private boolean isContextVariablesPresent;
    private List<CustomLink> customLinks;

    public LogRecord() {
        this.testingStatus = TestingStatuses.UNKNOWN;
    }

    /**
     * Compare the current status and the new status.
     */
    public void setTestingStatus(TestingStatuses testingStatus) {
        if (nonNull(testingStatus)) {
            this.testingStatus = TestingStatuses.compareAndGetPriority(this.testingStatus, testingStatus);
        }
    }

    /**
     * Set the new status.
     */
    public void setTestingStatusHard(TestingStatuses testingStatus) {
        if (nonNull(testingStatus)) {
            this.testingStatus = testingStatus;
        }
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Child extends RamObject {

        private TypeAction type;
        private TestingStatuses testingStatus;
        private ExecutionStatuses executionStatus;
        private StepLinkMetaInfo editorMetaInfo;
        private List<CustomLink> customLinks;

        /**
         * Child constructor.
         *
         * @param logRecord log record
         */
        public Child(LogRecord logRecord) {
            super(logRecord.getUuid(), logRecord.getName());
            this.type = logRecord.getType();
            this.testingStatus = logRecord.getTestingStatus();
            this.executionStatus = logRecord.getExecutionStatus();
            this.editorMetaInfo = logRecord.getMetaInfo() == null
                    || logRecord.getMetaInfo().getEditorMetaInfo() == null
                    || StringUtils.isEmpty(logRecord.getMetaInfo().getEditorMetaInfo().getEngineType())
                    ? null
                    : logRecord.getMetaInfo().getEditorMetaInfo();
            this.customLinks = logRecord.getCustomLinks();
        }
    }

    public boolean isContextVariablesPresent() {
        return isContextVariablesPresent || contextVariables != null && !contextVariables.isEmpty()
                || stepContextVariables != null && !stepContextVariables.isEmpty();
    }

    /**
     * Json property for isMessageParametersPresent.
     *
     * @return true if messageParameters not empty or isMessageParametersPresent = true
     */
    @JsonProperty(value = "isMessageParametersPresent")
    public boolean isMessageParametersPresent() {
        return isMessageParametersPresent || !isEmpty(messageParameters);
    }
}
