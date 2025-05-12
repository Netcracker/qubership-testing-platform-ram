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

package org.qubership.atp.ram.models.dictionary;

import static java.util.UUID.fromString;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum DefectFoundInEnum {
    ANALYSIS(fromString("1d511571-7f35-4a58-8e2c-1674b4d71cc0"), "Analysis"),
    BUILD(fromString("a179635d-ab5c-4304-b210-ca6762e898fe"), "Build"),
    E2E_TEST(fromString("ff634055-f906-4535-9f60-361739d61069"), "End-to-End Test"),
    SUPPORT(fromString("0da22ad5-1819-4a60-b005-ca5567738396"), "Support"),
    SVT(fromString("2fcce036-0d07-4482-acd6-fce6964d30b0"), "SVT"),
    SYSTEM_TEST(fromString("8c1ddadb-5ac7-4f09-a103-60b43e784712"), "System Test"),
    UAT(fromString("8a58dd76-cf6e-42df-a085-a5979ce7d8b4"), "UAT"),
    WARRANTY(fromString("1eb000cd-5a6e-464c-bf4b-3dfa5c2bb111"), "Warranty");

    private final DefectFoundIn defectFoundIn;

    DefectFoundInEnum(UUID id, String name) {
        this.defectFoundIn = new DefectFoundIn(id, name, null);
    }

    public static List<DefectFoundInEnum> getAll() {
        return Arrays.asList(values());
    }
}
