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

package org.qubership.atp.ram.config;

import org.bson.Document;
import org.bson.UuidRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.model.Indexes;
import io.micrometer.core.instrument.MeterRegistry;

@Configuration("ram-gridfs-config")
public class GridFsConfiguration {

    @Value("${gridfs.host}")
    private String host;
    @Value("${gridfs.port}")
    private String port;
    @Value("${gridfs.database}")
    private String database;
    @Value("${gridfs.user}")
    private String user;
    @Value("${gridfs.password}")
    private String password;
    @Value("${max.connection.idle.time}")
    private String maxConnectionIdleTime;
    @Value("${min.connections.per.host}")
    private String minConnectionsPerHost;
    @Value("${connections.per.host}")
    private String connectionsPerHost;

    /**
     * Provides {@link GridFSBucket} for getting files from database. Creates descending index on fs.files collection on
     * "metadata.logRecordUuid" field.
     * Properties should contains "gridfs.host","gridfs.port" and "gridfs.database".
     *
     * @return GridFSBucket by specified parameters.
     */
    @Bean
    public GridFSBucket provideGridFileSystemBuckets(MeterRegistry meterRegistry) {
        String mongoClientUri = "mongodb://" + user + ":" + password
                + "@" + host + ":" + Integer.parseInt(port) + "/?authSource"
                + "=" + database;
        MongoClientSettings.Builder mongoClientSettingsBuilder =
                ConfigHelper.createOptsBuilder(mongoClientUri,
                        Integer.parseInt(maxConnectionIdleTime),
                        Integer.parseInt(minConnectionsPerHost),
                        Integer.parseInt(connectionsPerHost),
                        meterRegistry)
                        .uuidRepresentation(UuidRepresentation.JAVA_LEGACY);
        MongoClient mongo = MongoClients.create(mongoClientSettingsBuilder.build());
        MongoDatabase db = mongo.getDatabase(database);
        GridFSBucket gridFsBucket = GridFSBuckets.create(db);
        MongoCollection<Document> filesCollection = db.getCollection("fs.files");
        MongoCollection<Document> chunksCollection = db.getCollection("fs.chunks");
        filesCollection.createIndex(Indexes.descending("metadata.logRecordUuid"));
        chunksCollection.createIndex(Indexes.descending("files_id"));
        return gridFsBucket;
    }
}
