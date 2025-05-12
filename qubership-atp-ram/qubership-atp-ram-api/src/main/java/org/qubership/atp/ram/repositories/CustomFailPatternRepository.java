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

package org.qubership.atp.ram.repositories;

import org.qubership.atp.ram.dto.response.PaginationResponse;
import org.qubership.atp.ram.models.FailPattern;
import org.qubership.atp.ram.models.FailPatternSearchRequest;
import org.qubership.atp.ram.models.PaginationSearchRequest;
import org.springframework.data.domain.Pageable;

public interface CustomFailPatternRepository {
    PaginationResponse<FailPattern> findAllFailPatterns(FailPatternSearchRequest request, Pageable pageable);

    PaginationResponse getAllIssuesWithPagination(PaginationSearchRequest request);

    PaginationResponse getAllFailReasonsWithPagination(PaginationSearchRequest request);
}
