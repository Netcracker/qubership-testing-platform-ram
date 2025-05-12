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

package org.qubership.atp.ram.repositories;

import java.util.List;
import java.util.stream.Stream;

import org.qubership.atp.ram.model.JaversCountResponse;
import org.qubership.atp.ram.model.JaversIdsResponse;

public interface JaversSnapshotRepository {

    List<JaversCountResponse> findCdoIdAndCount(List<String> listOfCdoId);

    Stream<JaversIdsResponse> findAllCdoIds();

    void deleteByCdoIdAndVersions(String cdoId, List<Long> versions);

    void updateAsInitial(String cdoId, Long version);

    List<JaversIdsResponse> findTerminatedSnapshots();

    void deleteByCdoIds(List<String> cdoIds);
}
