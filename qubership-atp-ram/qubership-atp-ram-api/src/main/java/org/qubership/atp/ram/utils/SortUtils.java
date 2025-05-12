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

import org.springframework.data.domain.Sort;

public class SortUtils {

    /**
     * Parse sort direction from string. Return descending order if can't parse.
     *
     * @param sortDirection sort direction
     * @return sort direction
     */
    public static Sort.Direction parseSortDirection(String sortDirection) {
        return !Sort.Direction.ASC.name().equals(sortDirection) && !Sort.Direction.DESC.name().equals(sortDirection)
                ? Sort.Direction.DESC
                : Sort.Direction.fromString(sortDirection);
    }

}
