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

import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

public enum DefaultRootCauseType {
    AT_ISSUE("AT Issue"),
    DESIGN_CHANGE("Design Change"),
    ERROR_IN_CONFIGURATION("Error in configuration"),
    INVESTIGATION_NEEDED("Investigation Needed"),
    NOT_ANALYZED("Not Analyzed"),
    PERFOMANCE_ISSUE("Performance Issue"),
    SOLUTION_ISSUE("Solution Issue"),
    ATP_MAINTENCE("ATP Maintenance"),
    NETWORK_ISSUE("Network Issue"),
    ENVIROMENT_ISSUE("Environment Issue"),
    TEST_DATA_ISSUE("Test Data Issue");

    private static final Map<String, DefaultRootCauseType> NAME_INDEX =
            Maps.newHashMapWithExpectedSize(values().length);

    static {
        for (DefaultRootCauseType defaultRootCauseType : values()) {
            NAME_INDEX.put(defaultRootCauseType.getName(), defaultRootCauseType);
        }
    }

    private String name;

    DefaultRootCauseType(String name) {
        this.name = name;
    }

    /**
     * Return root cause type by name.
     *
     * @return default root cause type {@link DefaultRootCauseType}
     */
    public static DefaultRootCauseType fromName(String name) {
        DefaultRootCauseType result = NAME_INDEX.get(name);
        Preconditions.checkNotNull(result, "Not valid root cause type: ", name);
        return result;
    }

    public String getName() {
        return name;
    }
}
