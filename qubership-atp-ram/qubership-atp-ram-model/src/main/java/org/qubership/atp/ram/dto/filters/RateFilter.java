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

import org.qubership.atp.ram.dto.filters.types.FilterType;
import org.qubership.atp.ram.dto.filters.types.SingleValueFilterType;
import org.qubership.atp.ram.exceptions.executionrequests.RamExecutionRequestUnexpectedFilterTypeException;
import org.springframework.data.mongodb.core.query.Criteria;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class RateFilter extends AbstractFilter {

    public static final String TYPE = "RateFilter";

    private FilterType filterType;
    private Integer value;
    private Integer to;
    private Integer from;
    private SingleValueFilterType valueFilterType;

    @Override
    public Criteria buildFilterCriteriaForField(String field) {
        switch (filterType) {
            case VALUE:
                if (valueFilterType == null) {
                    return Criteria.where(field).is(value);
                }
                return FilterUtils.getCriteriaLessOrMoreThanValue(field, valueFilterType, value);
            case RANGE:
                if (from == null || to == null) {
                    return null;
                }
                return new Criteria().andOperator(
                        Criteria.where(field).lte(to),
                        Criteria.where(field).gte(from)
                );
            default:
                log.error("Unexpected execution request filter type: {}", filterType);
                throw new RamExecutionRequestUnexpectedFilterTypeException(filterType.name());
        }
    }
}