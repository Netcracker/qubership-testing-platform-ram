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

import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.qubership.atp.ram.migration.mongoevolution.java.annotation.ChangeLog;
import org.qubership.atp.ram.migration.mongoevolution.java.annotation.ChangeSet;
import org.qubership.atp.ram.models.ExecutionRequestConfig;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.mongodb.BasicDBObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@ChangeLog(version = 5)
@Slf4j
public class DeleteDuplicateExecutionRequestConfigMigrateV5 {

    private static final String OPERATOR = "$";
    private static final String ID = "_id";
    private static final String $_id = OPERATOR + ID;
    private static final String EXECUTION_REQUEST_ID = "executionRequestId";
    private static final String EXECUTION_REQUEST_CONFIGS = "erConfigs";
    private static final String EXECUTION_REQUEST_CONFIG_ID = "erConfigId";
    private static final String COUNT = "count";
    private static final String EXECUTION_REQUEST_CONFIG_COLLECTION_NAME =
            ExecutionRequestConfig.class.getAnnotation(Document.class).collection();

    private ModelMapper modelMapper;

    /**
     * Delete duplicate execution request config by execution request id.
     *
     * @param mongoTemplate mongo template
     */
    @ChangeSet(order = 1)
    public void run(MongoTemplate mongoTemplate) {
        modelMapper = new ModelMapper();
        dropIndex(mongoTemplate);

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.group(EXECUTION_REQUEST_ID).count().as(COUNT)
                        .push(new BasicDBObject(EXECUTION_REQUEST_CONFIG_ID, $_id))
                        .as(EXECUTION_REQUEST_CONFIGS),
                Aggregation.match(Criteria.where(COUNT).gt(1))
        );
        List<org.bson.Document> documents = mongoTemplate.aggregate(aggregation,
                EXECUTION_REQUEST_CONFIG_COLLECTION_NAME, org.bson.Document.class).getMappedResults();

        List<UUID> erConfigIdsNeedToDelete = documents.stream()
                .map(document -> modelMapper.map(document, ExecutionRequest.class))
                .map(er -> er.getErConfigs().stream().map(Config::getErConfigId).skip(1).collect(toList()))
                .flatMap(Collection::stream)
                .collect(toList());

        mongoTemplate.remove(new Query(Criteria.where(ID).in(erConfigIdsNeedToDelete)), ExecutionRequestConfig.class);
        mongoTemplate.indexOps(EXECUTION_REQUEST_CONFIG_COLLECTION_NAME).ensureIndex(
                new Index().on(EXECUTION_REQUEST_ID, Sort.Direction.ASC)
                        .named(EXECUTION_REQUEST_ID)
                        .unique()
                        .background()
        );
    }

    private void dropIndex(MongoTemplate mongoTemplate) {
        IndexOperations indexOperations = mongoTemplate.indexOps(EXECUTION_REQUEST_CONFIG_COLLECTION_NAME);
        Optional<IndexInfo> indexInfo
                = indexOperations.getIndexInfo().stream()
                .filter(io -> io.getName().equals(EXECUTION_REQUEST_ID))
                .findFirst();
        if (indexInfo.isPresent()) {
            indexOperations.dropIndex(EXECUTION_REQUEST_ID);
        }
    }

    @Data
    public static class ExecutionRequest {
        @Id
        UUID id;
        long count;
        List<Config> erConfigs;
    }

    @Data
    public static class Config {
        UUID erConfigId;
    }
}
