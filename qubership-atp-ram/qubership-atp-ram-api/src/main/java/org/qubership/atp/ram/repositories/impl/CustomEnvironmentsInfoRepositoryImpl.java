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

package org.qubership.atp.ram.repositories.impl;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.qubership.atp.ram.models.EnvironmentsInfo;
import org.qubership.atp.ram.repositories.CustomEnvironmentsInfoRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@Repository
public class CustomEnvironmentsInfoRepositoryImpl implements CustomEnvironmentsInfoRepository {

    private final MongoTemplate mongoTemplate;

    private static final String ENVIRONMENTS_INFO_COLLECTION_NAME =
            EnvironmentsInfo.class.getAnnotation(Document.class).collection();

    @Override
    public List<EnvironmentsInfo> findByExecutionRequestIdInForErTestResult(Set<UUID> requestIds) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(new Criteria(FieldConstants.EXECUTION_REQUEST_ID).in(requestIds)),
                Aggregation.project(FieldConstants.ENVIRONMENT_ID, FieldConstants.NAME,
                                FieldConstants.EXECUTION_REQUEST_ID, FieldConstants.QA_SYSTEM_INFO_LIST,
                                FieldConstants.TA_SYSTEM_INFO_LIST)
                        .andExclude(FieldConstants._ID)
        );
        AggregationResults<EnvironmentsInfo> res = mongoTemplate
                .aggregate(aggregation, ENVIRONMENTS_INFO_COLLECTION_NAME, EnvironmentsInfo.class);

        return res.getMappedResults();
    }
}
