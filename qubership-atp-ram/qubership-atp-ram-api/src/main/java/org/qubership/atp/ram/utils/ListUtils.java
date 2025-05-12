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

import static java.util.Objects.nonNull;

import java.util.Collections;
import java.util.List;

public class ListUtils {

    /**
     * Applies pagination to list of data with type T.
     *
     * @param data list of data
     * @param page number of page for pagination
     * @param size count of items at 1 page
     * @param <T>  type of data
     * @return Paginated list of data
     */
    public static <T> List<T> applyPagination(List<T> data, Integer page, Integer size) {
        return applyPagination(data, page, size, null);
    }

    /**
     * Applies pagination to list of data with type T.
     *
     * @param data list of data
     * @param page number of page for pagination
     * @param size count of items at 1 page
     * @param <T>  type of data
     * @return Paginated list of data
     */
    public static <T> List<T> applyPagination(List<T> data, Integer page, Integer size, Integer shift) {
        if (nonNull(shift)) {
            data = data.subList(shift, data.size());
        }
        int startIndex = page * size;
        int endIndex = startIndex + size;
        return applyPaginationByIndexes(data, startIndex, endIndex);
    }

    /**
     * Applies pagination to list of data with type T.
     *
     * @param data       list of data
     * @param startIndex start index
     * @param endIndex   end index
     * @param <T>        type of data
     * @return Paginated list of data
     */
    public static <T> List<T> applyPaginationByIndexes(List<T> data, Integer startIndex, Integer endIndex) {
        if (startIndex > data.size()) {
            return Collections.emptyList();
        } else {
            return data.subList(startIndex, Math.min(data.size(), endIndex));
        }
    }

}
