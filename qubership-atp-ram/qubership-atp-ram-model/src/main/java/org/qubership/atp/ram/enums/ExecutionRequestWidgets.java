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

package org.qubership.atp.ram.enums;

import java.util.UUID;

public enum ExecutionRequestWidgets {
    SUMMARY_STATISTIC(UUID.fromString("fcf40bf3-2610-461d-8d0a-3d05299ff396")),
    SUMMARY_STATISTIC_FOR_USAGES(UUID.fromString("8de42425-9578-41c4-8225-f8fe46a572d2")),
    SUMMARY_STATISTIC_SCENARIO_TYPE(UUID.fromString("15a0119b-e7b0-41bb-9c26-f2729ed5a30b")),
    TEST_CASES(UUID.fromString("df1571e8-ad62-45b3-ba30-1055330a4fb7")),
    TOP_ISSUES(UUID.fromString("3896a389-cc01-46b1-8f5c-8ab963c979ad"));

    private UUID widgetId;

    ExecutionRequestWidgets(UUID widgetId) {
        this.widgetId = widgetId;
    }

    public UUID getWidgetId() {
        return widgetId;
    }
}
