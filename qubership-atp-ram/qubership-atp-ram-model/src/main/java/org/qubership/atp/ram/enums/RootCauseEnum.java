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

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.qubership.atp.ram.models.RootCause;
import org.qubership.atp.ram.models.RootCauseType;

public enum RootCauseEnum {

    AT_ISSUE(new RootCause(
            UUID.fromString("f70b415a-e312-447e-b2c3-88caca636a88"),
            RootCauseType.GLOBAL,
            "AT Issue")
    ),
    ATP_MAINTENANCE(new RootCause(
            UUID.fromString("f0fd41e2-e1f8-4779-986b-705cef2ace4c"),
            RootCauseType.GLOBAL,
            "ATP Maintenance")
    ),
    ATP_WARNING(new RootCause(
            UUID.fromString("325a8ffb-e936-4ff6-9af3-18f5953070d1"),
            RootCauseType.GLOBAL,
            "ATP Warning")
    ),
    DESIGN_CHANGE(new RootCause(
            UUID.fromString("85923624-26c2-4f8a-96ce-15ad3d2a5494"),
            RootCauseType.GLOBAL,
            "Design Change")
    ),
    ENVIRONMENT_ISSUE(new RootCause(
            UUID.fromString("5980c906-25eb-438e-b8ac-14d69d925605"),
            RootCauseType.GLOBAL,
            "Environment Issue")
    ),
    ERROR_IN_CONFIGURATION(new RootCause(
            UUID.fromString("8365f275-1c9d-4977-b515-a2dfb3976cbe"),
            RootCauseType.GLOBAL,
            "Error in configuration")
    ),
    INVESTIGATION_NEEDED(new RootCause(
            UUID.fromString("513d65f6-2774-421d-ab71-1b316b9283f8"),
            RootCauseType.GLOBAL,
            "Investigation Needed")
    ),
    NETWORK_ISSUE(new RootCause(
            UUID.fromString("9261919a-185d-43c6-ab42-a7a42e12c8c7"),
            RootCauseType.GLOBAL,
            "Network Issue")
    ),
    NOT_ANALYZED(new RootCause(
            UUID.fromString("9015ba95-3fa9-4036-ba69-da2f1ab743a2"),
            RootCauseType.GLOBAL,
            "Not Analyzed")
    ),
    PERFORMANCE_ISSUE(new RootCause(
            UUID.fromString("e8e8e022-a7d5-468e-a959-f03c6328170f"),
            RootCauseType.GLOBAL,
            "Performance Issue")
    ),
    SOLUTION_ISSUE(new RootCause(
            UUID.fromString("621d6ee7-7687-4da9-9adb-7e81ca1f0cd8"),
            RootCauseType.GLOBAL,
            "Solution Issue")
    ),
    TEST_DATA_ISSUE(new RootCause(
            UUID.fromString("5bbf39f7-9aaf-44c2-b8cd-b44562d017ac"),
            RootCauseType.GLOBAL,
            "Test Data Issue")
    );

    private RootCause rootCause;

    RootCauseEnum(RootCause rootCause) {
        this.rootCause = rootCause;
    }

    public RootCause getRootCause() {
        return rootCause;
    }

    public static List<RootCause> getAll() {
        return Arrays.stream(values())
                .map(RootCauseEnum::getRootCause)
                .collect(Collectors.toList());
    }
}
