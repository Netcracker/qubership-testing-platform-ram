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

package org.qubership.atp.ram.migration.changelogs;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bson.Document;
import org.modelmapper.ModelMapper;
import org.qubership.atp.ram.dto.response.MessageParameter;
import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.enums.TypeAction;
import org.qubership.atp.ram.migration.mongoevolution.java.annotation.ChangeLog;
import org.qubership.atp.ram.migration.mongoevolution.java.annotation.ChangeSet;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.MetaInfo;
import org.qubership.atp.ram.models.RamObject;
import org.qubership.atp.ram.models.RootCause;
import org.qubership.atp.ram.models.logrecords.TechnicalLogRecord;
import org.qubership.atp.ram.models.logrecords.parts.CommandInfo;
import org.qubership.atp.ram.models.logrecords.parts.ContextVariable;
import org.qubership.atp.ram.models.logrecords.parts.Log;
import org.qubership.atp.ram.models.logrecords.parts.ValidationTable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.DefaultIndexOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.PartialIndexFilter;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.schema.JsonSchemaObject;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ChangeLog(version = 3)
public class LogRecordsMigrateV3 {
    private ModelMapper modelMapper = new ModelMapper();
    private static final int STEP = 500;
    private static final String COLLECTION_NAME = "logrecord";
    private static final String FILE_METADATA_FIELD = "fileMetadata";

    /**
     * Update file metadata field for olg LogRecords. FileMetadata -> List(FileMetadata).
     * @param mongoTemplate mongo template
     */
    @ChangeSet(order = 1)
    public void updateLogRecordsFileMetadata(MongoTemplate mongoTemplate) {
        String processName = LogRecordsMigrateV3.class.getName();
        log.info("Start mongo evolution process: {}", processName);
        createIndexForFileMetadata(mongoTemplate);

        Query query = new Query().addCriteria(
                        where(FILE_METADATA_FIELD).exists(true).not().type(JsonSchemaObject.Type.ARRAY));
        List<OldLogRecord> oldLogRecords;
        int limit = STEP;
        int countLogRecords = 0;
        do {
            oldLogRecords = mongoTemplate.find(query.skip(limit - STEP).limit(limit), OldLogRecord.class,
                    COLLECTION_NAME);
            countLogRecords += oldLogRecords.size();
            oldLogRecords.forEach(oldLogRecord -> {
                        Class clazz = oldLogRecord.getClazz();
                        mongoTemplate.save(modelMapper.map(oldLogRecord, (Type) clazz), COLLECTION_NAME);
                    });
            limit += STEP;
        } while (!oldLogRecords.isEmpty());
        log.info("End mongo evolution process: {}. Number of updated log records: {}.", processName, countLogRecords);
    }

    private void createIndexForFileMetadata(MongoTemplate mongoTemplate) {
        Index myIndex = new Index()
                .named('_' + FILE_METADATA_FIELD)
                .on(FILE_METADATA_FIELD, Sort.Direction.ASC)
                .partial(PartialIndexFilter.of(where(FILE_METADATA_FIELD).exists(true)));

        DefaultIndexOperations indexOperations = new DefaultIndexOperations(mongoTemplate, COLLECTION_NAME, null);
        indexOperations.ensureIndex(myIndex);
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    private static class OldLogRecord extends RamObject {
        @Field("_class")
        private String clazz;
        private Date lastUpdated;
        private Date createdDate;
        private long createdDateStamp;
        private String message;
        private UUID parentRecordId;
        private UUID testRunId;
        private boolean isSection;
        private TestingStatuses testingStatus;
        private ExecutionStatuses executionStatus;
        private String server;
        private Date startDate;
        private Date endDate;
        private long duration;
        private RootCause rootCause;
        private String stackTrace;
        private boolean isCompaund;
        private String snapshotId;
        private Set<String> configInfoId = new HashSet<>();
        private String urlToLogCollectorData;
        private TypeAction type;
        private List<UUID> duplicateId = new ArrayList<>();
        private List<Log.TaToolLog> taToolsLogs;
        private ValidationTable validationTable;
        private Set<String> validationLabels;
        private List<MessageParameter> messageParameters;
        private boolean isMessageParametersPresent;
        private String preview;
        private MetaInfo metaInfo;
        private List<LogRecord.Child> children;
        private List<ContextVariable> contextVariables;
        private List<ContextVariable> stepContextVariables;
        private Document fileMetadata;
        private Document request;
        private Document response;
        private Map<String, String> rules;
        private String linkToTool;
        private Boolean isGroup;
        private String command;
        private Map<String, List<String>> result;
        private Map<String, String> connectionInfo;
        private String output;
        private CommandInfo commandInfo;
        private String screenId;

        public List<Document> getFileMetadata() {
            return Collections.singletonList(this.fileMetadata);
        }

        Class getClazz() {
            try {
                return ClassLoader.getSystemClassLoader().loadClass(this.clazz);
            } catch (ClassNotFoundException e) {
                log.error("Class not found: {}.", clazz, e);
                return TechnicalLogRecord.class;
            }
        }
    }
}
