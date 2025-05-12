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

package org.qubership.atp.ram.services;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.qubership.atp.ram.enums.EngineCategory;
import org.qubership.atp.ram.models.ToolConfigInfo;
import org.qubership.atp.ram.repositories.ToolConfigInfoRepository;
import org.qubership.atp.ram.utils.JsonHelper;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ToolConfigInfoService extends CrudService<ToolConfigInfo> {
    private static final String CONFIG_INFO_ID_KEY = "configInfoId";

    private final ToolConfigInfoRepository repository;

    @Override
    protected MongoRepository<ToolConfigInfo, UUID> repository() {
        return repository;
    }

    public ToolConfigInfo getByUuid(UUID uuid) {
        return repository.findByUuid(uuid);
    }

    public List<ToolConfigInfo> getAll() {
        return repository.findAll();
    }

    public ToolConfigInfo save(ToolConfigInfo configFiles) {
        return repository.save(configFiles);
    }

    /**
     * Save configuration files.
     *
     * @param request save request
     * @return File Id set.
     */
    public Set<UUID> saveConfigs(JsonObject request) {
        Set<String> fileIds = new HashSet<>();
        if (request.has(CONFIG_INFO_ID_KEY)) {
            final JsonArray arrayFiles = request.getAsJsonArray(CONFIG_INFO_ID_KEY);

            Set<UUID> files = new HashSet<>();
            for (int i = 0; i < arrayFiles.size(); i++) {
                JsonObject configFile = arrayFiles.get(i).getAsJsonObject();

                ToolConfigInfo file = new ToolConfigInfo();
                file.setName(JsonHelper.getStringValue(configFile, "name"));
                file.setData(JsonHelper.getStringValue(configFile, "data"));
                String categoryString = JsonHelper.getStringValue(configFile, "category");
                EngineCategory category = EngineCategory.fromString(categoryString);
                if (category == null) {
                    log.error("Engine Category {} not found.", categoryString);
                } else {
                    file.setCategory(category);
                }
                files.add(save(file).getUuid());
            }
            log.debug("Config files saved successfully. Config files: {}", fileIds);

            return files;
        } else {
            log.error("The request must contain the parameter: \"configFiles\". "
                    + "Request: {}", request);

            return Collections.emptySet();
        }
    }
}
