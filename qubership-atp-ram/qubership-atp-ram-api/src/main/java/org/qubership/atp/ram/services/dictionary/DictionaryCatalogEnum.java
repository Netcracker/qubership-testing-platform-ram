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

package org.qubership.atp.ram.services.dictionary;

import java.util.List;
import java.util.function.Supplier;

import org.qubership.atp.ram.models.dictionary.DefectFoundIn;
import org.qubership.atp.ram.models.dictionary.Dictionary;

import lombok.Getter;

@Getter
public enum DictionaryCatalogEnum {

    DEFECT_FOUND_IN("defectFoundIn", () -> DictionaryService.getAllByType(DefectFoundIn.TYPE));

    private final String name;
    private final Supplier<List<Dictionary>> supplier;

    DictionaryCatalogEnum(String name, Supplier<List<Dictionary>> supplier) {
        this.name = name;
        this.supplier = supplier;
    }
}
