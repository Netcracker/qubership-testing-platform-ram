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

package org.qubership.atp.ram;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.qubership.atp.ram.models.RootCause;
import org.qubership.atp.ram.models.RootCauseType;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RootCauseMock {

    public List<RootCause> getAllRootCauses() {
        List<RootCause> rootCauses = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            rootCauses.add(generateRootCause("Root cause " + i));
        }
        return rootCauses;
    }

    public RootCause generateRootCause(String name) {
        return generateRootCause(name, RootCauseType.GLOBAL);
    }

    public RootCause generateRootCause(String name, RootCauseType type) {
        RootCause rootCause = new RootCause();
        rootCause.setName(name);
        rootCause.setUuid(UUID.randomUUID());
        rootCause.setType(type);

        return rootCause;
    }
}
