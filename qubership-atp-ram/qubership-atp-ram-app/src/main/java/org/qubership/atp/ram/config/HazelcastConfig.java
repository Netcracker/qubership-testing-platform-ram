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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.qubership.atp.ram.constants.CacheConstants;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.cache.CacheBuilder;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.ClientConnectionStrategyConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.HazelcastInstance;

@Configuration
@EnableCaching
@ConditionalOnProperty(
        value = "hazelcast.enable-caching",
        havingValue = "true",
        matchIfMissing = false
)
public class HazelcastConfig {

    @Value("${hazelcast.cluster-name}")
    private String clusterName;

    @Value("${hazelcast.address}")
    private String hazelcastAddress;

    /**
     * Create {@link ClientConfig} bean.
     *
     * @return bean
     */
    @Bean(name = "clientConfig")
    public ClientConfig clientConfig() {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setClusterName(clusterName);
        clientConfig.setInstanceName("atp-ram-" + UUID.randomUUID());
        clientConfig.getNetworkConfig().addAddress(hazelcastAddress);
        clientConfig.getConnectionStrategyConfig()
                .setReconnectMode(ClientConnectionStrategyConfig.ReconnectMode.ASYNC);
        return clientConfig;
    }

    /**
     * Create {@link HazelcastInstance} bean.
     *
     * @return bean
     */
    @Bean
    public HazelcastInstance hazelcastClient(@Qualifier("clientConfig") ClientConfig clientConfig) {
        HazelcastInstance hazelcastClient = HazelcastClient.getOrCreateHazelcastClient(clientConfig);
        hazelcastClient.getConfig()
                .addMapConfig(new MapConfig(CacheConstants.ATP_RAM_REPORTS))
                .addMapConfig(new MapConfig(CacheConstants.ATP_RAM_DICTIONARIES));
        return hazelcastClient;
    }

    /**
     * Create {@link CacheManager} bean.
     *
     * @return bean
     */
    @Bean(name = "hazelcastCacheManager")
    public CacheManager hazelcastCacheManager(HazelcastInstance hazelcastClient) {
        List<Cache> caches = new ArrayList<>();
        caches.add(new ConcurrentMapCache(CacheConstants.ATP_RAM_REPORTS,
                hazelcastClient.getMap(CacheConstants.ATP_RAM_REPORTS), true));
        caches.add(new ConcurrentMapCache(CacheConstants.ROOTCAUSES_CACHE));
        caches.add(new ConcurrentMapCache(CacheConstants.ATP_RAM_DICTIONARIES,
                hazelcastClient.getMap(CacheConstants.ATP_RAM_DICTIONARIES), true));
        caches.add(new ConcurrentMapCache(CacheConstants.PROJECT_CACHE,
                CacheBuilder.newBuilder().expireAfterWrite(2, TimeUnit.MINUTES).maximumSize(100).build().asMap(),
                true));
        caches.add(new ConcurrentMapCache(CacheConstants.TEST_RUNS_INFO_CACHE,
                CacheBuilder.newBuilder().expireAfterWrite(2, TimeUnit.MINUTES).maximumSize(100).build().asMap(),
                true));
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(caches);
        return cacheManager;
    }
}
