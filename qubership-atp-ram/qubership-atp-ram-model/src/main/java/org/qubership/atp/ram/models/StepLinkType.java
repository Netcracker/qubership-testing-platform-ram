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

import java.util.Arrays;

import com.google.common.base.Strings;

public enum StepLinkType {
    EXECUTOR, ITFACTION, ITFCALLCHAIN, ITFWARMUP, BV, NEWMAN_RUNNER, ITF_LITE, MIA;

    /**
     * StepLinkType get by string.
     *
     * @param type type as string
     * @return StepLinkType
     */
    public static StepLinkType findByValue(String type) {
        if (Strings.isNullOrEmpty(type)) {
            return StepLinkType.EXECUTOR;
        }
        return Arrays.stream(StepLinkType.values())
                .filter(x -> x.toString().equalsIgnoreCase(type))
                .findAny()
                .orElse(StepLinkType.EXECUTOR);
    }
}
