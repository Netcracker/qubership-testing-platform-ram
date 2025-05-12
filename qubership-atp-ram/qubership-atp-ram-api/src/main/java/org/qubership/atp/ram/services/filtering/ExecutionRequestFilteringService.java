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

package org.qubership.atp.ram.services.filtering;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.qubership.atp.ram.dto.filters.AbstractFilter;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.CriteriaDefinition;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class ExecutionRequestFilteringService {

    /**
     * Build searchCriteria to find necessary Issues by fields.
     *
     * @return search criteria
     */
    public CriteriaDefinition buildSearchCriteria(UUID testPlanId, List<AbstractFilter> filterList) {
        List<Criteria> criteriaList = new ArrayList<>();
        criteriaList.add(Criteria.where(ExecutionRequest.TEST_PLAN_ID_FIELD).is(testPlanId));
        if (!CollectionUtils.isEmpty(filterList)) {
            for (AbstractFilter filter : filterList) {
                Criteria criteria = filter.buildFilterCriteriaForField(filter.getColumn());
                if (criteria != null) {
                    criteriaList.add(criteria);
                }
            }
        }
        return new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));
    }
}
