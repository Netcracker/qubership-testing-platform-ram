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

import java.util.regex.Pattern;

import org.qubership.atp.ram.exceptions.executionrequests.RamExecutionRequestUnexpectedFilterTypeException;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.util.StringUtils;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class TextFilter extends AbstractFilter {
    public static final String TYPE = "TextFilter";

    private TextFilterType filterType;
    private boolean caseSensitive;
    private String value;

    @Override
    public Criteria buildFilterCriteriaForField(String field) {
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        Pattern regex = getPattern();
        return getCriteria(field, regex);
    }

    private Criteria getCriteria(String field, Pattern regex) {
        Criteria criteria;
        if (caseSensitive) {
            criteria = Criteria.where(field).regex(regex);
        } else {
            criteria = Criteria.where(field).regex(regex.pattern(), "i");
        }
        return criteria;
    }

    private Pattern getPattern() {
        Pattern regex;
        switch (filterType) {
            case CONTAINS:
                regex = Pattern.compile(Pattern.quote(value));
                break;
            case STARTS_WITH:
                regex = Pattern.compile("^" + Pattern.quote(value));
                break;
            default:
                log.error("Unexpected execution request filter type: {}", filterType);
                throw new RamExecutionRequestUnexpectedFilterTypeException(filterType.name());
        }
        return regex;
    }

    public enum TextFilterType {
        CONTAINS,
        STARTS_WITH
    }
}


