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

package org.qubership.atp.ram.utils;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections.CollectionUtils.isEmpty;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.modelmapper.ModelMapper;
import org.qubership.atp.ram.enums.TestScopeSections;
import org.qubership.atp.ram.models.RamObject;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StreamUtils {

    private static final ModelMapper modelMapper = new ModelMapper();

    private StreamUtils() {
    }

    /**
     * Extract id's from any type entities.
     *
     * @param entities  input entities
     * @param extractor id extractor
     * @param <T>       processed entities type
     * @return result set
     */
    public static <T> Set<UUID> extractIds(Collection<T> entities, Function<T, UUID> extractor) {
        if (entities == null) {
            return Collections.emptySet();
        }

        return getIdsStream(entities, extractor)
                .collect(toSet());
    }

    /**
     * Extract id's set from any type entities.
     *
     * @param entities input entities
     * @param <T>      processed entities type
     * @return result set
     */
    public static <T extends RamObject> Set<UUID> extractIds(Collection<T> entities) {
        return extractIds(entities, RamObject::getUuid);
    }

    /**
     * Extract flat id's from any type entities.
     *
     * @param entities  input entities
     * @param extractor id extractor
     * @param <T>       processed entities type
     * @return result set
     */
    public static <T> Set<UUID> extractFlatIds(Collection<T> entities, Function<T, Collection<UUID>> extractor) {
        if (entities == null) {
            return Collections.emptySet();
        }

        return getFlatIdsStream(entities, extractor)
                .collect(toSet());
    }

    /**
     * Extract flat entities from any type entities.
     *
     * @param entities  input entities
     * @param extractor flat entity extractor
     * @param <T>       processed entities type
     * @return result list
     */
    public static <T, E> List<E> extractFlatEntities(Collection<T> entities, Function<T, Collection<E>> extractor) {
        if (entities == null) {
            return Collections.emptyList();
        }

        return getFlatStream(entities, extractor)
                .collect(toList());
    }

    /**
     * Extract id's set from any type entities.
     *
     * @param entities input entities
     * @param <T>      processed entities type
     * @return result set
     */
    public static <T extends RamObject> List<String> extractNames(Collection<T> entities) {
        return entities == null
                ? Collections.emptyList()
                : entities.stream().map(RamObject::getName).collect(toList());
    }


    /**
     * Get id's stream from any type entities.
     *
     * @param entities  input entities
     * @param extractor id extractor
     * @param <T>       processed entities type
     * @return result stream
     */
    private static <T> Stream<UUID> getIdsStream(Collection<T> entities, Function<T, UUID> extractor) {
        return entities.stream()
                .map(extractor)
                .filter(Objects::nonNull);
    }

    private static <T> Stream<UUID> getFlatIdsStream(Collection<T> entities, Function<T, Collection<UUID>> extractor) {
        return entities.stream()
                .filter(elem -> !isEmpty(extractor.apply(elem)))
                .flatMap(elem -> extractor.apply(elem).stream());
    }

    private static <T, E> Stream<E> getFlatStream(Collection<T> entities, Function<T, Collection<E>> extractor) {
        return entities.stream()
                .filter(elem -> !isEmpty(extractor.apply(elem)))
                .flatMap(elem -> extractor.apply(elem).stream());
    }

    /**
     * Extract id's list from any type entities.
     *
     * @param entities input entities
     * @param <T>      processed entities type
     * @return result list
     */
    public static <T extends RamObject> List<UUID> extractIdsToList(Collection<T> entities) {
        return getIdsStream(entities, RamObject::getUuid)
                .collect(toList());
    }

    public static <T> Stream<T> stream(Iterable<T> entities) {
        return StreamSupport.stream(entities.spliterator(), false);
    }

    public static <T> Map<UUID, T> toKeyEntityMap(Iterable<T> entities, Function<T, UUID> keyExtractor) {
        return stream(entities)
                .collect(Collectors.toMap(keyExtractor, identity()));
    }

    public static <T> Map<UUID, T> toIdEntityMap(Iterable<T> entities, Function<T, UUID> keyExtractor) {
        return stream(entities)
                .collect(Collectors.toMap(keyExtractor, identity()));
    }

    public static <T extends RamObject> Map<UUID, T> toIdEntityMap(Iterable<T> entities) {
        return stream(entities)
                .collect(Collectors.toMap(RamObject::getUuid, identity()));
    }

    public static <T, R> Map<R, T> toEntityMap(Iterable<T> entities, Function<T, R> keyExtractor) {
        return stream(entities)
                .collect(Collectors.toMap(keyExtractor, identity()));
    }

    public static <T extends RamObject> Map<UUID, String> toIdNameEntityMap(Iterable<T> entities) {
        return stream(entities)
                .collect(Collectors.toMap(RamObject::getUuid, RamObject::getName));
    }

    public static <T> Map<UUID, List<T>> toMapWithListEntitiesValues(Iterable<T> entities,
                                                                     Function<T, UUID> keyExtractor) {
        return stream(entities)
                .collect(Collectors.groupingBy(keyExtractor));
    }

    public static <T> Map<UUID, List<T>> toEntityListMap(Iterable<T> entities,
                                                         Function<T, UUID> keyExtractor) {
        return stream(entities)
                .collect(Collectors.groupingBy(keyExtractor));
    }

    /**
     * Map entities from list to another type.
     *
     * @param entities entities list
     * @param clazz    convert class
     * @param <T>      entities type
     * @param <R>      convert type
     * @return result list
     */
    public static <T, R> List<R> mapToClazz(Iterable<T> entities, Class<R> clazz) {
        return stream(entities)
                .map(entity -> modelMapper.map(entity, clazz))
                .collect(toList());
    }

    /**
     * Map entity to another type.
     *
     * @param entity entity
     * @param clazz  convert class
     * @param <T>    entities type
     * @param <R>    convert type
     * @return result list
     */
    public static <T, R> R mapToClazz(T entity, Class<R> clazz) {
        return modelMapper.map(entity, clazz);
    }

    /**
     * Map entities using provided map function.
     *
     * @param entities entities
     * @param mapFunc  map function
     * @param <T>    input entites type
     * @param <R>    output entites type
     * @return result list
     */
    public static <T, R> List<R> map(Collection<T> entities, Function<T, R> mapFunc) {
        return entities.stream()
                .map(mapFunc)
                .collect(toList());
    }

    /**
     * Extract specified field from entities list.
     *
     * @param entities  entities list
     * @param extractor extract function
     * @param <T>       entities type
     * @param <R>       convert type
     * @return result set
     */
    public static <T, R> Set<R> extractFields(Collection<T> entities, Function<T, R> extractor) {
        if (entities == null) {
            return Collections.emptySet();
        }

        return entities.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .collect(toSet());
    }

    /**
     * Filter list with specified predicate.
     *
     * @param entities    input entities list
     * @param predicate   list predicate
     * @param <T>         entity type
     * @return result list
     */
    public static <T> List<T> filterList(Collection<T> entities, Predicate<T> predicate) {
        return entities.stream()
                .filter(predicate)
                .collect(toList());
    }

    /**
     * Filter list with specified predicate.
     *
     * @param entities    input entities list
     * @param predicate   list predicate
     * @param sort        list sort condition
     * @param <T>         entity type
     * @return result list
     */
    public static <T extends RamObject> List<T> filterList(Collection<T> entities, Predicate<T> predicate,
                                                           Comparator<T> sort) {
        return entities.stream()
                .filter(predicate)
                .sorted(sort)
                .collect(toList());
    }

    /**
     * Filter list with specified keys.
     *
     * @param entities    input entities list
     * @param containKeys filter entities keys
     * @param <T>         entity type
     * @return result list
     */
    public static <T extends RamObject> List<T> filterList(Collection<T> entities,
                                                           Collection<UUID> containKeys) {
        return entities.stream()
                .filter(entity -> containKeys.contains(entity.getUuid()))
                .collect(toList());
    }

    /**
     * Filter list with specified keys.
     *
     * @param entities    input entities list
     * @param containKeys filter entities keys
     * @param <T>         entity type
     * @return result list
     */
    public static <T> List<T> filterList(Collection<T> entities,
                                         Function<T, UUID> entityKeyExtractFunc,
                                         Collection<UUID> containKeys) {
        return entities.stream()
                .filter(entity -> containKeys.contains(entityKeyExtractFunc.apply(entity)))
                .collect(toList());
    }

    /**
     * Find first entity in list with specified predicate.
     *
     * @param entities  input entities list
     * @param predicate list predicate
     * @param <T>       entity type
     * @return result entity
     */
    public static <T> T findFirstInList(Collection<T> entities, Predicate<T> predicate) {
        return entities.stream()
                .filter(predicate)
                .findFirst()
                .get();
    }

    /**
     * Filter list with specified keys.
     *
     * @param entities    input entities list
     * @param testScopeSection test scope section
     * @param <T>         entity type
     * @return result list
     */
    public static <T> List<T> filterByTestScopeSection(Collection<T> entities,
                                                       Function<T, TestScopeSections> entityKeyExtractFunc,
                                                       TestScopeSections testScopeSection) {
        return entities.stream()
                .filter(entity -> testScopeSection.equals(entityKeyExtractFunc.apply(entity)))
                .collect(toList());
    }

    public static <T> boolean isAllListIdsPresent(Collection<T> entities,
                                                  Collection<UUID> ids,
                                                  Function<T, UUID> entityKeyExtractFunc) {
        return extractIds(entities, entityKeyExtractFunc).containsAll(ids);
    }

    /**
     * Get list of entities from corresponding map.
     *
     * @param ids         entities identifiers
     * @param entitiesMap entities map
     * @return list of entities
     */
    public static <T> List<T> getEntitiesFromMap(Set<UUID> ids, Map<UUID, T> entitiesMap) {
        return ids.stream()
                .map(entitiesMap::get)
                .filter(Objects::nonNull)
                .collect(toList());
    }

    /**
     * Check entities collection and return the first one.
     *
     * @param entities entities
     * @param <T> entity type
     * @return first entity from the collection
     */
    public static <T> T checkAndReturnSingular(Collection<T> entities) {
        if (isEmpty(entities)) {
            throw new IllegalStateException("Provided entities collection is nullable");
        }
        if (entities.size() != 1) {
            throw new IllegalStateException("Expect one entity in the provided collection");
        }

        return entities.iterator().next();
    }

    /**
     * Uppercase string collection.
     *
     * @param input input collection
     * @return collection with uppercase strings
     */
    public static Collection<String> toUpperCase(Collection<String> input) {
        return input.stream()
                .map(String::toUpperCase)
                .collect(toList());
    }
}
