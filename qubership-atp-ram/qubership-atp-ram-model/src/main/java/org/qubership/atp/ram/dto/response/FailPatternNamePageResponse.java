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

package org.qubership.atp.ram.dto.response;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.qubership.atp.ram.models.FailPattern;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class FailPatternNamePageResponse {

    private long totalCount;
    private List<RamObjectResponse> failPatterns;

    /**
     * Constructor for {@link FailPatternNamePageResponse}.
     * @param failPatterns List of Fail Patterns.
     * @param totalCount   Number of Fail Patterns.
     */
    public FailPatternNamePageResponse(Collection<FailPattern> failPatterns, long totalCount) {
        this.totalCount = totalCount;
        this.failPatterns = failPatterns.stream()
                .map(failPattern -> new RamObjectResponse(failPattern.getUuid(), failPattern.getName()))
                .collect(Collectors.toList());
    }
}
