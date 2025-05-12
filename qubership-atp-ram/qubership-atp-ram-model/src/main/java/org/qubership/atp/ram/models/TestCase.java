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

package org.qubership.atp.ram.models;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
@JsonIgnoreProperties(ignoreUnknown = true)
public class TestCase extends RamObject {

    private List<TestCaseOrder> order;
    private UUID testScenarioUuid;
    private String jiraTicket;

    /**
     * Find test case order in specified scope.
     *
     * @param testScopeId scope identifier
     * @return scope order
     */
    public TestCaseOrder findByScope(UUID testScopeId) {
        return Optional.of(order)
                .orElseThrow(() -> {
                    String errMsg = String.format("Cannot found order for test case '%s' in scope '%s'",
                            this.getUuid(), testScopeId);
                    log.error(errMsg);
                    return new IllegalStateException(errMsg);
                })
                .stream()
                .filter(order -> order.getTestScopeId().equals(testScopeId))
                .findFirst()
                .orElseThrow(() -> {
                    String errMsg = String.format("Cannot found order sequence for test case '%s' in scope '%s'",
                            this.getUuid(), testScopeId);
                    log.error(errMsg);
                    return new IllegalStateException(errMsg);
                });
    }
}
