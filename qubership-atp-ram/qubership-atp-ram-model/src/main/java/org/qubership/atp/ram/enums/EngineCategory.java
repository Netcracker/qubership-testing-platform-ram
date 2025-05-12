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

public enum EngineCategory {
    BV("Bulk Validator"),
    ITF("ITF"),
    EXE("Executor");

    private String name;

    EngineCategory(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     * Find EngineCategory by string name.
     * @param nameCategory Name.
     * @return EngineCategory (BV, ITF, EXE).
     */
    public static EngineCategory fromString(String nameCategory) {
        for (EngineCategory category : EngineCategory.values()) {
            if (category.getName().equals(nameCategory)) {
                return category;
            }
        }
        return null;
    }
}
