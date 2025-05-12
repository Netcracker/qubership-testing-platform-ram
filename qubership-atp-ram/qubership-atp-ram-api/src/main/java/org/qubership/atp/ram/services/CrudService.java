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

import static org.qubership.atp.auth.springbootstarter.utils.ReflectionUtils.getGenericClassSimpleName;

import java.util.List;
import java.util.UUID;

import org.qubership.atp.auth.springbootstarter.exceptions.AtpEntityNotFoundException;
import org.qubership.atp.ram.models.RamObject;
import org.springframework.data.mongodb.repository.MongoRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class CrudService<T extends RamObject> {

    protected abstract MongoRepository<T, UUID> repository();

    /**
     * Get entity by id.
     *
     * @param id entity id
     * @return founded entity
     */
    public T get(UUID id) {
        return repository().findById(id).orElseThrow(() -> {
            String entityName = getGenericClassSimpleName(this);
            log.error("Failed to found {} entity with id: {}", entityName, id);
            return new AtpEntityNotFoundException(entityName, id);
        });
    }

    public T save(T object) {
        return repository().save(object);
    }

    public List<T> getAll() {
        return repository().findAll();
    }

    public List<T> saveAll(List<T> objects) {
        return repository().saveAll(objects);
    }
}
