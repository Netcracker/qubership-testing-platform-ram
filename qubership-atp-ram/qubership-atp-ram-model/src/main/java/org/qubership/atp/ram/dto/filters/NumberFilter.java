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

import static java.util.Objects.isNull;

import org.qubership.atp.auth.springbootstarter.utils.ExceptionUtils;
import org.qubership.atp.ram.dto.filters.types.FilterType;
import org.qubership.atp.ram.dto.filters.types.SingleValueFilterType;
import org.qubership.atp.ram.exceptions.executionrequests.RamExecutionRequestIllegalFilterParameterException;
import org.qubership.atp.ram.exceptions.executionrequests.RamExecutionRequestUnexpectedFilterTypeException;
import org.springframework.data.mongodb.core.query.Criteria;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class NumberFilter extends AbstractFilter {

    public static final String TYPE = "NumberFilter";

    private FilterType filterType;
    private SingleValueFilterType valueFilterType;
    private Integer from;
    private Integer to;

    @Override
    public Criteria buildFilterCriteriaForField(String field) {
        if (isNull(from) && isNull(to)) {
            ExceptionUtils.throwWithLog(log, new RamExecutionRequestIllegalFilterParameterException());
        }
        switch (filterType) {
            case VALUE:
                switch (valueFilterType) {
                    case FROM:
                        return FilterUtils.getCriteriaLessOrMoreThanValue(field, valueFilterType, from);
                    case TO:
                        return FilterUtils.getCriteriaLessOrMoreThanValue(field, valueFilterType, to);
                    default:
                        log.error("Unexpected execution request filter type: {}", valueFilterType);
                        throw new RamExecutionRequestUnexpectedFilterTypeException(valueFilterType.name());
                }
            case RANGE:
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