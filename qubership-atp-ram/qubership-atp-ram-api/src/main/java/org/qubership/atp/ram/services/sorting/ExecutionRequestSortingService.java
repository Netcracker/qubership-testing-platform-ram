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

package org.qubership.atp.ram.services.sorting;

import java.util.List;
import java.util.stream.Collectors;

import org.qubership.atp.ram.dto.request.SortingParams;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class ExecutionRequestSortingService {

    /** Build sort by sorting params.
     * @param sortingParamsList list of sortingParams
     * @return sort
     */
    public Sort buildSort(List<SortingParams> sortingParamsList) {
        if (sortingParamsList != null) {
            List<Sort.Order> orders = sortingParamsList.stream()
                    .map(this::getSortingOrder)
                    .collect(Collectors.toList());
            return Sort.by(orders);
        } else {
            return Sort.unsorted();
        }
    }

    private Sort.Order getSortingOrder(SortingParams sortingParams) {
        final SortingParams.Direction sortType = sortingParams.getSortType();
        final String columnType = sortingParams.getColumn();

        switch (sortType) {
            case ASC:
                return Sort.Order.asc(columnType);
            case DESC:
            default:
                return Sort.Order.desc(columnType);
        }
    }
}
