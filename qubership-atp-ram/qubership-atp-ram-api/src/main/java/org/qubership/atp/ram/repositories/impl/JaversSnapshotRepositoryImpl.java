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

import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.qubership.atp.ram.model.JaversCountResponse;
import org.qubership.atp.ram.model.JaversIdsResponse;
import org.qubership.atp.ram.repositories.JaversSnapshotRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class JaversSnapshotRepositoryImpl implements JaversSnapshotRepository {

    private final MongoTemplate mongoTemplate;

    private static final String JV_SNAPSHOT_COLLECTION_NAME = "jv_snapshots";
    private static final String SEPARATOR = ".";
    private static final String LINK = "$";
    private static final String ID = "_id";
    private static final String GLOBAL_ID = "globalId";
    private static final String CDO_ID = "cdoId";
    private static final String VERSION = "version";
    private static final String DELETE = "delete";
    private static final String VERSIONS = "versions";
    private static final String TYPE = "type";
    private static final String INITIAL_VALUE = "INITIAL";
    private static final String TERMINAL_VALUE = "TERMINAL";
    private static final String LINK_ID = LINK + ID;
    private static final String GLOBAL_ID_CDO_ID = GLOBAL_ID + SEPARATOR + CDO_ID;
    private static final String LINK_GLOBAL_ID_CDO_ID = LINK + GLOBAL_ID_CDO_ID;
    private static final String LINK_VERSION = LINK + VERSION;

    /**
     * Returns cdoId and number of objects with current cdoId.
     * Query example:
     * {
     *     "$match": {
     *         "globalId.cdoId": {
     *             "$in": {listOfCdoId}
     *         }
     *     }
     * }, {
     *     "$group": {
     *         "_id": "$globalId.cdoId",
     *         "version": {
     *             "$addToSet": "$version"
     *         }
     *     }
     * }, {
     *     "$project": {
     *         "cdoId": "$_id",
     *         "$versions": "$version",
     *         "_id": 0
     *     }
     * }
     *
     * @return {@link List} of {@link JaversCountResponse}
     */
    @Override
    public List<JaversCountResponse> findCdoIdAndCount(List<String> listOfCdoId) {
        MatchOperation matchOperation = Aggregation.match(where(GLOBAL_ID_CDO_ID).in(listOfCdoId));
        GroupOperation groupOperation = Aggregation.group(LINK_GLOBAL_ID_CDO_ID)
                .addToSet(LINK_VERSION).as(VERSION);
        ProjectionOperation projectionOperation = Aggregation.project()
                .and(LINK_ID).as(CDO_ID)
                .and(LINK_VERSION).as(VERSIONS)
                .andExclude(ID);
        Aggregation aggregation = Aggregation.newAggregation(matchOperation, groupOperation, projectionOperation);
        AggregationResults<JaversCountResponse> aggregationResults =
                mongoTemplate.aggregate(aggregation, JV_SNAPSHOT_COLLECTION_NAME, JaversCountResponse.class);
        return aggregationResults.getMappedResults();
    }

    /**
     * Returns all unique cdoIds.
     * Query example:
     * {
     *     "$group": {
     *         "_id": "$globalId.cdoId",
     *     }
     * }, {
     *     "$project": {
     *         "cdoId": "$_id",
     *         "_id": 0
     *     }
     * }
     *
     * @return {@link List} of {@link JaversIdsResponse}
     */
    @Override
    public Stream<JaversIdsResponse> findAllCdoIds() {
        GroupOperation groupOperation = Aggregation.group(LINK_GLOBAL_ID_CDO_ID);
        ProjectionOperation projectionOperation = Aggregation.project()
                .and(LINK_ID).as(CDO_ID)
                .andExclude(ID);
        Aggregation aggregation = Aggregation.newAggregation(groupOperation, projectionOperation);
        Iterable<JaversIdsResponse> iterable = () -> mongoTemplate.aggregateStream(aggregation,
                JV_SNAPSHOT_COLLECTION_NAME,
                JaversIdsResponse.class);
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    /**
     * Remove old objects by cdoId and versions.
     * Query example:
     * {
     *     "globalId.cdoId": {cdoId},
     *     "version": {
     *         "$in": [{versions}]
     *     }
     * }
     *
     * @param cdoId    cdo id
     * @param versions {@link List} of {@link Long} of old versions
     */
    @Override
    public void deleteByCdoIdAndVersions(String cdoId, List<Long> versions) {
        Query query = new Query()
                .addCriteria(where(GLOBAL_ID_CDO_ID).is(cdoId))
                .addCriteria(where(VERSION).in(versions));
        mongoTemplate.remove(query, JV_SNAPSHOT_COLLECTION_NAME);
    }

    /**
     * Update object with min version as initial.
     * Query example:
     * {
     *     "globalId.cdoId": {cdoId},
     *     "version": {version},
     * }, {
     *     $set: {
     *         "type": "INITIAL"
     *     }
     * }
     *
     * @param cdoId   cdo id
     * @param version version
     */
    @Override
    public void updateAsInitial(String cdoId, Long version) {
        Query query = new Query()
                .addCriteria(where(GLOBAL_ID_CDO_ID).is(cdoId))
                .addCriteria(where(VERSION).is(version));
        Update update = new Update()
                .set(TYPE, INITIAL_VALUE);
        mongoTemplate.updateFirst(query, update, JV_SNAPSHOT_COLLECTION_NAME);
    }

    /**
     * Returns terminal snapshots id.
     * Query example:
     * {
     *     $match: {
     *         "type": "TERMINAL"
     *     }
     * },
     * {
     *     $project: {
     *         "cdoId": "$globalId.cdoId",
     *         "_id": 0
     *     }
     * }
     *
     * @return {@link List} of {@link JaversIdsResponse}
     */
    @Override
    public List<JaversIdsResponse> findTerminatedSnapshots() {
        MatchOperation matchOperation = Aggregation.match(where(TYPE).is(TERMINAL_VALUE));
        ProjectionOperation projectionOperation = Aggregation.project()
                .and(LINK_GLOBAL_ID_CDO_ID).as(CDO_ID)
                .andExclude(ID);
        Aggregation aggregation = Aggregation.newAggregation(matchOperation, projectionOperation);
        AggregationResults<JaversIdsResponse> aggregationResults =
                mongoTemplate.aggregate(aggregation, JV_SNAPSHOT_COLLECTION_NAME, JaversIdsResponse.class);
        return aggregationResults.getMappedResults();
    }

    /**
     * Remove objects by cdoIds.
     * Query example:
     * {
     *     "globalId.cdoId": {
     *         $in: [{cdoIds}]
     *     }
     * }
     *
     * @param cdoIds {@link List} of {@link String} of old cdoIds.
     */
    @Override
    public void deleteByCdoIds(List<String> cdoIds) {
        Query query = new Query()
                .addCriteria(where(GLOBAL_ID_CDO_ID).in(cdoIds));
        mongoTemplate.remove(query, JV_SNAPSHOT_COLLECTION_NAME);
    }
}
