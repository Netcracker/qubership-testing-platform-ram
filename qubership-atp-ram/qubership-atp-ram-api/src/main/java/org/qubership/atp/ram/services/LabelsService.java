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

package org.qubership.atp.ram.services;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.qubership.atp.ram.models.Label;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class LabelsService {

    private final CatalogueService catalogueService;

    /**
     * Returns labels by label ids.
     *
     * @param uuidLabels UUID of labels.
     * @return collection of labels where label uuid in {@code uuidLabels}
     */
    public List<Label> getLabels(Set<UUID> uuidLabels) {
        return catalogueService.getLabelsByIds(uuidLabels);
    }
}
