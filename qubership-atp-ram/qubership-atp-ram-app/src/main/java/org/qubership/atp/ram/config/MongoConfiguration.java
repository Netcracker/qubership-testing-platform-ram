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

import static org.qubership.atp.ram.migration.MigrationConstants.CHANGE_LOGS_SCAN_PACKAGE;

import java.util.HashMap;
import java.util.Map;

import org.bson.UuidRepresentation;
import org.qubership.atp.auth.springbootstarter.services.UsersService;
import org.qubership.atp.ram.converters.DateToTimestampConverter;
import org.qubership.atp.ram.converters.FileTypeConverter;
import org.qubership.atp.ram.handlers.UpdatingIndexesHandler;
import org.qubership.atp.ram.migration.mongoevolution.SpringMongoEvolution;
import org.qubership.atp.ram.migration.mongoevolution.java.dataaccess.ConnectionSearchKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.SneakyThrows;

@Configuration
public class MongoConfiguration extends AbstractMongoClientConfiguration {

    private static final String DOT_REPLACEMENT = "-DOT";

    @Value("${mongodb.host}")
    private String host;
    @Value("${mongodb.port}")
    private String port;
    @Value("${mongodb.database}")
    private String database;
    @Value("${mongodb.user}")
    private String user;
    @Value("${mongodb.password}")
    private String password;
    @Value("${max.connection.idle.time}")
    private String maxConnectionIdleTime;
    @Value("${min.connections.per.host}")
    private String minConnectionsPerHost;
    @Value("${connections.per.host}")
    private String connectionsPerHost;
    @Value("${atp.ram.logrecords.index.date-expire-sec}")
    private long logrecordsIndexExpireDate;
    @Value("${atp.ram.context.variables.index.date-expire-sec}")
    private long logrecordsContextIndexExpireDate;

    private final UsersService usersService;

    private final Environment environment;

    private final MeterRegistry meterRegistry;

    public MongoConfiguration(final UsersService usersService,
                              final Environment environment,
                              final MeterRegistry meterRegistry) {
        this.usersService = usersService;
        this.environment = environment;
        this.meterRegistry = meterRegistry;
    }

    @Override
    protected String getDatabaseName() {
        return database;
    }

    @Override
    public void configureConverters(MongoCustomConversions.MongoConverterConfigurationAdapter adapter) {
        adapter.registerConverter(new FileTypeConverter());
        adapter.registerConverter(new DateToTimestampConverter());
    }

    /**
     * Provides {@link MongoTemplate} for getting files from database.
     * Properties should contain "mongodb.host", "mongodb.port", "mongodb.database", "mongodb.user"
     * and "mongodb.password".
     *
     * @return GridFSBucket by specified parameters.
     */
    @Bean
    @Override
    @SneakyThrows
    public MongoClient mongoClient() {
        MongoClient mongoClient = super.mongoClient();
        Map<String, Object> beansMap = new HashMap<String, Object>() {
            {
                put("usersService", usersService);
                put("environments", environment);
            }
        };
        new SpringMongoEvolution(mongoClient, database, new ConnectionSearchKey(null, "default"))
                .evolve(CHANGE_LOGS_SCAN_PACKAGE, environment, beansMap);
        new UpdatingIndexesHandler(mongoClient, database).checkAndRecreateIndexes(logrecordsIndexExpireDate,
                logrecordsContextIndexExpireDate);
        return mongoClient;
    }

    /**
     * Bean for MongoClientSettings.
     *
     * @return MongoClientSettings instance
     */
    @Override
    protected MongoClientSettings mongoClientSettings() {
        String mongoClientUri = "mongodb://" + user + ":" + password
                + "@" + host + ":" + Integer.parseInt(port) + "/?authSource"
                + "=" + database;
        return ConfigHelper.createOptsBuilder(mongoClientUri,
                        Integer.parseInt(maxConnectionIdleTime),
                        Integer.parseInt(minConnectionsPerHost),
                        Integer.parseInt(connectionsPerHost),
                        meterRegistry)
                .uuidRepresentation(UuidRepresentation.JAVA_LEGACY).build();
    }

    /**
     * Bean for MappingMongoConverter.
     *
     * @return MappingMongoConverter instance.
     * @throws Exception exception
     */
    @Bean
    public MappingMongoConverter mappingMongoConverter() throws Exception {
        DbRefResolver dbRefResolver = new DefaultDbRefResolver(mongoDbFactory());
        MappingMongoConverter mongoConverter = new MappingMongoConverter(dbRefResolver,
                mongoMappingContext(customConversions()));
        mongoConverter.setMapKeyDotReplacement(DOT_REPLACEMENT);
        mongoConverter.setCustomConversions(customConversions());
        return mongoConverter;
    }

    @Override
    protected boolean autoIndexCreation() {
        return true;
    }
}
