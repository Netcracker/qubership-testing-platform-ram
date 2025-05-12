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
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum DefectPriority {
    LOW("Low", 0),
    NORMAL("Normal", 1),
    MAJOR("Major", 2),
    CRITICAL("Critical", 3),
    BLOCKER("Blocker", 4);

    @Getter
    private String name;
    @Getter
    private int id;

    public static List<DefectPriority> getAll() {
        return Arrays.asList(values());
    }
}
