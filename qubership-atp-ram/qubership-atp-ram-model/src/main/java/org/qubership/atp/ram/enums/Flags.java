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

public enum Flags {
    SKIP_IF_DEPENDENCY_FAILED(UUID.fromString("8596d3da-0226-4df8-9877-0a05f7784586"),
            "skip_if_dependency_failed",
            "Skip TRs if any TRs, on which they depends, are failed", Flags.Types.EXECUTION),

    STOP_ON_FAIL(UUID.fromString("3e482af5-19fb-4e2b-82b5-879519342c68"),
            "stop_on_fail",
            "Stops the Test Run(s) executing, if the error occurred during execution",
            Flags.Types.EXECUTION),

    TERMINATE_IF_FAIL(UUID.fromString("547c84b1-4111-457d-bc7d-76e3a2a9d157"),
            "terminate_if_fail",
            "Terminate the Execution Request, if the error occurred during execution",
            Flags.Types.EXECUTION),

    IGNORE_PREREQUISITE_IN_PASS_RATE(UUID.fromString("10f36a40-5ed9-473d-a037-87a949bb56f0"),
            "ignore_prerequisite_in_pass_rate",
            "Ignore prerequisite section in pass rate", Flags.Types.EXECUTION),

    IGNORE_VALIDATION_IN_PASS_RATE(UUID.fromString("1e1a7ebb-23b3-48dc-83e5-5b0c4b92788b"),
            "ignore_validation_in_pass_rate",
            "Ignore validation section in pass rate", Flags.Types.EXECUTION);

    private UUID id;
    private String name;
    private String description;
    private Flags.Types type;

    Flags(UUID id, String name, String description, Flags.Types type) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public UUID getId() {
        return id;
    }

    public enum Types {
        EXECUTION, COLLECTION
    }
}
