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

package org.qubership.atp.ram.services.dictionary;

import static java.util.Arrays.stream;
import static org.qubership.atp.ram.constants.CacheConstants.ATP_RAM_DICTIONARIES;

import java.util.List;

import org.qubership.atp.ram.models.dictionary.Dictionary;
import org.qubership.atp.ram.repositories.DictionaryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DictionaryService {

    private static DictionaryRepository repository;

    @Autowired
    public void setRepository(DictionaryRepository repository) {
        DictionaryService.repository = repository;
    }

    /**
     * Get dictionary list by specified name.
     *
     * @param name input dictionary name
     * @return list of founded dictionaries
     */

    @Cacheable(value = ATP_RAM_DICTIONARIES, key = "{#name}")
    public List<Dictionary> getAllByName(String name) {
        return stream(DictionaryCatalogEnum.values())
                .filter(dictionaryCatalogEnum -> dictionaryCatalogEnum.name().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Can't found dictionary by specified name: " + name))
                .getSupplier()
                .get();
    }

    @CacheEvict(value = ATP_RAM_DICTIONARIES, key = "{#name}")
    public void evictDictionaryCacheByName(String name) {
        log.info("Dictionary cache for name '{}' has been evicted", name);
    }

    public static List<Dictionary> getAllByType(String type) {
        return repository.getAllByType(type);
    }
}
