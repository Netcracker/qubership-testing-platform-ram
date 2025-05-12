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

package org.qubership.atp.ram.dto.filters;

import java.util.Set;
import java.util.UUID;

import org.springframework.data.mongodb.core.query.Criteria;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class ReferenceFilter extends AbstractFilter {

    public static final String TYPE = "ReferenceFilter";

    private Set<UUID> values;

    @Override
    public Criteria buildFilterCriteriaForField(String field) {
        return new Criteria().andOperator(
                Criteria.where(field).in(values)
        );
    }

}
