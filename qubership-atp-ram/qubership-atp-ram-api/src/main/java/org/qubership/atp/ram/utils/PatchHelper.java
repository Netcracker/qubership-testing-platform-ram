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

import static java.util.Arrays.asList;

import java.beans.FeatureDescriptor;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * Util class containing useful for pathing methods.
 */
@Slf4j
@Component
public class PatchHelper {

    public static final FieldFilter nullProperties = PatchHelper::getNullPropertyNames;
    public static final FieldFilter emptyCollectionsProperties = PatchHelper::getEmptyCollectionPropertiesNames;
    public static final FieldFilter emptyMapsPropertiesFilter = PatchHelper::getEmptyMapPropertiesNames;

    /**
     * Copies all fields from source to target, except those filtered by field filters.
     *
     * @return target object with patched values.
     */
    public Object partialUpdate(Object source, Object target, FieldFilter... fieldFilters) {
        log.debug("Patching object {} with {} using filters {}", target, source, asList(fieldFilters));
        BeanWrapper wrappedSource = new BeanWrapperImpl(source);
        String[] ignoredProperties = Arrays.stream(fieldFilters)
                .parallel()
                .map(fieldFilter -> fieldFilter.filterFieldsToIgnore(wrappedSource))
                .reduce(Stream::concat)
                .orElseGet(Stream::empty)
                .toArray(String[]::new);
        log.trace("Ignore properties: {}", asList(ignoredProperties));
        BeanUtils.copyProperties(source, target, ignoredProperties);
        return target;
    }

    public FieldFilter getConcretePropertiesFilter(List<String> properties) {
        return wrappedSource -> getConcretePropertyNames(wrappedSource, properties);
    }

    private static Stream<String> getNullPropertyNames(BeanWrapper wrappedSource) {
        return Stream.of(wrappedSource.getPropertyDescriptors())
                .map(FeatureDescriptor::getName)
                .filter(propertyName -> wrappedSource.getPropertyValue(propertyName) == null);
    }

    private static Stream<String> getEmptyCollectionPropertiesNames(BeanWrapper wrappedSource) {
        return Stream.of(wrappedSource.getPropertyDescriptors())
                .parallel()
                .filter(propertyDescriptor -> Collection.class.isAssignableFrom(propertyDescriptor.getPropertyType()))
                .filter(propertyDescriptor -> CollectionUtils.isEmpty(
                        (Collection) wrappedSource.getPropertyValue(propertyDescriptor.getName())))
                .map(FeatureDescriptor::getName);
    }

    private static Stream<String> getEmptyMapPropertiesNames(BeanWrapper wrappedSource) {
        return Stream.of(wrappedSource.getPropertyDescriptors())
                .parallel()
                .filter(propertyDescriptor -> Map.class.isAssignableFrom(propertyDescriptor.getPropertyType()))
                .filter(propertyDescriptor -> CollectionUtils.isEmpty(
                        (Map) wrappedSource.getPropertyValue(propertyDescriptor.getName())))
                .map(FeatureDescriptor::getName);
    }

    private static Stream<String> getConcretePropertyNames(BeanWrapper wrappedSource, List<String> properties) {
        return Stream.of(wrappedSource.getPropertyDescriptors())
                .map(FeatureDescriptor::getName)
                .filter(properties::contains);
    }

    @FunctionalInterface
    public interface FieldFilter {

        Stream<String> filterFieldsToIgnore(BeanWrapper beanWrapper);
    }
}
