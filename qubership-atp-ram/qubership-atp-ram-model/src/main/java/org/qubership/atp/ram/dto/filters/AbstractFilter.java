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

import org.springframework.data.mongodb.core.query.Criteria;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TextFilter.class, name = TextFilter.TYPE),
        @JsonSubTypes.Type(value = StatusFilter.class, name = StatusFilter.TYPE),
        @JsonSubTypes.Type(value = DateFilter.class, name = DateFilter.TYPE),
        @JsonSubTypes.Type(value = RateFilter.class, name = RateFilter.TYPE),
        @JsonSubTypes.Type(value = BooleanFilter.class, name = BooleanFilter.TYPE),
        @JsonSubTypes.Type(value = NumberFilter.class, name = NumberFilter.TYPE),
        @JsonSubTypes.Type(value = ReferenceFilter.class, name = ReferenceFilter.TYPE)
})
@Data
public abstract class AbstractFilter {

    private String column;
    private String type;

    public abstract Criteria buildFilterCriteriaForField(String field);

}
