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

package org.qubership.atp.ram.repositories.impl;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.util.Collection;
import java.util.UUID;

import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class GenericEntitySearchComponent implements FieldConstants {

    private static final String REGEX_SYMBOLS = "([$.*+?~^!|\\-()\\[\\]{}\\\\])";

    /**
     * Create criteria for search entity by names contains and ignore case.
     *
     * @param names name for search
     * @return Criteria
     */
    public Criteria namesRegexIgnoreCase(String fieldName, Collection<String> names) {
        Criteria nameCriteria = new Criteria();
        nameCriteria.orOperator(names.stream()
                .map(name -> where(fieldName)
                        .regex(".*" + name.replaceAll(REGEX_SYMBOLS, "\\\\$1") + ".*", "i"))
                .toArray(Criteria[]::new));
        return nameCriteria;
    }

    public Criteria namesRegexIgnoreCase(String fieldName, String name) {
        return where(fieldName).regex(".*" + name.replaceAll(REGEX_SYMBOLS, "\\\\$1") + ".*", "i");
    }

    public Criteria foundInArrayStrings(String fieldName, Collection<String> fieldValues) {
        return where(fieldName).in(fieldValues);
    }

    public Criteria foundInArrayIds(String fieldName, Collection<UUID> ids) {
        return where(fieldName).in(ids);
    }
}
