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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import net.htmlparser.jericho.Segment;
import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.TextExtractor;

public class Utils {

    private static final SimpleFilterProvider filterProvider = new SimpleFilterProvider();

    /**
     * Util method to crete json representation of object using field filter.
     *
     * @param fields   - array of field names that should be included in result. If null, all fields are returned.
     * @param mapper   - {@link ObjectMapper} that should be used for serialization.
     * @param filterId - {@link com.fasterxml.jackson.databind.ser.PropertyFilter} id string.
     *                 See {@link com.fasterxml.jackson.annotation.JsonFilter}
     */
    public static String filterAllExceptFields(Object objectToConvert, String[] fields, ObjectMapper mapper,
                                               String filterId) throws JsonProcessingException {
        filterProvider.addFilter(filterId, fields != null
                ? SimpleBeanPropertyFilter.filterOutAllExcept(fields) :
                SimpleBeanPropertyFilter.serializeAll());
        return mapper.writer(filterProvider).withDefaultPrettyPrinter().writeValueAsString(objectToConvert);
    }

    /**
     * Remove tags from xml source.
     *
     * @param htmlString xml source string.
     * @return formatted string without tags.
     */
    public static String cleanXmlTags(String htmlString) {
        if (Objects.nonNull(htmlString)) {
            Source source = new Source(htmlString);
            Segment segment = new Segment(source, 0, source.length());
            return new TextExtractor(segment).toString();
        }
        return null;
    }

    /**
     * Get all fields in class how list.
     */
    public static List<Field> getAllDeclaredFields(Class clazz) {
        if (clazz == null) {
            return Collections.emptyList();
        } else {
            List<Field> result = new ArrayList(getAllDeclaredFields(clazz.getSuperclass()));
            result.addAll((Collection) Arrays.stream(clazz.getDeclaredFields()).collect(Collectors.toList()));
            return result;
        }
    }
}
