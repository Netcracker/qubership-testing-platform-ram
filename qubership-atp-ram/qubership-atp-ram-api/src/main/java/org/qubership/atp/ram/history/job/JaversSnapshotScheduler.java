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

package org.qubership.atp.ram.history.job;

import static java.util.Objects.nonNull;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.qubership.atp.ram.model.JaversCountResponse;
import org.qubership.atp.ram.model.JaversIdsResponse;
import org.qubership.atp.ram.model.JaversVersionsResponse;
import org.qubership.atp.ram.repositories.JaversSnapshotRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Iterators;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

@Service
@Slf4j
@RequiredArgsConstructor
public class JaversSnapshotScheduler {

    private static final String UTC_TIMEZONE = "UTC";

    private final JaversSnapshotRepository repository;

    @Value("${atp.last.revision.count}")
    private Integer maxCount;

    @Value("${atp.archive.job.limit-of-data}")
    private Integer step;

    @Value("${atp.archive.job.bulk-delete-count}")
    private Integer bulkDeleteCount;

    /**
     * Job that removes irrelevant data from the change history.
     */
    @Scheduled(cron = "${atp.archive.cron.expression}", zone = UTC_TIMEZONE)
    @SchedulerLock(name = "${atp.archive.job.name}", lockAtMostFor = "12h", lockAtLeastFor = "2h")
    public void run() {
        log.info("Start remove irrelevant data from the change history.");
        deleteTerminatedSnapshots();

        Iterators.partition(repository.findAllCdoIds().iterator(), step)
                .forEachRemaining(listOfCdoId ->
                        findCdoIdAndCount(listOfCdoId
                                .stream()
                                .map(JaversIdsResponse::getCdoId).collect(Collectors.toList())
                        ).forEach(this::execute)
                );
        log.info("Finish remove irrelevant data from the change history.");
    }

    /**
     * Get list of unique cdoId with number of objects with this cdoId.
     *
     * @return {@link List} of {@link JaversCountResponse}.
     */
    public List<JaversCountResponse> findCdoIdAndCount(List<String> listOfCdoId) {
        List<JaversCountResponse> response = repository.findCdoIdAndCount(listOfCdoId);
        response.forEach(entity ->
            entity.setVersions(entity.getVersions()
                    .stream()
                    .sorted(Comparator.comparingLong(Long::longValue))
                    .collect(Collectors.toList()))
        );
        log.debug("findCdoIdAndCount [Number of unique cdo: {}]", response.size());
        return response;
    }

    /**
     * Get old cdo objects.
     *
     * @param response object with cdo id and versions.
     * @return {@link List} of {@link JaversVersionsResponse}.
     */
    public List<JaversVersionsResponse> findOldObjects(JaversCountResponse response) {
        int countVersions = response.getVersions().size();
        log.debug("findOldObjects [cdoId={}, count={}]", response.getCdoId(), countVersions);
        int countOld = countVersions - maxCount;
        if (countOld > 0) {
            List<JaversVersionsResponse> responses = response.getVersions().stream().limit(countOld).map(version -> {
                JaversVersionsResponse javersVersionsResponse = new JaversVersionsResponse();
                javersVersionsResponse.setCdoId(response.getCdoId());
                javersVersionsResponse.setVersion(version);
                return javersVersionsResponse;
            }).collect(Collectors.toList());
            log.debug("findOldObjects [Number of old objects: {} for cdoId: {}]",
                    responses.size(), response.getCdoId());
            return responses;
        }
        return null;
    }

    /**
     * Get cdo object with min version.
     *
     * @return {@link JaversVersionsResponse}.
     */
    public Long findObjectWithMinVersion(JaversCountResponse response, List<JaversVersionsResponse> oldObjects) {
        log.debug("findObjectWithMinVersion [cdoId={}]", response.getCdoId());
        Long minVersion = response.getVersions().subList(oldObjects.size(), response.getVersions().size() - 1)
                .stream().min(Comparator.comparingLong(Long::longValue)).get();
        log.debug("findObjectWithMinVersion [minVersion={}]", minVersion);
        return minVersion;
    }

    /**
     * Delete old cdo objects by cdo id and version.
     *
     * @param cdoId    cdo id.
     * @param versions {@link List} of {@link Long} with old versions.
     */
    public void deleteByCdoIdAndVersions(String cdoId, List<Long> versions) {
        log.debug("deleteByCdoIdAndVersions [cdoId={}, versions={}]", cdoId, versions);
        Iterators.partition(versions.iterator(), bulkDeleteCount)
                .forEachRemaining(ids -> repository.deleteByCdoIdAndVersions(cdoId, ids));
        log.debug("deleteByCdoIdAndVersions [Objects with cdoId={}. Number of old versions for deleting: {}]",
                cdoId, versions.size());
    }

    /**
     * Update the oldest cdo object as initial object.
     *
     * @param cdoId   cdo id.
     * @param version the oldest version.
     */
    public void updateObjectAsInitial(String cdoId, Long version) {
        log.debug("updateObjectAsInitial [cdoId={}, version={}]", cdoId, version);
        repository.updateAsInitial(cdoId, version);
        log.debug("updateObjectAsInitial [Object with cdoId={}, version={} updated]", cdoId, version);
    }

    /**
     * Get terminated cdo ids.
     *
     * @return {@link List} of {@link String} of terminated ids.
     */
    public List<String> findTerminatedCdoId() {
        List<JaversIdsResponse> snapshots = repository.findTerminatedSnapshots();
        log.debug("findTerminatedSnapshots [Number of terminated snapshots: {}]", snapshots.size());
        return findOldCdoIds(snapshots);
    }

    /**
     * Delete terminated objects.
     */
    public void deleteTerminatedSnapshots() {
        List<String> terminatedCdoId = findTerminatedCdoId();
        Iterators.partition(terminatedCdoId.iterator(), bulkDeleteCount)
                .forEachRemaining(repository::deleteByCdoIds);
        log.debug("Terminated snapshots deleted");
    }

    private List<String> findOldCdoIds(List<JaversIdsResponse> oldObjects) {
        return oldObjects.stream()
                .map(JaversIdsResponse::getCdoId)
                .collect(Collectors.toList());
    }

    /**
     * Get old versions of cdo object.
     *
     * @param oldObjects {@link List} of {@link JaversVersionsResponse} of old objects.
     * @return {@link List} of {@link Long} with old versions.
     */
    private List<Long> findOldVersions(List<JaversVersionsResponse> oldObjects) {
        return oldObjects.stream()
                .filter(response -> nonNull(response.getVersion()))
                .map(JaversVersionsResponse::getVersion)
                .collect(Collectors.toList());
    }

    /**
     * Delete old versions.
     */
    @Transactional
    public void execute(JaversCountResponse response) {
        List<JaversVersionsResponse> oldObjects = findOldObjects(response);
        if (nonNull(oldObjects)) {
            List<Long> versions = findOldVersions(oldObjects);
            deleteByCdoIdAndVersions(response.getCdoId(), versions);
            Long version = findObjectWithMinVersion(response, oldObjects);
            updateObjectAsInitial(response.getCdoId(), version);
        }
    }
}
