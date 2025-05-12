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

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.qubership.atp.ram.dto.filters.types.FilterType;
import org.qubership.atp.ram.dto.filters.types.SingleValueFilterType;
import org.qubership.atp.ram.exceptions.executionrequests.RamExecutionRequestUnexpectedFilterTypeException;
import org.springframework.data.mongodb.core.query.Criteria;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class DateFilter extends AbstractFilter {

    public static final String TYPE = "DateFilter";

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static final long DAY_MS = 86399999;

    private FilterType filterType;
    private String value;
    private String to;
    private String from;
    private SingleValueFilterType valueFilterType;

    @Override
    public Criteria buildFilterCriteriaForField(String field) {
        if (valueFilterType == SingleValueFilterType.EQUAL) {
            filterType = FilterType.RANGE;
        }
        switch (filterType) {
            case VALUE:
                    Timestamp timestamp = getTimeStampFromString(value);
                    if (timestamp == null) {
                        return null;
                    }
                    if (valueFilterType == SingleValueFilterType.TO) {
                        timestamp.setTime(timestamp.getTime() + DAY_MS);
                    }
                    return FilterUtils.getCriteriaLessOrMoreThanValue(field, valueFilterType, timestamp);

            case RANGE:
                Timestamp from = getTimeStampFromString(this.from);
                Timestamp to = getTimeStampFromString(this.to);
                if (from == null || to == null) {
                    return null;
                }
                to.setTime(to.getTime() + DAY_MS);
                return new Criteria().andOperator(
                        Criteria.where(field).lte(to),
                        Criteria.where(field).gte(from)
                );
            default:
                log.error("Unexpected execution request filter type: {}", filterType);
                throw new RamExecutionRequestUnexpectedFilterTypeException(filterType.name());
        }
    }

    private Timestamp getTimeStampFromString(String date) {
        try {
            return new Timestamp(DATE_FORMAT.parse(date).getTime());
        } catch (ParseException e) {
            log.error("DateFilter:" + e.getLocalizedMessage());
            return null;
        }
    }
}
