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

import lombok.Getter;

public enum UserManagementEntities {
    EXECUTION_REQUEST("Execution Request"),
    FAIL_REASON("Fail Reason"),
    FAIL_PATTERN("Fail Pattern"),
    MAIL_TEMPLATE("Mail Template"),
    TEST_RUN("Test Run"),
    VALIDATION_LABEL_TEMPLATE("Validation Label Template"),
    WIDGET_CONFIGURATION("Widget Configuration");

    @Getter
    private String name;

    UserManagementEntities(String name) {
        this.name = name;
    }
}
