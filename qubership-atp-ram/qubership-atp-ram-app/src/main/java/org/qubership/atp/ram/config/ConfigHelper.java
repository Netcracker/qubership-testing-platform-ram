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

import java.util.concurrent.TimeUnit;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.connection.ConnectionPoolSettings;
import com.mongodb.management.JMXConnectionPoolListener;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.mongodb.MongoMetricsCommandListener;
import io.micrometer.core.instrument.binder.mongodb.MongoMetricsConnectionPoolListener;

public class ConfigHelper {

    /**
     * Configure mongo client options builder for next creating.
     *
     * @param maxConnectionIdleTime the maximum idle time for a pooled connection im ms
     * @param minConnectionsPerHost the minimum size of the connection pool per host
     * @param connectionsPerHost    the maximum size of the connection pool per host
     * @return configured {@link MongoClientSettings}
     */
    public static MongoClientSettings.Builder createOptsBuilder(String url,
                                                                Integer maxConnectionIdleTime,
                                                                Integer minConnectionsPerHost,
                                                                Integer connectionsPerHost,
                                                                MeterRegistry meterRegistry) {
        return MongoClientSettings.builder()
                .retryWrites(true)
                .addCommandListener(new MongoMetricsCommandListener(meterRegistry))
                .applyToConnectionPoolSettings((ConnectionPoolSettings.Builder builder) -> {
                    builder.maxSize(connectionsPerHost)
                            .minSize(minConnectionsPerHost)
                            .maxConnectionIdleTime(maxConnectionIdleTime, TimeUnit.MILLISECONDS)
                            .addConnectionPoolListener(new JMXConnectionPoolListener())
                            .addConnectionPoolListener(new MongoMetricsConnectionPoolListener(meterRegistry))
                            .build();
                })
                .applyConnectionString(new ConnectionString(url));
    }
}
