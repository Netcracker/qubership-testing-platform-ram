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

package org.qubership.atp.ram.service.history.impl;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

import org.javers.core.Javers;
import org.javers.core.metamodel.annotation.DiffInclude;
import org.javers.repository.jql.JqlQuery;
import org.javers.repository.jql.QueryBuilder;
import org.javers.shadow.Shadow;
import org.qubership.atp.ram.exceptions.history.RamHistoryRevisionRestoreException;
import org.qubership.atp.ram.models.DateAuditorEntity;
import org.qubership.atp.ram.service.history.RestoreHistoryService;
import org.qubership.atp.ram.services.CrudService;
import org.qubership.atp.ram.utils.Utils;
import org.springframework.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractRestoreHistoryService<E extends DateAuditorEntity> implements RestoreHistoryService {

    protected final Javers javers;
    protected final CrudService<E> entityService;
    protected final ValidateReferenceExistsService<E> validateReferenceExistsService;

    public static final Predicate<Field> IS_DIFFINLCUDE_ANNOTATED_PROPERTY_FILTER =
            field -> field.isAnnotationPresent(DiffInclude.class);

    public static final Predicate<Field> NOT_CHILDACTIONS_PROPERTY_FILTER =
            field -> !CHILD_ACTIONS_PROPERTY.equals(field.getName());


    /**
     * Restore service constructor.
     */
    public AbstractRestoreHistoryService(Javers javers,
                                         CrudService<E> entityService,
                                         ValidateReferenceExistsService<E> validateReferenceExistsService) {
        this.javers = javers;
        this.entityService = entityService;
        this.validateReferenceExistsService = validateReferenceExistsService;
    }

    /**
     * Restores the object to a state defined by revision number.
     *
     * @param id         of object being restored.
     * @param revisionId revision number to restore.
     */
    @Override
    public void restoreToRevision(UUID id, long revisionId) {

        JqlQuery query = QueryBuilder.byInstanceId(id, getEntityClass())
                .withVersion(revisionId)
                .build();

        E actualObject = getObject(id);

        validateReferenceExistsService.validateEntity(actualObject);

        List<Shadow<Object>> shadows = javers.findShadows(query);

        if (CollectionUtils.isEmpty(shadows)) {
            log.error("No shadows found for entity '{}' with revision='{}' and uuid='{}'",
                    getItemType(), revisionId, id);

            throw new RamHistoryRevisionRestoreException();
        }
        Shadow<Object> objectShadow = shadows.iterator().next();

        Object restoredObject = restoreValues(objectShadow, actualObject);
        saveRestoredObject((E) restoredObject);
    }

    protected Object restoreValues(Shadow<Object> shadow, DateAuditorEntity actualObject) {

        Object snapshot = shadow.get();

        Class<? extends DateAuditorEntity> actualObjectClass = actualObject.getClass();
        Class<?> snapshotClass = snapshot.getClass();
        if (!actualObjectClass.equals(snapshotClass)) {
            log.error("History revision was not restored. Object and snapshot classes are not equal: {}, {}",
                    actualObjectClass, snapshotClass);
            throw new RamHistoryRevisionRestoreException();
        }

        List<Field> declaredFields = Utils.getAllDeclaredFields(snapshotClass);

        declaredFields
                .stream()
                .filter(getPredicates().stream().reduce(x -> true, Predicate::and))
                .forEach(field -> copyValue(actualObject, snapshot, field));

        return actualObject;
    }

    protected List<Predicate<Field>> getPredicates() {
        return Arrays.asList(IS_DIFFINLCUDE_ANNOTATED_PROPERTY_FILTER, NOT_CHILDACTIONS_PROPERTY_FILTER);
    }

    private void copyValue(DateAuditorEntity targetObject, Object sourceObject, Field field) {

        try {
            field.setAccessible(true);
            Object newValue = field.get(sourceObject);
            field.set(targetObject, newValue);
        } catch (IllegalAccessException e) {
            log.error("Error occurred while copying parameter '{}' for entity '{}'", field.getName(), sourceObject, e);
            throw new RamHistoryRevisionRestoreException();
        }
    }

    public E getObject(UUID id) {
        return entityService.get(id);
    }

    public void saveRestoredObject(E object) {
        entityService.save(object);
    }
}
