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
import java.util.concurrent.TimeUnit;

import org.qubership.atp.ram.constants.CacheConstants;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
@ConditionalOnProperty(
        value = "hazelcast.enable-caching",
        havingValue = "false",
        matchIfMissing = true
)
public class CacheConfig {

    /**
     * Create {@link CacheManager} bean.
     *
     * @return bean
     */
    @Bean
    public CacheManager cacheManager() {
        List<Cache> caches = new ArrayList<>();
        caches.add(new CaffeineCache(CacheConstants.ATP_RAM_REPORTS,
                Caffeine.newBuilder().recordStats().expireAfterWrite(2, TimeUnit.MINUTES).build(), true));
        caches.add(new CaffeineCache(CacheConstants.ROOTCAUSES_CACHE,
                Caffeine.newBuilder().recordStats().expireAfterWrite(2, TimeUnit.MINUTES).build()));
        caches.add(new CaffeineCache(CacheConstants.ATP_RAM_DICTIONARIES,
                Caffeine.newBuilder().recordStats().expireAfterWrite(2, TimeUnit.MINUTES).build(), true));
        caches.add(new CaffeineCache(CacheConstants.PROJECT_CACHE,
                Caffeine.newBuilder().recordStats().expireAfterWrite(2, TimeUnit.MINUTES).maximumSize(100).build(),
                true));
        caches.add(new CaffeineCache(CacheConstants.TEST_RUNS_INFO_CACHE,
                Caffeine.newBuilder().recordStats().expireAfterWrite(2, TimeUnit.MINUTES).maximumSize(100).build(),
                true));
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(caches);
        return cacheManager;
    }
}
